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

 

define( function (require) {
    "use strict";
		
    var AnnotationLayer = require('./AnnotationLayer'),
        annotationDeferred;

	return {
	
		createLayers: function(layersJSON, map) {
			var i;
			for (i=0; i<layersJSON.length; i++) {
				this.createLayer(layersJSON[i], map);
			}
		},
	

		createLayer: function(layerJSON, map) {

			var spec = {
				map: map,
				layer: layerJSON.id,
                priorities: layerJSON.priorities,
				filters: layerJSON.filters
			};
			return new AnnotationLayer( spec );

		},


        /**
         * Receive all annotation layers from server
         * @param callback     the callback that is called upon receiving data from server
         */
        requestLayers: function() {
	        if (!annotationDeferred) {
		        annotationDeferred = $.Deferred();

		        // Request the layer information
		        aperture.io.rest('/annotation',
		                         'POST',
		                         function (layers) {
			                         annotationDeferred.resolve(layers);
		                         },
		                         {
			                         postData: {
				                         "type": "list"
			                         },
			                         contentType: 'application/json'
		                         });
	        }
	        return annotationDeferred;
        }
    };	
});
