package hu.upscale.akka.demo.exception;

/**
 * @author László Zoltán
 */
public class DbException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;

    public DbException(String message, Throwable cause) {
        super(message, cause);
    }
}
