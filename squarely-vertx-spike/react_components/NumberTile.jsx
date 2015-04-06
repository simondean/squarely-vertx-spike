var React = require('react');

module.exports = React.createClass({
  render: function() {
    var styles = {
      container: {
        color: '#ffffff',
        backgroundColor: '#1e1e1e',
        height: '100%'
      },
      valueLine: {
        textAlign: 'center',
        paddingTop: '1em'
      },
      value: {
        fontSize: '6em'
      }
    };

    var value = 0;
    var prefix = '';
    var suffix = '';

    function useIfDefined(current, next) {
      if (typeof next === 'undefined') {
        return current;
      }
      return next;
    }

    value = useIfDefined(value, this.props.value);
    prefix = useIfDefined(prefix, this.props.prefix);
    suffix = useIfDefined(suffix, this.props.suffix);

    var metrics = this.props.metrics;
    console.log('NumberTile metrics', metrics);

    if (metrics && metrics.length > 0) {
      var points = metrics[0].points;

      if (points.length > 0) {
        var point = points[points.length - 1];
        value = useIfDefined(value, point.value);
        prefix = useIfDefined(prefix, point.prefix);
        suffix = useIfDefined(suffix, point.suffix);
      }
    }

    return (
      <div style={styles.container}>
        <div style={styles.valueLine}><span style={styles.prefix}>{prefix}</span><span style={styles.value}>{value}</span><span style={styles.suffix}>{suffix}</span></div>
      </div>
    );
  }
});
