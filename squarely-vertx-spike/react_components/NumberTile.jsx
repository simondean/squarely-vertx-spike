var React = require('react');

module.exports = React.createClass({
  getDefaultProps: function() {
    return {
      value: 0,
      prefix: null,
      suffix: null
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
      <div style={styles.container}>
        <div style={styles.valueLine}><span style={styles.prefix}>{this.props.prefix}</span><span style={styles.value}>{this.props.value}</span><span style={styles.suffix}>{this.props.suffix}</span></div>
      </div>
    );
  }
});
