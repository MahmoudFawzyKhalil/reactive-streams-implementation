package mahmoud.fawzy;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class ArrayPublisher<T> implements Publisher<T> {
    private final T[] array;
    private Subscriber<? super T> subscriber;

    public ArrayPublisher(T[] array) {
        this.array = array;
    }

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        this.subscriber = subscriber;
        this.subscriber.onSubscribe(new Subscription() {
            boolean completed;
            int index = 0;
            @Override
            public void request(long n) {
                for (int i = 0; i < n && index < array.length; i++, index++) {
                    subscriber.onNext(array[index]);
                }

                if (index == array.length && !completed){
                    subscriber.onComplete();
                    completed = true;
                }
            }

            @Override
            public void cancel() {

            }
        });
    }
}
