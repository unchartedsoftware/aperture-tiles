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

( function() {

    "use strict";

    module.exports = {

        // Binning
        AreaOfInterestTilePyramid: require('./binning/AreaOfInterestTilePyramid'),
        TileIterator: require('./binning/TileIterator'),
        WebMercatorTilePyramid: require('./binning/WebMercatorTilePyramid'),

        // Layer
        AnnotationLayer: require('./layer/AnnotationLayer'),
        AxisLayer: require('./layer/AxisLayer'),
        BaseLayer: require('./layer/BaseLayer'),
        ServerLayer: require('./layer/ServerLayer'),
        ClientLayer: require('./layer/ClientLayer'),
        Carousel: require('./layer/Carousel'),
        LayerUtil: require('./layer/LayerUtil'),

        // Renderer
        Renderer: require('./layer/renderer/Renderer'),
        RendererUtil: require('./layer/renderer/RendererUtil'),
        GraphLabelRenderer: require('./layer/renderer/GraphLabelRenderer'),
        GraphNodeRenderer: require('./layer/renderer/GraphNodeRenderer'),
        PointAggregateRenderer: require('./layer/renderer/PointAggregateRenderer'),
        PointRenderer: require('./layer/renderer/PointRenderer'),
        TextByFrequencyRenderer: require('./layer/renderer/TextByFrequencyRenderer'),
        TextScoreRenderer: require('./layer/renderer/TextScoreRenderer'),
        TextScoreWeightedRenderer: require('./layer/renderer/TextScoreWeightedRenderer'),
        WordCloudRenderer: require('./layer/renderer/WordCloudRenderer'),
        RenderTheme: require('./layer/renderer/RenderTheme'),

        // Map
        Map: require('./map/Map'),
        MapUtil: require('./map/MapUtil'),
        Axis: require('./map/Axis'),
        AxisUtil: require('./map/AxisUtil'),

        // REST
        AnnotationService: require('./rest/AnnotationService'),
        LayerService: require('./rest/LayerService'),
        LegendService: require('./rest/LegendService'),
        TileService: require('./rest/TileService'),

        // UI
        LayerControls: require('./ui/LayerControls'),

        // Util
        PubSub: require('./util/PubSub'),
        Util: require('./util/Util')
    };

}());
