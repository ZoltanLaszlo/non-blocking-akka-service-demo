package hu.upscale.akka.demo.route;

import static akka.http.javadsl.server.Directives.complete;
import static akka.http.javadsl.server.Directives.onComplete;
import static akka.http.javadsl.server.Directives.pathEndOrSingleSlash;
import static akka.http.javadsl.server.Directives.post;

import akka.actor.ActorRef;
import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.server.Directives;
import akka.http.javadsl.server.Route;
import akka.pattern.Patterns;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import hu.upscale.akka.demo.actor.AccountStatementGeneratorActor;
import hu.upscale.akka.demo.actor.AccountStatementGeneratorActor.AccountStatementGeneratorRequest;
import hu.upscale.akka.demo.actor.AccountStatementGeneratorActor.AccountStatementGeneratorResponse;
import hu.upscale.akka.demo.client.PostAccountStatementRequest;
import hu.upscale.akka.demo.client.PostAccountStatementResponse;
import java.time.Duration;
import java.util.concurrent.CompletionStage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author László Zoltán
 */
@Singleton
public class StatementRoute {

    private static final Logger LOGGER = LoggerFactory.getLogger(StatementRoute.class);

    private final ObjectMapper objectMapper;
    private final ActorRef accountStatementGeneratorActor;

    @Inject
    public StatementRoute(ObjectMapper objectMapper, @Named(AccountStatementGeneratorActor.ACTOR_NAME) ActorRef accountStatementGeneratorActor) {
        this.objectMapper = objectMapper;
        this.accountStatementGeneratorActor = accountStatementGeneratorActor;

    }

    public Route getPostAccountStatementRoute() {
        return pathEndOrSingleSlash(() -> post(this::createAccountStatement));
    }

    private Route createAccountStatement() {
        return Directives.entity(Jackson.unmarshaller(objectMapper, PostAccountStatementRequest.class), postAccountStatementRequest -> {
            LOGGER.info("Creating account statement from lastTransactionId: [{}]", postAccountStatementRequest.getLastTransactionId());

            CompletionStage<PostAccountStatementResponse> result = Patterns
                .ask(accountStatementGeneratorActor,
                    AccountStatementGeneratorRequest.builder().lastTransactionId(postAccountStatementRequest.getLastTransactionId()).build(),
                    Duration.ofSeconds(90)
                )
                .thenApply(AccountStatementGeneratorResponse.class::cast)
                .thenApply(AccountStatementGeneratorResponse::getAccountStatementId)
                .thenApply(accountStatementId -> PostAccountStatementResponse.builder().accountStatementId(accountStatementId).build())
                .whenComplete((postAccountStatementResponse, failure) -> {
                    if (failure != null) {
                        LOGGER.error("Failed to create account statement", failure);
                    } else {
                        LOGGER.info("Account statement successfully created - postAccountStatementResponse: [{}]", postAccountStatementResponse);
                    }
                });

            return onComplete(
                result,
                postAccountStatementResponse -> complete(StatusCodes.CREATED, postAccountStatementResponse.get(), Jackson.marshaller(objectMapper))
            );
        });
    }

}
