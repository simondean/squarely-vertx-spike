var React = require('react');

module.exports = React.createClass({
  render: function() {
    var styles = {
      container: {
        color: '#ffffff',
        backgroundColor: '#1e1e1e',
        height: '100%'
      },
      title: {
        textAlign: 'center'
      },
      itemLabel: {},
      itemValue: {
        float: 'right',
        marginLeft: '1em'
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

      metric.points.forEach(function(point) {
        data.items.push({
          label: point.label,
          value: point.value
        });
      });
    }

    var title = (typeof data.title === 'undefined') ? '' : <div style={styles.title}>{data.title}</div>;
    var items = [];

    data.items.forEach(function(item) {
      items.push(<li><span style={styles.itemLabel}>{item.label}</span><span style={styles.itemValue}>{item.value}</span></li>);
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
        {title}
        {list}
      </div>
    );
  }
});
