package ticketingsystem;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ThreadLocalRandom;

public class TicketingDS implements TicketingSystem {
    private int routenum;
    private int coachnum;
    private int seatnum;
    private final int tryTimes = 10;

    private AtomicLong tid;                                               // Next available tid.
    private HashSet<Ticket> tickets;                                      // Tickets sold.
    private ArrayList<ArrayList<AtomicLong>> seats;                       // Seat id to stations of every route.

    public TicketingDS(int _routenum, int _coachnum, int _seatnum, int _stationnum, int _threadnum) {
        routenum    = _routenum;
        coachnum    = _coachnum;
        seatnum     = _seatnum;

        tid         = new AtomicLong(0);
        tickets     = new HashSet<Ticket>();
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
        int i = -1;
        long bitmask = ((-1) >>> (departure - 1)) & ((-1) << (64 - arrival + 1));  
        for (int j = 0; j < tryTimes; ++j) {
            int seat = ThreadLocalRandom.current().nextInt(0, coachnum * seatnum);
            if (isAvailable(route, bitmask, seat)) {
                i = seat;
            }
        }
        if (i == -1) {
            for (i = 0; i < coachnum * seatnum; ++i) {
                if (isAvailable(route, bitmask, i)) {
                    break;
                }
            }
            if (i == coachnum * seatnum) {
                return null;
            }
        }

        /* Occupied the stations. */
        while (true) {
            long oldAva = seats.get(route - 1).get(i).get();
            long newAva = bitmask | oldAva; 
            if (seats.get(route - 1).get(i).compareAndSet(oldAva, newAva)) {
                break;
            }
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
        synchronized (this) {
            if (!tickets.contains(ticket)) {
                return false;
            }
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
