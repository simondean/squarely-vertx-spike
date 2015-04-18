package io.squarely.vertxspike.queries.where;

import org.joda.time.DateTime;

public class NowOperation extends Operation {
  @Override
  public Object evaluate(Object leftHandValue) throws InvalidExpressionException {
    return DateTime.now().getMillis();
  }
}
