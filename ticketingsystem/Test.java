package ticketingsystem;

import java.util.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.concurrent.atomic.AtomicInteger;

public class Test{
    static int threadnum;//input
    static int testnum;//input
    static boolean isSequential;//input
    static int totalPc;
    final static int testTimes = 10;

    /****************Manually Set Testing Information **************/

    static int routenum = 3; // route is designed from 1 to 3
    static int coachnum = 3; // coach is arranged from 1 to 5
    static int seatnum = 5; // seat is allocated from 1 to 20
    static int stationnum = 5; // station is designed from 1 to 5

    static int refRatio = 10; 
    static int buyRatio = 20; 
    static int inqRatio = 30; 

    static TicketingDS tds;
    final static String[] methodList = new String[3];
    final static Integer[] freqList = new Integer[3];

    static long[] buyTime;
    static long[] inqTime;
    static long[] refTime;
    static long[] buyCount;
    static long[] inqCount;
    static long[] refCount;

    static double[] avgBuyCost = new double[testTimes];
    static double[] avgInqCost = new double[testTimes];
    static double[] avgRefCost = new double[testTimes];
    static double[] throughput = new double[testTimes];
    
    private final static AtomicInteger threadId = new AtomicInteger(0);

    volatile static boolean initLock = false;
    final static Random rand = new Random();

    private static void clear() {
        threadId.set(0);
    }

    public static void initialization(){
        tds = new TicketingDS(routenum, coachnum, seatnum, stationnum, threadnum);
        buyTime = new long[threadnum];
        inqTime = new long[threadnum];
        refTime = new long[threadnum];
        buyCount = new long[threadnum];
        inqCount = new long[threadnum];
        refCount = new long[threadnum];
        //method freq is up to	
        methodList[0] = "refundTicket";
        freqList[0] = refRatio;
        methodList[1] = "buyTicket";
        freqList[1] = refRatio + buyRatio;
        methodList[2] = "inquiry";
        freqList[2] = refRatio + buyRatio + inqRatio;
        totalPc = refRatio + buyRatio + inqRatio;
    }
    public static String getPassengerName() {
        long uid = rand.nextInt(testnum);
        return "passenger" + uid; 
    }

    private static boolean readConfig(String filename) {
        try {
            Scanner scanner = new Scanner(new File(filename));

            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                //System.out.println(line);
                Scanner linescanner = new Scanner(line);
                if (line.equals("")) {
                    linescanner.close();
                    continue;
                }
                if (line.substring(0,1).equals("#")) {
                    linescanner.close();
                    continue;
                }
                routenum = linescanner.nextInt();
                coachnum = linescanner.nextInt();
                seatnum = linescanner.nextInt();
                stationnum = linescanner.nextInt();

                refRatio = linescanner.nextInt();
                buyRatio = linescanner.nextInt();
                inqRatio = linescanner.nextInt();
                //System.out.println("route: " + routenum + ", coach: " + coachnum + ", seatnum: " + seatnum + ", station: " + stationnum + ", refundRatio: " + refRatio + ", buyRatio: " + buyRatio + ", inquiryRatio: " + inqRatio);
                linescanner.close();
            }
            scanner.close();
        }catch (FileNotFoundException e) {
            System.out.println(e);
        }
        return true;
    }
    
    static double getAvg(double[] array) {
        double ans = 0;
        for (int i = 4; i < array.length; ++i) {
            ans += array[i];
        }
        return ans / (array.length - 4);
    } 

    /***********VeriLin***********/
    public static void main(String[] args) throws InterruptedException {
        if(args.length != 2){
            System.out.println("The arguments of Test is threadNum, testNum");
            return;
        }
        threadnum = Integer.parseInt(args[0]);
        testnum = Integer.parseInt(args[1]);
        readConfig("TrainConfig");
        Thread[] threads = new Thread[threadnum];

        initialization();

        for (int n = 0; n < testTimes; n++) {
            clear();
            for (int i = 0; i < threadnum; i++) {
                threads[i] = new Thread(new Runnable() {
                    public void run() {
                        int id = threadId.getAndIncrement();
                        ArrayList<Ticket> soldTicket = new ArrayList<Ticket>();
                        for(int k = 0; k < testnum; k++){
                            int sel = rand.nextInt(totalPc);
                            int cnt = 0;
                            for(int j = 0; j < methodList.length; j++){
                                if(sel >= cnt && sel < cnt + freqList[j]){
                                    int route, departure, arrival;
                                    long preTime, postTime;
                                    Ticket ticket = new Ticket();
                                    switch(j){
                                        case 0://refund
                                            if(soldTicket.size() == 0)
                                                break;
                                            int n = rand.nextInt(soldTicket.size());
                                            ticket = soldTicket.remove(n);
                                            if(ticket == null){
                                                break;
                                            }
                                            preTime = System.nanoTime();
                                            tds.refundTicket(ticket);
                                            postTime = System.nanoTime();
                                            refTime[id] += postTime - preTime;
                                            refCount[id]++;
                                            break;
                                        case 1://buy
                                            String passenger = getPassengerName();
                                            route = rand.nextInt(routenum) + 1;
                                            departure = rand.nextInt(stationnum - 1) + 1;
                                            arrival = departure + rand.nextInt(stationnum - departure) + 1;
                                            preTime = System.nanoTime();
                                            ticket = tds.buyTicket(passenger, route, departure, arrival);
                                            postTime = System.nanoTime();
                                            buyTime[id] += postTime - preTime;
                                            buyCount[id]++;
                                            if (ticket != null) {
                                                soldTicket.add(ticket);
                                            }
                                            break;
                                        case 2://inquiry
                                            ticket.route = rand.nextInt(routenum) + 1;
                                            ticket.departure = rand.nextInt(stationnum - 1) + 1;
                                            ticket.arrival = ticket.departure + rand.nextInt(stationnum - ticket.departure) + 1; // arrival is always greater than departure
                                            preTime = System.nanoTime();
                                            ticket.seat = tds.inquiry(ticket.route, ticket.departure, ticket.arrival);
                                            postTime = System.nanoTime();
                                            inqTime[id] += postTime - preTime;
                                            inqCount[id]++;
                                            break;
                                        default:
                                            System.out.println("Error in execution.");
                                            break;
                                    }
                                    cnt += freqList[j];
                                }
                            }
                        }
                    }
                });
            }
            long startTime = System.nanoTime();
            for (int i = 0; i < threadnum; i++) {
                threads[i].start();
            }
            for (int i = 0; i < threadnum; i++) {
                threads[i].join();
            }	
            double totalTime = System.nanoTime() - startTime;

            long totalBuyTime = 0;
            long totalInqTime = 0;
            long totalRefTime = 0;
            long totalBuyCount = 0;
            long totalInqCount = 0;
            long totalRefCount = 0;

            for (int i = 0; i < threadnum; ++i) {
                totalBuyTime += buyTime[i];
                totalInqTime += inqTime[i];
                totalRefTime += refTime[i];
                totalBuyCount += buyCount[i];
                totalInqCount += inqCount[i];
                totalRefCount += refCount[i];
            }

            long totalCount = totalBuyCount + totalInqCount + totalRefCount;

            avgBuyCost[n] = (double) totalBuyTime / totalBuyCount;
            avgInqCost[n] = (double) totalInqTime / totalInqCount;
            avgRefCost[n] = (double) totalRefTime / totalRefCount;
            throughput[n] = totalCount / totalTime * 1e6;

        }
        System.out.println("========== ThreadNum: " + threadnum + " Begin ============");

        System.out.println("Method: buyTicket");
        System.out.println("Average Cost: " + getAvg(avgBuyCost) + " ns.\n");

        System.out.println("Method: inquiry");
        System.out.println("Average Cost: " + getAvg(avgInqCost) + " ns.\n");

        System.out.println("Method: refundTicket");
        System.out.println("Average Cost: " + getAvg(avgRefCost)  + " ns.\n");

        System.out.println("Throughput: " + getAvg(throughput) + " times / ms.");

        System.out.println("========== ThreadNum: " + threadnum + " End ============");
    } 
}

