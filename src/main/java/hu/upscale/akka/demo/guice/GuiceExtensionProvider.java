package hu.upscale.akka.demo.guice;

import akka.actor.AbstractExtensionId;
import akka.actor.ExtendedActorSystem;
import akka.actor.Extension;
import akka.actor.ExtensionId;
import akka.actor.ExtensionIdProvider;

/**
 * Class required in Akka for creating an actor system extension with {@link GuiceExtension}.
 *
 * @author Bíró Gergő, Torma Balázs
 */
public class GuiceExtensionProvider extends AbstractExtensionId<GuiceExtension> implements ExtensionIdProvider {

    public static final GuiceExtensionProvider PROVIDER = new GuiceExtensionProvider();

    /**
     * Creates the Akka actor system extension.
     *
     * @param system the system to be extended
     * @return {@link GuiceExtension} instance
     */
    @Override
    public GuiceExtension createExtension(ExtendedActorSystem system) {
        return new GuiceExtension();
    }

    /**
     * Returns the canonical ExtensionId for this Extension
     *
     * @return {@link ExtensionId} instance
     */
    @Override
    public ExtensionId<? extends Extension> lookup() {
        return PROVIDER;
    }
}
