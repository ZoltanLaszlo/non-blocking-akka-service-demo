package hu.upscale.akka.demo.entity;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Data;

/**
 * @author László Zoltán
 */
@Data
@JsonDeserialize(builder = FinancialTransaction.FinancialTransactionBuilder.class)
@Builder(builderClassName = "FinancialTransactionBuilder", toBuilder = true)
public final class FinancialTransaction {

    public static final String TRANSACTION_ID_COLUMN_NAME = "TransactionId";
    public static final String PREVIOUS_TRANSACTION_ID_COLUMN_NAME = "PreviousTransactionId";
    public static final String DATA_COLUMN_NAME = "Data";

    private final String transactionId;
    private final String previousTransactionId;
    private final byte[] data;

    @JsonPOJOBuilder(withPrefix = "")
    public static final class FinancialTransactionBuilder {
    }
}
