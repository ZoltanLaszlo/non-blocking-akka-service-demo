package hu.upscale.akka.demo.actor;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.pattern.Patterns;
import hu.upscale.akka.demo.actor.rsa.RsaActor;
import hu.upscale.akka.demo.actor.rsa.RsaSignerActor.RsaSignerRequest;
import hu.upscale.akka.demo.actor.rsa.RsaSignerActor.RsaSignerResponse;
import hu.upscale.akka.demo.actor.zip.ZipActor;
import hu.upscale.akka.demo.actor.zip.ZipCompressorActor.ZipCompressorRequest;
import hu.upscale.akka.demo.actor.zip.ZipCompressorActor.ZipCompressorResponse;
import hu.upscale.akka.demo.entity.ArchiveFinancialTransaction;
import hu.upscale.akka.demo.entity.FinancialTransaction;
import hu.upscale.akka.demo.exception.DbException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import javax.sql.DataSource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author László Zoltán
 */
@AllArgsConstructor
public class AccountStatementArchiverActor extends AbstractActor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccountStatementArchiverActor.class);

    private static final String DELETE_FINANCIAL_TRANSACTION_BY_TRANSACTION_ID
        = "DELETE FROM demo.FinancialTransaction WHERE TransactionId = ?";
    private static final String INSERT_ARCHIVE_FINANCIAL_TRANSACTION
        = "INSERT INTO demo.ArchiveFinancialTransaction (AccountStatementId, TransactionId, TransactionNumber, CompressedData, Signature)"
        + "VALUES (?, ?, ?, ?, ?); ";

    private static final Duration COMPRESS_TIMEOUT = Duration.ofMinutes(1);
    private static final Duration SIGN_TIMEOUT = Duration.ofSeconds(30);

    private final ActorRef zipActor = getContext().actorOf(ZipActor.props());
    private final ActorRef rsaActor = getContext().actorOf(RsaActor.props());

    private final DataSource dataSource;
    private final Executor jdbcExecutor;

    public static Props props(DataSource dataSource, Executor jdbcExecutor) {
        return Props.create(AccountStatementArchiverActor.class, () -> new AccountStatementArchiverActor(dataSource, jdbcExecutor));
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
            .match(AccountStatementArchiverRequest.class, accountStatementArchiverRequest ->
                Patterns.pipe(archiveAccountStatement(accountStatementArchiverRequest), getContext().getDispatcher()).pipeTo(getSender(), getSelf())
            )
            .build();
    }

    private CompletionStage<AccountStatementArchiverResponse> archiveAccountStatement(AccountStatementArchiverRequest accountStatementArchiverRequest) {
        FinancialTransaction financialTransaction = accountStatementArchiverRequest.getFinancialTransaction();
        byte[] data = financialTransaction.getData();

        LOGGER.info("Archiving financial transaction - transactionId: [{}]", financialTransaction.getTransactionId());

        CompletionStage<byte[]> compressedDataCompletionStage = Patterns.ask(zipActor, ZipCompressorRequest.builder().rawData(data).build(), COMPRESS_TIMEOUT)
            .thenApply(ZipCompressorResponse.class::cast)
            .thenApply(ZipCompressorResponse::getCompressedData);

        CompletionStage<byte[]> signatureCompletionStage = Patterns.ask(rsaActor, RsaSignerRequest.builder().data(data).build(), SIGN_TIMEOUT)
            .thenApply(RsaSignerResponse.class::cast)
            .thenApply(RsaSignerResponse::getSignature);

        return compressedDataCompletionStage
            .thenCombine(signatureCompletionStage, (compressedData, signature) ->
                ArchiveFinancialTransaction.builder()
                    .accountStatementId(accountStatementArchiverRequest.getAccountStatementId().toString())
                    .transactionId(financialTransaction.getTransactionId())
                    .transactionNumber(accountStatementArchiverRequest.getTransactionNumber())
                    .compressedData(compressedData)
                    .signature(signature)
                    .build()
            )
            .thenCompose(this::moveFinancialTransactionToArchive)
            .thenApply(ignore ->
                AccountStatementArchiverResponse.builder().accountStatementId(accountStatementArchiverRequest.getAccountStatementId()).build()
            );
    }

    private CompletionStage<Void> moveFinancialTransactionToArchive(ArchiveFinancialTransaction archiveFinancialTransaction) {
        return CompletableFuture.runAsync(() -> {
            try (
                Connection connection = dataSource.getConnection();
                PreparedStatement deletePreparedStatement = connection.prepareStatement(DELETE_FINANCIAL_TRANSACTION_BY_TRANSACTION_ID);
                PreparedStatement insertPreparedStatement = connection.prepareStatement(INSERT_ARCHIVE_FINANCIAL_TRANSACTION)
            ) {
                connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
                connection.setAutoCommit(false);

                deletePreparedStatement.setString(1, archiveFinancialTransaction.getTransactionId());
                deletePreparedStatement.execute();

                insertPreparedStatement.setString(1, archiveFinancialTransaction.getAccountStatementId());
                insertPreparedStatement.setString(2, archiveFinancialTransaction.getTransactionId());
                insertPreparedStatement.setInt(3, archiveFinancialTransaction.getTransactionNumber());
                insertPreparedStatement.setBytes(4, archiveFinancialTransaction.getCompressedData());
                insertPreparedStatement.setBytes(5, archiveFinancialTransaction.getSignature());
                insertPreparedStatement.execute();

                connection.commit();
            } catch (SQLException e) {
                throw new DbException("Failed to move financial transaction to archive", e);
            }
        }, jdbcExecutor).whenComplete((ignore, failure) -> {
            if (failure != null) {
                LOGGER.error("Failed to delete financial transaction and save it to archive financial transactions - transactionId: [{}]",
                    archiveFinancialTransaction.getTransactionId(),
                    failure
                );
            } else {
                LOGGER.info(
                    "Financial transaction deleted and saved to archive financial transactions - transactionId: [{}]",
                    archiveFinancialTransaction.getTransactionId()
                );
            }
        });
    }

    @Data
    @Builder
    public static final class AccountStatementArchiverRequest {

        private final UUID accountStatementId;
        private final FinancialTransaction financialTransaction;
        private final int transactionNumber;

    }

    @Data
    @Builder
    public static final class AccountStatementArchiverResponse {

        private final UUID accountStatementId;

    }
}
