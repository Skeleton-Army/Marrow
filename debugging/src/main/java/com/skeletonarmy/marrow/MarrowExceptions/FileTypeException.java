package com.skeletonarmy.marrow.MarrowExceptions;

public class FileTypeException extends RuntimeException {
  public FileTypeException(String message) {
    super(message);
  }
  public FileTypeException(String message, Throwable cause) {super(message, cause);}
  public FileTypeException(Throwable cause) {super(cause);}
}
