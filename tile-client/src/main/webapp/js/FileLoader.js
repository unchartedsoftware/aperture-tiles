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

	    // Loop over all keys, downcaseing as necessary
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

    loadJSONDatum: function (file) {
        "use strict";
        var result = $.Deferred();
        $.get(file, function (contents) {
            result.resolve(contents);
        }, "json");
        return result;
    },

    /**
     * Fetch a set of JSON files, returning each one to the provided callback 
     * function as it is returned.
     *
     * @param ... All arguments but the last are names of the files to load.
     */
    loadJSONData: function () {
        "use strict";
        var result, fileDeferreds, fileMap, addToMap, i;

        result = $.Deferred();
        fileDeferreds = [];
        fileMap = {};
        addToMap = function (file) {
            return function (contents) {
                fileMap[file] = contents;
            };
        };

        // Request all our data.
        for (i=0; i<arguments.length; ++i) {
            fileDeferreds[i] = this.loadJSONDatum(arguments[i]).then(addToMap(arguments[i]));
        }

        $.when.apply(this, fileDeferreds).done(function () {
            result.resolve(fileMap);
        }).fail(function () {
            result.fail();
        });

        return result;
    }
});
