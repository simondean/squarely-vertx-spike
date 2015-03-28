var React   = require('react');
var ResponsiveReactGridLayout = require('react-grid-layout').Responsive;
var SockJS = require('sockjs-client');
var NumberTile = require('./NumberTile.jsx');
var LineChartTile = require('./LineChartTile.jsx');

module.exports = React.createClass({
  getInitialState: function() {
    return {
      number1: {},
      unitTestCodeCoverage: {
        data: null
      }
    };
  },
  componentDidMount: function() {
    self = this;
    self.socket = new SockJS('/events');

    self.socket.onopen = function() {
      var message = {
        command: 'listen',
        payload: {
          eventNames: ['number1', 'serverMetrics.cpuUsage', 'unitTestCodeCoverage']
        }
      };
      var messageText = JSON.stringify(message);
      console.log('Listen message: ' + messageText);
      self.socket.send(messageText);
    };

    self.socket.onmessage = function(e) {
      console.log('Received message', e.data);
      var message = JSON.parse(e.data);

      if (message.command === 'event') {
        if (message.eventName == 'unitTestCodeCoverage') {
          console.log(message);
          var labels = [];
          var dataSets = [];

          message.payload.metrics.forEach(function(metric) {
            var dataSet = {
              label: metric.codebase,
              data: []
            };
            metric.values.forEach(function(value) {
              dataSet.data.push({
                label: value.timestamp,
                value: value.value
              });
              if (labels.indexOf(value.timestamp) == -1) {
                labels.push(value.timestamp);
              }
            });
            dataSets.push(dataSet);
          });

          labels.sort();

          colors = [
            //'004358',
            //'1f8a70',
            //'bedb39',
            //'ffe11a',
            //'fd7400'

            'fdb432',
            '426efd',
            '26fd3d',
            'fd2f1f',
            '6865fd',
            'fdd136'
          ];

          colors = colors.map(function(color) {
            var rgb = parseInt(color.slice(0, 2), 16) + ', ' +
              parseInt(color.slice(2, 4), 16) + ', ' +
              parseInt(color.slice(4, 6), 16);
            return {
              strokeColor: 'rgba(' + rgb + ', 0.8)',
              fillColor: 'rgba(' + rgb + ', 0.4)'
            }
          });

          dataSets.forEach(function(dataSet, dataSetIndex) {
            var sortedData = [];
            dataSet.data.forEach(function(dataItem) {
              sortedData[labels.indexOf(dataItem.label)] = dataItem.value;
            });
            dataSet.data = sortedData;
            var colorIndex = dataSetIndex % colors.length;
            dataSet.strokeColor = colors[colorIndex].strokeColor;
            dataSet.fillColor = colors[colorIndex].fillColor;
          });

          labels = labels.map(function(timestamp) {
            var date = new Date(timestamp * 1000);
            return ('00' + (date.getMonth() + 1)).slice(-2) + '-' + ('00' + date.getDate()).slice(-2);
          });

          var partialState = {
            unitTestCodeCoverage: {
              data: {
                labels: labels,
                dataSets: dataSets
              }
            }
          };
          console.log('Set state', partialState);
          self.setState(partialState);
        }
        else if (message.eventName == 'number1') {
          var partialState = {
            number1: {
              value: message.payload.value,
              suffix: '%'
            }
          };
          console.log('Set state', partialState);
          self.setState(partialState);
        }
      }
    };

    self.socket.onclose = function() {
      // TODO: Something
      console.log('close');
    };
  },
  componentWillUnmount: function() {

  },
  render: function() {
    var breakpoints = {lg: 1200, md: 996, sm: 768, xs: 480};
    var cols = {lg: 12, md: 10, sm: 8, xs: 4};

    return (
      <ResponsiveReactGridLayout className="layout" breakpoints={breakpoints} cols={cols} rowHeight={30}>
        <div key={1} _grid={{x: 0, y: 0, w: 2, h: 6}}><NumberTile value={this.state.number1.value} suffix={this.state.number1.suffix} /></div>
        <div key={2} _grid={{x: 2, y: 0, w: 2, h: 6}}><NumberTile /></div>
        <div key={3} _grid={{x: 4, y: 0, w: 2, h: 6}}><NumberTile /></div>
        <div key={4} _grid={{x: 0, y: 6, w: 6, h: 6}}><LineChartTile data={this.state.unitTestCodeCoverage.data}/></div>
      </ResponsiveReactGridLayout>
    );
  }
});
