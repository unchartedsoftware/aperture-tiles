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
 * This module defines a TileIterator class, the equivalent of TileIterator in
 * binning-utilities.
 */
define(['class'], function (Class) {
    "use strict";

    var TileIterator;

    TileIterator = Class.extend({
        init: function (pyramid, level, minX, minY, maxX, maxY) {
            this.pyramid = pyramid;
            this.level = level;
            this.minTile = pyramid.rootToTile(minX, minY, level);
            this.maxTile = pyramid.rootToTile(maxX, maxY, level);
            this.curX = this.minTile.x;
            this.curY = this.minTile.y;
        },

        hasNext: function () {
            return (this.curX <= this.maxTile.x &&
                    this.curY <= this.maxTile.y);
        },

        next: function () {
            var tile = {
                x: this.curX,
                y: this.curY,
                level: this.level,
                numXBins: 256,
                numYBins: 256
            };

            this.curX = this.curX + 1;
            if (this.curX > this.maxTile.x) {
                this.curX = this.minTile.x;
                this.curY = this.curY + 1;
            }

            return tile;
        },

        getRest: function () {
            var all = [];

            while (this.hasNext()) {
                all[all.length] = this.next();
            }

            return all;
        }
    });

    return TileIterator;
});

