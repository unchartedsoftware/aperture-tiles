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



    var Class = require('../class'),
        Util = require('../util/Util'),
        createLayerRoot,
        createNodeRoot,
        createNode,
        destroyNode,
        doesNodeExist,
        getNodeById,
        getNodeByData,
        removeNodeById,
        removeNode,
        HtmlNodeLayer;


    /**
     * Creates the root element for the entire html node layer and attaches it to the
     * root element of the map. If event propagation is enabled, listeners are attached to
     * process, and then propagate events through to underlying DOM elements.
     */
    createLayerRoot = function( that ) {

        // create layer root div
        that.$root_ = $('<div style="position:relative; z-index:'+that.Z_INDEX+';"></div>');
        // append to map root
        that.map_.getRootElement().append( that.$root_ );
        if ( that.propagate ) {
            // allow mouse events to propagate through to map
            Util.enableEventPropagation( that.$root_ );
        }
    };


    /**
     * Creates the root element for a data node. This node is positioned using the xAttr and yAttr
     * attributes specified upon creation of the layer. If no attributes are specified, the root of
     * each individual node is set to the root for the entire layer.
     */
    createNodeRoot = function( that, data ) {

        var pos;
        // if no node position specified, set node root as layer root
        if ( !that.xAttr_ || !that.yAttr_ ) {
            return that.$root_;
        }
        // otherwise, create node root based on position
        pos = that.map_.getMapPixelFromCoord( data[that.xAttr_], data[that.yAttr_] );
        return $('<div style="position:absolute;left:'+pos.x+'px; top:'+ (that.map_.getMapHeight() - pos.y) +'px; height:0px; width:0px; -webkit-backface-visibility: hidden; backface-visibility: hidden;"></div>');
    };


    /**
     * Creates a node object that stores both the data of the node, and the root element, and the elements
     * of the node.
     */
    createNode = function( that, data ) {

        // create and append tile root to layer root
        var $nodeRoot = createNodeRoot( that, data );
        that.$root_.append( $nodeRoot );
        return {
             data : data,
             $root : $nodeRoot,
             $elements : null
        };
    };


    /**
     * Destroys a node ensuring that all respective DOM elements are removed correctly.
     */
    destroyNode = function( that, node ) {

        // no node position specified
        if ( !that.xAttr_ || !that.yAttr_ ) {
            // destroy elements
            if (node.$elements) {
                node.$elements.remove();
            }
            return;
        }
        if (node.$root) {
            // destroy node root (along with all elements)
            node.$root.remove();
        }
    };


    /**
     * When given a data object, checks if that data is represented currently by the layer.
     */
    doesNodeExist = function( that, data) {
        return getNodeByData( that, data ) !== null;
    };


    /**
     * Returns a node by object reference
     */
    getNodeByData = function( that, data ) {
        var nodes = that.nodes_,
            i;
        for (i=0; i<nodes.length; i++) {
            if ( nodes[i].data === data ) {
                return nodes[i];
            }
        }
        return null;
    };

    /**
     * Removes a node by idKey, ensuring layer variables are adjusted accordingly.
     */
    removeNodeById = function( that, key ) {

        var nodes = that.nodes_,
            nodesById = that.nodesById_,
            node = nodesById[ key ],
            index = nodes.indexOf( node );

        if (node) {
            destroyNode( that, node );
            nodes.splice(index, 1);
            delete nodesById[ key ];
        }
    };

    /**
     * Removes a node, ensuring layer variables are adjusted accordingly.
     */
    removeNode = function( that, node ) {
        var nodes = that.nodes_,
            index = nodes.indexOf( node );

        if (index !== -1) {
            destroyNode( that, nodes[index] );
            nodes.splice( index, 1 );
        }
    };


    HtmlNodeLayer = Class.extend({
        ClassName: "HtmlNodeLayer",

        Z_INDEX : 1000,

        init: function( spec ) {

            this.map_ = spec.map || null;
            this.xAttr_ = spec.xAttr || null;
            this.yAttr_ = spec.yAttr || null;
            this.idKey_=  spec.idKey || null;
            this.propagate = spec.propagate !== undefined ? spec.propagate : true;
            this.nodes_ = [];
            this.nodesById_ = {};
            this.layers_ = [];
            this.subset_ = [];
            createLayerRoot( this );
        },


        /**
         * Returns the root DOM element for the layer.
         */
        getRootElement: function() {

            return this.$root_;
        },

        /**
         * Adds an HtmlLayer object to the list of node layer representations.
         */
        addLayer : function( layer ) {

            this.layers_.push( layer );
        },


        /**
         * Removes an HtmlLayer object to the list of node layer representations.
         */
        removeLayer : function( layer ) {

            var layers = this.layers_,
                index = layers.indexOf( layer );
            if ( index !== -1 ) {
                layers.splice( layer );
            }
        },


        /**
         * Re-allocates the data for all nodes of the entire layer. All nodes for data that are not
         * currently represented are created, all defunct nodes for now missing data are removed. Produces
         * a subset for all new nodes.
         */
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
                // use existing id's, not the ids INSIDE the nodes, as these may be intentionally
                // changed to force a redraw
                for (key in nodesById) {
                    if (nodesById.hasOwnProperty( key )) {
                        defunctNodesById[ key ] = true;
                    }
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

                        removeNodeById( that, key );
                    }
                }

                // create nodes for new data
                for (i=0; i<newData.length; i++) {
                    node = createNode( that, newData[i] );
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
                    defunctNodesArray.push( getNodeByData( that, nodes[i].data ) );
                }

                // only root will execute the following code
                for (i=0; i<data.length; i++) {

                    if ( doesNodeExist( that, [i] ) ) {
                        // remove from tracking list
                        index = defunctNodesArray.indexOf(  getNodeByData( that, data[i] ) );
                        defunctNodesArray.splice(index, 1);
                    } else {
                        // new data
                        newData.push( data[i] );
                    }
                }

                // destroy and remove all remaining nodes
                for (i=0; i<defunctNodesArray.length; i++) {
                    // remove from array
                    removeNode( that, defunctNodesArray[i] );
                }

                // create nodes for new data
                for (i=0; i<newData.length; i++) {
                    node = createNode( that, newData[i] );
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


        /**
         * Adds new data to the layer. Produces a subset for all new nodes.
         */
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
                        node = createNode( that, data[i] );
                        nodes.push( node );
                        newNodes.push( node );
                        nodesById[ key ] = node;
                    }
                }
            }

            function joinNoKey() {

                for (i=0; i<data.length; i++) {

                    if ( doesNodeExist( that, data[i] ) ) {
                        node = createNode( that, data[i] );
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


        /**
         * Removes data from the layer. Produces a subset for all remaining nodes.
         */
        remove : function( data ) {

            var that = this,
                idKey = this.idKey_,
                key,
                i;

            // remove by data
            function removeByData() {
                for (i=0; i<data.length; i++) {
                    removeNode( that, getNodeByData( that, data[i] ) );
                }
            }

            // remove by data, by id
            function removeByDataId() {

                for (i=0; i<data.length; i++) {
                    key = data[i][ idKey ];
                    removeNodeById( that, key );
                }

            }

            // remove by ids
            function removeById() {

                for (i=0; i<data.length; i++) {
                    key = data[i];
                    removeNodeById( that, key );
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

            this.subset_ = this.nodes_;
            return this;
        },


        /**
         * Removes all data from the layer.
         */
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


        /**
         * Produces a subset depending on the evaluation criteria provided. This may be
         * node id's, node data objects, or functions evaluating on nodes.
         */
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
                    node = getNodeByData( that, idEval[i] );
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


        /**
         * Renders all nodes in the current subset. Removes and recreates any nodes that currently
         * exist in DOM.
         */
        redraw: function() {
            var subset = this.subset_,
                layers = this.layers_,
                i;

            // remove any prior elements, do this here rather than
            // inside HtmlLayer in case multiple layers are attached
            for (i=0; i<subset.length; i++) {
                if ( subset[i].$elements ) {
                    subset[i].$elements.remove();
                }
            }

            // redraw layers
            for (i=0; i<layers.length; i++) {
                layers[i].redraw( subset );
            }
        }


    });

    return HtmlNodeLayer;
});
