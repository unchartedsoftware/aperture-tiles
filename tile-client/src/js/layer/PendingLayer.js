( function() {

    'use strict';

    var Layer = require('./Layer'),
        LayerUtil = require('./LayerUtil'),
        PendingTile = require('./PendingTile'),
        HtmlTileLayer = require('./HtmlTileLayer'),
        PubSub = require('../util/PubSub'),
        DEBOUNCE_DELAY = 400,
        _animations = {},
        _counts = {},
        _prevTilekeys = {};

    function getTileHash( tile ) {
        return LayerUtil.getTilekey( tile.layer, tile.bounds ).replace( /,/g, "-" );
    }

    function clearPendingTile( tilekey ) {
        delete _counts[ tilekey ];
        clearTimeout( _animations[ tilekey ] );
        _animations[ tilekey ] = setTimeout( function() {
            $( '.olTilePending_' + tilekey ).css('opacity', 0);
            delete _animations[ tilekey ];
        }, DEBOUNCE_DELAY );
    }

    function flagPendingTile( tilekey ) {
        _counts[ tilekey ] = 0;
        if ( _animations[ tilekey ] ) {
            clearTimeout( _animations[ tilekey ] );
            delete _animations[ tilekey ];
        }
        $( '.olTilePending_' + tilekey ).css('opacity', 1);
    }

    function incrementRequestCount( tilekey, tileId ) {
        if ( !_counts[ tilekey ] ) {
            flagPendingTile( tilekey );
        }
        _counts[ tilekey ]++;
        _prevTilekeys[ tileId ] = tilekey;
    }

    function decrementRequestCount( tilekey, tileId ) {
        if ( !_counts[ tilekey ] ) {
            // this can occur if an 'unload' event occurs while pending
            return;
        }
        _counts[ tilekey ]--;
        if ( _counts[ tilekey ] === 0 ) {
            clearPendingTile( tilekey );
        }
        _prevTilekeys[ tileId ] = null;
    }

    function reloadTile( newKey, tileId ) {
        var oldKey = _prevTilekeys[ tileId ];
        decrementRequestCount( oldKey, tileId );
        incrementRequestCount( newKey, tileId );
    }

    function trackTiles( layer ) {
        return function( message ) {
            if ( layer.olLayer && message.field === "activate" ) {
                layer.olLayer.events.register( 'addtile', layer.olLayer, function( data ) {
                    // Add listeners to tile
                    data.tile.events.register( 'loadstart', data.tile, function( arg ) {
                        incrementRequestCount( getTileHash( arg.object ), arg.object.id );
                    });
                    data.tile.events.register( 'loadend', data.tile, function( arg ) {
                        decrementRequestCount( getTileHash( arg.object ), arg.object.id );
                    });
                    data.tile.events.register( 'loaderror', data.tile, function( arg ) {
                        decrementRequestCount( getTileHash( arg.object ), arg.object.id );
                    });
                    data.tile.events.register( 'reload', data.tile, function( arg ) {
                        reloadTile( getTileHash( arg.object ), arg.object.id );
                    });
                    data.tile.events.register( 'unload', data.tile, function( arg ) {
                        var tileId = arg.object.id;
                        decrementRequestCount( _prevTilekeys[ tileId ], tileId );
                    });
                });
            }
        };
    }

    function PendingLayer( spec ) {
        Layer.call( this, spec );
        this.zIndex = 9999;
        this.domain = "pending";
        this.source = {};
        this.tileClass = PendingTile;
    }

    PendingLayer.prototype = Object.create( Layer.prototype );

    PendingLayer.prototype.activate = function() {
        // add the new layer
        this.olLayer = new HtmlTileLayer(
            'Pending Tile Layer',
            this.source.tms,
            {
                layername: this.source.id,
                type: 'json',
                maxExtent: new OpenLayers.Bounds(
                    -20037500, -20037500,
                    20037500,  20037500),
                isBaseLayer: false,
                getURL: null,
                tileClass: this.tileClass
            });
        // set whether it is enabled or not before attaching, to prevent
        // needless tile reqeests
        this.setEnabled( this.isEnabled() );
        // publish activate event before appending to map
        PubSub.publish( this.getChannel(), { field: 'activate', value: true } );
        // attach to map
        this.map.olMap.addLayer( this.olLayer );
        // set z-index after
        this.setZIndex( this.zIndex );
        // publish add event
        PubSub.publish( this.getChannel(), { field: 'add', value: true } );
    };

    PendingLayer.prototype.deactivate = function() {
        if ( this.olLayer ) {
            this.map.olMap.removeLayer( this.olLayer );
            PubSub.publish( this.getChannel(), { field: 'remove', value: true } );
            this.olLayer.destroy();
            this.olLayer = null;
        }
        PubSub.publish( this.getChannel(), { field: 'deactivate', value: true } );
    };

    PendingLayer.prototype.setZIndex = function ( zIndex ) {
        // we by-pass the OpenLayers.Map.setLayerIndex() method and manually
        // set the z-index of the layer dev. setLayerIndex sets a relative
        // index based on current map layers, which then sets a z-index. This
        // caused issues with async layer loading.
        this.zIndex = zIndex;
        if ( this.olLayer ) {
            $( this.olLayer.div ).css( 'z-index', zIndex );
        }
        PubSub.publish( this.getChannel(), { field: 'zIndex', value: zIndex });
    };

    PendingLayer.prototype.getZIndex = function () {
        return this.zIndex;
    };

    PendingLayer.prototype.register = function( layer ) {
        layer.pendingFunc = trackTiles( layer );
        PubSub.subscribe( layer.getChannel(), layer.pendingFunc );
    };

    PendingLayer.prototype.unregister = function( layer ) {
        PubSub.unsubscribe( layer.getChannel(), layer.pendingFunc );
        delete layer.pendingFunc;
    };

    module.exports = PendingLayer;

}());
