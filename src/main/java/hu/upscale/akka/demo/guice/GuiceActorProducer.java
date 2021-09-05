package hu.upscale.akka.demo.guice;

import akka.actor.Actor;
import akka.actor.IndirectActorProducer;
import com.google.inject.Injector;

/**
 * Actor Producer class, that uses the actor system extended with an {@link Injector} to create the actors. This enables
 * the DI of actors in the system, which can be used to inject e.g. mock actors in unit tests.
 *
 * @author Bíró Gergő, Torma Balázs
 */
public class GuiceActorProducer implements IndirectActorProducer {

    private final Injector injector;
    private final Class<? extends Actor> actorClass;

    /**
     * Instantiates {@link GuiceActorProducer}. Called by the Akka actor system.
     *
     * @param injector Used by the producer to inject actors into the system
     * @param actorClass The class of actors that this producer can inject
     */
    public GuiceActorProducer(Injector injector, Class<? extends Actor> actorClass) {
        this.injector = injector;
        this.actorClass = actorClass;
    }

    /**
     * Produce an {@link Actor} instance
     *
     * @return {@link Actor} instance
     */
    @Override
    public Actor produce() {
        return injector.getInstance(actorClass);
    }

    /**
     * Return with the class of actors that this producer can create.
     *
     * @return Class of actors that this producer can create
     */
    @Override
    public Class<? extends Actor> actorClass() {
        return actorClass;
    }

}
