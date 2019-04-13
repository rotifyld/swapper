package swapper;

import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Swapper<E> {


    private final Set<E> contents = new HashSet<>();        ///< Holds the contents of a swapper
    private final Lock lock = new ReentrantLock(); /// Lock synchronizing threads
    private final Map<Collection<E>, Freezer> waiting = new HashMap<>(); ///< Structure to coordinate waiting of threads

    /**
     * Class holding threads of the same removed collection waiting for missing elements
     */
    private class Freezer {
        private int waiting = 0;            ///< Number of waiting threads
        private final Condition condition;  ///< Conditional variable for threads to await on

        /**
         * Default constructor
         *
         * @param c - conditional variable on parent's class lock for threads to await on
         */
        public Freezer(Condition c) {
            this.condition = c;
        }

        /**
         * Freezes calling thread on a conditional variable and gives back the lock
         *
         * @throws InterruptedException - if thread was interrupted on conditional variable
         */
        public void freeze() throws InterruptedException {
            waiting++;
            try {
                condition.await();
            } finally {
                waiting--;
            }
        }

        /**
         * Unfreezes one of the waiting threads
         */
        public void unfreezeSingle() {
            condition.signal();
        }

        /**
         * Checks if there are any of threads waiting
         *
         * @return true if no thread is waiting, false otherwise
         */
        public boolean isEmpty() {
            return waiting == 0;
        }
    }

    /**
     * An empty constructor
     */
    public Swapper() {
    }

    /**
     * Suspends thread until all element in the collection removed are in the swapper. Then, atomically:
     * removes from the swapper all elements from the collection removed,
     * adds to swapper all elements from the collection added.
     * Adding currently existing elements have no effect.
     * Interrupting thread currently in swapper guarantees that it does not alter swapper and throws exception.
     *
     * @param removed - collection of elements to remove
     * @param added   - collection of elements to add
     * @throws InterruptedException - if thread was interrupted while anywhere in swap method
     */
    public void swap(Collection<E> removed, Collection<E> added) throws InterruptedException {

        lock.lock();
        if (Thread.currentThread().isInterrupted()) {
            lock.unlock();
            throw new InterruptedException();
        }

        while (!contents.containsAll(removed)) {
            if (!waiting.containsKey(removed)) {
                waiting.put(removed, new Freezer(lock.newCondition()));
            }

            try {
                waiting.get(removed).freeze();
            } finally {
                if (waiting.get(removed).isEmpty()) {
                    waiting.remove(removed);
                }
                // we don't need to unlock the lock, as exception is thrown only while thread is awaiting
            }
        }

        contents.removeAll(removed);
        contents.addAll(added);

        // if thread was interrupted before exiting, reverse the changes
        if (Thread.currentThread().isInterrupted()) {
            contents.removeAll(added);
            contents.addAll(removed);
            releaseNextWaiting();
            lock.unlock();
            throw new InterruptedException();
        }

        releaseNextWaiting();
        lock.unlock();
    }

    /**
     * After changes in swapper, checks if any of awaiting threads can now perform swapping
     * caution: This implementation of swapper (especially releaseNextWaiting() method, which calls collections with
     * earlier iterator/hashcode first) may starve some of the threads
     */
    private void releaseNextWaiting() {
        for (Collection<E> key : waiting.keySet()) {
            if (contents.containsAll(key)) {
                waiting.get(key).unfreezeSingle();
                break;
            }
        }
    }
}