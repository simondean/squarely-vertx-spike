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
      client: {
        src: ['react_components/**/*.jsx'],
        dest: 'src/main/resources/web/scripts/app.built.js'
      }
    },

    cssmin: {
      target: {
        files: {
          'src/main/resources/web/styles/app.built.css': ['node_modules/normalize.css/normalize.css']
        }
      }
    }
  });

  grunt.loadNpmTasks('grunt-browserify');
  grunt.loadNpmTasks('grunt-contrib-cssmin');
  grunt.loadNpmTasks('grunt-contrib-watch');

  grunt.registerTask('default', [
    'browserify',
    'cssmin'
  ]);
};
