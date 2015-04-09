var React = require('react');
var LineChart = require("react-chartjs").Line;

module.exports = React.createClass({
  render: function() {
    var styles = {
      container: {
        color: '#ffffff',
        backgroundColor: '#1e1e1e',
        height: '100%'
      },
      legendList: {
        listStyleType: 'none'
      }
    };

    var chart;
    var metrics = this.props.metrics;

    if (metrics && metrics.length > 0) {
      console.log('Creating chart for ', metrics);
      var labels = [];
      var dataSets = [];

      metrics.forEach(function(metric) {
        var dataSet = {
          label: metric.label,
          data: []
        };
        metric.points.forEach(function(point) {
          dataSet.data.push({
            label: point.time,
            value: point.value
          });
          if (labels.indexOf(point.time) == -1) {
            labels.push(point.time);
          }
        });
        dataSets.push(dataSet);
      });

      labels.sort();

      var colors = [
        //'004358',
        //'1f8a70',
        //'bedb39',
        //'ffe11a',
        //'fd7400'

        'fdb432',
        '426efd',
        '26fd3d',
        'fd2f1f',
        '6865fd',
        'fdd136'
      ];

      colors = colors.map(function(color) {
        var rgb = parseInt(color.slice(0, 2), 16) + ', ' +
          parseInt(color.slice(2, 4), 16) + ', ' +
          parseInt(color.slice(4, 6), 16);
        return {
          strokeColor: 'rgba(' + rgb + ', 0.8)',
          fillColor: 'rgba(' + rgb + ', 0.4)'
        }
      });

      dataSets.forEach(function(dataSet, dataSetIndex) {
        var sortedData = [];
        labels.forEach(function(label, labelIndex) {
          sortedData[labelIndex] = null;
        });
        dataSet.data.forEach(function(dataItem) {
          sortedData[labels.indexOf(dataItem.label)] = dataItem.value;
        });
        dataSet.data = sortedData;
        var colorIndex = dataSetIndex % colors.length;
        dataSet.strokeColor = colors[colorIndex].strokeColor;
        dataSet.fillColor = colors[colorIndex].fillColor;
      });

      // TODO: Support more than just printing the month and day of the timestamp
      labels = labels.map(function(timestamp) {
        var date = new Date(timestamp * 1000);
        return ('00' + (date.getMonth() + 1)).slice(-2) + '-' + ('00' + date.getDate()).slice(-2);
      });

      var chartOptions = {
      };

      var chartData = {
        labels: labels,
        datasets: dataSets
      };

      console.log('chartData', chartData);

      var legend = dataSets.map(function(dataSet) {
        var legendItemStyles = {
          bullet: {
            //display: 'inline-block',
            font: 'normal normal normal 14px/1 FontAwesome',
            fontSize: 'inherit',
            textRendering: 'auto',
            WebkitFontSmoothing: 'antialiased',
            MozOsxFontSmoothing: 'grayscale',
            transform: 'translate(0, 0)',
            color: dataSet.fillColor
          },
          label: {
            //backgroundColor: dataSet.fillColor
          }
        };

        return (
          <li><span style={legendItemStyles.bullet}>&#xf068;</span> <span style={legendItemStyles.label}>{dataSet.label}</span></li>
        );
      });

      chart = (<div>
        <LineChart data={chartData} options={chartOptions} style={{width:'100%', height:'100%'}} />
        <ul style={styles.legendList}>
            {legend}
        </ul>
      </div>);
    }
    else {
      chart = '';
    }

    return (
      <div style={styles.container}>
        {chart}
      </div>
    );
  }
});
