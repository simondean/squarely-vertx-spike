# squarely-vertx-spike

## Quick Start

Prerequisites:
  * Install Oracle JDK 1.8+
  * Install Maven 3.2+
  * Install node.js 0.10+
  * Install Redis 2.8+

Run:
  * $ npm install
  * $ npm install -g grunt-cli
  * $ grunt build
  * $ mvn package vertx:runMod
  * Open http://localhost:8080/dashboards/sample in a browser

## API

### Create Metrics

HTTP POST http://localhost:8080/api/metrics

Headers:
  * Content-Type: application/json

Request Body:

``` json
{
    "metrics": [{
        "name": "examples.api",
        "points": [{
            "time": 1,
            "value": 10
        },
        {
            "time": 2,
            "value": 20
        }]
    }]
}
```

View the metric by browsing to http://localhost:8080/dashboards/api

## To Dos
  * Replace milliseconds with microseconds
  * Support more than 1 Jenkins server with the Jenkins collector
  * Support more than 1 SonarQube server with the SonarQube collector
  * Reconnect SockJS connection in the browser when connection is lost.  Include auto retries if not successful
  * Create integration tests
  * Check whether static resources should be moved to src/site/ which is a directory the maven archetype creates
  * Pull common functionality of collectors out into a base class
  * Switch from Grunt to Gulp

## Useful Resources
  * http://vertx.io/docs.html
  * http://vertx.io/core_manual_java.html
  * http://vertx.io/dev_guide.html
  * http://facebook.github.io/react/
  * https://github.com/STRML/react-grid-layout
  * https://speakerdeck.com/vjeux/react-css-in-js
  * http://simonsmith.io/writing-react-components-as-commonjs-modules/
  * http://blog.fusioncharts.com/2014/09/comparing-jquery-grid-plugins-masonry-vs-isotope-vs-packery-vs-gridster-vs-shapeshift-vs-shuffle-js/
  * http://nemo.sonarqube.org/
  * http://www.perceptualedge.com/articles/visual_business_intelligence/rules_for_using_color.pdf
  * http://colorbrewer2.org/
  * http://fortawesome.github.io/Font-Awesome/cheatsheet/
  * http://pmlopes.github.io/yoke/reference/java.html
  * https://github.com/pmlopes/yoke/blob/develop/examples/kitcms/src/main/java/com/jetdrone/vertx/kitcms/KitCMS.java