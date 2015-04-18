package io.squarely.vertxspike.queries;

import io.squarely.vertxspike.queries.where.Expression;
import io.squarely.vertxspike.queries.where.ExpressionFactory;
import io.squarely.vertxspike.queries.where.InvalidExpressionException;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class Query {
  private final JsonObject metricClause;
  private final JsonObject pointClause;
  private final JsonArray fromClause;
  private final Map<String, Expression> whereClause;
  private final JsonArray groupClause;
  private final JsonObject aggregateClause;

  public Query(JsonObject metricClause, JsonObject pointClause, JsonArray fromClause, Map<String, Expression> whereClause, JsonArray groupClause, JsonObject aggregateClause) {
    this.metricClause = metricClause;
    this.pointClause = pointClause;
    this.fromClause = fromClause;
    this.whereClause = whereClause;
    this.groupClause = groupClause;
    this.aggregateClause = aggregateClause;
  }

  public static Query fromJsonObject(JsonObject query) throws InvalidQueryException {
    return new Query(getMetricClauseFromQuery(query),
      getPointClauseFromQuery(query),
      getFromClauseFromQuery(query),
      getWhereClauseFromQuery(query),
      getGroupClauseFromQuery(query),
      getAggregateClauseFromQuery(query));
  }

  private static JsonObject getPointClauseFromQuery(JsonObject query) {
    return query.getObject("point");
  }

  private static JsonObject getMetricClauseFromQuery(JsonObject query) {
    return query.getObject("metric");
  }

  private static JsonArray getFromClauseFromQuery(JsonObject query) {
    Object from = query.getValue("from");
    JsonArray fromItems;

    if (from instanceof JsonArray) {
      fromItems = (JsonArray)from;
    }
    else {
      fromItems = new JsonArray();
      fromItems.addString((String)from);
    }

    return fromItems;
  }

  private static Map<String, Expression> getWhereClauseFromQuery(JsonObject query) throws InvalidQueryException {
    JsonObject whereJsonExpression = query.getObject("where");
    HashMap<String, Expression> where = new HashMap<>();

    try {
      if (whereJsonExpression != null) {
        for (String whereFieldName : whereJsonExpression.getFieldNames()) {
          where.put(whereFieldName, ExpressionFactory.fromJsonExpression(whereJsonExpression.getValue(whereFieldName)));
        }
      }
    }
    catch (InvalidExpressionException e) {
      throw new InvalidQueryException("Invalid where clause in query", e);
    }

    return where;
  }

  private static JsonArray getGroupClauseFromQuery(JsonObject query) {
    return query.getArray("group");
  }

  private static JsonObject getAggregateClauseFromQuery(JsonObject query) {
    return query.getObject("aggregate");
  }

  public JsonObject metricClause() {
    return metricClause;
  }

  public JsonObject pointClause() {
    return pointClause;
  }

  public JsonArray fromClause() {
    return fromClause;
  }

  public Map<String, Expression> whereClause() {
    return whereClause;
  }

  public JsonArray groupClause() {
    return groupClause;
  }

  public JsonObject aggregateClause() {
    return aggregateClause;
  }

  public boolean hasMetricClause() {
    return metricClause() != null;
  }

  public boolean hasPointClause() {
    return pointClause() != null;
  }

  public boolean hasGroupClause() {
    return groupClause() != null;
  }

  public boolean hasAggregateClause() {
    return aggregateClause() != null;
  }
}
