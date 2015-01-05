/*
 * Copyright (c) 2014 Oculus Info Inc.
 * http://www.oculusinfo.com/
 *
 * Released under the MIT License.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:

 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.

 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

"use strict";

global._ = require('lodash');

var path = require('path'),
    assert = require("assert"),
    srcPath = path.join( __dirname, '../../src/js/' ),  
    AnnotationService = require( srcPath + 'rest/AnnotationService' ),
    Util = require( srcPath + 'util/Util' ),
    RestTestUtil = require( './RestTestUtil' ),
    API_VERSION = 'v1.0',
    TEST_LAYER = 'layerId',
    TEST_ARGS = {
        level: 4,
        x: 3,
        y: 6
    };

describe('AnnotationService', function() {

    describe('#getTileJSON()', function() {
        it('should make a GET request to the URL "rest/'+API_VERSION+'/annotation/{layerId}/{level}/{x}/{y}.json"', function() {      
            // mock
            global.$ = RestTestUtil.urlRequestMock( 'get', 'rest/'+API_VERSION+'/annotation/'+TEST_LAYER+'/'+TEST_ARGS.level+'/'+TEST_ARGS.x+'/'+TEST_ARGS.y+'.json' );
            AnnotationService.getTileJSON( TEST_LAYER, TEST_ARGS.level, TEST_ARGS.x, TEST_ARGS.y );
        });       
        it('should provide a function to be called upon error', function() {
            // mock
            global.$ = RestTestUtil.errorFunctionMock( 'get' );               
            AnnotationService.getTileJSON( TEST_LAYER );
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
            global.$ = RestTestUtil.urlRequestMock( 'get', 'rest/'+API_VERSION+'/annotation/'+TEST_LAYER+'/'+TEST_ARGS.level+'/'+TEST_ARGS.x+'/'+TEST_ARGS.y+'.json' + Util.encodeQueryParams( params ) );
            AnnotationService.getTileJSON( TEST_LAYER, TEST_ARGS.level, TEST_ARGS.x, TEST_ARGS.y, params );
        });
        it('should accept an optional "success" callback function as a final parameter', function() {
            // mock
            global.$ = RestTestUtil.successFunctionMock( 'get', true );
            // with params, with success
            AnnotationService.getTileJSON( TEST_LAYER, TEST_ARGS.level, TEST_ARGS.x, TEST_ARGS.y, {}, function() {
            });
            // without params, with success
            AnnotationService.getTileJSON( TEST_LAYER, TEST_ARGS.level, TEST_ARGS.x, TEST_ARGS.y, function() {
            });

            // mock
            global.$ = RestTestUtil.successFunctionMock( 'get', false );
            // with params, without success
            AnnotationService.getTileJSON( TEST_LAYER, TEST_ARGS.level, TEST_ARGS.x, TEST_ARGS.y, {} );
            // without params, without success
            AnnotationService.getTileJSON( TEST_LAYER, TEST_ARGS.level, TEST_ARGS.x, TEST_ARGS.y );
        });
    });

    describe('#writeAnnotation()', function() {
        it('should make a POST request to the URL "rest/'+API_VERSION+'/annotation"', function() {      
            // mock
            global.$ = RestTestUtil.urlRequestMock( 'post', 'rest/'+API_VERSION+'/annotation' );
            AnnotationService.writeAnnotation( TEST_LAYER, {} );
        });
        it('should set the "type" attribute of the query object as "write"', function() {
            // mock
            global.$ = {
                post: function( url, param ) {
                        assert( JSON.parse( param ).type === "write" );
                    return {
                        then: function() {
                        }
                    };
                }
            };
            AnnotationService.writeAnnotation( TEST_LAYER, {} );
        });
        it('should set the "layer" attribute of the query object as {layerId}', function() {
            // mock
            global.$ = {
                post: function( url, param ) {
                        assert( JSON.parse( param ).layer === TEST_LAYER );
                    return {
                        then: function() {
                        }
                    };
                }
            };
            AnnotationService.writeAnnotation( TEST_LAYER, {} );
        });  
        it('should provide a function to be called upon error', function() {
            // mock
            global.$ = RestTestUtil.errorFunctionMock( 'post' );               
            AnnotationService.writeAnnotation( TEST_LAYER, {} );
        });
        it('should accept an annotation object as a 2nd parameter', function() {
            var annotation = {
                    data: {}
                };  
            // mock
            global.$ = {
                post: function( url, param ) {
                        assert( JSON.stringify( JSON.parse( param ).annotation ) === JSON.stringify( annotation ) );
                    return {
                        then: function() {
                        }
                    };
                }
            };
            AnnotationService.writeAnnotation( TEST_LAYER, annotation );
        });        
        it('should accept an optional "success" callback function as a final parameter', function() {
            // mock
            global.$ = RestTestUtil.successFunctionMock( 'post', true );
            // with success
            AnnotationService.writeAnnotation( TEST_LAYER, {}, function() {
            });
            // mock
            global.$ = RestTestUtil.successFunctionMock( 'post', false );
            // without success
            AnnotationService.writeAnnotation( TEST_LAYER, {} );
        });
    });

    describe('#modifyAnnotation()', function() {
        it('should make a POST request to the URL "rest/'+API_VERSION+'/annotation"', function() {      
            // mock
            global.$ = RestTestUtil.urlRequestMock( 'post', 'rest/'+API_VERSION+'/annotation' );
            AnnotationService.modifyAnnotation( TEST_LAYER, {} );
        });
        it('should set the "type" attribute of the query object as "modify"', function() {
            // mock
            global.$ = {
                post: function( url, param ) {
                        assert( JSON.parse( param ).type === "modify" );
                    return {
                        then: function() {
                        }
                    };
                }
            };
            AnnotationService.modifyAnnotation( TEST_LAYER, {} );
        });
        it('should set the "layer" attribute of the query object as {layerId}', function() {
            // mock
            global.$ = {
                post: function( url, param ) {
                        assert( JSON.parse( param ).layer === TEST_LAYER );
                    return {
                        then: function() {
                        }
                    };
                }
            };
            AnnotationService.modifyAnnotation( TEST_LAYER, {} );
        });  
        it('should provide a function to be called upon error', function() {
            // mock
            global.$ = RestTestUtil.errorFunctionMock( 'post' );               
            AnnotationService.modifyAnnotation( TEST_LAYER, {} );
        });
        it('should accept an annotation object as a 2nd parameter', function() {
            var annotation = {
                    data: {}
                };  
            // mock
            global.$ = {
                post: function( url, param ) {
                        assert( JSON.stringify( JSON.parse( param ).annotation ) === JSON.stringify( annotation ) );
                    return {
                        then: function() {
                        }
                    };
                }
            };
            AnnotationService.modifyAnnotation( TEST_LAYER, annotation );
        });        
        it('should accept an optional "success" callback function as a final parameter', function() {
            // mock
            global.$ = RestTestUtil.successFunctionMock( 'post', true );
            // with success
            AnnotationService.modifyAnnotation( TEST_LAYER, {}, function() {
            });
            // mock
            global.$ = RestTestUtil.successFunctionMock( 'post', false );
            // without success
            AnnotationService.modifyAnnotation( TEST_LAYER, {} );
        });
    });

    describe('#removeAnnotation()', function() {
        it('should make a POST request to the URL "rest/'+API_VERSION+'/annotation"', function() {      
            // mock
            global.$ = RestTestUtil.urlRequestMock( 'post', 'rest/'+API_VERSION+'/annotation' );
            AnnotationService.removeAnnotation( TEST_LAYER, {} );
        });
        it('should set the "type" attribute of the query object as "remove"', function() {
            // mock
            global.$ = {
                post: function( url, param ) {
                        assert( JSON.parse( param ).type === "remove" );
                    return {
                        then: function() {
                        }
                    };
                }
            };
            AnnotationService.removeAnnotation( TEST_LAYER, {} );
        });
        it('should set the "layer" attribute of the query object as {layerId}', function() {
            // mock
            global.$ = {
                post: function( url, param ) {
                        assert( JSON.parse( param ).layer === TEST_LAYER );
                    return {
                        then: function() {
                        }
                    };
                }
            };
            AnnotationService.removeAnnotation( TEST_LAYER, {} );
        });  
        it('should provide a function to be called upon error', function() {
            // mock
            global.$ = RestTestUtil.errorFunctionMock( 'post' );               
            AnnotationService.removeAnnotation( TEST_LAYER, {} );
        });
        it('should accept a certificate object as a 2nd parameter', function() {
            var certificate = {
                    timestamp: '1419375072',
                    uuid: 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855'
                };  
            // mock
            global.$ = {
                post: function( url, param ) {
                        assert( JSON.stringify( JSON.parse( param ).certificate ) === JSON.stringify( certificate ) );
                    return {
                        then: function() {
                        }
                    };
                }
            };
            AnnotationService.removeAnnotation( TEST_LAYER, certificate );
        });        
        it('should accept an optional "success" callback function as a final parameter', function() {
            // mock
            global.$ = RestTestUtil.successFunctionMock( 'post', true );
            // with success
            AnnotationService.removeAnnotation( TEST_LAYER, {}, function() {
            });
            // mock
            global.$ = RestTestUtil.successFunctionMock( 'post', false );
            // without success
            AnnotationService.removeAnnotation( TEST_LAYER, {} );
        });
    });

});