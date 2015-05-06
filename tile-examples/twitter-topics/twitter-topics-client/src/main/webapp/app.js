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
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

var appStart = function() {

    "use strict";

    /* Request layers from server, returning an array of
     * layer configuration objects.
     */
    tiles.LayerService.getLayers( function( layers ) {

        /*
         * Parse layers into an object keyed by layer id, this function also
         * parses the meta data json strings into their respective runtime objects
         */
        layers = tiles.LayerUtil.parse( layers.layers );

        var map,
            axis0,
            axis1,
            baseLayer,
            darkRenderTheme,
            wordCloudRenderer,
            clientLayer,
            serverLayer;

        /*
         * Instantiate the baselayer object, we are using a Google maps baselayer
         * so type is set to "Google", this baselayer is associated with the "dark"
         * theme, Google Maps specific options are passed under the "options" node.
         *
         * type    {String}  - The type of baselayer, ["Blank", "Google", "TMS"]. Default = "Blank"
         * opacity {float}   - The opacity of the layer. Default = 1.0
         * enabled {boolean} - Whether the layer is visible or not. Default = true
         * url     {String}  - if TMS layer, the url for tile requests. Default = undefined
         * options {Object}  - type specific instantiation attributes. Default = {color:rgb(0,0,0)}
         *
         */
        baseLayer = new tiles.BaseLayer({
            type: "Google",
            options : {
               styles : [
                    { featureType: "all",
                      stylers : [ { invert_lightness : true },
                                    { saturation : -100 },
                                    { visibility : "simplified" } ] },
                    { featureType: "administrative",
                      elementType: "geometry",
                        stylers: [ { visibility: "off" } ] },
                    { featureType : "landscape.natural.landcover",
                      stylers : [ { visibility : "off" } ] },
                    { featureType : "road",
                      stylers : [ { visibility : "on" } ] },
                    { featureType : "landscape.man_made",
                      stylers : [ { visibility : "off" } ] },
                    { featureType : "landscape",
                      stylers : [ { lightness : "-100" } ] },
                    { featureType : "poi",
                      stylers : [ { visibility : "off" } ] },
                    { featureType : "administrative.country",
                      elementType : "geometry",
                      stylers : [ { visibility : "on" },
                                    { lightness : -56 } ] },
                    { elementType : "labels",
                      stylers : [ { lightness : -46 },
                                    { visibility : "on" } ] }
                ]
            }
        });

        /*
         * Instantiate a server rendered layer, passing the "tweet-heatmap" layer as its source.
         *
         * opacity {float}   - The opacity of the layer. Default = 1.0
         * enabled {boolean} - Whether the layer is visible or not. Default = true
         * zIndex {integer}  - The z index of the layer. Default = 1
         * renderer: {
         *     coarseness {integer} - The pixel by pixel coarseness. Default based on server configuration.
         *     ramp       {String}  - The color ramp type. Default based on server configuration.
         *     rangeMin   {integer} - The minimum percentage to clamp the low end of the color ramp. Default based on server configuration.
         *     rangeMax   {integer} - The maxiumum percentage to clamp the high end of the color ramp. Default based on server configuration.
         * },
         * valueTransform: {
         *     type {String} - Value transformer type. Default based on server configuration.
         * },
         * tileTransform: {
         *     type {String} - Tile transformer type. Default based on server configuration.
         *     data {Object} - The tile transformer data initialization object. Default based on server configuration.
         * }
         *
         */
        serverLayer = new tiles.ServerLayer({
            source: layers["tweet-heatmap"],
            valueTransform: {
                type: "log10"
            }
        });

        /*
         * Instantiate a render theme object to apply themed css to a render componenet.
         *
         * color                  {String} - The css color attribute for the component.
         * color:hover            {String} - The css color attribute for the component, under hover.
         * background-color       {String} - The css background-color attribute for the component.
         * background-color:hover {String} - The css background-color attribute for the component, under hover.
         * text-shadow            {String} - The css text-shadow attribute for the component.
         * border                 {String} - The css border attribute for the component.
         */
        darkRenderTheme = new tiles.RenderTheme( "dark", {
            'color': "#FFFFFF",
            'color:hover': "#09CFFF",
            'text-shadow': "#000"
        });

        /*
         * Instantiate a word cloud renderer. Attach a render theme and hook function to
         * give access to the renderer DOM element and respective data entry.
         *
         * text: {
         *     textKey  {String} - The attribute for the text in the data entry.
         *     countKey {String} - The attribute for the count in the data entry.
         *     themes   {Array}  - The array of RenderThemes to be attached to this component.
         * }
         * hook {Function} - The hook function that is executed on every rendered entry.
         *
         *     Arguments:
         *         elem    {HTMLElement} - The html element for the entry.
         *         entry   {Object}      - The data entry.
         *         entries {Array}       - All entries for the tile.
         *         data    {Object}      - The raw data object for the tile.
         *
         */
        wordCloudRenderer = new tiles.WordCloudRenderer({
            text: {
                textKey: "topic",
                countKey: "countMonthly",
                themes: [ darkRenderTheme ]
            },
            hook: function( elem, entry, entries, data ) {
                elem.onclick = function() {
                    console.log( elem );
                    console.log( entry );
                };
            }
        });

        /*
         * Instantiate a client renderer layer, passing the "top-tweets" laye as its source.
         *
         * opacity {float}   - The opacity of the layer. Default = 1.0
         * enabled {boolean} - Whether the layer is visible or not. Default = true
         * zIndex  {integer} - The z index of the layer. Default = 1000
         *
         * Rendering options:
         *
         *     renderer {Renderer} - The tile renderer object.
         *
         *          or
         *
         *     html {String|Function|HTMLElement|jQuery} - The html for the tile.
         *
         */
        clientLayer = new tiles.ClientLayer({
            source: layers["top-tweets"],
            renderer: wordCloudRenderer
        });

        /*
         * Instantiate the "Longitude" and "Latitude" axis:
         *
         *  position {String}  - Set the position to the bottom of the map. Default = "bottom"
         *  title    {String}  - Set the title of the axis label. Default = "Axis"
         *  enabled  {boolean} - Have the axis initialize to an open or closed state. Default = true
         *  repeat   {boolean} - Whether or not the axis repeats. Default = false
         *  intervals: {
         *      type        {String}  - Whether the intervals are by "percentage" or by "value". Default = "percentage"
         *      increment   {number}  - The interval increment in. Default = 10
         *      pivot       {number}  - The value from with increments are generated from. Default = 0
         *      scaleByZoom {boolean} - Whether the increments should be scaled by zoom level. Default = true
         *  }
         *  units: {
         *      type     {String}  - The type of unit, ["integer", "decimal", "thousands", "millions", "billions", "degrees"]. Default = "decimal"
         *      decimals {number}  - The number of decimals to display, if applicable. Default = 2
         *      stepDown {boolean} - Whether values should step down by unit type, if applicable. Default = true
         *  }
         */
        axis0 = new tiles.Axis({
            position: 'bottom',
            title: 'Longitude',
            enabled: false,
            repeat: true,
            intervals: {
                type: 'fixed',
                increment: 120,
                pivot: 0
            },
            units: {
                type: 'degrees'
            }
        });

        axis1 =  new tiles.Axis({
            position: 'left',
            title: 'Latitude',
            enabled: false,
            repeat: true,
            intervals: {
                type: 'fixed',
                increment: 60
            },
            units: {
                type: 'degrees'
            }
        });

        /*
         * Instantiate the map, attach it to DOM element with id of "map". No pyramid is provided
         * as default is "WebMercator". Attach all map components.
         *
         * map {String} The id for the DOM to contain the map.
         */
        map = new tiles.Map( "map" );
        map.add( serverLayer );
        map.add( clientLayer );
        map.add( axis0 );
        map.add( axis1 );
        map.add( baseLayer );
        map.zoomTo( -60, -15, 4 );

        /*
         * Create the layer controls and append them to the controls element.
         */
        $( '.controls' ).append( tiles.LayerControls.create( [ clientLayer, serverLayer ] ) );
    });
};
