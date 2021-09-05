package hu.upscale.akka.demo.entity;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Data;

/**
 * @author László Zoltán
 */
@Data
@JsonDeserialize(builder = ArchiveFinancialTransaction.ArchiveFinancialTransactionBuilder.class)
@Builder(builderClassName = "ArchiveFinancialTransactionBuilder", toBuilder = true)
public final class ArchiveFinancialTransaction {

    private final String accountStatementId;
    private final String transactionId;
    private final int transactionNumber;
    private final byte[] compressedData;
    private final byte[] signature;

    @JsonPOJOBuilder(withPrefix = "")
    public static final class ArchiveFinancialTransactionBuilder {
    }

}
