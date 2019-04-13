package swapper;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntUnaryOperator;

public class ProducerConsumer {

    private static final int BUFFER_SIZE = 30;
    private static final int PRODUCERS = 10;
    private static final int CONSUMERS = 10;

    // an artificial class for product that's to be send
    private static class Product {}

    private static Product[] buffer = new Product[BUFFER_SIZE];
    // items in buffer are listed as 1, 2, ..., BUFFER_SIZE

    private static final IntUnaryOperator nextInBuffer = operand -> {
        operand++;
        return (operand == BUFFER_SIZE + 1) ? 1 : operand;
    };
    // positive numbers are available products, negative ones signify, that there is a free spot for object of a minus that id
    // zero is a buffer lock, so that only one process at the time can modify the array

    private static final Swapper<Integer> swapper = new Swapper<>();

    static {
        try {
            for (int i = 1; i <= BUFFER_SIZE; i++) {
                swapper.swap(Collections.emptySet(), Collections.singleton(-i));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            assert (false) : "Unable to initialize swapper (thread interrupted)";
        }
    }

    private static class Producer implements Runnable {

        static AtomicInteger bufferFront = new AtomicInteger(1);

        private final int id;

        public Producer(int id) {
            this.id = id;
        }

        @Override
        public void run() {
            try {
                for (; ; ) {
                    ownBussines();

                    Product product = produce();

                    int placeInBuffer = entrySection();

                    send(placeInBuffer, product);

                    exitSection(placeInBuffer);

                    remainderSection();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("A producer of id " + id + " thread has been interrupted");
            }
        }

        private Product produce() {
            return null;
        }

        private void ownBussines() {
        }

        private int entrySection() throws InterruptedException {
            int i = bufferFront.getAndUpdate(nextInBuffer);
            swapper.swap(Arrays.asList(0, -i), Collections.emptySet());
            return i;
        }

        private void send(int placeInBuffer, Product product) {
            buffer[placeInBuffer] = product;
        }

        private void exitSection(int placeInBuffer) throws InterruptedException {
            swapper.swap(Collections.emptySet(), Arrays.asList(0, placeInBuffer));
        }

        private void remainderSection() {
        }

    }

    private static class Consumer implements Runnable {

        static AtomicInteger bufferBack = new AtomicInteger(1);

        private final int id;

        public Consumer(int id) {
            this.id = id;
        }

        @Override
        public void run() {
            try {
                for (; ; ) {
                    ownBussines();

                    int expectedProduct = bufferBack.getAndUpdate(nextInBuffer);
                    swapper.swap(Collections.singleton(expectedProduct), Collections.singleton(-expectedProduct));

                    remainderSection();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("A consumer of id " + id + " thread has been interrupted");
            }
        }

        private void ownBussines() {
        }

        private void remainderSection() {
        }
    }

    public static void main(String[] args) {
        for (int i = 0; i < PRODUCERS; i++) {
            Thread t = new Thread(new Producer(i));
            t.start();
        }
        for (int i = 0; i < CONSUMERS; i++) {
            Thread t = new Thread(new Consumer(i));
            t.start();
        }
    }
}