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

/* JSLint global declarations: these objects don't need to be declared. */
/*global OpenLayers */



/**
 * This module defines a TileAnnotationIndexer class, the equivalent of TileAnnotationIndexer in
 * annotation-services.
 */
define(function (require) {
    "use strict";



    var Class = require('../class'),
        WebPyramid = require('../binning/WebTilePyramid'),
        NUM_LEVELS = 20,    // MUST MATCH AnnotationIndexer.NUM_LEVELS in 'AnnotationIndexer.java'
        NUM_BINS = 8,       // MUST MATCH AnnotationTile.NUM_BINS in 'AnnotationTile.java'
        TileAnnotationIndexer;

		
		
    TileAnnotationIndexer = Class.extend({
        ClassName: "TileAnnotationIndexer",
        init: function () {
            this._pyramid = new WebPyramid();
        },

        getIndices: function( data ) {
            var indices = [],
                i;

            for (i=0; i<NUM_LEVELS; i++) {
                indices.push( this.getIndex( data, i ) );
            }
            return indices;
        },

        getIndex: function( data, level ) {

            // fill in defaults if dimensions are missing
            var xExists = data.x !== null && data.x !== undefined,
                yExists = data.y !== null && data.y !== undefined,
                x = xExists ? data.x : 0,
                y = yExists ? data.y : 0,
                tile, bin, index;

            // map from raw x and y to tile and bin
            tile = this._pyramid.rootToTile( x, y, level, NUM_BINS );
            bin = this._pyramid.rootToBin( x, y, tile );

            if ( xExists && yExists ) {

                index = {
                    tilekey: tile.level + "," + tile.xIndex + "," + tile.yIndex,
                    binkey: "[" + bin.x + ", " + bin.y + "]"
                };

            } else if ( !xExists ) {

                index = {
                    tilekey: tile.level + "," + (-1).toString() + "," + tile.yIndex,
                    binkey: "[" + (-1).toString() + ", " + bin.y + "]"
                };

            } else {

                index = {
                    tilekey: tile.level + "," + tile.xIndex + "," + (-1).toString(),
                    binkey: "[" + bin.x + ", " + (-1).toString() + "]"
                };
            }

            return index;
        }

    });

    return TileAnnotationIndexer;
});
