package hu.upscale.akka.demo.actor.zip;

import static hu.upscale.akka.demo.actor.zip.ZipActor.KB_IN_BYTES;

import akka.actor.AbstractActor;
import akka.actor.Props;
import hu.upscale.akka.demo.exception.CompressionException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;
import lombok.Builder;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author László Zoltán
 */
public class ZipDecompressorActor extends AbstractActor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZipDecompressorActor.class);

    public static Props props() {
        return Props.create(ZipDecompressorActor.class, ZipDecompressorActor::new);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
            .match(ZipDecompressorRequest.class, zipDecompressorRequest ->
                getSender().tell(
                    ZipDecompressorResponse.builder().decompressedData(decompress(zipDecompressorRequest.getCompressedData())),
                    getSelf()
                )
            )
            .build();
    }

    private byte[] decompress(byte[] compressedData) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream(compressedData.length)) {
            Inflater inflater = new Inflater();
            inflater.setInput(compressedData);

            byte[] buffer = new byte[KB_IN_BYTES];
            while (!inflater.finished()) {
                int count = inflater.inflate(buffer);
                outputStream.write(buffer, 0, count);
            }

            byte[] decompressedData = outputStream.toByteArray();

            LOGGER.info("Zip decompression compressed data length: {} Kb", compressedData.length / KB_IN_BYTES);
            LOGGER.info("Zip decompression decompressed data length: {} Kb", decompressedData.length / KB_IN_BYTES);

            return decompressedData;
        } catch (IOException | DataFormatException e) {
            throw new CompressionException("Failed to decompress data", e);
        }
    }

    @Data
    @Builder
    public static final class ZipDecompressorRequest {

        private final byte[] compressedData;

    }

    @Data
    @Builder
    public static final class ZipDecompressorResponse {

        private final byte[] decompressedData;

    }
}
