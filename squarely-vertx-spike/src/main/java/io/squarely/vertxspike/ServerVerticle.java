package io.squarely.vertxspike;

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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

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
                metrics.addObject(new JsonObject(redisValue));
              }

              publishMetrics(metrics);
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

      publishMetrics(metrics);
    });

    container.logger().info("ServerVerticle started");
  }

  private void publishMetrics(JsonArray metrics) {
    for (JsonObject metric : new JsonArrayIterable<JsonObject>(metrics)) {
      for (Map.Entry<SockJSSocket, SocketState> socketAndSocketState : socketStates.entrySet()) {
        SockJSSocket socket = socketAndSocketState.getKey();
        SocketState socketState = socketAndSocketState.getValue();

        for (Map.Entry<String, JsonObject> keyAndQuery : socketState.queries().entrySet()) {
          JsonObject query = keyAndQuery.getValue();
          String queryKey = keyAndQuery.getKey();
          String metricName = metric.getString("name");

          if (metricMatchesQuery(metricName, query)) {
            logger.info("Metric " + metricName + " does match query " + queryKey);

            JsonArray transformedMetrics;

            if (query.containsField("group")) {
              transformedMetrics = applyGroupedQueryToMetricAndPoints(query, metric);
            }
            else {
              transformedMetrics = new JsonArray()
                .addObject(applyQueryToMetricAndPoints(query, metric));
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
          }
          else {
            logger.info("Metric " + metricName + " does not match query " + queryKey);
          }
        }
      }
    }
  }

  private JsonObject applyQueryToMetricAndPoints(JsonObject query, JsonObject metric) {
    return applyQueryToMetric(query, metric)
      .putArray("points", applyQueryToPoints(query, metric.getArray("points")));
  }

  private JsonArray applyQueryToPoints(JsonObject query, JsonArray points) {
    JsonObject where = query.getObject("where");
    JsonArray transformedPoints = new JsonArray();

    for (JsonObject point : new JsonArrayIterable<JsonObject>(points)) {
      if (pointMatchesQuery(where, point)) {
        transformedPoints.addObject(applyQueryToPoint(query, point));
      }
    }

    return transformedPoints;
  }

  private JsonArray applyGroupedQueryToMetricAndPoints(JsonObject query, JsonObject metric) {
    JsonArray pointGroups = applyGroupedQueryToPoints(query, metric.getArray("points"));
    JsonArray groupTransformedMetrics = new JsonArray();

    for (JsonObject pointGroup : new JsonArrayIterable<JsonObject>(pointGroups)) {
      JsonObject groupTransformedMetric = metric.copy().mergeIn(pointGroup);
      groupTransformedMetric = applyQueryToMetric(query, groupTransformedMetric)
        .putArray("points", pointGroup.getArray("points"));
      groupTransformedMetrics.addObject(groupTransformedMetric);
    }

    return groupTransformedMetrics;
  }

  private JsonArray applyGroupedQueryToPoints(JsonObject query, JsonArray points) {
    JsonObject where = query.getObject("where");
    JsonObject pointProjection = query.getObject("point");
    JsonArray group = query.getArray("group");
    HashMap<ArrayList<Object>, JsonObject> transformedMetrics = new HashMap<>();

    for (JsonObject point : new JsonArrayIterable<JsonObject>(points)) {
      if (pointMatchesQuery(where, point)) {
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

  private boolean pointMatchesQuery(JsonObject where, JsonObject point) {
    if (where == null) {
      return true;
    }

    boolean isMatch = true;

    for (String whereFieldName : where.getFieldNames()) {
      Object whereFieldValue = where.getValue(whereFieldName);
      Object pointFieldValue = point.getValue(whereFieldName);

      if (whereFieldValue == null) {
        if (pointFieldValue != null) {
          isMatch = false;
          break;
        }
      }
      else {
        if (!whereFieldValue.equals(pointFieldValue)) {
          isMatch = false;
          break;
        }
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