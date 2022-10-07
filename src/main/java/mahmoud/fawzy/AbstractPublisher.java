package mahmoud.fawzy;

import org.reactivestreams.Publisher;

import java.util.function.Function;

public abstract class AbstractPublisher<IN> implements Publisher<IN> {

    public <OUT> Publisher<OUT> map(Function<IN, OUT> mapper) {
        return new MapProcessor<>(this, mapper);
    }
}
