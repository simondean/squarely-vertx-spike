package io.squarely.vertxspike;

import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

public class SquarelyVerticle extends Verticle {

  public void start() {
    final Logger log = container.logger();

    vertx.createHttpServer()
      .requestHandler(new Handler<HttpServerRequest>() {
        public void handle(HttpServerRequest request) {
          String file = "";
          if (request.path().equals("/")) {
            file = "index.html";
          } else if (!request.path().contains("..")) {
            file = request.path();
          }
          request.response().sendFile("web/" + file, "web/index.html");
        }
      })
      .listen(8080);

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
}
