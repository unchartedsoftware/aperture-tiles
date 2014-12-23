"use strict";

var path = require('path'),
    assert = require("assert"),
    srcPath = path.join( __dirname, '../../src/js/rest/' ),  
    LayerService = require( srcPath + 'LayerService' ),
    RestTestUtil = require( './RestTestUtil' ),
    API_VERSION = 'v1.0',
    TEST_LAYER = 'layerId',
    TEST_STATE = 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855';

describe('LayerService', function() {

    describe('#getLayers()', function() {
        it('should make a GET request to the URL "rest/'+API_VERSION+'/layers"', function() {        
            // mock
            global.$ = RestTestUtil.urlRequestMock( 'get', 'rest/'+API_VERSION+'/layers' );
            LayerService.getLayers();
        });
        it('should provide a function to be called upon error', function() {
            // mock
            global.$ = RestTestUtil.errorFunctionMock( 'get' );               
            LayerService.getLayers();
        });
        it('should accept an optional "success" callback function as a final parameter', function() {
            // mock
            global.$ = RestTestUtil.successFunctionMock( 'get', true );
            // with success
            LayerService.getLayers( function() {
            });

            // mock
            global.$ = RestTestUtil.successFunctionMock( 'get', false );
            // without success
            LayerService.getLayers();
        });
    });

    describe('#getLayer()', function() {
        it('should make a GET request to the URL "rest/'+API_VERSION+'/layers/{layerId}"', function() {
            // mock
            global.$ = RestTestUtil.urlRequestMock( 'get', 'rest/'+API_VERSION+'/layers/'+TEST_LAYER );   
            LayerService.getLayer( TEST_LAYER );
        });
        it('should provide a function to be called upon error', function() {
            // mock
            global.$ = RestTestUtil.errorFunctionMock( 'get' );               
            LayerService.getLayer( TEST_LAYER );
        });
        it('should accept an optional "success" callback function as a final parameter', function() {
            // mock
            global.$ = RestTestUtil.successFunctionMock( 'get', true );
            // with success
            LayerService.getLayer( TEST_LAYER, function() {
            });

            // mock
            global.$ = RestTestUtil.successFunctionMock( 'get', false );
            // without success
            LayerService.getLayer( TEST_LAYER );
        });
    });

    describe('#saveLayerState()', function() {
        it('should make a POST request to the URL "rest/'+API_VERSION+'/layers/{layerId}/states"', function() {
            // mock
            global.$ = RestTestUtil.urlRequestMock( 'post', 'rest/'+API_VERSION+'/layers/'+TEST_LAYER+'/states' );              
            LayerService.saveLayerState( TEST_LAYER, {} );
        });
        it('should provide a function to be called upon error', function() {
            // mock
            global.$ = RestTestUtil.errorFunctionMock( 'post' );                
            LayerService.saveLayerState( TEST_LAYER );
        });
        it('should accept a "parameters" object as a second parameter', function() {
            // mock
            global.$ = {
                post: function( url, param ) {
                        assert( param );
                    return {
                        then: function( success ) {
                            success();
                        }
                    };
                }
            };
            LayerService.saveLayerState( TEST_LAYER, {}, function() {
            });
        });
        it('should accept an optional "success" callback function as a final parameter', function() {
            
            // mock
            global.$ = RestTestUtil.successFunctionMock( 'post', true );
            // with success
            LayerService.saveLayerState( TEST_LAYER, {}, function() {
            });

            // mock
            global.$ = RestTestUtil.successFunctionMock( 'post', false );
            // without success
            LayerService.saveLayerState( TEST_LAYER, {} );
        });
    });

    describe('#getLayerState()', function() {
        it('should make a GET request to the URL "rest/'+API_VERSION+'/layers/{layerId}/states/{stateId}"', function() {
            // mock
            global.$ = RestTestUtil.urlRequestMock( 'get', 'rest/'+API_VERSION+'/layers/'+TEST_LAYER+'/states/'+TEST_STATE );            
            LayerService.getLayerState( TEST_LAYER, TEST_STATE );
        });
        it('should provide a function to be called upon error', function() {
            // mock
            global.$ = RestTestUtil.errorFunctionMock( 'get' );                 
            LayerService.getLayerState( TEST_LAYER, TEST_STATE );
        });
        it('should accept an optional "success" callback function as a final parameter', function() {
            
            // mock
            global.$ = RestTestUtil.successFunctionMock( 'get', true );
            // with success
            LayerService.getLayerState( TEST_LAYER, TEST_STATE, function() {
            });

            // mock
            global.$ = RestTestUtil.successFunctionMock( 'get', false );
            // without success
            LayerService.getLayerState( TEST_LAYER, TEST_STATE );
        });
    });

    describe('#getLayerStates()', function() {
        it('should make a GET request to the URL "rest/'+API_VERSION+'/layers/{layerId}/states"', function() {
            // mock
            global.$ = RestTestUtil.urlRequestMock( 'get', 'rest/'+API_VERSION+'/layers/'+TEST_LAYER+'/states' );            
            LayerService.getLayerStates( TEST_LAYER );
        });
        it('should provide a function to be called upon error', function() {
            // mock
            global.$ = RestTestUtil.errorFunctionMock( 'get' );                 
            LayerService.getLayerStates( TEST_LAYER );
        });
        it('should accept an optional "success" callback function as a final parameter', function() {
            
            // mock
            global.$ = RestTestUtil.successFunctionMock( 'get', true );
            // with success
            LayerService.getLayerStates( TEST_LAYER, function() {
            });

            // mock
            global.$ = RestTestUtil.successFunctionMock( 'get', false );
            // without success
            LayerService.getLayerStates( TEST_LAYER );
        });
    });

});