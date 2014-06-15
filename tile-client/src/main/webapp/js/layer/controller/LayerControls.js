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
        AxisUtil = require('../../map/AxisUtil'),
        LayerControls,
        addLayer,
        showLayerSettings,
        createSettingsButton,
        createVisibilityButton,
        createOpacitySlider,
        createFilterSlider,
        createFilterAxis,
        createFilterAxisLabels,
        createPromotionButton,
        createBaseLayerButtons,
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
     * @param {Object} children - The children to replace.  Null will result in children being removed only.
     * @returns {Array} - The removed children.
     */
    replaceChildren = function ( $parent, children ) {
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


    createSettingsButton = function( $parentElement, layerState, controlsMapping ) {

        var $settingsButton = $('<button class="settings-link">settings</button>');
        $settingsButton.click(function () {
            showLayerSettings( $parentElement, layerState );
        });
        controlsMapping.settingsLink = $settingsButton;

        return $settingsButton;
    };

    createVisibilityButton = function( layerState, controlsMapping ) {

        var $toggleDiv = $('<div class="layer-toggle"></div>'),
            $toggleBox = $('<input type="checkbox" checked="checked">');

        $toggleDiv.append( $toggleBox );
        // Initialize the button from the model and register event handler.
        $toggleBox.prop("checked", layerState.isEnabled());
        $toggleBox.click(function () {
            layerState.setEnabled( $toggleBox.prop("checked") );
        });

        controlsMapping.enabledCheckbox = $toggleBox;

        return $toggleDiv;
    };

    createOpacitySlider = function( layerState, controlsMapping ) {

        var sliderClass = ( layerState.domain === 'server' ) ? "opacity-slider" : "base-opacity-slider",
            $opacitySliderContainer = $('<div class="' + sliderClass + '"></div>'),
            $opacitySliderLabel = $('<div class="slider-label">Opacity</div>'),
            $opacitySlider = $('<div class="opacity-slider-bar"></div>').slider({
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

        $opacitySliderContainer.append( $opacitySliderLabel );
        $opacitySliderContainer.append( $opacitySlider );

        controlsMapping.opacitySlider = $opacitySlider;

        return $opacitySliderContainer;
    };

    createFilterSlider = function( layerState, controlsMapping ) {

        var filterRange = layerState.getFilterRange(),
            $filterSliderContainer = $('<div class="filter-slider"></div>'),
            $filterLabel = $('<div class="slider-label">Filter</div>'),
            $filterSlider = $('<div class="filter-slider-img"></div>'),
            $filterAxis;

        $filterSliderContainer.append( $filterLabel );

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
        $( ".ui-slider-range", $filterSlider ).css({"background": "none"});

        // Set the ramp image
        $filterSlider.css({'background': 'url(' + layerState.getRampImageUrl() + ')', 'background-size': '100%'});
        //create the filter axis
        $filterAxis = createFilterAxis( layerState.getRampMinMax() );

        $filterSliderContainer.append( $filterSlider );
        $filterSliderContainer.append( $filterAxis );

        controlsMapping.filterSlider = $filterSlider;
        controlsMapping.filterAxis = $filterAxis;

        return $filterSliderContainer;
    };

    /** generates the filter axis major and minor tick marks
     *  note - 5 major and 4 minor tick marks will be created.
     * @param $filterAxis the tick mark container
     */
    createFilterAxis = function ( minMax ) {

        var axisTicks = '<div class="filter-axis-tick-major filter-axis-tick-first"></div>', //the first tick
            major = false, // next tick is a minor tick
            majorCount = 1,
            numberOfInnerTicks = 7,
            $filterAxis = $('<div class="filter-axis"></div>'),
            $filterAxisTicksContainer = $('<div class="filter-axis-ticks-container"></div>'),
            $filterAxisLabelContainer = $('<div class="filter-axis-label-container"></div>'),
            i;

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

        $filterAxisTicksContainer.append( axisTicks );
        $filterAxisLabelContainer.append( createFilterAxisLabels( majorCount, minMax ) );

        $filterAxis.append( $filterAxisTicksContainer );
        $filterAxis.append( $filterAxisLabelContainer );

        return $filterAxis;

    };

    /** Generates the filter labels and their initial values.
     *
     * @param majorTicks the number of major tick marks
     * @param $labelDiv the label container
     */
    createFilterAxisLabels = function( majorTicks, minMax ){
        var val = minMax[0],
            increment = ( minMax[1] - minMax[0] ) / majorTicks,
            unitSpec = {
                'allowStepDown' : true,
                'decimals' : 1,
                'type': 'b'
            },
            html,
            i;

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

        return $(html);
    };

    createPromotionButton = function( layerState, nextLayerState, controlsMapping ) {

        var $promotionDiv = $('<div class="promotion-container"></div>'),
            $promotionButton = $('<button class="layer-promotion-button" title="pop layer to top"></button>');

        $promotionButton.click(function () {
            var otherZ;
            if ( nextLayerState ) {
                otherZ = nextLayerState.getZIndex();
                nextLayerState.setZIndex( layerState.getZIndex() );
                layerState.setZIndex(otherZ);
            }
        });
        $promotionDiv.append( $promotionButton );

        controlsMapping.promotionButton = $promotionButton;

        return $promotionDiv;
    };


    createBaseLayerButtons = function( $layerContent, layerState, controlsMapping ) {

        var $baseLayerButtonSet = $('<fieldset class="baselayer-fieldset"></fieldset>'),
            $radioButton,
            $radioLabel,
            baseLayer,
            i;

        function onClick() {
            var index = $(this).val();
            layerState.setBaseLayerIndex( index );
        }

        for (i=0; i<layerState.BASE_LAYERS.length; i++) {

            baseLayer = layerState.BASE_LAYERS[i];

            if (baseLayer.type === "BlankBase") {
                $layerContent.css( 'display', 'none' );
            } else {
                $layerContent.css( 'display', 'block' );
            }

            // create radio button
            $radioButton = $( '<input type="radio" class="baselayer-radio-button" name="baselayer-radio-button" id="'+(baseLayer.options.name+i)+'"'
                            + 'value="' + i + '"' + ( ( i === layerState.getBaseLayerIndex() )? 'checked>' : '>'));
            $radioButton.on( 'click', onClick );
            // create radio label
            $radioLabel = $('<label for="'+(baseLayer.options.name+i)+'">' + baseLayer.options.name + '</label>');
            $baseLayerButtonSet.append( $radioButton ).append( $radioLabel );
        }

        controlsMapping.baseLayerButtonSet = $baseLayerButtonSet;

        return $baseLayerButtonSet;
    };


    /**
     * Adds a new set of layer controls to the panel.
     *
     * @param layerState - The layer state model the controls are bound to.
     *
     * @param $layerControlsContainer - The parent element in the document tree to add the controls to.
     *
     * @param controlsMap - Maps layers to the sets of controls associated with them.
     */
    addLayer = function ( sortedLayers, index, $layerControlsContainer, controlsMap ) {
        var layerState = sortedLayers[index],
            name = layerState.getName() || layerState.getId(),
            $layerControlRoot,
            $layerControlTitleBar,
            $layerContent,
            layerStateId,
            controlsMapping;

        layerStateId = layerState.getId();

        if ( controlsMap[layerStateId] ) {
            // ensure only one layer controls UI is created per ID, this is used for
            // multiple client layers which will share ids but offer different renderings
            return;
        }

        controlsMap[layerStateId] = {};
        controlsMapping = controlsMap[layerStateId];

        // create layer root
        $layerControlRoot = $('<div id="layer-controls-' + layerStateId + '" class="layer-controls-layer"></div>');
        // create title div
        $layerControlTitleBar = $('<div class="layer-title"><span class="layer-labels">' + name + '</span></div>');

        $layerControlRoot.append( $layerControlTitleBar );

        // create content div
        $layerContent = $('<div class="layer-content"></div>');
        $layerControlRoot.append( $layerContent );

        // create settings button, only for server layers
        if ( layerState.domain === 'server' ) {
            $layerControlTitleBar.append( createSettingsButton( $layerControlsContainer, layerState, controlsMapping ) );
        }

        // add visibility toggle box
        $layerContent.append( createVisibilityButton( layerState, controlsMapping ) );

        // add opacity slider
        $layerContent.append( createOpacitySlider( layerState, controlsMapping ) );

        if ( layerState.domain === 'server' ) {
            // add filter slider
            $layerContent.append( createFilterSlider( layerState, controlsMapping ) );
            // add layer promotion button
            $layerContent.append( createPromotionButton( layerState, sortedLayers[index - 1] || null, controlsMapping ) );
        }

        //add base layer radio buttons when this layer is the base layer
        if( layerState.domain === "base" && layerState.BASE_LAYERS.length > 1 ) {
            $layerControlTitleBar.append( createBaseLayerButtons( $layerContent, layerState, controlsMapping) );
        }

        $layerControlsContainer.append( $layerControlRoot );
    };

    /**
     * Displays a settings panel for a layer.
     *
     * @param {object} $parent - The parent node to attach the layer panel to.
     * @param {object} layerState - The layer state model the panel will read from and update.
     */
    showLayerSettings = function( $parent, layerState ) {

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

        for (i=0; i<layerState.RAMP_TYPES.length; i++) {
            // for each ramp type
            name = layerState.RAMP_TYPES[i].name;
            id = layerState.RAMP_TYPES[i].id;
            // add half types to left, and half to right
            span = (i < layerState.RAMP_TYPES.length/2) ? $leftSpan : $rightSpan;
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

        for (i=0; i<layerState.RAMP_FUNCTIONS.length; i++) {
            name = layerState.RAMP_FUNCTIONS[i].name;
            id = layerState.RAMP_FUNCTIONS[i].id;
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
    makeLayerStateObserver = function (layerState, controlsMap, layerStates, $layersControlListRoot) {
        return function (fieldName) {

            var controlsMapping = controlsMap[ layerState.getId() ],
                range;

            if (fieldName === "enabled") {
                controlsMapping.enabledCheckbox.prop("checked", layerState.isEnabled());
            } else if (fieldName === "opacity") {
                controlsMapping.opacitySlider.slider("option", "value", layerState.getOpacity() * OPACITY_RESOLUTION);
            } else if (fieldName === "filterRange") {
                range = layerState.getFilterRange();
                controlsMapping.filterSlider.slider("option", "values", [range[0] * FILTER_RESOLUTION, range[1] * FILTER_RESOLUTION]);
            } else if (fieldName === "rampImageUrl") {
                controlsMapping.filterSlider.css({'background': 'url(' + layerState.getRampImageUrl() + ')', 'background-size': '100%'});
            } else if (fieldName === "zIndex") {
                replaceLayers( sortLayers(layerStates), $layersControlListRoot, controlsMap );
            } else if (fieldName === "rampMinMax") {
                controlsMapping.filterAxis.html( createFilterAxis( layerState.getRampMinMax() ).children() );
            }
        };
    };

    /**
     * Replace the existing layer controls with new ones derived from the set of LayerState objects.  All the
     * new control references will be stored in the controlsMap for later access.
     *
     * @param {object} layerStates - An array map of LayerState objects.
     * @param {object} $layerControlsListRoot  - The JQuery node that acts as the parent of all the layer controls.
     * @param {object} controlsMap - A map indexed by layer ID contain references to the individual layer controls.
     */
    replaceLayers = function ( layerStates, $layerControlsContainer, controlsMap ) {
        var sortedLayerStates = sortLayers( layerStates ),
            i, key;

        $layerControlsContainer.empty();
        // Clear out any existing the controls map
        for (key in controlsMap) {
            if (controlsMap.hasOwnProperty(key)) {
                delete controlsMap[key];
            }
        }

        // Add layers - this will update the controls list.
        for (i = 0; i < sortedLayerStates.length; i += 1) {
            addLayer( sortedLayerStates, i, $layerControlsContainer, controlsMap );
        }

        //set the content div height depending on the number of layers
        $($('#content')).css('height', ($('.layer-controls-layer').length * 90) + 'px');
    };

    /**
     * Converts the layer state map into an array and then sorts it based layer
     * z indices.
     *
     * @param layerStates - An array of LayerState objects.
     * @returns {Array} - An array of LayerState objects sorted highest to lowest by z index.
     */
    sortLayers = function (layerStates) {

        var arrayCopy = layerStates.concat();

        arrayCopy.sort( function (a, b) {
            return b.zIndex - a.zIndex;
        });
        return arrayCopy;
    };




    LayerControls = Class.extend({
        ClassName: "LayerControls",

        /**
         * Initializes the layer controls by modifying the DOM tree, and registering
         * callbacks against the LayerState obj
         *
         * @param layerStates - The list of layers the layer controls reflect and modify.
         */
        init: function ( controlsId, layerStates ) {

            var i;

            // "Private" vars
            this.controlsMap = {};

            // find the container
            this.$layerControlsContainer = $('#'+controlsId);

            // Add layers visuals and register listeners against the model
            replaceLayers( layerStates, this.$layerControlsContainer, this.controlsMap );

            for (i=0; i<layerStates.length; i++) {

                layerStates[i].addListener( makeLayerStateObserver(
                    layerStates[i],
                    this.controlsMap,
                    layerStates,
                    this.$layerControlsContainer
                ));
            }
        },

        noop: function() {
            return true;
        }

    });

    return LayerControls;
});
