package io.squarely.vertxspike.collectors;
/*
 * Copyright 2013 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * @author <a href="http://tfox.org">Tim Fox</a>
 */

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

import java.util.Random;

public class CollectorVerticle extends Verticle {

  public void start() {
    EventBus eventBus = vertx.eventBus();
    Random random = new Random();

    vertx.setPeriodic(10000, new Handler<Long>() {
          @Override
          public void handle(Long aLong) {
            eventBus.publish("data.serverMetrics", getServerMetrics(random));
            eventBus.publish("data.unitTestCodeCoverage", getUnitTestCodeCoverageMetrics());
          }

//        vertx.eventBus().registerHandler("ping-address", new Handler<Message<String>>() {
//          @Override
//          public void handle(Message<String> message) {
//            message.reply("pong!");
//            container.logger().info("Sent back pong");
//          }
//        });
        });

    container.logger().info("CollectorVerticle started");
  }

  private JsonObject getServerMetrics(Random random) {
    JsonArray items = new JsonArray();

    for (int i = 0; i < 10; i++) {
      String server = "example.server" + (i + 1);

      items.addObject(new JsonObject()
          .putString("server", server)
          .putString("what", "cpu_usage")
          .putString("type", "used")
          .putString("unit", "%")
          .putNumber("value", random.nextInt(10001) / 100.0));

      items.addObject(new JsonObject()
          .putString("server", server)
          .putString("what", "memory_usage")
          .putString("type", "used")
          .putString("unit", "B")
          .putNumber("value", (long)random.nextInt(16 * 1024) * 1024 * 1024));
    }

    JsonObject message = new JsonObject();
    message.putArray("metrics", items);
    return message;
  }

  private JsonObject getUnitTestCodeCoverageMetrics() {
    JsonArray items = new JsonArray();
    //DateTimeFormatter dateTimeFormatter = ISODateTimeFormat.dateTime();

    for (int i = 0; i < 4; i++) {
      String codebase = "codebase" + (i + 1);
      Random predictableRandom = new Random(i);

      JsonArray values = new JsonArray();
      double currentValue = predictableRandom.nextInt(10001) / 100.0;

      for (int dayIndex = 27; dayIndex >= 0; dayIndex--) {
        double delta = (predictableRandom.nextInt(1001) - 500) / 100.0;
        double nextValue = Math.max(Math.min(currentValue + delta, 100.0), 0.0);
        JsonObject value = new JsonObject();
        value.putNumber("timestamp", getTimestamp(getTodayMinusDays(dayIndex)));
        value.putNumber("value", nextValue);
        values.addObject(value);
      }

      items.addObject(new JsonObject()
          .putString("codebase", codebase)
          .putString("what", "unit_test_code_coverage")
          .putString("type", "branch_coverage")
          .putString("unit", "%")
          .putArray("values", values));
    }

    JsonObject message = new JsonObject();
    message.putArray("metrics", items);
    return message;
  }

  private DateTime getTodayMinusDays(int dayIndex) {
    return new DateTime().withTimeAtStartOfDay().minusDays(dayIndex);
  }

  private long getTimestamp(DateTime dt) {
    return dt.getMillis() / 1000;
  }

  private String getISODateString(DateTime dt, DateTimeFormatter dateTimeFormatter) {
    return dateTimeFormatter.print(dt);
  }
}
