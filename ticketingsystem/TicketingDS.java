package ticketingsystem;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.Arrays;

class BitMap {
    class TimeStampedLong {
        long value;        
        long timeStamp;
        public TimeStampedLong(long _value, long _timeStamp) {
            value = _value;
            timeStamp = _timeStamp;
        }
        @Override
        public int hashCode() {
            return Objects.hash(value, timeStamp);
        }
        @Override
        public boolean equals(Object object) {
            TimeStampedLong t = (TimeStampedLong) object;
            if (t == null) return false;
            if (value != t.value) return false;
            if (timeStamp != t.timeStamp) return false;
            return true;
        }
    }
    private static final int LONG_BITS = Long.BYTES * 8;
    public int blockNum;
    public AtomicLongArray blocks;
    public AtomicLongArray timeStamps;
    public BitMap(int capacity) {
        blockNum = (capacity + LONG_BITS - 1) / LONG_BITS;
        blocks = new AtomicLongArray(blockNum);
        timeStamps = new AtomicLongArray(blockNum);
    }
    public void setBit(int pos) {
        int i = pos / LONG_BITS;
        long j = pos % LONG_BITS;
        while (true) {
            long oldValue = blocks.get(i);
            long newValue = oldValue | (1L << j);
            if (blocks.compareAndSet(i, oldValue, newValue)) {
                timeStamps.getAndIncrement(i);
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
                timeStamps.getAndIncrement(i);
                break;
            }
        }
    }
    private TimeStampedLong[] collect() {
        TimeStampedLong[] copy = new TimeStampedLong[blockNum];
        for (int i = 0; i < blockNum; ++i) {
            copy[i] = new TimeStampedLong(blocks.get(i), timeStamps.get(i));
        }
        return copy;
    }
    public long[] scan() {
        TimeStampedLong[] oldCopy = collect();
        TimeStampedLong[] newCopy;
        while (true) {
            newCopy = collect();
            if (Arrays.equals(oldCopy, newCopy)) {
                break;
            }
            oldCopy = newCopy;
        }
        long[] ans = new long[newCopy.length];
        for (int i = 0; i < newCopy.length; ++i) {
            ans[i] = newCopy[i].value;
        }
        return ans;
    }
    public static int countOnes(long[] bitsArray) {
        int cnt = 0;
        for (long bits : bitsArray) {
            cnt += Long.bitCount(bits); 
        }
        return cnt;
    }
    public static int findFirstZero(long[] bitsArray) {
        int ans = Integer.MAX_VALUE;
        for (int blockId = 0; blockId < bitsArray.length; ++blockId) {
            int pos = Long.numberOfTrailingZeros(~ bitsArray[blockId]);
            if (pos < LONG_BITS) {
                ans = blockId * LONG_BITS + pos;
                break;
            }
        }
        return ans;
    }
};

public class TicketingDS implements TicketingSystem {
    private static final int LONG_BITS = Long.BYTES * 8;
    private int routenum;
    private int coachnum;
    private int seatnum;
    private int stationnum;
    private int totalseatnum;
    private int blockNum;

    private AtomicLong tid;                                               // Next available tid.
    private ConcurrentHashMap<Long, Ticket> tickets;                      // Tickets sold.
    private AtomicLong[][] seats;                       // Seat id to stations of every route.
    private BitMap[][] stations;

    public TicketingDS(int _routenum, int _coachnum, int _seatnum, int _stationnum, int _threadnum) {
        routenum        = _routenum;
        coachnum        = _coachnum;
        seatnum         = _seatnum;
        stationnum      = _stationnum;
        totalseatnum    = coachnum * seatnum;
        blockNum        = (totalseatnum + LONG_BITS - 1) / LONG_BITS;

        tid             = new AtomicLong(0);
        tickets         = new ConcurrentHashMap<Long, Ticket>();
        seats           = new AtomicLong[routenum][totalseatnum];
        stations        = new BitMap[routenum][stationnum];
        for (int i = 0; i < routenum; ++i) {
            for (int j = 0; j < totalseatnum; ++j) {
                seats[i][j] = new AtomicLong(0);
            }
            for (int j = 0; j < stationnum; ++j) {
                stations[i][j] = new BitMap(totalseatnum);
            }
        }
    }
    
    private boolean isAvailable(int route, long bitmask, int i) {
        if ((seats[route - 1][i].get() & bitmask) == 0) {
            return true;
        }
        return false;
    } 

    @Override
    public Ticket buyTicket(String passenger, int route, int departure, int arrival) {
        /* Find a seat of route, which isn't occupied [departure, arrival). */
        long bitmask = ((-1) >>> (departure - 1)) & ((-1) << (LONG_BITS - arrival + 1));  
        int i;
        search: while (true) {
            long[] seatVector = new long[blockNum];
            for (int s = departure; s < arrival; ++s) {
                for (int j = 0; j < blockNum; ++j) {
                    seatVector[j] |= stations[route - 1][s - 1].blocks.get(j);
                }
            }
            i = BitMap.findFirstZero(seatVector);
            if (i >= totalseatnum) {
                return null;
            }
            /* Occupy the stations. */
            while (isAvailable(route, bitmask, i)) {
                long oldValue = seats[route - 1][i].get();
                long newValue = bitmask | oldValue; 
                /* If no modified, occupy successfully, otherwise test again. */
                if (seats[route - 1][i].compareAndSet(oldValue, newValue)) {
                    break search;
                }
            }
        }
        for (int s = departure; s < arrival; ++s) {
            stations[route - 1][s - 1].setBit(i);
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
        long[] seatVector = new long[blockNum];
        for (int i = departure; i < arrival; ++i) {
            long[] snapshot = stations[route - 1][i - 1].scan();
            for (int j = 0; j < blockNum; ++j) {
                seatVector[j] |= snapshot[j];
            }
        }
        return totalseatnum - BitMap.countOnes(seatVector);
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
            long oldValue = seats[ticket.route - 1][seat].get();
            long newValue = (~bitmask) & oldValue;
            if (seats[ticket.route - 1][seat].compareAndSet(oldValue, newValue)) {
                break;
            }
        }

        for (int s = ticket.departure; s < ticket.arrival; ++s) {
            stations[ticket.route - 1][s - 1].clearBit(seat);
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
