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
define(function (require) {
	"use strict";

	var Layer = require('../Layer'),
        PubSub = require('../../util/PubSub');

	function BaseLayer( spec ) {
        // set defaults
        spec = spec || {};
        spec.type = spec.type || "BlankBase";
        spec.theme = spec.theme || "dark";
        spec.options = spec.options || {
            name : "black",
            color : "rgb(0,0,0)"
        };
        spec.tileBorder = spec.tileBorder || {
            color : "rgba(255, 255, 255, .5)",
            weight : "1px",
            style : "solid"
        };
        spec.domain = "base";
        // call base constructor
        Layer.call( this, spec );
        // set basic map properties
        this.visibility = ( spec.enabled !== undefined ) ? spec.enabled : true ;
        this.opacity = spec.opacity || 1.0;
    }

    BaseLayer.prototype = Object.create( Layer.prototype );

    BaseLayer.prototype.activate = function() {

        var $map = this.map.getElement(),
            map = this.map.map,
            styledMapType;

        if( this.spec.type === 'BlankBase' ) {

            // changing to blank base layer
            $map.css( 'background-color', this.spec.options.color );

        } else {

            //reset the background color
            $map.css( 'background-color', '' );
            // create new layer instance
            this.spec.isBaseLayer = false;
            if ( this.spec.type === 'Google' ) {
                this.layer = new OpenLayers.Layer.Google( "BaseLayer", this.spec.options );
            } else {
                this.layer = new OpenLayers.Layer.TMS( "BaseLayer", this.spec.url, this.spec.options );
            }
            this.map.addLayer( this.layer );

            if ( this.spec.options.type === 'styled' ) {
				styledMapType = new google.maps.StyledMapType( this.spec.options.style, {name: 'Styled Map'} );
				this.layer.mapObject.mapTypes.set( 'styled', styledMapType );
				this.layer.mapObject.setMapTypeId( 'styled');
			}

            // attach, and refresh it by toggling visibility
            //map.baseLayer = this.layer;
            map.setBaseLayer( this.layer );
            // ensure baselayer remains bottom layer
            this.map.setLayerIndex( this.layer, -1 );
            // toggle visibility to force redraw
            this.layer.setVisibility(false);
            this.layer.setVisibility(true);
        }

        if ( this.spec.theme && this.spec.theme.toLowerCase() === "light" ) {
            $("body").removeClass('dark-theme').addClass('light-theme');
        } else {
            $("body").removeClass('light-theme').addClass('dark-theme');
        }

        // update tile border
        this.setTileBorderStyle();

        if ( this.spec.type !== "BlankBase" ) {
            // if switching to a non-blank baselayer, ensure opacity and visibility is restored
            this.setOpacity( this.getOpacity() );
            this.setVisibility( this.getVisibility() );
        }
    };

    BaseLayer.prototype.deactivate = function() {
        var $map = this.map.getElement();
        if( this.spec.type !== 'BlankBase' ) {
            // destroy previous baselayer
            this.layer.destroy();
            //reset the background color
            $map.css( 'background-color', '' );
        }
    };

    BaseLayer.prototype.setTileBorderStyle = function () {
        var tileBorder = this.spec.tileBorder;
        // remove any previous style
        $( document.body ).find( "#tiles-border-style" ).remove();
        if ( tileBorder === 'default' ) {
            tileBorder = {
                "color" : "rgba(255, 255, 255, .5)",
                "style" : "solid",
                "weight" : "1px"
            };
        }
        //set individual defaults if they are omitted.
        tileBorder.color = tileBorder.color || "rgba(255, 255, 255, .5)";
        tileBorder.style = tileBorder.style || "solid";
        tileBorder.weight = tileBorder.weight || "1px";
        $( document.body ).prepend(
            $('<style id="tiles-border-style" type="text/css">' + ('#' + this.map.id) + ' .olTileImage {' +
                'border-left : ' + tileBorder.weight + ' ' + tileBorder.style + ' ' + tileBorder.color +
                '; border-top : ' + tileBorder.weight + ' ' + tileBorder.style + ' ' + tileBorder.color +';}' +
              '</style>')
        );
    };

    BaseLayer.prototype.setOpacity = function( opacity ) {
        this.opacity = opacity;
        this.layer.setOpacity ( opacity );
        PubSub.publish( this.getChannel(), { field: 'opacity', value: opacity });
    };

    BaseLayer.prototype.getOpacity = function() {
        return this.opacity;
    };

    BaseLayer.prototype.setVisibility = function( visibility ) {
        this.visibility = visibility;
        this.layer.setVisibility( visibility );
        PubSub.publish( this.getChannel(), { field: 'enabled', value: visibility });
    };

    BaseLayer.prototype.getVisibility = function() {
        return this.visibility;
    };

	return BaseLayer;
});
