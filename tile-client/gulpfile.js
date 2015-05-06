var gulp = require('gulp'),
    concat = require('gulp-concat'),
    browserify = require('browserify'),
    source = require('vinyl-source-stream');

function bundle( b, output ) {
    return b.bundle()
        .on( 'error', function( e ) {
            console.log( e );
        })
        .pipe( source( output ) )
        .pipe( gulp.dest( 'build' ) );
}

function bundleMin( b, output ) {
    var uglify = require('gulp-uglify'),
        buffer = require('vinyl-buffer');
    return b.bundle()
        .on( 'error', function( e ) {
            console.log( e );
        })
        .pipe( source( output ) )
        .pipe( buffer() )
        .pipe( uglify() )
        .pipe( gulp.dest( 'build' ) );
}

function build( root, output ) {
    var b = browserify( root, {
            debug: true,
            standalone: 'tiles'
        })
    return bundle( b, output );
}

function buildMin( root, output ) {
    var b = browserify( root, {
            standalone: 'tiles'
        })
    return bundleMin( b, output );
}

function handleError( err ) {
    console.log( err.toString() );
    this.emit('end');
}

gulp.task('clean', function () {
    var del = require('del');
   	del.sync([ 'build/*']);
    del.sync([ 'docs/*']);
});

gulp.task('lint', function() {
    var jshint = require('gulp-jshint');
     return gulp.src( ['src/js/**/*.js'] )
             .pipe( jshint() )
             .pipe( jshint('.jshintrc') )
             .pipe( jshint.reporter('jshint-stylish') );
});

gulp.task('test', function() {
    var istanbul = require('gulp-istanbul'),
        mocha = require('gulp-mocha');
    return gulp.src( [ 'src/js/**/*.js' ] )
       .pipe( istanbul( { includeUntested: false } ) ) // Covering files
       .pipe( istanbul.hookRequire() )
       .on( 'finish', function () {
            gulp.src( [ 'test/**/*.js' ] )
                .pipe( mocha( { reporter: 'spec' } )
                    .on( 'error', handleError) ) // print mocha error message
                .pipe( istanbul.writeReports() ); // Creating the reports after tests runned
        });
});

gulp.task('build-min-css', function () {
    var csso = require('gulp-csso');
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
    return buildMin( './src/js/api.js', 'tiles.min.js' );
});

gulp.task('build-js', function() {
    return build( './src/js/api.js', 'tiles.js' );
});

gulp.task('generate-docs', function () {
    var run = require('gulp-run');
    run('jsdoc src/js/ src/js/README.md --destination docs --recurse --template node_modules/jaguarjs-jsdoc ').exec()
})

gulp.task('build', [ 'clean' ], function() {
    gulp.start( 'build-js' );
    gulp.start( 'build-css' );
    gulp.start( 'build-min-js' );
    gulp.start( 'build-min-css' );
    gulp.start( 'generate-docs' );
});

gulp.task('default', [ 'build' ], function() {
});
