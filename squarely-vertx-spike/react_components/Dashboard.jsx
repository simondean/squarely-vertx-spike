var React   = require('react');
var ResponsiveReactGridLayout = require('react-grid-layout').Responsive;
var SockJS = require('sockjs-client');
var Number = require('./Number.jsx');

module.exports = React.createClass({
  getInitialState: function() {
    return {number1: {}};
  },
  componentDidMount: function() {
    self = this;
    self.socket = new SockJS('/events');

    self.socket.onopen = function() {
      var message = {
        command: 'listen',
        payload: {
          eventNames: ['number1', 'serverMetrics.cpuUsage']
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
        var partialState = {};
        partialState[message.eventName] = message.payload;
        console.log('Set state', partialState);
        self.setState(partialState);
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
        <div key={1} _grid={{x: 0, y: 0, w: 2, h: 6}}><Number value={this.state.number1.value} suffix={this.state.number1.suffix} /></div>
        <div key={2} _grid={{x: 2, y: 0, w: 2, h: 6}}><Number /></div>
        <div key={3} _grid={{x: 4, y: 0, w: 2, h: 6}}><Number /></div>
      </ResponsiveReactGridLayout>
    );
  }
});
