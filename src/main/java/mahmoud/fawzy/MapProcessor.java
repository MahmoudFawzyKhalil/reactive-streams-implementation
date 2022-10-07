package mahmoud.fawzy;

import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.Objects;
import java.util.function.Function;

public class MapProcessor<IN, OUT> implements Processor<IN, OUT>, Subscription {


    private final Publisher<? extends IN> upstreamPublisher;
    private final Function<IN, OUT> mapper;
    private Subscriber<? super OUT> downstreamSubscriber;
    private boolean terminated;
    private Subscription subscriptionToUpstreamPublisher;


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
        this.subscriptionToUpstreamPublisher = subscription;
        downstreamSubscriber.onSubscribe(this);
    }

    @Override
    public void onNext(IN in) {

        // Transform signal and forward it to downstream Subscriber

        if (terminated) return; // Protection against asynchronous cancellation

        OUT apply;
        try {
            Objects.requireNonNull(apply = mapper.apply(in));
        } catch (Throwable t) {
            cancel(); // Cancel subscription upstream, cancellation is asynchronous potentially
            onError(t); // Signal error downstream
            return;
        }

        downstreamSubscriber.onNext(apply);
    }

    @Override
    public void onError(Throwable throwable) {
        if (terminated) return;

        // Forward signal to downstream Subscriber
        this.terminated = true;
        downstreamSubscriber.onError(throwable);
    }

    @Override
    public void onComplete() {
        if (terminated) return;

        // Forward signal to downstream Subscriber
        this.terminated = true;
        downstreamSubscriber.onComplete();
    }

    // Make ourselves a subscription in order to avoid creating a new subscription object
    @Override
    public void request(long l) {
        if (terminated) return;

        this.subscriptionToUpstreamPublisher.request(l);
    }

    @Override
    public void cancel() {

        this.subscriptionToUpstreamPublisher.cancel();
    }
}
