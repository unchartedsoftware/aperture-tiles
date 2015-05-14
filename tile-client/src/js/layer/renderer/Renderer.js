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

( function() {

    "use strict";

    var Util = require('../../util/Util' ),
        RendererUtil = require('./RendererUtil');

    /**
     * Instantiate a Renderer object.
     * @class Renderer
     * @classdesc The Renderer class is designed to provide generic rendering capabilities
     * that can be reused across applications. This base class stores functionality that is
     * common across all renderer implementations, mainly the execution of the 'hook'
     * function to give access to the generated DOM elements to provide application
     * specific behavior.
     * <br>
     * Typically each unique component generated from a renderer will be under a separate
     * specification attribute. For example the text elements of the TextByFrequencyRenderer
     * is under 'text' and the frequency distribution elements are under 'frequency'.
     * <br>
     * Each of the separate components SHOULD accept *Key attributes to instruct it as to
     * where the relevant data should be found in the tile data object. Each of these
     * separate components SHOULD accept isolated RenderThemes (in the case that RenderThemes
     * are used).
     *
     * @param spec {Object} The specification object.
     * <pre>
     * {
     *     hook {Function} - The hook function that is executed on every rendered entry providing the following arguments:
     *         elem    {HTMLElement} - The html element for the entry.
     *         entry   {Object}      - The data entry.
     *         entries {Array}       - All entries for the tile.
     *         data    {Object}      - The raw data object for the tile.
     * }
     * </pre>
     */
    function Renderer( spec ) {
        this.spec = spec || {};
        this.uuid = Util.generateUuid();
        if ( spec.aggregator ) {
            this.aggregator = spec.aggregator;
        }
    }

    /**
     * Add a hook function to the renderer.
     * @memberof Renderer
     *
     * @param {Function} hook - the callback function.
     */
    Renderer.prototype.addHook = function( hook ) {
        if ( this.spec.hook ) {
            this.spec.hooks = [ this.spec.hook ];
            delete this.spec.hook;
        }
        this.spec.hooks = this.spec.hooks || [];
        if ( hook && typeof hook === "function" ) {
            this.spec.hooks.push( hook );
        }
    };

    /**
     * Remove a hook function from the renderer.
     * @memberof Renderer
     *
     * @param {Function} hook - the callback function.
     */
    Renderer.prototype.removeHook = function( hook ) {
        var index;
        if ( this.spec.hook && this.spec.hook === hook ) {
            delete this.spec.hook;
            this.spec.hooks = [ this.spec.hook ];
        }
        if ( this.spec.hooks ) {
            index = this.spec.hooks.indexOf( hook );
            if ( index !== -1 ) {
                this.spec.hooks.splice( index, 1 );
            }
        }
    };

    /**
     * When rendering a tile, the renderer will by default assume each immediate sibling
     * corresponds to a data entry. This function allows a renderer implementation to
     * provide a selector in situations where this is not the case. This is only relevant
     * if the hook callback is set.
     * @memberof Renderer
     * @private
     *
     * @returns {boolean|string} The DOM element selector for each rendered entry.
     */
    Renderer.prototype.getEntrySelector = function() {
        return false;
    };

    /**
     * Select all entries in the rendered layer that also share the same selected value. The
     * 'selectKey' of the 'select' option must be set.
     * @memberof Renderer
     *
     * @param {Object} selectedEntry - The selected data entry.
     */
    Renderer.prototype.select = function( selectedEntry ) {
        var $tiles = $( this.parent.olLayer.div ).find( '.olTileHtml,.olTileUnivariate' ),
            selectKey = this.spec.select.selectKey,
            selector = this.getEntrySelector(),
            uuid = this.uuid,
            selectValue,
            $entries;
        // if no key specified, exit
        if ( !selectKey ) {
            return;
        }
        // get the select value based on key
        this.selectValue = selectValue = RendererUtil.getAttributeValue( selectedEntry, selectKey );
        // if entry selector is set, use it to select entries
        $entries = selector ? $tiles.find( selector ) : $tiles.children();
        // for each entry, check if they have the matching value to the select
        $entries.each( function() {
            var $elem = $( this ),
                entry,
                value;
            // if this renderer did not create the elements, abort
            if ( uuid !== $elem.data( 'uuid' ) ) {
                return;
            }
            // get the entry and value
            entry = $elem.data( 'entry' );
            value = RendererUtil.getAttributeValue( entry, selectKey );
            if ( value === selectValue ) {
                 $elem.removeClass( 'de-emphasized' ).addClass( 'emphasized' );
            } else {
                $elem.removeClass( 'emphasized' ).addClass( 'de-emphasized' );
            }
        });
    };

    /**
     * Unselect all entries in the rendered layer.
     * @memberof Renderer
     */
    Renderer.prototype.unselect = function() {
        var $tiles = $( this.parent.olLayer.div ).find( '.olTileHtml,.olTileUnivariate' ),
            selector = this.getEntrySelector(),
            $entries;
        // if entry selector is set, use it to select entries
        $entries = selector ? $tiles.find( selector ) : $tiles.children();
        // for each entry, remove relevant classes
        $entries.each( function() {
            $( this ).removeClass( 'de-emphasized' ).removeClass( 'emphasized' );
        });
        // clear the select value
        this.selectValue = null;
    };

    /**
     * The central rendering function. This function is called for every tile containing data.
     * Returns an object containing the tiles html, along with an array of each data entry. The
     * implementation of this function is unique to each renderer.
     * @memberof Renderer
     * @private
     *
     * @returns {{html: string, entries: Array}} The html to render and an array of all rendered data entries.
     */
    Renderer.prototype.render = function() {
        return {
            html: "",
            entries: []
        };
    };

    /**
     * Attaches the renderer to its respective layer. This method should not be called
     * manually.
     * @memberof Renderer
     * @private
     *
     * @param {Layer} layer - The layer to attach to the renderer.
     */
    Renderer.prototype.attach = function( layer ) {
        if ( this.parent && this.parent !== layer ) {
            console.log( "This renderer has already been attached " +
                         "to a different layer, please use another instance." );
            return;
        }
        this.parent = layer;
        if ( this.aggregator ) {
            this.aggregator.attach( layer );
        }
    };

    /**
     * This iterates over every data entry's DOM element and if there is a current selected
     * value in the renderer, it will inject the correct emphasize and de-emphasize flags.
     * This method should not be called manually.
     * @memberof Renderer
     * @private
     *
     * @param {HTMLCollection} elements - A collection of html elements.
     * @param {Array} entries - The array of all data entries.
     */
    Renderer.prototype.injectEntries = function( elements, entries ) {
        var selector = this.getEntrySelector(),
            $elements = $( elements ),
            selectValue = this.selectValue,
            select = this.spec.select,
            uuid = this.uuid,
            selectKey;
        if ( !select || !select.selectKey ) {
            return;
        }
        // get select key
        selectKey = select.selectKey;
        // if entry selector is set, use it to select entries
        if ( selector ) {
            $elements = $elements.find( selector );
        }
        // call entry function on each entry
        $elements.each( function( index, elem ) {
            var $elem = $( elem ),
                entry = entries[index],
                value = RendererUtil.getAttributeValue( entry, selectKey );
            // store the entry data in the entry element
            $elem.data( 'entry', entry );
            // store the renderer uuid
            $elem.data( 'uuid', uuid );
            if ( selectValue && selectKey ) {
                if ( value === selectValue ) {
                    $elem.removeClass( 'de-emphasized' ).addClass( 'emphasized' );
                } else {
                    $elem.removeClass( 'emphasized' ).addClass( 'de-emphasized' );
                }
            }
        });
    };

    /**
     * The hook callback executor function. If a hook function is provided to the renderer, this
     * will execute it passing the respective element and data entry along with all entries and
     * the raw tile data. This is called by the HtmlTile object, should not be called manually.
     * @memberof Renderer
     * @private
     *
     * @param {HTMLCollection} elements - A collection of html elements.
     * @param {Array} entries - The array of all data entries.
     * @param {Object} data - The raw tile data object.
     */
    Renderer.prototype.executeHooks = function( elements, entries, data ) {

        function execHook( index, elem ) {
            hook( elem, entries[index], entries, data );
        }

        var hooks = this.spec.hook ? [ this.spec.hook ] : this.spec.hooks,
            selector = this.getEntrySelector(),
            hook,
            $elements,
            i;
        if ( hooks ) {
            for ( i=0; i<hooks.length; i++ ) {
                hook = hooks[i];
                if ( typeof hook === "function" ) {
                    // get the entries
                    $elements = $( elements );
                    // if entry selector is set, use it to select entries
                    if ( selector ) {
                        $elements = $elements.find( selector );
                    }
                    // call entry function on each entry
                    $elements.each( execHook );
                }
            }
        }
    };

    module.exports = Renderer;
}());
