package io.squarely.vertxspike.queries;

public abstract class LogicalOperation extends OperationWithArguments {
  public LogicalOperation(Iterable<Expression> arguments) {
    super(arguments);
  }

  public LogicalOperation(Expression operand) {
    super(operand);
  }

  protected boolean evaluatesToTrue(Object value) {
    if (value == null || !(value instanceof Boolean)) {
      return false;
    }

    return (boolean) value;
  }
}
