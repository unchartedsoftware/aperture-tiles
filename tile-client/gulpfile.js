var gulp = require('gulp'),
  	del = require('del'),
    concat = require('gulp-concat'),
    csso = require('gulp-csso'),    
    istanbul = require('gulp-istanbul'),
    mocha = require('gulp-mocha'),
    jshint = require('gulp-jshint'),
    browserify = require('browserify'),
    source = require('vinyl-source-stream'),
    buffer = require('vinyl-buffer'),
    run = require('gulp-run'),
    uglify = require('gulp-uglify');

function bundle( b, output ) {
    return b.bundle()
        .on( 'error', function( e ) {
            console.log( e );
        })
        .pipe( source( output ) )
        .pipe( gulp.dest( 'build' ) );
}

function bundleMin( b, output ) {
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
   	del.sync([ 'build/*']);
    del.sync([ 'docs/*']);
});

gulp.task('lint', function() {
     return gulp.src( ['src/js/**/*.js'] )
             .pipe( jshint() )
             .pipe( jshint('.jshintrc') )
             .pipe( jshint.reporter('jshint-stylish') );
});

gulp.task('test', function() {
    return gulp.src( [ 'src/js/**/*.js' ] )
       .pipe( istanbul( { includeUntested: false } ) ) // Covering files
       .on( 'finish', function () {
            gulp.src( [ 'test/**/*.js' ] )
                .pipe( mocha( { reporter: 'spec' } )
                    .on( 'error', handleError) ) // print mocha error message
                .pipe( istanbul.writeReports() ); // Creating the reports after tests runned
        });
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
    return buildMin( './src/js/api.js', 'tiles.min.js' );
});

gulp.task('build-js', function() {
    return build( './src/js/api.js', 'tiles.js' );
});

gulp.task('generate-docs', function () { 
  run('jsdoc src/js/ --destination docs --recurse --template node_modules/jaguarjs-jsdoc').exec();
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