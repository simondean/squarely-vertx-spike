package io.squarely.vertxspike.queries.where;

public class NotOperation extends LogicalOperation {
  public NotOperation(Expression expression) {
    super(expression);
  }

  @Override
  public Object evaluate(Object leftHandValue) throws InvalidExpressionException {
    return !evaluatesToTrue(getArgument(0).evaluate(leftHandValue));
  }
}

