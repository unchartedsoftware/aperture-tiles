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
var TagMap = function() {
    var _tagMapState = {
        nodeLayer : null,
        labelLayer : null,
        map : null, // Create the map in the DOM
        rangeY : new aperture.Scalar('Values', [0,75]),
        position : [-2,-1, 0, 1, 2],
        positionOffset : {},
        tileMap : {},
        spacer : 20,
        minFontSize : 10,
        maxFontSize : 45,
        selectedTileId : null,
        btnOffset : 0.23,
        dotOffset : 0.20,
        tileSize : 200,
        showSecretSauce : true //Set to TRUE to see hashtag labels. False to see the dot placeholder.
    };

    var _appTypes = {
        'WORDS' : 'words',
        'TREND' : 'trend',
        "STACKEDBAR" : 'stackedbar',
        'SLOPE' : 'slope'
    };
    var _appState = {
        DEFAULT_INDEX : 0,
        syncIndex : 0,
        syncAllTiles : false,
        appIndexMap : {},
        appList : [_appTypes.WORDS, _appTypes.TREND, _appTypes.STACKEDBAR, _appTypes.SLOPE],
        appOpacity : 0.85,
        storeUrl : 'img/tileApps/',
        setCount : 5
    };

    /******************************
     * State update methods
     ******************************/
    var updateSelectedTile = function(tileId){
        _tagMapState.selectedTileId = tileId;
    };

    var updateMap = function (event) {
        var bounds = _tagMapState.map.olMap_.getExtent();

        var proj900913 = new OpenLayers.Projection("EPSG:900913");
        var projWGS84 = new OpenLayers.Projection("EPSG:4326");

        var bottomLeft = new OpenLayers.Pixel(bounds.left, bounds.bottom);
        var topRight = new OpenLayers.Pixel(bounds.right, bounds.top);

        var pointBL = new OpenLayers.LonLat(bottomLeft.y, bottomLeft.x);
        var pointTR = new OpenLayers.LonLat(topRight.y, topRight.x);

        pointBL.transform(proj900913, projWGS84);
        pointTR.transform(proj900913, projWGS84);

        // instead do this when the bounds or zoom level changes:
        aperture.io.rest(
            '/jsonTile',
            'POST',
            dataProcessor,
            {
                postData: {
                    "layer": "top10twitterHashtags", // this is what I renamed the directory of tiles Jesse gave me.
                    "level": _tagMapState.map.getZoom(),     // Zoom level
                    "bounds": {
                        "xmin": pointBL.lon,//bounds.left, // fill in from olMap values of current view
                        "ymin": pointBL.lat,//bounds.bottom,
                        "xmax": pointTR.lon,//bounds.right,
                        "ymax": pointTR.lat//bounds.top
                    }
                },
                contentType: 'application/json'
            }
        );
    };

    /******************************
     * Utility methods
     ******************************/
    var getMapTile = function(lonLat){
        // Get the first Grid-based layer.
        var layerList = _tagMapState.map.olMap_.getLayersByClass('OpenLayers.Layer.TMS');
        if (layerList.length > 0){
            var gridLayer = layerList[0];
            for (var row=0; row < gridLayer.grid.length; row++){
                var gridRow = gridLayer.grid[row];
                for (var col=0; col < gridRow.length; col++){
                    var tile = gridRow[col];
                    if (tile.bounds.containsLonLat(lonLat)){
                        // TODO: Add flag to disable listeners when zooming/panning.
                        // This is a quick hack.
                        if (tile.url == null){
                            // The tile hasn't loaded yet, ignore this event.
                            return null;
                        }
                        var src = tile.url;
                        // Parse out the Level/X/Y information.
                        var nameIndex = src.indexOf(gridLayer.layername)+gridLayer.layername.length+1;
                        var fullPath = src.substring(nameIndex);
                        // Now remove the image extension.
                        var extIndex = fullPath.lastIndexOf('.');
                        var tilePath = fullPath.substring(0,extIndex);
                        var tokens = tilePath.split('/');

                        var imageTile = $(".olTileImage[src$='" + src + "']");
                        // TODO: Add flag to disable listeners when zooming/panning.
                        // This is a quick hack.
                        if (imageTile.length == 0 || imageTile.position() == null){
                            return null;
                        }
                        var position = imageTile.position();
                        return {
                            id : tilePath,
                            index : {
                                level : tokens[0],
                                x : tokens[1],
                                y : tokens[2]
                            },
                            tile : tile
                        };
                    }
                }
            }
        }
        return null;
    };

    var postProcessTile = function(data){
        data.icons = [
            {
                "id": "nav",
                "x" : -_tagMapState.btnOffset,
                "y" : 0,
                "url" : "img/chevron_L.png"
            },
            {
                "id": "nav",
                "x" : _tagMapState.btnOffset,
                "y" : 0,
                "url" : "img/chevron_R.png"
            }
        ];
        data.dotSpecs = [
            {
                "id": _appTypes.WORDS,
                "x" : -0.045,
                "y" : _tagMapState.dotOffset,
                "icons" : [
                    {
                        url : "img/no_select.png"
                    },
                    {
                        url : "img/select.png"
                    }
                ]
            },
            {
                "id": _appTypes.TREND,
                "x" : -0.015,
                "y" : _tagMapState.dotOffset,
                "icons" : [
                    {
                        url : "img/no_select.png"
                    },
                    {
                        url : "img/select.png"
                    }
                ]
            },
            {
                "id": _appTypes.STACKEDBAR,
                "x" : 0.015,
                "y" : _tagMapState.dotOffset,
                "icons" : [
                    {
                        url : "img/no_select.png"
                    },
                    {
                        url : "img/select.png"
                    }
                ]
            },
            {
                "id": _appTypes.SLOPE,
                "x" : 0.045,
                "y" : _tagMapState.dotOffset,
                "icons" : [
                    {
                        url : "img/no_select.png"
                    },
                    {
                        url : "img/select.png"
                    }
                ]
            }
        ];
    };

    var processTileBatch = function(jsonData){
        // Clear previous entries;
        _tagMapState.tileMap = {};
        for (var i=0; i < jsonData.length; i++){
            var data = jsonData[i];
            // Cache the locations of the tiles.
            var level = data.index.level;
            var x = data.index.x;
            var y = data.index.y;
            if (_tagMapState.tileMap[level] == null){
                _tagMapState.tileMap[level] = {};
            }
            if (_tagMapState.tileMap[level][x] == null){
                _tagMapState.tileMap[level][x] = {};
            }
            if (_tagMapState.tileMap[level][x][y] == null){
                _tagMapState.tileMap[level][x][y] = {};
            }
            _tagMapState.tileMap[level][x][y] = data;

            // Process hashtag values.
            var hashtags = jsonData[i].hashtags;
            for (var j=0; j < hashtags.length; j++){
                _tagMapState.rangeY.expand(hashtags[j].value);
            }

            // Init the app index state.
            var appSpec = _appState.appIndexMap[data.id];
            if (appSpec == null){
                appSpec = {
                    index : _appState.syncAllTiles?_appState.syncIndex:_appState.DEFAULT_INDEX,
                    set : getRandomAppSet(_appState.DEFAULT_INDEX)
                };
            }
            // If tile syncing is enabled, make sure existing tiles are switched over.
            else if (_appState.syncAllTiles && appSpec.index != _appState.syncIndex){
                appSpec.index = _appState.syncIndex;
            }
            _appState.appIndexMap[data.id] = appSpec;
            postProcessTile(data);
        }
    };

    var syncTilesToIndex = function(appIndex, syncAllLevels){
        var currentLevel = _tagMapState.map.getZoom();
        // Update all tiles from this level with the current app.
        for (var tileId in _appState.appIndexMap){
            var tokens = tileId.split('/');
            var level = tokens[0];
            if (currentLevel == level || syncAllLevels){
                _appState.appIndexMap[tileId].index = appIndex;
            }
        }
        _appState.syncIndex = appIndex;
    };

    var getRandomAppSet = function(appType){
        return Math.ceil(Math.random()*_appState.setCount);
    };
    /******************************
     * Listeners and callback methods.
     ******************************/
    var onMove = function(event){
        var parentOffset = $(this).parent().offset();
        var relX = event.pageX - parentOffset.left;
        var relY = event.pageY - parentOffset.top;

        var lonLat = _tagMapState.map.olMap_.getLonLatFromViewPortPx(new OpenLayers.Pixel(relX, relY));
        var tileInfo = getMapTile(lonLat);
        if (tileInfo != null && _tagMapState.selectedTileId != tileInfo.id){
            updateSelectedTile(tileInfo.id);
            var index = tileInfo.index;
            if (_tagMapState.tileMap[index.level]
                &&_tagMapState.tileMap[index.level][index.x]){
                var data = _tagMapState.tileMap[index.level][index.x][index.y];
                if (data){
                    // Trigger an update to redraw our visuals
                    // with the new tile app config.
                    _tagMapState.map.all().redraw();
                }
            }
        }
    };

    var onAppChange = function(event){
        // Need to figure out if this was the left or right side button.
        var lonLat = new OpenLayers.LonLat(event.data.longitude, event.data.latitude);
        // Project lonlat into map coordinates.
        lonLat = lonLat.transform(new OpenLayers.Projection("EPSG:4326"), _tagMapState.map.olMap_.projection);
        // New re-project to get pixel location.
        var pixelPt = _tagMapState.map.olMap_.getViewPortPxFromLonLat(lonLat);

        // Get the current app index for the selected tile.
        var nextAppIndex = _appState.appIndexMap[_tagMapState.selectedTileId].index;
        if (pixelPt.x < event.source.clientX){
            nextAppIndex++;
            if (nextAppIndex > _appState.appList.length-1){
                nextAppIndex = 0;
            }
        }
        else{
            nextAppIndex--;
            if (nextAppIndex < 0){
                nextAppIndex = _appState.appList.length-1;
            }
        }
        if (_appState.syncAllTiles){
            syncTilesToIndex(nextAppIndex);
        }
        else {
            var appSet = getRandomAppSet(_appState.appList[nextAppIndex]);
            _appState.appIndexMap[_tagMapState.selectedTileId] = {
                index : nextAppIndex,
                set : appSet
            };
        }
        _tagMapState.map.all().redraw();
    };

    var onAppSyncChange = function(source){
        _appState.syncAllTiles = $('input[name="appSync"]:checked').val();
    };

    var onResetView = function(source){
        syncTilesToIndex(_appState.DEFAULT_INDEX, true);
        _tagMapState.map.all().redraw();
    };
    var dataProcessor = function(response){

        var jsonData = response.json;
        _tagMapState.nodeLayer.all( jsonData );

        processTileBatch(jsonData);
        if (_tagMapState.showSecretSauce){
            _tagMapState.mapKey = _tagMapState.rangeY.mapKey([_tagMapState.minFontSize, _tagMapState.maxFontSize]);
            _tagMapState.labelLayer.map('font-size').from('hashtags[].value').using(_tagMapState.mapKey);
        }
        _tagMapState.map.all().redraw();
    };

    /******************************
     * Configuration methods.
     ******************************/
    var setupCarousel = function(){
        // Clear existing listener.
        $('#map').off('mousemove', onMove);
        $('#map').on('mousemove', onMove);
        // Create layer for showing carousel controls.
        var navLayer = _tagMapState.nodeLayer.addLayer(aperture.IconLayer, {
            'width'    : 15,
            'height'   : 42,
            'anchor-x' : 0.5,
            'anchor-y' : 0.5
        });

        navLayer.map('x').from(function(index){
            return this.icons[index].x/Math.pow(2,_tagMapState.map.getZoom()-1);
        });
        navLayer.map('y').from(function(index){
            return this.icons[index].y/Math.pow(2,_tagMapState.map.getZoom()-1);
        });
        navLayer.map('visible').from(function(index){
            return _tagMapState.selectedTileId == this.id;
        });

        navLayer.map('icon-count').from('icons.length');
        navLayer.map('url').from('icons[].url');
        navLayer.off('click', onAppChange);
        navLayer.on('click', onAppChange);

        constructAppLayer();
        constructIndexLayer();

        // Add listener for synchronizing app states
        // acroos all visible tiles.
        $('input[name="appSync"]').off('change', onAppSyncChange);
        $('input[name="appSync"]').on('change', onAppSyncChange);

        $('#resetViewBtn').off('click', onResetView);
        $('#resetViewBtn').on('click', onResetView);
    };

    var constructAppLayer = function(){
        // Create layer for showing carousel controls.
        var appLayer = _tagMapState.nodeLayer.addLayer(aperture.IconLayer, {
            'width'    : _tagMapState.tileSize,
            'height'   : _tagMapState.tileSize,
            'anchor-x' : 0.5,
            'anchor-y' : 0.5
        });

        appLayer.map('visible').from(function(index){
            // Check if this tile has a previous state.
            var prevAppIndex = _appState.appIndexMap[this.id].index;
            // If the tile hasn't been visited yet or is on WORDS,
            // we don't want to show anything.
            return (prevAppIndex != _appState.DEFAULT_INDEX);
        });

        appLayer.map('icon-count').asValue(1);
        appLayer.map('url').from(function(index){
            var appSpec = _appState.appIndexMap[this.id];
            var appType = _appState.appList[appSpec.index];
            return _appState.storeUrl + appSpec.set + '/' + appType + '.png'
        });
        appLayer.map('opacity').asValue(_appState.appOpacity);
    };

    var constructIndexLayer = function(){
        // Create layer for showing carousel controls.
        var indexLayer = _tagMapState.nodeLayer.addLayer(aperture.IconLayer, {
            'width'    : 12,
            'height'   : 12,
            'anchor-x' : 0.5,
            'anchor-y' : 0.5
        });

        indexLayer.map('x').from(function(index){
            return this.dotSpecs[index].x/Math.pow(2,_tagMapState.map.getZoom()-1);
        });
        indexLayer.map('y').from(function(index){
            return this.dotSpecs[index].y/Math.pow(2,_tagMapState.map.getZoom()-1);
        });
        indexLayer.map('visible').from(function(index){
            return (this.id == _tagMapState.selectedTileId);
            // Check if this tile has a previous state.
            var prevAppIndex = _appState.appIndexMap[this.id].index;
            // If the tile hasn't been visited yet or is on WORDS,
            // we don't want to show anything.
            return (prevAppIndex != _appState.DEFAULT_INDEX);
        });

        indexLayer.map('icon-count').from('dotSpecs.length');
        indexLayer.map('url').from(function(index){
            var appIndex = _appState.appIndexMap[this.id].index;
            return this.dotSpecs[index].id != _appState.appList[appIndex]?this.dotSpecs[index].icons[0].url:this.dotSpecs[index].icons[1].url;
        });
        indexLayer.map('opacity').asValue(_appState.appOpacity);
    };
    var constructLabelLayer = function(){
        _tagMapState.labelLayer = _tagMapState.nodeLayer.addLayer(aperture.LabelLayer);
        _tagMapState.labelLayer.map('offset-y').from(function(index){
            var fontSize = _tagMapState.mapKey.map(this.hashtags[index].value);
            var id = this.longitude+'_'+this.latitude;
            _tagMapState.positionOffset[id] = fontSize;

            var offset = _tagMapState.spacer;
            return _tagMapState.position[index] * offset;
        });

        _tagMapState.labelLayer.map('text').from('hashtags[].text');
        _tagMapState.labelLayer.map('label-count').from('hashtags.length');
        _tagMapState.labelLayer.map('fill').asValue('#FFF');
        _tagMapState.labelLayer.map('font-outline').asValue('#222');
        _tagMapState.labelLayer.map('font-outline-width').asValue(3);
        _tagMapState.labelLayer.map('visible').from(function(index){
            // Check if this tile has a previous state.
            var prevAppIndex = _appState.appIndexMap[this.id].index;
            return (prevAppIndex == _appState.DEFAULT_INDEX);
        });
    };

    var initTagLayer = function(){
        // Create a location layer
        _tagMapState.nodeLayer = _tagMapState.map.addLayer( aperture.geo.MapNodeLayer );
        _tagMapState.nodeLayer.map('latitude').from('latitude');
        _tagMapState.nodeLayer.map('longitude').from('longitude');
        _tagMapState.nodeLayer.map('visible').asValue(true);

        if (_tagMapState.showSecretSauce){
            constructLabelLayer();
        }
        else {
            var dotLayer = _tagMapState.nodeLayer.addLayer( aperture.RadialLayer );
            dotLayer.map('stroke').asValue('#222');
            dotLayer.map('radius').asValue(10);
            dotLayer.map('opacity').asValue(0.5);
            dotLayer.map('stroke-width').asValue(1.5);
            dotLayer.map('fill').asValue('#EE8C0F');
        }

        // Clear any existing listeners.
        _tagMapState.map.off('zoom',updateMap);
        _tagMapState.map.off('panend', updateMap);
        _tagMapState.map.on('zoom',updateMap);
        _tagMapState.map.on('panend', updateMap);
        // Setup the hover listener for the carousel.
        setupCarousel();

        //Zoom to the area of the world with the data.
        _tagMapState.map.zoomTo( 20, 25, 2 );
        // Trigger the initial map update
        // to render ApertureJS visuals.
        updateMap(null, null);
    };
    var loadJson = function(jsonFile){
        $.getJSON(jsonFile , function(data) {
            var plots = data.Plots;
            for (var i=0; i < plots.length; i++){
                var plotInfo = plots[i];
                var options = {
                    layerList : plotInfo.Layers,
                    //goTo : {"x":128, "y":128, "zoom":2},
                    goTo : {x: 0, y: 0, zoom:2},
                    hasBackgroundToggle : false,
                    hideAxis : true,
                    debug : false,
                    components : {
                        map : {
                            "divId" : "map",
                            "width" : plots['Display Width'],
                            "height" : plots['Display Height'],
                            "layers" : plotInfo.Layers,
                            "baseLayer" : {
                                "opacity" : plotInfo.baseLayer?plotInfo.baseLayer.Opacity:1
                            }
                        }
                    }
                };

                // Read in the OpenLayers baselayer info.
                if (plotInfo.baseLayer){
                    options.baseLayer = plotInfo.baseLayer;
                }
                var mapPlot = new XDataMap(options);
                mapPlot.start(function(canvas){
                    _tagMapState.map = canvas;
                    initTagLayer();
                });
            }
        });
    };

    return {
        start : function(jsonData){
            //load data
            loadJson(jsonData);
        }
    };
};
/*
 var clipBounds = function(index){
 var x = locations.valueFor('longitude', this, 0, index);
 var y = locations.valueFor('latitude', this, 0, index);
 var bounds = _tagMapState.map.olMap_.getExtent();

 var minPt = new OpenLayers.Geometry.Point(bounds.left, bounds.bottom);
 minPt = minPt.transform(new OpenLayers.Projection("EPSG:900913"), new OpenLayers.Projection("EPSG:4326"));

 var maxPt = new OpenLayers.Geometry.Point(bounds.right, bounds.top);
 maxPt = maxPt.transform(new OpenLayers.Projection("EPSG:900913"), new OpenLayers.Projection("EPSG:4326"));

 var projPt = new OpenLayers.Geometry.Point(x, y);
 projPt = projPt.transform(new OpenLayers.Projection("EPSG:4326"), new OpenLayers.Projection("EPSG:900913"));

 // Add world bounds from meta data
 var visible = bounds.containsLonLat(new OpenLayers.LonLat(projPt.x, projPt.y));

 console.log(visible + ' x:' + x + ' minX: ' + minPt.x + ' maxX: ' + maxPt.x);
 return visible;
 };
 */