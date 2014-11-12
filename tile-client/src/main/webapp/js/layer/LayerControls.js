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
 * of each layer in the system.
 * This class follows the Separable Model pattern used by Swing widgets in Java, where
 * the controller and view are collapsed into a single class.
 */
define(function (require) {
    "use strict";

    var Class = require('../class'),
        Util = require('../util/Util'),
        PubSub = require('../util/PubSub'),
        AxisUtil = require('../map/AxisUtil'),
        TOOLTIP_SETTINGS_BUTTON = "Open filter settings menu",
        TOOLTIP_SETTINGS_BACK_BUTTON = "Return to layer controls menu",
        TOOLTIP_VISIBILITY_BUTTON = "Toggle layer visibility",
        TOOLTIP_OPACITY_SLIDER = "Adjust layer opacity",
        TOOLTIP_FILTER_SLIDER = "Filter layer values",
        TOOLTIP_BASELAYER_BUTTON = "Change base layer",
        TOOLTIP_RAMP_TYPE_BUTTON = "Change ramp color scale",
        TOOLTIP_RAMP_FUNCTION_BUTTON = "Change ramp function",
        LayerControls,
        addLayer,
        showLayerSettings,
        createSettingsButton,
        createVisibilityButton,
        createSliderHoverLabel,
        removeSliderHoverLabel,
        createOpacitySlider,
        createFilterSlider,
        createFilterAxisContent,
        createFilterAxisLabels,
        createBaseLayerButtons,
        addLayerDragCallbacks,
        OPACITY_RESOLUTION,
        FILTER_RESOLUTION,
        replaceChildren,
        makeLayerControlsSubscriber,
        convertSliderValueToFilterValue,
        convertFilterValueToSliderValue,
        updateFilterSliderHandles,
        replaceLayers,
        sortLayers;

    // constant initialization
    OPACITY_RESOLUTION = 100.0;
    FILTER_RESOLUTION = 100.0;


    // converts linear slider value from [0, 1] to actual filter ramp value based on ramp function
    convertSliderValueToFilterValue = function( layer, normalizedValue ) {
        var rampFunc = layer.getRampFunction(),
            minMax = layer.getRampMinMax(),
            range = minMax[1] - minMax[0],
            val;
        function log10(val) {
            return Math.log(val) / Math.LN10;
        }
        function checkLogInput( value ) {
            return ( value === 0 ) ? 1 : Math.pow( 10, log10( value ) * normalizedValue );
        }
        if ( rampFunc === "log10" ) {
            val = checkLogInput( minMax[1] );
        } else {
            val = normalizedValue * range + minMax[0];
        }
        return val;
    };


    // converts actual filter ramp value to the linear slider value between [0, 1] based on ramp function
    convertFilterValueToSliderValue = function( layer, filterValue ) {
        var rampFunc = layer.getRampFunction(),
            minMax = layer.getRampMinMax(),
            range = minMax[1] - minMax[0],
            val;
        function log10(val) {
            return Math.log(val) / Math.LN10;
        }
        function checkLogInput( value ) {
            return ( filterValue === 0 || filterValue === 1 ) ? 0 : log10( filterValue ) / log10( value );
        }
        if ( rampFunc === "log10" ) {
            val = checkLogInput( minMax[1] );
        } else {
            val = ( ( filterValue - minMax[0] ) / range );
        }
        return val;
    };


    updateFilterSliderHandles = function( layer, filterSlider ) {
        if ( layer.getLayerSpec().preserveRangeMin === true ) {
            filterSlider.slider( "values", 0, convertFilterValueToSliderValue( layer, layer.getFilterValues()[0] ) * FILTER_RESOLUTION );
        }
        if ( layer.getLayerSpec().preserveRangeMax === true ) {
            filterSlider.slider( "values", 1, convertFilterValueToSliderValue( layer, layer.getFilterValues()[1] ) * FILTER_RESOLUTION );
        }
    };


    /**
     * Replaces node's children and returns the replaced for storage. Fades out old content,
     * animates resizing to new container size, then fades in new contents.
     *
     * @param {JQuery} $controlsContent - The node to remove the children from.
     * @param {Object} $children - The children to replace.  Null will result in children being removed only.
     *
     * @returns {Array} - The removed children.
     */
    replaceChildren = function ( $controlsContent, $children ) {

        var removed,
            previousHeight = $controlsContent.outerHeight();

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
            container.animate( {"height": newHeight}, 200 )
                .promise()
                    .done( callback );
        }

        function fadeInChildren( container ) {
            container.children().animate( {"opacity":1}, 100 )
                .promise()
                    .done( function() {
                        container.css( "height", "" );
                    });
        }

        // fade current children out
        fadeOutChildren( $controlsContent, function() {
            // swap old children with new children
            removed = swapChildren( $controlsContent, $children );
            // resize parent container
            animateToNewSize( $controlsContent, previousHeight, function() {
                // fade new children in
                fadeInChildren( $controlsContent );
            });
        });
        return removed;
    };

    /**
     * Creates and returns a jquery element object for the settings menu button.
     *
     * @param {object} $controlsContent - The JQuery node that is the overlay content container.
     * @param {object} $layersContainer  - The JQuery node that acts as the parent of all the layer controls.
     * @param {Object} layer - The layer object.
     * @param {Object} controlsMapping - The control mapping from the layer id to the associated control elements.
     * @param {Object} settingsCustomization - An optional settings customization function.
     *
     * @returns {JQuery} - The created element wrapped in a jquery object.
     */
    createSettingsButton = function( $controlsContent,
                                     $layerContent,
                                     layer,
                                     controlsMapping,
                                     settingsCustomization ) {

        var $settingsButton = $('<button class="layer-controls-button">settings</button>');
        // set callback
        $settingsButton.click(function () {
            showLayerSettings( $controlsContent, layer, settingsCustomization );
        });
        // set tooltip
        Util.enableTooltip( $settingsButton,
                         TOOLTIP_SETTINGS_BUTTON );
        controlsMapping.settingsLink = $settingsButton;
        return $settingsButton;
    };

    /**
     * Creates and returns a jquery element object for the layer visibility toggle box.
     *
     * @param {Object} layer - The layer object
     * @param {Object} controlsMapping - The control mapping from the layer id to the associated control elements.
     *
     * @returns {JQuery} - The created element wrapped in a jquery object.
     */
    createVisibilityButton = function( layer, controlsMapping ) {

        var $toggleDiv = $('<div class="layer-toggle"></div>'),
            $toggleBox = $('<input type="checkbox" checked="checked" id="layer-toggle-box-' + layer.uuid + '">')
                             .add($('<label for="layer-toggle-box-' + layer.uuid + '"></label>'));

        $toggleDiv.append( $toggleBox );
        // Initialize the button from the model and register event handler.
        $toggleBox.prop("checked", layer.getVisibility() );
        // set click callback
        $toggleBox.click(function () {
            var value = $toggleBox.prop("checked");
            layer.setVisibility( value );
        });
        // set tooltip
        Util.enableTooltip( $toggleBox,
                         TOOLTIP_VISIBILITY_BUTTON );

        controlsMapping.enabledCheckbox = $toggleBox;
        return $toggleDiv;
    };

    /**
     * Creates and appends a label to the supplied element.
     *
     * @param {JQuery} $this - The element to which the label is appended onto
     * @param {Real} value - The value to be written on the label
     */
    createSliderHoverLabel = function( $this, value ) {
        var unitSpec = {
                'allowStepDown' : true,
                'decimals' : 2,
                'type': 'b'
            },
            $label = $('<div class="slider-value-hover" style="left:'+ $this.width()/2 +'px; top:0px;">'
                     +    '<div class="slider-value-hover-label">'+ AxisUtil.formatText( value, unitSpec ) +'</div>'
                     + '</div>');
        // remove previous label if it exists
        $this.find('.slider-value-hover').finish().remove();
        // add new label
        $this.append( $label );
        // reposition to be centred above cursor
        $label.css( {"margin-top": -$label.outerHeight()*1.2, "margin-left": -$label.outerWidth()/2 } );
    };

    /**
     * Removes a label from the supplied element.
     *
     * @param {JQuery} $this - The element to which the label is removed
     */
    removeSliderHoverLabel = function( $this ) {
        $this.find('.slider-value-hover').animate({
                opacity: 0
            },
            {
                complete: function() {
                    $this.find('.slider-value-hover').remove();
                },
                duration: 800
            }
        );
    };

    /**
     * Creates and returns a jquery element object for the layer opacity slider bar.
     *
     * @param {Object} layer - The layer object.
     * @param {Object} controlsMapping - The control mapping from the layer id to the associated control elements.
     *
     * @returns {JQuery} - The created element wrapped in a jquery object.
     */
    createOpacitySlider = function( layer, controlsMapping ) {

        var sliderClass = ( layer.domain === 'server' ) ? "opacity-slider" : "base-opacity-slider",
            $opacitySliderContainer = $('<div class="' + sliderClass + '"></div>'),
            $opacitySliderLabel = $('<div class="slider-label">Opacity</div>'),
            $opacitySlider = $('<div class="opacity-slider-bar"></div>').slider({
                range: "min",
                min: 0,
                max: OPACITY_RESOLUTION,
                value: layer.getOpacity() * OPACITY_RESOLUTION,
                slide: function( event, ui ) {
                    var value = ui.value / OPACITY_RESOLUTION;
                    layer.setOpacity( value );
                    createSliderHoverLabel( $opacitySlider.find(".ui-slider-handle"), value );
                },
                start: function( event, ui ) {
                    createSliderHoverLabel( $opacitySlider.find(".ui-slider-handle"), ui.value / OPACITY_RESOLUTION );
                },
                stop: function( event, ui ) {
                    var $handle = $opacitySlider.find(".ui-slider-handle");
                    if ( !$handle.is(':hover') ) {
                        // normally this label is removed on mouse out, in the case that
                        // the user has moused out while dragging, this will cause the label to
                        // be removed
                        removeSliderHoverLabel( $handle );
                    }
                }
             });

        $opacitySlider.find(".ui-slider-handle").mouseover( function() {
            createSliderHoverLabel( $(this), layer.getOpacity() );
        });
        $opacitySlider.find(".ui-slider-handle").mouseout( function() {
            removeSliderHoverLabel( $(this) );
        });

        // set tooltip
        Util.enableTooltip( $opacitySlider,
                            TOOLTIP_OPACITY_SLIDER );

        $opacitySliderContainer.append( $opacitySliderLabel );
        $opacitySliderContainer.append( $opacitySlider );
        controlsMapping.opacitySlider = $opacitySlider;
        return $opacitySliderContainer;
    };

    /**
     * Creates and returns a jquery element object for the layer ramp filter slider bar.
     *
     * @param {Object} layer - The layer object.
     * @param {Object} controlsMapping - The control mapping from the layer id to the associated control elements.
     *
     * @returns {JQuery} - The created element wrapped in a jquery object.
     */
    createFilterSlider = function( layer, controlsMapping ) {

        var filterRange = layer.getFilterRange(),
            $filterSliderContainer = $('<div class="filter-slider"></div>'),
            $filterLabel = $('<div class="slider-label">Filter</div>'),
            $filterSlider = $('<div style="background:rgba(0,0,0,0);"></div>'),
            $filterSliderImg,
            $filterAxis;

        $filterSliderContainer.append( $filterLabel );

        $filterSlider.slider({
            range: true,
            min: 0,
            max: FILTER_RESOLUTION,
            values: filterRange,
            change: function( event, ui ) {
                var values = ui.values,
                    previousValues = layer.getFilterRange();
                if ( values[0] !== previousValues[0] || values[1] !== previousValues[1] ) {
                    // prevent re-configuration on click / dblclick
                    layer.setFilterRange( values[0], values[1] );
                }
            },
            slide: function( event, ui ) {
                var handleIndex = $(ui.handle).index() - 1,
                    values = ui.values,
                    value = convertSliderValueToFilterValue( layer, values[ handleIndex ] / FILTER_RESOLUTION );
                createSliderHoverLabel( $( $filterSlider[0].children[ 1 + handleIndex ] ), value );
                layer.setFilterValues( convertSliderValueToFilterValue( layer, values[0] / FILTER_RESOLUTION ),
                                       convertSliderValueToFilterValue( layer, values[1] / FILTER_RESOLUTION ) );
            },
            start: function( event, ui ) {
                var handleIndex = $(ui.handle).index() - 1,
                    values = ui.values,
                    value = convertSliderValueToFilterValue( layer, values[ handleIndex ] / FILTER_RESOLUTION );
                createSliderHoverLabel( $( $filterSlider[0].children[ 1 + handleIndex ] ), value );
            },
            stop: function( event, ui ) {
                var handleIndex = $(ui.handle).index() - 1,
                    $handle = $( $filterSlider[0].children[ 1 + handleIndex ] );
                if ( !$handle.is(':hover') ) {
                    // normally this label is removed on mouse out, in the case that
                    // the user has moused out while dragging, this will cause the label to
                    // be removed
                    removeSliderHoverLabel( $handle );
                }
            }
        });

        $filterSlider.find('.ui-slider-handle').each( function( index, elem ) {
            // set initial style
            if ( layer.isFilterValueLocked( index ) === true ) {
                $(elem).addClass('sticky');
            }
            $(elem).dblclick( function() {
                if ( layer.isFilterValueLocked( index ) === true ) {
                    $(elem).removeClass('sticky');
                    layer.setFilterValueLocking( index, false );
                } else {
                    $(elem).addClass('sticky');
                    layer.setFilterValueLocking( index, true );
                }
            });
            $(elem).mouseover( function() {
                var value = convertSliderValueToFilterValue( layer, layer.getFilterRange()[index] / FILTER_RESOLUTION );
                createSliderHoverLabel( $(elem), value );
            });
            $(elem).mouseout( function() {
                removeSliderHoverLabel( $(elem) );
            });
        });

        // initialize filter values
        layer.setFilterValues( convertSliderValueToFilterValue( layer, layer.getFilterRange()[0] / FILTER_RESOLUTION ),
                               convertSliderValueToFilterValue( layer, layer.getFilterRange()[1] / FILTER_RESOLUTION ) );

        // set tooltip
        Util.enableTooltip( $filterSlider,
                            TOOLTIP_FILTER_SLIDER );

        $filterSliderImg = $filterSlider.find(".ui-slider-range");
        $filterSliderImg.addClass("filter-slider-img");

        // Disable the background for the range slider
        $filterSliderImg.css({"background": "none"});

        // Set the ramp image
        $filterSliderImg.css({'background': 'url(' + layer.getRampImageUrl() + ')', 'background-size': 'contain'});
        //create the filter axis
        $filterAxis = $('<div class="filter-axis"></div>');
        $filterAxis.append( createFilterAxisContent( layer.getRampMinMax(), layer.getRampFunction() ) );

        $filterSliderContainer.append( $filterSlider );
        $filterSliderContainer.append( $filterAxis );

        controlsMapping.filterSlider = $filterSlider;
        controlsMapping.filterSliderImg = $filterSliderImg;
        controlsMapping.filterAxis = $filterAxis;

        return $filterSliderContainer;
    };

    /**
     * Generates the filter axis major and minor tick marks. 5 major and 4 minor tick marks will be created.
     * @param {Array} minMax - The min and max values for the axis.
     * @param {String} rampFunc - The ramp transform function id.
     *
     * @returns {JQuery} - The created filter axis object.
     */
    createFilterAxisContent = function ( minMax, rampFunc ) {

        var axisTicks = '<div class="filter-axis-tick-major filter-axis-tick-first"></div>', //the first tick
            major = false, // next tick is a minor tick
            majorCount = 1,
            numberOfInnerTicks = 7,
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
        $filterAxisLabelContainer.append( createFilterAxisLabels( majorCount, minMax, rampFunc ) );

        return $filterAxisTicksContainer.add( $filterAxisLabelContainer );
    };

    /** Generates the filter labels and their initial values.
     *
     * @param {Integer} majorCount - The number of major tick marks.
     * @param {Array} minMax - The min and max values for the axis.
     * @param {String} rampFunc - The ramp transform function id.
     *
     * @returns {JQuery} - The created filter axis labels.
     */
    createFilterAxisLabels = function( majorCount, minMax, rampFunc ){
        var log10 = function(val) {
                return Math.log(val) / Math.LN10;
            },
            checkLogInput = function( value ) {
                return ( value === 0 ) ? 1 : Math.pow( 10, log10( value ) );
            },
            val = ( rampFunc === "log10" ) ? checkLogInput( minMax[0] ) : minMax[0],
            increment,
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
        if ( rampFunc === "log10" ) {
            for( i = 1; i < majorCount; i++ ){
                val = ( minMax[1] === 0 ) ? 1 : Math.pow( 10, log10( minMax[1] ) * (  i / majorCount ) );
                html += '<div class="filter-axis-label">' + AxisUtil.formatText(val, unitSpec) + '</div>';
            }
        } else {
            increment = ( minMax[1] - minMax[0] ) / majorCount;
            for( i = 1; i < majorCount; i++ ){
                val += increment;
                html += '<div class="filter-axis-label">' + AxisUtil.formatText(val, unitSpec) + '</div>';
            }
        }

        //add the last label
        val = ( rampFunc === "log10" ) ? checkLogInput( minMax[1] ) : minMax[1];
        html += '<div class="filter-axis-label filter-axis-label-last">' + AxisUtil.formatText(val, unitSpec) + '</div>';

        return $(html);
    };

    /**
     * Sets the drag and drop functionality for layer z-index ordering. Only domain-alike layers may be swapped.
     *
     * @param {Object} sortedLayers - All layers, sorted by z-index, highest first.
     * @param {Object} $layerControlsContainer - The layer controls container element.
     * @param {Object} $layerControlsRoot - The layer controls root element for the particular layer.
     * @param {Object} layer - The layer object.
     * @param {Object} layersByUuid - The layers by uuid.
     * @param {Object} controlsMap - The entire control map for all objects to their associated control elements.
     *
     * @returns {JQuery} - The created element wrapped in a jquery object.
     */
    addLayerDragCallbacks = function( sortedLayers, $controlsContent, $layersContainer, $layerControlRoot, layer, layersByUuid, controlsMap ) {

        var controlsMapping = controlsMap[ layer.uuid ];

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
            "accept": ".layer-controls-"+layer.domain,
            "hoverClass": "layer-drag-hover",
            "drop": function(event, ui) {

                var $draggedLayerRoot = ui.draggable,
                    $droppedLayerRoot = $(this),
                    dragId = $draggedLayerRoot.attr("id").substr(15),
                    dropId = this.id.substr(15),
                    dragLayer = layersByUuid[ dragId ],
                    dropLayer = layersByUuid[ dropId ],
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
                    replaceLayers( sortedLayers, $controlsContent, $layersContainer, controlsMap, layersByUuid );
                });
                // swap z-indexes
                otherZ = dropLayer.getZIndex();
                dropLayer.setZIndex( dragLayer.getZIndex() );
                dragLayer.setZIndex( otherZ );
            }
        });
    };

    /**
     * Creates and returns a jquery element object for the layer ramp filter slider bar.
     *
     * @param {JQuery} $layerContent - The containing jquery element for the respective layer.
     * @param {Object} layer - The layer object.
     * @param {Object} controlsMapping - The control mapping from the layer id to the associated control elements.
     *
     * @returns {JQuery} - The created element wrapped in a jquery object.
     */
    createBaseLayerButtons = function( $layerContent, layer, controlsMapping ) {

        var $baseLayerButtonSet = $('<div class="baselayer-fieldset"></div>'),
            $radioButton,
            $radioLabel,
            baseLayer,
            isActiveBaseLayer,
            i;

        function onClick() {
            var index = parseInt( $(this).val(), 10 );
            layer.setBaseLayerIndex( index );
        }

        for (i=0; i<layer.BASE_LAYERS.length; i++) {

            baseLayer = layer.BASE_LAYERS[i];
            isActiveBaseLayer = ( i === layer.getBaseLayerIndex() );

            // if active baselayer is blank, hide content
            if ( baseLayer.type === "BlankBase" && isActiveBaseLayer ) {
                $layerContent.css({height: '0px', "padding-bottom": "0px", "display" : "none"});
            }

            // create radio button
            $radioButton = $( '<input type="radio" class="baselayer-radio-button" name="baselayer-radio-button" id="'+(baseLayer.options.name+i)+'"'
                            + 'value="' + i + '"' + ( isActiveBaseLayer ? 'checked>' : '>' ) );
            $radioButton.on( 'click', onClick );
            // set tooltip
            Util.enableTooltip( $radioButton,
                                TOOLTIP_BASELAYER_BUTTON );

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
     * @param {Array} sortedLayers - The sorted array of layers.
     * @param {Object} layer - The layer to be added to the controls panel.
     * @param {JQuery} $controlsContent - The JQuery node that is the overlay content container.
     * @param {JQuery} $layersContainer - The parent element in the document tree to add the controls to.
     * @param {Object} controlsMap - Maps layers to the sets of controls associated with them.
     * @param {Object} layersByUuid - Layers by uuid.
     * @param {Object} settingsCustomization - An optional settings customization function.
     */
    addLayer = function ( sortedLayers,
                          layer,
                          $controlsContent,
                          $layersContainer,
                          controlsMap,
                          layersByUuid,
                          settingsCustomization ) {

        var uuid = layer.uuid,
            name = layer.name,
            domain = layer.domain,
            $layerControlRoot,
            $layerControlTitleBar,
            $layerContent,
            controlsMapping;

        controlsMap[uuid] = {};
        layersByUuid[uuid] = layer;
        controlsMapping = controlsMap[uuid];

        // create layer root
        $layerControlRoot = $('<div id="layer-controls-' + uuid + '" class="layer-controls-layer layer-controls-'+domain+'"></div>');
        $layersContainer.append( $layerControlRoot );
        controlsMapping.layerRoot = $layerControlRoot;
        // add layer dragging / dropping callbacks to swap layer z-index
        addLayerDragCallbacks( sortedLayers, $controlsContent, $layersContainer, $layerControlRoot, layer, layersByUuid, controlsMap );

        // create title div
        $layerControlTitleBar = $('<div class="layer-title"><span class="layer-labels">' + name + '</span></div>');
        $layerControlRoot.append( $layerControlTitleBar );

        // create content div
        $layerContent = $('<div class="layer-content"></div>');
        $layerControlRoot.append( $layerContent );
        controlsMapping.layerContent = $layerContent;

        // create settings button, only for server layers
        if ( domain === 'server' ) {
            $layerContent.append( createSettingsButton( $controlsContent, $layerContent, layer, controlsMapping, settingsCustomization ) );
        }

        // add visibility toggle box
        $layerContent.append( createVisibilityButton( layer, controlsMapping ) );

        // add opacity slider
        $layerContent.append( createOpacitySlider( layer, controlsMapping ) );

        if ( domain === 'server' ) {
            // add filter slider
            $layerContent.append( createFilterSlider( layer, controlsMapping ) );
        }

        // clear floats
        $layerContent.append( '<div style="clear:both"></div>' );

        //add base layer radio buttons when this layer is the base layer
        if( domain === "base" && layer.BASE_LAYERS.length > 1 ) {
            $layerControlTitleBar.append( createBaseLayerButtons( $layerContent, layer, controlsMapping) );
            // clear floats
            $layerControlTitleBar.append( '<div style="clear:both"></div>' );
        }
    };


    /**
     * Displays a settings panel for a layer.
     *
     * @param {JQuery} $controlsContent - The JQuery node that is the overlay content container.
     * @param {Object} layer - The layer object.
     * @param {Object} settingsCustomization - An optional settings customization function.
     */
    showLayerSettings = function( $controlsContent,
                                  layer,
                                  settingsCustomization ) {

        var $settingsContainer,
            $settingsTitleBar,
            $settingsContent,
            $coarsenessSettings,
            name,
            span,
            val,
            $leftSpan,
            $rightSpan,
            $rampTypes,
            $rampFunctions,
            $settingValue,
            id,
            oldChildren,
            $backButton,
            i;

        // Save the main layer controls hierarchy
        oldChildren = $controlsContent.children();

        $settingsContainer = $('<div class="settings-layer-container"></div>');

        // create title div
        $settingsTitleBar = $('<div class="settings-title"></div>');
        // add title span to div
        $settingsTitleBar.append($('<span class="layer-labels">' + layer.name + '</span>'));
        $settingsContainer.append($settingsTitleBar);

        // create content div
        $settingsContent = $('<div class="settings-content"></div>') ;
        $settingsContainer.append($settingsContent);

        // create back button
        $backButton = $('<button class="layer-controls-button">back</button>');
        // set tooltip
        Util.enableTooltip( $backButton,
                            TOOLTIP_SETTINGS_BACK_BUTTON );
        $backButton.click(function () {
            replaceChildren( $controlsContent, oldChildren );
        });

        $settingsTitleBar.append($backButton);

        // add the ramp types radio buttons
        $rampTypes = $('<div class="settings-ramp-types"/>');
        // add title to ramp types div
        $rampTypes.append($('<div class="settings-sub-title">Color Ramp</div>'));

        $settingsContent.append($rampTypes);
        // create left and right columns
        $leftSpan = $('<span class="settings-ramp-span-left"></span>');
        $rightSpan = $('<span class="settings-ramp-span-right"></span>');
        $rampTypes.append($leftSpan);
        $rampTypes.append($rightSpan);

        for (i=0; i<layer.RAMP_TYPES.length; i++) {
            // for each ramp type
            name = layer.RAMP_TYPES[i].name;
            id = layer.RAMP_TYPES[i].id;
            // add half types to left, and half to right
            span = (i < layer.RAMP_TYPES.length/2) ? $leftSpan : $rightSpan;
            $settingValue = $('<div class="settings-values"></div>')
                                .append($('<input type="radio" name="ramp-types" value="' + id + '" id="'+id+'">')
                                    .add($('<label for="' + id + '">' + name + '</label>') ) );
            // set tooltip
            Util.enableTooltip( $settingValue,
                                TOOLTIP_RAMP_TYPE_BUTTON );

            span.append( $settingValue );
        }

        // Update model on button changes
        $rampTypes.change( function () {
            var value = $(this).find('input[name="ramp-types"]:checked').val();
            layer.setRampType( value );
        });

        $rampTypes.find('input[name="ramp-types"][value="' + layer.getRampType() + '"]').prop('checked', true);

        // Add the ramp function radio buttons
        $rampFunctions = $('<div class="settings-ramp-functions"/>');
        $rampFunctions.append($('<div class="settings-sub-title">Color Scale</div>'));

        $settingsContent.append($rampFunctions);

        for (i=0; i<layer.RAMP_FUNCTIONS.length; i++) {
            name = layer.RAMP_FUNCTIONS[i].name;
            id = layer.RAMP_FUNCTIONS[i].id;
            $settingValue = $('<div class="settings-values"></div>')
                                .append($('<input type="radio" name="ramp-functions" value="' + id + '" id="'+id+'">')
                                    .add($('<label for="' + id + '">' + name + '</label>') ) );
            // set tooltip
            Util.enableTooltip( $settingValue,
                                TOOLTIP_RAMP_FUNCTION_BUTTON );

            $rampFunctions.append( $settingValue );
        }

        $rampFunctions.change( function () {
            var value = $(this).find('input[name="ramp-functions"]:checked').val();
            layer.setRampFunction( value );
        });

        $rampFunctions.find('input[name="ramp-functions"][value="' + layer.getRampFunction() + '"]').prop('checked', true);


        $coarsenessSettings = $('<div class="settings-coarseness"/>');
        $coarsenessSettings.append($('<div class="settings-sub-title">Coarseness</div>'));
        $settingsContent.append( $coarsenessSettings );

        for (i=0; i<4; i++) {
            val = Math.pow( 2, i );
            name =  val +"x"+ val+ " pixels";
            id = i + 1;
            $settingValue = $('<div class="settings-values"></div>')
                                .append($('<input type="radio" name="coarseness-values" value="' + id + '" id="'+id+'">')
                                    .add($('<label for="' + id + '">' + name + '</label>') ) );
            // set tooltip
            Util.enableTooltip( $settingValue,
                                TOOLTIP_RAMP_FUNCTION_BUTTON );

            $coarsenessSettings.append( $settingValue );
        }

        $coarsenessSettings.change( function () {
            var value = $(this).find('input[name="coarseness-values"]:checked').val();
            layer.setCoarseness( value );
        });

        $coarsenessSettings.find('input[name="coarseness-values"][value="' + layer.getCoarseness() + '"]').prop('checked', true);

        // if settings customization is provided, add it
        if ( settingsCustomization ) {
            $settingsContent.append( settingsCustomization( layer ) );
        }

        replaceChildren( $controlsContent, $settingsContainer );
    };

    /**
     * Creates an observer to handle layer state changes, and update the controls based on them.
     * @param {object} layers - A layer object.
     * @param {object} layers - An array map of layer objects.
     * @param {object} controlsMap - A map indexed by layer ID contain references to the individual layer controls.
     *
     * @returns {Function} - The subscriber function for the given layer.
     */
    makeLayerControlsSubscriber = function ( layer, layers, controlsMap ) {
        return function ( message, path ) {

            var field = message.field,
                value = message.value,
                controlsMapping = controlsMap[ layer.uuid ],
                baseLayer, previousBaseLayer, i;

            switch ( field ) {

                case "enabled":

                    controlsMapping.enabledCheckbox.prop( "checked", value );
                    break;

                case "opacity":

                     controlsMapping.opacitySlider.slider( "value", value * OPACITY_RESOLUTION );
                     break;

                case "rampFunction":

                    controlsMapping.filterAxis.html( createFilterAxisContent( layer.getRampMinMax(), value ) );
                    updateFilterSliderHandles( layer, controlsMapping.filterSlider );
                    break;

                case "filterRange":

                     controlsMapping.filterSliderImg.css( {'background': 'url(' + layer.getRampImageUrl() + ')', 'background-size': 'contain'});
                     break;

                case "rampImageUrl":

                    controlsMapping.filterSliderImg.css( {'background': 'url(' + layer.getRampImageUrl() + ')', 'background-size': 'contain'});
                    break;

                case "rampMinMax":

                    controlsMapping.filterAxis.html( createFilterAxisContent( layer.getRampMinMax(), layer.getRampFunction() ) );
                    updateFilterSliderHandles( layer, controlsMapping.filterSlider );
                    break;

                case "baseLayerIndex":

                    baseLayer = layer.BASE_LAYERS[ value ];
                    previousBaseLayer = layer.BASE_LAYERS[ layer.getPreviousBaseLayerIndex() ];

                    // if the switching from blank baselayer to TMS / Google, animate the
                    // hiding / showing of the opacity bar
                    if ( baseLayer.type !== previousBaseLayer.type ) {
                        if ( baseLayer.type === "BlankBase" ) {
                            controlsMapping.layerContent.animate({height: "0px", "padding-bottom": "0px"}, {
                                complete: function() {
                                    controlsMapping.layerContent.css('display', 'none');
                                }
                            });
                        } else if ( previousBaseLayer.type === "BlankBase" ) {
                            controlsMapping.layerContent.css('display', 'block');
                            controlsMapping.layerContent.animate({height: "44px", "padding-bottom": "10px"});
                        }
                    }

                    // change theme for all layers
                    for ( i=0; i<layers.length; i++ ) {
                        if ( layers[i].domain === "server" ) {
                            layers[i].updateTheme();
                        }
                    }
                    break;
            }
        };
    };

    /**
     * Replace the existing layer controls with new ones derived from the set of layer objects.  All the
     * new control references will be stored in the controlsMap for later access.
     *
     * @param {object} layers - An array map of layer objects.
     * @param {object} $controlsContent - The JQuery node that is the overlay content container.
     * @param {object} $layersContainer  - The JQuery node that acts as the parent of all the layer controls.
     * @param {object} controlsMap - A map indexed by layer ID contain references to the individual layer controls.
     * @param {Object} layersByUuid - Layers by uuid.
     * @param {Object} settingsCustomization - An optional settings customization function.
     */
    replaceLayers = function ( layers,
                               $controlsContent,
                               $layersContainer,
                               controlsMap,
                               layersByUuid,
                               settingsCustomization ) {

        var sortedLayers = sortLayers( layers ),
            i, key;

        // empty the container
        $layersContainer.empty();

        // Clear out any existing the controls map
        for (key in controlsMap) {
            if (controlsMap.hasOwnProperty(key)) {
                delete controlsMap[key];
            }
        }

        // Add layers - this will update the controls list.
        for (i = 0; i < sortedLayers.length; i += 1) {
            addLayer( sortedLayers,
                      sortedLayers[i],
                      $controlsContent,
                      $layersContainer,
                      controlsMap,
                      layersByUuid,
                      settingsCustomization );
        }
    };

    /**
     * Converts the layer state map into an array and then sorts it based layer
     * z indices.
     *
     * @param layers - An array of layer objects.
     * @returns {Array} - An array of layer objects sorted highest to lowest by z index.
     */
    sortLayers = function ( layers ) {

        var arrayCopy = layers.concat();

        arrayCopy.sort( function (a, b) {
            return b.getZIndex() - a.getZIndex();
        });
        return arrayCopy;
    };



    LayerControls = Class.extend({
        ClassName: "LayerControls",

        /**
         * Initializes the layer controls by modifying the DOM tree, and registering
         * callbacks against the layer object
         *
         * @param {JQuery} controlsContent - The DOM element used as the container for the controls panel elements.
         * @param {Array} layers - The array of layers objects.
         * @param {Object} settingsCustomization - An optional settings customization function.
         */
        init: function ( controlsContent, layers, settingsCustomization ) {

            var i;

            this.controlsMap = {};
            this.layersByUuid = {};
            this.$controlsContent = controlsContent;
            // wrap layer contents in this div, this will allow scrollability,
            // while hiding draggable overflow.
            this.$layersContainer = $( '<div class="layers-container"></div>' );
            this.$controlsContent.append( this.$layersContainer );

            // Add layers visuals and register listeners against the model
            replaceLayers( layers,
                           this.$controlsContent,
                           this.$layersContainer,
                           this.controlsMap,
                           this.layersByUuid,
                           settingsCustomization );

            for (i=0; i<layers.length; i++) {
                // create subscriber function for each layer
                PubSub.subscribe( layers[i].getChannel(),
                    makeLayerControlsSubscriber(
                        layers[i],
                        layers,
                        this.controlsMap
                    ));
            }
        },

        noop: function() {
            return true;
        }

    });

    return LayerControls;
});
