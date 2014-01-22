/**
 * Created by Chris Bethune on 16/01/14.
 */

/*global $, define */

define(['class'], function (Class) {
    "use strict";

    var LayerControls, controlsMap;

    LayerControls = Class.extend({
        ClassName: "LayerControls",

        init: function () {
            controlsMap = {};
        },

        /**
         *
         * @param id
         * @param name
         */
        addLayerControls: function (id, name) {
            var $sliderTableRow, $sliderTable, $subTable, $subTableRow, $cell, $filterSlider,
                $opacitySlider, $enabledCheckbox, $promotionButton, $settings, $controlSetRoot;

            $controlSetRoot = $('<div id="' + id + '"></div>');

            $settings = $('<a href="">settings</a>');
            $controlSetRoot.append($('<table style="width:100%"></table>')
                    .append($('<tr></tr>')
                        .append($('<td class="labels"></td>')
                            .append($('<span>' + name + '</span>')))
                        .append($('<td class="settings-link"></td>')
                            .append($settings))));

            // Table for checkbox + sliders
            $sliderTable = $('<table style="width:100%"></table>');
            $controlSetRoot.append($sliderTable);

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
            $cell = $('<td class="opacity-slider"></td>');
            $subTableRow.append($cell);

            $cell.append($('<div class="slider-label">Opacity</div>'));
            $opacitySlider = $('<div id="' + "opacity_slider_" + name + '"></div>').slider({
                range: "min",
                min: 0,
                max: 100,
                value: 10
            });
            $cell.append($opacitySlider);

            // Add the filter slider
            $cell = $('<td class="filter-slider"></td>');
            $subTableRow.append($cell);

            $cell.append($('<div class="slider-label">Filter</div>'));
            $filterSlider = ($('<div id="' + "filter_slider_" + name + '"></div>').slider({
                range: true,
                min: 0,
                max: 100,
                values: [10, 90]
            }));
            // Disable the background for the range slider
            $(".ui-slider-range", $filterSlider).css({"background": "none"});

            $cell.append($filterSlider);

            // Add the promotion button
            $cell = $('<td></td>');
            $subTableRow.append($cell);
            $promotionButton = $('<button class="layerPromotionButton" title="pop layer to top"></button>');
            $cell.append($promotionButton);

            $('#layer-controls').append($controlSetRoot);

            controlsMap[id] = {
                controlSetRoot: $controlSetRoot,
                filterSlider: $filterSlider,
                opacitySlider: $opacitySlider,
                enabledCheckbox: $enabledCheckbox,
                promotionButton: $promotionButton,
                settingsLink: $settings
            };
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
            return controlsMap[id].opacitySlider.slider("option", "value");
        },

        /**
         *
         * @param id
         * @param opacity
         */
        setOpacity: function (id, opacity) {
            controlsMap[id].opacitySlider.slider("option", "value", opacity);
        },

        /**
         *
         * @param id
         */
        getFilterRange: function (id) {
            return controlsMap[id].filterSlider.slider("option", "values");
        },

        /**
         *
         * @param id
         * @param range
         */
        setFilterRange: function (id, range) {
            controlsMap[id].filterSlider.slider("option", "values", range);
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