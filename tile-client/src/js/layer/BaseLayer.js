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
		this.url = spec.url;
		this.attribution = spec.attribution;
        this.options = spec.options || {
            color : "rgb(0,0,0)"
        };
        this.domain = "base";
    }

    BaseLayer.prototype = Object.create( Layer.prototype );

    /**
     * Activates the layer object. This should never be called manually.
     * @memberof BaseLayer
     * @private
     */
    BaseLayer.prototype.activate = function() {
        var styledMapType;
		// create base layer based on input type
        switch ( this.type.toLowerCase() ) {
            case "blank":
				// blank layer
                this.olLayer = new OpenLayers.Layer.Vector( "BaseLayer", {} );
				var mapElem = $( this.map.getElement() );
				mapElem.css( 'background-color', '' );
				mapElem.attr( 'style', mapElem.attr('style') + "; background-color: " + this.options.color +" !important" );
                break;
            case "google":
				// google maps layer
                if ( this.options.styles ) {
                    this.options.type = "styled";
                }
                this.olLayer = new OpenLayers.Layer.Google( "BaseLayer", this.options );
                break;
            case "tms":
				// tms layer
                this.olLayer = new OpenLayers.Layer.TMS( "BaseLayer", this.url, this.options );
                break;
			case "xyz":
				// xyz layer
                this.olLayer = new OpenLayers.Layer.XYZ( "BaseLayer", this.url, this.options );
                break;
        }
		if ( this.attribution ) {
			$( this.map.getElement() ).append(
				'<div class=baselayer-attribution>' +
				this.attribution +
				'</div>' );
		}
        // publish activate event before appending to map
        PubSub.publish( this.getChannel(), { field: 'activate', value: true } );
		// create baselayer and set as baselayer
        this.map.olMap.addLayer( this.olLayer );
        this.map.olMap.setBaseLayer( this.olLayer );
		// if google maps layer, set styles according to spec
        if ( this.options.styles ) {
            styledMapType = new google.maps.StyledMapType( this.options.styles, {name: 'Styled Map'} );
            this.olLayer.mapObject.mapTypes.set( 'styled', styledMapType );
        }
		if ( this.olLayer.mapObject ) {
			var gmapContainer = this.olLayer.mapObject.getDiv();
			$( gmapContainer ).css( "background-color", "rgba(0,0,0,0)" );
		}
        // ensure baselayer remains bottom layer
		$( this.olLayer.div ).css( 'z-index', -1 );
        // reset visibility / opacity
        this.setOpacity( this.getOpacity() );
        this.setEnabled( this.isEnabled() );
        this.setBrightness( this.getBrightness() );
        this.setContrast( this.getContrast() );
        // publish add event
        PubSub.publish( this.getChannel(), { field: 'add', value: true } );
    };

    /**
     * Dectivates the layer object. This should never be called manually.
     * @memberof BaseLayer
     * @private
     */
    BaseLayer.prototype.deactivate = function() {
        if ( this.olLayer ) {
            this.map.olMap.removeLayer( this.olLayer );
            PubSub.publish( this.getChannel(), { field: 'remove', value: true } );
            this.olLayer.destroy();
        }
		if ( this.attribution ) {
			$( this.map.getElement() ).find('.baselayer-attribution').remove();
		}
        this.map.getElement().style['background-color'] = '';
        PubSub.publish( this.getChannel(), { field: 'deactivate', value: true } );
    };

	/**
     * Set the z index of the layer.
     * @memberof BaseLayer
     *
     * @param {integer} zIndex - The new z-order value of the layer, where 0 is front.
     */
	BaseLayer.prototype.resetZIndex = function () {
        // we by-pass the OpenLayers.Map.setLayerIndex() method and manually
        // set the z-index of the layer dev. setLayerIndex sets a relative
        // index based on current map layers, which then sets a z-index. This
        // caused issues with async layer loading.
        if ( this.olLayer ) {
            $( this.olLayer.div ).css( 'z-index', -1 );
        }
        PubSub.publish( this.getChannel(), { field: 'zIndex', value: -1 });
    };

	module.exports = BaseLayer;
}());
