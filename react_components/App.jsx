var React   = require('react');
var ReactGridLayout = require('react-grid-layout');
var Number = require('./Number.jsx');

React.render(
  <ReactGridLayout className="layout" cols={12} rowHeight={30}>
    <div key={1} _grid={{x: 0, y: 0, w: 1, h: 2}}><Number /></div>
    <div key={2} _grid={{x: 1, y: 0, w: 1, h: 2}}><Number /></div>
    <div key={3} _grid={{x: 2, y: 0, w: 1, h: 2}}><Number /></div>
  </ReactGridLayout>,
  document.getElementById('content')
);
