package io.squarely.vertxspike.queries;

public abstract class Expression {
  public abstract Object evaluate(Object leftHandValue) throws InvalidExpressionException;
}
