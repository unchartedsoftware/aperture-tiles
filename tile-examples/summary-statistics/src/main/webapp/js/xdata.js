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
  define( function(require) {
    "use strict"

    return function (options) {
        var Y_TILE_FUNC_PASSTHROUGH = function(yValue){return yValue;}; // NO-OP
        var Y_TILE_FUNC_ZERO_CLAMP = function (yInput){ return 0; }; // Y always zero, for density strips.

        var yFunction = Y_TILE_FUNC_PASSTHROUGH;

        var _mapState = {
            options : options,
            extents : null,
            legendRange: [0,100],
            baseLayerSliderOpacity: 100,
            overlayLayerSliderOpacity: 100,
            overlayList : [],
            baseLayerList : [],
            overlayInfoMap : {},
            layerVisible : {},
            loopCount : 0,
            defaultLook : "log10",
            defaultRamp : "ware",
            onStartComplete : null, // Callback for when start() has completed.
            isStartComplete : false,
            canvas : null     // Create a vizlet container
        };

        /**
         * Finds the min/max of for a given dimension of a bounding region.
         * @param layerBounds
         * @param maxBounds
         * @param index
         * @param comparator
         */
        var joinMapBounds = function(layerBounds, maxBounds, index, comparator){
            if (comparator == '>'){
                if (layerBounds[index] > maxBounds[index]){
                    maxBounds[index] = maxBounds[index];
                }
            }
            else if (layerBounds[index] < maxBounds[index]){
                maxBounds[index] = maxBounds[index];
            }
        };

        /**
         * Finds the total coverage area of two bounding regions.
         * @param layerBounds
         * @param maxBounds
         */
        var joinDataBounds = function(layerBounds, maxBounds){
            maxBounds.left = Math.min(layerBounds.left, maxBounds.left);
            maxBounds.bottom = Math.min(layerBounds.bottom, maxBounds.bottom);
            maxBounds.right = Math.max(layerBounds.right, maxBounds.right);
            maxBounds.top = Math.max(layerBounds.top, maxBounds.top);
        };

        /**
         * Calculate the total bounding region for a list of layers.
         * @param layerInfoMap
         * @returns {{dataBounds: null, mapBounds: null}}
         */
        var joinLayerInfo = function(layerInfoMap){
            // Iterate over all the layers and calculate
            // the union of the bounds.
            var maxDataBounds = null;
            var maxMapBounds = null;
            var maxZoom = 0; // Assume starting from level 0.
            var projection;
            var isFirst = true;

            for (var layerName in layerInfoMap){
                var layerInfo = layerInfoMap[layerName];
                if (isFirst){
                    maxDataBounds = layerInfo.dataBounds;
                    maxMapBounds = layerInfo.bounds;
                    // Assume all layers have the same projection.
                    projection = layerInfo.projection;
                    isFirst = false;
                }
                else {
                    for (var i=0; i < layerInfo.bounds.length; i ++){
                        joinMapBounds(layerInfo.bounds, maxMapBounds, i, (i<2)?'<':'>');
                    }
                    joinDataBounds(layerInfo.dataBounds, maxDataBounds);
                }
                if (layerInfo.maxzoom > maxZoom){
                    maxZoom = layerInfo.maxzoom;
                }
            }
            return {
                dataBounds : maxDataBounds,
                mapBounds : maxMapBounds,
                numZoomLevels : maxZoom + 1
            }
        };

        var getLayerSpec = function(layerList, layerName){
            for (var i=0; i < layerList.length; i++){
                if (layerList[i].Layer === layerName){
                    return layerList[i];
                }
            }
            return null;
        };

        var baseLayerSwitch = function(evt){
            var baseLayerIndex = $('input[name="base"]:checked').val();
            var baseLayerRef = _mapState.baseLayerList[baseLayerIndex];
            _mapState.canvas.olMap_.setBaseLayer(baseLayerRef);
            _mapState.canvas.all().redraw();
        };

        /**
         * Create a control for toggling the visibility
         * of all layers on the map.
         */
        var createLayerControl = function(){
            var toc = $('#layer-control');
            toc.empty(); // Clear out any existing controls.
            for (var i=0; i < _mapState.overlayList.length; i++){
                var overlayLayer = _mapState.overlayList[i];

                var isChecked = _mapState.layerVisible[overlayLayer.spec['Layer']];

                var toggle = $('<input/>').attr({
                    type : 'checkbox',
                    value : i
                });
                if (isChecked){
                    toggle.attr('checked', 'checked');
                }
                $(toggle).change(function(){
                    var overlayLayer = _mapState.overlayList[this.value];
                    var isChecked = $(this).is(':checked');
                    if (overlayLayer.spec['Type'] == 'tile' || overlayLayer.spec['Type' == null]){
                        overlayLayer.layer.olLayer_.setVisibility(isChecked);
                        overlayLayer.layer.all().redraw();
                    }
                    else if (overlayLayer.spec['Type'] == 'json'){
                        overlayLayer.layer.map('visible').asValue(isChecked);
                        overlayLayer.layer.all().redraw();
                    }

                    _mapState.layerVisible[overlayLayer.spec['Layer']] = isChecked;
                    // Toggle legend visibility and update.
                    var visibleData = hasSingleLayerVisible();
                    updateLegend(visibleData.layerName);
                });
                toc.append(toggle);
                toc.append(' ' + overlayLayer.spec['Layer'] + ' <br> ');
            }

            if ( _mapState.baseLayerList.length > 0){
                toc.append('Base Layers:</br>');
                toc.append('<form action="">');
                for (var i=0; i < _mapState.baseLayerList.length; i++){
                    var baseLayer = _mapState.baseLayerList[i];
                    if (i==0){
                        toc.append('<input type="radio" name="base" checked="checked" value='+i+'>' + baseLayer.options.name + '<br>');
                    } else {
                        toc.append('<input type="radio" name="base" value='+i+'>' + baseLayer.name + '<br>');
                    }
                }
                toc.append('</form>');
                $('input[name="base"]').on('change', baseLayerSwitch);
            }
        };

        var getRemappedExtent = function() {

            var overlayLayerBounds = joinLayerInfo(_mapState.overlayInfoMap);
            var maxMapBounds = overlayLayerBounds.mapBounds;
            var maxDataBounds = overlayLayerBounds.dataBounds;

            var geoViewBounds = _mapState.canvas.olMap_.getExtent();
            var leftData = linearRemap(geoViewBounds.left, maxMapBounds[0], maxMapBounds[2], maxDataBounds.left, maxDataBounds.right);
            var rightData = linearRemap(geoViewBounds.right, maxMapBounds[0], maxMapBounds[2], maxDataBounds.left, maxDataBounds.right);
            var bottomData = linearRemap(geoViewBounds.bottom, maxMapBounds[1], maxMapBounds[3], maxDataBounds.bottom, maxDataBounds.top);
            var topData = linearRemap(geoViewBounds.top, maxMapBounds[1], maxMapBounds[3], maxDataBounds.bottom, maxDataBounds.top);

            return {
                left: leftData,
                right: rightData,
                bottom: bottomData,
                top: topData
            };
        },

        linearRemap = function(x, oMin, oMax, nMin, nMax){
            //range check
            if (oMin === oMax){
                return null;
            }

            if (nMin === nMax){
                return null;
            }

            //check reversed input range
            var reverseInput = false;
            var oldMin = Math.min( oMin, oMax );
            var oldMax = Math.max( oMin, oMax );
            if (oldMin !== oMin){
                reverseInput = true;
            }

            //check reversed output range
            var reverseOutput = false;
            var newMin = Math.min( nMin, nMax );
            var newMax = Math.max( nMin, nMax ) ;
            if (newMin !== nMin){
                reverseOutput = true;
            }

            var portion = (x-oldMin)*(newMax-newMin)/(oldMax-oldMin);
            if (reverseInput){
                portion = (oldMax-x)*(newMax-newMin)/(oldMax-oldMin)
            }

            var result = portion + newMin;
            if (reverseOutput){
                result = newMax - portion;
            }

            return result
        },

        initPlotBaseLayer = function(){
            var overlayLayerBounds = joinLayerInfo(_mapState.overlayInfoMap);

            if(!options.mapOptions){
                options.mapOptions = {};
            }

            options.mapOptions.maxExtent = overlayLayerBounds.mapBounds;
            options.mapOptions.numZoomLevels = overlayLayerBounds.numZoomLevels;
            options.mapOptions.projection = overlayLayerBounds.projection;
            options.mapOptions.units = "m";
            options.mapOptions.restricted = false;

            var mapSpec = {
                id: options.components.map.divId,
                options: options.mapOptions
            };

            // Use the first listed base layer as the default base layer
            if(options.baseLayer){
                mapSpec.baseLayer = options.baseLayer[0];
            }
            _mapState.canvas = new aperture.geo.Map(mapSpec);

            _mapState.canvas.olMap_.events.register('mousemove', _mapState.canvas.olMap_, function (e) {
                var xSpec = getAxisSpec('xaxis');
                var ySpec = getAxisSpec('yaxis');

                if (!xSpec || !ySpec)return;

                var xVal = PlotAxis.getTickMarkLocationForPixelLocation(xSpec, e.xy.x);
                var yVal = PlotAxis.getTickMarkLocationForPixelLocation(ySpec, e.xy.y);
                $('#'+_mapState.options.components.map.divId).prop('title', 'x: ' + xVal + ', y: ' + yVal);
            });

            _mapState.canvas.on('zoom', onZoom);
            _mapState.canvas.on('move', onPan);
            _mapState.canvas.on('panend', onPan);
            $('#'+options.components.map.divId).resize(onMapResize);

            // Assign the base layer properties if present.
            var baseLayerOptions = _mapState.options.components['map'].baseLayer;
            if (baseLayerOptions != null){
                if (baseLayerOptions.opacity != null){
                    _mapState.canvas.olMap_.baseLayer.setOpacity(baseLayerOptions.opacity);
                }
            }

            // Draw
            _mapState.canvas.all().redraw();
        },

        constructPlotLayer = function(layerSpec){
            // Check the layer type. We only want to construct
            // tile layers. If not type is present, default to
            // to type "tile".
            if (layerSpec['Type'] == 'tile' ||layerSpec['Type'] == null){
                var layerInfo = _mapState.overlayInfoMap[layerSpec['Layer']];
                var olBounds = new OpenLayers.Bounds.fromArray(layerInfo.bounds);

                // TODO: Just move the Y-value logic to the server side. Assume zero-Y-bounds means
                var overlayLayer = _mapState.canvas.addLayer(aperture.geo.MapTileLayer.TMS, {},
                    {
                        'name': "Aperture TMS",
                        'url': layerInfo.tms,
                        'options': {
                            'layername': layerInfo.layer,
                            'type': 'png',
                            'version': '1.0.0',
                            'maxExtent' : olBounds,
                            transparent: true,
                            getURL: function(bounds){
                                var res = this.map.getResolution();
                                var x = Math.round((bounds.left - this.maxExtent.left) / (res * this.tileSize.w));
                                var y = Math.round((bounds.bottom - this.maxExtent.bottom) / (res * this.tileSize.h));
                                var z = this.map.getZoom();

                                y = yFunction(y);
                                if (x >= 0 && y >= 0) {
                                    return this.url + this.version + "/" + this.layername + "/" + z + "/" + x + "/" + y + "." + this.type;
                                }
                            }
                        }
                    }
                );

                // Assign the layer opacity if there is one.
                // Does Aperture support this mapping?
                if (layerSpec && layerSpec.Opacity != null){
                    overlayLayer.olLayer_.setOpacity(layerSpec.Opacity);
                }
                _mapState.overlayList.push({
                    layer : overlayLayer,
                    spec : layerSpec
                });
                // Set store this as visible by default.
                _mapState.layerVisible[layerSpec['Layer']] = true;
            }
        },

        addDataOverlay = function (layerSpec, colourScaleType, colourRampType, legendRange, isFirstInit, callback) {

            var onNewLayerResource = function( layerInfo, statusInfo ) {

                if(!statusInfo.success){
                    console.log("DEBUG: Failed to configure tile service");
                    return;
                }
                console.log("DEBUG: Successfully configured tile service");
                var overlayLayerInfo = layerInfo;
                overlayLayerInfo.dataBounds = {
                    left:   layerInfo.bounds[0],
                    bottom: layerInfo.bounds[1],
                    right:  layerInfo.bounds[2],
                    top:    layerInfo.bounds[3],
                    getCenterX: function(){(this.right - this.left)/2},
                    getCenterY: function(){(this.top - this.bottom)/2}
                };
                // Cache the layer info.
                _mapState.overlayInfoMap[layerInfo.layer] = overlayLayerInfo;

                // HACK: Override!
                layerInfo.bounds = [-20037500,
                                    -20037500,
                                    20037500,
                                    20037500];
                layerInfo.projection = "EPSG:900913";

                if (callback){
                    callback();
                }
            };

            var postData = {
                         request: "configure",
                         layer: layerSpec['Layer'],
                         configuration: layerSpec['Config']
                     };

            console.log("DEBUG: Sending configuration request for layer '" + layerSpec['Layer'] + "'");

            aperture.io.rest(
                '/layer',
                'POST',
                onNewLayerResource,
                {
                     postData: {
                         request: "configure",
                         layer: layerSpec['Layer'],
                         configuration: layerSpec['Config']
                     },
                     contentType: 'application/json'
                }
            );

                /*
                {
                    postData: {
                        'transform': colourScaleType,
                        'ramp': colourRampType,
                        'layer': layerSpec['Layer'] ,
                        'legendRange': legendRange
                    },
                    contentType: 'application/json'
                });
                */
            
        },

        getAxisSpec = function(type){
            if(!_mapState.options.components[type] || _mapState.options.hideAxis){ // This axis not enabled.
                return;
            }

            var mapExtent = getRemappedExtent();
            //console.log("pixelBounds X range: " + map.olMap_.minPx.x + " to " + map.olMap_.maxPx.x);
            //console.log("pixelBounds Y range: " + map.olMap_.minPx.y + " to " + map.olMap_.maxPx.y);
            var tilesTotalPixelSpan = {
                x: 256*Math.pow(2,_mapState.canvas.olMap_.getZoom()),
                y: _mapState.canvas.olMap_.viewPortDiv.clientHeight
            };

            var overlayBounds = joinLayerInfo(_mapState.overlayInfoMap);
            var spec = {
                map : {
                    divId : _mapState.options.components.map.divId,
                    zoom : _mapState.canvas.olMap_.getZoom(),
                    pixelBounds : {
                        min : {
                            x: tilesTotalPixelSpan.x - _mapState.canvas.olMap_.maxPx.x,
                            y: tilesTotalPixelSpan.y - _mapState.canvas.olMap_.maxPx.y
                        },
                        max : {
                            x: tilesTotalPixelSpan.x - _mapState.canvas.olMap_.minPx.x,
                            y: tilesTotalPixelSpan.y - _mapState.canvas.olMap_.minPx.y
                        }
                    },
                    tileSize : type=='xaxis' ? _mapState.canvas.olMap_.getTileSize().w : _mapState.canvas.olMap_.getTileSize().h
                },
                layer : {
                    bounds : {
                        min : {
                            x : overlayBounds.dataBounds.left,
                            y : overlayBounds.dataBounds.bottom
                        },
                        max : {
                            x : overlayBounds.dataBounds.right,
                            y : overlayBounds.dataBounds.top
                        }
                    }
                },
                type : type,
                divId : _mapState.options.components[type].divId,
                options : _mapState.options.components[type],
                parentId : _mapState.options.components[type].parentId,
                title : {
                    offset : _mapState.options.components[type].titleOffset
                }
            };
            return spec;
        },
      

        generateZoomLevelSlider = function (parentId) {
            var overlayLayerBounds = joinLayerInfo(_mapState.overlayInfoMap);
            var mapZoom = _mapState.canvas.getZoom();
            var zoomSliderParent = $('#'+parentId);
            var uniqueSliderDivId = parentId + "-zoomLevelSlider";
            _mapState.options.components.zoomLevelSlider.zoomLevelSliderDivId = uniqueSliderDivId;
            var slider = $('<div id= "'+ uniqueSliderDivId +'" > <span style="position:absolute; left: -95px; top: -3px; width: 100px; font-size: 1.0em;">Zoom Level:</span> </div>');
            slider.addClass('plot-zoom-slider');
            var zoomControl = _mapState.canvas.olMap_.controls[1];
            _mapState.canvas.olMap_.removeControl(zoomControl);
            zoomSliderParent.append(slider);

            slider.slider({

                range: "min",
                value: mapZoom,
                min: 0,
                max: overlayLayerBounds.numZoomLevels-1,
                slide: function( event, ui ) {
                    _mapState.canvas.olMap_.zoomTo(ui.value);
                    slider.find(".ui-slider-handle").text(ui.value);
                    slider.find(".ui-slider-handle").css("text-decoration", "none");
                },
                change: function( event, ui ) {
                    slider.find(".ui-slider-handle").text(ui.value);
                    slider.find(".ui-slider-handle").css("text-decoration", "none");
                }
            });

            _mapState.canvas.olMap_.events.register('zoomend', _mapState.canvas.olMap_, function(event) {
                var zoom = event.object.getZoom();
                slider.slider('value', zoom);
            });

        },

        createAxes = function(){

            if(_mapState.options.components.xaxis){
                var xSpec = getAxisSpec('xaxis');
                PlotAxis.createAxis(xSpec);
            }
            if(_mapState.options.components.yaxis){
                var ySpec = getAxisSpec('yaxis');
                PlotAxis.createAxis(ySpec);
            }
        },

        generateScaleSlider = function (legendAxisParentId) {

            var legendAxisParent = $('#'+legendAxisParentId);
            var uniqueSliderDivId = legendAxisParentId + "-rangeSlider";
            _mapState.options.components.legend.rangeSliderDivId = uniqueSliderDivId;
            var slider = $('<div id= "'+ uniqueSliderDivId +'" ></div>');
            slider.addClass('plot-legend-range');

            var legendStyleClass = options.components.legend.styleClass;
            var h = $("."+legendStyleClass).css("height").replace('px', '');
            h = parseFloat(h).toFixed(0);
            slider.css({height: h});
            legendAxisParent.append(slider);

            slider.slider({
                orientation: "vertical",
                range: true,
                values: [ _mapState.legendRange[0], _mapState.legendRange[1] ],
                slide: function( event, ui ) {
                    _mapState.legendRange[0] = ui.values[ 0 ];
                    _mapState.legendRange[1] = ui.values[ 1 ];
                },
                change: function(event, ui) {
                    onLayerAttributeChange(event);
                }
            });
        },

        generateBaseLayerOpacitySlider = function (legendAxisParentId) {

            var legendAxisParent = $('#'+legendAxisParentId);
            var uniqueBaseLayerOpacitySliderDivId = legendAxisParentId + "-baseLayerOpacitySlider";
            _mapState.options.components.legend.baseLayerSliderDivId = uniqueBaseLayerOpacitySliderDivId;
            var baseLayerSlider = $('<div id= "'+ uniqueBaseLayerOpacitySliderDivId +'" > <span style="position:absolute; text-align:right; font-size: 0.7em; right: 20px; top:25px">Base Layer Opacity</span></div>');
            baseLayerSlider.addClass('base-layer-opacity');
            legendAxisParent.append(baseLayerSlider);

            baseLayerSlider.slider({
                orientation: "vertical",
                range: "min",
                min: 0,
                max: 100,
                value: _mapState.baseLayerSliderOpacity,
                slide: function( event, ui ) {
                    var opacity = ui.value/100;
                    _mapState.baseLayerSliderOpacity = ui.value;
                    _mapState.canvas.olMap_.baseLayer.setOpacity(opacity);
                }
            });
        },

        generateOverlayLayerOpacitySlider = function (legendAxisParentId) {

            var legendAxisParent = $('#'+legendAxisParentId);
            var uniqueSliderDivId = legendAxisParentId + "-overlayLayerOpacitySlider";
            _mapState.options.components.legend.overlaySliderDivId = uniqueSliderDivId;
            var slider = $('<div id= "'+ uniqueSliderDivId +'" > <span style="position:absolute; font-size: 0.7em; left: 20px; top:25px">Data Layer Opacity</span></div>');
            slider.addClass('overlay-layer-opacity');
            legendAxisParent.append(slider);

            slider.slider({
                orientation: "vertical",
                range: "min",
                min: 0,
                max: 100,
                value: _mapState.overlayLayerSliderOpacity,
                slide: function( event, ui ) {
                    var opacity = ui.value/100;
                    _mapState.overlayLayerSliderOpacity = ui.value;
                    for (var i=0; i < _mapState.overlayList.length; i++){
                        var overlayLayer = _mapState.overlayList[i];
                        overlayLayer.layer.olLayer_.setOpacity(opacity);
                    }

                    _mapState.canvas.all().redraw();
                }
            });
        },

        generateLegendAxis = function (range) {
            var legendAxisParentId = options.components.legend.divId;
            var legendSytleClass = options.components.legend.styleClass;
            var legendAxisStyleClass = options.components.legend.axis.styleClass;
            var legendAxisParent = $('#'+legendAxisParentId);

            var legendAxisDivId = legendAxisParentId + '-yaxis';
            var legendAxis = $('<div id="' + legendAxisDivId + '"></div>');
            legendAxis.empty();
            legendAxis.addClass(legendAxisStyleClass);
            legendAxisParent.append(legendAxis);

            var width = $("."+legendAxisStyleClass).css("width").replace('px','');
            var height = $("."+legendSytleClass).css("height").replace('px','');
            legendAxis.css({"width": width, "height": height});

            var spec = {
                map : {
                    divId : legendAxisParentId,
                    zoom : 0,
                    pixelBounds : {
                        min : {
                            x: 0,
                            y: 0
                        },
                        max : {
                            x: width,
                            y: height
                        }
                    }
                },
                layer : {
                    bounds : {
                        min : {
                            x : 0,
                            y : range.min
                        },
                        max : {
                            x : 0,
                            y : range.max
                        }
                    }
                },
                type : 'yaxis',
                divId : legendAxisDivId,
                options : {
                    parentId : legendAxisParentId,
                    divId : legendAxisDivId,
                    intervals : 3,
                    title : "",
                    labelType : "k",
                    titleOffset : 100
                },
                parentId : legendAxisParentId,
                title : {
                    offset : 10
                }
            };

            PlotAxis.createAxis(spec);
        },

        onLegendUpdated = function (legend, statusInfo ) {

            if(!statusInfo.success){
                return;
            }

            var maxRange = 0;
            var mapZoom = _mapState.canvas.getZoom();
            for (var layerName in _mapState.overlayInfoMap){
                var overlayInfo = _mapState.overlayInfoMap[layerName];
                maxRange = Math.max(maxRange, overlayInfo.meta.levelMaxFreq[String(mapZoom)]);
            }
            var range = {
                min: 0,
                max: maxRange
            };

            if (options.components.controls.colorScaleInputName){
                var colourScaleType = $('input[name="'+ options.components.controls.colorScaleInputName +'"]:checked').val();
                if(colourScaleType === "log10"){
                    range.max = Math.log(range.max)/Math.log(10);
                }
            }

            if (options.components.legend){
                var legendDiv = $('#' + options.components.legend.divId );
                if(legendDiv.size() === 0){            // Hidden right now.
                    return;
                }
                legendDiv.empty();
                var legendStyleClass = options.components.legend.styleClass;
                legendDiv.addClass(legendStyleClass);
                legendDiv.html('<img src="' + legend + '" alt="Legend"/>');
                generateLegendAxis(range);
                generateScaleSlider(options.components.legend.divId);
            }
            if(options.components.baseOpacitySlider && options.components.baseOpacitySlider.enabled){
                generateBaseLayerOpacitySlider(options.components.legend.divId);
            }
            if(options.components.dataOpacitySlider && options.components.dataOpacitySlider.enabled){
                generateOverlayLayerOpacitySlider(options.components.legend.divId);
            }
        },

        showLegend = function(visible){
            if (options.components.legend){
                var legendDiv = $('#' + options.components.legend.divId );
                if (legendDiv.length > 0){
                    visible?legendDiv.show():legendDiv.hide();
                }
            }
        },

        getColorData = function(){
            return {
                scaleType : options.components.controls.colorScaleInputName?
                    $('input[name="'+ options.components.controls.colorScaleInputName +'"]:checked').val()
                    :_mapState.defaultLook,
                rampType : options.components.controls.colorRampInputName?
                    $('input[name="'+ options.components.controls.colorRampInputName +'"]:checked').val()
                    :_mapState.defaultRamp
            };
        },
        updateLegend = function (layerName) {
            if (options.components.legend == null){
                return;
            }

            if (layerName == null){
                showLegend(false);
                return;
            }

            showLegend(true);

            var colorData = getColorData();

            if(!options.components.legend.divId){
                return;
            }

            var legendStyleClass = options.components.legend.styleClass;
            var w = $("."+legendStyleClass).css("width").replace('px', '');
            var h = $("."+legendStyleClass).css("height").replace('px', '');
            w = parseFloat(w).toFixed(0);
            h = parseFloat(h).toFixed(0);

            aperture.io.rest(
                '/legend',
                'GET',
                onLegendUpdated,
                {
                    params: {
                        'transform': colorData.scaleType,
                        'ramp': colorData.rampType,
                        'layer': layerName,
                        'level': _mapState.canvas.getZoom(),
                        'width': w,
                        'height': h
                    }
                }
            );
        },

        clearAllMarkers = function(){

            if(_mapState.options.components.xaxis){
                $('#' + _mapState.options.components['xaxis'].divId).empty();
            }
            if(_mapState.options.components.yaxis){
                $('#' + _mapState.options.components['yaxis'].divId).empty();
            }
        },

        resetScaleMarkers = function(){
            if(_mapState.options.hideAxis){
                return;
            }

            clearAllMarkers();
            createAxes();
        },

        // TODO: Hack for now. Need a way for handling legends for multiple overlays.
        // If there are multiple overlays, we assume that the visibility for all layers
        // is initially set to TRUE and do not render any legends. Legends will appear
        // when only 1 overlay layer is visible.
        hasSingleLayerVisible = function(){
            // Check layer visibility list.
            var visibleCount = 0;
            var singleLayerName = null;
            for (var layerName in _mapState.layerVisible){
                if (_mapState.layerVisible[layerName]){
                    singleLayerName = layerName;
                    visibleCount++;
                }
                if (visibleCount > 1){
                    singleLayerName = null;
                    break;
                }
            }
            return {
                layerName : singleLayerName,
                visibleCount : visibleCount
            };
        },

        onZoom = function (source, event) {
            debug();

            resetScaleMarkers();
            // TODO: Currently we only support colour ramp and
            // legends for only 1-layer at a time. If multiple
            // layers are present, we will not enable them.

            // Get layer visibility state
            var visibleData = hasSingleLayerVisible();
            updateLegend(visibleData.layerName);
        },

        onPan = function (source, event) {
            resetScaleMarkers();
            debug();
        },

        onMapResize = function (source, event) {
            resetScaleMarkers();
        },

        debug = function(){
            if(!options.debug){
                return;
            }
            $('#debug').empty();
            var bounds = _mapState.canvas.olMap_.getExtent();
            $('#debug').append('Bounds: x: ' + bounds.left + ', ' + bounds.right + ' y: ' + bounds.bottom + ', ' + bounds.top + ' z: ' + _mapState.canvas.getZoom());
        },

        onLayerAttributeChange = function (evt) {
            var colorData = getColorData();
            var legendRange = null;

            if (_mapState.options.components.legend.rangeSliderDivId){
                legendRange = $('#' + _mapState.options.components.legend.rangeSliderDivId).slider("option", "values");
            }

            if(_mapState.overlayList.length > 0){
                //map.remove(_dataOverlayLayer);
                for (var i=0; i < _mapState.overlayList.length; i++){
                    _mapState.overlayList[i].layer.remove();
                }
                _mapState.overlayList = [];
                _mapState.overlayInfoMap = {};
            }
            addDataOverlay(options.layerList[0], colorData.scaleType, colorData.rampType, legendRange, true, onLayerProcessed);
        },

        onBgChange = function ( evt ){
            var bg = $('input[name="'+ options.components.controls.colorBackgroundInputName +'"]:checked').val();
            $('.olMap').css('background-color', bg);
        };

        var onLayerProcessed = function(){
            _mapState.loopCount++;
            // Check if all the REST calls have returned, we only want to do the
            // callback after all the responses have been received.
            if (_mapState.loopCount < options.layerList.length){
                // Process the next layer.
                var layerSpec = options.layerList[_mapState.loopCount];
                if (layerSpec['Type'] == 'tile' || layerSpec['Type'] == null){
                    var colorData = getColorData();
                    var legendRange = null;

                    if (_mapState.options.components.legend.rangeSliderDivId){
                        legendRange = $('#' + _mapState.options.components.legend.rangeSliderDivId).slider("option", "values");
                    }
                    addDataOverlay(layerSpec, colorData.scaleType, colorData.rampType, legendRange, false, onLayerProcessed);
                }
                else {
                    onLayerProcessed();
                }
                return;
            }
            else {
                // Reset the loop count.
                _mapState.loopCount = 0;
            }

            if (_mapState.canvas == null){
                // Construct base layer.
                initPlotBaseLayer();

                if (options.components.zoomLevelSlider && options.components.zoomLevelSlider.enabled){
                    generateZoomLevelSlider(options.components.map.divId);
                }
            }

            // Construct plot layers.
            for (var j=0; j < options.layerList.length; j++){
                constructPlotLayer(options.layerList[j]);
            }

            if(!_mapState.isStartComplete){
                _mapState.canvas.olMap_.zoomToMaxExtent();
            }

            if (!_mapState.options.hideAxis){
                createAxes();
            }

            // Get layer visibility state
            var visibleData = hasSingleLayerVisible();
            updateLegend(visibleData.layerName);

            // This assumes that all possible base layers are Google layers
            if (options.baseLayer){
                if (options.baseLayer.length > 1){
                    for (var i = 0; i < options.baseLayer.length; i++) {

                        // Skip the first layer, as it's already set up as the base layer at this point.
                        if (i==0){
                            var base = _mapState.canvas.olMap_.baseLayer;
                            _mapState.baseLayerList.push(base);
                        } else {
                            var mappity =  new OpenLayers.Layer.Google(
                                options.baseLayer[i].Google.options.name, {type: 'customStyle'});

                            var styledMapOptions = {
                                name: "Styled Map"
                            };

                            var styledMapType = new google.maps.StyledMapType(options.baseLayer[i].Google.options.style, styledMapOptions);

                            _mapState.canvas.olMap_.addLayer(mappity);

                            mappity.mapObject.mapTypes.set('customStyle', styledMapType);
                            mappity.mapObject.setMapTypeId('customStyle');

                            _mapState.baseLayerList.push(mappity);
                        }
                    }
                }
            }

            if (options.components.controls){
                if ($('input[name="'+ options.components.controls.colorScaleInputName +'"]')) {
                    $('input[name="'+ options.components.controls.colorScaleInputName +'"]').off('change', onLayerAttributeChange);
                    $('input[name="'+ options.components.controls.colorScaleInputName +'"]').on('change', onLayerAttributeChange);
                }
                if ($('input[name="'+ options.components.controls.colorRampInputName +'"]')) {
                    $('input[name="'+ options.components.controls.colorRampInputName +'"]').off('change', onLayerAttributeChange);
                    $('input[name="'+ options.components.controls.colorRampInputName +'"]').on('change', onLayerAttributeChange);
                }
                if(options.hasBackgroundToggle){
                    $('input[name="'+ options.components.controls.colorBackgroundInputName +'"]').on('change', onBgChange);
                    onBgChange(null);
                }
                if (options.hasLayerControl){
                    createLayerControl();
                }
            }

            if(options.goTo && !_mapState.isStartComplete){
                _mapState.canvas.zoomTo(options.goTo.y, options.goTo.x, options.goTo.zoom);
            }

            debug();
            _mapState.canvas.all().redraw();
            // Now that we're done creating all the layers,
            // check if there is an external callback to
            // be fired.
            if (_mapState.onStartComplete != null && !_mapState.isStartComplete){
                _mapState.onStartComplete(_mapState.canvas);
            }
            _mapState.isStartComplete = true;
        };

        this.start = function(callback) {
            _mapState.onStartComplete = callback;

            if(options.isDensityStrip){
                yFunction = Y_TILE_FUNC_ZERO_CLAMP;
            }

            // Sequentially add overlay layers to ensure ordering
            // is preserved for multi-layer plots.
            addDataOverlay(options.layerList[0], _mapState.defaultLook, _mapState.defaultRamp, null, true, onLayerProcessed);
        };

        this.createLayerControl = createLayerControl;

        this.updateLegend = function(){
            // Toggle legend visibility and update.
            var visibleData = hasSingleLayerVisible();
            updateLegend(visibleData.layerName);
        };

        this.getApertureMap = function(){
            return _mapState.canvas;
        };

        this.getParentDivId = function(){
            return _mapState.options.components.map.parentId;
        }

        this.options = _mapState.options;

        this.getViewBounds = function(){
            if(_mapState.canvas === null){
                return null;
            }
            return _mapState.canvas.olMap_.getExtent();
        };
    };
});
