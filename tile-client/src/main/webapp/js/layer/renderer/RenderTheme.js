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

define( function( require ) {
    "use strict";

    var RendererUtil = require('./RendererUtil');

    function RenderTheme( spec ) {
        spec = spec || {};
        spec.color = spec.color || "#fff";
        spec.hoverColor = spec.hoverColor || "#09CFFF";
        spec.blend = spec.blend || "#000";
        this.spec = spec;
    }

    RenderTheme.prototype.injectTheme = function( options ) {

        function outlineCss( outline, attribute ) {
            if ( !outline ) {
                return "";
            }
            if ( attribute === "color" ) {
                return "text-shadow:"
                     +"-1px -1px 0 "+outline+","
                     +" 1px -1px 0 "+outline+","
                     +"-1px  1px 0 "+outline+","
                     +" 1px  1px 0 "+outline+","
                     +" 1px  0   0 "+outline+","
                     +"-1px  0   0 "+outline+","
                     +" 0    1px 0 "+outline+","
                     +" 0   -1px 0 "+outline;
            }
            return "border: 1px solid " + outline;
        }

        var spec = this.spec,
            elemClass = options.elemClass,
            parentClass = options.parentClass,
            attribute = options.attribute,
            outline = outlineCss( spec.outline, attribute ),
            greyColor,
            css;

        greyColor = RendererUtil.hexBlend( RendererUtil.hexGreyscale( spec.color ), spec.blend );

        css = '<style id="'+spec.id+'-'+elemClass+'-theme" type="text/css">';

        css += '.'+spec.id + ' .'+elemClass+' {'+attribute+':'+spec.color+';'+outline+';}';
        if ( parentClass ) {
            css += '.'+spec.id + ' .'+parentClass+':hover .'+elemClass+' {'+attribute+':'+spec.hoverColor+';'+outline+';}';
        } else {
            css += '.'+spec.id + ' .'+elemClass+':hover {'+attribute+':'+spec.hoverColor+';'+outline+';}';
        }
        css += '.'+spec.id + ' .greyed .'+elemClass+'{'+attribute+':'+greyColor+';'+outline+';}';
        css += '.'+spec.id + ' .clicked-secondary .'+elemClass+' {'+attribute+':'+spec.color+';'+outline+';}';
        css += '.'+spec.id + ' .clicked-primary .'+elemClass+' {'+attribute+':'+spec.hoverColor+';'+outline+';}';

        css += '</style>';

        $( document.body ).prepend( css );
    };

    return RenderTheme;
});