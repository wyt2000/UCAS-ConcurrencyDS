package ticketingsystem;

class Ticket{
	long tid;
	String passenger;
	int route;
	int coach;
	int seat;
	int departure;
	int arrival;

    @Override
    public String toString() {
        String s = "tid: " + tid + "\npassenger: " 
            + passenger + "\nroute: " + route
            + "\ncoach: " + coach + "\nseat: " + seat
            + "\ndeparture: " + departure + "\narrival: " + arrival;
        return s;
    }

    @Override
    public int hashCode() {
        String s = tid + " " + passenger + " " + route + " " 
            + coach + " " + seat + " " + departure + " " + arrival;
        return s.hashCode();
    }
    
    @Override
    public boolean equals(Object object) {
        Ticket t = (Ticket) object;
        if (t == null) return false;
        if (tid != t.tid) return false;
        if (!passenger.equals(t.passenger)) return false;
        if (route != t.route) return false;
        if (coach != t.coach) return false;
        if (seat != t.seat) return false;
        if (departure != t.departure) return false;
        if (arrival != t.arrival) return false;
        return true;
    }
}

public interface TicketingSystem {
	Ticket buyTicket(String passenger, int route, int departure, int arrival);
	int inquiry(int route, int departure, int arrival);
	boolean refundTicket(Ticket ticket);
	boolean buyTicketReplay(Ticket ticket);
	boolean refundTicketReplay(Ticket ticket);
}
