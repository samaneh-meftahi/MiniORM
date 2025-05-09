package miniORM.exception;

public class OrmTransactionException extends OrmException {
    public OrmTransactionException(String message) { super(message); }
    public OrmTransactionException(String message, Throwable cause) { super(message, cause); }
}

