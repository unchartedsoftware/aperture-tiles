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
 * This module defines a DataService class that is to be injected into a 
 * TileTracker instance. This class is responsible for RESTful requests
 * from the server
 */
define(function (require) {
    "use strict";

    var Class = require('../class'),
        DataService;


    DataService = Class.extend({
        ClassName: "DataService",

        /**
         * Construct a DataService
         */
        init: function () {

            this.data = {};
            this.dataStatus = {};
            this.referenceCount = {};
            this.memoryQueue = [];
            this.getCallbacks = {};
            
        },

      
        getDataArray: function ( tilekeys ) {
            var i,
                allData = [];

            for(i=0; i<tilekeys.length; i++) {
                // if data exists in tile
                if ( this.data[ tilekeys[i] ] !== undefined ) {
                    // check format of data
                    if ( $.isArray( this.data[ tilekeys[i] ] ) ) {
                        // for each tile, data is an array, merge it together
                        $.merge( allData, this.data[ tilekeys[i] ] );
                    } else {
                        // for each tile, data is an object
                        allData.push( this.data[ tilekeys[i] ] );
                    }
                }
            }
            return allData;
        },


        getDataObject: function ( tilekeys ) {
            var i,
                allData = {};

            for(i=0; i<tilekeys.length; i++) {
                // if data exists in tile
                if ( this.data[ tilekeys[i] ] !== undefined ) {
                    allData[ tilekeys[i] ] = this.data[ tilekeys[i] ];
                }
            }
            return allData;
        },


        /**
         * Create a universal unique key for a given tile
         *
         * @param tilekeys Array of tile identification keys to merge into node data
         */
        createTileKey: function (tile) {
            return tile.level + "," + tile.xIndex + "," + tile.yIndex;
        },


        /**
         * Create a universal unique key for a given bin in a given tile
         *
         * @param tileKey The universal tile key, as calculated by the
         *                createTileKey method.
         * @param bin The bin; assumed to be in {x: #, y: #} format.
         */
        createBinKey: function (tileKey, bin) {
            return tileKey + ":" + bin.x + "," + bin.y;
        },


        /**
         * Increment reference count for specific tile data
         *
         * @param tilekey tile identification key for data
         */
        addReference: function(tilekey) {

            if (this.referenceCount[tilekey] === undefined) {
                this.referenceCount[tilekey] = 0;
            }

            // ensure fresh requests cause tile to be pushed onto the back of queue
            if (this.memoryQueue.indexOf(tilekey) !== -1) {
                this.memoryQueue.splice(this.memoryQueue.indexOf(tilekey), 1);
            }
            this.memoryQueue.push(tilekey);

            this.referenceCount[tilekey]++;
        },


        /**
         * Decrement reference count for specific tile data, if reference count hits 0,
         * call memory management function
         *
         * @param tilekey tile identification key for data
         */
        removeReference: function(tilekey) {

            this.referenceCount[tilekey]--;
            if (this.referenceCount[tilekey] === 0) {
                this.memoryManagement();
            }

        },


        /**
         * Returns current reference count of a tile
         *
         * @param tilekey tile identification key for data
         */
        getReferenceCount: function(tilekey) {
            if (this.referenceCount[tilekey] === undefined) {
                return 0;
            }
            return this.referenceCount[tilekey];
        },


        /**
         * Manages how defunct tile data is de-allocated, once max number
         * of in memory tiles is reached, de-allocates tiles that were
         * used longest ago. Current max tile count is 100
         */
        memoryManagement: function() {

            var i = 0,
                tilekey,
                MAX_NUMBER_OF_ENTRIES = 1000;

            while (this.memoryQueue.length > MAX_NUMBER_OF_ENTRIES) {

                // while over limit of tiles in memory,
                // iterate from last used tile to most recent and remove them if
                // reference counts are 0
                tilekey = this.memoryQueue[i];
                if (this.getReferenceCount(tilekey) === 0) {
                    delete this.data[tilekey];
                    delete this.dataStatus[tilekey];
                    delete this.referenceCount[tilekey];
                    this.memoryQueue.splice(i, 1);
                }
                i++;
            }

        },


        getDataFromServer: function(requestedTiles, callback) {
            console.log("Warning: DataService class is intended to be extended");
        },


        getRequest: function(tilekey, callback) {
            console.log("Warning: DataService class is intended to be extended");
        },


        getCallback: function( annotationData ) {
            console.log("Warning: DataService class is intended to be extended");
        },


        postRequest: function(annotation) {
            console.log("Warning: DataService class is intended to be extended");
        },


        postCallback: function( postResult ) {
            console.log("Warning: DataService class is intended to be extended");
        }

    });

    return DataService;
});
