package miniORM.exception;

public class OrmDatabaseException extends OrmException {
    public OrmDatabaseException(String message) { super(message); }
    public OrmDatabaseException(String message, Throwable cause) { super(message, cause); }
}
