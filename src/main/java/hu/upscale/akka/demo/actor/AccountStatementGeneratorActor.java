package hu.upscale.akka.demo.actor;

import akka.NotUsed;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.pattern.Patterns;
import akka.stream.OverflowStrategy;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import akka.stream.javadsl.SourceQueueWithComplete;
import akka.util.Timeout;
import hu.upscale.akka.demo.actor.AccountStatementArchiverActor.AccountStatementArchiverRequest;
import hu.upscale.akka.demo.actor.AccountStatementArchiverActor.AccountStatementArchiverResponse;
import hu.upscale.akka.demo.entity.FinancialTransaction;
import hu.upscale.akka.demo.exception.DbException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import lombok.Builder;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author László Zoltán
 */
public class AccountStatementGeneratorActor extends AbstractActor {

    public static final String ACTOR_NAME = "account-statement-generator-actor";

    private static final Logger LOGGER = LoggerFactory.getLogger(AccountStatementGeneratorActor.class);

    private static final int PARALLELISM = 8;

    private static final String SELECT_FINANCIAL_TRANSACTION_BY_TRANSACTION_ID
        = "SELECT TransactionId, PreviousTransactionId, Data FROM demo.FinancialTransaction WHERE TransactionId = ?";

    private static final int LAST_TRANSACTION_NUMBER_ON_ACCOUNT_STATEMENT = 1;

    private final DataSource dataSource;
    private final Executor jdbcExecutor;
    private final ActorRef accountStatementArchiverActor;

    public static Props props(DataSource dataSource, Executor jdbcExecutor) {
        return Props.create(AccountStatementGeneratorActor.class, () -> new AccountStatementGeneratorActor(dataSource, jdbcExecutor));
    }

    public AccountStatementGeneratorActor(DataSource dataSource, Executor jdbcExecutor) {
        this.dataSource = dataSource;
        this.jdbcExecutor = jdbcExecutor;
        accountStatementArchiverActor = getContext().actorOf(AccountStatementArchiverActor.props(dataSource, jdbcExecutor));
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
            .match(AccountStatementGeneratorRequest.class, accountStatementGeneratorRequest ->
                Patterns.pipe(generateAccountStatement(accountStatementGeneratorRequest), getContext().getDispatcher())
                    .to(getSender(), getSelf())
            )
            .build();
    }

    private CompletionStage<AccountStatementGeneratorResponse> generateAccountStatement(AccountStatementGeneratorRequest accountStatementGeneratorRequest) {
        UUID accountStatementId = UUID.randomUUID();

        LOGGER.info("Generating account statement - accountStatementId: [{}]", accountStatementId);

        SourceQueueWithComplete<FinancialTransaction> sourceQueue = Source.<FinancialTransaction>queue(PARALLELISM, OverflowStrategy.backpressure())
            .via(archiveFinancialTransaction(accountStatementId))
            .to(Sink.ignore())
            .run(getContext().getSystem());

        CompletionStage<Void> financialTransactionLoad = loadFinancialTransactionChain(sourceQueue,
            accountStatementGeneratorRequest.getLastTransactionId().toString());

        return financialTransactionLoad
            .thenCompose(ignore -> sourceQueue.watchCompletion())
            .thenApply(ignore -> AccountStatementGeneratorResponse.builder().accountStatementId(accountStatementId).build());
    }

    private CompletionStage<Void> loadFinancialTransactionChain(SourceQueueWithComplete<FinancialTransaction> sourceQueue, String lastTransactionId) {
        return findFinancialTransaction(lastTransactionId).thenCompose(financialTransaction ->
            sourceQueue.offer(financialTransaction)
                .thenCompose(ignore ->
                    Optional.ofNullable(financialTransaction.getPreviousTransactionId())
                        .map(previousTransactionId -> loadFinancialTransactionChain(sourceQueue, previousTransactionId))
                        .orElseGet(() -> CompletableFuture.completedFuture(null).thenRun(sourceQueue::complete))
                )
        );
    }

    private CompletionStage<FinancialTransaction> findFinancialTransaction(String transactionId) {
        return CompletableFuture.supplyAsync(() -> {
            try (
                Connection connection = dataSource.getConnection();
                PreparedStatement preparedStatement = connection.prepareStatement(SELECT_FINANCIAL_TRANSACTION_BY_TRANSACTION_ID)
            ) {
                connection.setAutoCommit(true);
                connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
                preparedStatement.setString(1, transactionId);
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    if (resultSet.next()) {
                        return resultSetToFinancialTransaction(resultSet);
                    }

                    throw new SQLException("No financial transaction found for id: " + transactionId);
                }
            } catch (SQLException e) {
                throw new DbException("Failed to select financial transaction by id", e);
            }
        }, jdbcExecutor);
    }

    private Flow<FinancialTransaction, UUID, NotUsed> archiveFinancialTransaction(UUID accountStatementId) {
        AtomicInteger transactionNumberCounter = new AtomicInteger(LAST_TRANSACTION_NUMBER_ON_ACCOUNT_STATEMENT);
        return Flow.<FinancialTransaction>create()
            .map(financialTransaction -> AccountStatementArchiverRequest.builder()
                .accountStatementId(accountStatementId)
                .financialTransaction(financialTransaction)
                .transactionNumber(transactionNumberCounter.getAndIncrement())
                .build()
            )
            .ask(PARALLELISM, accountStatementArchiverActor, AccountStatementArchiverResponse.class, Timeout.create(Duration.ofMinutes(1)))
            .map(AccountStatementArchiverResponse::getAccountStatementId);
    }

    private FinancialTransaction resultSetToFinancialTransaction(ResultSet resultSet) throws SQLException {
        FinancialTransaction financialTransaction = FinancialTransaction.builder()
            .transactionId(resultSet.getString(FinancialTransaction.TRANSACTION_ID_COLUMN_NAME))
            .previousTransactionId(resultSet.getString(FinancialTransaction.PREVIOUS_TRANSACTION_ID_COLUMN_NAME))
            .data(resultSet.getBytes(FinancialTransaction.DATA_COLUMN_NAME))
            .build();

        LOGGER.info("Financial transaction found - transactionId: [{}]", financialTransaction.getTransactionId());

        return financialTransaction;
    }

    @Data
    @Builder
    public static final class AccountStatementGeneratorRequest {

        private final UUID lastTransactionId;

    }

    @Data
    @Builder
    public static final class AccountStatementGeneratorResponse {

        private final UUID accountStatementId;

    }
}
