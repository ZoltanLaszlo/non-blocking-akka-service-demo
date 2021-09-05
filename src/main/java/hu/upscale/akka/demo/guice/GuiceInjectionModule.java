package hu.upscale.akka.demo.guice;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.http.javadsl.Http;
import akka.http.javadsl.server.Route;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.microsoft.sqlserver.jdbc.SQLServerDataSource;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import hu.upscale.akka.demo.actor.AccountStatementGeneratorActor;
import hu.upscale.akka.demo.route.ServerRoute;
import hu.upscale.akka.demo.util.ObjectMapperProvider;
import java.util.concurrent.Executor;
import javax.sql.DataSource;

/**
 * @author László Zoltán
 */
public class GuiceInjectionModule extends AbstractModule {

    private static final String ACTOR_SYSTEM_NAME = "non-blocking-akka-service-demo";
    private static final String DB_CONNECTION_POOL_NAME = "non-blocking-akka-service-connection-pool";

    public static final String JDBC_EXECUTOR = "akka.actor.jdbc-dispatcher";

    private static final String DATASOURCE_URL_CONFIGURATION_KEY = "microservice.datasource.url";
    private static final String DATASOURCE_USER_CONFIGURATION_KEY = "microservice.datasource.user";
    private static final String DATASOURCE_PASSWORD_CONFIGURATION_KEY = "microservice.datasource.password";
    private static final String DATASOURCE_CONNECTION_POOL_MIN_SIZE_CONFIGURATION_KEY = "microservice.datasource.connection-pool-min-size";
    private static final String DATASOURCE_CONNECTION_POOL_MAX_SIZE_CONFIGURATION_KEY = "microservice.datasource.connection-poll-max-size";
    private static final String DATASOURCE_CONNECTION_TIMEOUT_CONFIGURATION_KEY = "microservice.datasource.connection-timeout";
    private static final String DATASOURCE_INITIALIZATION_FAIL_TIMEOUT_CONFIGURATION_KEY = "microservice.datasource.initialization-fail-timeout";
    private static final String DATASOURCE_CONNECTION_VALIDATION_TIMEOUT_CONFIGURATION_KEY = "microservice.datasource.connection-validation-timeout";
    private static final String DATASOURCE_CONNECTION_IDLE_TIMEOUT_CONFIGURATION_KEY = "microservice.datasource.connection-idle-timeout";
    private static final String DATASOURCE_CONNECTION_MAX_LIFETIME_CONFIGURATION_KEY = "microservice.datasource.connection-max-lifetime";
    private static final String DATASOURCE_THRESHOLD_CONFIGURATION_KEY = "microservice.datasource.connection-leak-detection-threshold";

    @Provides
    @Singleton
    public Config provideConfig() {
        return ConfigFactory.load();
    }

    @Provides
    @Singleton
    public ActorSystem provideActorSystem(Injector injector, Config config) {
        ActorSystem system = ActorSystem.create(ACTOR_SYSTEM_NAME, config);
        system.registerExtension(GuiceExtensionProvider.PROVIDER);
        GuiceExtension guiceExtension = GuiceExtensionProvider.PROVIDER.get(system);
        guiceExtension.setInjector(injector);

        return system;
    }

    @Provides
    @Singleton
    public Http provideHttp(ActorSystem actorSystem) {
        return Http.get(actorSystem);
    }

    @Provides
    @Singleton
    public ObjectMapper provideObjectMapper() {
        return ObjectMapperProvider.getObjectMapper();
    }

    @Provides
    @Singleton
    public Route provideServerRoute(ServerRoute serverRoute) {
        return serverRoute.getRoute();
    }

    @Provides
    @Singleton
    public DataSource provideDataSource(Config config, ActorSystem actorSystem) {
        String jdbcUrl = config.getString(DATASOURCE_URL_CONFIGURATION_KEY);

        SQLServerDataSource sqlServerDataSource = new SQLServerDataSource();
        sqlServerDataSource.setURL(jdbcUrl);
        sqlServerDataSource.setUser(config.getString(DATASOURCE_USER_CONFIGURATION_KEY));
        sqlServerDataSource.setPassword(config.getString(DATASOURCE_PASSWORD_CONFIGURATION_KEY));

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setDataSource(sqlServerDataSource);
        hikariConfig.setAutoCommit(false);
        hikariConfig.setMinimumIdle(config.getInt(DATASOURCE_CONNECTION_POOL_MIN_SIZE_CONFIGURATION_KEY));
        hikariConfig.setMaximumPoolSize(config.getInt(DATASOURCE_CONNECTION_POOL_MAX_SIZE_CONFIGURATION_KEY));
        hikariConfig.setRegisterMbeans(true);
        hikariConfig.setConnectionTimeout(config.getLong(DATASOURCE_CONNECTION_TIMEOUT_CONFIGURATION_KEY));
        hikariConfig.setInitializationFailTimeout(config.getLong(DATASOURCE_INITIALIZATION_FAIL_TIMEOUT_CONFIGURATION_KEY));
        hikariConfig.setValidationTimeout(config.getLong(DATASOURCE_CONNECTION_VALIDATION_TIMEOUT_CONFIGURATION_KEY));
        hikariConfig.setIdleTimeout(config.getLong(DATASOURCE_CONNECTION_IDLE_TIMEOUT_CONFIGURATION_KEY));
        hikariConfig.setMaxLifetime(config.getLong(DATASOURCE_CONNECTION_MAX_LIFETIME_CONFIGURATION_KEY));
        hikariConfig.setLeakDetectionThreshold(config.getLong(DATASOURCE_THRESHOLD_CONFIGURATION_KEY));
        hikariConfig.setPoolName(DB_CONNECTION_POOL_NAME);
        hikariConfig.setConnectionTestQuery("SELECT 1;");

        if (jdbcUrl.matches("^.*database(Name)?=\\w+(;\\w+=\\w+)*$")) {
            String[] properties = jdbcUrl.substring(jdbcUrl.indexOf(';')).split(";");
            for (String property : properties) {
                if (property.startsWith("database")) {
                    String databaseName = property.split("=")[1];
                    hikariConfig.setConnectionInitSql("USE " + databaseName);
                    break;
                }
            }
        }

        HikariDataSource hikariDataSource = new HikariDataSource(hikariConfig);
        actorSystem.registerOnTermination(hikariDataSource::close);

        return hikariDataSource;
    }

    @Provides
    @Singleton
    @Named(AccountStatementGeneratorActor.ACTOR_NAME)
    public ActorRef provideAccountStatementGeneratorActor(ActorSystem actorSystem, DataSource dataSource, @Named(JDBC_EXECUTOR) Executor jdbcExecutor) {
        return actorSystem.actorOf(AccountStatementGeneratorActor.props(dataSource, jdbcExecutor));
    }

    @Provides
    @Singleton
    @Named(JDBC_EXECUTOR)
    public Executor provideJdbcExecutor(ActorSystem actorSystem) {
        return actorSystem.dispatchers().lookup(JDBC_EXECUTOR);
    }

}
