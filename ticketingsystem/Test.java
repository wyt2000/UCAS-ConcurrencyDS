package ticketingsystem;

import java.util.List;
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
    final static List<String> methodList = new ArrayList<String>();
    final static List<Integer> freqList = new ArrayList<Integer>();
    final static ArrayList<List<Ticket>> soldTicket = new ArrayList<List<Ticket>>();

    final static ArrayList<Long> buyTime = new ArrayList<Long>();
    final static ArrayList<Long> inqTime = new ArrayList<Long>();
    final static ArrayList<Long> refTime = new ArrayList<Long>();
    final static ArrayList<Long> buyCount = new ArrayList<Long>();
    final static ArrayList<Long> inqCount = new ArrayList<Long>();
    final static ArrayList<Long> refCount = new ArrayList<Long>();
    
    private final static AtomicInteger threadId = new AtomicInteger(0);

    volatile static boolean initLock = false;
    final static Random rand = new Random();

    private static void clear() {
        for (int i = 0; i < threadnum; ++i) {
            buyTime.set(i, 0L);
            inqTime.set(i, 0L);
            refTime.set(i, 0L);
            buyCount.set(i, 0L);
            inqCount.set(i, 0L);
            refCount.set(i, 0L);
        }
        soldTicket.clear();
        threadId.set(0);
        for(int i = 0; i < threadnum; i++){
            List<Ticket> threadTickets = new ArrayList<Ticket>();
            soldTicket.add(threadTickets);
        }
    }

    public static void initialization(){
        tds = new TicketingDS(routenum, coachnum, seatnum, stationnum, threadnum);
        for(int i = 0; i < threadnum; i++){
            List<Ticket> threadTickets = new ArrayList<Ticket>();
            soldTicket.add(threadTickets);
        }
        for (int i = 0; i < threadnum; ++i) {
            buyTime.add(0L);
            inqTime.add(0L);
            refTime.add(0L);
            buyCount.add(0L);
            inqCount.add(0L);
            refCount.add(0L);
        }
        //method freq is up to	
        methodList.add("refundTicket");
        freqList.add(refRatio);
        methodList.add("buyTicket");
        freqList.add(refRatio+buyRatio);
        methodList.add("inquiry");
        freqList.add(refRatio+buyRatio+inqRatio);
        totalPc = refRatio+buyRatio+inqRatio;
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

    public static boolean execute(int num, int id){
        int route, departure, arrival;
        long preTime, postTime;
        Ticket ticket = new Ticket();;
        switch(num){
            case 0://refund
                if(soldTicket.get(id).size() == 0)
                    return false;
                int n = rand.nextInt(soldTicket.get(id).size());
                ticket = soldTicket.get(id).remove(n);
                if(ticket == null){
                    return false;
                }
                preTime = System.nanoTime();
                tds.refundTicket(ticket);
                postTime = System.nanoTime();
                refTime.set(id, refTime.get(id) + (postTime - preTime));
                refCount.set(id, refCount.get(id) + 1);
                return true;
            case 1://buy
                String passenger = getPassengerName();
                route = rand.nextInt(routenum) + 1;
                departure = rand.nextInt(stationnum - 1) + 1;
                arrival = departure + rand.nextInt(stationnum - departure) + 1;
                preTime = System.nanoTime();
                ticket = tds.buyTicket(passenger, route, departure, arrival);
                postTime = System.nanoTime();
                buyTime.set(id, buyTime.get(id) + (postTime - preTime));
                buyCount.set(id, buyCount.get(id) + 1);
                if (ticket != null) {
                    soldTicket.get(id).add(ticket);
                }
                return true;
            case 2://inquiry
                ticket.route = rand.nextInt(routenum) + 1;
                ticket.departure = rand.nextInt(stationnum - 1) + 1;
                ticket.arrival = ticket.departure + rand.nextInt(stationnum - ticket.departure) + 1; // arrival is always greater than departure
                preTime = System.nanoTime();
                ticket.seat = tds.inquiry(ticket.route, ticket.departure, ticket.arrival);
                postTime = System.nanoTime();
                inqTime.set(id, inqTime.get(id) + (postTime - preTime));
                inqCount.set(id, inqCount.get(id) + 1);
                return true;
            default:
                System.out.println("Error in execution.");
                return false;
        }
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

        for (int n = 1; n <= testTimes; n++) {
            clear();
            for (int i = 0; i < threadnum; i++) {
                threads[i] = new Thread(new Runnable() {
                    public void run() {
                        int id = threadId.getAndIncrement();
                        for(int k = 0; k < testnum; k++){
                            int sel = rand.nextInt(totalPc);
                            int cnt = 0;
                            for(int j = 0; j < methodList.size(); j++){
                                if(sel >= cnt && sel < cnt + freqList.get(j)){
                                    execute(j, id);
                                    cnt += freqList.get(j);
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
                totalBuyTime += buyTime.get(i);
                totalInqTime += inqTime.get(i);
                totalRefTime += refTime.get(i);
                totalBuyCount += buyCount.get(i);
                totalInqCount += inqCount.get(i);
                totalRefCount += refCount.get(i);
            }

            long totalCount = totalBuyCount + totalInqCount + totalRefCount;

            System.out.println("========== Test: " + n + " Begin ============");

            System.out.println("Method: buyTicket");
            System.out.println("Call " + totalBuyCount + " times, ");
            System.out.println("Average Cost: " +  (double) totalBuyTime / totalBuyCount + " ns.\n");

            System.out.println("Method: inquiry");
            System.out.println("Call " + totalInqCount + " times, ");
            System.out.println("Average Cost: " +  (double) totalInqTime / totalInqCount + " ns.\n");

            System.out.println("Method: refundTicket");
            System.out.println("Call " + totalRefCount + " times, ");
            System.out.println("Average Cost: " +  (double) totalRefTime / totalRefCount + " ns.\n");

            System.out.println("Total Count: " + totalCount + " times.");
            System.out.println("Throughput: " + totalCount / totalTime * 1e6 + " times / ms.");

            System.out.println("========== Test: " + n + " End ============");
        }
    } 
}

