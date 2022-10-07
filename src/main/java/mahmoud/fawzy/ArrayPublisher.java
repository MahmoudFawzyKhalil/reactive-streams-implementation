package mahmoud.fawzy;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class ArrayPublisher<T> implements Publisher<T> {
    private final T[] array;

    public ArrayPublisher(T[] array) {
        this.array = array;
    }

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        // Publisher is just a thin wrapper around subscription, subscription does all the work
        // So to debug Reactive Streams, put break points inside the for loop of subscription's request(..) method
        subscriber.onSubscribe(new Subscription() {
            boolean canceled;
            boolean completed;
            AtomicInteger index = new AtomicInteger();

            AtomicLong requested = new AtomicLong();

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


                // If there's already work in progress return
                if (initialRequested > 0) {
                    return;
                }
                int sent = 0;
                while (true) {
                    for (; sent < requested.get() && index.get() < array.length; sent++) {
                        if (canceled) return;

                        T element = array[index.get()];

                        if (element == null) {
                            subscriber.onError(new NullPointerException());
                            return;
                        }

                        subscriber.onNext(element);
                        // All signals must be serialized,
                        // you can't have two simultaneous invocations of onNext(), or any two signals concurrently
                        // otherwise the complexity of handling concurrency falls on the shoulder of the subscriber
                        index.incrementAndGet();
                    }

                    if (canceled) return;

                    if (index.get() == array.length && !completed) {
                        subscriber.onComplete();
                        completed = true;
                        return;
                    }

                    long currentRequested = requested.addAndGet(-sent);
                    if (currentRequested == 0) {
                        return; // otherwise, repeat while loop to steal work
                    }
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
        });
    }
}
