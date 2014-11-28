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

    var Renderer = require('./Renderer'),
        RendererUtil = require('./RendererUtil');

    function GraphLabelRenderer( spec ) {
        Renderer.call( this, spec );
        this.setStyles();
    }

    GraphLabelRenderer.prototype = Object.create( Renderer.prototype );

    GraphLabelRenderer.prototype.setStyles = function() {
        var i;
        if ( this.spec.text.themes ) {
            for ( i=0; i<this.spec.text.themes.length; i++ ) {
                this.spec.text.themes[i].injectTheme({
                    elemClass: "node-label",
                    attribute: "color"
                });
            }
        }
    };

    GraphLabelRenderer.prototype.createHtml = function( data ) {

        var GRAPH_COORD_RANGE = 256,
            text = this.spec.text,
            meta = this.meta[ this.map.getZoom() ],
            communities = data.tile.values[0].value[0].communities,
            scale = Math.pow( 2, this.map.getZoom() ),
            range =  GRAPH_COORD_RANGE / scale,
            labelIndex = ( text.labelIndex !== undefined ) ? text.labelIndex : 0,
            metaCommunities,
            community,
            html = "",
            fontScale,
            fontSize,
            split,
            label,
            countNorm,
            percent,
            hierLevel,
            parentIDarray = [],
            x, y, i;

        function capitalize( str ) {
            return str.replace(/(?:^|,|\s)\S/g, function( a ) {
                return a.toUpperCase();
            });
        }

        metaCommunities = meta.minMax.max.communities[0];
        // get graph hierarchy level for this zoom level
        // assumes same hierarchy level for all tiles at a given zoom level
        hierLevel = metaCommunities.hierLevel;
        // if hierLevel = 0, normalize label attributes by community degree
        // else normalize label attributes by num internal nodes
        countNorm = ( hierLevel === 0 ) ? metaCommunities.degree/2 : metaCommunities.numNodes/2;

        for (i=0; i<communities.length; i++) {

            community = communities[i];

            // capitalize label array, split by comma
            split = community.metadata.split(",");
            // don't draw labels for isolated communities
            // and don't render if label string is empty
            // only draw one label per parent community per tile
            // only draw up to 5 labels per tile
            if ( community.degree === 0 ||
                 split[ labelIndex ] === "" ||
                 parentIDarray.length >= 5 ||
                 parentIDarray.indexOf( community.parentID ) !== -1 ) {
                // Skip this label entry
                continue;
            }
            else {
                // add this parent ID to the list, and draw the label (below
                parentIDarray.push( community.parentID );
            }

            // get label position
            x = ( community[ text.x ] % range ) * scale;
            y = ( community[ text.y ] % range ) * scale;

            // capitalize label
            label = capitalize( split[ labelIndex ].toLowerCase() );

            // get font scale based on hierarchy level
            fontScale = ( hierLevel === 0 ) ? community.degree : community.numNodes;
            fontSize = RendererUtil.getFontSize( fontScale, countNorm );

            percent = Math.min( 1, fontScale / countNorm ) + 0.5;

            html += '<div class="node-label" style="'
                  + 'left:'+x+'px;'
                  + 'bottom:'+y+'px;'
                  + 'font-size:' + fontSize + 'px;'
                  + 'line-height:' + fontSize + 'px;'
                  + 'margin-top:' + (-fontSize/2) + 'px;'
                  + 'height:' + fontSize + 'px;'
                  + 'color:' + RendererUtil.hexBlend( "#fff", "#000", percent ) + ";"
                  + 'z-index:' + Math.floor( fontSize ) + ';'
                  + '">'+label+'</div>';
        }

        return html;
    };

    return GraphLabelRenderer;
});