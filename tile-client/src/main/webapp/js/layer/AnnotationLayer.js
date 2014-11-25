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

define( function( require ) {
    "use strict";

    var Layer = require('./Layer'),
        HtmlTileLayer = require('./HtmlTileLayer'),
        PubSub = require('../util/PubSub');

    function AnnotationLayer( spec ) {
        // set reasonable defaults
        spec.enabled = ( spec.enabled !== undefined ) ? spec.enabled : true;
        spec.opacity = ( spec.opacity !== undefined ) ? spec.opacity : 1.0;
        spec.zIndex = ( spec.zIndex !== undefined ) ? spec.zIndex : 1000;
        spec.domain = "client";
        // call base constructor    
        Layer.call( this, spec );
    }

    AnnotationLayer.prototype = Object.create( Layer.prototype );

    AnnotationLayer.prototype.activate = function() {

        function getURL( bounds ) {
            var res = this.map.getResolution(),
                maxBounds = this.maxExtent,
                tileSize = this.tileSize,
                x = Math.round( (bounds.left-maxBounds.left) / (res*tileSize.w) ),
                y = Math.round( (bounds.bottom-maxBounds.bottom) / (res*tileSize.h) ),
                z = this.map.getZoom();
            if ( x >= 0 && y >= 0 ) {
                return this.url + this.layername
                    + "/" + z + "/" + x + "/" + y + "."
                    + this.type;
            }
        }

        // add the new layer
        this.layer = new HtmlTileLayer(
            'Aperture Tile Layers',
            this.spec.source.tms,
            {
                layername: this.spec.source.id,
                type: 'json',
                maxExtent: new OpenLayers.Bounds(-20037500, -20037500,
                                                  20037500,  20037500),
                isBaseLayer: false,
                getURL: getURL,
                html: this.spec.html
            });

        this.map.map.addLayer( this.layer );

        this.setZIndex( this.spec.zIndex );
        this.setOpacity( this.spec.opacity );
        this.setVisibility( this.spec.enabled );
        this.setTheme( this.map.getTheme() );

        if ( typeof this.spec.html === "object" ) {
            this.spec.html.meta = this.spec.source.meta.meta;
            this.spec.html.map = this.map;
            this.spec.html.parent = this;
        }
    };

    AnnotationLayer.prototype.deactivate = function() {
        // TODO: implement
        return true;
    };

    /**
     * Updates the theme associated with the layer
     */
    AnnotationLayer.prototype.setTheme = function( theme ) {
        this.spec.theme = theme;
    };

    /**
     * Get the current theme for the layer
     */
    AnnotationLayer.prototype.getTheme = function() {
        return this.spec.theme;
    };

    /**
     * @param {number} zIndex - The new z-order value of the layer, where 0 is front.
     */
    AnnotationLayer.prototype.setZIndex = function ( zIndex ) {
        // we by-pass the OpenLayers.Map.setLayerIndex() method and manually
        // set the z-index of the layer dev. setLayerIndex sets a relative
        // index based on current map layers, which then sets a z-index. This
        // caused issues with async layer loading.
        this.spec.zIndex = zIndex;
        $( this.layer.div ).css( 'z-index', zIndex );
        PubSub.publish( this.getChannel(), { field: 'zIndex', value: zIndex });
    };

    /**
     * Get the layers zIndex
     */
    AnnotationLayer.prototype.getZIndex = function () {
        return this.spec.zIndex;
    };

    /**
     * Create the a new annotation
     */
    AnnotationLayer.prototype.write = function( position ) {

        var that = this,
            coord,
            tilekey;

        // temp for debug writing
        function DEBUG_ANNOTATION( coord ) {
            var randomGroupIndex = Math.floor( that.spec.groups.length*Math.random() );
            return {
                x: coord.x,
                y: coord.y,
                group: that.spec.groups[ randomGroupIndex ],
                range: {
                    min: 0,
                    max: that.map.getZoom()
                },
                level: that.map.getZoom(),
                data: {}
            };
        }

        // get position and tilekey for annotation
        coord = this.map.getCoordFromViewportPixel( position.x, position.y );
        tilekey = this.map.getTileKeyFromViewportPixel( position.x, position.y );

        // write annotation
        $.post( '/v1.0/annotation/',
                {
                    type: "write",
                    annotation: DEBUG_ANNOTATION( coord ),
                    layer: this.spec.source.id
                }
            ).then(
                function() {
                   // TODO: refresh tile
                },
                function( jqXHR, status, error ) {
                    // TODO: handle error
                    return true;
                }
            );
    };


    /**
     * Modify an existing annotation
     */
    AnnotationLayer.prototype.modify = function( annotation ) {
        $.post( '/v1.0/annotation/',
                {
                    type: "modify",
                    annotation: annotation,
                    layer: this.spec.source.id
                }
            ).then(
                function() {
                   // TODO: request old and new tile locations in case of failure
                },
                function( jqXHR, status, error ) {
                    // TODO: handle error
                    return true;
                }
            ).always( function() {
                // TODO: request old and new tile locations in case of failure
                return true;
            });
    };


    /**
     * Remove an existing annotation.
     */
    AnnotationLayer.prototype.remove = function( annotation ) {
        $.post( '/v1.0/annotation/',
                {
                    type: "remove",
                    certificate: annotation.certificate,
                    layer: this.spec.source.id
                }
            ).then(
                function() {
                    // TODO: refresh tile
                    return true;
                },
                function( jqXHR, status, error ) {
                    // TODO: handle error
                    return true;
                }
            );
    };

    return AnnotationLayer;
});
