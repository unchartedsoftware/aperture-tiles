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


/**
 * Captures the visual state of a layer in the system, and provides a notification
 * mechanism to allow external code to react to changes to it.
 */
define(function (require) {
    "use strict";



    var Class = require('../class'),
        objectsEqual,
        arraysEqual,
        isEqual,
        LayerState;


    /**
     * Compares objects for equality.
     *
     * @param {Object} a - First object under comparison
     * @param {Object} b - Second object under comparison
     * @returns {boolean} true if they are equal, false otherwise.
     */
    objectsEqual = function (a, b) {

        var keyA, keyB, found;

        if ( $.isEmptyObject(a) !== $.isEmptyObject(b) ) {
            return false;
        }

        for ( keyA in a ) {         // iterate through a
            if ( a.hasOwnProperty(keyA) ) {
                found = false;
                for ( keyB in b ) { // iterate through b
                    if ( b.hasOwnProperty(keyB) ) {
                        if ( a[keyA] === b[keyB] ) {
                            found = true; // found and equal
                        }
                    }
                }
                if ( !found ) {
                    return false;
                }
            }
        }
        return true;
    };


    /**
     * Compares arrays for equality.
     *
     * @param {Array} a - First array under comparison
     * @param {Array} b - Second array under comparison
     * @returns {boolean} true if they are equal, false otherwise.
     */
    arraysEqual = function (a, b) {
        var i;
        if (a === b) {
            return true;
        }
        if (a === null || b === null) {
            return false;
        }
        if (a.length !== b.length) {
            return false;
        }
        for (i = 0; i < a.length; ++i) {
            if (a[i] !== b[i]) {
                return false;
            }
        }
        return true;
    };

    isEqual = function( a, b ) {

        if ( $.isArray( a ) ) {
            // is array
            return arraysEqual( a, b );
        }

        if ( $.isPlainObject( a ) ) {
            // is object
            return objectsEqual( a, b );
        }

        // primitive
        return a === b;
    };


    LayerState = Class.extend({
        ClassName: "LayerState",

        /**
         * Initializes a LayerState object with default values.
         *
         * @param {string} id - The immutable ID of the layer.
         */
        init: function (id, name, domain) {
            this.domain = domain;
            this.id = id;
            this.name = name;
            this.listeners = [];
        },

        /**
         * @returns {string} - The ID of this layer state object.
         */
        getId: function () {
            return this.id;
        },

        /**
         * @returns {string} - The simple name of the layer.  This can appear in user facing elements and
         * should be formatted accordingly.
         */
        getName: function () {
            return this.name;
        },


        /**
         * @returns {string} - The domain of this layer state object.
         */
        getDomain: function () {
            return this.domain;
        },


        /**
         * @param {string} fieldName - Name of the modified field.
         * @param {Array} listeners - Array of {valueChange} listeners to execute.
         */
        notify : function (fieldName, listeners) {
            var i;
            for (i = 0; i < listeners.length; i += 1) {
                listeners[i](fieldName);
            }
        },


        /**
         * Registers a listener that will be executed whenever a layer state value
         * is modified.
         *
         * @param {valueChange} listener - The listener to register.
         */
        addListener: function (listener) {
            this.listeners.push(listener);
        },


        /**
         * Removes a callback if it exists.
         *
         * @param {valueChange} listener - The callback to remove.
         */
        removeListener: function (listener) {
            var index = this.listeners.indexOf(listener);
            if (index > -1) {
                this.listeners.splice(index, 1);
            }
        },


        set: function( key, value0, value1 ) {

            if ( key === 'id' || key === 'domain' || key === 'name' ) {
                console.log( 'Warning: layer state ' + key + ' attribute is immutable' );
                return;
            }

            if ( value1 === undefined ) {

                if ( !isEqual( this[key], value0 ) ) {
                    if (value0 === null) {
                        delete this[key];
                    } else {
                        this[key] = value0;
                    }
                    this.notify( key, this.listeners );
                }

            } else {

                this[key] = ( this[key] === undefined ) ? {} : this[key];

                if ( !isEqual( this[key][value0], value1 ) ) {
                    if (value1 === null) {
                        delete this[key][value0];
                    } else {
                        this[key][value0] = value1;
                    }
                    this.notify( key, this.listeners );
                }

            }
        },


        get: function( key0, key1 ) {

            if ( key1 === undefined ) {
                return ( this[key0] !== undefined ) ? this[key0] : null;
            }

            return ( this[key0] !== undefined )
                ? ( this[key0][key1] !== undefined )
                    ? this[key0][key1] : null
                : null;
        },


        has: function( key0, key1 ) {

            function hasValue( attr ) {
                if ( attr === undefined || attr === null ||
                   ( $.isPlainObject( attr ) && $.isEmptyObject( attr ) ) ||
                   ( $.isArray( attr ) && attr.length === 0 ) ) {
                    return false;
                }
                return true;
            }

            if ( key1 === undefined ) {
                return hasValue( this[key0] );
            }
            return hasValue( this[key0] ) && hasValue( this[key0][key1] );
        }

    });


    return LayerState;
});
