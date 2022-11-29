package ticketingsystem;

import java.util.Vector;
import java.util.HashSet;

public class TicketingDS implements TicketingSystem {
    private int routenum;
    private int coachnum;
    private int seatnum;
    private int stationnum;
    private int threadnum;

    private long tid;                                               // Next available tid.
    private HashSet<Ticket> tickets;                                // Tickets sold.
    private Vector<Vector<Vector<Boolean>>> seats;                  // Seat id to stations of every route.

    public TicketingDS(int _routenum, int _coachnum, int _seatnum, int _stationnum, int _threadnum) {
        routenum    = _routenum;
        coachnum    = _coachnum;
        seatnum     = _seatnum;
        stationnum  = _stationnum;
        threadnum   = _threadnum;

        tid         = 0;
        tickets     = new HashSet<Ticket>();
        seats       = new Vector<Vector<Vector<Boolean>>>();
        for (int i = 0; i < routenum; ++i) {
            Vector<Vector<Boolean>> seatsPerCoach = new Vector<Vector<Boolean>>();
            for (long j = 0; j < coachnum * seatnum; ++j) {
                Vector<Boolean> stations = new Vector<Boolean>();
                for (int k = 0; k < stationnum; ++k) {
                    stations.add(false);
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
                if (seats.get(route - 1).get(i).get(j - 1)) {
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
            seats.get(route - 1).get(i).set(j - 1, true);
        }

        int coach = i / seatnum + 1; 
        int seat = i % seatnum + 1; 

        Ticket t = new Ticket();

        t.tid = tid;
        tid = tid + 1;

        t.passenger = passenger;
        t.route = route;
        t.coach = coach;
        t.seat = seat;
        t.departure = departure;
        t.arrival = arrival;
        tickets.add(t);
        return t;
    }

    @Override
    public int inquiry(int route, int departure, int arrival) {
        int cnt = 0;
        int i;
        for (i = 0; i < coachnum * seatnum; ++i) {
            int j;
            for (j = departure; j < arrival; ++j) {
                if (seats.get(route - 1).get(i).get(j - 1)) {
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
        if (!tickets.contains(ticket)) {
            for (Ticket t : tickets) {
                System.out.println(t);
            }
            return false;
        }
        int seat = (ticket.coach - 1) * seatnum + (ticket.seat - 1);
        for (int k = ticket.departure; k < ticket.arrival; ++k) {
            seats.get(ticket.route - 1).get(seat).set(k - 1, false);
        }
        tickets.remove(ticket);
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
