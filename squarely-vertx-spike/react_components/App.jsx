var React = require('react');
var Dashboard = require('./Dashboard.jsx');
var NumberTile = require('./NumberTile.jsx');
var LineChartTile = require('./LineChartTile.jsx');

var breakpoints = {lg: 1200, md: 996, sm: 768, xs: 480};
var cols = {lg: 12, md: 10, sm: 8, xs: 4};

React.render(
  <Dashboard breakpoints={breakpoints} cols={cols} rowHeight={30}>
    <NumberTile key={1} _grid={{x: 0, y: 0, w: 2, h: 6}}
      query={{point: {value: 'value'}, from: 'ci.sonarqube.coverage', where: {projectKey: 'org.apache.archiva:archiva'}}}
      suffix={'%'} />
    <NumberTile key={2} _grid={{x: 2, y: 0, w: 2, h: 6}} />
    <LineChartTile key={3} _grid={{x: 0, y: 6, w: 6, h: 6}}
      query={{metric: {label: 'projectKey'}, point: {value: 'value'}, from: 'ci.sonarqube.coverage', group: ['projectKey']}} />
  </Dashboard>,
  document.getElementById('content')
);
