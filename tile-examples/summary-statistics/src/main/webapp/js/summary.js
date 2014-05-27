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
        MapService = require('./map/MapService'),
        LayerService = require('./layer/LayerService'),
        Map = require('./map/Map'),
        UIMediator = require('./layer/controller/UIMediator'),
        ServerLayerFactory = require('./layer/view/server/ServerLayerFactory'),
        LayerControls = require('./layer/controller/LayerControls');

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
            return inputString.replace(/\./g, '_dot_').replace(/\,/g, '');
        };

        // Clear the layer control of all checkboxes.
        /*var resetLayerControl = function(){
            var layerControl = $("div[id='layer-control']");
            layerControl.empty();
        };*/

        /*var resetLegend = function(tabLayerId){
            var dataPlot = _summaryState.plotMap[tabLayerId];
            if (dataPlot != null){
                dataPlot.updateLegend();
            }
        };*/

        // Create the layerId by concatenating the layer names together;
        var getTabLayerId = function(id){
            /*var tabLayerId = '';
            for (var i=0; i < layerList.length; i++){
                 //JQuery lookups will get confused with periods as part of the DIV ids.
                var layer = layerList[i].Layer.replace(/\./g, '_dot_');
                tabLayerId += (i>0?'_':'') + layer;
            }
            return tabLayerId;*/
            var resultId = id.replace(/\./g, '');
            return resultId.replace(/\ /g, '_').replace(/\,/g, '');
        };

        var constructPlot = function(event, ui){
            var tabLayerId = null;
            if (event.type == 'tabsbeforeactivate'){

                // Check if this is from a tab switch.
                var panelId = ui.newPanel.attr('id');
                if (ui.newTab.text() != _summaryState.tabLabel && panelId.indexOf('tab-plot-') < 0){
                   return;
                }
                if (panelId == 'tabs-plots'){

                	var $tabs = $('#tabs-plots').tabs({active: true});
                    var activeTabId = $tabs.tabs('option', 'active');
                    tabLayerId =  _summaryState.tabLayerMap[activeTabId];
                }
                else {
                    tabLayerId = panelId.replace('tab-plot-', '');
                }
            }
            else if (event.type == 'tabscreate'){
                tabLayerId = ui.panel.attr('id').replace('tab-plot-', '');
            }
            if (!_summaryState.layerInfoMap[tabLayerId]){
                return;
            }

            if (_summaryState.layerInfoMap[tabLayerId].plotDiv == null){
                console.error("Unable to find matching layer DIV.");
                return;
            }
            if ($("div[id='"+ _summaryState.layerInfoMap[tabLayerId].plotDiv +"']").is(':empty')){
                console.log("isEmpty is true");
                var plotInfo = _summaryState.layerInfoMap[tabLayerId];
                var options = {

                    layerList : plotInfo.Layers,
                    //goTo : {"x":128, "y":128, "zoom":2},
                    goTo : {x: 0, y: 0, zoom:1},
                    hasBackgroundToggle : true,
                    hasLayerControl : true,
                    hideAxis : false,
                    debug : false,
                    components : {
                        map : {
                            "divId" : plotInfo.plotDiv,
                            "parentId" : plotInfo.plotParentDiv,
                            "layers" : plotInfo.Layers,
                            "baseLayer" : {
                                "opacity" : plotInfo.baseLayer?plotInfo.baseLayer.Opacity:1
                            }
                        },
                        legend : {
                            "divId" : plotInfo.plotLegendDiv,
                            "styleClass" : "plot-legend",
                            "axis" : {
                                "styleClass" : "plot-legend-axis"
                            }
                        },
                        controls : {
                            "colorScaleInputName" : "colourScale",
                            "colorRampInputName" : "colourRamp",
                            "colorBackgroundInputName" : "bg"
                        },
                        xaxis : {
                            parentId : plotInfo.plotParentDiv, //'tab-' + plotInfo.plotDiv,
                            divId : 'xaxis_' + plotInfo.plotDiv,
                            intervals : 6,
                            title : plotInfo['X Axis'],
                            labelSpec : plotInfo['X Axis Label'],
                            titleOffset : 65
                        },
                        yaxis : {
                            parentId : plotInfo.plotParentDiv, //l'tab-' + plotInfo.plotDiv,
                            divId : 'yaxis_' + plotInfo.plotDiv,
                            intervals : 6,
                            title : plotInfo['Y Axis'],
                            labelSpec : plotInfo['Y Axis Label'],
                            titleOffset : 100
                        },
                        baseOpacitySlider : {
                            enabled : true
                        },
                        dataOpacitySlider : {
                            enabled : true
                        },
                        zoomLevelSlider : {
                            enabled :  false
                        }
                    }
                };
                if(_summaryState.layerInfoMap[tabLayerId].baseLayer){
                    console.log("is baseLayer");
                    options.baseLayer = _summaryState.layerInfoMap[tabLayerId].baseLayer;
                }
            }
            else {
                console.log("else createLayerControl");
                _summaryState.plotMap[tabLayerId].createLayerControl();
            }
        };

        $( "#tabs-major").tabs({
            beforeActivate : function(event, ui){
                constructPlot(event, ui);
            }
        });
		
		$( "#dialog-controls").dialog({
			autoOpen:false,
			resizeable: false,
			width: 370,
			height: "auto",
			position: {my: "right top", at: "right top", of: window}
		});
		$('#accordion').accordion({ heightStyle: "content", autoHeight: false });

        var generateJsonTables = function(jsonFile, onComplete) {
            if(jsonFile === null || jsonFile.length === 0){
                return;
            }

            $.getJSON(jsonFile , function(data) {

                $('#summary-header-title').append(data["Title"]);
                document.title = data["Title"];

                var records = data["Total Records"];
                var samples = data["Sample Size"];
                var percent = (samples/records)*100;
                $('#summary-header-records').append('Sample Size: ' + $.number(samples) + ' records (' + $.number(percent, 1) + '% of dataset)');

                var elipsisNum = 0;

                $.each(data["Summaries"], function(sk, sv) {
                    var tbl_body = "";
                    var tbl_header = "";
                    var isFirstPass = true;

                    var tabType = sv["Type"];
                    var tabId = "tab-table-" + tabType;
                    var tableId = "table-" + tabType;

                    $('#tabs-tables ul').append('<li><a href="#'+ tabId +'">'+ tabType +'</a></li>');
                    var tableTab = $('<div id="'+ tabId +'"></div>');
                    tableTab.addClass('table-sub-tab');
                    $('#tabs-tables').append(tableTab);
                    var emptyTable = $('<table id="'+ tableId +'"><tbody></tbody></table>');
                    tableTab.append(emptyTable);

                    var rowIndex = 0;

                    var _axisMap = {};
                    $.each(sv["Fields"], function(fk, fv) {
                        var tbl_row = "";
                        var columnCount = 0;
                        var layerList = null;
                        var divId = null;

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
                                layerList = v.Layers;
                                var layerName = layerList[0].Layer;
                                divId = tabType + "-" + makeSafeIdForJQuery(layerName);

                                // todo: read from css.
                                var cssSize = {
                                    height: 22,
                                    width : 256
                                };

                                var cell = $('<td></td>');
                                var stripParent = $('<div></div>');
                                var stripParentId = divId + "-parent";
                                stripParent.attr('id', stripParentId);
                                stripParent.css("height", cssSize.height+"px");
                                stripParent.css("width", (cssSize.width+30)+"px");
                                stripParent.css("position", "relative");

                                var strip = $('<div></div>');
                                strip.attr('id', divId);
    //                            strip.addClass('miniStrip');
    //                            var miniStripWidth = $('div.miniStrip').css("width").replace('px', '');
    //                            var miniStripHeight = $('div.miniStrip').css("height").replace('px', '');
                                strip.css(cssSize);
                                strip.addClass('densityStrip');

                                stripParent.append(strip);

                                var btnImg = $('<img>');
                                btnImg.attr('src', 'img/expandIcon.png');
                                btnImg.addClass('stripExpandBtn');
                                cell.append(btnImg);
                                cell.append(stripParent);

                                tbl_row += cell[0].outerHTML;

    //                            tbl_row += '<td><div id="' + divId + '" class="densityStrip" style="height:22px;width:256px;"></div></td>';

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
                                if((typeof value) == "number"){
                                    value = $.number(value, 2);
                                    tbl_row += '<td style="text-align:right">'+ value +'</td>';
                                }
                                else if(Object.prototype.toString.call( value ) === '[object Array]'){
                                    value = "";
                                    $.each(v, function(vk, vv){
                                        $.each(vv, function(vvk, vvv){    // Assumes array of objects.
                                            var vvValue = vvv;
                                            //var temp = "";
                                            if((typeof vvValue) == "number"){
                                                vvValue = $.number(vvValue);
                                            }
                                            value += vvk + ": " + vvValue + ",<br />";

                                        });
                                    });
                                    var rowHtml = '';
                                    var tokens = value.split(/<br \/>/g);
                                    if (tokens.length > 0){
                                        var hasEllipse = false;
                                        for (var i=0; i < tokens.length; i++){
                                            if (i>0){
                                                rowHtml += '</br>';
                                            }
                                            var token = tokens[i];
                                            if(token.length > _summaryState.charLimitCount){
                                                var trunc = token.substring(0, _summaryState.charLimitCount-10) + '...';
                                                rowHtml += trunc;
                                                hasEllipse = true;
                                            }
                                            else {
                                                rowHtml += token;
                                            }
                                        }
                                        if (hasEllipse){
                                            var temp = rowHtml;
                                            rowHtml = '<div id="'+ elipsisNum +'-elipsisDialog" class="elipsisDialog"><p>'+ value +'</p></div>' +
                                                temp + '<button id="'+ elipsisNum +'-elipsisButton" class="elipsisButton">...</button>';

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
                                            var decimalPlaces = 2
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

                        var parentRowID = tabType + "-" + rowIndex;

                        // If layerList is populated, we need to setup a LARGE density strip.
                        if(layerList){
                            tbl_body += '<tr class="parent stripRow hoverableRow" id="'+ parentRowID +'">'+tbl_row+'</tr>';
                            var largeDivId = divId + "-large";
                            var parentDivId = largeDivId + "-parent";
                            var legend = $('<div></div>');
                            var legendDivId = largeDivId+'-legend';
                            legend.attr('id', legendDivId);
                            legend.addClass('strip-legend');

                            tbl_body += '<tr class="child-'+ parentRowID +' stripExpansion"><td colspan="'+ columnCount +'">' +
                                            '<div id="'+parentDivId+'" class="expandedDensityStrip-parent expandedDensityStrip-size" >' +
                                                '<div id="' + largeDivId + '" class="expandedDensityStrip expandedDensityStrip-size" ></div>' +
                                                legend[0].outerHTML +
                                            '</div>' +
                                        '</td></tr>';

                            // Construct a new layer spec object
                            // with the layer name.
                            var layerName = layerList[0].Layer;
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

                if($('div.expandedDensityStrip-size').length > 0){
                    // workaround - very annoying OpenLayers bug where you can't define the map size
                    // via class-based css.
                    var expandedStripWidth = $('div.expandedDensityStrip-size').css("width").replace('px', '');
                    var expandedStripHeight = $('div.expandedDensityStrip-size').css("height").replace('px', '');
                    $('div.expandedDensityStrip').css("width", expandedStripWidth);
                    $('div.expandedDensityStrip').css("height", expandedStripHeight);
                }

                if($("div.elipsisDialog").length > 0){
                    $("div.elipsisDialog").dialog({
                        autoOpen: false,
                        resizeable: true,
                        width: 500,
                        height: 300,
                        position: {my: "center center", at: "center center", of: window}
                    });
                    $( "button.elipsisButton" )
                        .button()
                        .click(function(evt) {


                            var dialogId = (this.id.replace('-elipsisButton','')) + '-elipsisDialog'
                            $( "#"+ dialogId ).dialog( "open" );

                            if (evt.stopPropagation)    evt.stopPropagation();
                            if (evt.cancelBubble!=null) evt.cancelBubble = true;
                          //  evt.preventDefault();
                           // evt.stopPropagation();
                           // evt.stopImmediatePropagation();
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

            var layerDeferreds = LayerService.requestLayers();
            var mapDeferreds = MapService.requestMaps();

            $.when( mapDeferreds, layerDeferreds).done( function( maps, layers ) {

                var plotId = 0;

                //iterate over each of the cross plots, generate the map, and container div
                $.each(maps, function (pk, pv) {
                    // Initialize our maps...
                    // mapID is conditional on the map being a bitcoin or a twitter map.
                    var mapID,
                        tabLayerId,
                        layerConfig,
                        mapConfig;

                    // bitcoin
                    if (datasetLowerCase === 'bitcoin') {

                        //not a bitcoin map
                        if(!pv["id"] || pv["id"].toLowerCase().trim().indexOf(datasetLowerCase) == -1){
                            return;
                        }
                        mapID = pv["id"];
                        tabLayerId = getTabLayerId(mapID);
                        layerConfig = getLayer(layers, mapID);
                        mapConfig = getBitcoinMapConfig(maps, mapID);


                    } else { // twitter
                        var layer;
                        for(var i = 0; i < layers.length; i++){
                            if (layers[i].id.toLowerCase().trim() === datasetLowerCase) {
                                for (var j = 0; j < layers[i]["children"].length; j++) {
                                    mapID = layers[i]["children"][j].name;
                                    layer = layers[i]["children"][j];
                                    tabLayerId = getTabLayerId(mapID);
                                    layerConfig = getLayer(layer, mapID);
                                    mapConfig = pv;//getTwitterMapConfig(layer);

                                    plotId++;
                                    var plotTabDiv = "tab-plot-" + tabLayerId;
                                    var plotDiv = "plot-" + tabLayerId;

                                    $('#tabs-plots ul').append('<li><a href="#' + plotTabDiv + '">' + mapID.replace(datasetLowerCase, '').trim() + '</a></li>');
                                    var $plotTab = $('<div id="' + plotTabDiv + '">');
                                    var $plotVisual = $('<div id="' + plotDiv + '"></div>');

                                    $plotVisual.addClass('plot-parent plot plot-size'); // plot in crossplot.css
                                    $plotVisual.css({width: "100%", height: "100%"});
                                    $plotTab.append($plotVisual);
                                    $('#tabs-plots').append($plotTab);

                                    //add map after the containing div has been added

                                    $plotVisual.append(getMap(plotDiv, mapConfig, layerConfig));
                                }
                            }
                        }
                        //continue the "maps" for each loop after setting up twitter
                        return true;
                    }

                    //something is wrong with the map
                    if (typeof mapConfig === 'undefined') {
                        return;
                    }

                    plotId++;
                    var plotTabDiv = "tab-plot-" + tabLayerId;
                    var plotDiv = "plot-" + tabLayerId;

                    $('#tabs-plots ul').append('<li><a href="#' + plotTabDiv + '">' + mapID.replace(datasetLowerCase, '').trim() + '</a></li>');
                    var $plotTab = $('<div id="' + plotTabDiv + '">');
                    var $plotVisual = $('<div id="' + plotDiv + '"></div>');

                    $plotVisual.addClass('plot-parent plot plot-size'); // plot in crossplot.css
                    $plotVisual.css({width: "100%", height: "100%"});
                    $plotTab.append($plotVisual);
                    $('#tabs-plots').append($plotTab);

                    //add map after the containing div has been added

                    $plotVisual.append(getMap(plotDiv, mapConfig, layerConfig));
                });

                $('#tabs-plots ul').each(function () {
                    var $active, $content, $links = $(this).find('a');
                    $active = $($links.filter('[href="' + location.hash + '"]')[0] || $links[0]);
                    $active.addClass('active');
                    if($active[0]) {
                        $content = $($active[0].hash);
                    }

                    // Hide the remaining content
                    $links.not($active).each(function () {
                        $(this.hash).hide();
                    });

                    // Bind the click event handler
                    $(this).on('click', 'a', function (e) {
                        // Make the old tab inactive.
                        $active.removeClass('active');
                        $content.hide();

                        // Update the variables with the new link and content
                        $active = $(this);
                        $content = $(this.hash);

                        // Make the tab active.
                        $active.addClass('active');
                        $content.show();

                        // Prevent the anchor's default click action
                        e.preventDefault();
                    });
                });
            });
        };

        var getBitcoinMapConfig = function(maps, mapID){

            var result = [],
                i,
                length = maps.length,
                mapConfig;

            for(i=0; i<length; i++){
                if (maps[i].id && maps[i].id.toLowerCase().trim().indexOf(datasetLowerCase) != -1){
                    result.push(maps[i]);
                }
            }

            $.each(result, function (pk, pv) {
                if (pv.id === mapID) {
                    mapConfig = pv;
                }
            });

            return mapConfig;
        };

        var getLayer = function(layers, mapID){
            var layerArray,
                layer;

            mapID = mapID.toLowerCase();

            //find the right collection of layers
            $.each(layers, function (pk, pv) {
                if (pv.id === datasetLowerCase) {
                    layerArray = pv;
                }
            });

            // bitcoin
            if (datasetLowerCase === 'bitcoin') {

                if (layerArray) {
                    $.each(layerArray.children, function (pk, pv) {
                        //if mapID contains pv.name
                        var name = pv.name.toLowerCase().trim();
                        if (mapID.indexOf(name) != -1)
                            layer = pv;
                    });
                }

                return [
                    {
                        "layer": layer["id"],
                        "domain": layer.renderers[0].domain,
                        "name": layer.name,
                        "renderer": layer.renderers[0].renderer,
                        "transform": layer.renderers[0].transform
                    }
                ];
                //the dataset didn't exist in the layers array if layer is undefined;
            } else { // twitter
                layer = layers;

                return [
                    {
                        "layer": layer["id"],
                        "domain": layer.renderers[0].domain,
                        "name": layer.name,
                        "renderer": layer.renderers[0].renderer,
                        "transform": layer.renderers[0].transform
                    }
                ];

            }
            //return layer;

        };

        var getMap = function(mapID, mapConfig, layerConfig){
            var worldMap = new Map(mapID, mapConfig),
                uiMediator;

            // ... (set up our map axes) ...
            worldMap.setAxisSpecs(MapService.getAxisConfig(mapConfig));

            uiMediator = new UIMediator();
            if(layerConfig[0]["domain"]==='server') {
                // Create client and server layers
                //ClientLayerFactory.createLayers(layerConfig, uiMediator, worldMap);
                ServerLayerFactory.createLayers(layerConfig, uiMediator, worldMap);
            }

            new LayerControls().initialize( uiMediator.getLayerStateMap() );


        };

        this.start = function(){

            //setup the ui layout
            var tocPane = $('#toc');
            tocPane.load('toc.html #toc-contents');
            tocPane.addClass('ui-layout-west');
            $('#head').addClass('ui-layout-north');
            $('#summary').addClass('ui-layout-center');
            var layout = $('#container').layout({applyDemoStyles: true, north:{size:140}, west:{size:230}});
            layout.panes.west.css({
                background:  "rgb(204,204,204)"
            });
            layout.panes.north.css({
                background:  "rgb(204,204,204)"
            });
            layout.panes.center.css({
                background:  "rgb(204,204,204)"
            });

            if(!summaryBuilderOptions.dataset){
            	$('#summary').html('<h2>No dataset selected.</h2>');
            	return;
            }
        	
            var tableJsonFile = summaryBuilderOptions.dataDir + '/' + summaryBuilderOptions.dataset + '/tables.json';
            //var plotJsonFile = summaryBuilderOptions.dataDir + '/' + summaryBuilderOptions.dataset + '/plots.json';

            var showControls = $('<div id="show-controls"></div>');
            showControls.addClass('show-controls');
            $('#tabs-tables').append(showControls);

            var showButton = $("<button>Controls</button>")
                .button()
                .click(function( event ) {
                    event.preventDefault();
                    $( "#dialog-controls").dialog("open");
                    $('#accordion').accordion({ autoHeight: false });
                });

            showControls.append(showButton);

            generateJsonTables(tableJsonFile, function(){
                var len = _densityStrips.length;
                var xDataMaps = [];
                for(var i = 0; i < len; i++){
                    var options = {
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

                    var startupCallback = null;

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

            });//end generateJsonTables call

            generateJsonPlots(function(){
                //console.log('callback fired');
                $("#tabs-plots").tabs({
                    create : function(event, ui) {
                        //console.log('create: some random layer');
                        if (!ui.panel.attr('id')) {
                            //console.log("returning from create");
                            return;
                        }

                        var layerName = ui.panel.attr('id').replace('tab-plot-', '');
                        console.log('create: ' + layerName);
                    },
                    beforeActivate : function(event, ui) {
                        console.log('generateJsonPlots: ');
                        if (!ui.newPanel.attr('id')) {
                            //console.log("returning from beforeActivate");
                            return;
                        }

                        var layerName = ui.newPanel.attr('id').replace('tab-plot-', '');
                        //console.log('generateJsonPlots: ' + layerName);
                        constructPlot(event, ui);
                    }
                });
            });
      
        };
    };
});

