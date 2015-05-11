module.exports = function(grunt) {
  grunt.initConfig({
    pkg: grunt.file.readJSON('package.json'),

    watch: {
      react: {
        files: ['components/**/*.jsx', 'dashboards/**/*.jsx'],
        tasks: ['browserify']
      }
    },

    browserify: {
      options: {
        transform: [ require('grunt-react').browserify ]
      },
      dashboards: {
        files: [
          {
            expand: true,
            cwd: 'dashboards/',
            src: ['*.jsx'],
            dest: 'src/main/resources/static/scripts/dashboards/',
            ext: '.built.js',
            extDot: 'first'
          }
        ]
      }
    },

    cssmin: {
      dashboards: {
        files: {
          'src/main/resources/static/styles/dashboard.built.css': ['node_modules/normalize.css/normalize.css']
        }
      }
    },

    copy: {
      fonts: {
        files: [
          {
            expand: true,
            cwd: 'node_modules/font-awesome/fonts/',
            src: ['*'],
            dest: 'src/main/resources/static/fonts/',
            filter: 'isFile'
          }
        ]
      }
    }
  });

  grunt.loadNpmTasks('grunt-browserify');
  grunt.loadNpmTasks('grunt-contrib-cssmin');
  grunt.loadNpmTasks('grunt-contrib-copy');
  grunt.loadNpmTasks('grunt-contrib-watch');

  grunt.registerTask('build', ['browserify', 'cssmin', 'copy']);
  grunt.registerTask('default', ['build', 'watch']);
};
