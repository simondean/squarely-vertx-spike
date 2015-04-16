package io.squarely.vertxspike.queries;

import java.util.ArrayList;
import java.util.List;

public abstract class OperationWithArguments extends Operation {
  private List<Expression> arguments = new ArrayList<>();

  public OperationWithArguments(Iterable<Expression> arguments) {
    for (Expression item : arguments) {
      this.arguments.add(item);
    }
  }

  public OperationWithArguments(Expression operand) {
    this.arguments.add(operand);
  }

  public Iterable<Expression> getArguments() {
    return arguments;
  }

  public Expression getArgument(int index) {
    return arguments.get(index);
  }
}
