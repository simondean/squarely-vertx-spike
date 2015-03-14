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
            eventBus.publish("data.serverMetrics", getServerMetrics());
          }

          private JsonObject getServerMetrics() {
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
            message.putArray("items", items);
            return message;
          }
        });
//        vertx.eventBus().registerHandler("ping-address", new Handler<Message<String>>() {
//          @Override
//          public void handle(Message<String> message) {
//            message.reply("pong!");
//            container.logger().info("Sent back pong");
//          }
//        });

    container.logger().info("CollectorVerticle started");
  }
}
