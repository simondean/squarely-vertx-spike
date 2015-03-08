var React   = require('react');

module.exports = React.createClass({
  render: function() {
    var style = {
      color: '#ffffff',
      backgroundColor: '#1e1e1e',
      height: '100%'
    };

    var value = 10;

    return (
      <div className="number" style={style}>
        <span>{value}</span>
      </div>
    );
  }
});
