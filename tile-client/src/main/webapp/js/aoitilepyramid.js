/**
 * Copyright (c) 2013 Oculus Info Inc.
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

/* JSLint global declarations: these objects don't need to be declared. */
/*global OpenLayers */

/**
 * This module defines a TilePyramid class, the equivalent of AOITilePyramid in
 * binning-utilities.
 */
define(['class'], function (Class) {
    "use strict";

    var AoITilePyramid;

    AoITilePyramid = Class.extend({
        init: function (minX, minY, maxX, maxY) {
            this.minX = minX;
            this.minY = minY;
            this.maxX = maxX;
            this.maxY = maxY;
        },

        getProjection: "EPSG:4326",
        getTileScheme: "TMS",

        rootToTile: function (x, y, level, bins) {
            var numDivs, tileX, tileY;

            numDivs = 1 << level;
            tileX = Math.floor(numDivs * (x - this.minX) / (this.maxX - this.minX));
            tileY = Math.floor(numDivs * (y - this.minY) / (this.maxY - this.minY));

            if (!bins) {
                bins = 256;
            }

            return {level: level,
                    x: tileX,
                    y: tileY,
                    numXBins: bins,
                    numYBins: bins};
        },

        rootToBin: function (x, y, tile) {
            var pow2, tileXSize, tileYSize, xInTile, yInTile, binX, binY;

            pow2 = 1 << tile.level;
            tileXSize = (this.maxX - this.minX) / pow2;
            tileYSize = (this.maxY - this.minY) / pow2;

            xInTile = x - this.minX - tile.x * tileXSize;
            yInTile = y - this.minY - tile.y*tileYSize;

            binX = Math.floor(xInTile * tile.numXBins / tileXSize);
            binY = Math.floor(yInTile * tile.numYBins / tileYSize);

            return {x: binX,
                    y: tile.numYBins - 1 - binY};
        },

        getTileBounds: function (tile) {
            var pow2, tileXSize, tileYSize;

            pow2 = 1 << tile.level;
            tileXSize = (this.maxX - this.minX) / pow2;
            tileYSize = (this.maxY - this.minY) / pow2;

            return {minX: this.minX + tileXSize * tile.x,
                    minY: this.minY + tileYSize * tile.y,
                    maxX: this.minX + tileXSize * (tile.x + 1),
                    maxY: this.minY + tileYSize * (tile.y + 1),
                    centerX: this.minX + tileXSize * (tile.x + 0.5),
                    centerY: this.minY + tileYSize * (tile.y + 0.5),
                    width: tileXSize,
                    height: tileYSize};
        },

        getBinBounds: function (tile, bin) {
            var pow2, tileXSize, tileYSize, binXSize, binYSize, adjustedY;

            pow2 = 1 << tile.level;
            tileXSize = (this.maxX - this.minX) / pow2;
            tileYSize = (this.maxY - this.minY) / pow2;
            binXSize = tileXSize / tile.numXBins;
            binYSize = tileYSize / tile.numYBins;

            adjustedY = tile.numYBins - 1 - bin.y;

            return {minX: this.minX + tileXSize * tile.x + binXSize * bin.x,
                    minY: this.minY + tileYSize * tile.y + binYSize * adjustedY,
                    maxX: this.minX + tileXSize * tile.x + binXSize * (bin.x + 1),
                    maxY: this.minY + tileYSize * tile.y + binYSize * (adjustedY + 1),
                    centerX: this.minX + tileXSize * tile.x + binXSize * (bin.x + 0.5),
                    centerY: this.minY + tileYSize * tile.y + binYSize * (adjustedY + 0.5),
                    width: binXSize,
                    height: binYSize};
        }
    });

    return AoITilePyramid;
});
