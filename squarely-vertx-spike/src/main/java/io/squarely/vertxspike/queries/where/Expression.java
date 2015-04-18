package io.squarely.vertxspike.queries.where;

public abstract class Expression {
  public abstract Object evaluate(Object leftHandValue) throws InvalidExpressionException;
}
