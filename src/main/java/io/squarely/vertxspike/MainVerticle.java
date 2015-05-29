package io.squarely.vertxspike;

import org.simondean.vertx.async.Series;
import org.vertx.java.core.Future;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.platform.Verticle;

import java.util.Map;

public class MainVerticle extends Verticle {
  private Logger logger;
  private Map<String, String> env;

  public void start(final Future<Void> startedResult) {
    logger = container.logger();
    env = container.env();
    JsonObject config = container.config();

    mergeConfigWithEnvironmentVariables(config);

    new Series<String>()
      .task(handler -> container.deployVerticle("io.tiler.ServerVerticle", config.getObject("server"), 1, handler))
      .task(handler -> container.deployModule("io.tiler~tiler-collector-jenkins~0.1.3", config.getObject("jenkins"), 1, handler))
      .task(handler -> container.deployModule("io.tiler~tiler-collector-sonarqube~0.1.2", config.getObject("sonarQube"), 1, handler))
      .task(handler -> container.deployModule("io.tiler~tiler-collector-loggly~0.1.2", config.getObject("loggly"), 1, handler))
      .run(result -> {
        if (result.failed()) {
          startedResult.setFailure(result.cause());
          return;
        }

        container.logger().info("MainVerticle started");
        startedResult.setResult(null);
      });
  }

  private void mergeConfigWithEnvironmentVariables(JsonObject config) {
    mergeConfigWithLogglyEnvironmentVariables(config);
  }

  private void mergeConfigWithLogglyEnvironmentVariables(JsonObject config) {
    JsonObject loggly = config.getObject("loggly");

    if (loggly == null) {
      logger.warn("Loggly is missing from the config");
    }

    JsonArray servers = loggly.getArray("servers");

    if (servers.size() == 0) {
      logger.warn("Loggly server missing from the config");
    }

    JsonObject server = servers.get(0);
    server.putString("host", getEnvironmentVariable("LOGGLY_HOST"));
    server.putString("username", getEnvironmentVariable("LOGGLY_USERNAME"));
    server.putString("password", getEnvironmentVariable("LOGGLY_PASSWORD"));
  }

  private String getEnvironmentVariable(String name) {
    String value = env.get(name);

    if (value == null) {
      logger.warn("Environment variable '" + name + "' has not been set");
    }

    return value;
  }
}
