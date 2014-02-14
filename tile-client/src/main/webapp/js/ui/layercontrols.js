/**
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

    var Class = require('../class'),
        LayerState = require('../layerstate'),
        LayerControls,
        addLayer,
        showLayerSettings,
        OPACITY_RESOLUTION,
        FILTER_RESOLUTION,
        replaceChildren,
        makeLayerStateObserver,
        replaceLayers,
        sortLayers;

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
     * @param $root - The root control element.
     *
     * @param controlsMap - Maps layers to the sets of controls associated with them.
     */
    addLayer = function (sortedLayers, index, $parentElement, $root, controlsMap) {
        var $sliderTableRow, $sliderTable, $subTable, $subTableRow, $cell, $filterSlider,
            $opacitySlider, $enabledCheckbox, $promotionButton, $settings, $layerControlSetRoot,
            className, hasFilter, name, filterRange, id, layerState;

        layerState = sortedLayers[index];

        $layerControlSetRoot = $('<div id="layer-controls-' + layerState.getId() + '"></div>');

        if (layerState.getRampFunction() !== null && layerState.getRampType() !== null) {
            $settings = $('<a>settings</a>');
            $settings.click(function () {
                showLayerSettings($root, layerState);
            });
        }

        name = layerState.getName();
        name = name === undefined ||  name === "" ? layerState.getId() : layerState.getName();
        $layerControlSetRoot.append($('<table style="width:100%"></table>')
            .append($('<tr></tr>')
                .append($('<td class="layer_labels"></td>')
                    .append($('<span>' + name + '</span>')))
                .append($('<td class="settings-link"></td>')
                    .append($settings))));

        // Table for checkbox + sliders
        $sliderTable = $('<table style="width:100%"></table>');
        $layerControlSetRoot.append($sliderTable);

        // Add a table row
        $sliderTableRow = $('<tr></tr>');
        $sliderTable.append($sliderTableRow);

        // Add check box to the row
        $enabledCheckbox = $('<input type="checkbox" checked="checked"></td>');
        $sliderTableRow.append($('<td class="toggle">').append($enabledCheckbox));

        // Initialize the button from the model and register event handler.
        $enabledCheckbox.prop("checked", layerState.isEnabled());
        $enabledCheckbox.click(function () {
            layerState.setEnabled($enabledCheckbox.prop("checked"));
        });

        // Add sub-table to hold sliders
        $subTable = $('<table style="width:100%"></table>');
        $sliderTableRow.append($('<td></td>').append($subTable));

        $subTableRow = $('<tr></tr>');
        $subTable.append($subTableRow);

        // Add the opacity slider
        filterRange = layerState.getFilterRange();
        hasFilter = filterRange !== null && filterRange[0] >= 0 && filterRange[1] >= 0;
        className = hasFilter ? "opacity-slider" : "base-opacity-slider";
        $cell = $('<td class="' + className + '"></td>');
        $subTableRow.append($cell);

        $cell.append($('<div class="slider-label">Opacity</div>'));
        $opacitySlider = $('<div id="' + "opacity_slider_" + name + '"></div>').slider({
            range: "min",
            min: 0,
            max: OPACITY_RESOLUTION,
            value: layerState.getOpacity() * OPACITY_RESOLUTION,
            slide: function () {
                layerState.setOpacity($opacitySlider.slider("option", "value") / OPACITY_RESOLUTION);
            }
        });
        $cell.append($opacitySlider);

        // Add the filter slider
        if (hasFilter) {
            $cell = $('<td class="filter-slider"></td>');
            $subTableRow.append($cell);

            $cell.append($('<div class="slider-label">Filter</div>'));
            $filterSlider = $('<div id="' + "filter_slider_" + name + '"></div>');
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
        } else {
            $filterSlider = null;
        }

        // Add the promotion button
        if (layerState.getZIndex() !== null && layerState.getZIndex() >= 0) {
            $cell = $('<td></td>');
            $subTableRow.append($cell);
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
            $cell.append($promotionButton);
        }

        $parentElement.append($layerControlSetRoot);

        id = layerState.getId();
        controlsMap[id] = {
            controlSetRoot: $layerControlSetRoot,
            filterSlider: $filterSlider,
            opacitySlider: $opacitySlider,
            enabledCheckbox: $enabledCheckbox,
            promotionButton: $promotionButton,
            settingsLink: $settings
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
        var $settingsControls, $settingsTitleBar, name, $rampTypes, $rampFunctions, id, oldChildren, $back, rampType;

        // Save the main layer controls hierarchy
        oldChildren = replaceChildren($parent, null);

        $settingsControls = $('<div class="settings-controls"></div>');

        $settingsTitleBar = $('<div class="settings-title-bar"></div>');
        $settingsControls.append($settingsTitleBar);

        $settingsTitleBar.append($('<span class="settings-title">' + layerState.getName() + ' Layer Settings</span>'));

        $back = $('<span class="settings-back-link">back</span>');
        $settingsTitleBar.append($back);
        $back.click(function () {
            replaceChildren($parent, oldChildren);
        });

        // Add the ramp types radio buttons
        $rampTypes = $('<div id="ramp-types" class="settings-ramp-types"/>');
        $settingsControls.append($rampTypes);
        $rampTypes.append($('<div class="settings-ramp-title">Color Ramp</div>'));

        for (rampType in LayerState.RAMP_TYPES) {
            if (LayerState.RAMP_TYPES.hasOwnProperty(rampType)) {
                name = LayerState.RAMP_TYPES[rampType].name;
                id = LayerState.RAMP_TYPES[rampType].id;
                $rampTypes.append($('<div class="settings-values"></div>').append(
                    $('<input type="radio" name="ramp-types" value="' + id + '">').add(
                        $('<label for="' + id + '">' + name + '</label>')
                    )
                ));
            }
        }

        // Add the ramp function radio buttons
        $rampFunctions = $('<div id="ramp-functions" class="settings-ramp-functions"/>');
        $settingsControls.append($rampFunctions);
        $rampFunctions.append($('<span class="settings-ramp-title">Color Scale</span>'));
        for (rampType in LayerState.RAMP_FUNCTIONS) {
            if (LayerState.RAMP_FUNCTIONS.hasOwnProperty(rampType)) {
                name = LayerState.RAMP_FUNCTIONS[rampType].name;
                id = LayerState.RAMP_FUNCTIONS[rampType].id;
                $rampFunctions.append($('<div class="settings-values"></div>').append(
                    $('<input type="radio" name="ramp-functions" value="' + id + '">').add(
                        $('<label for="' + id + '">' + name + '</label>')
                    )
                ));
            }
        }
        $parent.append($settingsControls);

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
    makeLayerStateObserver = function (layerState, controlsMap, layerStateMap, $layersControlListRoot, $root) {
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
                replaceLayers(sortLayers(layerStateMap), $layersControlListRoot, $root, controlsMap);
            }
        };
    };

    /**
     * Replace the existing layer controls with new ones derived from the set of LayerState objects.  All the
     * new control references will be stored in the controlsMap for later access.
     *
     * @param {object} layerStateMap - A hash map of LayerState objects.
     * @param {object} $layerControlsListRoot  - The JQuery node that acts as the parent of all the layer controls.
     * @param {object} $root  - The root JQuery node of the entire layer control set.
     * @param {object} controlsMap - A map indexed by layer ID contain references to the individual layer controls.
     */
    replaceLayers = function (layerStateMap, $layerControlsListRoot, $root, controlsMap) {
        var i, key, sortedLayerStateList;
        sortedLayerStateList = sortLayers(layerStateMap);
        $layerControlsListRoot.empty();
        // Clear out the controls map
        for (key in controlsMap) {
            if (controlsMap.hasOwnProperty(key)) {
                delete controlsMap[key];
            }
        }
        // Add layers - this will update the controls list.
        for (i = 0; i < sortedLayerStateList.length; i += 1) {
            addLayer(sortedLayerStateList, i, $layerControlsListRoot, $root, controlsMap, sortedLayerStateList);
        }
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

    LayerControls = Class.extend({
        ClassName: "LayerControls",

        /**
         * Initializes the layer controls by modifying the DOM tree, and registering
         * callbacks against the LayerState obj
         *
         * @param layerStateMap - The map layer the layer controls reflect and modify.
         */
        initialize: function (layerStateMap) {
            var layerState;

            // "Private" vars
            this.controlsMap = {};
            this.$root = null;
            this.$layerControlsListRoot = null;
            this.$tileOutlineButton = null;
            this.$layerControlsRoot = null;

            // Add the title
            this.$root = $('#layer-controls');

            this.$layerControlsRoot = $('<div id="layer-controls-root"></div>');
            this.$root.append(this.$layerControlsRoot);

            this.$layerControlsRoot.append($('<div id="layer-control-title" class="title">Layer Controls</div>'));

            // Add the layer control list area
            this.$layerControlsListRoot = $('<div id="layer-control-list"></div>');
            this.$layerControlsRoot.append(this.$layerControlsListRoot);

            // Add layers visuals and register listeners against the model
            replaceLayers(layerStateMap, this.$layerControlsListRoot, this.$root, this.controlsMap);
            for (layerState in layerStateMap) {
                if (layerStateMap.hasOwnProperty(layerState)) {
                    layerStateMap[layerState].addListener(makeLayerStateObserver(
                        layerStateMap[layerState],
                        this.controlsMap,
                        layerStateMap,
                        this.$layerControlsListRoot,
                        this.$root
                    ));
                }
            }
        }
    });

    return LayerControls;
});
