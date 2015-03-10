package io.squarely.vertxspike;

import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.sockjs.SockJSServer;
import org.vertx.java.core.sockjs.SockJSSocket;
import org.vertx.java.platform.Verticle;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

public class SquarelyVerticle extends Verticle {
  private final HashMap<String, HashSet<SockJSSocket>> eventNamesToSockets = new HashMap<String, HashSet<SockJSSocket>>();
  private final HashMap<SockJSSocket, HashSet<String>> socketsToEventNames = new HashMap<SockJSSocket, HashSet<String>>();

  public void start() {
    final Logger log = container.logger();

    HttpServer httpServer = vertx.createHttpServer();

    httpServer.requestHandler(new Handler<HttpServerRequest>() {
      public void handle(HttpServerRequest request) {
        String file = "";
        if (request.path().equals("/")) {
          file = "index.html";
        } else if (!request.path().contains("..")) {
          file = request.path();
        }
        request.response().sendFile("web/" + file, "web/index.html");
      }
    });

    SockJSServer sockJSServer = vertx.createSockJSServer(httpServer);

    // TODO: Set the library_url value
    JsonObject sockJSConfig = new JsonObject().putString("prefix", "/events");

    sockJSServer.installApp(sockJSConfig, new Handler<SockJSSocket>() {
      public void handle(final SockJSSocket socket) {
        socket.dataHandler(new Handler<Buffer>() {
          public void handle(Buffer buffer) {
            JsonObject message = new JsonObject(buffer.toString());
            String command = message.getString("command");

            if ("listen".equals(command)) {
              JsonObject payload = message.getObject("payload");
              String eventName = payload.getString("eventName");
              log.info("Received listen request for event: " + eventName);

              addListener(eventName, socket);
            }
          }
        });

        socket.endHandler(new Handler<Void>() {
          public void handle(Void aVoid) {
            log.info("Removing listener");
            removeListener(socket);
          }
        });
      }
    });

    httpServer.listen(8080);

    final Random random = new Random();

    vertx.setPeriodic(10000, new Handler<Long>() {
      public void handle(Long timerID) {
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
      }
    });

//    vertx.eventBus().registerHandler("ping-address", new Handler<Message<String>>() {
//      @Override
//      public void handle(Message<String> message) {
//        message.reply("pong!");
//        container.logger().info("Sent back pong");
//      }
//    });
//
//    container.logger().info("SquarelyVerticle started");
  }

  private void addListener(String eventName, SockJSSocket socket) {
    mapEventNameToSocket(eventName, socket);
    mapSocketToEventName(eventName, socket);
  }

  private void mapEventNameToSocket(String eventName, SockJSSocket socket) {
    HashSet<SockJSSocket> sockets = eventNamesToSockets.get(eventName);

    if (sockets == null) {
      sockets = new HashSet<SockJSSocket>();
      eventNamesToSockets.put(eventName, sockets);
    }

    if (!sockets.contains(socket)) {
      sockets.add(socket);
    }
  }

  private void mapSocketToEventName(String eventName, SockJSSocket socket) {
    HashSet<String> eventNames = socketsToEventNames.get(socket);

    if (eventNames == null) {
      eventNames = new HashSet<String>();
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
