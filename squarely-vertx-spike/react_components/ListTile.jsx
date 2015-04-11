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
      }
    };

    var data = {
      title: undefined,
      items: []
    };

    function useIfDefined(previous, next) {
      if (typeof next === 'undefined') {
        return previous;
      }
      return next;
    }

    function updateData(data, partialData) {
      data.title = useIfDefined(data.title, partialData.title);
    }

    updateData(data, this.props);

    var metrics = this.props.metrics;
    console.log('ListTile metrics', metrics);

    if (metrics && metrics.length > 0) {
      var metric = metrics[0];
      updateData(data, metric);
      var points = metric.points;

      if (points.length > 0) {
        var point = points[points.length - 1];
        data.items.push({
          label: point.label,
          value: point.value
        });
      }
    }

    var titleDiv = (typeof data.title === 'undefined') ? '' : <div style={styles.titleDiv}>{data.title}</div>;
    var items = [];

    data.items.forEach(function(item) {
      items.push(<li><span>{item.label}</span> <span>{item.value}</span></li>);
    });

    var list;

    if (this.props.ordered) {
      list = <ol>{items}</ol>;
    }
    else {
      list = <ul>{items}</ul>;
    }

    return (
      <div style={styles.container}>
        {titleDiv}
        {list}
      </div>
    );
  }
});
