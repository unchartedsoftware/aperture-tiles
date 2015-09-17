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

( function() {

    "use strict";

    var Layer = require('./Layer'),
        LayerUtil = require('./LayerUtil'),
        Util = require('../util/Util'),
        AnnotationService = require('../rest/AnnotationService'),
        HtmlTileLayer = require('./HtmlTileLayer'),
        PubSub = require('../util/PubSub');

    /**
     * Ensures an annotation object meets the minimum attribute requirements.
     *
     * @param {Object} annotation - The annotation object.
     *
     * @returns {boolean} Whether or not the input is valid.
     */
    function isAnnotationInputValid( annotation ) {
        if ( annotation.x === undefined ) {
            console.error( "Annotation argument is missing the 'x' attribute");
            return false;
        }
        if ( annotation.y === undefined ) {
            console.error( "Annotation argument is missing the 'y' attribute");
            return false;
        }
        if ( annotation.level === undefined ) {
            console.error( "Annotation argument is missing the 'level' attribute");
            return false;
        }
        if ( annotation.range === undefined ) {
            console.error( "Annotation argument is missing the 'range' attribute");
            return false;
        }
        if ( annotation.group === undefined ) {
            console.error( "Annotation argument is missing the 'group' attribute");
            return false;
        }
        if ( annotation.data === undefined ) {
            console.error( "Annotation argument is missing the 'data' attribute");
            return false;
        }
        return true;
    }

    /**
     * Instantiate an AnnotationLayer object.
     * @class AnnotationLayer
     * @augments Layer
     * @classdesc A client rendered layer object. Uses JSON data retrieved from the
     *            server in conjunction with a Renderer object or html function to
     *            create interactable DOM elements. AnnotationLayers differ from
     *            ClientLayers in that the data they represent is mutable.
     *
     * @param {Object} spec - The specification object.
     * <pre>
     * {
     *     opacity  {float}    - The opacity of the layer. Default = 1.0
     *     enabled  {boolean}  - Whether the layer is visible or not. Default = true
     *     zIndex   {integer}  - The z index of the layer. Default = 1000
     *     renderer {Renderer} - The tile renderer object.
     * }
     * </pre>
     */
    function AnnotationLayer( spec ) {
        var that = this,
            getURL = spec.getURL || LayerUtil.getURL;
        // call base constructor
        Layer.call( this, spec );
        // set reasonable defaults
        this.zIndex = ( spec.zIndex !== undefined ) ? parseInt( spec.zIndex, 10 ) : 500;
        this.filter = spec.filter || {};
        this.domain = "annotation";
        this.source = spec.source;
        this.getURL = function( bounds ) {
            return getURL.call( this, bounds ) + that.getQueryParamString();
        };
        if ( spec.tileClass) {
            this.tileClass = spec.tileClass;
        }
        if ( spec.renderer ) {
            this.setRenderer( spec.renderer );
        }
    }

    AnnotationLayer.prototype = Object.create( Layer.prototype );

    /**
     * Activates the layer object. This should never be called manually.
     * @memberof AnnotationLayer
     * @private
     */
    AnnotationLayer.prototype.activate = function() {
        // add the new layer
        this.olLayer = new HtmlTileLayer(
            'Annotation Tile Layer',
            this.source.tms,
            {
                layername: this.source.id,
                type: 'json',
                maxExtent: new OpenLayers.Bounds(-20037500, -20037500,
                    20037500,  20037500),
                isBaseLayer: false,
                getURL: this.getURL,
                tileClass: this.tileClass,
                renderer: this.renderer
            });
        // set whether it is enabled or not before attaching, to prevent
        // needless tile requests
        this.setEnabled( this.isEnabled() );
        this.setTheme( this.map.getTheme() );
        this.setOpacity( this.getOpacity() );
        this.setBrightness( this.getBrightness() );
        this.setContrast( this.getContrast() );
        // publish activate event before appending to map
        PubSub.publish( this.getChannel(), { field: 'activate', value: true } );
        // attach to map
        this.map.olMap.addLayer( this.olLayer );
        // set z-index after
        this.setZIndex( this.zIndex );
        // publish add event
        PubSub.publish( this.getChannel(), { field: 'add', value: true } );
    };

    /**
     * Dectivates the layer object. This should never be called manually.
     * @memberof AnnotationLayer
     * @private
     */
    AnnotationLayer.prototype.deactivate = function() {
        if ( this.olLayer ) {
            this.map.olMap.removeLayer( this.olLayer );
            PubSub.publish( this.getChannel(), { field: 'remove', value: true } );
            this.olLayer.destroy();
            this.olLayer = null;
        }
        PubSub.publish( this.getChannel(), { field: 'deactivate', value: true } );
    };

    /**
     * Sets the current renderer of the layer.
     * @memberof AnnotationLayer
     *
     * @param {Renderer} renderer - The renderer to attach to the layer.
     */
     AnnotationLayer.prototype.setRenderer = function( renderer ) {
        this.renderer = renderer;
        this.renderer.attach( this );
    };

    /**
     * Updates the theme associated with the layer.
     * @memberof AnnotationLayer
     *
     * @param {String} theme - The theme identifier string.
     */
    AnnotationLayer.prototype.setTheme = function( theme ) {
        this.theme = theme;
    };

    /**
     * Get the current theme for the layer.
     * @memberof AnnotationLayer
     *
     * @returns {String} The theme identifier string.
     */
    AnnotationLayer.prototype.getTheme = function() {
        return this.theme;
    };

    /**
     * Set the z index of the layer.
     * @memberof AnnotationLayer
     *
     * @param {integer} zIndex - The new z-order value of the layer, where 0 is front.
     */
    AnnotationLayer.prototype.setZIndex = function ( zIndex ) {
        // we by-pass the OpenLayers.Map.setLayerIndex() method and manually
        // set the z-index of the layer dev. setLayerIndex sets a relative
        // index based on current map layers, which then sets a z-index. This
        // caused issues with async layer loading.
        this.zIndex = zIndex;
        if ( this.olLayer ) {
            $( this.olLayer.div ).css( 'z-index', zIndex );
        }
        PubSub.publish( this.getChannel(), { field: 'zIndex', value: zIndex });
    };

    /**
     * Get the layers zIndex.
     * @memberof AnnotationLayer
     *
     * @returns {integer} The zIndex for the layer.
     */
    AnnotationLayer.prototype.getZIndex = function () {
        return this.zIndex;
    };

    /**
     * Write the a new annotation to the layer.
     * @memberof AnnotationLayer
     *
     * @param {Object} annotation - The target annotation.
     * @param {Function} callback - The callback function executing on success.
     */
    AnnotationLayer.prototype.write = function( annotation, callback ) {
        if ( !isAnnotationInputValid( annotation ) ) {
            return;
        }
        AnnotationService.writeAnnotation(
            this.source.id,
            annotation,
            function() {
                // TODO: refresh tile
                callback();
            });
    };

    /**
     * Modify an existing annotation in the layer.
     * @memberof AnnotationLayer
     *
     * @param {Object} annotation - The target annotation.
     * @param {Function} callback - The callback function executing on success.
     */
    AnnotationLayer.prototype.modify = function( annotation, callback ) {
        if ( !isAnnotationInputValid( annotation ) ) {
            return;
        }
        AnnotationService.modifyAnnotation(
            this.source.id,
            annotation,
            function() {
                // TODO: refresh tile
                callback();
            });
    };

    /**
     * Remove an existing annotation from the layer.
     * @memberof AnnotationLayer
     *
     * @param {Object} certificate - The target annotation certificate.
     * @param {Function} callback - The callback function executing on success.
     */
    AnnotationLayer.prototype.remove = function( certificate, callback ) {
        AnnotationService.removeAnnotation(
            this.source.id,
            certificate,
            function() {
               // TODO: refresh tile
                callback();
            });
    };

	/**
     * Set the layer's filter function type.
     * @memberof AnnotationLayer
     *
     * @param {String} filterType - The annotation filter type.
     */
    AnnotationLayer.prototype.setFilterType = function ( filterType ) {
        if ( this.filter.type !== filterType ) {
            this.filter.type = filterType;
            this.redraw();
            PubSub.publish( this.getChannel(), {field: 'filterType', value: filterType} );
        }
    };

    /**
     * Get the layers filter type.
     * @memberof AnnotationLayer
     *
     * @return {String} The tile filter type.
     */
    AnnotationLayer.prototype.getFilterType = function () {
        return this.filter.type;
    };


	/**
     * Set the annotation filter data attribute
     * @memberof AnnotationLayer
     *
     * @param {Object} filterData - The filter data attribute.
     */
    AnnotationLayer.prototype.setFilterData = function ( filterData ) {
        if ( this.filter.data !== filterData ) {
            this.filter.data = filterData;
            this.redraw();
            PubSub.publish( this.getChannel(), {field: 'filterData', value: filterData} );
        }
    };

	/**
     * Get the filter data attribute.
     * @memberof AnnotationLayer
     *
     * @returns {Object} The tile filter data attribute.
     */
    AnnotationLayer.prototype.getFilterData = function () {
        return this.filter.data || {};
    };

    /**
     * Generate query parameters based on state of layer
     * @memberof AnnotationLayer
     *
     * @returns {String} The query parameter string based on the attributes of this layer.
     */
     AnnotationLayer.prototype.getQueryParamString = function() {
        var query = {
            filter: this.filter
        };
        return Util.encodeQueryParams( query );
    };

    /**
     * Redraws the entire layer.
     * @memberof ServerLayer
     */
    AnnotationLayer.prototype.redraw = function () {
        if ( this.olLayer ) {
            this.olLayer.redraw();
            // If we're using the TileManager we need to force it into a refresh. There is no nice way to
            // do this as of 2.13.1, so we fake the expiry of the move/zoom timeout.
            if ( this.olLayer.map && this.olLayer.map.tileManager ) {
                this.olLayer.map.tileManager.updateTimeout(
                    this.olLayer.map,
                    this.olLayer.map.tileManager.zoomDelay,
                    true );
            }
        }
    };

    module.exports = AnnotationLayer;
}());
