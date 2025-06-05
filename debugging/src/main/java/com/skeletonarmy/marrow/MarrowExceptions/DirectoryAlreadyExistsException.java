package com.skeletonarmy.marrow.MarrowExceptions;

public class DirectoryAlreadyExistsException extends RuntimeException {
    public DirectoryAlreadyExistsException(String message) {
        super(message);
    }
    public DirectoryAlreadyExistsException (String message, Throwable cause) {
        super(message, cause);
    }
}
