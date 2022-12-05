package ticketingsystem;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class TicketingDS implements TicketingSystem {
    private int routenum;
    private int coachnum;
    private int seatnum;

    private AtomicLong tid;                                               // Next available tid.
    private ConcurrentHashMap<Long, Ticket> tickets;                                      // Tickets sold.
    private ArrayList<ArrayList<AtomicLong>> seats;                       // Seat id to stations of every route.

    public TicketingDS(int _routenum, int _coachnum, int _seatnum, int _stationnum, int _threadnum) {
        routenum    = _routenum;
        coachnum    = _coachnum;
        seatnum     = _seatnum;

        tid         = new AtomicLong(0);
        tickets     = new ConcurrentHashMap<Long, Ticket>();
        seats       = new ArrayList<ArrayList<AtomicLong>>();
        for (int i = 0; i < routenum; ++i) {
            ArrayList<AtomicLong> seatsPerCoach = new ArrayList<AtomicLong>();
            for (long j = 0; j < coachnum * seatnum; ++j) {
                seatsPerCoach.add(new AtomicLong(0));
            }
            seats.add(seatsPerCoach);
        }
    }
    
    private boolean isAvailable(int route, long bitmask, int i) {
        if ((seats.get(route - 1).get(i).get() & bitmask) == 0) {
            return true;
        }
        return false;
    } 

    @Override
    public Ticket buyTicket(String passenger, int route, int departure, int arrival) {
        /* Find a seat of route, which isn't occupied [departure, arrival). */
        long bitmask = ((-1) >>> (departure - 1)) & ((-1) << (64 - arrival + 1));  
        int i;
        search: for (i = 0; i < coachnum * seatnum; ++i) {
            /* Occupy the stations. */
            while (isAvailable(route, bitmask, i)) {
                long oldAva = seats.get(route - 1).get(i).get();
                long newAva = bitmask | oldAva; 
                /* If no modified, occupy successfully, otherwise test again. */
                if (seats.get(route - 1).get(i).compareAndSet(oldAva, newAva)) {
                    break search;
                }
            }
        }
        if (i == coachnum * seatnum) {
            return null;
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
        
        tickets.put(t.tid, t);
        return t;
    }

    @Override
    public int inquiry(int route, int departure, int arrival) {
        long bitmask = ((-1) >>> (departure - 1)) & ((-1) << (64 - arrival + 1));  
        int cnt = 0;
        int i;
        for (i = 0; i < coachnum * seatnum; ++i) {
            if (isAvailable(route, bitmask, i)) {
                ++cnt;
            }
        }
        return cnt;
    }

    @Override
    public boolean refundTicket(Ticket ticket) {
        if (!tickets.containsKey(ticket.tid)) {
            return false;
        }
        if (!ticket.equals(tickets.get(ticket.tid))) {
            return false;
        }
        int seat = (ticket.coach - 1) * seatnum + (ticket.seat - 1);

        long bitmask = ((-1) >>> (ticket.departure - 1)) & ((-1) << (64 - ticket.arrival + 1));  
        
        while (true) {
            long oldAva = seats.get(ticket.route - 1).get(seat).get();
            long newAva = (~bitmask) & oldAva;
            if (seats.get(ticket.route - 1).get(seat).compareAndSet(oldAva, newAva)) {
                break;
            }
        }

        tickets.remove(ticket.tid);
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
