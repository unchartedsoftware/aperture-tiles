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
 * This module handles communications with the server to get the list of 
 * layers the server can serve.
 */
define(function (require) {
    "use strict";

    var Class = require('../class'),
        AllLayers;


    AllLayers = Class.extend({
        ClassName: "AllLayers",
        init: function () {
            this.layers = 0;
            this.callbacks = [];
        },

        requestLayers: function (callback) {
            if (this.layers) {
                callback(this.layers);
            } else {
                this.callbacks.push(callback);
                aperture.io.rest('/layer',
                                 'POST',
                                 $.proxy(this.onLayersListRetrieved, this),
                                 {
                                     postData: {
                                         requestType: "list"
                                     },
                                     contentType: 'application/json'
                                 }
                                );
            }
        },

        onLayersListRetrieved: function (layers, statusInfo) {
            if (!statusInfo.success) {
                return;
            }
            var callbacks = this.callbacks;
            this.callbacks = [];
            callbacks.foreach(function(callback) {
                callback(layers);
                    });
        }
    });

    return AllLayers;
});
