package io.squarely.vertxspike;

import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

public class MainVerticle extends Verticle {
  public void start() {
    JsonObject config = container.config();

    container.deployModule("io.tiler~tiler~0.1.2", config.getObject("server"), 1);
    container.deployVerticle("io.squarely.vertxspike.JenkinsCollectorVerticle", config.getObject("jenkins"), 1);
    container.deployVerticle("io.squarely.vertxspike.SonarQubeCollectorVerticle", config.getObject("sonarQube"), 1);

    container.logger().info("MainVerticle started");
  }
}
