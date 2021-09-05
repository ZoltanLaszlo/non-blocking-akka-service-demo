package hu.upscale.akka.demo.guice;

import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.routing.DefaultResizer;
import akka.routing.RoundRobinPool;
import com.google.inject.Injector;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import java.util.function.Function;

/**
 * Utility class that helps create Akka actors via an actor producer ({@link GuiceActorProducer})
 *
 * @author László Zoltán
 */
public final class GuiceActorUtils {

    private static final String DYNAMIC_POOL = "dynamicPool";

    private static final String AKKA_ACTOR_DYNAMIC_POOL_LOWER_BOUND = "akka.actor.%s.lowerBound";
    private static final String AKKA_ACTOR_DYNAMIC_POOL_UPPER_BOUND = "akka.actor.%s.upperBound";
    private static final String AKKA_ACTOR_DYNAMIC_POOL_PRESSURE_THRESHOLD = "akka.actor.%s.pressureThreshold";
    private static final String AKKA_ACTOR_DYNAMIC_POOL_RAMPUP_RATE = "akka.actor.%s.rampupRate";
    private static final String AKKA_ACTOR_DYNAMIC_POOL_BACKOFF_THRESHOLD = "akka.actor.%s.backoffThreshold";
    private static final String AKKA_ACTOR_DYNAMIC_POOL_BACKOFF_RATE = "akka.actor.%s.backoffRate";
    private static final String AKKA_ACTOR_DYNAMIC_POOL_MESSAGES_PER_RESIZE = "akka.actor.%s.messagesPerResize";

    private GuiceActorUtils() {
        // Default constructor
    }

    /**
     * Get the {@link Injector} instance contained within the actor system.
     *
     * @param actorSystem Actor system whose injector is returned
     * @return {@link Injector} instance
     */
    public static Injector getInjector(ActorSystem actorSystem) {
        return GuiceExtensionProvider.PROVIDER.get(actorSystem).getInjector();
    }

    /**
     * Create {@link Props} properties for a single actor in the actor system.
     *
     * @param actorSystem Actor system to create the actor in
     * @param clazz The class of the actor to be created
     * @return {@link Props} of the actor
     */
    public static Props makeGuiceProps(ActorSystem actorSystem, Class<?> clazz) {
        return Props.create(GuiceActorProducer.class, getInjector(actorSystem), clazz);
    }

    /**
     * Create {@link Props} properties for a pool of actors in the actor system.
     *
     * @param actorSystem Actor system to create the actor in
     * @param clazz The class of the actor to be created
     * @param poolSize The size of the actor pool
     * @return {@link Props} of the actor
     */
    public static Props makeGuicePropsActorPool(ActorSystem actorSystem, Class<?> clazz, int poolSize) {
        return new RoundRobinPool(poolSize)
            .props(Props.create(GuiceActorProducer.class, getInjector(actorSystem), clazz));
    }

    /**
     * Creates a new dynamic {@link RoundRobinPool} with a {@link DefaultResizer} configure by the values from the {@link Config} defined by the poolConfig
     * parameter.
     *
     * @param config Configuration instance to access configurations.
     * @param poolConfig name of the pool config node in the <b>microservice:akka:actor</b> node.
     * @return the configure {@link RoundRobinPool}.
     */
    public static RoundRobinPool makeDynamicRoundRobinPool(Config config, String poolConfig) {
        int lowerBound = getIntActorConfigOrDefault(config, AKKA_ACTOR_DYNAMIC_POOL_LOWER_BOUND, poolConfig);
        int upperBound = getIntActorConfigOrDefault(config, AKKA_ACTOR_DYNAMIC_POOL_UPPER_BOUND, poolConfig);
        int pressureThreshold = getIntActorConfigOrDefault(config, AKKA_ACTOR_DYNAMIC_POOL_PRESSURE_THRESHOLD,
            poolConfig);
        double rampupRate = getDoubleActorConfigOrDefault(config, AKKA_ACTOR_DYNAMIC_POOL_RAMPUP_RATE,
            poolConfig);
        double backoffThreshold = getDoubleActorConfigOrDefault(config, AKKA_ACTOR_DYNAMIC_POOL_BACKOFF_THRESHOLD,
            poolConfig);
        double backoffRate = getDoubleActorConfigOrDefault(config, AKKA_ACTOR_DYNAMIC_POOL_BACKOFF_RATE,
            poolConfig);
        int messagesPerResize = getIntActorConfigOrDefault(config, AKKA_ACTOR_DYNAMIC_POOL_MESSAGES_PER_RESIZE,
            poolConfig);

        var pool = new RoundRobinPool(lowerBound);

        return pool.withResizer(
            new DefaultResizer(lowerBound, upperBound, pressureThreshold, rampupRate, backoffThreshold, backoffRate,
                messagesPerResize));
    }

    /**
     * Create {@link Props} properties for a pool of actors in the actor system. The pool of actors is dynamic, and uses {@link DefaultResizer}. Its
     * configuration values are taken from {@link Config}, either from the config values given via {@param poolConfig} or the default values.
     *
     * @param actorSystem Actor system to create the actor in
     * @param clazz The class of the actor to be created
     * @param poolConfig Name assigned to the config values of this actor pool in {@link Config}
     * @return {@link Props} of the actor
     */
    public static Props makeGuicePropsActorDynamicPool(Config config, ActorSystem actorSystem, Class<?> clazz,
        String poolConfig) {
        return makeDynamicRoundRobinPool(config, poolConfig)
            .props(Props.create(GuiceActorProducer.class, getInjector(actorSystem), clazz));
    }

    /**
     * Get int value from config.
     *
     * @param config config
     * @param key config key
     * @param actorNameInConfig actor name in config
     */
    private static int getIntActorConfigOrDefault(Config config, String key, String actorNameInConfig) {
        return getActorConfigOrDefault(config::getInt, String.format(key, actorNameInConfig), String.format(key, DYNAMIC_POOL));
    }

    /**
     * Get double value from config.
     *
     * @param config config
     * @param key config key
     * @param actorNameInConfig actor name in config
     */
    private static double getDoubleActorConfigOrDefault(Config config, String key, String actorNameInConfig) {
        return getActorConfigOrDefault(
            config::getDouble, String.format(key, actorNameInConfig), String.format(key, DYNAMIC_POOL));
    }

    /**
     * Return the actor config if exist; otherwise return default actor config
     *
     * @param configGetter config getter function
     * @param key key
     * @param defaultKey default key
     * @param <T> return type
     * @return config value
     */
    private static <T> T getActorConfigOrDefault(Function<String, T> configGetter, String key, String defaultKey) {
        try {
            return configGetter.apply(key);
        } catch (ConfigException e) {
            return configGetter.apply(defaultKey);
        }
    }

}
