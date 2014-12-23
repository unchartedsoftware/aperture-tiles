"use strict";

global._ = require('lodash');

var path = require('path'),
    srcPath = path.join( __dirname, '../../src/js/' ),  
    LegendService = require( srcPath + 'rest/LegendService' ),
    Util = require( srcPath + 'util/Util' ),
    RestTestUtil = require( './RestTestUtil' ),
    API_VERSION = 'v1.0',
    TEST_LAYER = 'layerId';

describe('LegendService', function() {

    describe('#getEncodedImage()', function() {
        it('should make a GET request to the URL "rest/'+API_VERSION+'/legend/{layerId}"', function() {      
            // mock
            global.$ = RestTestUtil.urlRequestMock( 'get', 'rest/'+API_VERSION+'/legend/'+TEST_LAYER );
            LegendService.getEncodedImage( TEST_LAYER );
        });       
        it('should provide a function to be called upon error', function() {
            // mock
            global.$ = RestTestUtil.errorFunctionMock( 'get' );
            LegendService.getEncodedImage( TEST_LAYER );
        });
        it('should accept a "parameters" object as an optional 2nd parameter, and append it as dot-notated query parameters', function() {
            var params = {
                    a: {
                        a: 'a',
                        b: {
                            c: 'd'
                        }
                    }
                };      
            // mock
            global.$ = RestTestUtil.urlRequestMock( 'get', 'rest/'+API_VERSION+'/legend/'+TEST_LAYER+ Util.encodeQueryParams( params ) );
            LegendService.getEncodedImage( TEST_LAYER, params );
        });
        it('should accept an optional "success" callback function as a final parameter', function() {
            // mock
            global.$ = RestTestUtil.successFunctionMock( 'get', true );
            // with params, with success
            LegendService.getEncodedImage( TEST_LAYER, {}, function() {
            });
            // without params, with success
            LegendService.getEncodedImage( TEST_LAYER, function() {
            });

            // mock
            global.$ = RestTestUtil.successFunctionMock( 'get', false );
            // with params, without success
            LegendService.getEncodedImage( TEST_LAYER, {} );
            // without params, without success
            LegendService.getEncodedImage( TEST_LAYER );
        });
    });

    describe('#getImage()', function() {
        it('should make a GET request to the URL "rest/'+API_VERSION+'/legend/{layerId}"', function() {      
            // mock
            global.$ = RestTestUtil.urlRequestMock( 'get', 'rest/'+API_VERSION+'/legend/'+TEST_LAYER+ Util.encodeQueryParams( { output: 'png' } ) );
            LegendService.getImage( TEST_LAYER );
        });       
        it('should provide a function to be called upon error', function() {
            // mock
            global.$ = RestTestUtil.errorFunctionMock( 'get' );
            LegendService.getImage( TEST_LAYER );
        });
        it('should accept a "parameters" object as an optional 2nd parameter, and append it as dot-notated query parameters', function() {
            var params = {
                    a: {
                        a: 'a',
                        b: {
                            c: 'd'
                        }
                    },
                    output: 'png'
                };      
            // mock
            global.$ = RestTestUtil.urlRequestMock( 'get', 'rest/'+API_VERSION+'/legend/'+TEST_LAYER+ Util.encodeQueryParams( params ) );
            LegendService.getImage( TEST_LAYER, params );
        });
        it('should accept an optional "success" callback function as a final parameter', function() {
            // mock
            global.$ = RestTestUtil.successFunctionMock( 'get', true );
            // with params, with success
            LegendService.getImage( TEST_LAYER, {}, function() {
            });
            // without params, with success
            LegendService.getImage( TEST_LAYER, function() {
            });

            // mock
            global.$ = RestTestUtil.successFunctionMock( 'get', false );
            // with params, without success
            LegendService.getImage( TEST_LAYER, {} );
            // without params, without success
            LegendService.getImage( TEST_LAYER );
        });
    });

});