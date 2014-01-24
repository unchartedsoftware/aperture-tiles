/**
 * Created by Chris Bethune on 16/01/14.
 */

/*global $, define, console */

define(['class'], function (Class) {
    "use strict";

    var LayerControls, addLayer, OPACITY_RESOLUTION, FILTER_RESOLUTION;

    // constant initialization
    OPACITY_RESOLUTION = 100.0;
    FILTER_RESOLUTION = 100.0;

    /**
     * Adds a new set of layer controls to the panel.
     *
     * @param layerState
     *      The layer state model the controls are bound to.
     *
     * @param $parentElement
     *      The parent element in the document tree to add the controls to.
     */
    addLayer = function (layerState, $parentElement, controlsMap) {
        var $sliderTableRow, $sliderTable, $subTable, $subTableRow, $cell, $filterSlider,
            $opacitySlider, $enabledCheckbox, $promotionButton, $settings, $layerControlsRoot,
            className, hasFilter, name, filterRange, id;

        $layerControlsRoot = $('<div id="layer-controls-' + layerState.getId() + '"></div>');

        $settings = $('<a href="">settings</a>');
        name = layerState.getName();
        name = name === undefined ||  name === "" ? layerState.getId() : layerState.getName();
        $layerControlsRoot.append($('<table style="width:100%"></table>')
            .append($('<tr></tr>')
                .append($('<td class="labels"></td>')
                    .append($('<span>' + name + '</span>')))
                .append($('<td class="settings-link"></td>')
                    .append($settings))));

        // Table for checkbox + sliders
        $sliderTable = $('<table style="width:100%"></table>');
        $layerControlsRoot.append($sliderTable);

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

        $parentElement.append($layerControlsRoot);

        id = layerState.getId();
        controlsMap[id] = {
            controlSetRoot: $layerControlsRoot,
            filterSlider: $filterSlider,
            opacitySlider: $opacitySlider,
            enabledCheckbox: $enabledCheckbox,
            promotionButton: $promotionButton,
            settingsLink: $settings
        };
    };

    LayerControls = Class.extend({
        ClassName: "LayerControls",

        /**
         * Initializes the layer controls.
         *
         * @param mapLayer
         *      The map layer the layer controls reflect and modify.
         */
        init: function (layerStateMap) {
            // "Private" vars
            this.controlsMap = {};
            this.$root = null;
            this.$layerControlsListRoot = null;
            this.$tileOutlineButton = null;;

            var layerState, makeLayerStateObserver;

                // Add the title
            this.$root = $('#layer-controls');
            this.$root.append($('<div id="layer-control-title" class="title">Layer Controls</div>'));

            // Add the layer control list area
            this.$layerControlsListRoot = $('<div id="layer-control-list"></div>');
            this.$root.append(this.$layerControlsListRoot);

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
                }
            };

            // Add the layers
            for (layerState in layerStateMap) {
                if (layerStateMap.hasOwnProperty(layerState)) {
                    addLayer(layerStateMap[layerState], this.$layerControlsListRoot, this.controlsMap);
                    layerStateMap[layerState].addCallback(makeLayerStateObserver(layerStateMap[layerState], this.controlsMap));
                }
            }

            // Add the outline toggle button
            this.$tileOutlineButton = $('<button class="tile-outline-button">Toggle Tile Outline</button>');
            this.$root.append(this.$tileOutlineButton);

        }
    });

    return LayerControls;
});