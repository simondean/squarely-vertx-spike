package io.squarely.vertxspike.queries.where;

import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.ArrayList;

public class ExpressionFactory {
  private static final String GREATER_THAN_OPERATOR = "$gt";
  private static final String GREATER_THAN_OR_EQUAL_TO_OPERATOR = "$gte";
  private static final String LESS_THAN_OPERATOR = "$lt";
  private static final String LESS_THAN_OR_EQUAL_TO_OPERATOR = "$lte";
  private static final String EQUALS_OPERATOR = "$eq";
  private static final String NOT_EQUALS_OPERATOR = "$ne";
  private static final String NOW_OPERATOR = "$now";
  private static final String MINUS_OPERATOR = "$minus";
  private static final String AND_OPERATOR = "$and";
  private static final String OR_OPERATOR = "$or";
  private static final String NOR_OPERATOR = "$nor";
  private static final String NOT_OPERATOR = "$not";

  public static Operation fromJsonExpression(Object value) throws InvalidExpressionException {
    return createOperationFromExpression(createExpressionFromJsonExpression(value));
  }

  private static Operation createOperationFromExpression(Expression expression) {
    if (expression instanceof Operation) {
      return (Operation) expression;
    }

    return new EqualsOperation(expression);
  }

  private static Iterable<Expression> createExpressionsFromJsonObject(JsonObject jsonObject) throws InvalidExpressionException {
    ArrayList<Expression> expressions = new ArrayList<>();

    for (String fieldName : jsonObject.getFieldNames()) {
      expressions.add(createOperationWithArguments(fieldName, jsonObject.getField(fieldName)));
    }

    return expressions;
  }

  private static Operation createOperationWithArguments(String operator, Object jsonExpression) throws InvalidExpressionException {
    switch (operator) {
      case AND_OPERATOR:
        checkIsJsonArray(jsonExpression);
        return new AndOperation(createExpressionsFromJsonArray((JsonArray) jsonExpression));
      case OR_OPERATOR:
        checkIsJsonArray(jsonExpression);
        return new OrOperation(createExpressionsFromJsonArray((JsonArray) jsonExpression));
      case NOR_OPERATOR:
        checkIsJsonArray(jsonExpression);
        return new NorOperation(createExpressionsFromJsonArray((JsonArray) jsonExpression));
      case NOT_OPERATOR:
        return new NotOperation(createExpressionFromJsonExpression(jsonExpression));
      case EQUALS_OPERATOR:
        return new EqualsOperation(createExpressionFromJsonExpression(jsonExpression));
      case NOT_EQUALS_OPERATOR:
        return new NotEqualsOperation(createExpressionFromJsonExpression(jsonExpression));
      case GREATER_THAN_OPERATOR:
        return new GreaterThanOperation(createExpressionFromJsonExpression(jsonExpression));
      case GREATER_THAN_OR_EQUAL_TO_OPERATOR:
        return new GreaterThanOrEqualOperation(createExpressionFromJsonExpression(jsonExpression));
      case LESS_THAN_OPERATOR:
        return new LessThanOperation(createExpressionFromJsonExpression(jsonExpression));
      case LESS_THAN_OR_EQUAL_TO_OPERATOR:
        return new LessThanOrEqualOperation(createExpressionFromJsonExpression(jsonExpression));
      case MINUS_OPERATOR:
        checkIsJsonArray(jsonExpression);
        return new MinusOperation(createExpressionsFromJsonArray((JsonArray) jsonExpression));
      default:
        throw new InvalidExpressionException("Invalid operator " + operator);
    }
  }

  private static Operation createOperationWithNoArguments(String operator) throws InvalidExpressionException {
    switch (operator) {
      case NOW_OPERATOR:
        return new NowOperation();
      default:
        throw new InvalidExpressionException("Invalid operator " + operator);
    }
  }

  private static Expression createExpressionFromJsonExpression(Object value) throws InvalidExpressionException {
    if (value == null) {
      return new ConstantExpression(null);
    }

    if (value instanceof JsonObject) {
      JsonObject jsonObject = (JsonObject) value;

      switch (jsonObject.size()) {
        case 0:
          throw new InvalidExpressionException("Empty object");
        case 1:
          String fieldName = getFirstFieldNameFromJsonObject(jsonObject);
          return createOperationWithArguments(fieldName, jsonObject.getValue(fieldName));
        default:
          return new AndOperation(createExpressionsFromJsonObject(jsonObject));
      }
    }

    if (value instanceof String) {
      String string = (String) value;

      if (string.startsWith("$")) {
        return createOperationWithNoArguments(string);
      }
    }

    return new ConstantExpression(value);
  }

  private static void checkIsJsonArray(Object value) throws InvalidExpressionException {
    if (!(value instanceof JsonArray)) {
      throw new InvalidExpressionException("Expressions must be in a JsonArray");
    }
  }

  private static Iterable<Expression> createExpressionsFromJsonArray(JsonArray value) throws InvalidExpressionException {
    ArrayList<Expression> expressions = new ArrayList<>();

    for (Object item : value) {
      expressions.add(createExpressionFromJsonExpression(item));
    }

    return expressions;
  }

  private static String getFirstFieldNameFromJsonObject(JsonObject jsonObject) {
    for (String fieldName : jsonObject.getFieldNames()) {
      return fieldName;
    }

    return null;
  }
}
