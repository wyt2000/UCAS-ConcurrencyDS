package ticketingsystem;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;

class BitMap {
    private final int LONG_BITS = Long.BYTES * 8;
    public int blockNum;
    public AtomicLongArray blocks;
    public BitMap(int capacity) {
        blockNum = (capacity + LONG_BITS - 1) / LONG_BITS;
        blocks = new AtomicLongArray(blockNum);
    }
    public void setBit(int pos) {
        int i = pos / LONG_BITS;
        long j = pos % LONG_BITS;
        while (true) {
            long oldValue = blocks.get(i);
            long newValue = oldValue | (1L << j);
            if (blocks.compareAndSet(i, oldValue, newValue)) {
                break;
            }
        }
    }
    public void clearBit(int pos) {
        int i = pos / LONG_BITS;
        long j = pos % LONG_BITS;
        while (true) {
            long oldValue = blocks.get(i);
            long newValue = oldValue & (~(1L << j));
            if (blocks.compareAndSet(i, oldValue, newValue)) {
                break;
            }
        }
    }
    public static int countOnes(long bits) {
        int cnt = 0;
        while (bits != 0) {
            bits &= (bits - 1);
            ++cnt;
        }
        return cnt;
    }
    public static int countOnes(ArrayList<Long> bitsArray) {
        int cnt = 0;
        for (long bits : bitsArray) {
            cnt += countOnes(bits);
        }
        return cnt;
    }
};

public class TicketingDS implements TicketingSystem {
    private final int LONG_BITS = Long.BYTES * 8;
    private int routenum;
    private int coachnum;
    private int seatnum;
    private int stationnum;
    private int totalseatnum;
    private int blockNum;

    private AtomicLong tid;                                               // Next available tid.
    private ConcurrentHashMap<Long, Ticket> tickets;                      // Tickets sold.
    private ArrayList<ArrayList<AtomicLong>> seats;                       // Seat id to stations of every route.
    private ArrayList<ArrayList<BitMap>> stations;

    public TicketingDS(int _routenum, int _coachnum, int _seatnum, int _stationnum, int _threadnum) {
        routenum        = _routenum;
        coachnum        = _coachnum;
        seatnum         = _seatnum;
        stationnum      = _stationnum;
        totalseatnum    = coachnum * seatnum;
        blockNum        = (totalseatnum + LONG_BITS - 1) / LONG_BITS;

        tid             = new AtomicLong(0);
        tickets         = new ConcurrentHashMap<Long, Ticket>();
        seats           = new ArrayList<ArrayList<AtomicLong>>();
        stations        = new ArrayList<ArrayList<BitMap>>();
        for (int i = 0; i < routenum; ++i) {
            ArrayList<AtomicLong> seatsPerCoach = new ArrayList<AtomicLong>();
            for (long j = 0; j < totalseatnum; ++j) {
                seatsPerCoach.add(new AtomicLong(0));
            }
            seats.add(seatsPerCoach);
            ArrayList<BitMap> stationsPerRoute = new ArrayList<BitMap>();
            for (int j = 0; j < stationnum; ++j) {
                stationsPerRoute.add(new BitMap(totalseatnum));
            }
            stations.add(stationsPerRoute);
        }
    }
    
    private boolean isAvailable(int route, long bitmask, int i) {
        if ((seats.get(route - 1).get(i).get() & bitmask) == 0) {
            return true;
        }
        return false;
    } 

    private ArrayList<Long> getSeatsOccupied(int route, int departure, int arrival) {
        ArrayList<Long> seats = new ArrayList<Long>();
        for (int j = 0; j < blockNum; ++j) {
            seats.add(0L);
        } 
        ArrayList<BitMap> stationsThisRoute = stations.get(route - 1);
        for (int i = departure; i < arrival; ++i) {
            for (int j = 0; j < blockNum; ++j) {
                seats.set(j, seats.get(j) | stationsThisRoute.get(i - 1).blocks.get(j));
            }
        }
        return seats;
    }

    @Override
    public Ticket buyTicket(String passenger, int route, int departure, int arrival) {
        /* Find a seat of route, which isn't occupied [departure, arrival). */
        long bitmask = ((-1) >>> (departure - 1)) & ((-1) << (LONG_BITS - arrival + 1));  
        int i;
        search: for (i = 0; i < totalseatnum; ++i) {
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
        if (i == totalseatnum) {
            return null;
        }
        for (int s = departure; s < arrival; ++s) {
            stations.get(route - 1).get(s - 1).setBit(i);
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
        return totalseatnum - BitMap.countOnes(getSeatsOccupied(route, departure, arrival));
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

        long bitmask = ((-1) >>> (ticket.departure - 1)) & ((-1) << (LONG_BITS - ticket.arrival + 1));  
        
        while (true) {
            long oldAva = seats.get(ticket.route - 1).get(seat).get();
            long newAva = (~bitmask) & oldAva;
            if (seats.get(ticket.route - 1).get(seat).compareAndSet(oldAva, newAva)) {
                break;
            }
        }

        for (int s = ticket.departure; s < ticket.arrival; ++s) {
            stations.get(ticket.route - 1).get(s - 1).clearBit(seat);
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
