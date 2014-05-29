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
 * A simple test layer to test the client side of client rendering.
 *
 * This layer simply puts the tile coordinates and another string in 
 * the middle of each tile.
 */
define(function (require) {
    "use strict";
	
	
	
    var ClientRenderer = require('../ClientRenderer'),
        DebugRenderer;

		
		
    DebugRenderer = ClientRenderer.extend({
        ClassName: "DebugLayer",
		
        init: function () {
            this._super('debug');
        },		
		
		
        createLayer: function(nodeLayer) {

			var that = this,
                hoveredTilekey = "";

            this.plotLayer = nodeLayer;
            this.labelLayer = this.plotLayer.addLayer(aperture.LabelLayer);
			this.labelLayer.map('label-count').asValue(1);
			this.labelLayer.map('text').from(function() { return this.tilekey; });
			this.labelLayer.map('offset-x').asValue(10);
			this.labelLayer.map('offset-y').asValue(118);
			this.labelLayer.map('text-anchor').asValue('start');
            this.labelLayer.map('fill').from(function() {
                return hoveredTilekey === this.tilekey ? that.BLUE_COLOUR : that.WHITE_COLOUR;
            });
            this.labelLayer.map('font-outline').asValue('#000000');
            this.labelLayer.map('font-outline-width').asValue(3);
            this.labelLayer.map('visible').from(function(){
                return that.isSelectedView(this);
			});

            this.labelLayer.on('mousemove', function(event) {
                hoveredTilekey = event.data.tilekey;
                that.plotLayer.all().where(event.data).redraw();
                return true;
            });

            this.labelLayer.on('mouseout', function(event) {
                hoveredTilekey = "";
                that.plotLayer.all().where(event.data).redraw();
            });

        }

    });

    return DebugRenderer;
});
