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



/**
 * A module with methods to enable pre-loading of sets of JSON files from the
 * server.  This is essentially the equivalent of a static class in Java -
 * there are no constructors, only essentially static methods.
 */
define({
    /**
     * Takes a javascript object, and creates a copy identical except that all 
     * its keys are fully lower-case.
     *
     * @param object The object to sanitize to lower-case
     * @param depth The depth in the object tree to search for upper-case keys 
     *        to change.
     */
    downcaseObjectKeys: function (obj, depth) {
        "use strict";
        var newObj, key, value;

	// Make sure we have something to do
        if (0 === depth) { return obj; }
	if (!("object" === typeof obj)) { return obj; }

        // Default to a depth of 1 (but note above that an explicit 0 is a no-op)
        if (!depth) { depth = 1; }

	// Make the appropriate type of return object
	if (Array.isArray(obj)) {
	    newObj = [];
	} else {
	    newObj = {};
	}

	// Loop over all keys, downcasing as necessary
        for (key in obj) {
            if (obj.hasOwnProperty(key)) {
                // Recurse into the value to downcase it if needed
                value = obj[key];
                if ("object" === typeof value) {
                    value = this.downcaseObjectKeys(value, depth-1);
                }

                // Change the key if necessary
                if ("string" === typeof key) {
                    newObj[key.toLowerCase()] = value;
                } else {
                    newObj[key] = value;
                }
            }
        }

        return newObj;
    },

    /**
     * Fetch a set of JSON files, returning each one to the provided callback 
     * function as it is reterned.
     *
     * @param ... All arguments but the last are names of the files to load.
     * @param callback The last argument is a callback that is called after
     *        all the files are loaded.  It is called with a single argument -
     *        an object with the file names as properties, and the loaded 
     *        JSON objects as their values.
     */
    loadJSONData: function (dummyFileArgs, dummyCallbackArg) {
        "use strict";
        var i, files, file, fileCallback, callback, leftToLoad, result;
        result = {};

        if (arguments.length < 1) { return; }
        files = Array.prototype.slice.call(arguments, 0);
        callback = files.pop();
        if (!("function" === typeof callback)) { return; }
        // We have a valid callback.  See if we have any valid files to load.
        if (files.length < 1) {
            callback({});
            return;
        }

        // Save a copy of the files we need to load so we can tell when 
        // we're done.
        leftToLoad = files.slice(0);

        // Set up what to do when we recieve each piece of data.
        fileCallback = function (file) {
            return function (data) {
                // If failed, data should be undefined

                var index = leftToLoad.indexOf(file);
                // Make sure not to double-process any data.
                if (-1 === index) { return; }

                // Remove this file from the list of ones remaining
                leftToLoad.splice(index, 1);
                // Add the data to our return object
                result[file] = data;

                // See if we're done, and if we are, use our callback parameter
                if (leftToLoad.length < 1) {
                    callback(result);
                }
            };
        };

        // Request all our data.
        for (i=0; i<files.length; ++i) {
            file = files[i];
            $.get(file, fileCallback(file), "json").fail(fileCallback(file));
        }
    }
});
