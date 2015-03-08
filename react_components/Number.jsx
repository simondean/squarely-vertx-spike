var React   = require('react');

module.exports = React.createClass({
  getInitialState: function() {
    return {
      prefix: '',
      value: 0,
      suffix: ''
    };
  },
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

    return (
      <div className="number" style={styles.container}>
        <div style={styles.valueLine}><span style={styles.prefix}>{this.state.prefix}</span><span style={styles.value}>{this.state.value}</span><span style={styles.suffix}>{this.state.suffix}</span></div>
      </div>
    );
  }
});
