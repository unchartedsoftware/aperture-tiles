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

/*global $, define, console */

/**
 * Implements a panel that creates a set of controls to change the visual settings
 * of each layer in the system.  The panel works with a map of LayerState objects that
 * are populated externally.  The control set consists of a check box to control overall
 * visibility, a slider to control opacity, a range slider to set a value filter, buttons
 * to control layer ordering, and a linked settings panel that allows for ramp parameters
 * to be adjusted.  Note that setting a ramp type or function to null will result in the
 * filter control being hidden.
 *
 * This class follows the Separable Model pattern used by Swing widgets in Java, where
 * the controller and view are collapsed into a single class.
 */
define(function (require) {
    "use strict";

    var Class = require('../../class'),
        LayerState = require('../model/LayerState'),
        AxisUtil = require('../../map/AxisUtil'),
        LayerControls,
        addLayer,
        showLayerSettings,
        OPACITY_RESOLUTION,
        FILTER_RESOLUTION,
        replaceChildren,
        makeLayerStateObserver,
        replaceLayers,
        sortLayers,
        generateFilterAxis,
        generateFilterAxisLabels,
        updateAxisLabels;

    // constant initialization
    OPACITY_RESOLUTION = 100.0;
    FILTER_RESOLUTION = 100.0;

    /**
     * Replaces node's children and returns the replaced for storage.
     *
     * @param {Object} $parent - The node to remove the children from.
     *
     * @param {Object} children - The children to replace.  Null will result
     * in children being removed only.
     *
     * @returns {Array} - The removed children.
     */
    replaceChildren = function ($parent, children) {
        var i, removed;

        // Remove existing children.
        removed = $parent.children();
        for (i = 0; i < removed.length; i += 1) {
            removed.eq(i).detach();
        }

        // Add in new children.
        if (children !== null) {
            for (i = 0; i < children.length; i += 1) {
                children.eq(i).appendTo($parent);
            }
        }
        return removed;
    };

    /**
     * Adds a new set of layer controls to the panel.
     *
     * @param layerState - The layer state model the controls are bound to.
     *
     * @param $parentElement - The parent element in the document tree to add the controls to.
     *
     * @param controlsMap - Maps layers to the sets of controls associated with them.
     */
    addLayer = function (sortedLayers, index, $parentElement, controlsMap, layerControlsSpec) {
        var $cell,
            $filterSlider,
            $opacitySlider,
            $toggleDiv,
            $toggleBox,
            $promotionDiv,
            $promotionButton,
            $settingsButton,
            $layerControlSetRoot,
            $layerControlTitleBar,
            $layerContent,
            className,
            hasFilter,
            name,
            filterRange,
            id,
            layerState,
            $filterAxis,
            count,
            radioHTML,
            $mapDiv,
            key,
            $fieldset;

        layerState = sortedLayers[index];

        $layerControlSetRoot = $('<div id="layer-controls-' + layerState.getId() + '" class="layer-controls-layer"></div>');

        name = layerState.getName();
        name = name === undefined ||  name === "" ? layerState.getId() : layerState.getName();

        // create title div
        $layerControlTitleBar = $('<div class="layer-title"></div>');
        // add title span to div
        $layerControlTitleBar.append($('<span class="layer-labels">' + name + '</span>'));
        $layerControlSetRoot.append($layerControlTitleBar);

        // create content div
        $layerContent = $('<div class="layer-content"></div>');
        $layerControlSetRoot.append($layerContent);

        // create settings button
        if ( layerState.getRampFunction() !== null && layerState.getRampType() !== null) {
            $settingsButton = $('<button class="settings-link">settings</button>');
            $settingsButton.click(function () {
                showLayerSettings($parentElement, layerState);
            });
            $layerControlTitleBar.append($settingsButton);
        }

        // add visibility toggle box
        $toggleDiv = $('<div class="layer-toggle"></div>');
        $toggleBox = $('<input type="checkbox" checked="checked">');
        $toggleDiv.append($toggleBox);
        // Initialize the button from the model and register event handler.
        $toggleBox.prop("checked", layerState.isEnabled());
        $toggleBox.click(function () {
            layerState.setEnabled($toggleBox.prop("checked"));
        });
        $layerContent.append($toggleDiv);

        // add opacity slider
        filterRange = layerState.getFilterRange();
        hasFilter = filterRange !== null && filterRange[0] >= 0 && filterRange[1] >= 0;
        className = hasFilter ? "opacity-slider" : "base-opacity-slider";

        $cell = $('<div class="' + className + '"></div>');
        $layerContent.append($cell);

        $cell.append($('<div class="slider-label">Opacity</div>'));
        $opacitySlider = $('<div id="' + "opacity-slider-" + name + '"></div>').slider({
            range: "min",
            min: 0,
            max: OPACITY_RESOLUTION,
            value: layerState.getOpacity() * OPACITY_RESOLUTION,
            change: function () {
                layerState.setOpacity($opacitySlider.slider("option", "value") / OPACITY_RESOLUTION);
            },
            slide: function () {
                layerState.setOpacity($opacitySlider.slider("option", "value") / OPACITY_RESOLUTION);
            }
        });
        $cell.append($opacitySlider);

        // add filter slider
        if (hasFilter) {
            $cell = $('<div class="filter-slider"></div>');
            $layerContent.append($cell);

            $cell.append($('<div class="slider-label">Filter</div>'));
            $filterSlider = $('<div class="filter-slider-img"></div>');
            $filterSlider.slider({
                range: true,
                min: 0,
                max: FILTER_RESOLUTION,
                values: [filterRange[0] * FILTER_RESOLUTION, filterRange[1] * FILTER_RESOLUTION],
                change: function () {
                    var result = $filterSlider.slider("option", "values");
                    layerState.setFilterRange([result[0] / FILTER_RESOLUTION, result[1] / FILTER_RESOLUTION]);
                }
            });
            // Disable the background for the range slider
            $(".ui-slider-range", $filterSlider).css({"background": "none"});

            // Set the ramp image
            $filterSlider.css({'background': 'url(' + layerState.getRampImageUrl() + ')', 'background-size': '100%'});
            $cell.append($filterSlider);

            //create the filter axis container and append it to the .filter-slider
            $filterAxis = $('<div class="filter-axis"></div>');
            $cell.append($filterAxis);

            //create the filter axis
            generateFilterAxis( $filterAxis, layerControlsSpec );
        } else {
            $filterSlider = null;
        }

        // add layer promotion button
        if (layerState.getZIndex() !== null && layerState.getZIndex() >= 0) {
            $promotionDiv = $('<div class="promotion-container"></div>');
            $layerContent.append($promotionDiv);
            $promotionButton = $('<button class="layer-promotion-button" title="pop layer to top"></button>');
            $promotionButton.click(function () {
                var nextLayerState, otherZ;
                if (index > 0) {
                    nextLayerState = sortedLayers[index - 1];
                    otherZ = nextLayerState.getZIndex();
                    nextLayerState.setZIndex(layerState.getZIndex());
                    layerState.setZIndex(otherZ);
                }
            });
            $promotionDiv.append($promotionButton);
        }

        $parentElement.append($layerControlSetRoot);

        //add base layer radio buttons when this layer is the base layer
        if(name === 'Base Layer'){
            count = 0;
            radioHTML = '';
            $mapDiv = $('#' + layerControlsSpec.map.id);
            $fieldset = $('<fieldset id="fieldset_' + layerControlsSpec.map.id + '"></fieldset>');

            for(key in layerControlsSpec.baseLayers){
                if(layerControlsSpec.baseLayers.hasOwnProperty( key )){
                    radioHTML += '<input type="radio" class="radio_' + layerControlsSpec.map.id + '" id="baseLayerId_' + layerControlsSpec.map.id +
                        '_' + layerControlsSpec.baseLayers[key].options.name + '" name="base-names_' + layerControlsSpec.map.id + '" value="' + count +
                            '"' + ((count===0)? 'checked>' : '>') +
                        '<label for="baseLayerId_' + layerControlsSpec.map.id + '_' + layerControlsSpec.baseLayers[key].options.name + '">' +
                            layerControlsSpec.baseLayers[key].options.name + '</label>';
                    count++;
                }
            }

            $layerControlTitleBar.append($fieldset.html(radioHTML));

            //turn off the opacity slide and checkbox if there is only 1 base layer
            if($('#' + $fieldset[0].id + ' input[type=radio]').length < 2){
                $toggleDiv.css( 'visibility', 'hidden' );
                $cell.css( 'visibility', 'hidden' );
            }
            // register listener to update the map BaseLayer,
            // update base layer.
            $('.radio_'+layerControlsSpec.map.id).on('click', function(){
                var newBase,
                    bl = layerControlsSpec.baseLayers[this.value],
                    olMap_ = layerControlsSpec.map.map.olMap_,
                    spec = {},
                    k=0;

                if( bl.type==='BlankBase' ) {
                    $mapDiv.css( 'background-color', bl.options.color );
                    if(olMap_.baseLayer){
                        olMap_.baseLayer.setVisibility(false);
                    }
                    //turn off the opacity slide and checkbox
                    $toggleDiv.css( 'visibility', 'hidden' );
                    $cell.css( 'visibility', 'hidden' );
                }
                if( bl.type==='Google' ){
                    //reset the back-ground color to black
                    $mapDiv.css( 'background-color', 'rgb(0,0,0)' );
                    //turn on the opacity slide and checkbox
                    $toggleDiv.css( 'visibility', 'visible' );
                    $toggleBox.prop( 'checked', true );
                    $cell.css( 'visibility', 'visible' );
                    if(olMap_.baseLayer.options.name !== bl.options.name){
                        for(k; k<layerControlsSpec.baseLayers.length; k++){
                            if(layerControlsSpec.baseLayers[k].options.name === bl.options.name){
                                olMap_.baseLayer.destroy();
                                spec = layerControlsSpec.baseLayers[k];
                                newBase = layerControlsSpec.map.map.addLayer(aperture.geo.MapTileLayer.Google, {}, spec);
                                olMap_.baseLayer = newBase.olLayer_;
                                olMap_.setBaseLayer(newBase.olLayer_);
                                olMap_.baseLayer.setVisibility(true);
                                olMap_.zoomIn();
                                olMap_.zoomOut();
                                return true;
                            }
                        }
                    } else {
                        olMap_.baseLayer.setVisibility(true);
                    }
                }
            });

            //only display the base layer selector fieldset if there is more than one
            if($('#' + $fieldset[0].id + ' input[type=radio]').length < 2){
                $($fieldset).css('display', 'none');
            }
        }
        id = layerState.getId();
        controlsMap[id] = {
            controlSetRoot: $layerControlSetRoot,
            filterSlider: $filterSlider,
            opacitySlider: $opacitySlider,
            enabledCheckbox: $toggleBox,
            promotionButton: $promotionButton,
            settingsLink: $settingsButton
        };
    };

    /**
     * Displays a settings panel for a layer.
     *
     * @param {object} $parent - The parent node to attach the layer panel to.
     *
     * @param {object} layerState - The layer state model the panel will read from and update.
     */
    showLayerSettings = function ($parent, layerState) {

        var $settingsTitleBar,
            $settingsContent,
            name,
            span,
            $leftSpan,
            $rightSpan,
            $rampTypes,
            $rampFunctions,
            id,
            oldChildren,
            $backButton,
            i;

        // Save the main layer controls hierarchy
        oldChildren = replaceChildren($parent, null);

        // create title div
        $settingsTitleBar = $('<div class="settings-title"></div>');
        // add title span to div
        $settingsTitleBar.append($('<span class="layer-labels">' + layerState.getName() + '</span>'));
        $parent.append($settingsTitleBar);

        // create content div
        $settingsContent = $('<div class="settings-content"></div>');
        $parent.append($settingsContent);

        // create back button
        $backButton = $('<button class="settings-back-link">back</button>');
        $backButton.click(function () {
            replaceChildren($parent, oldChildren);
        });
        $settingsTitleBar.append($backButton);

        // add the ramp types radio buttons
        $rampTypes = $('<div class="settings-ramp-types"/>');
        // add title to ramp types div
        $rampTypes.append($('<div class="settings-ramp-title">Color Ramp</div>'));
        $settingsContent.append($rampTypes);
        // create left and right columns
        $leftSpan = $('<span class="settings-ramp-span-left"></span>');
        $rightSpan = $('<span class="settings-ramp-span-right"></span>');
        $rampTypes.append($leftSpan);
        $rampTypes.append($rightSpan);

        for (i=0; i<LayerState.RAMP_TYPES.length; i++) {
            // for each ramp type
            name = LayerState.RAMP_TYPES[i].name;
            id = LayerState.RAMP_TYPES[i].id;
            // add half types to left, and half to right
            span = (i < LayerState.RAMP_TYPES.length/2) ? $leftSpan : $rightSpan;
            span.append($('<div class="settings-values"></div>')
                    .append($('<input type="radio" name="ramp-types" value="' + id + '">')
                        .add($('<label for="' + id + '">' + name + '</label>')
                )
            ));
        }

        // Add the ramp function radio buttons
        $rampFunctions = $('<div class="settings-ramp-functions"/>');
        $rampFunctions.append($('<div class="settings-ramp-title">Color Scale</div>'));
        $settingsContent.append($rampFunctions);

        for (i=0; i<LayerState.RAMP_FUNCTIONS.length; i++) {
            name = LayerState.RAMP_FUNCTIONS[i].name;
            id = LayerState.RAMP_FUNCTIONS[i].id;
            $rampFunctions.append($('<div class="settings-values"></div>')
                            .append($('<input type="radio" name="ramp-functions" value="' + id + '">')
                                .add($('<label for="' + id + '">' + name + '</label>')
                )
            ));
        }

        // Set initial value based on layer state model
        $('input[name="ramp-types"][value="' + layerState.getRampType() + '"]').prop('checked', true);
        $('input[name="ramp-functions"][value="' + layerState.getRampFunction() + '"]').prop('checked', true);

        // Update model on button changes
        $('input[name="ramp-types"]').change(function () {
            layerState.setRampType($('input[name="ramp-types"]:checked').val());
        });

        $('input[name="ramp-functions"]').change(function () {
            layerState.setRampFunction($('input[name="ramp-functions"]:checked').val());
        });
    };

    /**
     * Creates an observer to handle layer state changes, and update the controls based on them.
     */
    makeLayerStateObserver = function (layerState, controlsMap, layerStateMap, $layersControlListRoot) {
        return function (fieldName) {
            if (fieldName === "enabled") {
                controlsMap[layerState.getId()].enabledCheckbox.prop("checked", layerState.isEnabled());
            } else if (fieldName === "opacity") {
                controlsMap[layerState.getId()].opacitySlider.slider("option", "value", layerState.getOpacity() * OPACITY_RESOLUTION);
            } else if (fieldName === "filterRange") {
                var range = layerState.getFilterRange();
                controlsMap[layerState.getId()].filterSlider.slider("option", "values", [range[0] * FILTER_RESOLUTION, range[1] * FILTER_RESOLUTION]);
            } else if (fieldName === "rampImageUrl") {
                controlsMap[layerState.getId()].filterSlider.css({'background': 'url(' + layerState.getRampImageUrl() + ')', 'background-size': '100%'});
            } else if (fieldName === "zIndex") {
                replaceLayers( sortLayers(layerStateMap), $layersControlListRoot, controlsMap );
            }
        };
    };

    /**
     * Replace the existing layer controls with new ones derived from the set of LayerState objects.  All the
     * new control references will be stored in the controlsMap for later access.
     *
     * @param {object} layerStateMap - A hash map of LayerState objects.
     * @param {object} $layerControlsListRoot  - The JQuery node that acts as the parent of all the layer controls.
     * @param {object} controlsMap - A map indexed by layer ID contain references to the individual layer controls.
     */
    replaceLayers = function (layerStateMap, $layerControlsContainer, controlsMap, layerControlsSpec ) {
        var i, key, sortedLayerStateList;
        sortedLayerStateList = sortLayers(layerStateMap);
        $layerControlsContainer.empty();
        // Clear out the controls map
        for (key in controlsMap) {
            if (controlsMap.hasOwnProperty(key)) {
                delete controlsMap[key];
            }
        }
        // Add layers - this will update the controls list.
        for (i = 0; i < sortedLayerStateList.length; i += 1) {
            addLayer(sortedLayerStateList, i, $layerControlsContainer, controlsMap, layerControlsSpec);
        }

        //set the content div height depending on the number of layers
        $($('#content')).css('height', ($('.layer-controls-layer').length * 90) + 'px');
    };

    /**
     * Converts the layer state map into an array and then sorts it based layer
     * z indices.
     *
     * @param layerStateMap - An object-based hash map of LayerState objects.
     * @returns {Array} - An array of LayerState objects sorted highest to lowest by z index.
     */
    sortLayers = function (layerStateMap) {
        var sortedList, layerState;
        // Sort the layers
        sortedList = [];
        for (layerState in layerStateMap) {
            if (layerStateMap.hasOwnProperty(layerState)) {
                sortedList.push(layerStateMap[layerState]);
            }
        }
        sortedList.sort(function (a, b) {
            return b.zIndex - a.zIndex;
        });
        return sortedList;
    };

    /** generates the filter axis major and minor tick marks
     *  note - 5 major and 4 minor tick marks will be created.
     * @param $filterAxis the tick mark container
     */
    generateFilterAxis = function ($filterAxis, layerControlsSpec) {
        var axisTicks = '<div class="filter-axis-tick-major filter-axis-tick-first"></div>', //the first tick
            major = false, //start with a minor tick
            majorCount = 1,
            numberOfInnerTicks = 7,
            i,
            $labelDiv = $('<div class="filter-axis-label-container"></div>');

        //create the inner ticks
        for(i = 0; i < numberOfInnerTicks; i++) {
            if(major) {
                axisTicks += '<div class="filter-axis-tick-major"></div>';
                majorCount++;
                major = !major;
            } else {
                axisTicks += '<div class="filter-axis-tick-minor"></div>';
                major = !major;
            }
        }

        //add the last tick
        axisTicks += '<div class="filter-axis-tick-major filter-axis-tick-last"></div>';
        $filterAxis.html('<div class="filter-axis-ticks-container">' + axisTicks + '</div>');
        $filterAxis.append($labelDiv);
        generateFilterAxisLabels(majorCount, $labelDiv, layerControlsSpec);
    };

    /** Generates the filter labels and their initial values.
     *
     * @param majorTicks the number of major tick marks
     * @param $labelDiv the label container
     */
    generateFilterAxisLabels = function(majorTicks, $labelDiv, layerControlsSpec){
        var html,
            val,
            increment,
            unitSpec,
            i;

        layerControlsSpec.$labelDiv = $labelDiv;

        unitSpec = {
            'allowStepDown' : true,
            'decimals' : 1,
            'type': 'b'
        };

        if (layerControlsSpec) {
            if (layerControlsSpec.levelMinFreq) {
                val = parseFloat(layerControlsSpec.levelMinFreq[layerControlsSpec.map.getZoom()]);
            } else if (layerControlsSpec.levelMinimums) {
                val = parseFloat(layerControlsSpec.levelMinimums[layerControlsSpec.map.getZoom()]);
            } else {
                val = 0;
            }

            if (layerControlsSpec.levelMaxFreq) {
                increment = (parseFloat(layerControlsSpec.levelMaxFreq[layerControlsSpec.map.getZoom()]) - val) / majorTicks;
            } else if (layerControlsSpec.levelMaximums) {
                increment = (parseFloat(layerControlsSpec.levelMaximums[layerControlsSpec.map.getZoom()]) - val) / majorTicks;
            } else {
                increment = 10 / majorTicks;
            }
        } else {
            val = 0;
            increment = 10 / majorTicks;
        }

        //start with the first label
        html = '<div class="filter-axis-label filter-axis-label-first">' + AxisUtil.formatText(val, unitSpec) + '</div>';

        //iterate over the inner labels
        for(i = 1; i < majorTicks; i++){
            val += increment;
            html += '<div class="filter-axis-label">' + AxisUtil.formatText(val, unitSpec) + '</div>';
        }

        //add the last label
        val += increment;
        html += '<div class="filter-axis-label filter-axis-label-last">' + AxisUtil.formatText(val, unitSpec) + '</div>';

        $labelDiv.html(html);
    };

    //called on map zoom event
    updateAxisLabels = function(layerControlsSpec){
        var $labels = layerControlsSpec.$labelDiv,
            val,
            increment,
            unitSpec,
            i,
            length = $labels === undefined ? 0 : $labels[0].children.length;

        unitSpec = {
            'allowStepDown' : true,
            'decimals' : 1,
            'type': 'b'
        };

        if (layerControlsSpec) {
            if (layerControlsSpec.levelMinFreq) {
                val = parseFloat(layerControlsSpec.levelMinFreq[layerControlsSpec.map.getZoom()]);
            }
            else if (layerControlsSpec.levelMinimums) {
                val = parseFloat(layerControlsSpec.levelMinimums[layerControlsSpec.map.getZoom()]);
            }
            else {
                val = 0;
            }

            if (layerControlsSpec.levelMaxFreq) {
                increment = (parseFloat(layerControlsSpec.levelMaxFreq[layerControlsSpec.map.getZoom()]) - val) / length;
            } else if (layerControlsSpec.levelMaximums) {
                increment = (parseFloat(layerControlsSpec.levelMaximums[layerControlsSpec.map.getZoom()]) - val) / length;
            } else {
                increment = 10 / length;
            }
        } else {
            val = 0;
            increment = 10 / length;
        }
        for(i = 0; i < length; i++){
            $labels[0].children[i].innerHTML = AxisUtil.formatText(val, unitSpec);
            val += increment;
        }
    };

    LayerControls = Class.extend({
        ClassName: "LayerControls",

        /**
         * Initializes the layer controls by modifying the DOM tree, and registering
         * callbacks against the LayerState obj
         *
         * @param layerStateMap - The map layer the layer controls reflect and modify.
         */
        init: function ( controlsId, layerStateMap, layerControlsSpec ) {
            var layerState;

            // "Private" vars
            this.controlsMap = {};
            this.layerControlsSpec = layerControlsSpec;

            if (this.layerControlsSpec) {

                //create a listener for updating the filter axis labels
                this.layerControlsSpec.map.on('zoomend', function () {
                    updateAxisLabels( layerControlsSpec );
                });

                //add a default base layer
                if (!Array.isArray(layerControlsSpec.baseLayers) || layerControlsSpec.baseLayers.length < 1 || !layerControlsSpec.baseLayers[0].type){
                    layerControlsSpec.baseLayers = [];
                    layerControlsSpec.baseLayers.push({
                        "type" : "BlankBase",
                        "options" : {
                            "name" : "black",
                            "color" : "rgb(0,0,0)"
                        }
                    });
                }
            }

            // Add the title
            this.$layerControlsContainer = $('#'+controlsId);

            // Add layers visuals and register listeners against the model
            replaceLayers(layerStateMap, this.$layerControlsContainer, this.controlsMap, this.layerControlsSpec);
            for (layerState in layerStateMap) {
                if (layerStateMap.hasOwnProperty(layerState)) {
                    layerStateMap[layerState].addListener(makeLayerStateObserver(
                        layerStateMap[layerState],
                        this.controlsMap,
                        layerStateMap,
                        this.$layerControlsContainer
                    ));
                }
            }
        },

        noop: function() {
            return true;
        }

    });

    return LayerControls;
});
