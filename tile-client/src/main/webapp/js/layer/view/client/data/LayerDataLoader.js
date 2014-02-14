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
 * This module allows the client to pass an array of layer requirements along with a callback function. Upon
 * loading all required data trackers, the callback function is executed
 */
define( function (require) {
    "use strict";

    var DataTracker = require('./DataTracker'),
        requestStatus = {},
        requestCache = {},
		pendingRequests = []
        ;
			
	return {
	

		/**
		 * Given an array of layer specification JSON objects, upon loading all layer info objects from server, 
		 * executes callback function, passing array of data trackers as argument
		 * @param layerSpecs	array of layer specification JSON objects
		 * @param callback		the callback function called after all data trackers are loaded in memory
		 */
        get: function(args, callback) {

			var arg,
				pendingIndex,
				i;

			// add to pending requests
			pendingRequests.push( { 
				callback : callback,
				ids: [],
				data: {}
			});
			
			// the index of this request
			pendingIndex = pendingRequests.length-1;
			
			for (i=0; i<args.length; i++) {
           
				arg = args[i];
		   		
				// prevent duplicate layer requests
				if (pendingRequests[pendingIndex].ids.indexOf(arg.id) === -1) {
					
					// add layer to pending request, once these are all received, call callback function
					pendingRequests[pendingIndex].ids.push(arg.id);
					   
					if (requestStatus[arg.id] === undefined) {
						// layer has not been requested, set its status to loading, and create cache entry
						requestStatus[arg.id] = "loading";
						requestCache[arg.id] = {
							type : arg.type,
						    spec : arg.spec
						};

						arg.func(arg.spec, this.createRequireCallback(arg.id));
						
					/* } else if (requestStatus[arg.id] === "loading") {
						// layer has already been requested, and is still waiting on server 
					*/
					} else if (requestStatus[arg.id] === "loaded") {
						// layer info is held in cache
						this.processPendingRequest(pendingIndex, arg.id);							
					}
				}			
			}
        },

		
		createRequireCallback: function(id) {
			
			var that = this;
			
			return function (data) {

				var i;								
				// flag as loaded
				requestStatus[id] = "loaded";
				if (requestCache[id].type === "data-tracker") {
					// create data tracker object
					requestCache[id].data = new DataTracker(data.layerInfos[id]);
				} else {
					// store raw data
					requestCache[id].data = data;
				}
				
				// find all pending requests that require this layer info
				for (i=pendingRequests.length-1; i>=0; i--) {
				
					that.processPendingRequest(i, id);		
				}
			};
			
		},
		
		
		processPendingRequest: function(requestIndex, id) {
		
			var pendingRequest = pendingRequests[requestIndex],
				index = pendingRequest.ids.indexOf(id);
			
			// is this request is waiting on this layer?	
			if (index !== -1) {					
				// remove layer from pending list
				pendingRequest.ids.splice(index, 1);
				// store data tracker with request
				pendingRequest.data[id] = requestCache[id].data;
				// if no more layers pending, call callback function
				if (pendingRequest.ids.length === 0) {
					// call callback function
					pendingRequest.callback(pendingRequest.data);
					// remove this pending request
					pendingRequests.splice(requestIndex, 1);				
				}
			}		
		
		}
		
    };	
	
});
