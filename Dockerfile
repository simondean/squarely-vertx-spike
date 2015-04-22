FROM maven:3.3.1-jdk-8
RUN apt-get update && apt-get install -y npm nodejs nodejs-legacy redis-server
WORKDIR /opt
RUN mkdir squarely-vertx-spike
WORKDIR squarely-vertx-spike
ADD . .
WORKDIR squarely-vertx-spike
RUN npm install
RUN npm install -g grunt-cli
RUN grunt build
EXPOSE 8080
CMD redis-server --daemonize yes && mvn package vertx:runMod
