package hu.upscale.akka.demo.guice;

import akka.actor.Extension;
import com.google.inject.Injector;

/**
 * Akka actor system extension, that includes an {@link Injector} instance into the system.
 *
 * @author Bíró Gergő, Torma Balázs
 */
public class GuiceExtension implements Extension {

    private Injector injector;

    /**
     * Get {@link Injector} instance
     *
     * @return {@link Injector} instance
     */
    public Injector getInjector() {
        return injector;
    }

    /**
     * Set {@link Injector} instance
     *
     * @param injector {@link Injector} instance
     */
    public void setInjector(Injector injector) {
        this.injector = injector;
    }
}
