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

    var Class = require('../class'),
        AxisUtil = require('../map/AxisUtil'),
        LayerControls,
        addLayer,
        showLayerSettings,
        createSettingsButton,
        createVisibilityButton,
        createOpacitySlider,
        createFilterSlider,
        createFilterAxis,
        createFilterAxisLabels,
        createBaseLayerButtons,
        addLayerDragCallbacks,
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
     * Replaces node's children and returns the replaced for storage. Fades out old content,
     * animates resizing to new container size, then fades in new contents.
     *
     * @param {JQuery} $layerControlsContainer - The node to remove the children from.
     * @param {Object} children - The children to replace.  Null will result in children being removed only.
     * @returns {Array} - The removed children.
     */
    replaceChildren = function ( $layerControlsContainer, children ) {

        var removed,
            previousHeight = $layerControlsContainer.outerHeight();

        function fadeOutChildren( container, callback ) {
            // animate and set the current contents to 0 opacity
            container.children().animate( {"opacity":0}, 100 )
                .promise()
                    .done( callback );
        }

        function swapChildren( container, newChildren ) {
            var removed;
            // first set the child elements to 0 opacity
            newChildren.css( "opacity", 0 );
            // Remove existing children.
            removed = container.children();
            removed.detach();
            // Add in new children.
            if ( newChildren ) {
                newChildren.appendTo( container );
            }
            return removed;
        }

        function animateToNewSize( container, previousHeight, callback ) {
            var newHeight = container.outerHeight();
            container.css( "height", previousHeight );
            container.animate( {"height": newHeight} )
                .promise()
                    .done( callback );
        }

        function fadeInChildren( container ) {
            container.children().animate( {"opacity":1}, 100 )
                .promise()
                    .done( function() {
                          container.css( "height", "100%" );
                    });
        }

        // fade current children out
        fadeOutChildren( $layerControlsContainer, function() {
            // swap old children with new children
            removed = swapChildren( $layerControlsContainer, children );
            // resize parent container
            animateToNewSize( $layerControlsContainer, previousHeight, function() {
                // fade new children in
                fadeInChildren( $layerControlsContainer );
            });
        });
        return removed;
    };

    /**
     * Creates and returns a jquery element object for the settings menu button.
     *
     * @param {Object} $layerControlsContainer - The layer controls container element.
     * @param {Object} layerState - The layerstate object for the respective layer.
     * @param {Object} controlsMapping - The control mapping from the layerstate layer id to the associated control elements.
     * @returns {JQuery} - The created element wrapped in a jquery object.
     */
    createSettingsButton = function( $layerControlsContainer, $layerContent, layerState, controlsMapping ) {

        var $settingsButton = $('<button class="settings-link">settings</button>');
        $settingsButton.click(function () {
            showLayerSettings( $layerControlsContainer, $layerContent, layerState );
        });
        controlsMapping.settingsLink = $settingsButton;
        return $settingsButton;
    };

    /**
     * Creates and returns a jquery element object for the layer visibility toggle box.
     *
     * @param {Object} layerState - The layerstate object for the respective layer.
     * @param {Object} controlsMapping - The control mapping from the layerstate layer id to the associated control elements.
     * @returns {JQuery} - The created element wrapped in a jquery object.
     */
    createVisibilityButton = function( layerState, controlsMapping ) {

        var $toggleDiv = $('<div class="layer-toggle"></div>'),
            $toggleBox = $('<input type="checkbox" checked="checked" id="layer-toggle-box-' + layerState.getId() + '">')
                             .add($('<label for="layer-toggle-box-' + layerState.getId() + '"></label>'));

        $toggleDiv.append( $toggleBox );
        // Initialize the button from the model and register event handler.
        $toggleBox.prop("checked", layerState.isEnabled());
        $toggleBox.click(function () {
            var value = $toggleBox.prop("checked");
            layerState.setEnabled( value );
            if (layerState.domain === "client") {
                layerState.setCarouselEnabled( value );
            }
        });
        controlsMapping.enabledCheckbox = $toggleBox;
        return $toggleDiv;
    };

    /**
     * Creates and returns a jquery element object for the layer opacity slider bar.
     *
     * @param {Object} layerState - The layerstate object for the respective layer.
     * @param {Object} controlsMapping - The control mapping from the layerstate layer id to the associated control elements.
     * @returns {JQuery} - The created element wrapped in a jquery object.
     */
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

    /**
     * Creates and returns a jquery element object for the layer ramp filter slider bar.
     *
     * @param {Object} layerState - The layerstate object for the respective layer.
     * @param {Object} controlsMapping - The control mapping from the layerstate layer id to the associated control elements.
     * @returns {JQuery} - The created element wrapped in a jquery object.
     */
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

    /**
     * Generates the filter axis major and minor tick marks. 5 major and 4 minor tick marks will be created.
     * @param {Array} minMax - The min and max values for the axis.
     * @returns {JQuery} - The created filter axis object.
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
     * @param {Integer} majorTicks - The number of major tick marks.
     * @param {Array} minMax - The min and max values for the axis.
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

    /**
     * Sets the drag and drop functionality for layer z-index ordering. Only domain-alike layers may be swapped.
     *
     * @param {Object} sortedLayers - All layers, sorted by z-index, highest first.
     * @param {Object} $layerControlsContainer - The layer controls container element.
     * @param {Object} $layerControlsRoot - The layer controls root element for the particular layer.
     * @param {Object} layerState - The layerstate object for the respective layer.
     * @param {Object} layerStateMap - The layerstate map for all layers keyed by layer id.
     * @param {Object} controlsMap - The entire control map for all layerstate objects to their associated control elements.
     * @returns {JQuery} - The created element wrapped in a jquery object.
     */
    addLayerDragCallbacks = function( sortedLayers, $layerControlsContainer, $layerControlRoot, layerState, layerStateMap, controlsMap ) {

        var controlsMapping = controlsMap[ layerState.getId() ];

        $layerControlRoot.draggable({
            "revert": function(valid) {

                var $that = $(this);
                if( !valid ) {
                    // dropped in an invalid location
                    setTimeout( function() {
                        $that.css({'box-shadow':"none", "z-index": 0});
                    }, 500);
                }
                return !valid;
            },
            "start": function() {
                controlsMapping.startPosition = $layerControlRoot.position();
            },
            "drag": function() {
               $(this).css({'box-shadow':"0 5px 15px #000", "z-index": 1000});
            }
        });

        $layerControlRoot.droppable({
            "accept": ".layer-controls-"+layerState.domain,
            "hoverClass": "layer-drag-hover",
            "drop": function(event, ui) {

                var $draggedLayerRoot = ui.draggable,
                    $droppedLayerRoot = $(this),
                    dragId = $draggedLayerRoot.attr("id").substr(15),
                    dropId = this.id.substr(15),
                    dragLayerState = layerStateMap[ dragId ],
                    dropLayerState = layerStateMap[ dropId ],
                    controlsMapping = controlsMap[dragId],
                    dragStartPosition = controlsMapping.startPosition,
                    dropStartPosition = $droppedLayerRoot.position(),
                    endPosition = $droppedLayerRoot.position(),
                    otherZ, key;

                // remove all dragability until this transition finishes
                for ( key in controlsMap ) {
                    if ( controlsMap.hasOwnProperty(key) ) {
                        controlsMap[key].layerRoot.css('pointer-events', 'none');
                    }
                }
                // give pop-up effect to droppable
                $droppedLayerRoot.css({'box-shadow':"0 5px 15px #000", "z-index": 999});
                // animate swap
                $.when( $draggedLayerRoot.animate({
                    // move dragged layer to its new position
                    top: endPosition.top - dragStartPosition.top,
                    left: endPosition.left - dragStartPosition.left
                }),
                $droppedLayerRoot.animate({
                    // move dropped layer to its new position
                    top: dragStartPosition.top - dropStartPosition.top,
                    left: dragStartPosition.left - dropStartPosition.left
                })).done( function () {
                    // once animation is complete, re-do the html
                    replaceLayers( sortedLayers, $layerControlsContainer, controlsMap, layerStateMap );
                });
                // swap z-indexes
                otherZ = dropLayerState.getZIndex();
                dropLayerState.setZIndex( dragLayerState.getZIndex() );
                dragLayerState.setZIndex(otherZ);
            }
        });
    };

    /**
     * Creates and returns a jquery element object for the layer ramp filter slider bar.
     *
     * @param {JQuery} $layerContent - The containing jquery element for the respective layer.
     * @param {Object} layerState - The layerstate object for the respective layer.
     * @param {Object} controlsMapping - The control mapping from the layerstate layer id to the associated control elements.
     * @returns {JQuery} - The created element wrapped in a jquery object.
     */
    createBaseLayerButtons = function( $layerContent, layerState, controlsMapping ) {

        var $baseLayerButtonSet = $('<div class="baselayer-fieldset"></div>'),
            $radioButton,
            $radioLabel,
            baseLayer,
            isActiveBaseLayer,
            i;

        function onClick() {
            var index = parseInt( $(this).val(), 10 );
            layerState.setBaseLayerIndex( index );
        }

        for (i=0; i<layerState.BASE_LAYERS.length; i++) {

            baseLayer = layerState.BASE_LAYERS[i];
            isActiveBaseLayer = ( i === layerState.getBaseLayerIndex() );

            // if active baselayer is blank, hide content
            if ( baseLayer.type === "BlankBase" && isActiveBaseLayer ) {
                $layerContent.css({height: '0px', "padding-bottom": "0px"});
            }

            // create radio button
            $radioButton = $( '<input type="radio" class="baselayer-radio-button" name="baselayer-radio-button" id="'+(baseLayer.options.name+i)+'"'
                            + 'value="' + i + '"' + ( isActiveBaseLayer ? 'checked>' : '>' ) );
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
     * @param {Array} sortedLayers - The sorted array of layer states.
     * @param {Integer} index - The index of the layer to be added to the controls panel.
     * @param {JQuery } $layerControlsContainer - The parent element in the document tree to add the controls to.
     * @param {Object} controlsMap - Maps layers to the sets of controls associated with them.
     */
    addLayer = function ( sortedLayers, index, $layerControlsContainer, controlsMap, layerStateMap ) {
        var layerState = sortedLayers[index],
            name = layerState.getName() || layerState.getId(),
            $layerControlRoot,
            $layerControlTitleBar,
            $layerContent,
            layerStateId,
            controlsMapping;

        layerStateId = layerState.getId();
        controlsMap[layerStateId] = {};
        layerStateMap[layerStateId] = layerState;
        controlsMapping = controlsMap[layerStateId];

        // create layer root
        $layerControlRoot = $('<div id="layer-controls-' + layerStateId + '" class="layer-controls-layer layer-controls-'+layerState.domain+'"></div>');
        $layerControlsContainer.append( $layerControlRoot );
        controlsMapping.layerRoot = $layerControlRoot;
        // add layer dragging / dropping callbacks to swap layer z-index
        addLayerDragCallbacks( sortedLayers, $layerControlsContainer, $layerControlRoot, layerState, layerStateMap, controlsMap );

        // create title div
        $layerControlTitleBar = $('<div class="layer-title"><span class="layer-labels">' + name + '</span></div>');
        $layerControlRoot.append( $layerControlTitleBar );

        // create content div
        $layerContent = $('<div class="layer-content"></div>');
        $layerControlRoot.append( $layerContent );
        controlsMapping.layerContent = $layerContent;

        // create settings button, only for server layers
        if ( layerState.domain === 'server' ) {
            $layerContent.append( createSettingsButton( $layerControlsContainer, $layerContent, layerState, controlsMapping ) );
        }

        // add visibility toggle box
        $layerContent.append( createVisibilityButton( layerState, controlsMapping ) );

        // add opacity slider
        $layerContent.append( createOpacitySlider( layerState, controlsMapping ) );

        if ( layerState.domain === 'server' ) {
            // add filter slider
            $layerContent.append( createFilterSlider( layerState, controlsMapping ) );
            // add layer promotion button
            //$layerContent.append( createPromotionButton( layerState, sortedLayers[index - 1] || null, controlsMapping, sortedLayers, $layerControlsContainer, controlsMap ) );
        }

        //add base layer radio buttons when this layer is the base layer
        if( layerState.domain === "base" && layerState.BASE_LAYERS.length > 1 ) {
            $layerControlTitleBar.append( createBaseLayerButtons( $layerContent, layerState, controlsMapping) );
        }
    };

    /**
     * Displays a settings panel for a layer.
     *
     * @param {object} $layerControlsContainer - The parent node to attach the layer panel to.
     * @param {object} layerState - The layer state model the panel will read from and update.
     */
    showLayerSettings = function( $layerControlsContainer, $layerContent, layerState ) {

        var $settingsContainer,
            $settingsTitleBar,
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
        oldChildren = $layerControlsContainer.children();

        $settingsContainer = $('<div class="settings-layer-container"></div>');

        // create title div
        $settingsTitleBar = $('<div class="settings-title"></div>');
        // add title span to div
        $settingsTitleBar.append($('<span class="layer-labels">' + layerState.getName() + '</span>'));
        $settingsContainer.append($settingsTitleBar);

        // create content div
        $settingsContent = $('<div class="settings-content"></div>');
        $settingsContainer.append($settingsContent);

        // create back button
        $backButton = $('<button class="settings-link">back</button>');
        $backButton.click(function () {
            replaceChildren( $layerControlsContainer, oldChildren );
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
                    .append($('<input type="radio" name="ramp-types" value="' + id + '" id="'+id+'">')
                        .add($('<label for="' + id + '">' + name + '</label>')
                )
            ));
        }

        // Update model on button changes
        $rampTypes.change( function () {
            layerState.setRampType( $(this).find('input[name="ramp-types"]:checked').val() );
        });

        $rampTypes.find('input[name="ramp-types"][value="' + layerState.getRampType() + '"]').prop('checked', true);

        // Add the ramp function radio buttons
        $rampFunctions = $('<div class="settings-ramp-functions"/>');
        $rampFunctions.append($('<div class="settings-ramp-title">Color Scale</div>'));
        $settingsContent.append($rampFunctions);

        for (i=0; i<layerState.RAMP_FUNCTIONS.length; i++) {
            name = layerState.RAMP_FUNCTIONS[i].name;
            id = layerState.RAMP_FUNCTIONS[i].id;
            $rampFunctions.append($('<div class="settings-values"></div>')
                            .append($('<input type="radio" name="ramp-functions" value="' + id + '" id="'+id+'">')
                                .add($('<label for="' + id + '">' + name + '</label>')
                )
            ));
        }

        $rampFunctions.change( function () {
            layerState.setRampFunction($(this).find('input[name="ramp-functions"]:checked').val());
        });

        $rampFunctions.find('input[name="ramp-functions"][value="' + layerState.getRampFunction() + '"]').prop('checked', true);

        replaceChildren($layerControlsContainer, $settingsContainer);
    };

    /**
     * Creates an observer to handle layer state changes, and update the controls based on them.
     */
    makeLayerStateObserver = function (layerState, controlsMap, layerStates, $layersControlListRoot) {
        return function (fieldName) {

            var controlsMapping = controlsMap[ layerState.getId() ],
                baseLayer, previousBaseLayer;

            switch (fieldName) {

                case "enabled":

                    controlsMapping.enabledCheckbox.prop("checked", layerState.isEnabled());
                    break;

                case "opacity":

                     controlsMapping.opacitySlider.slider("option", "value", layerState.getOpacity() * OPACITY_RESOLUTION);
                     break;

                case "filterRange":

                     controlsMapping.filterSlider.css({'background': 'url(' + layerState.getRampImageUrl() + ')', 'background-size': '100%'});
                     break;

                case "rampImageUrl":

                    controlsMapping.filterSlider.css({'background': 'url(' + layerState.getRampImageUrl() + ')', 'background-size': '100%'});
                    break;

                case "rampMinMax":

                    controlsMapping.filterAxis.html( createFilterAxis( layerState.getRampMinMax() ).children() );
                    break;

                case "baseLayerIndex":

                    baseLayer = layerState.BASE_LAYERS[ layerState.getBaseLayerIndex() ];
                    previousBaseLayer = layerState.BASE_LAYERS[ layerState.getPreviousBaseLayerIndex() ];

                    if ( baseLayer.type !== previousBaseLayer.type ) {
                        if ( baseLayer.type === "BlankBase" ) {
                            controlsMapping.layerContent.animate({height: "0px", "padding-bottom": "0px"});
                        } else if ( previousBaseLayer.type === "BlankBase" ) {
                            controlsMapping.layerContent.animate({height: "44px", "padding-bottom": "10px"});
                        }
                    }
                    break;
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
    replaceLayers = function ( layerStates, $layerControlsContainer, controlsMap, layerStateMap ) {
        var sortedLayerStates = sortLayers( layerStates ),
            i, key;

        // empty the container
        $layerControlsContainer.empty();

        $layerControlsContainer.append( '<div class="layer-controls-buffer"></div>');

        // Clear out any existing the controls map
        for (key in controlsMap) {
            if (controlsMap.hasOwnProperty(key)) {
                delete controlsMap[key];
            }
        }

        // Add layers - this will update the controls list.
        for (i = 0; i < sortedLayerStates.length; i += 1) {
            addLayer( sortedLayerStates, i, $layerControlsContainer, controlsMap, layerStateMap );
        }

        // append a spacer element at the bottom, padding causes jitter in overlay animation
        $layerControlsContainer.append( '<div class="layer-controls-buffer"></div>');
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
         * @param controlsId - The DOM element id used as the container for the controls panel elements.
         * @param layerStates - The list of layers the layer controls reflect and modify.
         */
        init: function ( controlsId, layerStates ) {

            var i;

            // "Private" vars
            this.controlsMap = {};
            this.layerStateMap = {};

            // find the container
            this.$layerControlsRoot = $('#'+controlsId);

            // Add layers visuals and register listeners against the model
            replaceLayers( layerStates, this.$layerControlsRoot, this.controlsMap, this.layerStateMap );

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
