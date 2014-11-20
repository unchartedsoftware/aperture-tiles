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
 * This module defines a TilePyramid class, the equivalent of AOITilePyramid in
 * binning-utilities.
 */
define( function () {
	"use strict";

	function AreaOfInterestTilePyramid( spec ) {
        this.minX = spec.minX;
        this.minY = spec.minY;
        this.maxX = spec.maxX;
        this.maxY = spec.maxY;
    }

    AreaOfInterestTilePyramid.prototype.getProjection = function() {
        return "EPSG:4326";
    };

	AreaOfInterestTilePyramid.prototype.getTileScheme = function() {
        return "TMS";
    };

	AreaOfInterestTilePyramid.prototype.rootToFractionalTile = function( root ) {
        var numDivs, tileX, tileY;
        numDivs = 1 << root.level;
        tileX = numDivs * (root.xIndex - this.minX) / (this.maxX - this.minX);
        tileY = numDivs * (root.yIndex - this.minY) / (this.maxY - this.minY);
        return {
            'level': root.level,
            'xIndex': tileX,
            'yIndex': tileY
        };
    };

    AreaOfInterestTilePyramid.prototype.fractionalTileToRoot = function( tile ) {
        var pow2, tileXSize, tileYSize;
        pow2 = 1 << tile.level;
        tileXSize = (this.maxX - this.minX) / pow2;
        tileYSize = (this.maxY - this.minY) / pow2;
        return {
            level: tile.level,
            xIndex: this.minX + tileXSize * tile.xIndex,
            yIndex: this.minY + tileYSize * tile.yIndex
        };
    };

    AreaOfInterestTilePyramid.prototype.rootToTile = function (x, y, level, bins) {
        var numDivs, tileX, tileY;
        numDivs = 1 << level;
        tileX = Math.floor(numDivs * (x - this.minX) / (this.maxX - this.minX));
        tileY = Math.floor(numDivs * (y - this.minY) / (this.maxY - this.minY));
        if (!bins) {
            bins = 256;
        }
        return {level:     level,
                xIndex:    tileX,
                yIndex:    tileY,
                xBinCount: bins,
                yBinCount: bins};
    };

    AreaOfInterestTilePyramid.prototype.rootToBin = function (x, y, tile) {
        var pow2, tileXSize, tileYSize, xInTile, yInTile, binX, binY;
        pow2 = 1 << tile.level;
        tileXSize = (this.maxX - this.minX) / pow2;
        tileYSize = (this.maxY - this.minY) / pow2;
        xInTile = x - this.minX - tile.xIndex * tileXSize;
        yInTile = y - this.minY - tile.yIndex * tileYSize;
        binX = Math.floor(xInTile * tile.xBinCount / tileXSize);
        binY = Math.floor(yInTile * tile.yBinCount / tileYSize);
        return {x: binX,
                y: tile.yBinCount - 1 - binY};
    };

    AreaOfInterestTilePyramid.prototype.getTileBounds = function (tile) {
        var pow2, tileXSize, tileYSize;
        pow2 = 1 << tile.level;
        tileXSize = (this.maxX - this.minX) / pow2;
        tileYSize = (this.maxY - this.minY) / pow2;
        return {minX: this.minX + tileXSize * tile.xIndex,
                minY: this.minY + tileYSize * tile.yIndex,
                maxX: this.minX + tileXSize * (tile.xIndex + 1),
                maxY: this.minY + tileYSize * (tile.yIndex + 1),
                centerX: this.minX + tileXSize * (tile.xIndex + 0.5),
                centerY: this.minY + tileYSize * (tile.yIndex + 0.5),
                width: tileXSize,
                height: tileYSize};
    };

    AreaOfInterestTilePyramid.prototype.getBinBounds = function (tile, bin) {
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
        return {minX: left + binXSize * bin.x,
                minY: bottom + binYSize * adjustedY,
                maxX: left + binXSize * (bin.x + 1),
                maxY: bottom + binYSize * (adjustedY + 1),
                centerX: left + binXSize * (bin.x + 0.5),
                centerY: bottom + binYSize * (adjustedY + 0.5),
                width: binXSize,
                height: binYSize};
    };

    AreaOfInterestTilePyramid.prototype.toJSON = function () {
        return {
            "type": "AreaOfInterest",
            "minX": this.minX,
            "maxX": this.maxX,
            "minY": this.minY,
            "maxY": this.maxY
        };
    };

	return AreaOfInterestTilePyramid;
});
