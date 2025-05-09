package miniORM.exception;

public class OrmMappingException extends OrmException {
    public OrmMappingException(String message) { super(message); }
    public OrmMappingException(String message, Throwable cause) { super(message, cause); }
}
