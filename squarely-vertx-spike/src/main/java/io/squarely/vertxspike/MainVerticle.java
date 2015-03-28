package io.squarely.vertxspike;

import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

public class MainVerticle extends Verticle {
  public static final String REDIS_ADDRESS = "io.squarely.vertxspike.redis";

  public void start() {
    JsonObject config = new JsonObject()
      .putString("address", REDIS_ADDRESS)
      .putString("host", "localhost")
      .putNumber("port", 6379);

    container.deployModule("io.vertx~mod-redis~1.1.4", config, 1);

    container.deployVerticle("io.squarely.vertxspike.ServerVerticle");
    container.deployVerticle("io.squarely.vertxspike.JenkinsCollectorVerticle");
    container.deployVerticle("io.squarely.vertxspike.SonarQubeCollectorVerticle");

    container.logger().info("MainVerticle started");
  }
}
