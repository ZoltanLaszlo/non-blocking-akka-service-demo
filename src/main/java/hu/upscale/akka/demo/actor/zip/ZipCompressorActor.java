package hu.upscale.akka.demo.actor.zip;

import static hu.upscale.akka.demo.actor.zip.ZipActor.KB_IN_BYTES;

import akka.actor.AbstractActor;
import akka.actor.Props;
import hu.upscale.akka.demo.exception.CompressionException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.Deflater;
import lombok.Builder;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author László Zoltán
 */
public class ZipCompressorActor extends AbstractActor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZipCompressorActor.class);

    public static Props props() {
        return Props.create(ZipCompressorActor.class, ZipCompressorActor::new);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
            .match(ZipCompressorRequest.class, zipCompressorRequest ->
                getSender().tell(
                    ZipCompressorResponse.builder().compressedData(compress(zipCompressorRequest.getRawData())).build(),
                    getSelf()
                )
            )
            .build();
    }

    private byte[] compress(byte[] rawData) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream(rawData.length)) {
            Deflater deflater = new Deflater();
            deflater.setLevel(Deflater.BEST_COMPRESSION);
            deflater.setInput(rawData);
            deflater.finish();

            byte[] buffer = new byte[KB_IN_BYTES];
            while (!deflater.finished()) {
                int count = deflater.deflate(buffer);
                outputStream.write(buffer, 0, count);
            }

            byte[] compressedData = outputStream.toByteArray();

            LOGGER.info("Zip compression original data length: {} Kb", rawData.length / KB_IN_BYTES);
            LOGGER.info("Zip compression compressed data length: {} Kb", compressedData.length / KB_IN_BYTES);

            return compressedData;
        } catch (IOException e) {
            throw new CompressionException("Failed to compress data", e);
        }
    }

    @Data
    @Builder
    public static final class ZipCompressorRequest {

        private final byte[] rawData;

    }

    @Data
    @Builder
    public static final class ZipCompressorResponse {

        private final byte[] compressedData;

    }
}
