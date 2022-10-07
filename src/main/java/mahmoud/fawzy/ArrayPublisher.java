package mahmoud.fawzy;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

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
            int index = 0;

            long requested = 0;

            @Override
            public void request(long n) {
                if (canceled) return;

                if (n <= 0 && !canceled) {
                    cancel(); // Cancel to prevent sending onError signal multiple times
                    subscriber.onError(new IllegalArgumentException());
                }

                // If there's work in progress
                if (requested > 0) {
                    // Increment the work in progress and return to avoid StackOverflowError
                    requested += n;
                    return;
                } else {
                    // Add to the work in progress and start doing it
                    requested += n;
                }


                for (int i = 0; i < requested && index < array.length; i++) {
                    if (canceled) return;

                    T element = array[index];

                    if (element == null) {
                        subscriber.onError(new NullPointerException());
                        return;
                    }

                    subscriber.onNext(element);
                    index++;
                }

                if (canceled) return;

                // Finished sending, reset requested
                requested = 0;

                if (index == array.length && !completed) {
                    subscriber.onComplete();
                    completed = true;
                }
            }

            @Override
            public void cancel() {
                canceled = true;
            }
        });
    }
}
