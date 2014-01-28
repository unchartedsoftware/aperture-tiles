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
 * A simple test layer to test the client side of client rendering.
 *
 * This layer simply puts the tile coordinates and another string in 
 * the middle of each tile.
 */
define(function (require) {
    "use strict";
    var MapLayer = require('./MapLayer'),
        TileIterator = require('../client-rendering/TileIterator'),
        AoIPyramid = require('../client-rendering/AoITilePyramid'),
        mapPyramid, DebugLayer;

    mapPyramid = new AoIPyramid(-20037500, -20037500,
                                20037500,  20037500);

    DebugLayer = MapLayer.extend({
        ClassName: "DebugLayer",
        init: function (id) {
            this._super(id);
            this.tracker.setPosition('center');
        },

        createLayer: function(nodeLayer) {
            var mapUpdateFcn;

            this.labelLayer = this._nodeLayer.addLayer(aperture.LabelLayer);
            this.labelLayer.map('label-count').from(function () {
                return this.bin.value.length;
            });
            this.labelLayer.map('text').from(function (index) {
                return this.bin.value[index];
            });
            this.labelLayer.map('offset-y').from(function (index) {
                var yOffset = 16*(index - this.bin.value.length/2);
//                console.log("Writing text "+this.bin.value[index]+" to ["+this.longitude+", "+this.latitude+"], offset="+yOffset+" ("+index+" of "+this.bin.value.length+")");
                return yOffset;
            });
            this.labelLayer.map('fill').asValue('#FFF');
            this.labelLayer.map('font-outline').asValue('#222');
            this.labelLayer.map('font-outline-width').asValue(3);
            this.labelLayer.map('visible').asValue(true);

            mapUpdateFcn = $.proxy(this.updateData, this);
            this.map.map.on('zoom', mapUpdateFcn);
            this.map.map.on('panend', mapUpdateFcn);
            mapUpdateFcn();
        },

        updateData: function (event) {
            var i, tiles, tile, tileData, x, y,
                level = this.map.map.getZoom(),
                bounds = this.map.map.olMap_.getExtent(),
                bins = 2;

            // Figure out what tiles we need
            tiles = new TileIterator(mapPyramid, level,
                                     bounds.left, bounds.bottom,
                                     bounds.right, bounds.top).getRest();

            tiles = this.tracker.filterTiles(tiles);

            for (i=0; i<tiles.length; ++i) {
                tile = tiles[i];
                x = tile.xIndex;
                y = tile.yIndex;

                tileData = {
                    tileIndex: {
                        xIndex: x,
                        yIndex: y,
                        level: level
                    },
                    tile: {
                        xIndex: x, 
                        yIndex: y, 
                        level: level,
                        xBinCount: bins,
                        yBinCount: bins,
                        values: [
                            {value: [
                                "["+level+","+x+","+y+"::0,0]",
                                "bin 0"
                            ]},
                            {value: [
                                "["+level+","+x+","+y+"::1,0]",
                                "bin 1"
                            ]},
                            {value: [
                                "["+level+","+x+","+y+"::0,1]",
                                "bin 2"
                            ]},
                            {value: [
                                "["+level+","+x+","+y+"::1,1]",
                                "bin 3"
                            ]}
                        ],
                        "default": "",
                        meta: "Test string array data"
                    }
                };
                this.tracker.addTileData(tileData);
            }
        }
    });

    return DebugLayer;
});
