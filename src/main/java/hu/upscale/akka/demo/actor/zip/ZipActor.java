package hu.upscale.akka.demo.actor.zip;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.routing.RoundRobinPool;
import hu.upscale.akka.demo.actor.zip.ZipCompressorActor.ZipCompressorRequest;
import hu.upscale.akka.demo.actor.zip.ZipDecompressorActor.ZipDecompressorRequest;

/**
 * @author László Zoltán
 */
public class ZipActor extends AbstractActor {

    public static final int KB_IN_BYTES = 1_024;

    private static final int ACTOR_POOL_SIZE = 16;

    public static Props props() {
        return Props.create(ZipActor.class, ZipActor::new);
    }

    private final ActorRef zipCompressorActor = getContext().actorOf(new RoundRobinPool(ACTOR_POOL_SIZE).props(ZipCompressorActor.props()));
    private final ActorRef zipDecompressorActor = getContext().actorOf(new RoundRobinPool(ACTOR_POOL_SIZE).props(ZipDecompressorActor.props()));

    @Override
    public Receive createReceive() {
        return receiveBuilder()
            .match(ZipCompressorRequest.class, zipCompressorRequest ->
                zipCompressorActor.forward(zipCompressorRequest, getContext()))
            .match(ZipDecompressorRequest.class, zipDecompressorRequest ->
                zipDecompressorActor.forward(zipDecompressorRequest, getContext()))
            .build();
    }
}
