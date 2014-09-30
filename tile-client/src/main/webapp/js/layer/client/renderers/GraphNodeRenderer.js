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


define(function (require) {
    "use strict";



    var //Util = require('../../../util/Util'),
        GenericHtmlRenderer = require('./GenericHtmlRenderer'),
        GraphNodeRenderer;



    GraphNodeRenderer = GenericHtmlRenderer.extend({
        ClassName: "GraphNodeRenderer",

        init: function( map, spec ) {
            this._super( map, spec );
        },


        parseInputSpec: function( spec ) {
            spec.node = this.parseStyleSpec( spec.node );
            spec.criticalNode = this.parseStyleSpec( spec.criticalNode );
            spec.parentNode = this.parseStyleSpec( spec.parentNode );
            return spec;
        },


        getSelectableElement: function() {
            return 'community-node';
        },


        createThemedCss: function( subSpec, className ) {
            var theme,
                themeName,
                color,
                hoverColor,
                outline,
                css = "";

            for ( themeName in subSpec.themes ) {
                if ( subSpec.themes.hasOwnProperty( themeName ) ) {
                    theme = subSpec.themes[ themeName ];
                    color = theme.color[0];
                    hoverColor = theme.hoverColor[0];
                    outline = ( theme.outline ) ? 'border: 2px solid ' + theme.outline : '';
                    css += '.'+themeName + ' .' +className + '{background-color:'+color+'; '+outline+'; }';
                    css += '.'+themeName + ' .' +className + ':hover {background-color:'+hoverColor+'; '+outline+'; }';
                }
            }
            return css;
        },

        createUnThemedCss: function( subSpec, className ) {
            var css = "",
                color,
                hoverColor,
                outline;

            color = subSpec.color[0];
            hoverColor = subSpec.hoverColor[0];
            outline = ( subSpec.outline ) ? 'border: 2px solid ' + subSpec.outline : '';

            css += '.' +className + '{ background-color:'+color+'; '+outline+'; }';
            css += '.' +className + ':hover { background-color:'+hoverColor+'; '+outline+'; }';
            return css;
        },

        createStyles: function() {

            var spec = this.spec,
                css;

            css = '<style id="generic-graph-node-renderer-css-'+this.id+'" type="text/css">';

            if ( spec.node.color ) {
                css += this.createUnThemedCss( spec.node, 'community-node' );
            } else if ( spec.node.themes ) {
                css += this.createThemedCss( spec.node, 'community-node' );
            }

            if ( spec.criticalNode.color ) {
                css += this.createUnThemedCss( spec.criticalNode, 'community-critical-node' );
            } else if ( spec.criticalNode.themes ) {
                css += this.createThemedCss( spec.criticalNode, 'community-critical-node' );
            }

            if ( spec.parentNode.color ) {
                css += this.createUnThemedCss( spec.parentNode, 'community-parent-node' );
            } else if ( spec.parentNode.themes ) {
                css += this.createThemedCss( spec.parentNode, 'community-parent-node' );
            }

            css += '</style>';

            $( document.body ).prepend( css );
        },

        createHtml : function( data ) {

            var spec = this.spec,
                values = data.values,
                tilekey = data.tilekey,
                value, community, className,
                tilePos = this.map.getTopLeftViewportPixelForTile( tilekey ),
                pos, offset, parentPos, parentOffset, radius, parentRadius,
                $html = $([]),
                $node,
                i, j;


            for (i=0; i<values.length; i++) {

                value = values[i];

                for (j=0; j<value.communities.length; j++) {

                    community = value.communities[j];
                    pos = this.map.getViewportPixelFromCoord( community[spec.node.x], community[spec.node.y] );
                    radius = this.map.getViewportPixelFromCoord( community[spec.node.radius], 0 ).x - this.map.getViewportPixelFromCoord( 0, 0 ).x;
                    radius *= 2;
                    radius -= 1;
                    offset = {
                        x : pos.x - tilePos.x,
                        y : pos.y - tilePos.y
                    };

                    parentPos = this.map.getViewportPixelFromCoord( community[spec.parentNode.x], community[spec.parentNode.y] );
                    parentRadius = this.map.getViewportPixelFromCoord( community[spec.parentNode.radius], 0 ).x - this.map.getViewportPixelFromCoord( 0, 0 ).x;
                    parentRadius *= 2;
                    parentRadius -= 1;
                    parentOffset = {
                        x : parentPos.x - tilePos.x,
                        y : parentPos.y - tilePos.y
                    };

                    if ( community[spec.criticalNode.flag] ) {
                        className = 'community-node community-critical-node';
                    } else {
                        className = 'community-node';
                    }

                    $node = $( '<div class="'+className+'" style="'
                          + 'height:'+radius+'px;'
                          + 'width:'+radius+'px;'
                          + 'border-radius:'+radius+'px;'
                          + 'left:'+(offset.x - radius/2)+'px;'
                          + 'top:'+(offset.y - radius/2)+'px;'
                          + '"></div>' );

                    $html = $html.add( $node );

                    if ( community[spec.criticalNode.flag] ) {
                        $html = $html.add( '<div class="community-parent-node" style="'
                             + 'height:'+parentRadius+'px;'
                             + 'width:'+parentRadius+'px;'
                             + 'border-radius:'+parentRadius+'px;'
                             + 'left:'+(parentOffset.x - parentRadius/2)+'px;'
                             + 'top:'+(parentOffset.y - parentRadius/2)+'px;'
                             + '"></div>' );
                    }
                }
            }

            return $html;
        }
    });

    return GraphNodeRenderer;
});