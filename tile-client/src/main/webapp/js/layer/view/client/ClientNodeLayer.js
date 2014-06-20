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


define(function (require) {
    "use strict";



    var Class = require('../../../class'),
        ClientNodeLayer;



    ClientNodeLayer = Class.extend({
        ClassName: "ClientNodeLayer",

        Z_INDEX_OFFSET : 100,

        init: function( spec ) {

            this.map_ = spec.map || null;
            this.xAttr_ = spec.xAttr || null;
            this.yAttr_ = spec.yAttr || null;
            this.idKey_=  spec.idKey || null;
            this.propagate = spec.propagate === undefined ? true : spec.propagate;

            this.Z_INDEX = this.map_.getZIndex() + this.Z_INDEX_OFFSET;

            this.createLayerRoot();
            this.nodes_ = [];
            this.nodesById_ = {};

            this.layers_ = [];
            this.subset_ = [];

            this.map_.on( 'zoom', $.proxy(this.clear, this) );
        },


        getRootElement: function() {

            return this.$root_;
        },


        removeLayer : function( layer ) {

            var layers = this.layers_,
                index = layers.indexOf( layer );
            if ( index !== -1 ) {
                layers.nodeLayer_ = null;
                layers.splice( layer );
            }
        },


        addLayer : function( layer ) {

            layer.nodeLayer_ = this;
            this.layers_.push( layer );
        },


        createLayerRoot : function() {
            // create layer root div
            this.$root_ = $('<div style="position:relative; z-index:'+this.Z_INDEX+';"></div>');
            // append to map root
            this.map_.getRootElement().append( this.$root_ );
            if ( this.propagate ) {
                // allow mouse events to propagate through to map
                this.map_.enableEventToMapPropagation( this.$root_ );
            }
        },


        createNodeRoot : function(data) {
            var pos = this.map_.getMapPixelFromCoord( data[this.xAttr_], data[this.yAttr_] );
            return $('<div style="position:absolute; left:'+pos.x+'px; top:'+ (this.map_.getMapHeight() - pos.y) +'px; height:0px; width:0px; -webkit-backface-visibility: hidden; backface-visibility: hidden;"></div>');
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
            return {
                 data : data,
                 $root : $nodeRoot
            };
        },


        getNodeById: function( id ) {
            return this.nodesById_[ id ];
        },


        getNodeByData: function(data) {
            var nodes = this.nodes_,
                i;
            for (i=0; i<nodes.length; i++) {
                if ( nodes[i].data === data ) {
                    return nodes[i];
                }
            }
            return null;
        },


        removeNodeById: function( key ) {
            var nodes = this.nodes_,
                nodesById = this.nodesById_,
                node = nodesById[ key ],
                index = nodes.indexOf( node );

            if (node) {
                this.destroyNode( node );
                nodes.splice(index, 1);
                delete nodesById[ key ];
            }
        },


        removeNode: function( node ) {
            var nodes = this.nodes_,
                index = nodes.indexOf( node );

            if (index !== -1) {
                this.destroyNode( nodes[index] );
                nodes.splice( index, 1 );
            }
        },


        onMapUpdate: function() {
            var map = this.map_,
                pos;
            pos = map.getViewportPixelFromMapPixel( 0, map.getMapHeight() );
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
                newNodes = [];

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

                    if ( nodesById[key] !== undefined ) {
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
                    defunctNodesArray.push( that.getNodeByData( nodes[i].data ) );
                }

                // only root will execute the following code
                for (i=0; i<data.length; i++) {

                    if ( that.doesNodeExist( data[i] ) ) {
                        // remove from tracking list
                        index = defunctNodesArray.indexOf(  that.getNodeByData( data[i] ) );
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
                 newNodes = [];

            function joinByKey() {

                for (i=0; i<data.length; i++) {

                    key = data[i][idKey];

                    if ( nodesById[key] === undefined ) {
                        node = that.createNode( data[i] );
                        nodes.push( node );
                        newNodes.push( node );
                        nodesById[ key ] = node;
                    }
                }
            }

            function joinNoKey() {

                for (i=0; i<data.length; i++) {

                    if ( that.doesNodeExist( data[i] ) ) {
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
                    that.removeNode( that.getNodeByData( data[i] ) );
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


        clear: function() {

            var nodes = this.nodes_,
                i;

            for (i=0; i<nodes.length; i++) {
                this.destroyNode( nodes[i] );
            }
            this.nodes_ = [];
            this.nodesById_ = {};
            this.subset_ = [];

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
                    node = that.getNodeByData( idEval[i] );
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


        doesNodeExist: function(data) {
            return this.getNodeByData( data ) !== null;
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
