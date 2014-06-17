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
    "use strict";

    var XDataMap = require('./xdata'),
    	PlotLink = require('./plotlink'),
        Map = require('./map/Map'),
        MapService = require('./map/MapService'),
        LayerService = require('./layer/LayerService'),
        BaseLayerMediator = require('./layer/controller/BaseLayerMediator'),
        ClientLayerMediator = require('./layer/controller/ClientLayerMediator'),
        ServerLayerMediator = require('./layer/controller/ServerLayerMediator'),
        ClientLayerFactory = require('./layer/view/client/ClientLayerFactory'),
        ServerLayerFactory = require('./layer/view/server/ServerLayerFactory'),
        LayerControls = require('./layer/controller/LayerControls'),
        CarouselControls = require('./layer/controller/CarouselControls'),
        PyramidFactory = require('./binning/PyramidFactory'),
        OverlayButton = require('./ui/OverlayButton');

    return function(summaryBuilderOptions) {

        var _densityStrips = [],
            _summaryState = {
                tabLayerMap : {},
                layerInfoMap : {},
                tabLabel : 'Cross Plots',
                plot : {},
                charLimitCount : 40,
                plotMap : {},
                tocVisible : true
            },
            datasetLowerCase = summaryBuilderOptions.dataset ? summaryBuilderOptions.dataset.toLowerCase() : null;

        var makeSafeIdForJQuery = function (inputString){
            return inputString.replace(/\./g, '_dot_').replace(/\,/g, '').replace(/\ /g, '_');
        };

        // Create the layerId by concatenating the layer names together;
        var getTabLayerId = function(id){
            return id.replace(/\ /g, '_').replace(/\,/g, '').replace(/\./g, '');
        };

        var generateJsonTables = function(jsonFile, onComplete) {
            if(jsonFile === null || jsonFile.length === 0){
                return;
            }

            $.getJSON(jsonFile , function(data) {
                var records = data["Total Records"],
                    samples = data["Sample Size"],
                    percent = (samples/records)*100,
                    $tabsTablesUl = $('#tabs-tables ul'),
                    $expandedDensityStripSize,
                    $elipsisDialog,
                    elipsisNum = 0,
                    $summaryHeader = $('<div id="summary-header"></div>');

                document.title = data["Title"];
                $summaryHeader.append('<div id="summary-header-records"> Sample Size: ' + $.number(samples) + ' records (' + $.number(percent, 1) + '% of dataset)</div>');
                $summaryHeader.append('<div id="summary-header-title">' + data["Title"] + '</div>');
                $( 'header' ).append($summaryHeader);

                $.each(data["Summaries"], function(sk, sv) {
                    var tbl_body = "",
                        tbl_header = "",
                        isFirstPass = true,
                        tabType = sv["Type"],
                        tabId = "tab-table-" + tabType,
                        tableId = "table-" + tabType,
                        tableTab,
                        emptyTable,
                        rowIndex = 0,
                        _axisMap = {};

                    $tabsTablesUl.append('<li><a href="#'+ tabId +'">'+ tabType +'</a></li>');
                    tableTab = $('<div id="'+ tabId +'"></div>');
                    tableTab.addClass('table-sub-tab');
                    $('#tabs-tables').append(tableTab);
                    emptyTable = $('<table id="'+ tableId +'"><tbody></tbody></table>');
                    tableTab.append(emptyTable);

                    $.each(sv["Fields"], function(fk, fv) {
                        var tbl_row = "",
                            columnCount = 0,
                            layerList = null,
                            divId = null,
                            parentRowID;

                        $.each(fv, function(k, v){
                            columnCount++;

                            if(isFirstPass){
                                tbl_header += "<th>"+ k +"</th>";
                            }

                            if(k === "Field Alias"){
                                tbl_row += '<td class="aliasLabel">'+ v +'</td>';
                            }
                            else if(k === "Density Strip" && v.Layers != null){ // name of a tiled dataset
                                // Density Strips should only ever have 1 layer.
                                var layerName,
                                    cell = $('<td></td>'),
                                    stripParent  = $('<div></div>'),
                                    stripParentId,
                                    strip = $('<div></div>'),
                                    btnImg = $('<img>'),
                                    cssSize;

                                layerList = v.Layers;
                                layerName = layerList[0].Layer;
                                divId = tabType + "-" + makeSafeIdForJQuery(layerName);

                                // todo: read from css.
                                cssSize = {
                                    height: 22,
                                    width : 256
                                };

                                stripParentId = divId + "-parent";
                                stripParent
                                    .attr('id', stripParentId)
                                    .css({
                                        'height' : cssSize.height+'px',
                                        'width' : (cssSize.width+30)+'px',
                                        'position' : 'relative'
                                    });

                                strip
                                    .attr('id', divId)
                                    .css(cssSize)
                                    .addClass('densityStrip');
                                stripParent.append(strip);

                                btnImg
                                    .attr('src', 'img/expandIcon.png')
                                    .addClass('stripExpandBtn');
                                cell
                                    .append(btnImg)
                                    .append(stripParent);
                                tbl_row += cell[0].outerHTML;
                                _densityStrips.push({
                                    "Layers" : layerList,
                                    "parentDiv" : stripParentId,
                                    "mapDiv" : divId,
                                    "profile" : "mini", // Index mode, no zooming/panning.
                                    "goTo" : {x:0, y:0, zoom:0}
                                });
                                _axisMap[layerName] = v['X Axis Label'];
                            }
                            else {    // regular field
                                var value = v;
                                if ((typeof value) == "number") {
                                    value = $.number(value, 2);
                                    tbl_row += '<td style="text-align:right">'+ value +'</td>';
                                }
                                else if ($.isArray(value)) {
                                    value = "";
                                    var rowHtml = '',
                                        tokens = '';

                                    $.each(v, function(vk, vv) {
                                        $.each(vv, function(vvk, vvv){    // Assumes array of objects.
                                            var vvValue = vvv;
                                            if((typeof vvValue) == "number"){
                                                vvValue = $.number(vvValue);
                                            }
                                            value += vvk + ": " + vvValue + ",<br />";

                                        });
                                    });
                                    tokens = value.split(/<br \/>/g);
                                    if (tokens.length > 0) {
                                        var hasEllipse = false,
                                            i = 0;
                                        for (i; i < tokens.length; i++) {
                                            var token = tokens[i];
                                            if (i>0){
                                                rowHtml += '</br>';
                                            }
                                            if(token.length > _summaryState.charLimitCount){
                                                rowHtml += token.substring(0, _summaryState.charLimitCount-10) + '...';
                                                hasEllipse = true;
                                            }
                                            else {
                                                rowHtml += token;
                                            }
                                        }
                                        if (hasEllipse){
                                            rowHtml = '<div id="'+ elipsisNum +'-elipsisDialog" class="elipsisDialog"><p>'+ value +'</p></div>' +
                                                rowHtml + '<button id="'+ elipsisNum +'-elipsisButton" class="elipsisButton">...</button>';

                                            elipsisNum++;
                                        }
                                    }
                                    else {
                                        rowHtml = value;
                                    }
                                    tbl_row += "<td>"+ rowHtml +"</td>";
                                }
                                else if((typeof value) == "object"){
                                    value = "";
                                    $.each(v, function(vk, vv){
                                        if((typeof vv) == 'object' && vv["Type"]){   // A typed value.
                                            var decimalPlaces = 2;
                                            if(vv["Type"] == "integer"){
                                                decimalPlaces = 0;
                                            }
                                            else if(vv["Type"] == "float"){
                                                decimalPlaces = 2;
                                            }
                                            value += $.number(vv["Value"], decimalPlaces) + vv["Unit"] + "<br />";
                                        }
                                        else{
                                            value += vk + ": " + vv + ",<br />";
                                        }
                                    });
                                    tbl_row += "<td>"+ value +"</td>";
                                }
                                else {
                                    tbl_row += "<td>"+ value +"</td>";
                                }
                            }
                        });

                        isFirstPass = false;
                        parentRowID = tabType + "-" + rowIndex;

                        // If layerList is populated, we need to setup a LARGE density strip.
                        if(layerList){
                            var largeDivId = divId + "-large",
                                parentDivId = largeDivId + "-parent",
                                legend = $('<div></div>'),
                                legendDivId = largeDivId+'-legend',
                                layerName = layerList[0].Layer;

                            tbl_body += '<tr class="parent stripRow hoverableRow" id="'+ parentRowID +'">'+tbl_row+'</tr>';

                            legend
                                .attr('id', legendDivId)
                                .addClass('strip-legend');

                            tbl_body += '<tr class="child-'+ parentRowID +' stripExpansion"><td colspan="'+ columnCount +'">' +
                                            '<div id="'+parentDivId+'" class="expandedDensityStrip-parent expandedDensityStrip-size" >' +
                                                '<div id="' + largeDivId + '" class="expandedDensityStrip expandedDensityStrip-size" ></div>' +
                                                legend[0].outerHTML +
                                            '</div>' +
                                        '</td></tr>';

                            // Construct a new layer spec object with the layer name.
                            _densityStrips.push({
                                "Layers" : [{
                                    "Layer" : layerName,
                                    "Type" : "tile",
                                    "Config" : layerList[0].Config
                                }],
                                "parentDiv" : parentDivId,
                                "mapDiv" : largeDivId,
                                "legendDiv" : legendDivId,
                                "profile" : "normal",      // normal scrollable/zoomable density strip.
                                "goTo" : {x:0, y:0, zoom:1},
                                "labelSpec" : _axisMap[layerName]
                            });
                        } else {
                            tbl_body += '<tr class="stripRow">'+tbl_row+'</tr>';
                        }
                        rowIndex++;
                        layerName = null;
                    }); // end row
                    $("#" + tableId + " tbody").html("<tr>"+tbl_header+"</tr>" + tbl_body);
                });
                $expandedDensityStripSize = $('div.expandedDensityStrip-size');
                if($expandedDensityStripSize.length > 0){
                    // workaround - very annoying OpenLayers bug where you can't define the map size
                    // via class-based css.
                    $expandedDensityStripSize.css("width", $expandedDensityStripSize.css("width").replace('px', ''));
                    $expandedDensityStripSize.css("height", $expandedDensityStripSize.css("height").replace('px', ''));
                }
                $elipsisDialog = $("div.elipsisDialog");
                if($elipsisDialog.length > 0){
                    $elipsisDialog.dialog({
                        autoOpen: false,
                        resizeable: true,
                        width: 500,
                        height: 300,
                        position: {my: "center center", at: "center center", of: window}
                    });
                    $( "button.elipsisButton" )
                        .button()
                        .click(function(evt) {
                            var dialogId = (this.id.replace('-elipsisButton','')) + '-elipsisDialog';
                            $( "#"+ dialogId ).dialog( "open" );
                            if (evt.stopPropagation)    evt.stopPropagation();
                            if (evt.cancelBubble!=null) evt.cancelBubble = true;
                        });
                }
                $("#tabs-tables").tabs();
                $("tr.parent")
                    .css("cursor", "pointer")
                    .attr("title", "Click to expand/collapse")
                    .click(function() {
                        $(this).siblings(".child-"+this.id).toggle();
                        $(this).toggleClass("stripRow");
                        $(this).toggleClass("expandedStripRow");
                        $(this).find('.stripExpandBtn').attr('src',
                        $(this).hasClass('expandedStripRow')?'img/collapseIcon.png':'img/expandIcon.png');
                    });
                $("tr.stripExpansion").hide().children("td");
                onComplete();
            })
            .error(function(jqXHR, textStatus, errorThrown){
                console.error("Error reading summary JSON at " + jsonFile + ": " + errorThrown );
            });
        };

        var generateJsonPlots = function(){
            var layerDeferreds = LayerService.requestLayers(),
                mapDeferreds = MapService.requestMaps(),
                $tabsPlotsUl = $('#tabs-plots ul');

            var getLayerConfig = function(layer) {
                return [{
                    "layer": layer.id,
                    "domain": layer.renderers[0].domain,
                    "name": layer.name,
                    "renderer": layer.renderers[0].renderer,
                    "transform": layer.renderers[0].transform
                }];
            };

            var generateMap = function(mapID, mapConfig, layerConfig, layer){
                var tabLayerId = getTabLayerId(mapID),
                    plotTabDiv = "tab-plot-" + tabLayerId,
                    $plotTab = $('<div id="' + plotTabDiv + '">'),
                    plotDiv = "plot-" + tabLayerId,
                    $plotVisual = $('<div id="' + plotDiv + '"></div>'),
                    plotControls = plotDiv + '-controls',
                    baseLayerMediator,
                    worldMap,
                    $plotControls,
                    controlsButton,
                    serverLayerDeferred,
                    baseLayers = [];

                $tabsPlotsUl.append('<li><a href="#' + plotTabDiv + '">' + mapID.replace(datasetLowerCase, '').trim() + '</a></li>');
                $plotTab.append($plotVisual);
                $('#tabs-plots').append($plotTab);

                worldMap = new Map(plotDiv, mapConfig);
                worldMap.setAxisSpecs(MapService.getAxisConfig(mapConfig));
                worldMap.setTileBorderStyle(mapConfig);

                baseLayerMediator = new BaseLayerMediator();
                baseLayerMediator.registerLayers( worldMap );

                //create the controls
                $plotControls = $('<div id="' + plotControls + '">');
                $plotVisual.append($plotControls);
                controlsButton = new OverlayButton({
                    id: plotControls,
                    header: 'Controls',
                    content: ''
                });

                controlsButton.getHeaderElement().addClass('layer-controls-header');
                controlsButton.getContentElement().addClass('layer-controls-content');
                controlsButton.getContainerElement().addClass('layer-controls');

                var baseLayerMediator = new BaseLayerMediator();
                baseLayerMediator.registerLayers( worldMap );

                if (layerConfig[0].domain === 'server') {

                    var serverLayerMediator = new ServerLayerMediator();
                    var serverLayerFactory = new ServerLayerFactory();
                    var serverLayerDeferreds = serverLayerFactory.createLayers( layerConfig, worldMap, serverLayerMediator );

                    $.when( serverLayerDeferreds ).done( function( serverLayers ) {

                        var sharedStates = [];

                        $.merge( sharedStates, baseLayerMediator.getLayerStates() );
                        $.merge( sharedStates, serverLayerMediator.getLayerStates() );

                        new LayerControls(plotControls +'-content', sharedStates );
                    });

                } else {

                    var clientLayers = [{
                        "domain" : layer.renderers[0].domain,
                        "layer" : layer.id,
                        "name" : layer.name,
                        "renderers" : layer.renderers[0].renderers
                    }];

                    var clientLayerMediator = new ClientLayerMediator();
                    var clientLayerFactory = new ClientLayerFactory();
                    var clientLayerDeferreds = clientLayerFactory.createLayers( clientLayers, worldMap, clientLayerMediator );

                    $.when( clientLayerDeferreds ).done( function( clientLayers ) {

                        var sharedStates = [];

                        $.merge( sharedStates, baseLayerMediator.getLayerStates() );
                        $.merge( sharedStates, clientLayerMediator.getLayerStates() );

                        new LayerControls(plotControls +'-content', sharedStates );
                        new CarouselControls( clientLayerMediator.getLayerStates(), worldMap );

                    });
                }
            };

            $.when( mapDeferreds, layerDeferreds).done( function( maps, layers ) {
                var dataset = datasetLowerCase.split(".")[0];
                
                //iterate over each of the cross plots, generate the map, and container div
                $.each(maps, function (pk, mapConfig) {
                    // Initialize our maps...
                    var mapID,
                        layerConfig;

                    // determine whether the map matches our dataset
                    if (mapConfig.dataset.toLowerCase().trim() === dataset) {
                    	// for each layer if it matches our dataset and this map then add
                    	$.each(layers, function (pk, pv) {
                    		if (pv.id.toLowerCase().trim() === dataset) {
                    			$.each(pv.children, function (k, layer) {
                    				if (PyramidFactory.pyramidsEqual(layer.pyramid, mapConfig.PyramidConfig)){
                    					mapID = layer.name;
                    					layerConfig = getLayerConfig(layer);
                    					generateMap(layer.name, mapConfig, layerConfig, layer);
                    				}
                    			});
                    		}
                    	});
                	}
                });
                
                $tabsPlotsUl.each(function () {
                    $(this).click( function (){
                        $(window).resize();
                    });
                });

                $("#tabs-plots").tabs();
            });
        };

        this.start = function(){

            //setup the ui layout
            var tocPane = $('#toc'),
                $summaryDiv = $('#summary'),
                layout,
                tableJsonFile,
                showControls,
                showButton;

            tocPane.load('toc.html #toc-contents');
            tocPane.addClass('ui-layout-west');
            $('header').addClass('ui-layout-north');
            $summaryDiv.addClass('ui-layout-center');
            layout = $('#container').layout({applyDemoStyles: true, north:{size:95}, west:{size:278}});

            layout.panes.west.css({
                "background":  "rgb(204,204,204)"
            });
            layout.panes.north.css({
                "background":  "rgb(204,204,204)"
            });
            layout.panes.center.css({
                "background":  "rgb(204,204,204)"
            });

            if(!summaryBuilderOptions.dataset){
                $summaryDiv.html('<h2>No dataset selected.</h2>');
            	return;
            }

            $( "#tabs-major").tabs();

            $( "#dialog-controls").dialog({
                autoOpen:false,
                resizeable: false,
                width: 370,
                height: "auto",
                position: {my: "right top", at: "right top", of: window}
            });

            $('#accordion').accordion({ heightStyle: "content", autoHeight: false });
        	
            tableJsonFile = summaryBuilderOptions.dataDir + '/' + summaryBuilderOptions.dataset + '/tables.json';

            //create and add the controls button to the Tables tab
            showControls = $('<div id="show-controls"></div>');
            showControls.addClass('show-controls');
            $('#tabs-tables').append(showControls);

            showButton = $("<button>Controls</button>")
                .button()
                .click(function( event ) {
                    event.preventDefault();
                    $( '#dialog-controls' ).dialog( 'open' );
                    $( '#accordion' ).accordion({ autoHeight: false });
                });

            showControls.append(showButton);

            //when table controls is open, and Cross Plots (or Tables) is clicked, close the dialog
            //also trigger the first resize
            $( '#tabs-major li' ).click(function( event ) {
                $( '#dialog-controls').dialog( 'close' );
                //The following two click events are hacks
                $('#tabs-plots li').first().click(); // without this, the left-axis and map don't render properly
                $.each($('#tabs-plots .olMap'), function(){
                    $('#' + this.id + ' fieldset input[type=radio]').first().click() // without this, the map background loads white by default.
                });
            });

            generateJsonTables(tableJsonFile, function(){
                var len = _densityStrips.length,
                    xDataMaps = [],
                    i = 0;
                for(i; i < len; i++){
                    var startupCallback = null,
                        options = {
                        isDensityStrip : true,
                        layerList : _densityStrips[i].Layers,
                        goTo : _densityStrips[i].goTo,
                        hasBackgroundToggle : true,
                        hasLayerControl : false,
                        hideAxis : false,
                        debug : false,
                        components : {
                            map : {
                                "divId" : _densityStrips[i].mapDiv,
                                "parentId" : _densityStrips[i].parentDiv
                            },
                            legend : {
                                "divId" : _densityStrips[i].legendDiv,
                                "styleClass" : "strip-legend",
                                "axis" : {
                                    "styleClass" : "strip-legend-axis"
                                }
                            },
                            zoomLevelSlider : {
                                enabled : true
                            },
                            controls : {
                                "colorScaleInputName" : "colourScale",
                                "colorRampInputName" : "colourRamp",
                                "colorBackgroundInputName" : "bg"
                            }
                        }
                    };
                    if(_densityStrips[i]["profile"] === "mini"){
                        options.mapOptions = {
                            controls: []
                        };
                        options.components.zoomLevelSlider.enabled = false;
                    }else { // Full size
                        options.components.xaxis = {
                            parentId : _densityStrips[i].parentDiv,
                            divId : 'xaxis_' + _densityStrips[i].mapDiv,
                            intervals : 6,
                            title : "",
                            labelSpec : _densityStrips[i].labelSpec,
                            titleOffset : 65
                        };
                    }

                    xDataMaps[i] = new XDataMap(options);

                    if ($("div[id='"+ _densityStrips[i]["mapDiv"] +"']").length === 0){ // exists yet?
                        console.error("Could not start map with missing div for " + _densityStrips[i]["layer"]);
                    }

                    // We know the array is populated [mini, full-size, mini, full-size, ...]
                    if(_densityStrips[i]["profile"] === "mini") {
                        startupCallback = null;
                    } else {
                        startupCallback = function(trackerIndex, trackeeIndex){
                            return function(xDataMap){
                                var plotLink = new PlotLink({
                                    tracker: xDataMaps[trackerIndex],
                                    trackee: xDataMaps[trackeeIndex]
                                });
                                plotLink.start(xDataMap);
                            };
                        }(i-1, i);
                    }

                    xDataMaps[i].start(startupCallback);
                }
            });

            generateJsonPlots();
        };
    };
});