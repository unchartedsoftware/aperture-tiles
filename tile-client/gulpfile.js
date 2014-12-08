// var gulp = require('gulp'),
// 	concat = require('gulp-concat'),
// 	uglify = require('gulp-uglify'),
// 	amdOptimize = require('amd-optimize'),
// 	csso = require('gulp-csso'),
// 	del = require('del');


// var requireConfig = {
// 	    shim: {
// 	        "OpenLayers": {
// 	            exports: "OpenLayers"
// 	        }
// 	    },
// 	    paths: {
// 	        "jquery": 'bower_components/jquery/jquery.min',
// 	        "lodash": 'bower_components/lodash/dist/lodash.min',
// 	        "OpenLayers": 'src/js/openlayers/OpenLayers.light'
// 	    }
// 	};

// gulp.task('build-js-min', function () {
// 	return gulp.src('./src/js/**/*.js')
//         .pipe( amdOptimize('src/js/api', requireConfig ) )
//         .pipe( concat('tiles.min.js') )
//         .pipe( uglify() )
//         .pipe( gulp.dest('build') );
// });

// gulp.task('build-js', function () {
// 	return gulp.src('./src/js/**/*.js')
//         .pipe( amdOptimize('src/js/api', requireConfig ) )
//         .pipe( concat('tiles.js') )
//         .pipe( gulp.dest('build') );
// });

// gulp.task('build-css', function () {
// 	return gulp.src('./src/css/*.css')
//         .pipe( csso() )
//         .pipe( concat('tiles.css') )
//         .pipe( gulp.dest('build') );
// });

// gulp.task('clean', function () {
//   	del([ 'build/*']);
// });

// gulp.task('default', ['clean'], function () {
// 	gulp.start( ['build-js-min', 'build-js', 'build-css'] );
// });

var gulp = require('gulp'),
  	del = require('del'),
    concat = require('gulp-concat'),
    csso = require('gulp-csso'),
    jshint = require('gulp-jshint'),
    browserify = require('browserify'),
    source = require('vinyl-source-stream'),
    buffer = require('vinyl-buffer'),
    uglify = require('gulp-uglify');


function bundle( b, minify ) {
    return b.bundle()
        .on( 'error', function( e ) {
            console.log( e );
        })
        .pipe( source( 'tiles.js' ) )
        .pipe( gulp.dest( 'build' ) );
}

function bundleMin( b, minify ) {
    return b.bundle()
        .on( 'error', function( e ) {
            console.log( e );
        })
        .pipe( source( 'tiles.min.js' ) )
        .pipe( buffer() )
        .pipe( uglify() )
        .pipe( gulp.dest( 'build' ) );
}

function build() {
    var b = browserify( './src/js/api.js', { 
            debug: true,
            standalone: 'tiles'
        });
    return bundle( b );
}

function buildMin() {
    var b = browserify( './src/js/api.js', { 
            standalone: 'tiles'
        });
    return bundleMin( b );
}

function handleError( err ) {
    console.log( err.toString() );
    this.emit('end');
}

gulp.task('clean', function () {
   	del([ 'build/*']);
});

gulp.task('lint', function() {
     return gulp.src( ['src/js/**/*.js', '!src/js/openlayers/*.js'] )
             .pipe( jshint() )
             .pipe( jshint('.jshintrc') )
             .pipe( jshint.reporter('jshint-stylish') );
});

gulp.task('build-min-css', function () {
    return gulp.src( 'src/css/*.css' )
        .pipe( csso() )
        .pipe( concat('tiles.min.css') )
        .pipe( gulp.dest('build') );
});

gulp.task('build-css', function () {
    return gulp.src( 'src/css/*.css' )
        .pipe( concat('tiles.css') )
        .pipe( gulp.dest('build') );
});

gulp.task('build-min-js', [ 'lint' ], function() {
    return buildMin();
});

gulp.task('build-js', [ 'lint' ], function() {
    return build();
});

gulp.task('build', [ 'clean' ], function() {
    gulp.start( 'build-js' );
    gulp.start( 'build-min-js' );
    gulp.start( 'build-css' );
    gulp.start( 'build-min-css' );
});

gulp.task('default', [ 'build' ], function() {
});