package hu.upscale.akka.demo;

import akka.Done;
import akka.actor.ActorSystem;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.server.Route;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.typesafe.config.Config;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author László Zoltán
 */
@Singleton
public class AkkaHttpServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(AkkaHttpServer.class);

    private static final String DEFAULT_INTERFACE_CONFIG_KEY = "akka.http.server.default-interface";
    private static final String DEFAULT_HTTP_PORT_CONFIG_KEY = "akka.http.server.default-http-port";
    private static final String DEFAULT_HTTPS_PORT_CONFIG_KEY = "akka.http.server.default-https-port";

    private final Config config;
    private final ActorSystem actorSystem;
    private final Http http;
    private final Route serverRoute;

    private final AtomicReference<ServerBinding> serverBindingAtomicReference = new AtomicReference<>();

    @Inject
    public AkkaHttpServer(Config config, ActorSystem actorSystem, Http http, Route serverRoute) {
        this.config = config;
        this.actorSystem = actorSystem;
        this.http = http;
        this.serverRoute = serverRoute;
    }

    public void start() {
        http.newServerAt(config.getString(DEFAULT_INTERFACE_CONFIG_KEY), config.getInt(DEFAULT_HTTP_PORT_CONFIG_KEY))
            .bind(serverRoute)
            .whenComplete((serverBinding, failure) -> {
                serverBindingAtomicReference.set(serverBinding);
                Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));

                if (failure != null) {
                    System.exit(15);
                }
            });

        actorSystem.getWhenTerminated().toCompletableFuture().join();
    }

    public void shutdown() {
        LOGGER.warn("Graceful shutdown initiated");
        unbindServerBinding()
            .handle((ignore, failure) -> terminateActorSystem())
            .thenCompose(Function.identity())
            .toCompletableFuture()
            .join();
    }

    private CompletionStage<Done> unbindServerBinding() {
        ServerBinding serverBinding = serverBindingAtomicReference.get();

        LOGGER.info("Unbinding Akka HTTP server binding");
        return (serverBinding == null ? CompletableFuture.completedStage(Done.done()) : serverBinding.unbind())
            .whenComplete((ignore, failure) -> {
                if (failure != null) {
                    LOGGER.error("Failed to gracefully unbind Akka HTTP server binding", failure);
                } else {
                    LOGGER.info("Akka HTTP server binding successfully unbound");
                }
            });
    }

    private CompletionStage<Done> terminateActorSystem() {
        LOGGER.info("Terminating Akka actor system");
        actorSystem.terminate();

        return actorSystem.getWhenTerminated()
            .thenApply(ignore -> Done.done())
            .whenComplete((ignore, failure) -> {
                if (failure != null) {
                    LOGGER.error("Failed to gracefully terminate Akka actor system", failure);
                } else {
                    LOGGER.info("Akka actor system successfully terminated");
                }
            });
    }
}
