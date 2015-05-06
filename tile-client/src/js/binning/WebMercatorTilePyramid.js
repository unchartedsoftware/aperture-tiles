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

	var EPSG_900913_SCALE_FACTOR = 20037508.342789244,
	    EPSG_900913_LATITUDE = 85.05112878,
	    DEGREES_TO_RADIANS = Math.PI / 180.0,	// Factor for changing degrees to radians
	    RADIANS_TO_DEGREES = 180.0 / Math.PI;	// Factor for changing radians to degrees

	function rootToTileMercator( lon, lat, level ) {
		var latR = lat * DEGREES_TO_RADIANS,
		    pow2 = 1 << level,
		    x = (lon + 180.0) / 360.0 * pow2,
		    y = (pow2 * (1 - Math.log(Math.tan(latR) + 1 / Math.cos(latR)) / Math.PI) / 2);
		return {
            x: x,
            y: pow2 - y
        };
	}

    function sinh( arg ) {
        return (Math.exp(arg) - Math.exp(-arg)) / 2.0;
    }

	function tileToLon( x, level ) {
		var pow2 = 1 << level;
		return x / pow2 * 360.0 - 180.0;
	}

	function tileToLat( y, level ) {
		var pow2 = 1 << level,
		    n    = -Math.PI + (2.0 * Math.PI * y) / pow2;
		return Math.atan(sinh(n)) * RADIANS_TO_DEGREES;
	}

	function linearToGudermannian( value ) {
		function gudermannian( y ) {
			// converts a y value from -PI(bottom) to PI(top) into the
			// mercator projection latitude
			return Math.atan(sinh(y)) * RADIANS_TO_DEGREES;
		}
		return gudermannian( (value / EPSG_900913_LATITUDE) * Math.PI );
	}

	function gudermannianToLinear(value) {
		function gudermannianInv( latitude ) {
			// converts a latitude value from -EPSG_900913_LATITUDE to EPSG_900913_LATITUDE into
			// a y value from -PI(bottom) to PI(top)
			var sign = ( latitude !== 0 ) ? latitude / Math.abs(latitude) : 0,
			    sin = Math.sin(latitude * DEGREES_TO_RADIANS * sign);
			return sign * (Math.log((1.0 + sin) / (1.0 - sin)) / 2.0);
		}
		return (gudermannianInv( value ) / Math.PI) * EPSG_900913_LATITUDE;
	}

    /**
     * Instantiate a WebMercatorTilePyramid object.
     * @class WebMercatorTilePyramid
     * @classdesc A TilePyramid implementation, the equivalent of WebMercatorTilePyramid
     *            in tile-service/binning-utilities.
     */
	function WebMercatorTilePyramid() {
        this.minX = -180.0;
        this.minY = -85.05;
        this.maxX = 180.0;
        this.maxY = 85.05;
        return this;
    }

    /**
     * Returns the projection code associated with the pyramid.
     * @memberof WebMercatorTilePyramid
     *
     * @returns {string} The projection code.
     */
    WebMercatorTilePyramid.prototype.getProjection = function(){
        return "EPSG:900913";
    };

    /**
     * Returns the tile scheme associated with the pyramid.
     * @memberof WebMercatorTilePyramid
     *
     * @returns {string} The scheme code.
     */
    WebMercatorTilePyramid.prototype.getTileScheme = function() {
        return "TMS";
    };

    /**
     * Maps a fractional tile coordinate to a point in the root coordinate system.
     * @memberof WebMercatorTilePyramid
     *
     * @param {Object} tile - The fractional tile coordinate.
     *
     * @returns {Object} The root coordinate.
     */
    WebMercatorTilePyramid.prototype.fractionalTileToRoot = function( tile ) {
        return {
            lon: tileToLon( tile.xIndex, tile.level ),
            lat: tileToLat( tile.yIndex, tile.level )
        };
    };

    /**
     * Maps a point from the root coordinate system to a fractional tile coordinate.
     * @memberof WebMercatorTilePyramid
     *
     * @param {number} lon - The longitude coordinate value.
     * @param {number} lat - The latitude coordinate value.
     * @param {integer} level - The zoom level.
     * @param {integer} bins - The number of bins per dimension in a tile.
     *
     * @returns {Object} The fractional tile coordinate.
     */
    WebMercatorTilePyramid.prototype.rootToFractionalTile = function( lon, lat, level, bins ) {
		bins = bins || 256;
        var tileMercator = rootToTileMercator( lon, lat, level );
        return {
            level: level,
            xIndex: tileMercator.x,
            yIndex: tileMercator.y,
            xBinCount: bins,
            yBinCount: bins
        };
    };

    /**
     * Maps a point from the root coordinate system to a tile coordinate.
     * @memberof WebMercatorTilePyramid
     *
     * @param {number} lon - The longitude coordinate value.
     * @param {number} lat - The latitude coordinate value.
     * @param {integer} level - The zoom level.
     * @param {integer} bins - The number of bins per dimension in a tile.
     *
     * @returns {Object} The tile coordinate.
     */
    WebMercatorTilePyramid.prototype.rootToTile = function( lon, lat, level, bins ) {
		var result = this.rootToFractionalTile( lon, lat, level, bins );
		result.xIndex = Math.floor( result.xIndex );
		result.yIndex = Math.floor( result.yIndex );
		return result;
    };

	/**
     * Maps a point from the root coordinate system to a specific fractional
	 * bin coordinate.
     * @memberof WebMercatorTilePyramid
     *
     * @param {number} lon - The longitude coordinate value.
     * @param {number} lat - The latitude coordinate value.
     * @param {Object} tile - The tile coordinate that holds the target bin.
     *
     * @returns {Object} The bin coordinate.
     */
    WebMercatorTilePyramid.prototype.rootToFractionalBin = function( lon, lat, tile ) {
        var tileMercator = rootToTileMercator( lon, lat, tile.level );
        return {
            x: (tileMercator.x - tile.xIndex) * tile.xBinCount,
            y: tile.yBinCount - 1 - (tileMercator.y - tile.yIndex ) * tile.yBinCount
        };
    };

    /**
     * Maps a point from the root coordinate system to a specific bin coordinate.
     * @memberof WebMercatorTilePyramid
     *
     * @param {number} lon - The longitude coordinate value.
     * @param {number} lat - The latitude coordinate value.
     * @param {Object} tile - The tile coordinate that holds the target bin.
     *
     * @returns {Object} The bin coordinate.
     */
    WebMercatorTilePyramid.prototype.rootToBin = function( lon, lat, tile ) {
		var result = this.rootToFractionalBin( lon, lat, tile );
		result.x = Math.floor( result.x );
		result.y = Math.floor( result.y );
        return result;
    };

    /**
     * Returns the bounds of a particular tile in EPSG 900913 meter units.
     * @memberof WebMercatorTilePyramid
     *
     * @param {Object} tile - The tile coordinate.
     * @param {Object} bin - The bin coordinate.
     *
     * @returns {Object} The bounds object.
     */
    WebMercatorTilePyramid.prototype.getEPSG900913Bounds = function( tile, bin ) {
        var pow2 = 1 << tile.level,
            tileIncrement = 1.0/pow2,
            minX = tile.xIndex * tileIncrement - 0.5,
            minY = tile.yIndex * tileIncrement - 0.5,
            maxX,
            maxY,
            linMinY,
            linMaxY,
            binXInc,
            binYInc,
            centerY;

        if ( bin ) {
            maxX = minX + tileIncrement;
            maxY = minY + tileIncrement;
        } else {
            binXInc = tileIncrement / tile.xBinCount;
            binYInc = tileIncrement / tile.yBinCount;
            minX = minX + bin.x * binXInc;
            minY = minY + (tile.yBinCount - bin.y - 1) * binYInc;
            maxX = minX + binXInc;
            maxY = minY + binYInc;
        }

        // as mercator latitude cannot be linearly interpolated, convert the gudermannian
        // coordinates back into their equivalent linear counterparts. Interpolate these,
        // then convert to the equivalent gudermannian coordinate.
        linMaxY = gudermannianToLinear( maxY );
        linMinY = gudermannianToLinear( minY );
        centerY = linearToGudermannian( (linMaxY+linMinY)/2 );

        return {
            minX:    minX * 2.0 * EPSG_900913_SCALE_FACTOR,
            minY:    minY * 2.0 * EPSG_900913_SCALE_FACTOR,
            maxX:    maxX * 2.0 * EPSG_900913_SCALE_FACTOR,
            maxY:    maxY * 2.0 * EPSG_900913_SCALE_FACTOR,
            centerX: (minX + maxX) * EPSG_900913_SCALE_FACTOR, // (minX+maxX)/2.0*2.0 optimized to (minX+maxX)
            centerY: centerY * 2.0 * EPSG_900913_SCALE_FACTOR,
            width:   (maxX - minX) * 2.0 * EPSG_900913_SCALE_FACTOR,
            height:  (maxY - minY) * 2.0 * EPSG_900913_SCALE_FACTOR
        };
    };

    /**
     * Returns the bounds of a particular tile in the root coordinate system.
     * @memberof WebMercatorTilePyramid
     *
     * @param {Object} tile - The tile coordinate.
     *
     * @returns {Object} The bounds object.
     */
    WebMercatorTilePyramid.prototype.getTileBounds = function( tile ) {
        var level = tile.level,
            north = tileToLat( tile.yIndex+1, level ),
            south = tileToLat( tile.yIndex, level ),
            east = tileToLon( tile.xIndex+1, level ),
            west = tileToLon( tile.xIndex, level ),
            // as mercator latitude cannot be linearly interpolated, convert the gudermannian
            // coordinates back into their equivalent linear counterparts. Interpolate these,
            // then convert to the equivalent gudermannian coordinate.
            linNorth = gudermannianToLinear( north ),
            linSouth = gudermannianToLinear( south ),
            centerY = linearToGudermannian( (linNorth+linSouth)/2.0 );
        return {
            minX: west,
            minY: south,
            maxX: east,
            maxY: north,
            centerX: (east+west)/2.0,
            centerY: centerY,
            width: (east-west),
            height: (north-south)
        };
    };

    /**
     * Returns the bounds of a particular bin in the root coordinate system.
     * @memberof WebMercatorTilePyramid
     *
     * @param {Object} tile - The tile coordinate.
     * @param {Object} bin - The bin coordinate.
     *
     * @returns {Object} The bounds object.
     */
    WebMercatorTilePyramid.prototype.getBinBounds = function( tile, bin ) {
        var level = tile.level,
            binXInc = 1.0 / tile.xBinCount,
            baseX = tile.xIndex + bin.x * binXInc,
            binYInc = 1.0 / tile.yBinCount,
            baseY = tile.yIndex + (tile.yBinCount - 1 - bin.y) * binYInc,
            north = tileToLat(baseY + binYInc, level),
            south = tileToLat(baseY, level),
            east = tileToLon(baseX + binXInc, level),
            west = tileToLon(baseX, level),
            // as mercator latitude cannot be linearly interpolated, convert the gudermannian
            // coordinates back into their equivalent linear counterparts. Interpolate these,
            // then convert to the equivalent gudermannian coordinate.
            linNorth = gudermannianToLinear(north),
            linSouth = gudermannianToLinear(south),
            centerY = linearToGudermannian( (linNorth+linSouth)/2.0 );
        return {
            minX: west,
            minY: south,
            maxX: east,
            maxY: north,
            centerX: (east+west)/2.0,
            centerY: centerY,
            width: (east-west),
            height: (north-south)
        };
    };

    /**
     * Returns the JSON representation of this tile pyramid as a string.
     * @memberof WebMercatorTilePyramid
     *
     * @returns {String} The bounds object.
     */
    WebMercatorTilePyramid.prototype.toJSON = function () {
        return '{'+
            '"type": "WebMercator"'+
        '}';
    };

	module.exports = WebMercatorTilePyramid;
}());
