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

/**
 * An overridden OpenLayers.Layer object to use the HtmlTile object to
 * create client rendered elements. Uses either a Renderer or html function
 * to generate the html.
 */
( function() {

    "use strict";

    var OpenLayers = require('../openlayers/OpenLayers.2.12.min'),
        HtmlTile = require('./HtmlTile');

    OpenLayers.Layer.HTML = function( name, url, options ) {
        OpenLayers.Layer.Grid.call( this, name, url, options );
        this.getURL = options.getURL;
        this.layername = options.layername;
        this.type = options.type;
        this.tileClass = HtmlTile;
        this.html = options.html;
        this.renderer = options.renderer;
        this.CLASS_NAME = 'OpenLayers.Layer.HTML';
    };

    OpenLayers.Layer.HTML.prototype = Object.create( OpenLayers.Layer.Grid.prototype );

    module.exports = OpenLayers.Layer.HTML;
}());
