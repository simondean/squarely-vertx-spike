package io.squarely.vertxspike;

import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

public class MainVerticle extends Verticle {
  public static final String REDIS_ADDRESS = "io.squarely.vertxspike.redis";

  public void start() {
    JsonObject config = new JsonObject()
      .putObject("redisConfig", new JsonObject()
        .putString("address", REDIS_ADDRESS)
        .putString("host", "localhost")
        .putNumber("port", 6379))
      .putObject("jenkinsConfig", new JsonObject()
        .putString("jenkinsHost", "builds.apache.org")
        .putNumber("jenkinsPort", 443)
        .putBoolean("jenkinsSsl", true)
        .putNumber("jobLimit", 1000))
      .putObject("sonarQubeConfig", new JsonObject()
        .putString("sonarQubeHost", "sonar.spring.io")
        .putNumber("sonarQubePort", 443)
        .putBoolean("sonarQubeSsl", true)
        .putNumber("projectLimit", 1000));

    container.deployModule("io.vertx~mod-redis~1.1.4", config.getObject("redisConfig"), 1);
    container.deployVerticle("io.squarely.vertxspike.ServerVerticle");
    container.deployVerticle("io.squarely.vertxspike.JenkinsCollectorVerticle", config.getObject("jenkinsConfig"));
    container.deployVerticle("io.squarely.vertxspike.SonarQubeCollectorVerticle", config.getObject("sonarQubeConfig"));

    container.logger().info("MainVerticle started");
  }
}
