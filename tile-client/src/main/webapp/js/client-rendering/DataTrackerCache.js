/**
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

/* JSLint global declarations: these objects don't need to be declared. */
/*global OpenLayers */


/**
 * This module allows the client to send arrays of layer specs along with a callback function to be
 * executing upon constructing all requested data trackers.
 */
define( function (require) {
    "use strict";

    var DataLayer = require('../datalayer'),
		DataTracker = require('./DataTracker'),
        layerStatus = {},
        layerCache = {},
		pendingRequests = []
        ;
		
		
	return {

        get: function(layerSpecs, callback) {

			var layer,
				spec,
				layerInfoListener,
				pendingIndex,
				i;

			// add to pending requests
			pendingRequests.push( { 
				callback : callback,
				layers : [],
				trackers : []
			});
			
			// the index of this request
			pendingIndex = pendingRequests.length-1;
			
			for (i=0; i<layerSpecs.length; i++) {
           
				spec = layerSpecs[i];
				layer = spec.layer;
		   		
				// prevent duplicate layer requests
				if (pendingRequests[pendingIndex].layers.indexOf(layer) === -1) {
					
					// add layer to pending request, once these are all received, call callback function
					pendingRequests[pendingIndex].layers.push(layer);
					   
					if (layerStatus[layer] === undefined) {
						// layer has not been requested, set its status to loading, and create cache entry
						layerStatus[layer] = "loading";
						layerCache[layer] = {
						   spec : spec
						};

						// send info request to server
						layerInfoListener = new DataLayer([spec]);
						layerInfoListener.setRetrievedCallback($.proxy(this.layerInfoReceivedCallback, this));
						setTimeout( $.proxy(layerInfoListener.retrieveLayerInfo, layerInfoListener), 0);
						return;
					}

					if (layerStatus[layer] === "loading") {
						// layer has already been requested, and is still waiting on server 
						return;
					}

					if (layerStatus[layer] === "loaded") {
						// layer info is held in cache
						this.processPendingRequest(pendingIndex, layer);		
						return;
					}
				}
				
			}
        },

		
        layerInfoReceivedCallback: function (dataLayer, layerInfo) {
		
			var layer = layerInfo.layer,
				i; //, index;
			
			// flag as loaded
            layerStatus[layer] = "loaded";
			// create data tracker object
			layerCache[layer].info = layerInfo;
			layerCache[layer].tracker = new DataTracker(layerInfo);
			
			// find all pending requests that require this layer info
			for (i=pendingRequests.length-1; i>=0; i--) {
			
				this.processPendingRequest(i, layer);		
			}
        },
		
		
		processPendingRequest: function(requestIndex, layer) {
		
			var pendingRequest = pendingRequests[requestIndex],
				index = pendingRequest.layers.indexOf(layer);
			
			// is this request is waiting on this layer?	
			if (index !== -1) {					
				// remove layer from pending list
				pendingRequest.layers.splice(index, 1);
				// store data tracker with request
				pendingRequest.trackers.push(layerCache[layer].tracker);
				// if no more layers pending, call callback function
				if (pendingRequest.layers.length === 0) {
					// call callback function
					pendingRequest.callback(pendingRequest.trackers);
					// remove this pending request
					pendingRequests.splice(requestIndex, 1);				
				}
			}		
		
		}
		
    };	
	

});
