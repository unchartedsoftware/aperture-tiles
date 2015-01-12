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
 * @package binning
 */
( function() {

    "use strict";

    /**
     * Instantiate a TileIterator object.

     * @class TileIterator
     * @classdesc A TileIterator class, the equivalent of TileIterator
     *            in tile-service/binning-utilities.
     *
     * @param {Object} spec - The specification object.
     */
    function TileIterator( spec ) {
        this.pyramid = spec.pyramid;
        this.level = spec.level;
        this.minTile = spec.pyramid.rootToTile( spec.minX, spec.minY, spec.level );
        this.maxTile = spec.pyramid.rootToTile( spec.maxX, spec.maxY, spec.level );
        this.curX = this.minTile.xIndex;
        this.curY = this.minTile.yIndex;
    }

    /**
     * Returns true if there is another tile, false if there isn't.
     * @memberof TileIterator
     *
     * @returns {boolean} Whether or not a next tile exists.
     */
    TileIterator.prototype.hasNext = function() {
        return (this.curX <= this.maxTile.xIndex &&
                this.curY <= this.maxTile.yIndex);
    };

    /**
     * Returns the next tile in the iterator.
     * @memberof TileIterator
     *
     * @returns {Object} The next tile object.
     */
    TileIterator.prototype.next = function() {
        var tile = {
            xIndex: this.curX,
            yIndex: this.curY,
            level: this.level,
            xBinCount: 256,
            yBinCount: 256
        };
        this.curX = this.curX + 1;
        if ( this.curX > this.maxTile.xIndex ) {
            this.curX = this.minTile.xIndex;
            this.curY = this.curY + 1;
        }
        return tile;
    };

    /**
     * Returns an array of all remaining tiles the iterator.
     * @memberof TileIterator
     *
     * @returns {Array} All tiles left in the iterator.
     */
    TileIterator.prototype.getRest = function () {
        var all = [];
        while (this.hasNext()) {
            all[all.length] = this.next();
        }
        return all;
    };

    /**
     * Returns a string containing all remaining tile indices in the iterator.
     * @memberof TileIterator
     *
     * @returns {String} The next tile object.
     */
    TileIterator.prototype.toString = function () {
        var srep = "", index;
        while ( this.hasNext() ) {
            if ( srep.length > 0 ) {
                srep = srep + "|";
            }
            index = this.next();
            srep = srep + "["+index.xIndex+"/"+index.xBinCount+","+index.yIndex+"/"+index.yBinCount+", lvl "+index.level+"]";
        }
        return srep;
    };

    /**
     * Returns the tile bounds of all tiles within the iterator.
     * @memberof TileIterator
     *
     * @returns {Object} The tile bounds of the iterator.
     */
    TileIterator.prototype.toTileBounds = function () {
        return {
            'minX': this.minTile.xIndex,
            'maxX': this.maxTile.xIndex,
            'minY': this.minTile.yIndex,
            'maxY': this.maxTile.yIndex,
            'minZ': this.level,
            'maxZ': this.level
        };
    };

    module.exports = TileIterator;
}());

