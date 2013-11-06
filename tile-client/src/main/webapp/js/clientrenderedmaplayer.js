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
 * This modules defines a basic layer class that can be added to maps.
 */
define(['class', 'datalayer', 'maplayer', 'aoitilepyramid', 'tileiterator'],
       function(Class, DataLayer, MapLayer, Pyramid, TileIterator) {
    "use strict";


    var ClientRenderedMapLayer;

    ClientRenderedMapLayer = MapLayer.extend({
        init: function (id, layerSpec) {
            this._super(id);
            this.layerSpec = layerSpec;
            this.layerInfo = null;

            this.dataListener = new DataLayer([layerSpec]);
            // Only one layer, so we don't have to worry about waiting until 
            // all requests are fulfilled
            this.dataListener.setRetrievedCallback($.proxy(this.useLayerInfo,
                                                           this));

            this.dataListener.retrieveLayerInfo();
        },

        /*
         * Called when data the basic information about the layer is recieved 
         * from the server.
         */
        useLayerInfo: function (dataListener, layerInfo) {
            this.layerInfo = layerInfo;
            // TODO: May need to check if a map update has already occurred.
            // Or wait to add until we have this information, and add now 
            // if an add has previously been requested.
        },

        createLayer: function (nodeLayer) {
            this.setUpdateFcn($.proxy(this.updateData, this));
        },

        onTileReceived: $.proxy(function (tile) {
            tile.foobar = 'foobar';
        }, this),

        updateData: function (event) {
            if (this.layerInfo) {
                var
                bounds = this.map.map.olMap_.getExtent(),
                level = this.map.map.getZoom(),
                // The bounds of the full OpenLayers map
                pyramid = new Pyramid(-20037500, -20037500,20037500, 20037500),
                tiles,
                url,
                i;

                tiles = new TileIterator(pyramid, level,
                                         bounds.left, bounds.bottom,
                                         bounds.right, bounds.top).getRest();
                for (i = 0; i < tiles.length; ++i) {
                    url = this.layerInfo.tms;
                    url = url.substring(url.indexOf("/rest/")+5);
                    url = (url+'1.0.0/'+this.layerInfo.layer+'/'+
                           tiles[i].level+'/'+tiles[i].x+'/'+tiles[i].y+'.json');
                    aperture.io.rest(
                        url,
                        'GET',
                        this.onTileReceived
                    );
                }
            }
        }
    });

    return ClientRenderedMapLayer;
});
