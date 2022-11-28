package ticketingsystem;

public class Test {

	public static void main(String[] args) throws InterruptedException {
        final int routenum = 5;
        final int coachnum = 8;
        final int seatnum = 100;
        final int stationnum = 10;
        final int threadnum = 16;

		final TicketingDS tds = new TicketingDS(routenum, coachnum, seatnum, stationnum, threadnum);
        tds.buyTicket("wyt", 0, 0, 1);

		//ToDo
	    
	}
}
