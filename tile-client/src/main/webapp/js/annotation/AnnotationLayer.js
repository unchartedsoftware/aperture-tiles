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

 /*global OpenLayers */
define(function (require) {
    "use strict";



    var Class        = require('../class'),
        AoIPyramid   = require('../binning/AoITilePyramid'),
        TileIterator = require('../binning/TileIterator'),
        AnnotationTracker = require('./AnnotationTracker'),
        defaultStyle,
        hoverStyle,
        selectStyle,
        temporaryStyle,
        AnnotationLayer;


        defaultStyle = new OpenLayers.Style({
            externalGraphic: 'http://www.openlayers.org/dev/img/marker.png', 
            graphicWidth: 21, 
            graphicHeight: 25,
            graphicYOffset: -24,
            'cursor': 'pointer'              
        });

        hoverStyle = new OpenLayers.Style({
            externalGraphic: 'http://www.openlayers.org/dev/img/marker-green.png', 
            graphicWidth: 21, 
            graphicHeight: 25,
            graphicYOffset: -24,
            'cursor': 'pointer'                 
        });

        selectStyle = new OpenLayers.Style({
            externalGraphic: 'http://www.openlayers.org/dev/img/marker-blue.png',
            graphicWidth: 21, 
            graphicHeight: 25,
            graphicYOffset: -24,
            'cursor': 'pointer'                  
        });

        temporaryStyle = new OpenLayers.Style({
            display:"none"               
        });



    AnnotationLayer = Class.extend({


        init: function (spec) {

            var that = this;

            this.map = spec.map;
            this.projection = spec.projection;
            this.layer = spec.layer;
            this.filters = spec.filters;
            this.tracker = new AnnotationTracker( spec.layer );

            // set callbacks
            this.map.olMap_.events.register('zoomend', that.map.olMap_, function() {
                that._olLayer.destroyFeatures();
                that.onMapUpdate();
            });

            this.map.on('panend', function() {
                that.onMapUpdate();
            });

            this.createLayer();
            // trigger callback to draw first frame
            this.onMapUpdate();
        },


        onMapUpdate: function() {

            var tiles,
                level = this.map.getZoom(),
                bounds = this.map.olMap_.getExtent(),
                mapExtents = this.map.olMap_.getMaxExtent(),
                mapPyramid = new AoIPyramid(mapExtents.left, mapExtents.bottom,
                                            mapExtents.right, mapExtents.top);

            // determine all tiles in view
            tiles = new TileIterator(mapPyramid, level,
                                     bounds.left, bounds.bottom,
                                     bounds.right, bounds.top).getRest();

            this.tracker.getAnnotations( tiles, $.proxy( this.onAnnotationReceive, this ) );
        },


        onAnnotationReceive: function( annotations ) {

            var i;
            console.log("OnReceive");
            for (i=0; i<annotations.length; i++) {
                console.log( "x: "+annotations[i].x );
                console.log( "y: "+annotations[i].y );
                console.log( "priority: "+annotations[i].priority );
                console.log( "data: "+annotations[i].data );
            }
            this.createAnnotations( annotations );
                
        },

        createLayer: function() {

            var that = this,          
                addPointControl,
                selectControl,
                dragControl,
                highlightControl;

            this._olLayer = new OpenLayers.Layer.Vector( "annotation-layer-"+this.layer, { 
                ratio: 2,
                styleMap: new OpenLayers.StyleMap({ 
                    "default" : defaultStyle,
                    "hover": hoverStyle,
                    "select": selectStyle,
                    "temporary": temporaryStyle
                }),
                eventListeners: {

                    /*
                    "featureselected": function(e) {
                    
                        var latlon = OpenLayers.LonLat.fromString(e.feature.geometry.toShortString()),
                            px,
                            size,
                            popup = new OpenLayers.Popup("marker-popup",
                                                     latlon, //OpenLayers.LonLat.fromString(e.feature.geometry.toShortString()),
                                                     null,
                                                     "<div style='padding-top:5px; padding-left:15px;'>"+
                                                     "<div style='font-weight:bold; padding-bottom:10px'>Title Here</div>"+
                                                     "<div style='width:200px;height:80px;overflow:auto;'>"+
                                                     "Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.</div>"+
                                                     "<button style='margin-top:10px; border-radius:5px;'>Button 1</button>"+
                                                     "<button style='margin-top:10px; border-radius:5px;'>Button 2</button>"+
                                                     "<button style='margin-top:10px; border-radius:5px;'>Button 3</button>"+
                                                     "</div>",
                                                     true);
                                                                                              

                        popup.backgroundColor = '#222222';
                        popup.autoSize = true;
                        popup.panMapIfOutOfView = true;
                        e.feature.popup = popup;
                        that.map.olMap_.addPopup(popup, false);

                        latlon = OpenLayers.LonLat.fromString(e.feature.geometry.toShortString());
                        px = that.map.olMap_.getLayerPxFromViewPortPx( that.map.olMap_.getPixelFromLonLat(latlon) );
                        size = popup.size;
                        px.x -= size.w / 2;
                        px.y -= size.h + 25;
                        popup.moveTo( px );
                    },
                    */
                    /*
                    "featureunselected": function(e) {
                        
                        that.map.olMap_.removePopup(e.feature.popup);
                        e.feature.popup.destroy();
                        e.feature.popup = null;
                        
                    },
                    */
                    "featureadded": function(e) {
                        var mapExtents = that.map.olMap_.getMaxExtent(),
                            mapPyramid = new AoIPyramid(mapExtents.left, mapExtents.bottom,
                                                    mapExtents.right, mapExtents.top),
                            projection = new OpenLayers.Projection( mapPyramid.getProjection ),
                            latlon = OpenLayers.LonLat.fromString( e.feature.geometry.toShortString() ),
                            trueLatLon = new OpenLayers.LonLat( latlon.lon, latlon.lat ).transform( that.projection, projection );
                                
                        console.log("Added: "+trueLatLon.lon +","+trueLatLon.lat);
                    }
                }
            });

            this.map.olMap_.addLayer( this._olLayer );

            addPointControl = new OpenLayers.Control.DrawFeature( this._olLayer, OpenLayers.Handler.Point );
            selectControl = new OpenLayers.Control.SelectFeature( this._olLayer, {
                clickout: true
            });

            dragControl = new OpenLayers.Control.DragFeature( this._olLayer, {
                onStart: function(feature, pixel){
                    selectControl.clickFeature(feature);
                }
                /*
                onDrag: function(feature, pixel){

                    var latlon = OpenLayers.LonLat.fromString(feature.geometry.toShortString()),
                        px = that.map.olMap_.getLayerPxFromViewPortPx( that.map.olMap_.getPixelFromLonLat(latlon) ),
                        size = feature.popup.size;
                    px.x -= size.w / 2;
                    px.y -= size.h + 25;
                    feature.popup.moveTo( px );
                }
                */
                                                   
            });
            highlightControl = new OpenLayers.Control.SelectFeature( this._olLayer, {
                hover: true,
                highlightOnly: true,
                renderIntent: "hover"

            });

            this.map.olMap_.addControl(addPointControl);
            this.map.olMap_.addControl(dragControl);
            this.map.olMap_.addControl(highlightControl);
            this.map.olMap_.addControl(selectControl);

            addPointControl.activate();
            dragControl.activate();
            highlightControl.activate();
            selectControl.activate();
        },

        createAnnotations: function( annotations ) {

            var i,
                points =[],
                mapExtents = this.map.olMap_.getMaxExtent(),
                mapPyramid = new AoIPyramid(mapExtents.left, mapExtents.bottom,
                                        mapExtents.right, mapExtents.top),
                projection = new OpenLayers.Projection( mapPyramid.getProjection ),
                latlon;


            for (i=0; i<annotations.length; i++) {

                latlon = new OpenLayers.LonLat( annotations[i].x, annotations[i].y ).transform( projection, this.projection );
                points.push( new OpenLayers.Feature.Vector( new OpenLayers.Geometry.Point( latlon.lon, 
                                                                                           latlon.lat) ) ); 
            }

            this._olLayer.addFeatures( points );
        }


     });

    return AnnotationLayer;
});
