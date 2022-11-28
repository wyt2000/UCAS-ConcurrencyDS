package ticketingsystem;

public class TicketingDS implements TicketingSystem {
    private int routenum;
    private int coachnum;
    private int seatnum;
    private int stationnum;
    private int threadnum;

    public TicketingDS(int _routenum, int _coach_num, int _seatnum, int _stationnum, int _threadnum) {
        routenum = _routenum;
        coachnum = _coach_num;
        seatnum = _seatnum;
        stationnum = _stationnum;
        threadnum = _threadnum;
    }

    @Override
    public Ticket buyTicket(String passenger, int route, int departure, int arrival) {
        Ticket t = new Ticket();
        t.tid = 0;
        t.passenger = passenger;
        t.route = route;
        t.coach = 0;
        t.seat = 0;
        t.departure = departure;
        t.arrival = arrival;
        return t;
    }

    @Override
    public int inquiry(int route, int departure, int arrival) {
        return 1;
    }

    @Override
    public boolean refundTicket(Ticket ticket) {
        return true;
    }

    @Override
    public boolean buyTicketReplay(Ticket ticket) {
        return true;
    }

    @Override
    public boolean refundTicketReplay(Ticket ticket) {
        return true;
    }

}
