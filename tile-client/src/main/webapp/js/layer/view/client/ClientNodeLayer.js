/*
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
 * This module defines the base class for a client render layer. Must be 
 * inherited from for any functionality.
 */
define(function (require) {
    "use strict";



    var Class = require('../../../class'),
        ClientNodeLayer;



    ClientNodeLayer = Class.extend({
        ClassName: "ClientNodeLayer",

        init: function( spec ) {

            this.map_ = spec.map || null;
            this.xAttr_ = spec.xAttr || null;
            this.yAttr_ = spec.yAttr || null;
            this.idKey_=  spec.idKey || null;

            this.$root_ = this.createLayerRoot();
            this.nodes_ = [];
            this.nodesById_ = {};

            this.map_.getElement().append( this.$root_ );
            this.map_.on('move', $.proxy( this.onMapUpdate, this ));

            this.layers_ = [];
            this.subset_ = [];
        },


        removeLayer : function( layer ) {

            var index = this.layers_.indexOf( layer );
            if ( index !== -1 ) {
                this.layers_.nodeLayer_ = null;
                this.layers_.splice( layer );
            }
        },


        addLayer : function( layer ) {

            layer.nodeLayer_ = this;
            this.layers_.push( layer );
        },


        createLayerRoot : function() {
            var pos = this.map_.getViewportPixelFromMapPixel( 0, this.map_.getMapHeight() );
            return $('<div class="client-layer" style="position:absolute; left:'+pos.x+'px; top:' +pos.y+ 'px; width=0px; height=0px"></div>');
        },


        createNodeRoot : function(data) {
            var pos = this.map_.getMapPixelFromCoord( data[this.xAttr_], data[this.yAttr_] ),
                nodeId = data[this.idKey_] || "";
            return $('<div id="'+nodeId+'" class="tile-root" style="position:absolute; left:'+pos.x+'px; top:'+ (this.map_.getMapHeight() - pos.y) +'px; width: 256px; height:256px; -webkit-backface-visibility: hidden; backface-visibility: hidden;"></div>');
        },


        destroyNode: function( node ) {
            if (node.$root) {
                node.$root.remove(); // this will destroy all child elements
            }
        },


        createNode: function( data ) {

            // create and append tile root to layer root
            var $nodeRoot = this.createNodeRoot( data );
            this.$root_.append( $nodeRoot );

            // allow events to propagate through to map
            this.map_.enableEventToMapPropagation( $nodeRoot );

            return {
                 data : data,
                 $root : $nodeRoot
            };
        },


        removeNodeById: function( key ) {
            var node = this.nodesById_[ key ],
                index = this.nodes_.indexOf( node );
            this.destroyNode( node );
            this.nodes_.splice(index, 1);
            delete this.nodesById_[ key ];
        },


        removeNode: function( node ) {
            var index = this.nodes_.indexOf( node );
            this.destroyNode( this.nodes_[index] );
            this.nodes_.splice( index, 1 );
        },


        onMapUpdate: function() {
            var pos;
            // only root will execute the following code
            pos = this.map_.getViewportPixelFromMapPixel( 0, this.map_.getMapHeight() );
            this.$root_.css({
                top: pos.y + "px",
                left: pos.x + "px"
            });
        },


        all: function( data ) {

            var that = this,
                nodes = this.nodes_,
                idKey = this.idKey_,
                i,
                node,
                newData = [],
                newNodes = [],
                exists;

            function allByKey() {

                var key,
                    nodesById = that.nodesById_,
                    defunctNodesById = {};

                // keep list of current nodes, to track which ones are not in the new set
                for (i=0; i<nodes.length; ++i) {
                    defunctNodesById[ nodes[i].data[idKey] ] = true;
                }

                // only root will execute the following code
                for (i=0; i<data.length; i++) {

                    key = data[i][idKey];
                    exists = nodesById[key] !== undefined;

                    if ( exists ) {
                        // remove from tracking list
                        delete defunctNodesById[ key ];
                    } else {
                        // new data
                        newData.push( data[i] );
                    }
                }

                // destroy and remove all remaining nodes
                for (key in defunctNodesById) {
                    if (defunctNodesById.hasOwnProperty( key )) {

                        that.removeNodeById( key );
                    }
                }

                // create nodes for new data
                for (i=0; i<newData.length; i++) {
                    node = that.createNode( newData[i] );
                    nodes.push( node );
                    newNodes.push( node );
                    key = newData[i][idKey];
                    nodesById[ key ] = node;
                }
            }

            function allNoKey() {

                var defunctNodesArray = [],
                    index;

                // keep list of current nodes, to track which ones are not in the new set
                for (i=0; i<nodes.length; ++i) {
                    defunctNodesArray.push( that.findNodeFromData( nodes[i].data ) );
                }

                // only root will execute the following code
                for (i=0; i<data.length; i++) {

                    exists = that.doesNodeExist( data[i] );

                    if ( exists ) {
                        // remove from tracking list
                        index = defunctNodesArray.indexOf(  that.findNodeFromData( data[i] ) );
                        defunctNodesArray.splice(index, 1);
                    } else {
                        // new data
                        newData.push( data[i] );
                    }
                }

                // destroy and remove all remaining nodes
                for (i=0; i<defunctNodesArray.length; i++) {
                    // remove from array
                    that.removeNode( defunctNodesArray[i] );
                }

                // create nodes for new data
                for (i=0; i<newData.length; i++) {
                    node = that.createNode( newData[i] );
                    nodes.push( node );
                    newNodes.push( node );
                }
            }

            if ( idKey ) {
                allByKey();
            } else {
                allNoKey();
            }

            this.subset_ = newNodes;
            return this;
        },


        join : function( data ) {

             var that = this,
                 nodes = this.nodes_,
                 nodesById = this.nodesById_,
                 idKey = this.idKey_,
                 i,
                 key,
                 node,
                 newNodes = [],
                 exists;

            function joinByKey() {

                for (i=0; i<data.length; i++) {

                    key = data[i][idKey];
                    exists = nodesById[key] !== undefined;

                    if ( !exists ) {
                        node = that.createNode( data[i] );
                        nodes.push( node );
                        newNodes.push( node );
                        nodesById[ key ] = node;
                    }
                }
            }

            function joinNoKey() {

                for (i=0; i<data.length; i++) {

                    exists = that.doesNodeExist( data[i] );

                    if ( !exists ) {
                        node = that.createNode( data[i] );
                        nodes.push( node );
                        newNodes.push( node );
                    }
                }
            }

            if ( idKey ) {
                joinByKey();
            } else {
                joinNoKey();
            }

            this.subset_ = newNodes;
            return this;
        },


        remove : function( data ) {

            var that = this,
                idKey = this.idKey_,
                key,
                i;

            // remove by data
            function removeByData() {
                for (i=0; i<data.length; i++) {
                    that.removeNode( that.findNodeFromData( data[i] ) );
                }
            }

            // remove by data, by id
            function removeByDataId() {

                for (i=0; i<data.length; i++) {
                    key = data[i][ idKey ];
                    that.removeNodeById( key );
                }

            }

            // remove by ids
            function removeById() {

                for (i=0; i<data.length; i++) {
                    key = data[i];
                    that.removeNodeById( key );
                }

            }

            // wrap in array if it already isn't
            if ( !$.isArray( data ) ) {
                data = [ data ];
            }

            switch ( typeof data[0] ) {
                case 'object':
                    if (idKey) {
                        removeByDataId();
                    } else {
                        removeByData();
                    }
                    break;
                case 'string':
                    removeById();
                    break;
            }

            return this;
        },


        where: function( idEval ) {

            var that = this,
                nodes = this.nodes_,
                nodesById = this.nodesById_,
                subset = [],
                node,
                i;

            function whereById() {
                for (i=0; i<idEval.length; i++) {
                    node = nodesById[ idEval[i] ];
                    if (node) {
                        subset.push( node );
                    }
                }
            }

            function whereByFunction() {
                for (i=0; i<nodes.length; i++) {
                    if ( idEval( nodes[i].data ) ) {
                        subset.push( nodes[i] );
                    }
                }
            }

            function whereByData() {
                for (i=0; i<idEval.length; i++) {
                    node = that.findNodeFromData( idEval[i] );
                    if (node) {
                        subset.push( node );
                    }
                }
            }

            // wrap in array if it already isn't
            if ( !$.isArray( idEval ) ) {
                idEval = [ idEval ];
            }

            switch ( typeof idEval[0] ) {
                case 'object':
                    whereByData();
                    break;
                case 'function':
                    whereByFunction();
                    break;
                case 'string':
                    whereById();
                    break;
            }

            this.subset_ = subset;
            return this;
        },


        findNodeFromData: function(data) {
            var nodes = this.nodes_,
                i;
            for (i=0; i<nodes.length; i++) {
                if ( nodes[i].data === data ) {
                    return nodes[i];
                }
            }
            return null;
        },


        doesNodeExist: function(data) {
            return this.findNodeFromData( data ) !== null;
        },


        redraw: function() {
            var subset = this.subset_,
                layers = this.layers_,
                i;

            for (i=0; i<subset.length; i++) {
                subset[i].$root.empty();
            }

            for (i=0; i<layers.length; i++) {
                layers[i].redraw( subset );
            }
        }


    });

    return ClientNodeLayer;
});
