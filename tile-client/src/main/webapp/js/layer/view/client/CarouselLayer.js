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

/*global OpenLayers*/

/**
 * This module defines a CarouselLayer class which inherits from a ViewController and provides a user
 * interface and event handler for switching between views for individual tiles
 */
define(function (require) {
    "use strict";



    var ViewController = require('./ViewController'),
        CarouselLayer;



    CarouselLayer = ViewController.extend({

        /**
         * Construct a carousel
         */
        init: function (spec) {

            var that = this;

            // call base class ViewController constructor
            this._super(spec);
            this.previousMouse = {};
            this.selectedTileInfo = {};

            // add mouse move and zoom callbacks
            this.map.on('mousemove', function(event) {
                var tilekey = that.map.getTileKeyUnderMouse(event.xy.x, event.xy.y);
                that.updateSelectedTile(tilekey);
                that.previousMouse.x = event.xy.x;
                that.previousMouse.y = event.xy.y;
            });

            this.map.on('zoomend', function() {
                var tilekey = that.map.getTileKeyUnderMouse( that.previousMouse.x, that.previousMouse.y );
                that.updateSelectedTile(tilekey);
            });

            // create the carousel UI
            this.createUI();
        },


        /**
         * Construct the user interface aperture.iconlayers
         */
        createUI: function() {

            var positionFactor = (this.views.length - 1) / 2,
                i, j;

            // TODO: everything should be put on its own PlotLayer instead of directly on the mapNodeLayer
            // TODO: currently does not render correctly if on its own PlotLayer...
            this.plotLayer = this.mapNodeLayer;

            // tile outline layer
            this.outline = this.createTileOutlineLayer();
            // left and right view buttons
            this.leftButton = this.createViewSelectionLayer('left');
            this.rightButton = this.createViewSelectionLayer('right');
            // index dots
            this.indexButtons = [];
            for (i=0, j=-positionFactor; j<=positionFactor; i++, j++) {
                this.indexButtons.push(this.createViewIndexLayer(i, j));
            }
        },


        /**
         * Construct aperture.iconlayers for the left/right view panning buttons
         */
        createViewSelectionLayer: function(position) {

            var that = this,
                icon = (position === 'left') ? "./images/chevron_L.png" : "./images/chevron_R.png",
                x = (position === 'left') ? 0.03 : 0.47, // -0.22 : 0.22,
                y = 0,
                hover = new aperture.Set('tilekey'), // separate tiles by tile key for hovering
                viewSelectionLayer;

            viewSelectionLayer = this.plotLayer.addLayer(aperture.IconLayer);

            viewSelectionLayer.map('width').asValue(15).filter(hover.scale(1.2));
            viewSelectionLayer.map('height').asValue(42).filter(hover.scale(1.5));
            viewSelectionLayer.map('anchor-x').asValue(0.5);
            viewSelectionLayer.map('anchor-y').asValue(0.5);
            viewSelectionLayer.map('url').asValue(icon);
            viewSelectionLayer.map('icon-count').asValue(1);
            viewSelectionLayer.map('x').from(function(){
                return x/Math.pow(2, that.map.getZoom()-1);
            });
            viewSelectionLayer.map('y').from(function(){
                return y/Math.pow(2, that.map.getZoom()-1);
            });

            viewSelectionLayer.on('mousemove', function(event) {
                hover.add(event.data.tilekey);
            });

            viewSelectionLayer.on('mouseout', function() {
                hover.clear();
            });

            viewSelectionLayer.on('click', function(event) {
				var tilekey = event.data.tilekey,
                    mod = function (m, n) {
                        return ((m % n) + n) % n;
                    },
                    inc = (position === 'left') ? -1 : 1,
                    oldIndex = that.getTileViewIndex(tilekey),
                    newIndex = mod(oldIndex + inc, that.views.length);

                if (event.source.button !== 0) {
                    // not left click, abort
                    return;
                }

                that.onTileViewChange(tilekey, newIndex);
                return true; // swallow event
            });

            viewSelectionLayer.map('visible').from( function() {
                return (this.tilekey === that.selectedTileInfo.tilekey);
            });

            return viewSelectionLayer;
        },


        /**
         * Construct aperture.iconlayers for the index dots representing each view
         */
        createViewIndexLayer: function(viewIndex, spacingFactor) {

            var that = this,
                noSelectIcon = "./images/no_select.png",
                selectIcon = "./images/select.png",
                spacing = 0.04,
                hover = new aperture.Set('tilekey'), // separate tiles by bin key for hovering
                viewIndexLayer;

            viewIndexLayer = this.plotLayer.addLayer(aperture.IconLayer);

            viewIndexLayer.map('width').asValue(12).filter(hover.scale(1.4));
            viewIndexLayer.map('height').asValue(12).filter(hover.scale(1.4));
            viewIndexLayer.map('anchor-x').asValue(0.5);
            viewIndexLayer.map('anchor-y').asValue(0.5);
            viewIndexLayer.map('icon-count').asValue(1);
            viewIndexLayer.map('x').from(function() {
                var zoomFactor = Math.pow(2, that.map.getZoom()-1);
                return (0.25/zoomFactor) + (spacingFactor*spacing/zoomFactor);
            });
            viewIndexLayer.map('y').from(function(){
                return 0.2/Math.pow(2, that.map.getZoom()-1);
            });

            viewIndexLayer.map('url').from(function() {
                var id = that.getTileViewIndex(this.tilekey),
                    url;
                if ( id !== viewIndex ) {
                    url = noSelectIcon;
                } else {
                    url = selectIcon;
                }
                return url;
            });


            viewIndexLayer.on('mousemove', function(event) {
                hover.add(event.data.tilekey);
            });

            viewIndexLayer.on('mouseout', function() {
                hover.clear();
            });

            viewIndexLayer.on('click', function(event) {
				if (event.source.button !== 0) {
                    // not left click, abort
                    return true;
                }
                that.onTileViewChange(event.data.tilekey, viewIndex);
                return true; // swallow event
            });

            viewIndexLayer.map('visible').from( function() {
                return (this.tilekey === that.selectedTileInfo.tilekey);
            });

            return viewIndexLayer;
        },


        /**
         * Construct aperture.barLayers for the tile outline during mouse hover
         */
        createTileOutlineLayer: function() {

            var that = this,
                OUTLINE_THICKNESS = 1,
                outlineLayer;

            outlineLayer = this.plotLayer.addLayer(aperture.BarLayer);
            outlineLayer.map('fill').asValue('#FFFFFF');
            outlineLayer.map('orientation').asValue('vertical');
            outlineLayer.map('bar-count').asValue(4);
            outlineLayer.map('length').from(function(index) {
                switch(index){
                    case 0: return 256;
                    case 1: return OUTLINE_THICKNESS;
                    case 2: return 256;
                    default: return OUTLINE_THICKNESS;
                }
            });
            outlineLayer.map('width').from(function(index) {
                switch(index){
                    case 0: return OUTLINE_THICKNESS;
                    case 1: return 256;
                    case 2: return OUTLINE_THICKNESS;
                    default: return 256;
                }
            });
            outlineLayer.map('offset-x').from( function(index) {
                switch(index){
                    case 0: return 0;
                    case 1: return 0;
                    case 2: return 256 - (OUTLINE_THICKNESS-1);
                    default: return 0;
                }
            });
            outlineLayer.map('offset-y').from( function(index) {
                switch(index){
                    case 0: return -128;
                    case 1: return 128 - (OUTLINE_THICKNESS-1);
                    case 2: return -128;
                    default: return -128;
                }
            });

            outlineLayer.map('visible').from( function() {
                return (this.tilekey === that.selectedTileInfo.tilekey);
            });

            return outlineLayer;
        },


        /**
         * Updates selected tile nodeLayer data and redraws UI
         * @param tilekey tile identification key of the form: "level,x,y"
         */
        updateSelectedTile: function(tilekey) {

            this.selectedTileInfo = {
                previouskey : this.selectedTileInfo.tilekey,
                tilekey : tilekey
            };
			
			this.redrawUI();
        },
		
		
		/**
		 *	Redraws the carousel specific layers
		 */
		redrawUI: function() {
			
			var i;
			
			this.outline.all().redraw();
            this.leftButton.all().redraw();
            this.rightButton.all().redraw();
            for (i=0; i<this.indexButtons.length; i++) {
                this.indexButtons[i].all().redraw();
            }
			
		},

        /**
         * Maps a tilekey to its current view index. If none is specified, use default
         * @param tilekey tile identification key of the form: "level,x,y"
         */
        getTileViewIndex: function(tilekey) {
            // given a tile key "level + "," + xIndex + "," + yIndex"
            // return the view index
            var viewIndex;
            if (this.tileViewMap[tilekey] === undefined) {
                viewIndex = this.defaultViewIndex ;
            } else {
                viewIndex = this.tileViewMap[tilekey];
            }
            return viewIndex;
        }

     });

    return CarouselLayer;
});
