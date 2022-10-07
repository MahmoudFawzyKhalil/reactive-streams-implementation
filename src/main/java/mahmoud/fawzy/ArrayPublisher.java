package mahmoud.fawzy;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.atomic.AtomicLong;

public class ArrayPublisher<T> extends Flow<T> {
    private final T[] array;

    public ArrayPublisher(T[] array) {
        this.array = array;
    }

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        // Publisher is just a thin wrapper around subscription, subscription does all the work
        // So to debug Reactive Streams, put break points inside the for loop of subscription's request(..) method
        subscriber.onSubscribe(new ArraySubscription<>(subscriber, array));
    }

    public static class ArraySubscription<T> implements Subscription {
        private final Subscriber<? super T> subscriber;
        volatile boolean canceled;
        volatile boolean completed;

        final T[] array;

        int index; // Doesn't even need to be volatile because the requested field gets written after it and read before it, which is a volatile read and write so the happens-before guarantee means any changes to index are published to all other threads (acquire + release)

        AtomicLong requested; // We can use an atomic long field updater so that multiple publishers all use the same object, and just have primitive volatile long s

        public ArraySubscription(Subscriber<? super T> subscriber, T[] array) {
            this.subscriber = subscriber;
            this.array = array;
            requested = new AtomicLong();
        }

        // We can also do a fastpath and slowpath optimization if someone requests Long.MAX_VALUE

        // request method can be called from any number of threads concurrently, it SHOULD be non-blocking
        @Override
        public void request(long n) {
            if (n <= 0 && !canceled) {
                cancel(); // Cancel to prevent sending onError signal multiple times
                subscriber.onError(new IllegalArgumentException());
            }

            if (canceled) return;


//                long initialRequested = requested.getAndAdd(n); too simplistic, vulnerable to overflow

            long initialRequested;
            long newRequested;
            do {
                initialRequested = requested.get();

                if (initialRequested == Long.MAX_VALUE) {
                    // We are already sending data that would take 292 years to send
                    // So just return, let the work stealing continue
                    return;
                }

                newRequested = initialRequested + n;

                if (n <= 0) { // if overflow
                    n = Long.MAX_VALUE;
                }
            } while (!requested.compareAndSet(initialRequested, newRequested));


            // If there's already work in progress, return (WIP guard, only one thread may pass)
            if (initialRequested > 0) {
                return;
            }

            int sent = 0;
            while (true) {
                for (; sent < n && index < array.length; sent++) {
                    if (canceled) return;

                    T element = array[index];

                    if (element == null) {
                        subscriber.onError(new NullPointerException());
                        return;
                    }

                    subscriber.onNext(element);
                    // All signals must be serialized,
                    // you can't have two simultaneous invocations of onNext(), or any two signals concurrently
                    // otherwise the complexity of handling concurrency falls on the shoulder of the subscriber
                    index++;
                }

                if (canceled) return;

                if (index == array.length && !completed) {
                    subscriber.onComplete();
                    completed = true;
                    return;
                }

                long currentRequested = requested.addAndGet(-sent);
                if (currentRequested == 0) {
                    return; // otherwise, repeat while loop to steal work
                }

                n = currentRequested;
                sent = 0;

                // If this completes after an Add from a competing thread, no one will send data anymore!
                // Must add -sent, can't just set to 0 because you have to re-get the requested field otherwise
//                    requested.set(0);
//                    long currentRequested = requested.get();
//                    if (currentRequested == 0) {
//                        return;
//                    }
            }
        }

        // cancel method MUST be non-blocking, imagine this is a WebSocketPublisher and we want to close the WebSocket
        // a potentially long-running operation, so we want to perform it in a non-blocking manner
        // ( don't block the calling thread of cancel() )
        @Override
        public void cancel() {
            canceled = true;
        }
    }
}
