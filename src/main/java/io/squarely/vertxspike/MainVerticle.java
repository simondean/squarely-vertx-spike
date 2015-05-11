package io.squarely.vertxspike;

import org.vertx.java.core.Future;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

public class MainVerticle extends Verticle {
  public void start(final Future<Void> startedResult) {
    JsonObject config = container.config();

    container.deployVerticle("io.tiler.ServerVerticle", config.getObject("server"), 1, result -> {
      if (result.failed()) {
        startedResult.setFailure(result.cause());
        return;
      }

      container.deployModule("io.tiler~tiler-collector-jenkins~0.1.2", config.getObject("jenkins"), 1, result2 -> {
        if (result.failed()) {
          startedResult.setFailure(result.cause());
          return;
        }

        container.deployModule("io.tiler~tiler-collector-sonarqube~0.1.1", config.getObject("sonarQube"), 1, result3 -> {
          if (result.failed()) {
            startedResult.setFailure(result.cause());
            return;
          }

          container.logger().info("MainVerticle started");
        });
      });
    });
  }
}
