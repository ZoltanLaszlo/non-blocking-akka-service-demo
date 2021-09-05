package hu.upscale.akka.demo.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * @author László Zoltán
 */
public final class ResourceUtil {

    private ResourceUtil() {
        // Static class
    }

    public static byte[] readResourceFile(String resource) {
        try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource)) {
            if (inputStream == null) {
                return new byte[0];
            }

            return inputStream.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read resource file: " + resource, e);
        }
    }

    public static String readResourceFileAsString(String resource, Charset charset) {
        return new String(readResourceFile(resource), charset);
    }

    public static Stream<String> readResourceFileLines(String resource, Charset charset) {
        return Arrays.stream(readResourceFileAsString(resource, charset).split("[\\r\\n]+"))
            .filter(Objects::nonNull)
            .filter(Predicate.not(String::isEmpty));
    }
}
