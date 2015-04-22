package io.squarely.vertxspike.queries.expressions;

public class LessThanOrEqualOperation extends ComparisonOperation {
  public LessThanOrEqualOperation(Expression argument) {
    super(argument);
  }

  @Override
  public Object evaluate(Object leftHandValue) throws InvalidExpressionException {
    return compare(leftHandValue, getArgument(0).evaluate(leftHandValue)) <= 0;
  }
}
