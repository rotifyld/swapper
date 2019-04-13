package swapper;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntUnaryOperator;

public class ReadersWriters {

    private static final int READING_ROOM_SIZE = 10;
    private static final int WRITERS = 3;
    private static final int READERS = 15;

    //
    private static final IntUnaryOperator nextReader = operand -> {
        operand++;
        return (operand == READING_ROOM_SIZE + 1) ? 1 : operand;
    };

    // positive numbers are readers places, zero stands for readers room lock
    private static final Swapper<Integer> swapper = new Swapper<>();

    static {
        try {
            for (int i = 0; i <= READING_ROOM_SIZE; i++) {
                swapper.swap(Collections.emptySet(), Collections.singleton(i));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            assert (false) : "Unable to initialize buffer (thread interrupted)";
        }
    }

    private static class Reader implements Runnable {

        static AtomicInteger readerRoomId = new AtomicInteger(1);

        private final int id;

        public Reader(int id) {
            this.id = id;
        }

        @Override
        public void run() {
            try {
                for (; ; ) {
                    ownBussines();

                    int seat = entrySection();

                    read(seat);

                    exitSection(seat);

                    remainderSection();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("A reader of id: " + id + " thread has been interrupted");
            }
        }

        private void exitSection(int seat) throws InterruptedException {
            // no need to obtain the lock, only free the seat
            swapper.swap(Collections.emptySet(), Collections.singleton(seat));
        }

        private int entrySection() throws InterruptedException {
            int seat = readerRoomId.getAndUpdate(nextReader);
            // obtain the lock, then obtain the seat and release the lock
            swapper.swap(Collections.singleton(0), Collections.emptySet());
            swapper.swap(Collections.singleton(seat), Collections.singleton(0));
            return seat;
        }

        private void ownBussines() {
        }

        private void read(int seat) {
            System.out.println("A reader of id " + id + " is reading at seat " + seat);
        }

        private void remainderSection() {
        }

    }

    private static class Writer implements Runnable {

        static Set<Integer> allSeats = new HashSet<>(READING_ROOM_SIZE);
        static Set<Integer> allSeatsPlusZero;

        static {
            for (int i = 1; i <= READING_ROOM_SIZE; i++) {
                allSeats.add(i);
            }

            allSeatsPlusZero = new HashSet<>(allSeats);
            allSeatsPlusZero.add(0);
        }

        private final int id;

        public Writer(int id) {
            this.id = id;
        }

        @Override
        public void run() {
            try {
                for (; ; ) {
                    ownBussines();

                    entrySection();

                    write();

                    exitSection();

                    remainderSection();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("A writer of id: " + id + " thread has been interrupted");
            }
        }

        private void entrySection() throws InterruptedException {
            swapper.swap(Collections.singleton(0), Collections.emptySet());
            swapper.swap(allSeats, Collections.emptySet());
        }

        private void exitSection() throws InterruptedException {
            swapper.swap(Collections.emptySet(), allSeatsPlusZero);
        }

        private void ownBussines() {
        }

        private void write() {
            System.out.println("A writer of id " + id + " is writing");
        }

        private void remainderSection() {
        }
    }

    public static void main(String[] args) {
        for (int i = 0; i < WRITERS; i++) {
            Thread t = new Thread(new Writer(i));
            t.start();
        }
        for (int i = 0; i < READERS; i++) {
            Thread t = new Thread(new Reader(i));
            t.start();
        }
    }
}