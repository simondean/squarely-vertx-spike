package io.squarely.vertxspike;

import com.jetdrone.vertx.yoke.Yoke;
import com.jetdrone.vertx.yoke.engine.StringPlaceholderEngine;
import com.jetdrone.vertx.yoke.middleware.*;
import io.squarely.vertxspike.json.JsonArrayIterable;
import io.squarely.vertxspike.queries.AggregateField;
import io.squarely.vertxspike.queries.InvalidQueryException;
import io.squarely.vertxspike.queries.Query;
import io.squarely.vertxspike.queries.expressions.Expression;
import io.squarely.vertxspike.queries.expressions.InvalidExpressionException;
import io.vertx.java.redis.RedisClient;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.sockjs.SockJSServer;
import org.vertx.java.core.sockjs.SockJSSocket;
import org.vertx.java.platform.Verticle;

import java.util.*;

public class ServerVerticle extends Verticle {
  private org.vertx.java.core.logging.Logger logger;
  private EventBus eventBus;
  private RedisClient redis;
  private final HashMap<SockJSSocket, SocketState> socketStates = new HashMap<>();

  public void start() {
    logger = container.logger();
    eventBus = vertx.eventBus();
    redis = new RedisClient(eventBus, MainVerticle.REDIS_ADDRESS);

    HttpServer httpServer = vertx.createHttpServer();

    Yoke yoke = new Yoke(vertx);
    yoke.engine(new StringPlaceholderEngine("io/squarely/vertxspike/views"));
    yoke.use(new Logger());
    yoke.use(new ErrorHandler(true));
    yoke.use(new Favicon());
    yoke.use("/static", new Static("static"));
    yoke.use(new BodyParser());
    yoke.use(new Router()
      .get("/", (request, next) -> {
        request.response().redirect("/dashboards/sample");
      })
      .get("/dashboards/:dashboardName", (request, next) -> {
        String dashboardName = request.getParameter("dashboardName");
        logger.info("Serving dashboard '" + dashboardName + "'");
        request.response().setContentType("text/html", "utf-8")
          .render("dashboards/" + dashboardName + ".shtml", next);
      })
      .post("/api/metrics", (request, next) -> {
        Object body = request.body();
        YokeResponse response = request.response();

        // TODO: Move this logic into a method for validating metrics JSON
        // TODO: Reuse the method for metrics that arrive via the ESB
        if (!(body instanceof JsonObject)) {
          sendClientError(response, "Request body needs to be a JSON object");
          return;
        }

        JsonObject jsonBody = (JsonObject) body;

        if (!jsonBody.containsField("metrics")) {
          sendClientError(response, "Request body needs to contain a 'metrics' field");
          return;
        }

        Object metrics = jsonBody.getValue("metrics");

        if (!(metrics instanceof JsonArray)) {
          sendClientError(response, "'metrics' field in request body must be an array");
          return;
        }

        JsonArray jsonMetrics = (JsonArray) metrics;

        // TODO: Validate name and points

        saveAndPublishMetrics(jsonMetrics, aVoid -> {
          logger.info("Metrics saved to Redis and published");
          response.setStatusCode(204).end();
        });
      }));
    yoke.listen(httpServer);

    SockJSServer sockJSServer = vertx.createSockJSServer(httpServer);

    // TODO: Set the library_url value
    JsonObject sockJSConfig = new JsonObject().putString("prefix", "/events");

    sockJSServer.installApp(sockJSConfig, socket -> {
      SocketState socketState = new SocketState();
      socketStates.put(socket, socketState);

      socket.dataHandler(buffer -> {
        JsonObject message = new JsonObject(buffer.toString());
        if ("subscribe".equals(message.getString("type"))) {
          JsonObject payload = message.getObject("payload");
          JsonObject queries = payload.getObject("queries");
          HashMap<String, Query> socketStateQueries = socketState.queries();
          ArrayList<Object> mgetArgs = new ArrayList<>();

          for (String key : queries.getFieldNames()) {
            Query query = null;

            try {
              query = Query.fromJsonObject(queries.getObject(key));
            } catch (InvalidQueryException e) {
              logger.error("Received invalid query from socket", e);
            }

            if (query != null) {
              for (String fromMetricName : new JsonArrayIterable<String>(query.fromClause())) {
                mgetArgs.add("metrics." + fromMetricName);
              }
              socketStateQueries.put(key, query);
            }
          }

          // TODO: Use timestamp on metrics to discard old metrics.  Where should this be done?  Client side or server side?

          mgetArgs.add((Handler<Message<JsonObject>>) reply -> {
            JsonObject body = reply.body();
            logger.info("Received Redis values " + body);
            String status = body.getString("status");

            if (!"ok".equals(status)) {
              logger.error("Unexpected Redis reply status of " + status);
            } else {
              JsonArray redisValues = body.getArray("value");
              JsonArray metrics = new JsonArray();

              for (String redisValue : new JsonArrayIterable<String>(redisValues)) {
                if (redisValue != null) {
                  metrics.addObject(new JsonObject(redisValue));
                }
              }

              publishMetrics(metrics, aVoid -> {
                logger.info("Metrics retrieved from Redis and published");
              });
            }
          });

          redis.mget(mgetArgs.toArray());
        }
      });

      socket.endHandler(aVoid -> {
        logger.info("Removing listener");
        socketStates.remove(socket);
      });
    });

    httpServer.listen(8080);

    vertx.eventBus().registerHandler("io.squarely.vertxspike.metrics", (Message<JsonObject> message) -> {
      logger.info("Received " + message.address() + " message");
      JsonArray metrics = message.body().getArray("metrics");
      logger.info("Received metrics " + metrics);

      saveAndPublishMetrics(metrics, aVoid -> {
        logger.info("Metrics saved to Redis and published");
      });
    });

    container.logger().info("ServerVerticle started");
  }

  private void sendClientError(YokeResponse response, String message) {
    JsonObject body = new JsonObject()
      .putObject("error", new JsonObject()
        .putString("message", message));
    response.setStatusCode(400).end(body);
  }

  private void saveAndPublishMetrics(JsonArray metrics, Handler<Void> handler) {
    saveMetrics(metrics, 0, aVoid -> {
      publishMetrics(metrics, handler);
    });
  }

  private void saveMetrics(JsonArray metrics, int metricIndex, Handler<Void> handler) {
    if (metricIndex >= metrics.size()) {
      handler.handle(null);
      return;
    }

    JsonObject metric = metrics.get(metricIndex);

    logger.info("Saving metrics to Redis");
    redis.set("metrics." + metric.getString("name"), metric.toString(), (Handler<Message<JsonObject>>) reply -> {
      String status = reply.body().getString("status");

      if (!"ok".equals(status)) {
        logger.error("Unexpected Redis reply status of " + status);
      }

      saveMetrics(metrics, metricIndex + 1, handler);
    });
  }

  private void publishMetrics(JsonArray metrics, Handler<Void> handler) {
    for (JsonObject metric : new JsonArrayIterable<JsonObject>(metrics)) {
      for (Map.Entry<SockJSSocket, SocketState> socketAndSocketState : socketStates.entrySet()) {
        SockJSSocket socket = socketAndSocketState.getKey();
        SocketState socketState = socketAndSocketState.getValue();

        for (Map.Entry<String, Query> queryEntry : socketState.queries().entrySet()) {
          Query query = queryEntry.getValue();
          String queryKey = queryEntry.getKey();
          String metricName = metric.getString("name");

          if (metricMatchesQuery(metricName, query)) {
            logger.info("Metric " + metricName + " does match query " + queryKey);
            JsonArray transformedMetrics = null;

            try {
              transformedMetrics = applyQueryToMetric(query, metric);
            } catch (InvalidExpressionException e) {
              logger.error("Invalid query expression", e);
            }

            if (transformedMetrics != null) {
              JsonObject payload = new JsonObject();
              payload.putString("key", queryKey);
              payload.putArray("metrics", transformedMetrics);
              JsonObject newMessage = new JsonObject();
              newMessage.putString("type", "notify");
              newMessage.putObject("payload", payload);

              logger.info("Sending SockJS message " + newMessage);

              Buffer newMessageBuffer = new Buffer(newMessage.encode());
              socket.write(newMessageBuffer);
            }
          } else {
            logger.info("Metric " + metricName + " does not match query " + queryKey);
          }
        }
      }
    }

    handler.handle(null);
  }

  private JsonArray applyQueryToMetric(Query query, JsonObject metric) throws InvalidExpressionException {
    JsonObject transformedMetric = copyMetric(metric);
    applyWhereClauseToMetric(query, transformedMetric);
    JsonArray transformedMetrics = applyGroupClauseToMetric(query, transformedMetric);
    applyAggregateClauseToMetrics(query, transformedMetrics);
    applyPointClauseToMetrics(query, transformedMetrics);
    transformedMetrics = applyMetricClauseToMetrics(query, transformedMetrics);

    // TODO: Implement aggregations

    return transformedMetrics;
  }

  private JsonObject copyMetric(JsonObject metric) {
    return metric.copy();
  }

  private void applyWhereClauseToMetric(Query query, JsonObject metric) throws InvalidExpressionException {
    Map<String, Expression> whereClause = query.whereClause();
    JsonArray matchingPoints = new JsonArray();

    for (JsonObject point : new JsonArrayIterable<JsonObject>(metric.getArray("points"))) {
      if (pointMatchesWhereClause(point, whereClause)) {
        matchingPoints.addObject(point);
      }
    }

    metric.putArray("points", matchingPoints);
  }

  private JsonArray applyGroupClauseToMetric(Query query, JsonObject metric) {
    JsonArray transformedMetrics = new JsonArray();

    if (query.hasGroupClause()) {
      JsonArray groups = applyGroupClauseToPoints(query.groupClause(), metric.getArray("points"));

      for (JsonObject group : new JsonArrayIterable<JsonObject>(groups)) {
        transformedMetrics.addObject(metric
          .copy()
          .mergeIn(group)
          .putArray("points", group.getArray("points")));
      }
    } else {
      transformedMetrics.addObject(metric);
    }

    return transformedMetrics;
  }

  private JsonArray applyGroupClauseToPoints(JsonArray groupClause, JsonArray points) {
    HashMap<ArrayList<Object>, JsonObject> groups = new HashMap<>();

    for (JsonObject point : new JsonArrayIterable<JsonObject>(points)) {
      ArrayList<Object> groupKey = new ArrayList<>();

      for (String groupFieldName : new JsonArrayIterable<String>(groupClause)) {
        groupKey.add(groupFieldName);
        groupKey.add(point.getValue(groupFieldName));
      }

      JsonObject group = groups.get(groupKey);
      JsonArray groupPoints;

      if (group == null) {
        groupPoints = new JsonArray();
        group = new JsonObject();

        for (String groupFieldName : new JsonArrayIterable<String>(groupClause)) {
          group.putValue(groupFieldName, point.getValue(groupFieldName));
        }

        group.putArray("points", groupPoints);
        groups.put(groupKey, group);
      } else {
        groupPoints = group.getArray("points");
      }

      groupPoints.addObject(point);
    }

    return convertCollectionToJsonArray(groups.values());
  }

  private void applyAggregateClauseToMetrics(Query query, JsonArray metrics) throws InvalidExpressionException {
    if (!query.hasAggregateClause()) {
      return;
    }

    Map<String, Expression> aggregateClause = query.aggregateClause();

    for (JsonObject metric : new JsonArrayIterable<JsonObject>(metrics)) {
      JsonArray transformedPoints = applyAggregateClauseToPoints(aggregateClause, metric.getArray("points"));
      metric.putArray("points", transformedPoints);
    }
  }

  private JsonArray applyAggregateClauseToPoints(Map<String, Expression> aggregateClause, JsonArray points) throws InvalidExpressionException {
    // TODO: Maybe combine this method with the equivalent for the group clause
    Set<Map.Entry<String, Expression>> aggregateClauseEntries = aggregateClause.entrySet();
    Set<String> aggregateClauseFieldNames = aggregateClause.keySet();
    HashMap<ArrayList<Object>, JsonObject> aggregatePoints = new HashMap<>();

    for (JsonObject point : new JsonArrayIterable<JsonObject>(points)) {
      ArrayList<Object> aggregateKey = new ArrayList<>();

      for (Map.Entry<String, Expression> aggregateClauseEntry : aggregateClauseEntries) {
        String fieldName = aggregateClauseEntry.getKey();
        Expression expression = aggregateClauseEntry.getValue();
        Object aggregateValue = expression.evaluate(point.getValue(fieldName));

        aggregateKey.add(fieldName);
        aggregateKey.add(aggregateValue);
      }

      JsonObject aggregatePoint = aggregatePoints.get(aggregateKey);

      if (aggregatePoint == null) {
        aggregatePoint = new JsonObject();

        for (int index = 0, count = aggregateKey.size(); index < count; index += 2) {
          aggregatePoint.putValue((String) aggregateKey.get(index), aggregateKey.get(index + 1));
        }

        aggregatePoints.put(aggregateKey, aggregatePoint);
      }

      for (String pointFieldName : point.getFieldNames()) {
        if (!aggregateClauseFieldNames.contains(pointFieldName)) {
          JsonArray aggregatePointFieldValue = aggregatePoint.getArray(pointFieldName);

          if (aggregatePointFieldValue == null) {
            aggregatePointFieldValue = new JsonArray();
            aggregatePoint.putArray(pointFieldName, aggregatePointFieldValue);
          }

          aggregatePointFieldValue.add(point.getValue(pointFieldName));
        }
      }
    }

    return convertCollectionToJsonArray(aggregatePoints.values());
  }

  private void applyPointClauseToMetrics(Query query, JsonArray metrics) throws InvalidExpressionException {
    if (!query.hasPointClause()) {
      return;
    }

    HashMap<String, AggregateField> pointClause = query.pointClause();

    for (JsonObject metric : new JsonArrayIterable<JsonObject>(metrics)) {
      applyPointClauseToPoints(pointClause, metric);
    }
  }

  private void applyPointClauseToPoints(HashMap<String, AggregateField> pointClause, JsonObject metric) throws InvalidExpressionException {
    Set<Map.Entry<String, AggregateField>> pointClauseEntries = pointClause.entrySet();
    JsonArray transformedPoints = new JsonArray();

    for (JsonObject point : new JsonArrayIterable<JsonObject>(metric.getArray("points"))) {
      JsonObject transformedPoint = new JsonObject();

      for (Map.Entry<String, AggregateField> pointClauseEntry : pointClauseEntries) {
        AggregateField aggregateField = pointClauseEntry.getValue();
        Object transformedPointFieldValue;

        if (aggregateField.hasExpression()) {
          Object pointFieldValue = point.getValue(aggregateField.fieldName());
          Expression expression = aggregateField.expression();

          if (pointFieldValue instanceof JsonArray) {
            pointFieldValue = ((JsonArray) pointFieldValue).toList();
          }

          transformedPointFieldValue = expression.evaluate(pointFieldValue);
        } else {
          transformedPointFieldValue = point.getValue(aggregateField.fieldName());
        }

        String transformedPointFieldName = pointClauseEntry.getKey();
        transformedPoint.putValue(transformedPointFieldName, transformedPointFieldValue);
      }

      transformedPoints.addObject(transformedPoint);
    }

    metric.putArray("points", transformedPoints);
  }

  private JsonArray applyMetricClauseToMetrics(Query query, JsonArray metrics) {
    if (!query.hasMetricClause()) {
      logger.info("No projection to apply to metrics");
      return metrics;
    }

    JsonObject metricClause = query.metricClause();
    JsonArray transformedMetrics = new JsonArray();

    for (JsonObject metric : new JsonArrayIterable<JsonObject>(metrics)) {
      logger.info("Applying projection " + metricClause + " to metric " + metric);
      JsonObject transformedMetric = new JsonObject();

      for (String projectionFieldName : metricClause.getFieldNames()) {
        String metricFieldName = metricClause.getString(projectionFieldName);
        transformedMetric.putValue(projectionFieldName, metric.getValue(metricFieldName));
      }

      transformedMetric.putArray("points", metric.getArray("points"));

      transformedMetrics.addObject(transformedMetric);
    }

    return transformedMetrics;
  }

  private <T> JsonArray convertCollectionToJsonArray(Collection<T> collection) {
    JsonArray jsonArray = new JsonArray();

    collection.forEach(jsonArray::add);

    return jsonArray;
  }

  private boolean pointMatchesWhereClause(JsonObject point, Map<String, Expression> where) throws InvalidExpressionException {
    boolean isMatch = true;

    for (Map.Entry<String, Expression> whereClauseEntry : where.entrySet()) {
      String fieldName = whereClauseEntry.getKey();
      Expression expression = whereClauseEntry.getValue();
      Object pointFieldValue = point.getValue(fieldName);

      Object result = expression.evaluate(pointFieldValue);

      if (result == null || !(result instanceof Boolean)) {
        isMatch = false;
        break;
      }

      boolean booleanResult = (boolean) result;

      if (booleanResult == false) {
        isMatch = false;
        break;
      }
    }
    return isMatch;
  }

  private boolean metricMatchesQuery(String metricName, Query query) {
    JsonArray fromItems = query.fromClause();

    boolean isMatch = false;

    for (String fromItem : new JsonArrayIterable<String>(fromItems)) {
      if (fromItem.equals(metricName)) {
        isMatch = true;
        break;
      }
    }
    return isMatch;
  }

  private class SocketState {
    private HashMap<String, Query> queries = new HashMap<>();

    public HashMap<String, Query> queries() {
      return queries;
    }
  }
}
