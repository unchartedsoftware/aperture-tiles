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
        HtmlTileLayer = require('./HtmlTileLayer'),
        PubSub = require('../util/PubSub');

    /**
     * Private: Returns the zoom callback function to update level min and maxes.
     *
     * @param layer {ServerLayer} The layer object.
     */
    function zoomCallback( layer ) {
        return function() {
            if ( layer.olLayer ) {
                layer.setLevelMinMax();
            }
        };
    }

    /**
     * Instantiate a ClientLayer object.
     * @class ClientLayer
     * @augments Layer
     * @classdesc A client rendered layer object. Uses JSON data retrieved from the
     *            server in conjunction with a Renderer object or html function to
     *            create interactable DOM elements.
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
    function ClientLayer( spec ) {
        var that = this,
            getURL = spec.getURL || LayerUtil.getURL;
        // call base constructor
        Layer.call( this, spec );
        // set reasonable defaults
        this.zIndex = ( spec.zIndex !== undefined ) ? parseInt( spec.zIndex, 10 ) : 1000;
        this.tileTransform = spec.tileTransform || {};
        this.domain = "client";
        this.source = spec.source;
        this.getURL = function( bounds ) {
            return getURL.call( this, bounds ) + that.getQueryParamString();
        };
        if ( spec.tileClass ) {
            this.tileClass = spec.tileClass;
        }
        if ( spec.renderer ) {
            this.setRenderer( spec.renderer );
        }
        if ( spec.aggregators ) {
            _.forIn( spec.aggregators, function( agg, key ) {
                this.addAggregator( key, agg );
            });
        }
    }

    ClientLayer.prototype = Object.create( Layer.prototype );

    /**
     * Activates the layer object. This should never be called manually.
     * @memberof ClientLayer
     * @private
     */
    ClientLayer.prototype.activate = function() {
        // set callback here so it can be removed later
        this.zoomCallback = zoomCallback( this );
        // set callback to update ramp min/max on zoom
        this.map.on( "zoomend", this.zoomCallback );
        // add the new layer
        this.olLayer = new HtmlTileLayer(
            'Client Rendered Tile Layer',
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
        // needless tile reqeests
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
        this.setLevelMinMax(); // set level min / max
        // publish add event
        PubSub.publish( this.getChannel(), { field: 'add', value: true } );
    };

    /**
     * Dectivates the layer object. This should never be called manually.
     * @memberof ClientLayer
     * @private
     */
    ClientLayer.prototype.deactivate = function() {
        if ( this.olLayer ) {
            this.map.olMap.removeLayer( this.olLayer );
            PubSub.publish( this.getChannel(), { field: 'remove', value: true } );
            this.olLayer.destroy();
            this.olLayer = null;
        }
        this.map.off( "zoomend", this.zoomCallback );
        this.zoomCallback = null;
        PubSub.publish( this.getChannel(), { field: 'deactivate', value: true } );
    };

    /**
     * Sets the current renderer of the layer.
     * @memberof ClientLayer
     *
     * @param {Renderer} renderer - The renderer to attach to the layer.
     */
    ClientLayer.prototype.setRenderer = function( renderer ) {
        this.renderer = renderer;
        this.renderer.attach( this );
    };

    /**
     * Adds an aggregator to the layer.
     * @memberof ClientLayer
     *
     * @param {String} id - The aggregator id.
     * @param {Renderer} renderer - The renderer to attach to the layer.
     */
    ClientLayer.prototype.addAggregator = function( id, aggregator ) {
        this.aggregators = this.aggregators || {};
        this.aggregators[ id ] = aggregator;
        this.aggregators[ id ].attach( this );
    };

    /**
     * Gets an aggregator by id.
     * @memberof ClientLayer
     *
     * @param {String} id - The aggregator id.
     */
    ClientLayer.prototype.getAggregator = function( id ) {
        return this.aggregators[ id ];
    };

    /**
     * Updates the theme associated with the layer.
     * @memberof ClientLayer
     *
     * @param {String} theme - The theme identifier string.
     */
    ClientLayer.prototype.setTheme = function( theme ) {
        this.theme = theme;
    };

    /**
     * Get the current theme for the layer.
     * @memberof ClientLayer
     *
     * @returns {String} The theme identifier string.
     */
    ClientLayer.prototype.getTheme = function() {
        return this.theme;
    };

    /**
     * Set the z index of the layer.
     * @memberof ClientLayer
     *
     * @param {integer} zIndex - The new z-order value of the layer, where 0 is front.
     */
    ClientLayer.prototype.setZIndex = function ( zIndex ) {
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
     * @memberof ClientLayer
     *
     * @returns {integer} The zIndex for the layer.
     */
    ClientLayer.prototype.getZIndex = function () {
        return this.zIndex;
    };

    /**
     * Set the layers tile transform function type.
     * @memberof ClientLayer
     *
     * @param {String} transformType - The tile transformer type.
     */
    ClientLayer.prototype.setTileTransformType = function ( transformType ) {
        if ( this.tileTransform.type !== transformType ) {
            this.tileTransform.type = transformType;
            this.redraw();
            PubSub.publish( this.getChannel(), {field: 'tileTransformType', value: transformType} );
        }
    };

    /**
     * Get the layers transformer type.
     * @memberof ClientLayer
     *
     * @return {String} The tile transform type.
     */
    ClientLayer.prototype.getTileTransformType = function () {
        return this.tileTransform.type;
    };

    /**
     * Set the tile transform data based on the time range passed in
     * @memberof ClientLayer
     *
     * @param {number} start - A unix timestamp representing the start of the time range
     * @param {number} end - A unix timestamp representing the end of the time range
     */
    ClientLayer.prototype.setTileTransformRange = function ( start, end ) {
		var meta = this.source.meta.meta,
			rangeMin = meta.rangeMin,
			rangeMax = meta.rangeMax,
			numBuckets = meta.bucketCount,
            bucketSize = ( rangeMax - rangeMin ) / numBuckets;
        if ( start > rangeMax && end < rangeMin ) {
            // outside range completely, send empty request
			this.setTileTransformData({
    			startBucket: -1,
    			endBucket: -1
            });
		}
		this.setTileTransformData({
			startBucket: Math.max( 0, Math.floor( ( start - rangeMin ) / bucketSize ) ),
			endBucket: Math.min( numBuckets - 1, Math.floor( ( end - rangeMin ) / bucketSize ) )
        });
	};

    /**
     * Set the tile transform data attribute internally
     * @memberof ClientLayer
     *
     * @param {Object} transformData - The tile transform data attribute.
     */
	ClientLayer.prototype.setTileTransformData = function( transformData ) {
        if ( !_.isEqual( this.tileTransform.data, transformData ) ) {
            this.tileTransform.data = transformData;
            this.redraw();
            PubSub.publish( this.getChannel(), {field: 'tileTransformData', value: transformData} );
        }
    };

    /**
     * Get the transformer data attribute.
     * @memberof ClientLayer
     *
     * @returns {Object} The tile transform data attribute.
     */
    ClientLayer.prototype.getTileTransformData = function () {
        return this.tileTransform.data || {};
    };

    /**
     * Get the current minimum and maximum values for the current zoom level.
     * @memberof ClientLayer
     *
     * @param {Object} The min and max of the level.
     */
    ClientLayer.prototype.getLevelMinMax = function() {
        return this.levelMinMax;
    };

    /**
     * Generate query parameters based on state of layer
     * @memberof ClientLayer
     *
     * @returns {String} The query parameter string based on the attributes of this layer.
     */
     ClientLayer.prototype.getQueryParamString = function() {
        var query = {
            tileTransform: this.tileTransform
        };
        return Util.encodeQueryParams( query );
    };

    /**
     * Redraws the entire layer.
     * @memberof ClientLayer
     */
    ClientLayer.prototype.redraw = function () {
        if ( this.olLayer ) {
            this.setLevelMinMax();
            this.olLayer.redraw();
            this.setZIndex(this.zIndex);
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

    /**
     * Sets the layers min and max values for the given zoom level.
     * @memberof ClientLayer
     * @private
     * @param layer {Object} the layer object.
     */
    ClientLayer.prototype.setLevelMinMax = function() {
        var zoomLevel = this.map.getZoom(),
            source = this.source,
            meta = source.meta && source.meta.meta ? source.meta.meta[ zoomLevel ] : null,
            transformData = this.tileTransform.data || {},
            levelMinMax = meta,
            renderer = this.renderer,
            aggregated;
        if ( meta ) {
            // aggregate the data if there is an aggregator attached
            if ( renderer && renderer.aggregator ) {
                // aggregate the meta data buckets
                aggregated = renderer.aggregator.aggregate(
                    meta.bins || [],
                    transformData.startBucket,
                    transformData.endBucket );
                if ( aggregated instanceof Array ) {
                    // take the first and last index, which correspond to max / min
                    levelMinMax = {
                        minimum: aggregated[aggregated.length - 1] || null,
                        maximum: aggregated[0] || null
                    };
                } else {
                    //
                    levelMinMax = {
                        minimum: aggregated,
                        maximum: aggregated
                    };
                }
            }
        } else {
            levelMinMax = {
                minimum: null,
                maximum: null
            };
        }
        this.levelMinMax = levelMinMax;
        PubSub.publish( this.getChannel(), { field: 'levelMinMax', value: levelMinMax });
    };

    module.exports = ClientLayer;
}());
