package mahmoud.fawzy;

import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.function.Function;

public class MapProcessor<IN, OUT> implements Processor<IN, OUT> {


    private final Publisher<? extends IN> upstreamPublisher;
    private final Function<IN, OUT> mapper;
    private Subscriber<? super OUT> downstreamSubscriber;


    public MapProcessor(Publisher<? extends IN> upstreamPublisher, Function<IN, OUT> mapper) {
        this.upstreamPublisher = upstreamPublisher;
        this.mapper = mapper;
    }

    // A Processor is a Publisher to its downstream Subscriber
    @Override
    public void subscribe(Subscriber<? super OUT> downstreamSubscriber) {
        // When someone subscribers to the processor, the processor should subscribe to its upstream Publisher
        this.downstreamSubscriber = downstreamSubscriber;
        this.upstreamPublisher.subscribe(this);
    }

    // A Processor is a Subscriber to its upstream Publisher
    @Override
    public void onSubscribe(Subscription subscription) {
        // Forward signal to downstream Subscriber, then the downstream subscriber will start to request(n), which is propagated through the subscription object
        // Then the upstream Publisher will call onNext() on its Subscriber, which is this Processor
        downstreamSubscriber.onSubscribe(subscription);
    }

    @Override
    public void onNext(IN in) {
        // Transform signal and forward it to downstream Subscriber
        downstreamSubscriber.onNext(mapper.apply(in));
    }

    @Override
    public void onError(Throwable throwable) {
        // Forward signal to downstream Subscriber
        downstreamSubscriber.onError(throwable);
    }

    @Override
    public void onComplete() {
        // Forward signal to downstream Subscriber
        downstreamSubscriber.onComplete();
    }
}
