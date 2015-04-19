package io.squarely.vertxspike.queries.expressions;

public abstract class Expression {
  public abstract Object evaluate(Object leftHandValue) throws InvalidExpressionException;
}
