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
define(function () {
    "use strict";

    var Config, i;

    Config = {
        loaded: false,
        loadConfiguration: function (baseLayerSpec) {
            if (!this.loaded) {
                var baseLayer;

                // Allow the user to specify multiple base layers; for now, if 
                // they do, just use the first.  Later we might let them switch 
                // between them somehow.
                if ("object" === typeof baseLayerSpec) {
                	if (Array.isArray(baseLayerSpec)) {
                    	for (i=0; i < baseLayerSpec.length; i++) {
                    		if (baseLayerSpec[i].hasOwnProperty("Google")) {
                    			// pick the first google base layer specified
                    			baseLayer = baseLayerSpec[i];
                    			break;
                    		}
                    	}
                    } else {
                        baseLayer = baseLayerSpec;
                    }
                }

                aperture.config.provide({
                    /*
                     * A default log configuration, which simply appends to the console.
                     */
                    'aperture.log' : {
                        'level' : 'info',
                        'appenders' : {
                            // Log to the console (if exists)
                            'consoleAppender' : {'level': 'info'}
                        }
                    },

                    /*
                     * The endpoint locations for Aperture services accessed through the io interface
                     */
                    'aperture.io' : {
                        'rpcEndpoint' : '%host%/aperture/rpc',
                        'restEndpoint' : '%host%/twitter-demo/rest'
                    },

                    /*
                     * A default map configuration for the examples
                     */
                    'aperture.map' : {
                        'defaultMapConfig' : {

                            /*
                             * Map wide options which are required for proper use of
                             * the tile set below.
                             */
                            'options' : {
                                'projection': 'EPSG:900913',
                                'displayProjection': 'EPSG:4326',
                                'units': 'm',
                                'numZoomLevels': 19,
                                'maxExtent': [
                                        -20037500,
                                        -20037500,
                                    20037500,
                                    20037500
                                ]
                            },

                            /* The example maps use a Tile Map Service (TMS), which when
                             * registered with the correct settings here in the client
                             * requires no server side code. Tile images are simply requested
                             * by predictable url paths which resolve to files on the server.
                             */
                            'baseLayer' : baseLayer
                        } // end defaultMapConfig
                    },

                    /*
                     * An example palette definition.
                     */
                    'aperture.palette' : {
                        'color' : {
                            'border' : '#9d9999',
                            'rule' : '#9d9999',
                            'link' : '#9d9999',
                            'bad'  : '#FF3333',
                            'good' : '#66CCC9',
                            'selected' : '#7777DD'
                        },

                        'colors' : {
                            'series.10' : ['#1f77b4', '#ff7f0e', '#2ca02c', '#d62728', '#9467bd',
                                           '#8c564b', '#e377c2', '#7f7f7f', '#bcbd22', '#17becf']
                        }
                    }
                });

                this.loaded = true;
            }
        }
    };

    return Config;
});
