"use strict";

global._ = require('lodash');

var path = require('path'),
    srcPath = path.join( __dirname, '../../src/js/' ),  
    TileService = require( srcPath + 'rest/TileService' ),
    Util = require( srcPath + 'util/Util' ),
    RestTestUtil = require( './RestTestUtil' ),
    API_VERSION = 'v1.0',
    TEST_LAYER = 'layerId',
    TEST_ARGS = {
        level: 4,
        x: 3,
        y: 6
    };

describe('TileService', function() {

    describe('#getTileJSON()', function() {
        it('should make a GET request to the URL "rest/'+API_VERSION+'/tile/{layerId}/{level}/{x}/{y}.json"', function() {      
            // mock
            global.$ = RestTestUtil.urlRequestMock( 'get', 'rest/'+API_VERSION+'/tile/'+TEST_LAYER+'/'+TEST_ARGS.level+'/'+TEST_ARGS.x+'/'+TEST_ARGS.y+'.json' );
            TileService.getTileJSON( TEST_LAYER, TEST_ARGS.level, TEST_ARGS.x, TEST_ARGS.y );
        });       
        it('should provide a function to be called upon error', function() {
            // mock
            global.$ = RestTestUtil.errorFunctionMock( 'get' );               
            TileService.getTileJSON( TEST_LAYER );
        });
        it('should accept a "parameters" object as an optional 5th parameter, and append it as dot-notated query parameters', function() {
            var params = {
                    a: {
                        a: 'a',
                        b: {
                            c: 'd'
                        }
                    }
                };      
            // mock
            global.$ = RestTestUtil.urlRequestMock( 'get', 'rest/'+API_VERSION+'/tile/'+TEST_LAYER+'/'+TEST_ARGS.level+'/'+TEST_ARGS.x+'/'+TEST_ARGS.y+'.json' + Util.encodeQueryParams( params ) );
            TileService.getTileJSON( TEST_LAYER, TEST_ARGS.level, TEST_ARGS.x, TEST_ARGS.y, params );
        });
        it('should accept an optional "success" callback function as a final parameter', function() {
            // mock
            global.$ = RestTestUtil.successFunctionMock( 'get', true );
            // with params, with success
            TileService.getTileJSON( TEST_LAYER, TEST_ARGS.level, TEST_ARGS.x, TEST_ARGS.y, {}, function() {
            });
            // without params, with success
            TileService.getTileJSON( TEST_LAYER, TEST_ARGS.level, TEST_ARGS.x, TEST_ARGS.y, function() {
            });

            // mock
            global.$ = RestTestUtil.successFunctionMock( 'get', false );
            // with params, without success
            TileService.getTileJSON( TEST_LAYER, TEST_ARGS.level, TEST_ARGS.x, TEST_ARGS.y, {} );
            // without params, without success
            TileService.getTileJSON( TEST_LAYER, TEST_ARGS.level, TEST_ARGS.x, TEST_ARGS.y );
        });
    });

    describe('#getTileImage()', function() {
        it('should make a GET request to the URL "rest/'+API_VERSION+'/tile/{layerId}/{level}/{x}/{y}.png"', function() {      
            // mock
            global.$ = RestTestUtil.urlRequestMock( 'get', 'rest/'+API_VERSION+'/tile/'+TEST_LAYER+'/'+TEST_ARGS.level+'/'+TEST_ARGS.x+'/'+TEST_ARGS.y+'.png' );
            TileService.getTileImage( TEST_LAYER, TEST_ARGS.level, TEST_ARGS.x, TEST_ARGS.y );
        });       
        it('should provide a function to be called upon error', function() {
            // mock
            global.$ = RestTestUtil.errorFunctionMock( 'get' );               
            TileService.getTileImage( TEST_LAYER );
        });
        it('should accept a "parameters" object as an optional 5th parameter, and append it as dot-notated query parameters', function() {
            var params = {
                    a: {
                        a: 'a',
                        b: {
                            c: 'd'
                        }
                    }
                };      
            // mock
            global.$ = RestTestUtil.urlRequestMock( 'get', 'rest/'+API_VERSION+'/tile/'+TEST_LAYER+'/'+TEST_ARGS.level+'/'+TEST_ARGS.x+'/'+TEST_ARGS.y+'.png' + Util.encodeQueryParams( params ) );
            TileService.getTileImage( TEST_LAYER, TEST_ARGS.level, TEST_ARGS.x, TEST_ARGS.y, params );
        });
        it('should accept an optional "success" callback function as a final parameter', function() {
            // mock
            global.$ = RestTestUtil.successFunctionMock( 'get', true );
            // with params, with success
            TileService.getTileImage( TEST_LAYER, TEST_ARGS.level, TEST_ARGS.x, TEST_ARGS.y, {}, function() {
            });
            // without params, with success
            TileService.getTileImage( TEST_LAYER, TEST_ARGS.level, TEST_ARGS.x, TEST_ARGS.y, function() {
            });

            // mock
            global.$ = RestTestUtil.successFunctionMock( 'get', false );
            // with params, without success
            TileService.getTileImage( TEST_LAYER, TEST_ARGS.level, TEST_ARGS.x, TEST_ARGS.y, {} );
            // without params, without success
            TileService.getTileImage( TEST_LAYER, TEST_ARGS.level, TEST_ARGS.x, TEST_ARGS.y );
        });
    });

});