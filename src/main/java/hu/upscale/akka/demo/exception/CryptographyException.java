package hu.upscale.akka.demo.exception;

/**
 * @author László Zoltán
 */
public class CryptographyException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public CryptographyException(String message, Throwable cause) {
        super(message, cause);
    }
}
