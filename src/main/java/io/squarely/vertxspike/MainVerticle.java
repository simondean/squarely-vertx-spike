package io.squarely.vertxspike;

import org.simondean.vertx.async.Series;
import org.vertx.java.core.Future;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

public class MainVerticle extends Verticle {
  public void start(final Future<Void> startedResult) {
    JsonObject config = container.config();

    new Series<String>()
      .task(handler -> container.deployVerticle("io.tiler.ServerVerticle", config.getObject("server"), 1, handler))
      .task(handler -> container.deployModule("io.tiler~tiler-collector-jenkins~0.1.2", config.getObject("jenkins"), 1, handler))
      .task(handler -> container.deployModule("io.tiler~tiler-collector-sonarqube~0.1.1", config.getObject("sonarQube"), 1, handler))
      .run(result -> {
        if (result.failed()) {
          startedResult.setFailure(result.cause());
          return;
        }

        container.logger().info("MainVerticle started");
        startedResult.setResult(null);
      });
  }
}
