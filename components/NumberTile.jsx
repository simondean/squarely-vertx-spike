var React = require('react');

module.exports = React.createClass({
  render: function() {
    var styles = {
      container: {
        color: '#ffffff',
        backgroundColor: '#1e1e1e',
        height: '100%'
      },
      titleDiv: {
        textAlign: 'center'
      },
      valueDiv: {
        textAlign: 'center',
        paddingTop: '1em'
      },
      value: {
        fontSize: '6em'
      }
    };

    var data = {
      title: undefined,
      value: 0,
      prefix: undefined,
      suffix: undefined
    };

    function useIfDefined(previous, next) {
      if (typeof next === 'undefined') {
        return previous;
      }
      return next;
    }

    function updateData(data, partialData) {
      data.title = useIfDefined(data.title, partialData.title);
      data.value = useIfDefined(data.value, partialData.value);
      data.prefix = useIfDefined(data.prefix, partialData.prefix);
      data.suffix = useIfDefined(data.suffix, partialData.suffix);
    }

    updateData(data, this.props);

    var metrics = this.props.metrics;
    console.log('NumberTile metrics', metrics);

    if (metrics && metrics.length > 0) {
      var metric = metrics[0];
      updateData(data, metric);
      var points = metric.points;

      if (points.length > 0) {
        var point = points[points.length - 1];
        updateData(data, point);
      }
    }

    var titleDiv = (typeof data.title === 'undefined') ? '' : <div style={styles.titleDiv}>{data.title}</div>;
    var prefixSpan = (typeof data.prefix === 'undefined') ? '' : <span style={styles.prefix}>{data.prefix}</span>;
    var suffixSpan = (typeof data.suffix === 'undefined') ? '' : <span style={styles.suffix}>{data.suffix}</span>;

    return (
      <div style={styles.container}>
        {titleDiv}
        <div style={styles.valueDiv}>{prefixSpan}<span style={styles.value}>{data.value}</span>{suffixSpan}</div>
      </div>
    );
  }
});
