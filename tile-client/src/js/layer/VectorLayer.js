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

(function() {

    "use strict";

    var Layer = require('./Layer'),
        PubSub = require('../util/PubSub');

    /**
     * Instantiate a VectorLayer object.
     * @class VectorLayer
     * @augments Layer
     * @classdesc A client rendered layer object.
     *
     * @param {Object} spec - The specification object.
     * <pre>
     * {
     *     opacity  {float}    - The opacity of the layer. Default = 1.0
     *     enabled  {boolean}  - Whether the layer is visible or not. Default = true
     *     zIndex   {integer}  - The z index of the layer. Default = 1000
     *     vectors {Array}     - Array of OpenLayers Vector objects to add to the map
     * }
     * </pre>
     */
    function VectorLayer(spec) {
        // call base constructor
        Layer.call(this, spec);
        // set reasonable defaults
        this.zIndex = spec.zIndex || 749;
        this.domain = spec.domain || "vector";
        this.source = spec.source;
        this.styleMap = spec.styleMap;
        this.strategies = spec.strategies;
        this.getData = spec.getData;
        this.teardown = spec.teardown;
        this.olFeatures = spec.olFeatures || [];
        this.group = spec.group || "";
    }

    VectorLayer.prototype = Object.create(Layer.prototype);

    /**
     * Activates the layer object. This should never be called manually.
     * @memberof VectorLayer
     * @private
     */
    VectorLayer.prototype.activate = function() {
        var layerSpec = {};
        if ( this.strategies ) {
            layerSpec.strategies = this.strategies;
        }
        if ( this.styleMap ) {
            layerSpec.styleMap = this.styleMap;
        }
        this.olLayer = new OpenLayers.Layer.Vector("Vector Layer", layerSpec);
        this.setEnabled( this.isEnabled() );
        this.setOpacity( this.getOpacity() );
        this.setBrightness( this.getBrightness() );
        this.setContrast( this.getContrast() );
        this.setTheme( this.map.getTheme() );
        // publish activate event before appending to map
        PubSub.publish( this.getChannel(), { field: 'activate', value: true } );
        this.map.olMap.addLayer( this.olLayer );
        if ( this.getData ) {
            this.getData( this );
        }
        if ( this.olFeatures ) {
            this.setFeatures( this.olFeatures );
        }
        this.setZIndex( this.zIndex );
        // publish add event
        PubSub.publish( this.getChannel(), { field: 'add', value: true } );
    };

    /**
     * Dectivates the layer object. This should never be called manually.
     * @memberof VectorLayer
     * @private
     */
    VectorLayer.prototype.deactivate = function() {
        if (this.teardown) {
            this.teardown();
        }
        if (this.olLayer) {
            if (this.olLayer.strategies) {
                this.olLayer.strategies.forEach(function(strategy) {
                    strategy.deactivate();
                });
            }
            this.olLayer.strategies = [];
            this.map.olMap.removeLayer(this.olLayer);
            PubSub.publish( this.getChannel(), { field: 'remove', value: true } );
            this.olLayer.destroyFeatures();
            this.olLayer.destroy();
            this.olLayer = null;
        }
        PubSub.publish( this.getChannel(), { field: 'deactivate', value: true } );
    };

    /**
     * Remove all features from the layer and add the new features
     * passed in
     *
     * @param {Array} featuresToAdd - Array of OpenLayers Features
     */
    VectorLayer.prototype.setFeatures = function(featuresToAdd) {
        if (this.olLayer) {
            this.olLayer.destroyFeatures();
            this.olFeatures = featuresToAdd;
            this.olLayer.addFeatures(featuresToAdd);
        }
    };

    /**
     * Updates the theme associated with the layer.
     * @memberof VectorLayer
     *
     * @param {String} theme - The theme identifier string.
     */
    VectorLayer.prototype.setTheme = function(theme) {
        this.theme = theme;
    };

    /**
     * Get the current theme for the layer.
     * @memberof VectorLayer
     *
     * @returns {String} The theme identifier string.
     */
    VectorLayer.prototype.getTheme = function() {
        return this.theme;
    };

    /**
     * Set the z index of the layer.
     * @memberof VectorLayer
     *
     * @param {integer} zIndex - The new z-order value of the layer, where 0 is front.
     */
    VectorLayer.prototype.setZIndex = function(zIndex) {
        // we by-pass the OpenLayers.Map.setLayerIndex() method and manually
        // set the z-index of the layer dev. setLayerIndex sets a relative
        // index based on current map layers, which then sets a z-index. This
        // caused issues with async layer loading.
        this.zIndex = zIndex;
        if (this.olLayer) {
            $(this.olLayer.div).css('z-index', zIndex);
            PubSub.publish(this.getChannel(), {
                field: 'zIndex',
                value: zIndex
            });
        }
    };

    /**
     * Get the layers zIndex.
     * @memberof VectorLayer
     *
     * @returns {integer} The zIndex for the layer.
     */
    VectorLayer.prototype.getZIndex = function() {
        return this.zIndex;
    };

    module.exports = VectorLayer;
}());
