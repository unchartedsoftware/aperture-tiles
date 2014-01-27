/**
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

/* JSLint global declarations: these objects don't need to be declared. */
/*global OpenLayers */



define( function (require) {
    "use strict";

    var DataLayer = require('./datalayer'),
        layerInfoStatus = {},
        layerInfoCache = {},
        layerInfoCallbacks = {};

    return {

        getDataInfo: function(layerSpec, callback) {

            var layerInfoListener;

            if (layerInfoStatus[layerSpec.layer] === undefined) {

                layerInfoStatus[layerSpec.layer] = "loading";
                layerInfoCache[layerSpec.layer] = {
                   spec : layerSpec
                };

                layerInfoCallbacks[layerSpec.layer] = [];
                layerInfoCallbacks[layerSpec.layer].push(callback);

                // send info request
                layerInfoListener = new DataLayer([layerSpec]);
                layerInfoListener.setRetrievedCallback($.proxy(this.layerInfoCallback, this));
                setTimeout( $.proxy(layerInfoListener.retrieveLayerInfo, layerInfoListener), 0);
                return;
            }

            if (layerInfoStatus[layerSpec.layer] === "loading") {
                layerInfoCallbacks[layerSpec.layer].push(callback);
                return;
            }

            if (layerInfoStatus[layerSpec.layer] === "loaded") {
                callback(layerInfoCache[layerSpec.layer].info);
            }
        },

        layerInfoCallback: function (dataLayer, layerInfo) {

            var i;
            layerInfoCache[layerInfo.layer].info = layerInfo;
            for (i =0; i <layerInfoCallbacks[layerInfo.layer].length; i++ ) {
                layerInfoCallbacks[layerInfo.layer][i](layerInfo);
            }
            layerInfoStatus[layerInfo.layer] = "loaded";
        }
    };


});
