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

/* JSLint global declarations: these objects don't need to be declared. */
/*global OpenLayers */

/**
 * This module defines a generic standardized slider class.
 *
 * It really doesn't do all that much, at the moment, over a standard JQuery
 * slider, but it does do a little - it sets up the visuals properly within
 * a passed-in control, mostly.
 */
define(function (require) {
    "use strict";

    var Class = require('../class'),
        SliderControl, onStart, onSlide, onStop;


    // Private methods
    /* Called when the slider starts sliding */
    onStart = function (sliderControl, event, ui) {
        if (sliderControl.startCallback) {
            sliderControl.startCallback(sliderControl);
        }
        sliderControl.startValue = sliderControl.getValue();
        sliderControl.lastValue = sliderControl.getValue();
    };

    /* Called as the slider slides */
    onSlide = function (sliderControl, event, ui) {
        if (sliderControl.slideCallback) {
            sliderControl.slideCallback(sliderControl.lastValue, sliderControl);
        }
        sliderControl.lastValue = sliderControl.getValue();
    };

    /* Called when the slider finishes sliding */
    onStop = function (sliderControl, event, ui) {
        if (sliderControl.stopCallback) {
            sliderControl.stopCallback(sliderControl.startValue, sliderControl);
        }
        sliderControl.startValue = null;
        sliderControl.lastValue = null;
    };


    SliderControl = Class.extend({
        /**
         * Construct a slider control.
         *
         * @param base The JQuery select item in which the slider should be built
         * @param label The label to apply to the slider
         * @param minimum The minimum value of the slider
         * @param maximum The maximum value of the slider
         * @param steps The number of intervals between legal slider values 
         *              (so the actual total legal number of values is steps+1)
         */
        init: function (base, id, label, minimum, maximum, steps) {
            var row, labelCell, sliderCell;
            this.id = id;
            this.sliderId = id+'.slider';
            this.startValue = null;
            this.lastValue = null;
            this.startCallback = null;
            this.slideCallback = null;
            this.stopCallback = null;
            this.minimum = minimum;
            this.maximum = maximum;
            this.steps = steps;

            // Set up our UI
            row = $('<tr></tr>');
            labelCell = $('<td></td>');
            sliderCell = $('<td></td>');
            base.append(row);
            row.append(labelCell);
            row.append(sliderCell);
            labelCell.append(document.createTextNode(label));

            this.slider = $('<div id="'+this.sliderId+'"></div>');
            this.slider.addClass("opacity-slider");
            sliderCell.append(this.slider);

            this.slider.slider({
                range: "min",
                min: 0,
                max: steps,
                value: steps/2,
                start: $.proxy(function (event, ui) {
                    onStart(this, event, ui);
                }, this),
                slide: $.proxy(function (event, ui) {
                    onSlide(this, event, ui);
                }, this),
                stop: $.proxy(function (event, ui) {
                    onStop(this, event, ui);
                }, this)
            });
        },

        /**
         * Set the (single) callback function to be called when the user 
         * starts dragging the slider around.
         *
         * @param callback The function to be called.  The function will be 
         *                 called with one parameter: this slider object, from 
         *                 which the current value can be obtained.
         */
        setOnStart: function (callback) {
            this.startCallback = callback;
        },

        /**
         * Set the (single) callback function to be called on individual slide 
         * messages as the user drags the slider around.
         *
         * @param callback The function to be called.  The function will be 
         *                 called with two parameters: the last known value
         *                 of the slider, and this slider object, from which 
         *                 the current value can be obtained.
         */
        setOnSlide: function (callback) {
            this.slideCallback = callback;
        },

        /**
         * Set the (single) callback function to be called when the user 
         * finishes dragging the slider around.
         *
         * @param callback The function to be called.  The function will be 
         *                 called with two parameters: the value of the slider 
         *                 when the slide began, and this slider object, from 
         *                 which the current value can be obtained.
         */
        setOnStop: function (callback) {
            this.stopCallback = callback;
        },

        /**
         * Set the current value of the slider.
         *
         * This function currently does no error checking that the value is in 
         * range; such checks are the responsibility of the caller for now.
         */
        setValue: function (newValue) {
            var rawValue;
            if (!isNaN(newValue)) {
                rawValue = Math.round(this.steps * (newValue - this.minimum)
                                      / (this.maximum - this.minimum));
                this.slider.slider("value", rawValue);
            }
        },

        /**
         * Get the current value of the slider.
         */
        getValue: function () {
            var rawValue = this.slider.slider("value");
            return this.minimum + (rawValue * (this.maximum - this.minimum) / this.steps);
        }
    });

    return SliderControl;
});
