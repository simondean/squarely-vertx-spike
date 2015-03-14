package io.squarely.vertxspike;

import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonElement;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.sockjs.SockJSServer;
import org.vertx.java.core.sockjs.SockJSSocket;
import org.vertx.java.platform.Verticle;

import java.util.*;

public class SquarelyVerticle extends Verticle {
  private final HashMap<String, HashSet<SockJSSocket>> eventNamesToSockets = new HashMap<>();
  private final HashMap<SockJSSocket, HashSet<String>> socketsToEventNames = new HashMap<>();

  public void start() {
    final Logger log = container.logger();

    HttpServer httpServer = vertx.createHttpServer();

    httpServer.requestHandler(request -> {
      String file = "";
      if (request.path().equals("/")) {
        file = "index.html";
      } else if (!request.path().contains("..")) {
        file = request.path();
      }
      request.response().sendFile("web/" + file, "web/index.html");
    });

    SockJSServer sockJSServer = vertx.createSockJSServer(httpServer);

    // TODO: Set the library_url value
    JsonObject sockJSConfig = new JsonObject().putString("prefix", "/events");

    sockJSServer.installApp(sockJSConfig, socket -> {
      socket.dataHandler(buffer -> {
        JsonObject message = new JsonObject(buffer.toString());
        String command = message.getString("command");

        if ("listen".equals(command)) {
          JsonObject payload = message.getObject("payload");
          JsonArray eventNames = payload.getArray("eventNames");
          ArrayList<String> eventNames2 = new ArrayList<>();

          for (Object eventName : eventNames) {
            String eventName2 = (String)eventName;
            log.info("Received listen request for event: " + eventName2);
            eventNames2.add(eventName2);
          }

          addListeners(eventNames2, socket);
        }
      });

      socket.endHandler(aVoid -> {
        log.info("Removing listener");
        removeListener(socket);
      });
    });

    httpServer.listen(8080);

    final Random random = new Random();

    vertx.setPeriodic(10000, timerID -> {
      String eventName = "number1";

      JsonObject payload = new JsonObject();
      payload.putNumber("value", random.nextInt(100));
      payload.putString("suffix", "%");
      JsonObject message = new JsonObject();
      message.putString("command", "event");
      message.putString("eventName", eventName);
      message.putObject("payload", payload);

      String messageText = message.encode();
      Buffer messageBuffer = new Buffer(messageText);

      HashSet<SockJSSocket> sockets = eventNamesToSockets.get(eventName);

      int socketCount = 0;

      if (sockets != null) {
        for (SockJSSocket socket : sockets) {
          socket.write(messageBuffer);
        }

        socketCount = sockets.size();
      }

      log.info("Listening sockets: " + socketCount);
    });

    vertx.eventBus().registerHandler("data.serverMetrics", (Message<JsonObject> message) -> {
      log.info("Received " + message.address() + " message");
      HashSet<SockJSSocket> sockets = eventNamesToSockets.get("serverMetrics.cpuUsage");

      int socketCount = 0;

      if (sockets != null) {
        JsonArray metrics = new JsonArray();

        for (Object metric : message.body().getArray("metrics")) {
          JsonObject jsonMetric = (JsonObject) metric;

          if ("cpu_usage".equals(jsonMetric.getString("what"))) {
            metrics.add(jsonMetric);
          }
        }

        JsonObject payload = new JsonObject();
        payload.putArray("metrics", metrics);
        JsonObject newMessage = new JsonObject();
        newMessage.putString("command", "event");
        newMessage.putString("eventName", "serverMetrics");
        newMessage.putObject("payload", payload);

        String newMessageText = newMessage.encode();
        Buffer newMessageBuffer = new Buffer(newMessageText);

        for (SockJSSocket socket : sockets) {
          socket.write(newMessageBuffer);
        }

        socketCount = sockets.size();
      }

      log.info("Listening sockets: " + socketCount);
    });

    vertx.eventBus().registerHandler("data.unitTestCodeCoverage", (Message<JsonObject> message) -> {
      log.info("Received " + message.address() + " message");
      HashSet<SockJSSocket> sockets = eventNamesToSockets.get("unitTestCodeCoverage");

      int socketCount = 0;

      if (sockets != null) {
        JsonArray metrics = new JsonArray();

        for (Object metric : message.body().getArray("metrics")) {
          JsonObject jsonMetric = (JsonObject)metric;

          if ("unit_test_code_coverage".equals(jsonMetric.getString("what")) &&
              "branch_coverage".equals(jsonMetric.getString("type"))) {
            metrics.add(jsonMetric);
          }
        }

        JsonObject payload = new JsonObject();
        payload.putArray("metrics", metrics);
        JsonObject newMessage = new JsonObject();
        newMessage.putString("command", "event");
        newMessage.putString("eventName", "unitTestCodeCoverage");
        newMessage.putObject("payload", payload);

        String newMessageText = newMessage.encode();
        Buffer newMessageBuffer = new Buffer(newMessageText);

        for (SockJSSocket socket : sockets) {
          socket.write(newMessageBuffer);
        }

        socketCount = sockets.size();
      }

      log.info("Listening sockets: " + socketCount);
    });

    container.logger().info("SquarelyVerticle started");
  }

  private void addListeners(List<String> eventNames, SockJSSocket socket) {
    for (String eventName : eventNames) {
      mapEventNameToSocket(eventName, socket);
      mapSocketToEventName(eventName, socket);
    }
  }

  private void mapEventNameToSocket(String eventName, SockJSSocket socket) {
    HashSet<SockJSSocket> sockets = eventNamesToSockets.get(eventName);

    if (sockets == null) {
      sockets = new HashSet<>();
      eventNamesToSockets.put(eventName, sockets);
    }

    if (!sockets.contains(socket)) {
      sockets.add(socket);
    }
  }

  private void mapSocketToEventName(String eventName, SockJSSocket socket) {
    HashSet<String> eventNames = socketsToEventNames.get(socket);

    if (eventNames == null) {
      eventNames = new HashSet<>();
      socketsToEventNames.put(socket, eventNames);
    }

    if (!eventNames.contains(eventName)) {
      eventNames.add(eventName);
    }
  }

  private void removeListener(SockJSSocket socket) {
    HashSet<String> eventNames = socketsToEventNames.remove(socket);

    if (eventNames != null) {
      for (String eventName : eventNames) {
        unmapSocketFromEventName(eventName, socket);
      }
    }
  }

  private void unmapSocketFromEventName(String eventName, SockJSSocket socket) {
    HashSet<SockJSSocket> sockets = eventNamesToSockets.get(eventName);
    sockets.remove(socket);

    if (sockets.isEmpty()) {
      eventNamesToSockets.remove(eventName);
    }
  }
}
