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

/*global google */
define( function( require ) {
	"use strict";

	var Layer = require('./Layer');

	function BaseLayer( spec ) {
        // set defaults
        spec = spec || {};
        spec.opacity = ( spec.opacity !== undefined ) ? spec.opacity : 1.0;
        spec.enabled = ( spec.enabled !== undefined ) ? spec.enabled : true;
        spec.type = spec.type || "Blank";
        spec.options = spec.options || {
            name : "black",
            color : "rgb(0,0,0)"
        };
        spec.domain = "base";
        // call base constructor
        Layer.call( this, spec );
    }

    BaseLayer.prototype = Object.create( Layer.prototype );

    BaseLayer.prototype.activate = function() {

        var spec = this.spec,
            styledMapType;

        switch ( this.spec.type ) {

            case "Blank":

                this.olLayer = new OpenLayers.Layer.Vector( "BaseLayer", {} );
                this.map.getElement().style['background-color'] = spec.options.color;
                break;

            case "Google":

                this.olLayer = new OpenLayers.Layer.Google( "BaseLayer", spec.options );
                break;

            case "TMS":

                this.olLayer = new OpenLayers.Layer.TMS( "BaseLayer", spec.url, spec.options );
                break;
        }

        this.map.olMap.addLayer( this.olLayer );
        this.map.olMap.setBaseLayer( this.olLayer );

        if ( spec.options.type === 'styled' ) {
            styledMapType = new google.maps.StyledMapType( spec.options.style, {name: 'Styled Map'} );
            this.olLayer.mapObject.mapTypes.set( 'styled', styledMapType );
        }

        // ensure baselayer remains bottom layer
        this.map.olMap.setLayerIndex( this.olLayer, -1 );

        this.setOpacity( this.getOpacity() );
        this.setVisibility( this.getVisibility() );
    };

    BaseLayer.prototype.deactivate = function() {
        if ( this.olLayer ) {
            this.map.olMap.removeLayer( this.olLayer );
            this.olLayer.destroy();
        }
        this.map.getElement().style['background-color'] = '';
    };

	return BaseLayer;
});
