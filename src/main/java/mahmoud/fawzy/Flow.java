package mahmoud.fawzy;

import org.reactivestreams.Publisher;

import java.util.function.Function;

public abstract class Flow<T> implements Publisher<T> {

    public <R> Flow<R> map(Function<T, R> mapper) {
        return new MapProcessor<>(this, mapper);
    }

    public static <T> Flow<T> fromArray(T... array) {
        return new ArrayPublisher<>(array);
    }
}
