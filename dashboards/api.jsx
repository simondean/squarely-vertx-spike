var React = require('react');
var Dashboard = require('tiler').Dashboard;
var ListTile = require('../components/ListTile.jsx');

var breakpoints = {lg: 1200, md: 996, sm: 768, xs: 480};
var cols = {lg: 12, md: 10, sm: 8, xs: 4};

React.render(
  <Dashboard breakpoints={breakpoints} cols={cols} rowHeight={30}>
    <ListTile key={2} _grid={{x: 0, y: 0, w: 8, h: 26}} title={'Metrics'} ordered={false}
      query={{
        metric: {label: 'name'},
        point: {label: 'time', value: 'value'},
        from: 'examples.api'
      }} />
  </Dashboard>,
  document.getElementById('content')
);
