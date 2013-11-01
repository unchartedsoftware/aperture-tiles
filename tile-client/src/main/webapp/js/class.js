/* Simple JavaScript Inheritance
 * By John Resig http://ejohn.org/
 * MIT Licensed.
 *
 * Original source: http://ejohn.org/blog/simple-javascript-inheritance/
 *
 * Slightly modified for readability, and so that it will pass jslint, by Nathan Kronenfeld
 */
// Inspired by base2 and Prototype
define([], function () {
    "use strict";
    var initializing = false, result;

    // The base Class implementation (does nothing)
    result = function(){ return undefined; };

    // Create a new Class that inherits from this class
    result.extend = function extendClass (prop) {
        var prototype, name, callSuper,
            _super = this.prototype;
   
        // Instantiate a base class (but only create the instance,
        // don't run the init constructor)
        initializing = true;
        prototype = new this();
        initializing = false;

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
                    prototype[name] = callSuper(name, prop[name]);
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
