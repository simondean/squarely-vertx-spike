package io.squarely.vertxspike;

import io.squarely.vertxspike.json.JsonArrayIterable;
import io.squarely.vertxspike.queries.Expression;
import io.squarely.vertxspike.queries.ExpressionFactory;
import io.squarely.vertxspike.queries.InvalidExpressionException;
import io.squarely.vertxspike.queries.Operation;
import io.vertx.java.redis.RedisClient;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerResponse;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.sockjs.SockJSServer;
import org.vertx.java.core.sockjs.SockJSSocket;
import org.vertx.java.platform.Verticle;

import java.util.*;

public class ServerVerticle extends Verticle {
  private Logger logger;
  private EventBus eventBus;
  private RedisClient redis;
  private final HashMap<SockJSSocket, SocketState> socketStates = new HashMap<>();

  public void start() {
    logger = container.logger();
    eventBus = vertx.eventBus();
    redis = new RedisClient(eventBus, MainVerticle.REDIS_ADDRESS);

    HttpServer httpServer = vertx.createHttpServer();

    httpServer.requestHandler(request -> {
      HttpServerResponse response = request.response();
      String file = "";
      if (request.path().equals("/")) {
        response.putHeader("Content-Type", "text/html; charset=utf-8");
        file = "index.html";
      } else if (!request.path().contains("..")) {
        file = request.path();
      }
      response.sendFile("web/" + file);
    });

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
          HashMap<String, JsonObject> socketStateQueries = socketState.queries();
          ArrayList<Object> mgetArgs = new ArrayList<>();

          for (String key : queries.getFieldNames()) {
            JsonObject query = queries.getObject(key);
            for (String fromMetricName : new JsonArrayIterable<String>(getFromItemsFromQuery(query))) {
              mgetArgs.add("metrics." + fromMetricName);
            }
            socketStateQueries.put(key, query);
          }

          // TODO: Use timestamp on metrics to discard old metrics.  Where should this be done?  Client side or server side?

          mgetArgs.add((Handler<Message<JsonObject>>) reply -> {
            JsonObject body = reply.body();
            logger.info("Received Redis values " + body);
            String status = body.getString("status");

            if (!"ok".equals(status)) {
              logger.error("Unexpected Redis reply status of " + status);
            }
            else {
              JsonArray redisValues = body.getArray("value");
              JsonArray metrics = new JsonArray();

              for (String redisValue : new JsonArrayIterable<String>(redisValues)) {
                if (redisValue != null) {
                  metrics.addObject(new JsonObject(redisValue));
                }
              }

              try {
                publishMetrics(metrics);
              }
              catch (InvalidExpressionException e) {
                logger.error("Invalid query expression", e);
              }            }

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

      try {
        publishMetrics(metrics);
      }
      catch (InvalidExpressionException e) {
        logger.error("Invalid query expression", e);
      }
    });

    container.logger().info("ServerVerticle started");
  }

  private void publishMetrics(JsonArray metrics) throws InvalidExpressionException {
    for (JsonObject metric : new JsonArrayIterable<JsonObject>(metrics)) {
      for (Map.Entry<SockJSSocket, SocketState> socketAndSocketState : socketStates.entrySet()) {
        SockJSSocket socket = socketAndSocketState.getKey();
        SocketState socketState = socketAndSocketState.getValue();

        for (Map.Entry<String, JsonObject> keyAndQuery : socketState.queries().entrySet()) {
          JsonObject query = keyAndQuery.getValue();
          String queryKey = keyAndQuery.getKey();
          String metricName = metric.getString("name");
          Map<String, Expression> where = getWhereFromQuery(query);

          try {
            if (metricMatchesQuery(metricName, query)) {
              logger.info("Metric " + metricName + " does match query " + queryKey);

              JsonArray transformedMetrics;

              if (query.containsField("group")) {
                transformedMetrics = applyGroupedQueryToMetricAndPoints(query, where, metric);
              } else {
                transformedMetrics = new JsonArray()
                  .addObject(applyQueryToMetricAndPoints(query, where, metric));
              }

              JsonObject payload = new JsonObject();
              payload.putString("key", queryKey);
              payload.putArray("metrics", transformedMetrics);
              JsonObject newMessage = new JsonObject();
              newMessage.putString("type", "notify");
              newMessage.putObject("payload", payload);

              logger.info("Sending SockJS message " + newMessage);

              Buffer newMessageBuffer = new Buffer(newMessage.encode());
              socket.write(newMessageBuffer);
            } else {
              logger.info("Metric " + metricName + " does not match query " + queryKey);
            }
          }
          catch (InvalidExpressionException e) {
            logger.error("Invalid query", e);
          }
        }
      }
    }
  }

  private Map<String, Expression> getWhereFromQuery(JsonObject query) throws InvalidExpressionException {
    JsonObject whereJsonExpression = query.getObject("where");
    HashMap<String, Expression> where = new HashMap<>();

    if (whereJsonExpression != null) {
      for (String whereFieldName : whereJsonExpression.getFieldNames()) {
        where.put(whereFieldName, ExpressionFactory.fromJsonExpression(whereJsonExpression.getValue(whereFieldName)));
      }
    }

    return where;
  }

  private JsonObject applyQueryToMetricAndPoints(JsonObject query, Map<String, Expression> where, JsonObject metric) throws InvalidExpressionException {
    return applyQueryToMetric(query, metric)
      .putArray("points", applyQueryToPoints(query, where, metric.getArray("points")));
  }

  private JsonArray applyQueryToPoints(JsonObject query, Map<String, Expression> where, JsonArray points) throws InvalidExpressionException {
    JsonArray transformedPoints = new JsonArray();

    for (JsonObject point : new JsonArrayIterable<JsonObject>(points)) {
      if (pointMatchesQuery(point, where)) {
        transformedPoints.addObject(applyQueryToPoint(query, point));
      }
    }

    return transformedPoints;
  }

  private JsonArray applyGroupedQueryToMetricAndPoints(JsonObject query, Map<String, Expression> where, JsonObject metric) throws InvalidExpressionException {
    JsonArray pointGroups = applyGroupedQueryToPoints(query, where, metric.getArray("points"));
    JsonArray groupTransformedMetrics = new JsonArray();

    for (JsonObject pointGroup : new JsonArrayIterable<JsonObject>(pointGroups)) {
      JsonObject groupTransformedMetric = metric.copy().mergeIn(pointGroup);
      groupTransformedMetric = applyQueryToMetric(query, groupTransformedMetric)
        .putArray("points", pointGroup.getArray("points"));
      groupTransformedMetrics.addObject(groupTransformedMetric);
    }

    return groupTransformedMetrics;
  }

  private JsonArray applyGroupedQueryToPoints(JsonObject query, Map<String, Expression> where, JsonArray points) throws InvalidExpressionException {
    JsonObject pointProjection = query.getObject("point");
    JsonArray group = query.getArray("group");
    HashMap<ArrayList<Object>, JsonObject> transformedMetrics = new HashMap<>();

    for (JsonObject point : new JsonArrayIterable<JsonObject>(points)) {
      if (pointMatchesQuery(point, where)) {
        ArrayList<Object> groupKey = new ArrayList<>();

        for (String groupFieldName : new JsonArrayIterable<String>(group)) {
          groupKey.add(groupFieldName);
          groupKey.add(point.getValue(groupFieldName));
        }

        JsonObject transformedMetric = transformedMetrics.get(groupKey);
        JsonArray transformedPoints;

        if (transformedMetric == null) {
          transformedPoints = new JsonArray();
          transformedMetric = new JsonObject();

          for (String groupFieldName : new JsonArrayIterable<String>(group)) {
            transformedMetric.putValue(groupFieldName, point.getValue(groupFieldName));
          }

          transformedMetric.putArray("points", transformedPoints);
          transformedMetrics.put(groupKey, transformedMetric);
        }
        else {
          transformedPoints = transformedMetric.getArray("points");
        }

        transformedPoints.addObject(applyQueryToPoint(pointProjection, point));
      }
    }

    return convertCollectionToJsonArray(transformedMetrics.values());
  }

  private JsonArray convertCollectionToJsonArray(Collection<JsonObject> collection) {
    JsonArray jsonArray = new JsonArray();

    collection.forEach(jsonArray::addObject);

    return jsonArray;
  }

  private JsonObject applyQueryToMetric(JsonObject query, JsonObject metric) {
    JsonObject projection = query.getObject("metric");

    if (projection == null) {
      logger.info("No projection to apply to metric");
      return metric.copy();
    }

    logger.info("Applying projection " + projection + " to metric " + metric);
    JsonObject transformedMetric = new JsonObject();

    for (String projectionFieldName : projection.getFieldNames()) {
      String metricFieldName = projection.getString(projectionFieldName);
      transformedMetric.putValue(projectionFieldName, metric.getValue(metricFieldName));
    }

    return transformedMetric;
  }

  private JsonObject applyQueryToPoint(JsonObject query, JsonObject point) {
    JsonObject projection = query.getObject("point");

    if (projection == null) {
      return point.copy();
    }

    JsonObject transformedPoint = new JsonObject();

    for (String projectionFieldName : projection.getFieldNames()) {
      String pointFieldName = projection.getString(projectionFieldName);
      transformedPoint.putValue(projectionFieldName, point.getValue(pointFieldName));
    }

    return transformedPoint;
  }

  private boolean pointMatchesQuery(JsonObject point, Map<String, Expression> where) throws InvalidExpressionException {
    boolean isMatch = true;

    for (Map.Entry<String, Expression> fieldNameAndExpression : where.entrySet()) {
      String fieldName = fieldNameAndExpression.getKey();
      Expression expression = fieldNameAndExpression.getValue();
      Object pointFieldValue = point.getValue(fieldName);

      Object result = expression.evaluate(pointFieldValue);

      if (result == null || !(result instanceof Boolean)) {
        isMatch = false;
        break;
      }

      boolean booleanResult = (boolean)result;

      if (booleanResult == false) {
        isMatch = false;
        break;
      }
    }
    return isMatch;
  }

  private boolean metricMatchesQuery(String metricName, JsonObject query) {
    JsonArray fromItems = getFromItemsFromQuery(query);

    boolean isMatch = false;

    for (String fromItem : new JsonArrayIterable<String>(fromItems)) {
      if (fromItem.equals(metricName)) {
        isMatch = true;
        break;
      }
    }
    return isMatch;
  }

  private JsonArray getFromItemsFromQuery(JsonObject query) {
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

  private class SocketState {
    private HashMap<String, JsonObject> queries = new HashMap<>();

    public HashMap<String, JsonObject> queries() {
      return queries;
    }
  }

}
