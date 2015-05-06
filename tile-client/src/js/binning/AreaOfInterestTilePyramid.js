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

    /**
     * Instantiate an AreaOfInterestTilePyramid object.
     * @class AreaOfInterestTilePyramid
     * @classdesc A TilePyramid implementation, the equivalent of AOITilePyramid in
     *            tile-service/binning-utilities.
     *
     * @param {Object} spec - The specification object.
     */
	function AreaOfInterestTilePyramid( spec ) {
        this.minX = spec.minX;
        this.minY = spec.minY;
        this.maxX = spec.maxX;
        this.maxY = spec.maxY;
    }

    /**
     * Returns the projection code associated with the pyramid.
     * @memberof AreaOfInterestTilePyramid
     *
     * @returns {string} The projection code.
     */
    AreaOfInterestTilePyramid.prototype.getProjection = function() {
        return "EPSG:4326";
    };

    /**
     * Returns the tile scheme associated with the pyramid.
     * @memberof AreaOfInterestTilePyramid
     *
     * @returns {string} The scheme code.
     */
	AreaOfInterestTilePyramid.prototype.getTileScheme = function() {
        return "TMS";
    };

    /**
     * Maps a fractional tile coordinate to a point in the root coordinate system.
     * @memberof AreaOfInterestTilePyramid
     *
     * @param {Object} tile - The fractional tile coordinate.
     *
     * @returns {Object} The root coordinate.
     */
    AreaOfInterestTilePyramid.prototype.fractionalTileToRoot = function( tile ) {
        var pow2, tileXSize, tileYSize;
        pow2 = 1 << tile.level;
        tileXSize = (this.maxX - this.minX) / pow2;
        tileYSize = (this.maxY - this.minY) / pow2;
        return {
            x: this.minX + tileXSize * tile.xIndex,
            y: this.minY + tileYSize * tile.yIndex
        };
    };

    /**
     * Maps a point from the root coordinate system to a fractional tile coordinate.
     * @memberof AreaOfInterestTilePyramid
     *
     * @param {number} x - The x root coordinate value.
     * @param {number} y - The y root coordinate value.
     * @param {integer} level - The zoom level.
     * @param {integer} bins - The number of bins per dimension in a tile.
     *
     * @returns {Object} The fractional tile coordinate.
     */
	AreaOfInterestTilePyramid.prototype.rootToFractionalTile = function( x, y, level, bins ) {
		bins = bins || 256;
        var numDivs, tileX, tileY;
        numDivs = 1 << level;
        tileX = numDivs * (x - this.minX) / (this.maxX - this.minX);
        tileY = numDivs * (y - this.minY) / (this.maxY - this.minY);
        return {
            'level': level,
            'xIndex': tileX,
            'yIndex': tileY,
            xBinCount: bins,
            yBinCount: bins
        };
    };

    /**
     * Maps a point from the root coordinate system to a tile coordinate.
     * @memberof AreaOfInterestTilePyramid
     *
     * @param {number} x - The x root coordinate value.
     * @param {number} y - The y root coordinate value.
     * @param {integer} level - The zoom level.
     * @param {integer} bins - The number of bins per dimension in a tile.
     *
     * @returns {Object} The tile coordinate.
     */
    AreaOfInterestTilePyramid.prototype.rootToTile = function( x, y, level, bins ) {
		var result = this.rootToFractionalTile( x, y, level, bins );
		result.xIndex = Math.floor( result.xIndex );
		result.yIndex = Math.floor( result.yIndex );
		return result;
    };

	/**
     * Maps a point from the root coordinate system to a specific bin fractional
	 * coordinate.
     * @memberof AreaOfInterestTilePyramid
     *
     * @param {number} x - The x root coordinate value.
     * @param {number} y - The y root coordinate value.
     * @param {Object} tile - The tile coordinate that holds the target bin.
     *
     * @returns {Object} The bin coordinate.
     */
    AreaOfInterestTilePyramid.prototype.rootToFractionalBin = function( x, y, tile ) {
        var pow2, tileXSize, tileYSize, xInTile, yInTile, binX, binY;
        pow2 = 1 << tile.level;
        tileXSize = (this.maxX - this.minX) / pow2;
        tileYSize = (this.maxY - this.minY) / pow2;
        xInTile = x - this.minX - tile.xIndex * tileXSize;
        yInTile = y - this.minY - tile.yIndex * tileYSize;
        binX = xInTile * tile.xBinCount / tileXSize;
        binY = yInTile * tile.yBinCount / tileYSize;
        return {
            x: binX,
            y: tile.yBinCount - 1 - binY
        };
    };

	/**
     * Maps a point from the root coordinate system to a specific bin coordinate.
     * @memberof AreaOfInterestTilePyramid
     *
     * @param {number} x - The x root coordinate value.
     * @param {number} y - The y root coordinate value.
     * @param {Object} tile - The tile coordinate that holds the target bin.
     *
     * @returns {Object} The bin coordinate.
     */
    AreaOfInterestTilePyramid.prototype.rootToBin = function( x, y, tile ) {
		var result = this.rootToFractionalBin( x, y, tile );
		result.x = Math.floor( result.x );
		result.y = Math.floor( result.y );
        return result;
    };

    /**
     * Maps a point from the root coordinate system to a specific bin coordinate.
     * @memberof AreaOfInterestTilePyramid
     *
     * @param {number} x - The x root coordinate value.
     * @param {number} y - The y root coordinate value.
     * @param {Object} tile - The tile coordinate that holds the target bin.
     *
     * @returns {Object} The bin coordinate.
     */
    AreaOfInterestTilePyramid.prototype.rootToBin = function( x, y, tile ) {
        var pow2, tileXSize, tileYSize, xInTile, yInTile, binX, binY;
        pow2 = 1 << tile.level;
        tileXSize = (this.maxX - this.minX) / pow2;
        tileYSize = (this.maxY - this.minY) / pow2;
        xInTile = x - this.minX - tile.xIndex * tileXSize;
        yInTile = y - this.minY - tile.yIndex * tileYSize;
        binX = Math.floor(xInTile * tile.xBinCount / tileXSize);
        binY = Math.floor(yInTile * tile.yBinCount / tileYSize);
        return {
            x: binX,
            y: tile.yBinCount - 1 - binY
        };
    };

    /**
     * Returns the bounds of a particular tile in the root coordinate system.
     * @memberof AreaOfInterestTilePyramid
     *
     * @param {Object} tile - The tile coordinate.
     *
     * @returns {Object} The bounds object.
     */
    AreaOfInterestTilePyramid.prototype.getTileBounds = function( tile ) {
        var pow2, tileXSize, tileYSize;
        pow2 = 1 << tile.level;
        tileXSize = (this.maxX - this.minX) / pow2;
        tileYSize = (this.maxY - this.minY) / pow2;
        return {
            minX: this.minX + tileXSize * tile.xIndex,
            minY: this.minY + tileYSize * tile.yIndex,
            maxX: this.minX + tileXSize * (tile.xIndex + 1),
            maxY: this.minY + tileYSize * (tile.yIndex + 1),
            centerX: this.minX + tileXSize * (tile.xIndex + 0.5),
            centerY: this.minY + tileYSize * (tile.yIndex + 0.5),
            width: tileXSize,
            height: tileYSize
        };
    };

    /**
     * Returns the bounds of a particular bin in the root coordinate system.
     * @memberof AreaOfInterestTilePyramid
     *
     * @param {Object} tile - The tile coordinate.
     * @param {Object} bin - The bin coordinate.
     *
     * @returns {Object} The bounds object.
     */
    AreaOfInterestTilePyramid.prototype.getBinBounds = function( tile, bin ) {
        var pow2, tileXSize, tileYSize, binXSize, binYSize, adjustedY,
            left, bottom;
        pow2 = 1 << tile.level;
        tileXSize = (this.maxX - this.minX) / pow2;
        tileYSize = (this.maxY - this.minY) / pow2;
        binXSize = tileXSize / tile.xBinCount;
        binYSize = tileYSize / tile.yBinCount;
        adjustedY = tile.yBinCount - 1 - bin.y;
        left = this.minX + tileXSize * tile.xIndex;
        bottom = this.minY + tileYSize * tile.yIndex;
        return {
            minX: left + binXSize * bin.x,
            minY: bottom + binYSize * adjustedY,
            maxX: left + binXSize * (bin.x + 1),
            maxY: bottom + binYSize * (adjustedY + 1),
            centerX: left + binXSize * (bin.x + 0.5),
            centerY: bottom + binYSize * (adjustedY + 0.5),
            width: binXSize,
            height: binYSize
        };
    };

    /**
     * Returns the JSON representation of this tile pyramid as a string.
     * @memberof AreaOfInterestTilePyramid
     *
     * @returns {String} The bounds object.
     */
    AreaOfInterestTilePyramid.prototype.toJSON = function() {
        return '{'+
            '"type": "AreaOfInterest",' +
            '"minX": this.minX,' +
            '"maxX": this.maxX,' +
            '"minY": this.minY,' +
            '"maxY": this.maxY' +
        '}';
    };

	module.exports = AreaOfInterestTilePyramid;

}());
