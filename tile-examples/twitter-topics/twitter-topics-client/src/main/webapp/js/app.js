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
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

var appStart = function() {

    "use strict";

    // request layers from server
    tiles.LayerService.getLayers( function( layers ) {

        // parse layers into nicer format
        layers = tiles.LayerUtil.parse( layers.layers );

        var map,
            axis0,
            axis1,
            baseLayer,
            clientLayer,
            serverLayer;

        baseLayer = new tiles.BaseLayer({
            "type": "Google",
            "theme" : "dark",
            "options" : {
                "name" : "Dark",
                "type" : "styled",
                "style" : [
                    {
                        'featureType': 'all',
                        'stylers': [
                            { 'saturation': -100 },
                            { 'invert_lightness' : true },
                            { 'visibility' : 'simplified' }
                        ]
                    },
                    {
                        'featureType': 'landscape.natural',
                        'stylers': [
                            { 'lightness': -50 }
                        ]
                    },
                    {
                        'featureType': 'poi',
                        'stylers': [
                            { 'visibility': 'off' }
                        ]
                    },
                    {
                        'featureType': 'road',
                        'stylers': [
                            { 'lightness': -50 }
                        ]
                    },
                    {
                        'featureType': 'road',
                        'elementType': 'labels',
                        'stylers': [
                            { 'visibility': 'off' }
                        ]
                    },
                    {
                        'featureType': 'road.highway',
                        'stylers': [
                            { 'lightness': -60 }
                        ]
                    },
                    {
                        'featureType': 'road.arterial',
                        'stylers': [
                            { 'visibility': 'off' }
                        ]
                    },
                    {
                        'featureType': 'administrative',
                        'stylers': [
                            { 'lightness': 10 }
                        ]
                    },
                    {
                        'featureType': 'administrative.province',
                        'elementType': 'geometry',
                        'stylers': [
                            { 'lightness': 15 }
                        ]
                    },
                    {
                        'featureType' : 'administrative.country',
                        'elementType' : 'geometry',
                        'stylers' : [
                            { 'visibility' : 'on' },
                            { 'lightness' : -56 }
                        ]
                    },
                    {
                        'elementType' : 'labels',
                        'stylers' : [
                            { 'lightness' : -46 },
                            { 'visibility' : 'on' }
                        ] }
                ]
            }
        });

        serverLayer = new tiles.ServerLayer({
            source: layers["tweet-heatmap"],
            valueTransform: {
                type: "log10"
            }
        });

        clientLayer = new tiles.ClientLayer({
            source: layers["top-tweets"],
            renderer: new tiles.WordCloudRenderer({
                text: {
                    textKey: "topic",
                    countKey: "countMonthly",
                    themes: [
                        new tiles.RenderTheme( ".dark-theme", {
                            'color': "#FFFFFF",
                            'color:hover': "#09CFFF",
                            'text-shadow': "#000"
                        })
                    ]
                },
                hook: function( elem, entry, entries, data ) {
                    elem.onclick = function() {
                        console.log( elem );
                        console.log( entry );
                    };
                }
            })
        });

        axis0 = new tiles.Axis({
            position: 'bottom',
            title: 'Longitude',
            repeat: true,
            intervals: {
                type: 'fixed',
                increment: 120,
                pivot: 0
            },
            units: {
                type: 'degrees'
            }
        });

        axis1 =  new tiles.Axis({
            position: 'left',
            title: 'Latitude',
            repeat: true,
            intervals: {
                type: 'fixed',
                increment: 60
            },
            units: {
                type: 'degrees'
            }
        });

        map = new tiles.Map( "map" );
        map.add( serverLayer );
        map.add( clientLayer );
        map.add( axis0 );
        map.add( axis1 );
        map.add( baseLayer );

    });
}