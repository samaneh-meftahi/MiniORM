package miniORM.exception;

public class OrmException extends RuntimeException {
    public OrmException() { super(); }
    public OrmException(String message) { super(message); }
    public OrmException(String message, Throwable cause) { super(message, cause); }
    public OrmException(Throwable cause) { super(cause); }
}
