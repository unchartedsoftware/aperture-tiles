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

    var RendererUtil = require('./RendererUtil');

    function injectSingleTheme( renderTheme, spec, options, percent ) {
        var theme = renderTheme.selector,
            selector = ( percent !== undefined ) ? options.selector + "-" + percent : options.selector,
            parentSelector = options.parentSelector,
            css;
        css = '<style class="render-theme" type="text/css">';

        // set color
        if ( spec['background-color'] ) {
            css += theme + ' ' + selector + '{background-color:'+spec['background-color']+';}';
        }
        if ( spec.color ) {
            css += theme + ' ' + selector + '{color:' + spec.color + ';}';
        }

        // set :hover color
        if ( parentSelector ) {
            if ( spec['background-color:hover'] ) {
                css += theme + ' '+parentSelector+':hover '+selector+' {background-color:'+spec['background-color:hover']+';}';
            }
            if ( spec['color:hover'] ) {
                css += theme + ' '+parentSelector+':hover '+selector+' {color:'+spec['color:hover']+';}';
            }
        } else {
            if ( spec['background-color:hover'] ) {
                css += theme + ' '+selector+':hover {background-color:'+spec['background-color:hover']+';}';
            }
            if ( spec['color:hover'] ) {
                css += theme + ' '+selector+':hover {color:'+spec['color:hover']+';}';
            }
        }

        // set borders
        if ( spec['text-shadow'] ) {
            css += theme + ' ' + selector + '{' + getOutlineCss( 'text-shadow', spec['text-shadow'] ) + ';}';
        }

        if ( spec.border ) {
            css += theme + ' ' + selector + '{' + getOutlineCss( 'border', spec.border ) + ';}';
        }
        css += '</style>';
        $( document.body ).prepend( css );
    }

    function getOutlineCss( type, value ) {
        function isColor( val ) {
            var split = val.replace(/\s+/g, '').split(/[\(\)]/);
            if ( split[0] === "rgb" || split[0] === "rgba" ) {
                return val;
            }
            return val[0] === "#" && ( val.length === 4 || val.length === 7 );
        }

        if ( !value ) {
            return "";
        }

        if ( type === "text-shadow" ) {
            if ( isColor( value ) ) {
                return "text-shadow:"
                    + "-1px -1px 0 " + value + ","
                    + " 1px -1px 0 " + value + ","
                    + "-1px  1px 0 " + value + ","
                    + " 1px  1px 0 " + value + ","
                    + " 1px  0   0 " + value + ","
                    + "-1px  0   0 " + value + ","
                    + " 0    1px 0 " + value + ","
                    + " 0   -1px 0 " + value;
            }
            return "text-shadow:" + value + ";";
        }
        if ( type === "border" ) {
            if ( isColor( value ) ) {
                return "border: 1px solid " + value;
            }
            return "border:" + value + ";";
        }
        return "";
    }

    /**
     * Instantiate a RenderTheme object.
     * @class RenderTheme
     * @classdesc The RenderTheme class provides a mechanism to style separate components
     * generated from a Renderer object. The theme's CSS is injected into the DOM under the
     * supplied selectors. This allows switching between themes on client rendered tiles by
     * simply appending the respective selector to a parent DOM element.
     *
     * It is the Renderer implementations responsibility to ensure that the RenderTheme
     * object is utilized correctly, as different implementations may require unique
     * styling.
     *
     * An alternative to using RenderThemes would be either bypassing Renderers all together
     * and using the lower level html provider function accepted by client rendered layers, or
     * using the hook function to insert application or theme specific classes into the
     * generated html entries.
     *
     * @param theme {String} The theme identification string. Currently restricted to "dark" and "light".
     * @param spec  {Object} The specification object.
     * <pre>
     * {
     *     color                  {String} - The css color attribute for the component.
     *     color:hover            {String} - The css color attribute for the component, under hover.
     *     background-color       {String} - The css background-color attribute for the component.
     *     background-color:hover {String} - The css background-color attribute for the component, under hover.
     *     text-shadow            {String} - The css text-shadow attribute for the component.
     *     border                 {String} - The css border attribute for the component.
     * }
     * </pre>
     */
    function RenderTheme( theme, spec ) {
        spec = spec || {};
        this.selector = ( theme === 'light' ) ? ".light-theme" : ".dark-theme";
        this.spec = spec;
    }

    /**
     * Injects the themes CSS under the provided selector and parent selector. This should
     * only be called from within a Renderer class implementation, and it is the responsibility
     * of the Renderer implementation to ensure it is used correctly.
     * @private
     *
     * @param {Object} options - The options object containing the selector and parentSelector.
     */
    RenderTheme.prototype.injectTheme = function( options ) {
        var blendSpec,
            from,
            to,
            i;
        if ( this.spec.from && this.spec.to ) {
            from = this.spec.from;
            to = this.spec.to;
            for ( i=0; i<=10; i++ ) {
                blendSpec = {};
                if ( from['background-color'] && to['background-color'] ) {
                     blendSpec['background-color'] = RendererUtil.hexBlend(
                         to['background-color'],
                         from['background-color'],
                         i/10 );
                }
                if ( from.color && to.color ) {
                     blendSpec.color = RendererUtil.hexBlend(
                         to.color,
                         from.color,
                         i/10 );
                }
                if ( from['background-color:hover'] && to['background-color:hover'] ) {
                     blendSpec['background-color:hover'] = RendererUtil.hexBlend(
                         to['background-color:hover'],
                         from['background-color:hover'],
                         i/10 );
                }
                if ( from['color:hover'] && to['color:hover'] ) {
                     blendSpec['color:hover'] = RendererUtil.hexBlend(
                         to['color:hover'],
                         from['color:hover'],
                         i/10 );
                }
                if ( from['text-shadow'] && to['text-shadow'] ) {
                     blendSpec['text-shadow'] = RendererUtil.hexBlend(
                         to['text-shadow'],
                         from['text-shadow'],
                         i/10 );
                }
                if ( from.border && to.border ) {
                     blendSpec.border = RendererUtil.hexBlend(
                         to.border,
                         from.border,
                         i/10 );
                }
                injectSingleTheme( this, blendSpec, options, i*10 );
            }
            injectSingleTheme( this, blendSpec, options );
        } else {
            injectSingleTheme( this, this.spec, options );
        }
    };

    module.exports = RenderTheme;
}());