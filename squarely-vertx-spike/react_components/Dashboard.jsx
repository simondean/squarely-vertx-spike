var React   = require('react/addons');
var ResponsiveReactGridLayout = require('react-grid-layout').Responsive;
var SockJS = require('sockjs-client');

module.exports = React.createClass({
  getInitialState: function() {
    var queries = {};

    React.Children.forEach(this.props.children, function(child) {
      var query = child.props.query;

      if (query) {
        var key = child.key;
        queries[key] = query;
      }
    }, this);

    return {
      queries: queries,
      metrics: {}
    };
  },
  componentDidMount: function() {
    self = this;
    self.socket = new SockJS('/events');

    self.socket.onopen = function() {
      //var message = {
      //  command: 'listen',
      //  payload: {
      //    eventNames: ['number1', 'serverMetrics.cpuUsage', 'unitTestCodeCoverage']
      //  }
      //};
      //var messageText = JSON.stringify(message);
      //console.log('Listen message: ' + messageText);
      //self.socket.send(messageText);

      console.log('Subscribing to metrics')

      var message = {
        type: 'subscribe',
        payload: {
          queries: self.state.queries
        }
      };
      var message = JSON.stringify(message);
      console.log('Listen message: ' + message);
      self.socket.send(message);
    };

    self.socket.onmessage = function(e) {
      console.log('Received message', e.data);
      var message = JSON.parse(e.data);

      if (message.type === 'notify') {
        self.setState(function(previousState, currentProps) {
          var previousMetrics = previousState.metrics;
          var nextMetrics = {};
          Object.keys(previousMetrics).forEach(function (key) {
            nextMetrics[key] = previousMetrics[key];
          });
          nextMetrics[message.payload.key] = message.payload.metrics;
          var nextState = {
            metrics: nextMetrics
          };
          console.log('Set state', nextState);
          return nextState;
        });

        //var labels = [];
        //var dataSets = [];
        //
        //message.payload.metrics.forEach(function(metric) {
        //  var dataSet = {
        //    label: metric.codebase,
        //    data: []
        //  };
        //  metric.values.forEach(function(value) {
        //    dataSet.data.push({
        //      label: value.timestamp,
        //      value: value.value
        //    });
        //    if (labels.indexOf(value.timestamp) == -1) {
        //      labels.push(value.timestamp);
        //    }
        //  });
        //  dataSets.push(dataSet);
        //});
        //
        //labels.sort();
        //
        //colors = [
        //  //'004358',
        //  //'1f8a70',
        //  //'bedb39',
        //  //'ffe11a',
        //  //'fd7400'
        //
        //  'fdb432',
        //  '426efd',
        //  '26fd3d',
        //  'fd2f1f',
        //  '6865fd',
        //  'fdd136'
        //];
        //
        //colors = colors.map(function(color) {
        //  var rgb = parseInt(color.slice(0, 2), 16) + ', ' +
        //    parseInt(color.slice(2, 4), 16) + ', ' +
        //    parseInt(color.slice(4, 6), 16);
        //  return {
        //    strokeColor: 'rgba(' + rgb + ', 0.8)',
        //    fillColor: 'rgba(' + rgb + ', 0.4)'
        //  }
        //});
        //
        //dataSets.forEach(function(dataSet, dataSetIndex) {
        //  var sortedData = [];
        //  dataSet.data.forEach(function(dataItem) {
        //    sortedData[labels.indexOf(dataItem.label)] = dataItem.value;
        //  });
        //  dataSet.data = sortedData;
        //  var colorIndex = dataSetIndex % colors.length;
        //  dataSet.strokeColor = colors[colorIndex].strokeColor;
        //  dataSet.fillColor = colors[colorIndex].fillColor;
        //});
        //
        //labels = labels.map(function(timestamp) {
        //  var date = new Date(timestamp * 1000);
        //  return ('00' + (date.getMonth() + 1)).slice(-2) + '-' + ('00' + date.getDate()).slice(-2);
        //});
        //
        //var partialState = {
        //  unitTestCodeCoverage: {
        //    data: {
        //      labels: labels,
        //      dataSets: dataSets
        //    }
        //  }
        //};
        //console.log('Set state', partialState);
        //self.setState(partialState);
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
    var newChildren = [];

    React.Children.forEach(this.props.children, function(child) {
      var metrics = this.state.metrics[child.key];
      console.log('Setting child metrics to', metrics)
      var newChild = React.addons.cloneWithProps(child, {metrics: metrics});
      var div = <div key={child.key} _grid={child.props._grid}>{newChild}</div>;
      newChildren.push(div);
    }, this);

    return (
      <ResponsiveReactGridLayout breakpoints={this.props.breakpoints} cols={this.props.cols} rowHeight={this.props.rowHeight}>
        {newChildren}
      </ResponsiveReactGridLayout>
    );
  }
});
