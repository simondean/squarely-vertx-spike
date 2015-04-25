module.exports = function(grunt) {
  grunt.initConfig({
    pkg: grunt.file.readJSON('package.json'),

    watch: {
      react: {
        files: 'react_components/*.jsx',
        tasks: ['browserify']
      }
    },

    browserify: {
      options: {
        transform: [ require('grunt-react').browserify ]
      },
      app: {
        src: ['react_components/**/*.jsx'],
        dest: 'src/main/resources/static/scripts/app.built.js'
      }
    },

    cssmin: {
      app: {
        files: {
          'src/main/resources/web/styles/app.built.css': ['node_modules/normalize.css/normalize.css']
        }
      }
    },

    copy: {
      app: {
        files: [
          {expand: true, cwd: 'node_modules/font-awesome/fonts/', src: ['*'], dest: 'src/main/resources/static/fonts/', filter: 'isFile'},
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
