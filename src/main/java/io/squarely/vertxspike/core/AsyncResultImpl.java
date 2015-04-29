package io.squarely.vertxspike.core;

import org.vertx.java.core.AsyncResult;

public class AsyncResultImpl<T> implements AsyncResult<T> {
  private Throwable cause;
  private T result;

  public AsyncResultImpl(Throwable cause, T result) {
    this.cause = cause;
    this.result = result;
  }

  public static <T> AsyncResult<T> succeed(T result) {
    return new AsyncResultImpl<>(null, result);
  }

  public static AsyncResult<Void> succeed() {
    return succeed(null);
  }

  public static <T> AsyncResult<T> fail(Throwable cause) {
    if (cause == null) {
      throw new IllegalArgumentException("cause argument cannot be null");
    }

    return new AsyncResultImpl<>(cause, null);
  }

  @Override
  public T result() {
    return result;
  }

  @Override
  public Throwable cause() {
    return cause;
  }

  @Override
  public boolean succeeded() {
    return cause == null;
  }

  @Override
  public boolean failed() {
    return cause != null;
  }
}