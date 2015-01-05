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
     *     renderer {Renderer} - The tile renderer object. (optional)
     *     html     {String|Function|HTMLElement|jQuery} - The html for the tile. (optional)
     * }
     * </pre>
     */
    function AnnotationLayer( spec ) {
        // set reasonable defaults
        spec.zIndex = ( spec.zIndex !== undefined ) ? spec.zIndex : 500;
        spec.domain = "annotation";
        // call base constructor    
        Layer.call( this, spec );
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
            this.spec.source.tms,
            {
                layername: this.spec.source.id,
                type: 'json',
                maxExtent: new OpenLayers.Bounds(-20037500, -20037500,
                    20037500,  20037500),
                isBaseLayer: false,
                getURL: LayerUtil.getURL,
                html: this.spec.html,
                renderer: this.spec.renderer,
                entry: this.spec.entry
            });

        this.map.olMap.addLayer( this.olLayer );

        this.setZIndex( this.spec.zIndex );
        this.setOpacity( this.spec.opacity );
        this.setVisibility( this.spec.enabled );
        this.setTheme( this.map.getTheme() );

        if ( this.spec.renderer ) {
            this.spec.renderer.meta = this.spec.source.meta.meta;
            this.spec.renderer.map = this.map;
            this.spec.renderer.parent = this;
        }
    };

    /**
     * Dectivates the layer object. This should never be called manually.
     * @memberof AnnotationLayer
     * @private
     */
    AnnotationLayer.prototype.deactivate = function() {
        if ( this.olLayer ) {
            this.map.olMap.removeLayer( this.olLayer );
            this.olLayer.destroy();
        }
    };

    /**
     * Updates the theme associated with the layer.
     * @memberof AnnotationLayer
     *
     * @param {String} theme - The theme identifier string.
     */
    AnnotationLayer.prototype.setTheme = function( theme ) {
        this.spec.theme = theme;
    };

    /**
     * Get the current theme for the layer.
     * @memberof AnnotationLayer
     *
     * @returns {String} The theme identifier string.
     */
    AnnotationLayer.prototype.getTheme = function() {
        return this.spec.theme;
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
        this.spec.zIndex = zIndex;
        $( this.olLayer.div ).css( 'z-index', zIndex );
        PubSub.publish( this.getChannel(), { field: 'zIndex', value: zIndex });
    };

    /**
     * Get the layers zIndex.
     * @memberof AnnotationLayer
     *
     * @returns {integer} The zIndex for the layer.
     */
    AnnotationLayer.prototype.getZIndex = function () {
        return this.spec.zIndex;
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
            this.spec.source.id,
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
            this.spec.source.id,
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
            this.spec.source.id,
            certificate,
            function() {
               // TODO: refresh tile
                callback();
            });
    };

    module.exports = AnnotationLayer;
}());
