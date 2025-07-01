package com.skeletonarmy.marrow.MarrowExceptions;

import java.io.IOException;
public class NotDirectoryException extends IOException {
    public NotDirectoryException (String message, Throwable throwable) {
        super(message, throwable);
    }
    public NotDirectoryException(String message) {
        super(message);
    }
}
