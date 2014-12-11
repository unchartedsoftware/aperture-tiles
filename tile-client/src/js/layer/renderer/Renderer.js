/*
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

/**
 * A renderer interface that stores common functionality across all renderers, mainly
 * the execution of the 'hook' function to give developers access to the generated DOM
 * elements to provided application specific behavior.
 *
 * Typically each unique component generated from a renderer will be under a separate
 * specification attribute. For example the text elements of the TextByFrequencyRenderer
 * is under 'text' and the frequency distribution elements are under 'frequency'.
 *
 * Each of the separate components SHOULD accept *Key attributes to instruct it as to
 * where the relevant data should be found in the tile data object.
 *
 * Each of these separate components SHOULD accept isolated RenderThemes (in the case
 * that RenderThemes are used).
 */
( function() {

    "use strict";

    /**
     * Base constructor for Renderer. Renderers are designed to provide generic rendering
     * capabilities that can be reused across applications.
     *
     * @param spec {Object} The specification object.
     * {
     *     hook {Function} - The hook function that is executed on every rendered entry.
     *
     *         arguments:
     *             elem    {HTMLElement} - The html element for the entry.
     *             entry   {Object}      - The data entry.
     *             entries {Array}       - All entries for the tile.
     *             data    {Object}      - The raw data object for the tile.
     * }
     */
    function Renderer( spec ) {
        this.spec = spec || {};
    }

    /**
     * When rendering a tile, the renderer will by default assume each immediate sibling
     * corresponds to a data entry. This function allows a renderer implementation to
     * provide a selector in situations where this is not the case. This is only relevant
     * if the hook callback is set.
     *
     * @returns {boolean || string}
     */
    Renderer.prototype.getEntrySelector = function() {
        return false;
    };

    /**
     * The central rendering function. This function is called for every tile containing data.
     * Returns an object containing the tiles html, along with an array of each data entry. The
     * implementation of this function is unique to each renderer.
     *
     * @returns {{html: string, entries: Array}}
     */
    Renderer.prototype.render = function() {
        return {
            html: "",
            entries: []
        };
    };

    /**
     * The hook callback executor function. If a hook function is provided to the renderer, this
     * will execute it passing the respective element and data entry along with all entries and
     * the raw tile data.
     *
     * @param elements {HTMLCollection} A collection of html elements.
     * @param entries  {Array} The array of all data entries.
     * @param data     {Object} The raw tile data object.
     */
    Renderer.prototype.hook = function( elements, entries, data ) {
        var hook = this.spec.hook,
            $elements;
        if ( hook && typeof hook === "function" ) {
            // get the entries
            $elements = $( elements );
            // if entry selector is set, use it to select entries
            if ( this.getEntrySelector() ) {
                $elements = $elements.find( this.getEntrySelector() );
            }
            // call entry function on each entry
            $elements.each( function( index, elem ) {
                hook( elem, entries[ index ], entries, data );
            });
        }
    };

    module.exports = Renderer;
}());