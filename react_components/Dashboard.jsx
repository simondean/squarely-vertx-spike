var React   = require('react');
var ResponsiveReactGridLayout = require('react-grid-layout').Responsive;
var Number = require('./Number.jsx');

module.exports = React.createClass({
  render: function() {
    var style = {
      color: '#ffffff',
      backgroundColor: '#1e1e1e',
      height: '100%'
    };

    var breakpoints = {lg: 1200, md: 996, sm: 768, xs: 480};
    var cols = {lg: 12, md: 10, sm: 8, xs: 4};

    return (
      <ResponsiveReactGridLayout className="layout" breakpoints={breakpoints} cols={cols} rowHeight={30}>
        <div key={1} _grid={{x: 0, y: 0, w: 2, h: 6}}><Number /></div>
        <div key={2} _grid={{x: 2, y: 0, w: 2, h: 6}}><Number /></div>
        <div key={3} _grid={{x: 4, y: 0, w: 2, h: 6}}><Number /></div>
      </ResponsiveReactGridLayout>
    );
  }
});
