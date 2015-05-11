'use strict';

var React = require('react');
var Dashboard = require('tiler').Dashboard;
var NumberTile = require('tiler-contrib-number-tile');
var ListTile = require('tiler-contrib-list-tile');
var LineChartTile = require('tiler-contrib-line-chart-tile');

var breakpoints = {lg: 1200, md: 996, sm: 768, xs: 480};
var cols = {lg: 12, md: 10, sm: 8, xs: 4};

React.render(
  <Dashboard breakpoints={breakpoints} cols={cols} rowHeight={30}>
    <NumberTile key={1} _grid={{x: 0, y: 0, w: 2, h: 6}}
      query={{
        point: {title: 'projectName', value: 'value'},
        from: 'ci.sonarqube.coverage',
        where: {projectKey: 'org.springframework:spring'}
      }}
      suffix={'%'} />
    <ListTile key={2} _grid={{x: 6, y: 0, w: 4, h: 26}} title={'Broken Builds'} ordered={false}
      query={{
        point: {label: 'jobName', value: 'value'},
        from: 'ci.jenkins.job_color',
        where: {value: 'red'}
      }} />
    <LineChartTile key={3} _grid={{x: 0, y: 6, w: 6, h: 20}}
      query={{
        metric: {label: 'projectName'},
        point: {time: 'time', value: {value: '$mean'}},
        from: 'ci.sonarqube.coverage',
        where: {time: {$gte: {$minus: ['$now', '600d']}}},
        group: ['projectKey', 'projectName'],
        aggregate: {time: {$intervals: {$size: '28d', $offset: {$minus: ['$now', '600d']}}}}
      }} />
  </Dashboard>,
  document.getElementById('content')
);
