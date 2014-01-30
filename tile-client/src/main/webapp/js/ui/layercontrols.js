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
 *
 * TODO:
 *
 * 1) This class provides buttons for ordering layers, but does not yet implement the backing
 * functionality.
 * 2) This class provides a button for toggling tile outline visiblity, but doesn't yet supply
 * a means to register a handler.
 */
define(function (require) {
    "use strict";

    var Class = require('../class'),
        LayerState = require('../layerstate'),
        LayerControls, addLayer, showLayerSettings, OPACITY_RESOLUTION, FILTER_RESOLUTION, replaceChildren;

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
    addLayer = function (layerState, $parentElement, $root, controlsMap) {
        var $sliderTableRow, $sliderTable, $subTable, $subTableRow, $cell, $filterSlider,
            $opacitySlider, $enabledCheckbox, $promotionButton, $settings, $layerControlSetRoot,
            className, hasFilter, name, filterRange, id;

        $layerControlSetRoot = $('<div id="layer-controls-' + layerState.getId() + '"></div>');

        $settings = $('<a>settings</a>');
        $settings.click(function () {
            showLayerSettings($root, layerState);
        });

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
        hasFilter = filterRange[0] >= 0 && filterRange[1] >= 0;
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
            $filterSlider = ($('<div id="' + "filter_slider_" + name + '"></div>').slider({
                range: true,
                min: 0,
                max: FILTER_RESOLUTION,
                values: [filterRange[0] * FILTER_RESOLUTION, filterRange[1] * FILTER_RESOLUTION],
                change: function () {
                    var result = $filterSlider.slider("option", "values");
                    layerState.setFilterRange([result[0] / FILTER_RESOLUTION, result[1] / FILTER_RESOLUTION]);
                }
            }));
            // Disable the background for the range slider
            $(".ui-slider-range", $filterSlider).css({"background": "none"});

            // Set the ramp image
            $(".ui-slider-range", $filterSlider).css({'background': 'url(' + layerState.getRampImageUrl() + ')', 'background-size': '100%'});

            $cell.append($filterSlider);
        } else {
            $filterSlider = null;
        }

        // Add the promotion button
        $cell = $('<td></td>');
        $subTableRow.append($cell);
        $promotionButton = $('<button class="layer-promotion-button" title="pop layer to top"></button>');
        $cell.append($promotionButton);

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
        var $settingsControls, $settingsTitleBar, name, $rampTypes, $rampFunctions, id, oldChildren, $back, i;

        // Save the main layer controls hierarchy
        oldChildren = replaceChildren($parent, null);

        $settingsControls = $('<div class="settings-controls"></div>');

        $settingsTitleBar = $('<div class="settings-title-bar"></div>');
        $settingsControls.append($settingsTitleBar);

        $settingsTitleBar.append($('<span class="settings-title">' + layerState.getName() + ' Layer Settings</span>'));

        $back = $('<span class="settings-back-link"> back </span>');
        $settingsTitleBar.append($back);
        $back.click(function () {
            replaceChildren($parent, oldChildren);
        });

        // Add the ramp types radio buttons
        $rampTypes = $('<div id="ramp-types" class="settings-ramp-types"/>');
        $settingsControls.append($rampTypes);
        $rampTypes.append($('<div class="settings-ramp-title">Color Ramp</div>'));
        for (i=0; i<LayerState.RAMP_TYPES.length; ++i) {
            name = LayerState.RAMP_TYPES[i].name;
            id = LayerState.RAMP_TYPES[i].id;
            $rampTypes.append($('<div><input class="settings-values" type="radio" name="ramp-types" value="'
                                + id + '">' + name + '</input></div>'));
        }

        // Add the ramp function radio buttons
        $rampFunctions = $('<div id="ramp-functions" class="settings-ramp-functions"/>');
        $settingsControls.append($rampFunctions);
        $rampFunctions.append($('<span class="settings-ramp-title">Color Scale</span>'));
        for (i=0; i<LayerState.RAMP_FUNCTIONS.length; ++i) {
            name = LayerState.RAMP_FUNCTIONS[i].name;
            id = LayerState.RAMP_FUNCTIONS[i].id;
            $rampFunctions.append($('<div><input class="settings-values" type="radio" name="ramp-functions" value="'
                                    + id + '">' + name + '</input></div>'));
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
            layerState.setRampFunction($('input[name="ramp-types"]:checked').val());
        });
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
            // "Private" vars
            this.controlsMap = {};
            this.$root = null;
            this.$layerControlsListRoot = null;
            this.$tileOutlineButton = null;
            this.$layerControlsRoot = null;

            var layerState, makeLayerStateObserver;

            // Add the title
            this.$root = $('#layer-controls');

            this.$layerControlsRoot = $('<div id="layer-controls-root"></div>');
            this.$root.append(this.$layerControlsRoot);

            this.$layerControlsRoot.append($('<div id="layer-control-title" class="title">Layer Controls</div>'));

            // Add the layer control list area
            this.$layerControlsListRoot = $('<div id="layer-control-list"></div>');
            this.$layerControlsRoot.append(this.$layerControlsListRoot);

            // Creates a layer state observer, which will update the control panel in response to model changes.
            makeLayerStateObserver = function (layerState, controlsMap) {
                return function (fieldName) {
                    if (fieldName === "enabled") {
                        controlsMap[layerState.getId()].enabledCheckbox.prop("checked", layerState.isEnabled());
                    } else if (fieldName === "opacity") {
                        controlsMap[layerState.getId()].opacitySlider.slider("option", "value", layerState.getOpacity() * OPACITY_RESOLUTION);
                    } else if (fieldName === "filterRange") {
                        var range = layerState.getFilterRange();
                        controlsMap[layerState.getId()].filterSlider.slider("option", "values", [range[0] * FILTER_RESOLUTION, range[1] * FILTER_RESOLUTION]);
                    } else if (fieldName === "rampImageUrl") {
                        $(".ui-slider-range", controlsMap[layerState.getId()].filterSlider).css({'background': 'url(' + layerState.getRampImageUrl() + ')', 'background-size': '100%'});
                    }
                };
            };

            // Add the layers
            for (layerState in layerStateMap) {
                if (layerStateMap.hasOwnProperty(layerState)) {
                    addLayer(layerStateMap[layerState], this.$layerControlsListRoot, this.$root, this.controlsMap);
                    layerStateMap[layerState].addListener(makeLayerStateObserver(layerStateMap[layerState], this.controlsMap));
                }
            }

            // Add the outline toggle button
            this.$tileOutlineButton = $('<button class="tile-outline-button">Toggle Tile Outline</button>');
            this.$layerControlsRoot.append(this.$tileOutlineButton);
            // TODO: Add a handler here that would call into externally registered callbacks.
        }
    });

    return LayerControls;
});
