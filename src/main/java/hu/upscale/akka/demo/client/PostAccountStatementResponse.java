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
@JsonDeserialize(builder = PostAccountStatementResponse.PostAccountStatementResponseBuilder.class)
@Builder(builderClassName = "PostAccountStatementResponseBuilder", toBuilder = true)
public final class PostAccountStatementResponse {

    private final UUID accountStatementId;

    @JsonPOJOBuilder(withPrefix = "")
    public static final class PostAccountStatementResponseBuilder {
    }

}
