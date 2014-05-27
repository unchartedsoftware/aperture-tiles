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
        },


        removeLayer : function( layer ) {

            // remove layer
            var index = this.layers_.indexOf( layer );
            if ( index !== -1 ) {
                this.layers_.nodeLayer_ = null;
                this.layers_.splice( layer );
            }
        },


        addLayer : function( layer ) {

            // add layer
            layer.nodeLayer_ = this;
            this.layers_.push( layer );
        },


        createLayerRoot : function() {
            var pos = this.map_.getViewportPixelFromMapPixel( 0, this.map_.getMapHeight() );
            return $('<div class="aperture-client-layer" style="position:absolute; left:'+pos.x+'px; top:' +pos.y+ 'px; width=0px; height=0px"></div>');
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

            var i,
                key,
                index,
                node,
                defunctNodesById = {},
                defunctNodesArray = [],
                newData = [],
                newNodes = [],
                exists;

            // keep list of current nodes, to track which ones are not in the new set
            for (i=0; i<this.nodes_.length; ++i) {
                if (this.idKey_) {
                    key = this.nodes_[i].data[this.idKey_];
                    defunctNodesById[ key ] = true;
                } else {
                    defunctNodesArray.push( this.findNodeFromData( this.nodes_[i].data ) );
                }
            }

            // only root will execute the following code
            for (i=0; i<data.length; i++) {

                if (this.idKey_) {
                    // if id attribute is specified, use that to check duplicates
                    key = data[i][this.idKey_];
                    exists = this.nodesById_[key] !== undefined;
                } else {
                    // otherwise test object reference
                    exists = this.doesNodeExist( data[i] );
                }

                if ( exists ) {
                    // remove from tracking list
                    if (this.idKey_) {
                        delete defunctNodesById[ key ];
                    } else {
                        index = defunctNodesArray.indexOf(  this.findNodeFromData( data[i] ) );
                        defunctNodesArray.splice(index, 1);
                    }
                } else {
                    // new data
                    newData.push(data[i]);
                }
            }

            // destroy and remove all remaining nodes
            if (this.idKey_) {
                // id is specified
                for (key in defunctNodesById) {
                    if (defunctNodesById.hasOwnProperty(key)) {
                        // remove from array
                        index = this.nodes_.indexOf( this.nodesById_[key] );
                        this.nodes_.splice(index, 1);
                        // destroy and delete from map
                        this.destroyNode( this.nodesById_[key] );
                        delete this.nodesById_[key];
                    }
                }
            } else {
                // no id specified
                for (i=0; i<defunctNodesArray.length; i++) {
                    // remove from array
                    index = this.nodes_.indexOf( defunctNodesArray[i] );
                    this.destroyNode( this.nodes_[index] );
                    this.nodes_.splice(index, 1);
                }
            }

            // create nodes for new data
            for (i=0; i<newData.length; i++) {
                node = this.createNode( newData[i] );
                this.nodes_.push( node );
                newNodes.push( node );
                if (this.idKey_) {
                    key = newData[i][this.idKey_];
                    this.nodesById_[key] = node;
                }
            }
            this.subSet = this.nodes_;
            return this;
        },


        join : function( data ) {

            var i,
                key,
                node,
                newNodes = [],
                exists;

            // only root will execute the following code
            for (i=0; i<data.length; i++) {

                if (this.idKey_) {
                    // if id attribute is specified, use that to check duplicates
                    key = data[i][this.idKey_];
                    exists = this.nodesById_[key] !== undefined;
                } else {
                    // otherwise test object reference
                    exists = this.doesNodeExist( data[i] );
                }

                if ( !exists ) {

                    node = this.createNode( data[i] );
                    this.nodes_.push( node );
                    newNodes.push( node );
                    if (this.idKey_) {
                        this.nodesById_[key] = node;
                    }
                }
            }
            this.subSet = this.nodes_;
            return this;
        },


        where: function( idEval ) {

            var node,
                i;

            this.subSet_ = [];

            if ( $.isFunction( idEval ) ) {
                // eval by function
                for (i=0; i<this.nodes_.length; i++) {
                    if ( idEval( this.nodes_.data ) ) {
                        this.subSet_.push( this.nodes_ );
                    }
                }

            } else if ($.isPlainObject( idEval )) {
                // eval by data match
                node = this.findNodeFromData( idEval );
                if (node) {
                    this.subSet_.push( node );
                }

            } else {

                // eval by id
                if (this.idKey_) {
                    if ( $.isArray( idEval ) ) {

                        for (i=0; i<idEval.length; i++) {

                            node = this.nodesById_[ idEval[i] ];
                            if (node) {
                                this.subSet_.push( node );
                            }
                        }
                    } else {
                        node = this.nodesById_[ idEval ];
                        if (node) {
                            this.subSet_.push( node );
                        }
                    }
                }
            }
            return this;
        },


        findNodeFromData: function(data) {

            var i;
            for (i=0; i<this.nodes_.length; i++) {
                if ( this.nodes_[i].data === data ) {
                    return this.nodes_[i];
                }
            }
            return null;
        },


        doesNodeExist: function(data) {

            var i;
            for (i=0; i<this.nodes_.length; i++) {
                if ( this.nodes_[i].data === data ) {
                    return true;
                }
            }
            return false;
        },


        redraw: function() {
            var i;

            for (i=0; i<this.subSet_.length; i++) {
                this.subSet_[i].$root.empty();
            }

            for (i=0; i<this.layers_.length; i++) {
                this.layers_[i].redraw( this.subSet_ );
            }
        }


    });

    return ClientNodeLayer;
});
