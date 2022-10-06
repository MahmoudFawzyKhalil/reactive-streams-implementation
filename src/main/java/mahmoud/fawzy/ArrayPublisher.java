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
        subscriber.onSubscribe(new Subscription() {
            boolean completed;
            int index = 0;
            @Override
            public void request(long n) {
                for (int i = 0; i < n && index < array.length; i++, index++) {
                    T element = array[index];

                    if (element == null){
                        subscriber.onError(new NullPointerException());
                        return;
                    }

                    subscriber.onNext(element);
                }

                if (index == array.length && !completed){
                    subscriber.onComplete();
                    completed = true;
                }
            }

            @Override
            public void cancel() {
                // Empty
            }
        });
    }
}
