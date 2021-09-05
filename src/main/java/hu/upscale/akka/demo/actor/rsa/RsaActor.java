package hu.upscale.akka.demo.actor.rsa;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.routing.RoundRobinPool;
import hu.upscale.akka.demo.actor.rsa.RsaSignatureVerifierActor.RsaSignatureVerifierRequest;
import hu.upscale.akka.demo.actor.rsa.RsaSignerActor.RsaSignerRequest;

/**
 * @author László Zoltán
 */
public class RsaActor extends AbstractActor {

    public static final String SIGNATURE_ALGORITHM = "SHA512withRSA";
    public static final String RSA_KEY_FACTORY_ALGORITHM = "RSA";

    private static final int ACTOR_POOL_SIZE = 16;

    public static Props props() {
        return Props.create(RsaActor.class, RsaActor::new);
    }

    private final ActorRef rsaSignerActor = getContext().actorOf(new RoundRobinPool(ACTOR_POOL_SIZE).props(RsaSignerActor.props()));
    private final ActorRef rsaSignatureVerifierActor = getContext().actorOf(new RoundRobinPool(ACTOR_POOL_SIZE).props(RsaSignatureVerifierActor.props()));

    @Override
    public Receive createReceive() {
        return receiveBuilder()
            .match(RsaSignerRequest.class, rsaSignerRequest ->
                rsaSignerActor.forward(rsaSignerRequest, getContext()))
            .match(RsaSignatureVerifierRequest.class, rsaSignatureVerifierRequest ->
                rsaSignatureVerifierActor.forward(rsaSignatureVerifierRequest, getContext()))
            .build();
    }
}
