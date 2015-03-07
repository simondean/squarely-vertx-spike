var React   = require('react');

module.exports = React.createClass({
  render: function() {
    var style = {
      color: '#ffffff',
      backgroundColor: '#1e1e1e'
    }

    return (
      <div className="number" style={style}>
        Hello, world! I am a Number.
      </div>
    );
  }
});
