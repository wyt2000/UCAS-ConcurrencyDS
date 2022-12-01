package ticketingsystem;

import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import java.io.File;
import java.io.FileNotFoundException;

public class Test{
	static int threadnum;//input
	static int testnum;//input
	static boolean isSequential;//input
	static int msec = 0;
	static int nsec = 0;
	static int totalPc;
    
	static  AtomicInteger sLock = new AtomicInteger(0); //Synchronization Lock
	static boolean[] fin;

	protected static boolean exOthNotFin(int tNum, int tid) {
		boolean flag = false;
		for (int k = 0; k < tNum; k++) {
			if (k == tid) continue;
			flag = (flag || !(fin[k])); 
		}
		return flag;
	}

    static void SLOCK_TAKE() {
    	while (sLock.compareAndSet(0, 1) == false) {}
    }

    static void SLOCK_GIVE() {
        sLock.set(0);
    }

    static boolean SLOCK_TRY() {
        return (sLock.get() == 0);
    }

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
	final static List<Ticket> currentTicket = new ArrayList<Ticket>();
	final static List<String> currentRes = new ArrayList<String>();
    final static ArrayList<List<Ticket>> soldTicket = new ArrayList<List<Ticket>>();
    final static ArrayList<Double> methodTime = new ArrayList<Double>();
    final static ArrayList<Long> methodCount = new ArrayList<Long>();
	volatile static boolean initLock = false;
//	final static AtomicInteger tidGen = new AtomicInteger(0);
	final static Random rand = new Random();
	public static void initialization(){
	  tds = new TicketingDS(routenum, coachnum, seatnum, stationnum, threadnum);
	  for(int i = 0; i < threadnum; i++){
		List<Ticket> threadTickets = new ArrayList<Ticket>();
		soldTicket.add(threadTickets);
		currentTicket.add(null);
		currentRes.add("");
	  }
		//method freq is up to	
	  methodList.add("refundTicket");
	  freqList.add(refRatio);
      methodTime.add(0.0);
      methodCount.add(0L);
	  methodList.add("buyTicket");
	  freqList.add(refRatio+buyRatio);
      methodTime.add(0.0);
      methodCount.add(0L);
	  methodList.add("inquiry");
	  freqList.add(refRatio+buyRatio+inqRatio);
      methodTime.add(0.0);
      methodCount.add(0L);
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

	public static void print(long preTime, long postTime, String actionName){
	  Ticket ticket = currentTicket.get(ThreadId.get());
	  System.out.println(preTime + " " + postTime + " " +  ThreadId.get() + " " + actionName + " " + ticket.tid + " " + ticket.passenger + " " + ticket.route + " " + ticket.coach + " " + ticket.departure + " " + ticket.arrival + " " + ticket.seat + " " + currentRes.get(ThreadId.get()));
	}

	public static boolean execute(int num){
	  int route, departure, arrival;
	  Ticket ticket = new Ticket();;
	  switch(num){
		case 0://refund
		  if(soldTicket.get(ThreadId.get()).size() == 0)
			return false;
		  int n = rand.nextInt(soldTicket.get(ThreadId.get()).size());
		  ticket = soldTicket.get(ThreadId.get()).remove(n);
		  if(ticket == null){
			return false;
		  }
		  currentTicket.set(ThreadId.get(), ticket);
		  boolean flag = tds.refundTicket(ticket);
		  currentRes.set(ThreadId.get(), "true"); 
		  return flag;
		case 1://buy
          String passenger = getPassengerName();
          route = rand.nextInt(routenum) + 1;
          departure = rand.nextInt(stationnum - 1) + 1;
          arrival = departure + rand.nextInt(stationnum - departure) + 1;
		  ticket = tds.buyTicket(passenger, route, departure, arrival);
		  if(ticket == null){
			ticket = new Ticket();
			ticket.passenger = passenger;
			ticket.route = route;
			ticket.departure = departure;
			ticket.arrival = arrival;
			ticket.seat = 0;
			currentTicket.set(ThreadId.get(), ticket);
			currentRes.set(ThreadId.get(), "false");
			return true;
		  }
		  currentTicket.set(ThreadId.get(), ticket);
		  currentRes.set(ThreadId.get(), "true");
		  soldTicket.get(ThreadId.get()).add(ticket);
		  return true;
		case 2://inquiry
          ticket.passenger = getPassengerName();
          ticket.route = rand.nextInt(routenum) + 1;
          ticket.departure = rand.nextInt(stationnum - 1) + 1;
          ticket.arrival = ticket.departure + rand.nextInt(stationnum - ticket.departure) + 1; // arrival is always greater than departure
		  ticket.seat = tds.inquiry(ticket.route, ticket.departure, ticket.arrival);
		  currentTicket.set(ThreadId.get(), ticket);
		  currentRes.set(ThreadId.get(), "true"); 
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
	testnum = Integer.parseInt(args[1]) / threadnum;
    isSequential = false;
	msec = 0;
	nsec = 0;
	readConfig("TrainConfig");
	Thread[] threads = new Thread[threadnum];
	fin = new boolean[threadnum];
    
	final long startTime = System.nanoTime();
	for (int i = 0; i < threadnum; i++) {
	    	threads[i] = new Thread(new Runnable() {
                public void run() {
					if(ThreadId.get() == 0){
					  initialization();
					  initLock = true;
					}
					else{
					  while(!initLock){
						;
					  }
					}
					for(int k = 0; k < testnum; k++){
					  int sel = rand.nextInt(totalPc);
					  int cnt = 0;

					  for(int j = 0; j < methodList.size(); j++){
						if(sel >= cnt && sel < cnt + freqList.get(j)){
						  long preTime = System.nanoTime() - startTime;
						  execute(j);
						  long postTime = System.nanoTime() - startTime;
                          double diffTime = postTime - preTime;
                          synchronized(this) {
                            // newavg = (oldavg * (size - 1) + newvalue) / size
                            methodCount.set(j, methodCount.get(j) + 1);
                            methodTime.set(j, (methodTime.get(j) * (methodCount.get(j) - 1) + diffTime) / methodCount.get(j));
                          }
						  cnt += freqList.get(j);
						}
					  }
					}
				}
            });
			threads[i].start();
	  }
	
	  for (int i = 0; i< threadnum; i++) {
		threads[i].join();
	  }	

      double totalTime = System.nanoTime() - startTime;
      long totalCount = 0;
      for (int i = 0; i < methodList.size(); ++i) {
        System.out.println("Method: " + methodList.get(i));
        System.out.println("Call " + methodCount.get(i) + " times, ");
        System.out.println("Average Cost: " + methodTime.get(i) + " ns.\n");
        totalCount += methodCount.get(i);
      }
      System.out.println("Total Count: " + totalCount + " times.");
      System.out.println("Throughput: " + totalCount / totalTime * 1e6 + " times / ms.");
	}
}

