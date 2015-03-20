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
		PubSub = require('../util/PubSub');

    /**
     * Instantiate a BaseLayer object.
     * @class BaseLayer
     * @augments Layer
     * @classdesc A base layer object that serves as the underlying layer of the
     *            map. Supports blank baselayers that are simply a color, geographic
     *            baselayers using the Google Maps API, or standard TMS layers.
     *
     * @param spec {Object} The specification object.
     * <pre>
     * {
     *     type    {String}  - The type of baselayer, ["Blank", "Google", "TMS"]. Default = "Blank"
     *     opacity {float}   - The opacity of the layer. Default = 1.0
     *     enabled {boolean} - Whether the layer is visible or not. Default = true
     *     url     {String}  - if TMS layer, the url for tile requests. Default = undefined
     *     options {Object}  - type specific instantiation attributes. Default = {color:rgb(0,0,0)}
     * }
     *</pre>
     */
	function BaseLayer( spec ) {
        spec = spec || {};
        // call base constructor
        Layer.call( this, spec );
        // set defaults
        this.type = spec.type || "blank";
        this.options = spec.options || {
            color : "rgb(0,0,0)"
        };
        this.domain = "base";
    }

    BaseLayer.prototype = Object.create( Layer.prototype );

    /**
     * Activates the layer object. This should never be called manually.
     * @memberof AnnotationLayer
     * @private
     */
    BaseLayer.prototype.activate = function() {

        var styledMapType;

        switch ( this.type.toLowerCase() ) {

            case "blank":

                this.olLayer = new OpenLayers.Layer.Vector( "BaseLayer", {} );
                this.map.getElement().style['background-color'] = this.options.color;
                break;

            case "google":

                if ( this.options.styles ) {
                    this.options.type = "styled";
                }
                this.olLayer = new OpenLayers.Layer.Google( "BaseLayer", this.options );
                break;

            case "tms":

                this.olLayer = new OpenLayers.Layer.TMS( "BaseLayer", this.url, this.options );
                break;
        }

        this.map.olMap.addLayer( this.olLayer );
        this.map.olMap.setBaseLayer( this.olLayer );

        if ( this.options.styles ) {
            styledMapType = new google.maps.StyledMapType( this.options.styles, {name: 'Styled Map'} );
            this.olLayer.mapObject.mapTypes.set( 'styled', styledMapType );
        }

        // ensure baselayer remains bottom layer
        this.map.olMap.setLayerIndex( this.olLayer, -1 );
        // reset visibility / opacity
        this.setOpacity( this.getOpacity() );
        this.setEnabled( this.isEnabled() );
		PubSub.publish( this.getChannel(), { field: 'activate', value: true } );
    };

    /**
     * Dectivates the layer object. This should never be called manually.
     * @memberof AnnotationLayer
     * @private
     */
    BaseLayer.prototype.deactivate = function() {
        if ( this.olLayer ) {
            this.map.olMap.removeLayer( this.olLayer );
            this.olLayer.destroy();
        }
        this.map.getElement().style['background-color'] = '';
        PubSub.publish( this.getChannel(), { field: 'deactivate', value: true } );
    };

	module.exports = BaseLayer;
}());
