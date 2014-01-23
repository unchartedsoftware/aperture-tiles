/**
 * Created by Chris Bethune on 16/01/14.
 */

/*global $, define */

define(['class'], function (Class) {
    "use strict";

    var LayerControls, controlsMap, $root, $layerControlsListRoot, $tileOutlineButton,
        addLayer,
        OPACITY_RESOLUTION, FILTER_RESOLUTION;

    // constant initialization
    OPACITY_RESOLUTION = 100.0;
    FILTER_RESOLUTION = 100.0;

    addLayer = function (id, name, hasFilter, $parentElement) {
        var $sliderTableRow, $sliderTable, $subTable, $subTableRow, $cell, $filterSlider,
            $opacitySlider, $enabledCheckbox, $promotionButton, $settings, $layerControlsRoot,
            className;

        $layerControlsRoot = $('<div id="layer-controls-' + id + '"></div>');

        $settings = $('<a href="">settings</a>');
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

        // Add sub-table to hold sliders
        $subTable = $('<table style="width:100%"></table>');
        $sliderTableRow.append($('<td></td>').append($subTable));

        $subTableRow = $('<tr></tr>');
        $subTable.append($subTableRow);

        // Add the opacity slider
        className = hasFilter ? "opacity-slider" : "base-opacity-slider";
        $cell = $('<td class="' + className + '"></td>');
        $subTableRow.append($cell);

        $cell.append($('<div class="slider-label">Opacity</div>'));
        $opacitySlider = $('<div id="' + "opacity_slider_" + name + '"></div>').slider({
            range: "min",
            min: 0,
            max: OPACITY_RESOLUTION,
            value: OPACITY_RESOLUTION
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
                values: [0, FILTER_RESOLUTION]
            }));
            // Disable the background for the range slider
            $(".ui-slider-range", $filterSlider).css({"background": "none"});

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
        init: function (mapLayer) {
            controlsMap = {};

            $root = $('#layer-controls');

            // Add the title
            $root.append($('<div id="layer-control-title" class="title">Layer Controls</div>'));

            // Add the layer control list area
            $layerControlsListRoot = $('<div id="layer-control-list"></div>');
            $root.append($layerControlsListRoot);

            // Add the base layer slider
            addLayer("base", "Base Layer", false, $root);

            // Add the outline toggle button
            $tileOutlineButton = $('<button class="tile-outline-button">Toggle Tile Outline</button>');
            $root.append($tileOutlineButton);
        },

        /**
         * Adds a set of layer controls to the panel.
         *
         * @param id
         *      The id of the layer the control is bound to.
         *
         * @param name
         *      The name to display.
         */
        addLayerControls: function (id, name) {
            addLayer(id, name, true, $layerControlsListRoot);
        },

        /**
         *
         * @param id
         */
        removeLayerControls: function (id) {
            controlsMap[id].controlSetRoot.remove();
            delete controlsMap[id];
        },

        /**
         *
         * @param id
         * @param handler
         */
        setEnableHandler: function (id, handler) {
            controlsMap[id].enabledCheckbox.click(handler);
        },

        /**
         *
         * @param id
         * @param handler
         */
        setSettingsHandler: function (id, handler) {
            controlsMap[id].settings.click(handler);
        },

        /**
         *
         * @param id
         * @param change
         * @param slide
         */
        setOpacityHandlers: function (id, change, slide) {
            controlsMap[id].opacitySlider.on("sliderchange", change);
            controlsMap[id].opacitySlider.on("slide", slide);
        },

        /**
         *
         * @param id
         * @param change
         * @param slide
         */
        setFilterHandlers: function (id, change, slide) {
            controlsMap[id].filterSlider.on("sliderchange", change);
            controlsMap[id].filterSlider.on("slide", slide);
        },

        /**
         *
         * @param id
         */
        isEnabled: function (id) {
            return controlsMap[id].enabledCheckbox.prop("checked");
        },

        /**
         *
         * @param id
         * @param enabled
         */
        setEnabled: function (id, enabled) {
            controlsMap[id].enabledCheckbox.prop("checked", enabled);
        },

        /**
         *
         * @param id
         */
        getOpacity: function (id) {
            return controlsMap[id].opacitySlider.slider("option", "value") / OPACITY_RESOLUTION;
        },

        /**
         *
         * @param id
         * @param opacity
         */
        setOpacity: function (id, opacity) {
            if (!isNaN(opacity)) {
                controlsMap[id].opacitySlider.slider("option", "value", opacity * OPACITY_RESOLUTION);
            }
        },

        /**
         *
         * @param id
         */
        getFilterRange: function (id) {
            var result = controlsMap[id].filterSlider.slider("option", "values");
            return [result[0] / FILTER_RESOLUTION, result[1] / FILTER_RESOLUTION];
        },

        /**
         *
         * @param id
         * @param range
         */
        setFilterRange: function (id, range) {
            controlsMap[id].filterSlider.slider("option", "values", [range[0] * FILTER_RESOLUTION, range[1] * FILTER_RESOLUTION]);
        },

        /**
         *
         * @param id
         */
        getFilterColorRamp: function (id) {
            return $(".ui-slider-range", controlsMap[id].filterSlider).css("background");
        },

        /**
         *
         * @param id
         * @param imageUrl
         */
        setFilterRangeImage: function (id, imageUrl) {
            $(".ui-slider-range", controlsMap[id].filterSlider).css({'background': 'url(' + imageUrl + ')', 'background-size': '100%'});
        }
    });

    return LayerControls;
});