var React = require('react');
var LineChart = require("react-chartjs").Line;

module.exports = React.createClass({
  getDefaultProps: function() {
    return {
      data: null
    };
  },
  render: function() {
    var styles = {
      container: {
        color: '#ffffff',
        backgroundColor: '#1e1e1e',
        height: '100%'
      }
    };

    var chart;

    if (this.props.data) {
      var chartOptions = {

      };

      var chartData = {
        labels: this.props.data.labels,
        datasets: this.props.data.dataSets
      };

      console.log('chartData', chartData);

      var legend = this.props.data.dataSets.map(function(dataSet) {
        var style = {
          backgroundColor: dataSet.fillColor
        };
        return (
            <li><span style={style}>dataSet.label</span></li>
          );
      });

      chart = (<div>
          <LineChart data={chartData} options={chartOptions} style={{width:'100%', height:'100%'}} />
          <ul>
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
