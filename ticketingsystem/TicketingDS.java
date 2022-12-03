package ticketingsystem;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicBoolean;

public class TicketingDS implements TicketingSystem {
    private int routenum;
    private int coachnum;
    private int seatnum;
    private int stationnum;

    private AtomicLong tid;                                         // Next available tid.
    private HashSet<Ticket> tickets;                                // Tickets sold.
    private ArrayList<ArrayList<ArrayList<AtomicBoolean>>> seats;         // Seat id to stations of every route.

    public TicketingDS(int _routenum, int _coachnum, int _seatnum, int _stationnum, int _threadnum) {
        routenum    = _routenum;
        coachnum    = _coachnum;
        seatnum     = _seatnum;
        stationnum  = _stationnum;

        tid         = new AtomicLong(0);
        tickets     = new HashSet<Ticket>();
        seats       = new ArrayList<ArrayList<ArrayList<AtomicBoolean>>>();
        for (int i = 0; i < routenum; ++i) {
            ArrayList<ArrayList<AtomicBoolean>> seatsPerCoach = new ArrayList<ArrayList<AtomicBoolean>>();
            for (long j = 0; j < coachnum * seatnum; ++j) {
                ArrayList<AtomicBoolean> stations = new ArrayList<AtomicBoolean>();
                for (int k = 0; k < stationnum; ++k) {
                    stations.add(new AtomicBoolean(false));
                }
                seatsPerCoach.add(stations);
            }
            seats.add(seatsPerCoach);
        }
    }

    @Override
    public Ticket buyTicket(String passenger, int route, int departure, int arrival) {
        /* Find a seat of route, which isn't occupied [departure, arrival). */
        int i;
        for (i = 0; i < coachnum * seatnum; ++i) {
            int j;
            for (j = departure; j < arrival; ++j) {
                if (seats.get(route - 1).get(i).get(j - 1).get()) {
                    break;
                }
            }
            if (j == arrival) {
                break;
            }
        }
        if (i == coachnum * seatnum) {
            return null;
        }

        /* Occupied the stations. */
        for (int j = departure; j < arrival; ++j) {
            seats.get(route - 1).get(i).get(j - 1).set(true);
        }

        int coach = i / seatnum + 1; 
        int seat = i % seatnum + 1; 

        Ticket t = new Ticket();
        t.tid = tid.getAndIncrement();
        t.passenger = passenger;
        t.route = route;
        t.coach = coach;
        t.seat = seat;
        t.departure = departure;
        t.arrival = arrival;
        synchronized (this) {
            tickets.add(t);
        }
        return t;
    }

    @Override
    public int inquiry(int route, int departure, int arrival) {
        int cnt = 0;
        int i;
        for (i = 0; i < coachnum * seatnum; ++i) {
            int j;
            for (j = departure; j < arrival; ++j) {
                if (seats.get(route - 1).get(i).get(j - 1).get()) {
                    break;
                }
            }
            if (j == arrival) {
                ++cnt;
            }
        }
        return cnt;
    }

    @Override
    public boolean refundTicket(Ticket ticket) {
        synchronized (this) {
            if (!tickets.contains(ticket)) {
                return false;
            }
        }
        int seat = (ticket.coach - 1) * seatnum + (ticket.seat - 1);
        for (int k = ticket.departure; k < ticket.arrival; ++k) {
            seats.get(ticket.route - 1).get(seat).get(k - 1).set(false);
        }
        synchronized (this) {
            tickets.remove(ticket);
        }
        return true;
    }

    @Override
    public boolean buyTicketReplay(Ticket ticket) {
        // Useless
        return true;
    }

    @Override
    public boolean refundTicketReplay(Ticket ticket) {
        // Useless
        return true;
    }

}
