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

/**
 * A utility namespace containing map coordinate conversion functionality.
 */
( function() {

	"use strict";

	var Util = require('../util/Util'),
        getMapMinAndMaxInViewportPixels,
        TILESIZE = 256;

    /**
     * Private: Returns the maps min and max pixels in viewport pixels.
     *
     * @param map {Map} The map object.
     * NOTE:    viewport [0,0] is TOP-LEFT
     *          map [0,0] is BOTTOM-LEFT
     */
    getMapMinAndMaxInViewportPixels = function( map ) {
        var olMap = map.olMap;
		return {
			min: olMap.getViewPortPxFromLonLat(
					new OpenLayers.LonLat(
						olMap.maxExtent.left,
						olMap.maxExtent.bottom ) ),
			max: olMap.getViewPortPxFromLonLat(
					new OpenLayers.LonLat(
						olMap.maxExtent.right,
						olMap.maxExtent.top ) )
		};
    };

    module.exports = {

        /**
		 * Transforms a point from data coordinates to viewport pixel coordinates
         *
         * @param map {Map}    The map object.
         * @param x   {number} The x coordinate of the data.
         * @param y   {number} The y coordinate of the data.
		 * NOTE:    viewport [0,0] is TOP-LEFT
		 *          data [0,0] is BOTTOM-LEFT
		 */
		getViewportPixelFromCoord: function( map, x, y ) {
			var mapPixel = this.getMapPixelFromCoord( map, x, y );
			return this.getViewportPixelFromMapPixel( map, mapPixel.x, mapPixel.y );
		},

		/**
		 * Transforms a point from map pixel coordinates to viewport pixel coordinates
         *
         * @param map {Map}    The map object.
         * @param mx  {number} The x pixel coordinate of the map.
         * @param my  {number} The y pixel coordinate of the map.
		 * NOTE:    viewport [0,0] is TOP-LEFT
		 *          map [0,0] is BOTTOM-LEFT
		 */
		getViewportPixelFromMapPixel: function( map, mx, my ) {
			var viewportMinMax = getMapMinAndMaxInViewportPixels( map );
			return {
				x: mx + viewportMinMax.min.x,
				y: map.getWidth() - my + viewportMinMax.max.y
			};
		},

		/**
		 * Transforms a point from data coordinates to map pixel coordinates
         *
         * @param map {Map} The map object.
         * @param x   {int} The x coordinate of the data.
         * @param y   {int} The y coordinate of the data.
		 * NOTE:    data and map [0,0] are both BOTTOM-LEFT
		 */
		getMapPixelFromCoord: function( map, x, y ) {
			var tile = map.pyramid.rootToFractionalTile( x, y, map.getZoom(), TILESIZE ),
			    bin = map.pyramid.rootToFractionalBin( x, y, tile);
			return {
				x: tile.xIndex * TILESIZE + bin.x,
				y: tile.yIndex * TILESIZE + TILESIZE - 1 - bin.y
			};
		},

		/**
		 * Transforms a point from viewport pixel coordinates to map pixel coordinates
         *
         * @param map {Map} The map object.
         * @param vx  {int} The x pixel coordinate of the viewport.
         * @param vy  {int} The y pixel coordinate of the viewport.
		 * NOTE:    viewport [0,0] is TOP-LEFT
		 *          map [0,0] is BOTTOM-LEFT
		 */
		getMapPixelFromViewportPixel: function( map, vx, vy ) {
            var viewportMinMax = getMapMinAndMaxInViewportPixels( map ),
			    totalPixelSpan = map.getWidth();
            return {
				x: totalPixelSpan + vx - viewportMinMax.max.x,
				y: totalPixelSpan - vy + viewportMinMax.max.y
			};
		},

		/**
		 * Transforms a point from map pixel coordinates to data coordinates
         *
         * @param map {Map}    The map object.
         * @param mx  {number} The x pixel coordinate of the map.
         * @param my  {number} The y pixel coordinate of the map.
		 * NOTE:    data and map [0,0] are both BOTTOM-LEFT
		 */
		getCoordFromMapPixel: function( map, mx, my ) {
            var tileAndBin = this.getTileAndBinFromMapPixel( map, mx, my, TILESIZE, TILESIZE ),
			    bounds = map.pyramid.getBinBounds( tileAndBin.tile, tileAndBin.bin );
			return {
				x: bounds.minX,
				y: bounds.minY
			};
		},

		/**
		 * Transforms a point from viewport pixel coordinates to data coordinates
         *
         * @param map {Map} The map object.
         * @param vx  {int} The x pixel coordinate of the viewport.
         * @param vy  {int} The y pixel coordinate of the viewport.
		 * NOTE:    viewport [0,0] is TOP-LEFT
		 *          data [0,0] is BOTTOM-LEFT
		 */
		getCoordFromViewportPixel: function( map, vx, vy ) {
			var mapPixel = this.getMapPixelFromViewportPixel( map, vx, vy );
			return this.getCoordFromMapPixel( map, mapPixel.x, mapPixel.y );
		},

		/**
		 * Returns the tile and bin index corresponding to the given viewport pixel coordinate
         *
         * @param map {Map} The map object.
         * @param vx  {int} The x pixel coordinate of the viewport.
         * @param vy  {int} The y pixel coordinate of the viewport.
         * @param xBinCount {int} The number of bins in the x dimension of the tile.
         * @param yBinCount {int} The number of bins in the y dimension of the tile.
		 */
		getTileAndBinFromViewportPixel: function( map, vx, vy, xBinCount, yBinCount ) {
			var mapPixel = this.getMapPixelFromViewportPixel( map, vx, vy );
			return this.getTileAndBinFromMapPixel( map, mapPixel.x, mapPixel.y, xBinCount, yBinCount );
		},

		/**
         * Returns the tile and bin index corresponding to the given map pixel coordinate
         *
         * @param map {Map}    The map object.
         * @param mx  {number} The x pixel coordinate of the map.
         * @param my  {number} The y pixel coordinate of the map.
         * @param xBinCount {int} The number of bins in the x dimension of the tile.
         * @param yBinCount {int} The number of bins in the y dimension of the tile.
         */
        getTileAndBinFromMapPixel: function( map, mx, my, xBinCount, yBinCount ) {
            var tileIndexX = Math.floor( mx / TILESIZE ),
                tileIndexY = Math.floor( my / TILESIZE ),
                tilePixelX = Util.mod( mx , TILESIZE ),
                tilePixelY = Util.mod( my, TILESIZE );
            return {
                tile: {
                    level : map.getZoom(),
                    xIndex : tileIndexX,
                    yIndex : tileIndexY,
                    xBinCount : xBinCount,
                    yBinCount : yBinCount
                },
                bin: {
                    x : Math.floor( tilePixelX / (TILESIZE / xBinCount ) ),
                    y : (yBinCount - 1) - Math.floor( tilePixelY / (TILESIZE / yBinCount) ) // bin [0,0] is top left
                }
            };
        },

        /**
		 * Returns the tile and bin index corresponding to the given data coordinate
         *
         * @param map {Map} The map object.
         * @param x   {int} The x coordinate of the data.
         * @param y   {int} The y coordinate of the data.
         * @param xBinCount {int} The number of bins in the x dimension of the tile.
         * @param yBinCount {int} The number of bins in the y dimension of the tile.
		 */
		getTileAndBinFromCoord: function( map, x, y, xBinCount, yBinCount ) {
			var mapPixel = this.getMapPixelFromCoord( map, x, y );
			return this.getTileAndBinFromMapPixel( map, mapPixel.x, mapPixel.y, xBinCount, yBinCount );
		},

        /**
         * Returns the top left pixel location in viewport coordinates from a tilekey..
         *
         * @param map     {Map}    The map object.
         * @param tilekey {string} The tilekey.
         */
        getTopLeftViewportPixelForTile: function( map, tilekey ) {
            var mapPixel = this.getTopLeftMapPixelForTile( map, tilekey );
            return this.getViewportPixelFromMapPixel( map, mapPixel.x, mapPixel.y );
        },

        /**
         * Returns the top left pixel location in viewport coord from a tile index.
         *
         * @param map     {Map}    The map object.
         * @param tilekey {string} The tilekey.
         */
        getTopLeftMapPixelForTile: function( map, tilekey ) {
            var parsedValues = tilekey.split( ',' ),
                x = parseInt( parsedValues[1], 10 ),
                y = parseInt( parsedValues[2], 10 ),
                mx = x * TILESIZE,
                my = y * TILESIZE + TILESIZE;
            return {
                x : mx,
                y : my
            };
        },

        /**
         * Returns the data coordinate value corresponding to the top left pixel of the tile
         *
         * @param map     {Map}    The map object.
         * @param tilekey {string} The tilekey.
         */
        getTopLeftCoordForTile: function( map, tilekey ) {
            var mapPixel = this.getTopLeftMapPixelForTile( map, tilekey );
            return this.getCoordFromMapPixel( map, mapPixel.x, mapPixel.y );
        }

	};
}());
