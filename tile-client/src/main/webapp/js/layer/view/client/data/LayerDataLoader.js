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
 * This module allows the client to pass an array of layer requirements along with a callback function. Upon
 * loading all requirements, the callback function is executed
 */
define( function (require) {
    "use strict";

    var TileService = require('./TileService'),
        requestStatus = {},
        requestCache = {},
		pendingRequests = []
        ;
			
	return {
	

		/**
		 * Given an array of arguments, of layer specification JSON objects, upon loading all layer info objects from server, 
		 * executes callback function, passing array of data trackers as argument
		 *
		 * Ensures that only 1 instance of all requested objects is ever loaded, even among different individual calls.
		 *
		 * @param args		array of arguments, each of the form:
		 *					arg = {
		 *						type : 	// the type of data to be loaded, this may be used to treat return values individual, ex "data-tracker"
		 *						id : 	// map key of argument
		 *						func : 	// the async loading function of the form func(arg.spec, callback), 
		 *								// takes attribute below (spec) as first argument, second argument is a function called after func completes
		 *								
		 *						spec : 	// the spec object that will be passed to the async func
		 *					}		
		 * @param callback		the callback function called after all requirements are in memory, passed a map of all loaded data
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
		   		
				// prevent duplicate arg requests in single call
				if (pendingRequests[pendingIndex].ids.indexOf(arg.id) === -1) {
					
					// add arg to pending request array, once these are all received, call callback function
					pendingRequests[pendingIndex].ids.push(arg.id);
					   
					if (requestStatus[arg.id] === undefined) {
						// arg has not been requested before, set its status to loading, and create cache entry
						requestStatus[arg.id] = "loading";
						requestCache[arg.id] = {
							type : arg.type,
						    spec : arg.spec
						};

						arg.func(arg.spec, this.createRequireCallback(arg.id));						
					/* 
					} else if (requestStatus[arg.id] === "loading") {
						// arg has already been requested, and is currently pending, do nothing
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
				if (requestCache[id].type === "tile-service") {
					// create data tracker object
					requestCache[id].data = new TileService(data.layerInfos[id]);
				} else {
					// store raw data
					requestCache[id].data = data;
				}
				
				// find all pending requests that require this arg
				for (i=pendingRequests.length-1; i>=0; i--) {
				
					that.processPendingRequest(i, id);		
				}
			};
			
		},
		
		
		processPendingRequest: function(requestIndex, id) {
		
			var pendingRequest = pendingRequests[requestIndex],
				index = pendingRequest.ids.indexOf(id);
			
			// is this request is waiting on this arg?	
			if (index !== -1) {					
				// remove arg from pending list
				pendingRequest.ids.splice(index, 1);
				// store data tracker with request
				pendingRequest.data[id] = requestCache[id].data;
				// if no more args pending, call callback function
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
