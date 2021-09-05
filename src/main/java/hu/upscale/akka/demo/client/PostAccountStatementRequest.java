package hu.upscale.akka.demo.client;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

/**
 * @author László Zoltán
 */
@Data
@JsonDeserialize(builder = PostAccountStatementRequest.PostAccountStatementRequestBuilder.class)
@Builder(builderClassName = "PostAccountStatementRequestBuilder", toBuilder = true)
public final class PostAccountStatementRequest {

    private final UUID lastTransactionId;

    @JsonPOJOBuilder(withPrefix = "")
    public static final class PostAccountStatementRequestBuilder {
    }

}
