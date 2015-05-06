package io.squarely.vertxspike;

import org.joda.time.DateTime;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

import java.util.List;

public class JenkinsCollectorVerticle extends CollectorVerticle {
  private Logger logger;
  private JsonObject config;
  private EventBus eventBus;
  private HttpClient httpClient;

  public void start() {
    logger = container.logger();
    config = container.config();
    eventBus = vertx.eventBus();
    httpClient = vertx.createHttpClient()
      .setHost(getJenkinsHost())
      .setPort(getJenkinsPort())
      .setSSL(getJenkinsSsl())
      .setTryUseCompression(true);
    // Get the following error without turning keep alive off.  Looks like a vertx bug
    // SEVERE: Exception in Java verticle
    // java.nio.channels.ClosedChannelException
    httpClient.setKeepAlive(false);

    final boolean[] isRunning = {true};

    collect(aVoid -> {
      isRunning[0] = false;
    });

    vertx.setPeriodic(3600000, aLong -> {
      if (isRunning[0]) {
        logger.info("Collection aborted as previous run still executing");
        return;
      }

      isRunning[0] = true;

      collect(aVoid -> {
        isRunning[0] = false;
      });
    });

    logger.info("JenkinsCollectorVerticle started");
  }

  private boolean getJenkinsSsl() {
    return config.getBoolean("ssl", false);
  }

  private Integer getJenkinsPort() {
    return config.getInteger("port", 8080);
  }

  private String getJenkinsHost() {
    return config.getString("host", "localhost");
  }

  private Integer getJobLimit() {
    return config.getInteger("jobLimit", 10);
  }

  private void collect(Handler<Void> handler) {
    logger.info("Collection started");
    getJobs(getJobLimit(), projects -> {
      transformMetrics(projects, metrics -> {
        publishNewMetrics(metrics, aVoid -> {
          logger.info("Collection finished");
          handler.handle(null);
        });
      });
    });
  }

  private void getJobs(int jobLimit, Handler<JsonArray> handler) {
    httpClient.getNow("/api/json?pretty=true", response -> {
      response.bodyHandler(body -> {
        logger.info("Received jobs " + body);
        JsonArray jobs = new JsonObject(body.toString()).getArray("jobs");

        if (jobs == null) {
          logger.error("Could not retrieve jobs");
          jobs = new JsonArray();
        }

        logger.info("Received " + jobs.size() + " jobs");
        logger.info("Jobs limit set to " + jobLimit);

        List jobList = jobs.toList();
        int jobCount = jobList.size();

        while (jobCount > jobLimit) {
          jobCount--;
          jobList.remove(jobCount);
        }

        jobs = new JsonArray(jobList);
        logger.info("There are " + jobs.size() + " jobs after limiting");

        handler.handle(jobs);
      });
    });
  }

  private void transformMetrics(JsonArray jobs, Handler<JsonArray> handler) {
    logger.info("Transforming metrics");
    long time = getCurrentMillisTimestamp();

    JsonArray newPoints = new JsonArray();
    JsonObject newMetric = new JsonObject()
      .putString("name", "ci.jenkins.job_color")
      .putArray("points", newPoints)
      .putNumber("timestamp", getCurrentMillisTimestamp());

    for (int jobIndex = 0, jobCount = jobs.size(); jobIndex < jobCount; jobIndex++) {
      JsonObject job = jobs.get(jobIndex);
      String jobName = job.getString("name");
      String jobColor = job.getString("color");

      newPoints.addObject(new JsonObject()
        .putNumber("time", time)
        .putString("jobName", jobName)
        .putString("value", jobColor));
    }

    JsonArray newMetrics = new JsonArray();
    newMetrics.addObject(newMetric);

    handler.handle(newMetrics);
  }

  private long getCurrentMillisTimestamp() {
    return DateTime.now().getMillis();
  }

  private void publishNewMetrics(JsonArray metrics, Handler<Void> handler) {
    logger.info("Publishing metrics to event bus");
    logger.info("New metrics " + metrics);
    JsonObject message = new JsonObject()
      .putArray("metrics", metrics);
    eventBus.publish("io.squarely.vertxspike.metrics", message);
    handler.handle(null);
  }
}
