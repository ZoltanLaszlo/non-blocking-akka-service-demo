package hu.upscale.akka.demo.route;

import static akka.http.javadsl.server.Directives.complete;
import static akka.http.javadsl.server.Directives.concat;
import static akka.http.javadsl.server.Directives.get;
import static akka.http.javadsl.server.Directives.handleExceptions;
import static akka.http.javadsl.server.Directives.pathEndOrSingleSlash;
import static akka.http.javadsl.server.Directives.pathPrefix;
import static akka.http.javadsl.server.PathMatchers.separateOnSlashes;
import static ch.megard.akka.http.cors.javadsl.CorsDirectives.cors;

import akka.http.javadsl.model.HttpMethods;
import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.server.ExceptionHandler;
import akka.http.javadsl.server.Route;
import ch.megard.akka.http.cors.javadsl.settings.CorsSettings;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.typesafe.config.Config;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author László Zoltán
 */
@Singleton
@RequiredArgsConstructor(onConstructor = @__({@Inject}))
public class ServerRoute {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerRoute.class);

    private static final String CONTEXT_PATH_CONFIG_KEY = "microservice.context-path";

    private final CorsSettings corsSettings = CorsSettings.defaultSettings()
        .withAllowedMethods(List.of(HttpMethods.DELETE, HttpMethods.GET, HttpMethods.POST, HttpMethods.PUT, HttpMethods.PATCH, HttpMethods.OPTIONS));

    private final Config config;
    private final StatementRoute statementRoute;

    private final ExceptionHandler generalExceptionHandler = ExceptionHandler.newBuilder()
        .matchAny(ex -> {
            LOGGER.error("Route processing failed", ex);
            return complete(StatusCodes.INTERNAL_SERVER_ERROR, ExceptionUtils.getStackTrace(ex));
        })
        .build();

    public Route getRoute() {
        return concat(
            pathPrefix("readiness", () -> pathEndOrSingleSlash(() -> get(() -> complete(StatusCodes.OK, Boolean.toString(true))))),
            pathPrefix("liveness", () -> pathEndOrSingleSlash(() -> get(() -> complete(StatusCodes.OK, Boolean.toString(true))))),
            cors(corsSettings, () ->
                pathPrefix(
                    separateOnSlashes(config.getString(CONTEXT_PATH_CONFIG_KEY)),
                    () -> handleExceptions(generalExceptionHandler,
                        () -> pathPrefix(separateOnSlashes("accounts/statements"), statementRoute::getPostAccountStatementRoute)
                    )
                )
            )
        );
    }

}
