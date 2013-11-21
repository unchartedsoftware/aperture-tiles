/* Simple JavaScript Inheritance
 * By John Resig http://ejohn.org/
 * MIT Licensed.
 *
 * Original source: http://ejohn.org/blog/simple-javascript-inheritance/
 *
 * Modified:
 *   Slightly for readability
 *   So that it will pass jslint
 *   Added profiling output
 * by Nathan Kronenfeld
 */
// Inspired by base2 and Prototype
define([], function () {
    "use strict";
    var initializing = false, result, _contexts, _rootContext, _byMethod;

    // Set up profiling contexts
    _rootContext = {};
    _byMethod = {};
    _contexts = [_rootContext];

    // The base Class implementation (does nothing)
    result = function(){ return undefined; };

    // Allow access to profiling output
    result.getProfileInfo = function () {
        return {inContext: _rootContext,
                byMethod: _byMethod};
    };

    // Create a new Class that inherits from this class
    result.extend = function extendClass (prop) {
        var prototype, name, callSuper, profile,
            _super = this.prototype;
   
        // Instantiate a base class (but only create the instance,
        // don't run the init constructor)
        initializing = true;
        prototype = new this();
        initializing = false;

        // Record profiling information on this function
        profile = function (className, methodName, fcn) {
            var key = className + "." + methodName;
            return function () {
                var startTime, endTime, elapsedTime, currentContext;
                currentContext = _contexts[_contexts.length-1];
                if (!currentContext[key]) {
                    currentContext[key] = {totalTime: 0.0,
                                           totalCalls: 0};
                }
                // Enter a new level for the current method.
                currentContext = currentContext[key];
                _contexts.push(currentContext);
                startTime = window.performance.now();
                try {
                    return fcn.apply(this, arguments);
                } finally {
                    endTime = window.performance.now();
                    elapsedTime = endTime - startTime;
                    if (!_byMethod[key]) {
                        _byMethod[key] = elapsedTime;
                    } else {
                        _byMethod[key] = _byMethod[key] + elapsedTime;
                    }
                    currentContext.totalTime = elapsedTime + currentContext.totalTime;
                    currentContext.totalCalls = currentContext.totalCalls + 1;
                    _contexts.pop();
                }
            };
        };

        // Copy the properties over onto the new prototype
        callSuper = function (name, fn) {
            return function () {
                var ret, tmp = this._super;

                // Add a new ._super() method that is the same method
                // but on the super-class
                this._super = _super[name];

                // The method only need to be bound temporarily, so we
                // remove it when we're done executing
                ret = fn.apply(this, arguments);        
                this._super = tmp;

                return ret;
            };
        };
        for (name in prop) {
            // Check if we're overwriting an existing function
            if (prop.hasOwnProperty(name)) {
                if (typeof prop[name] === "function" &&
                    typeof _super[name] === "function") {
                    prototype[name] = profile(prop.ClassName, name, callSuper(name, prop[name]));
                } else if (typeof prop[name] === "function") {
                    prototype[name] = profile(prop.ClassName, name, prop[name]);
                } else {
                    prototype[name] = prop[name];
                }
            }
        }

        // The dummy class constructor
        function Class() {
            // All construction is actually done in the init method
            if ( !initializing && this.init ) {
                this.init.apply(this, arguments);
            }
        }

        // Populate our constructed prototype object
        Class.prototype = prototype;

        // Enforce the constructor to be what we expect
        Class.prototype.constructor = Class;

        // And make this class extendable
        Class.extend = extendClass;

        return Class;
    };

    return result;
});
