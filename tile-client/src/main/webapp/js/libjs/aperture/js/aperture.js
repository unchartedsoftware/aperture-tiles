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

/*
 * Aperture
 */
var aperture = (function(aperture){

/**
 * Source: base.js
 * Copyright (c) 2014 Oculus Info Inc.
 * @fileOverview Defines the Aperture namespace and base functions.
 */

/*
 * TODO Provide runtime version, vml vs svg methods
 * TODO Check core dependency order here and report errors?
 */

/**
 * @namespace The root Aperture namespace, encapsulating all
 * Aperture functions and classes.
 */
aperture = (function(aperture) {

	/**
	 * The aperture release version number.
	 * @type String
	 */
	aperture.VERSION = '1.0.0';

	return aperture;

}(aperture || {}));


/*
 * Common functions that are private to all aperture code
 */

/**
 * @private
 * Regular expression that matches fieldName followed by a . or the end of the string.
 * Case insensitive, where fieldName is a string containing letters, numbers _, -, and
 * or $.  Technically any string can be a field name but we need to be a little restrictive
 * here because . and [] are special characters in the definition of nested fields.
 *
 * Used to parse mappings to data object fields
 */
var jsIdentifierRegEx = /([$0-9a-z_\-]+)(\[\])*(\.|$)/ig;

/**
 * @private
 * Function that takes an array of field names (the chain) and an optional index.
 * It will traverse down the field chain on the object in the "this" context and
 * return the result.
 *
 * @param {Array} chain
 *      An array of field identifiers where element of the array represents a
 *      field.  Each field may end with zero or more [] which indicate that an
 *      index into an array field is required.
 * @param {Array} indexes
 *      An array of index numbers to be used to index into any fields ending with []
 *      in the chain array.  The indexes in this array will be used in order with the
 *      []s found in the chain array.  The number of values in this array must match
 *      the number of []s in the chain array.
 *
 * @returns the value of the field specified by the chain and indices arrays
 */
var findFieldChainValue = function( chain, indexes ) {
	// Mutate the chain, shift of front
	var field = chain.shift(),
		arrayIdx, numArrays,
		value;

	// Pop []s off the end using the index
	if( (arrayIdx = field.indexOf('[]')) > 0 ) {
		numArrays = (field.length - arrayIdx)/2;
		// Remove the [] if in the field name (assume is at the end, only way valid)
		field = field.slice(0,arrayIdx);
		// Start by getting the array
		value = this[field];
		if (value == null) {
			return value;
		}
		// Now start digging down through the indexes
		while( numArrays > 0 ) {
			value = value[indexes.shift()];
			numArrays -= 1;
		}
	} else {
		// Straight-up non-indexed field
		value = this[field];
	}

	if( !chain.length ) {
		// Last item in chain, return property
		return value;
	} else {
		// Otherwise, dereference field, continue down the chain
		return findFieldChainValue.call( value, chain, indexes );
	}
};

/**
 * Source: util.js
 * Copyright (c) 2014 Oculus Info Inc.
 * @fileOverview Defines utility functions for Aperture.
 */

/*
 * Portions of this package are inspired by or extended from:
 *
 * Underscore.js 1.2.0
 * (c) 2011 Jeremy Ashkenas, DocumentCloud Inc.
 * Underscore is freely distributable under the MIT license.
 * Portions of Underscore are inspired or borrowed from Prototype,
 * Oliver Steele's Functional, and John Resig's Micro-Templating.
 * For all details and documentation:
 * http://documentcloud.github.com/underscore
 */

/**
 * @namespace Aperture makes use of a number of JavaScript utility
 * functions that are exposed through this namespace for general use.
 */
aperture.util = (function(ns) {

	/**
	 * Instantiates a new object whose JavaScript prototype is the
	 * object passed in.
	 *
	 * @param {Object} obj
	 *      the prototype for the new object.
	 *
	 * @returns {Object}
	 *		a new view of the object passed in.
	 *
	 * @name aperture.util.viewOf
	 * @function
	 */
	ns.viewOf = function(obj) {

		// generic constructor function
		function ObjectView() {}

		// inherit from object
		ObjectView.prototype = obj;

		// new
		return new ObjectView();

	};


	// native shortcuts
	var arr = Array.prototype,
		slice = arr.slice,
		nativeForEach = arr.forEach,
		nativeMap = arr.map,
		nativeFilter = arr.filter,
		nativeIndexOf = arr.indexOf,
		nativeIsArray = Array.isArray,
		nativeBind = Function.prototype.bind,
		hasOwnProperty = Object.prototype.hasOwnProperty,
		toString = Object.prototype.toString,
		ctor = function(){};

	/**
	 * Calls a function for each item in a collection. If ECMAScript 5
	 * is supported by the runtime execution environment (e.g. browser)
	 * this method delegates to a native implementation.
	 *
	 * @param {Array|Object|arguments} collection
	 *      the collection to iterate through.
	 *
	 * @param {Function} operation
	 *      the function to call for each item in the collection, with
	 *      the three arguments
	 *      <span class="fixedFont">(item, indexOrKey, collection)</span>.
	 *
	 * @param [context]
	 *      the optional calling context to use,
	 *      accessible from the operation as <span class="fixedFont">this</span>.
	 *
	 * @name aperture.util.forEach
	 * @function
	 */
	ns.forEach = function ( obj, operation, context ) {
		if ( obj == null ) return;

		// array, natively?
		if ( nativeForEach && obj.forEach === nativeForEach ) {
			obj.forEach( operation, context );

		// array-like?
		} else if ( obj.length === +obj.length ) {
			for (var i = 0, l = obj.length; i < l; i++) {
				i in obj && operation.call(context, obj[i], i, obj);
			}

		// object
		} else {
			for (var key in obj) {
				if (hasOwnProperty.call(obj, key)) {
					operation.call(context, obj[key], key, obj);
				}
			}
		}
	};

	/**
	 * Calls a function for each item in a collection, until the return
	 * value === the until condition.
	 *
	 * @param {Array|Object|arguments} collection
	 *      the collection to iterate through.
	 *
	 * @param {Function} operation
	 *      the function to call for each item in the collection, with
	 *      the three arguments
	 *      <span class="fixedFont">(item, indexOrKey, collection)</span>.
	 *
	 * @param until
	 *      the return value to test for when deciding whether to break iteration.
	 *
	 * @param [context]
	 *      the optional calling context to use,
	 *      accessible from the operation as <span class="fixedFont">this</span>.
	 *
	 * @returns
	 *      the return value of the last function iteration.
	 *
	 * @name aperture.util.forEachUntil
	 * @function
	 */
	ns.forEachUntil = function ( obj, operation, until, context ) {
		if ( obj == null ) return;

		var result;

		// array-like?
		if (obj.length === +obj.length) {
			for (var i = 0, l = obj.length; i < l; i++) {
				if (i in obj && (result = operation.call(context, obj[i], i, obj)) === until) return result;
			}
		// object
		} else {
			for (var key in obj) {
				if (hasOwnProperty.call(obj, key)) {
					if ((result = operation.call(context, obj[key], key, obj)) === until) return result;
				}
			}
		}
		return result;
	};

	/**
	 * Looks through each value in the collection, returning an array of
	 * all the values that pass a truth test. For arrays this method
	 * delegates to the native ECMAScript 5 array filter method, if present.
	 *
	 * @param {Array|Object|arguments} collection
	 *      the collection to search.
	 *
	 * @param {Function} test
	 *      the function called for each item to test for inclusion,
	 *      with the three arguments
	 *      <span class="fixedFont">(item, indexOrKey, collection)</span>
	 *
	 * @param [context]
	 *      the optional calling context to use,
	 *      accessible from the test as <span class="fixedFont">this</span>.
	 *
	 * @returns {Array}
	 *      an array containing the subset that passed the filter test.
	 *
	 * @name aperture.util.filter
	 * @function
	 */
	ns.filter = function ( obj, test, context ) {
		var results = [];

		if ( obj == null ) return results;

		// array, natively?
		if ( nativeFilter && obj.filter === nativeFilter ) {
			return obj.filter( test, context );
		}

		// any other iterable
		ns.forEach( obj, function( value, index ) {
			if ( test.call( context, value, index, obj )) {
				results[results.length] = value;
			}
		});

		return results;
	};

	/**
	 * Produces a new array of values by mapping each item in the collection
	 * through a transformation function. For arrays this method
	 * delegates to the native ECMAScript 5 array map method, if present.
	 *
	 * @param {Array|Object|arguments} collection
	 *      the collection to map.
	 *
	 * @param {Function} transformation
	 *      the function called for each item that returns a transformed value,
	 *      called with the three arguments
	 *      <span class="fixedFont">(item, indexOrKey, collection)</span>
	 *
	 * @param [context]
	 *      the optional calling context to use,
	 *      accessible from the transformation as <span class="fixedFont">this</span>.
	 *
	 * @returns {Array}
	 *      a new array containing the transformed values.
	 *
	 * @name aperture.util.map
	 * @function
	 */
	ns.map = function ( obj, map, context ) {
		var results = [];

		if ( obj != null ) {
			// array, natively?
			if ( nativeMap && obj.map === nativeMap ) {
				return obj.map( map, context );
			}

			// any other iterable
			ns.forEach( obj, function( value, index ) {
				results[results.length] = map.call( context, value, index, obj );
			});
		}

		return results;
	};

	/**
	 * Looks through each value in the collection, returning the first one that
	 * passes a truth test.
	 *
	 * @param {Array|Object|arguments} collection
	 *      the collection to search.
	 *
	 * @param {Function} test
	 *      the function called for each item that tests for fulfillment,
	 *      called with the three arguments
	 *      <span class="fixedFont">(item, indexOrKey, collection)</span>
	 *
	 * @param [context]
	 *      the optional calling context to use,
	 *      accessible from the test as <span class="fixedFont">this</span>.
	 *
	 * @returns
	 *      The item found, or <span class="fixedFont">undefined</span>.
	 *
	 * @name aperture.util.find
	 * @function
	 */
	ns.find = function ( obj, test, context ) {
		var result;

		if ( obj != null ) {
			ns.forEachUntil( obj, function( value, index ) {
				if ( test.call( context, value, index, obj ) ) {
					result = value;
					return true;
				}
			}, true );
		}

		return result;
	};

	/**
	 * Looks through a collection, returning true if
	 * it includes the specified value.
	 *
	 * @param {Array|Object|arguments} collection
	 *      the collection to search.
	 *
	 * @param value
	 *      the value to look for using the === test.
	 *
	 * @returns
	 *      True if found, else false.
	 *
	 * @name aperture.util.has
	 * @function
	 */
	ns.has = function ( collection, value ) {
		if ( !collection ) return false;

		// TODO: use indexOf here if able.
		return !!ns.forEachUntil( collection, function ( item ) {
			return item === value;
		}, true );
	};

	/**
	 * Looks through a collection, returning true if
	 * it contains any of the specified values.
	 *
	 * @param {Array|Object|arguments} collection
	 *      the collection to search.
	 *
	 * @param {Array} values
	 *      the values to look for using the === test.
	 *
	 * @returns
	 *      True if any are found, else false.
	 *
	 * @name aperture.util.hasAny
	 * @function
	 */
	ns.hasAny = function ( collection, values ) {
		if ( !collection || !values ) return false;

		return !!ns.forEachUntil( collection, function ( value ) {
			return ns.indexOf( values, value ) !== -1;
		}, true );
	};

	/**
	 * Looks through a collection, returning true if
	 * it contains all of the specified values. If there
	 * are no values to look for this function returns false.
	 *
	 * @param {Array|Object|arguments} collection
	 *      the collection to search.
	 *
	 * @param {Array} values
	 *      the values to look for using the === test.
	 *
	 * @returns
	 *      True if any are found, else false.
	 *
	 * @name aperture.util.hasAll
	 * @function
	 */
	ns.hasAll = function ( collection, values ) {
		if ( !collection || !values ) return false;

		return !!ns.forEachUntil( values, function ( value ) {
			return ns.indexOf( values, value ) !== -1;
		}, false );
	};

	/**
	 * Returns the index at which value can be found in the array,
	 * or -1 if not present. Uses the native array indexOf function
	 * if present.
	 *
	 * @param {Array} array
	 *      the array to search.
	 *
	 * @param item
	 *      the item to look for, using the === check.
	 *
	 * @returns {Number}
	 *      the index of the item if found, otherwise -1.
	 *
	 * @name aperture.util.indexOf
	 * @function
	 */
	ns.indexOf = function( array, item ) {
		if ( array != null ) {
			if ( nativeIndexOf && array.indexOf === nativeIndexOf ) {
				return array.indexOf( item );
			}

			// array-like?
			for ( var i = 0, l = array.length; i < l; i++ ) {
				if (array[i] === item) return i;
			}
		}
		return -1;
	};

	/**
	 * Returns a copy of the array with the specified values removed.
	 *
	 * @param {Array} array
	 *      the array to remove from.
	 *
	 * @param value
	 *      the item to remove, identified using the === check.
	 *
	 * @param etc
	 *      additional items to remove, as additional arguments.
	 *
	 * @returns {Array}
	 *      a new array with values removed
	 *
	 * @name aperture.util.without
	 * @function
	 */
	ns.without = function( array ) {
		var exclusions = slice.call( arguments, 1 );

		return ns.filter( array,
			function ( item ) {
				return !ns.has( exclusions, item );
		});
	};

	/**
	 * Copy all of the properties in the source object(s) over to the
	 * destination object, in order.
	 *
	 * @param {Object} destination
	 *      the object to extend.
	 *
	 * @param {Object} source
	 *      one or more source objects (supplied as additional arguments)
	 *      with properties to add to the destination object.
	 *
	 * @name aperture.util.extend
	 *
	 * @returns {Object}
	 *      the destination object
	 *
	 * @function
	 */
	ns.extend = function( obj ) {
		ns.forEach( slice.call( arguments, 1 ), function ( source ) {
			for ( var prop in source ) {
				if ( source[prop] !== undefined ) obj[prop] = source[prop];
			}
		});

		return obj;
	};

	/**
	 * Bind a function to an object, meaning that whenever the function is called,
	 * the value of <span class="fixedFont">this</span> will be the object.
	 * Optionally, bind initial argument values to the function, also known
	 * as partial application or 'curry'. This method delegates to a native
	 * implementation if ECMAScript 5 is present.
	 *
	 * @param {Function} function
	 *      the function to wrap, with bound context and, optionally, arguments.
	 *
	 * @param {Object} object
	 *      the object to bind to be the value of <span class="fixedFont">this</span>
	 *      when the wrapped function is called.
	 *
	 * @param [arguments...]
	 *      the optional argument values to prepend when the wrapped function
	 *      is called, which will be followed by any arguments supplied by the caller
	 *      of the bound function returned here.
	 *
	 * @returns {Function}
	 *      the bound function.
	 *
	 * @name aperture.util.bind
	 * @function
	 */
	ns.bind = function bind(func, context) {

		// native delegation
		if (nativeBind && func.bind === nativeBind) {
			return nativeBind.apply(func, slice.call(arguments, 1));
		}

		// must be a function
		if ( !ns.isFunction(func) ) throw new TypeError;

		var args = slice.call(arguments, 2), bound;

		// return the bound function
		return bound = function() {

			// normal call pattern: obj.func(), with curried arguments
			if ( !(this instanceof bound) )
				return func.apply( context, args.concat(slice.call(arguments)) );

			// constructor pattern, with curried arguments.
			ctor.prototype = func.prototype;

			var self = new ctor,
				result = func.apply( self, args.concat(slice.call(arguments)) );

			return (Object(result) === result)? result : self;
		};
	};

	/**
	 * Returns true if argument appears to be a number.
	 *
	 * @param candidate
	 *      the candidate to test.
	 *
	 * @returns true if of the queried type
	 *
	 * @name aperture.util.isNumber
	 * @function
	 */
	ns.isNumber = function( obj ) {
		return !!(obj === 0 || (obj && obj.toExponential && obj.toFixed));
	};

	/**
	 * Returns true if argument appears to be a string.
	 *
	 * @param candidate
	 *      the candidate to test.
	 *
	 * @returns true if of the queried type
	 *
	 * @name aperture.util.isString
	 * @function
	 */
	ns.isString = function(obj) {
		return !!(obj === '' || (obj && obj.charCodeAt && obj.substr));
	};

	/**
	 * Returns true if argument is an array. This method delegates to a native
	 * implementation if ECMAScript 5 is present.
	 *
	 * @param candidate
	 *      the candidate to test.
	 *
	 * @returns true if of the queried type
	 *
	 * @name aperture.util.isArray
	 * @function
	 */
	ns.isArray = nativeIsArray || function(obj) {
		return toString.call(obj) === '[object Array]';
	};

	/**
	 * Returns true if argument appears to be a function.
	 *
	 * @param candidate
	 *      the candidate to test.
	 *
	 * @returns true if of the queried type
	 *
	 * @name aperture.util.isFunction
	 * @function
	 */
	ns.isFunction = function(obj) {
		return !!(obj && obj.constructor && obj.call && obj.apply);
	};

	/**
	 * Returns true if argument appears to be an object.
	 *
	 * @param candidate
	 *      the candidate to test.
	 *
	 * @returns true if of the queried type
	 *
	 * @name aperture.util.isObject
	 * @function
	 */
	ns.isObject = function(obj) {
		return obj === Object(obj);
	};

	return ns;

}(aperture.util || {}));

/**
 * Source: Class.js
 * Copyright (c) 2014 Oculus Info Inc.
 * @fileOverview Simple JavaScript Inheritance
 */

/*
 * Portions of this implementation of a classical inheritance pattern are written
 * by John Resig http://ejohn.org/
 * MIT Licensed.
 *
 * The Resig approach has been extended here to support 'views' of
 * class object instances using the JavaScript prototype model, as
 * well as basic type reflection. A bug fix was added to handled the
 * overrides of toString.
 *
 * Resig ack: Inspired by base2 and Prototype.
 */

/**
 * @ignore
 * Ensure namespace exists
 */
aperture = (
/** @private */
function(namespace) {
	var initializing = false,
		nativeToString = Object.prototype.toString,

		// used in check below at root level
		rootTypeOf,

		// get property names.
		getOwnPropertyNames = function ( properties ) {
			var name, names = [];

			for (name in properties) {
				if (properties.hasOwnProperty(name)) {
					names[names.length] = name;
				}
			}

			// need to make a special case for toString b/c of IE bug
			if (properties.toString !== nativeToString) {
				names[names.length] = 'toString';
			}

			return names;
		},

		// create a new typeof method that checks against the properties of a type
		createTypeOfMethod = function ( name, constructor, superTypeOf ) {
			return function( type ) {
				return !type? name: (type === name || type === constructor
					|| (type.test && type.test(name)))?
							true : (superTypeOf || rootTypeOf)( type );
			};
		};

	// create the root level type checker.
	rootTypeOf = createTypeOfMethod( 'aperture.Class', namespace.Class, function() {return false;} );

	/**
	 * @class
	 * Class is the root of all extended Aperture classes, providing simple, robust patterns
	 * for classical extensibility. An example is provided below showing how to
	 * {@link aperture.Class.extend extend} a new class.
	 *
	 * @description
	 * This constructor is abstract and not intended to be invoked. The examples below demonstrate
	 * how to extend a new class from a base class, and how to add a view constructor to
	 * a base class.
	 *
	 * @name aperture.Class
	 */
	namespace.Class = function() {
	};

	/**
	 * Provides a method of checking class and view type inheritance, or
	 * simply returning the type name. A fully scoped name may be provided,
	 * or a regular expression for matching. Alternatively the class or
	 * view constructor may be passed in.
	 *
	 * @example
	 * var range = new aperture.Scalar('Percent Change GDP').symmetric();
	 *
	 * // check using regular expressions
	 * console.log( range.typeOf(/scalar/i) );         // 'true'
	 * console.log( range.typeOf(/symmetric/) );       // 'true'
	 *
	 * // check using names
	 * console.log( range.typeOf('aperture.Scalar') );  // 'true'
	 * console.log( range.typeOf('aperture.Range') );   // 'true'
	 * console.log( range.typeOf('aperture.Ordinal') ); // 'false'
	 *
	 * // check using prototypes. also 'true':
	 * console.log( range.typeOf( aperture.Scalar ) );
	 * console.log( range.typeOf( aperture.Scalar.prototype.symmetric ) );
	 *
	 * @param {String|RegExp|Constructor} [name]
	 *      the name, regular expression, or constructor of the view or class type to
	 *      check against, if checking inheritance.
	 *
	 * @returns {Boolean|String} if a name argument is provided for checking inheritance,
	 *      true or false indicating whether the object is an instance of the type specified,
	 *      else the name of this type.
	 *
	 *
	 * @name aperture.Class.prototype.typeOf
	 * @function
	 */

	/**
	 * Declares a class method which, when called on a runtime instance
	 * of this class, instantiates a 'view' of the object using
	 * JavaScript's prototype based inheritance, extending its methods
	 * and properties with those provided in the properties parameter.
	 * View methods may access a reference to the base object using the
	 * _base member variable. Views use powerful and efficient features
	 * of JavaScript, however however unlike in the case of Class
	 * extension, some allowances must be made in the design of base
	 * classes to support the derivation of views for correct behavior.
	 * The following provides an example:
	 *
	 * @example
	 *
	 * var ValueClass = aperture.Class.extend( 'ValueClass', {
	 *
	 *     // constructor
	 *     init : function( value ) {
	 *
	 *         // create a common object for all shared variables.
	 *         this.common = { value : value };
	 *     },
	 *
	 *     // sets the value in the object, even if called from a view.
	 *     setValue : function( value ) {
	 *
	 *         // use the common object to set the value for all,
	 *         // as opposed to overriding it locally.
	 *         this.common.value = value;
	 *     },
	 *
	 *     // returns the value.
	 *     getValue : function( value ) {
	 *         return this.common.value;
	 *     }
	 *
	 * });
	 *
	 * // declare a new view constructor
	 * ValueClass.addView( 'absolute', {
	 *
	 *    // optional view constructor, invoked by a call to absolute().
	 *    init : function( ) {
	 *
	 *        // Because of JavaScript prototype inheritance, note that
	 *        // any this.* property value we set will be an override
	 *        // in this view until deleted.
	 *    },
	 *
	 *    // overrides a parent class method.
	 *    getValue : function( ) {
	 *
	 *        // call same getValue method defined in base object.
	 *        // we can choose (but here do not) what this.* should resolve
	 *        // to in the base method call by using function apply() or call().
	 *        return Math.abs( this._base.getValue() );
	 *    }
	 *
	 * });
	 *
	 *
	 * // derive a view of an existing object
	 * var myObj = new MyClass( -2 ),
	 *     myAbsView = myObj.absolute();
	 *
	 * // value now depends on whether you call the base or view
	 * console.log( myObj.getValue() );     // '-2'
	 * console.log( myAbsView.getValue() ); //  '2'
	 *
	 *
	 * @param {String} viewName
	 *      the name of the view method to create, reflective of the type being declared
	 * @param {Object} properties
	 *      a hash of functions to add to (or replace on) the base object when the
	 *      view is created.
	 *
	 * @returns this (allows chaining)
	 *
	 * @name aperture.Class.addView
	 * @function
	 */
	var addView = function(viewName, properties) {
		var viewProto,
			fullName = this.prototype.typeOf() + '.prototype.' + viewName;

		// Create a function on the class's prototype that creates this view
		this.prototype[viewName] = viewProto = function( params ) {
			// First create a derived object
			// generic constructor function
			var ApertureView = function () {};
			// inherit from given instance
			ApertureView.prototype = this;
			// new
			var view = new ApertureView();

			// Provide access to the base object via "_base" member
			// We could check for access to this object and set in a wrapped method
			// like in the class extension below.
			view._base = this;

			aperture.util.forEach( getOwnPropertyNames(properties), function(name) {
				if (name !== 'init') {
					view[name] = properties[name];
				}
			});

			// override the typeOf function to evaluate against this type first, then fall to super.
			view.typeOf = createTypeOfMethod( fullName, viewProto, this.typeOf );

			// Call init (if given)
			if( properties.init ) {
				properties.init.apply( view, arguments );
			}

			return view;
		};

		return this;
	};


	/**
	 * Extends a new class from this class, with any new or overridden properties
	 * and an optional init constructor defined in the properties parameter.
	 * Any methods which are
	 * overridden may call this._super() from within the context of the overridden function
	 * to invoke the parent classes implementation.
	 * A className is supplied as the first parameter for typeOf() evaluation, which
	 * may be omitted for anonymous classes.
	 * Extend may be called on
	 * any previously extended Class object.
	 *
	 * For example:
	 *
	 * @example
	 * var MyClass = MyBaseClass.extend( 'MyClass', {
	 *
	 *    // optionally define constructor
	 *    init : function( exampleArg ) {
	 *
	 *        // optionally call super class constructor
	 *        this._super( exampleArg );
	 *    },
	 *
	 *    // example method override
	 *    exampleMethod : function() {
	 *
	 *        // optionally call same method in parent.
	 *        this._super();
	 *    },
	 *
	 *    ...
	 * });
	 *
	 * // example instantiation
	 * var myObj = new MyClass( exampleArg );
	 *
	 * @param {String} [className]
	 *      an optional type specifier which may be omitted for anonymous classes.
	 *
	 * @param {Object} properties
	 *      a hash of methods and members to extend the class with.
	 *
	 * @returns
	 *      a new class constructor.
	 *
	 * @name aperture.Class.extend
	 * @function
	 */
	namespace.Class.extend = function(className, properties) {

		// className is an optional arg,  but first.
		if (!properties) {
			properties = className;
			className = null;
		}

		var _super = this.prototype;

		// Instantiate a base class (but only create the instance,
		// don't run the init constructor)
		initializing = true;
		var prototype = new this();
		initializing = false;

		// Copy the properties over onto the new prototype
		aperture.util.forEach( getOwnPropertyNames(properties), function ( name ) {
			prototype[name] = properties[name];
		});

		// The dummy class constructor
		function ApertureClass() {
			// All construction is actually done in the init method
			if (!initializing && this.init) {
				this.init.apply(this, arguments);
			}
		}

		// add the type of now that we have the constructor.
		prototype.typeOf = createTypeOfMethod( className, ApertureClass, _super.typeOf );

		// Populate our constructed prototype object
		ApertureClass.prototype = prototype;

		// Enforce the constructor to be what we expect
		ApertureClass.constructor = ApertureClass;

		// And make this class extendable
		ApertureClass.extend = namespace.Class.extend;

		// And make this class able to create views of itself
		ApertureClass.addView = addView;

		return ApertureClass;
	};

	return namespace;

}(aperture || {}));
/**
 * Source: config.js
 * Copyright (c) 2014 Oculus Info Inc.
 * @fileOverview APIs for interacting with Configurations
 */

/**
 * @namespace APIs for interacting with configurations
 */
aperture.config = (function() {

	var registered = {};
	var currentConfig = {};

	return {

		/**
		 * Register a callback function to be notified with configuration details
		 * when a particular named configuration section is part of the object.
		 * This allows features to be given environment-specific configuration values
		 * by a server, container, or client.
		 */
		register : function( configName, callback ) {
			var existing = registered[configName];
			if (!existing) {
				existing = [];
				registered[configName] = existing;
			}

			existing.push({'callback':callback});

			// If we already have a configuration...
			if( currentConfig && currentConfig[configName] ) {
				// Immediately call the callback
				callback(currentConfig);
			}
		},

		/**
		 * Provides a given configuration object and notifies all registered listeners.
		 */
		provide : function( provided ) {
			currentConfig = provided;

			var key, i;
			for( key in currentConfig ) {
				if (currentConfig.hasOwnProperty(key)) {
					var existing = registered[key];
					if( existing ) {
						for( i=0; i<existing.length; i++ ) {
							existing[i].callback(currentConfig);
						}
					}
				}
			}
		},

		/**
		 * Returns the current configuration object provided via "provide"
		 */
		get : function() {
			return currentConfig;
		}
	};
}());
/**
 * Source: log.js
 * Copyright (c) 2014 Oculus Info Inc.
 * @fileOverview Aperture Logging API implementation
 */

/*
 * TODO Allow default appenders to be constructed based on config from server
 */

/**
 * @namespace Aperture logging API
 */
aperture.log = (function() {

		/**
		 * @class Logging level definitions
		 * @name aperture.log.LEVEL
		 */
	var LEVEL =
		/** @lends aperture.log.LEVEL */
		{
			/** @constant
			 *  @description Error logging level */
			ERROR: 'error',
			/** @constant
			 *  @description Warn logging level */
			WARN: 'warn',
			/** @constant
			 *  @description Info logging level */
			INFO: 'info',
			/** @constant
			 *  @description Debug logging level */
			DEBUG: 'debug',
			/** @constant
			 *  @description 'Log' logging level */
			LOG: 'log',
			/** @constant
			 *  @description Turn off logging */
			NONE: 'none'
		},

		levelOrder = {
			'error': 5,
			'warn': 4,
			'info': 3,
			'debug': 2,
			'log': 1,
			'none': 0
		},


		// The list of active appenders
		appenders = [],

		// The global logging level
		globalLevel = LEVEL.INFO,

		// The current indentation level.
		prefix = '',
		eightSpaces = '        ',
		
		
		/**
		 * @private
		 * Internal function that takes a format string and additional arguments
		 * and returns a single formatted string.  Essentially a cheap version of
		 * sprintf.  Parameters are referenced within the format string using {#}
		 * where # is the parameter index number, starting with 0.  Parameter references
		 * may be repeated and may be in any order within the format string.
		 *
		 * Example:
		 * <pre>
		 * formatString('{0} is fun - use {0} more. {1} is boring', 'JavaScript', 'C');
		 * </pre>
		 *
		 */
		formatString = function(message /*, params */) {
			// Extract all but first arg (message)
			var args = Array.prototype.slice.call(arguments, 1);
			// Return string with all {digit} replaced with value from argument
			return prefix + message.replace(/\{(\d+)\}/g, function(match, number) {
				return typeof args[number] != 'undefined' ?
					args[number] :
					'{' + number + '}';
			});
		},



		Appender = aperture.Class.extend( 'aperture.log.Appender',
		/** @lends aperture.log.Appender# */
		{
			/**
			 * @class Logging Appender base class.<br><br>
			 *
			 * @constructs
			 * @param {aperture.log.LEVEL} level The logging level threshold for this appender
			 */
			init : function(level) {
				this.level = level || LEVEL.WARN;
			},

			/**
			 * Sets or gets the current global logging level
			 * @param {aperture.log.LEVEL} [l] the new appender level threshold.  If value given, the
			 * threshold level is not changed and the current value is returned
			 * @param the current appender level threshold
			 */
			level : function(l) {
				if( l !== undefined ) {
					// Set new level if given one
					this.level = l;
				}
				// Return current level
				return this.level;
			},

			/**
			 * Log function called by the logging framework requesting that the
			 * appender handle the given data.  In general this function should
			 * not be overridden by sub-classed Appenders, instead they should
			 * implement logString and logObjects
			 * @param {aperture.log.LEVEL} level level at which to log the message
			 * @param {Object|String} toLog data to log, see api.log for details.
			 */
			log : function( level, toLog ) {
				// Check logging level
				if( levelOrder[level] >= levelOrder[this.level] ) {
					// Extract all arguments that are meant to be logged
					var toLogArgs = Array.prototype.slice.call(arguments, 1);
					// Is the first thing to log a string?
					if( aperture.util.isString(toLog) ) {
						// Log a string, assume is a format string if more args follow
						// Create log message and call appender's log string function
						this.logString( level, formatString.apply(null, toLogArgs) );

					} else {
						// Not a string, assume one or more objects, log as such
						if( this.logObjects ) {
							// Appender supports object logging
							// Call logObjects with the level and an array of objects to log
							this.logObjects( level, toLogArgs );
						} else {
							// Appender doesn't support object logging
							// Convert objects to a JSON string and log as such
							var message = window.JSON? JSON.stringify( toLogArgs ) :
								'No window.JSON interface exists to stringify logged object. A polyfill like json2.js is required.';

							this.logString( level, message );
						}
					}
				}
			},

			/**
			 * 'Abstract' function to log a given string message at the given level
			 * Appender sub-classes should implement this method to do something useful
			 * with the message
			 * @param {aperture.log.LEVEL} level the level of the message to log
			 * @param {String} message the message that should be logged as a string
			 */
			logString : function(level, message) {}

			/**
			 * 'Abstract' function to log javascript objects.  Appender sub-classes
			 * may elect to implement this method if they can log objects in a useful
			 * way.  If this method is not implemented, logString will be called
			 * with a string representation of the objects.
			 * <pre>
			 * logObjects : function(level, objects) {}
			 * </pre>
			 * @param {aperture.log.LEVEL} level the level of the entry to log
			 * @param {Object} ... the objects to log
			 */
		} ),

		/**
		 * Define the externally visible logging API
		 * @exports api as aperture.log
		 * @lends api
		 */
		api = {
			/**
			 * Returns a list of the current logging appenders
			 * @returns an array of active appenders
			 */
			appenders : function() {
				return appenders;
			},

			/**
			 * Adds an Appender instance to the set of active logging appenders
			 * @param {Appender} toAdd an appender instance to add
			 * @returns the added appender
			 */
			addAppender : function( toAdd ) {
				appenders.push( toAdd );
				return toAdd;
			},

			/**
			 * Removes an Appender instance from the set of active logging appenders
			 * @param {Appender} toRemove an appender instance currently in the list
			 * of active appenders that should be removed
			 * @returns the removed appender
			 */
			removeAppender : function( toRemove ) {
				appenders = aperture.util.without( appenders, toRemove );
				return toRemove;
			},

			/**
			 * Logs a message at the given level
			 * @param {aperture.log.LEVEL} level the level at which to log the given message
			 * @param {String|Object} message a message or object to log.
			 * The message may be a plain string, may be a format string followed by
			 * values to inject into the string, or may be one or more objects that should
			 * be logged as is.
			 * @param {String|Object} [...] additional objects to log or parameters
			 * for the format string contained in the message parameter.
			 */
			logAtLevel : function(level, message) {
				// Only log if message level is equal to or higher than the global level
				if( levelOrder[level] >= levelOrder[globalLevel] ) {
					var args = arguments;
					aperture.util.forEach(appenders, function(appender) {
						// Call the appender's log function with the arguments as given
						appender.log.apply(appender, args);
					});
				}
			},

			/**
			 * Sets or gets the current global logging level
			 * @param {LEVEL} [l] if provided, sets the global logging level
			 * @returns {LEVEL} the global logging level, if a get, the old logging level if a set
			 */
			level : function(l) {
				var oldLevel = globalLevel;
				if( l !== undefined ) {
					// Set new global level if given one
					globalLevel = l;
				}
				// Return original global level
				return oldLevel;
			},
			
			/**
			 * Returns true if configured to include the specified log level.
			 * @param {LEVEL} level
			 * @returns {Boolean} true if logging the specified level.
			 */
			isLogging : function(level) {
				return levelOrder[level] >= levelOrder[globalLevel];
			},
			
			/**
			 * If setting increments or decrements the indent by the specified number of spaces,
			 * otherwise returning the current indentation as a string of spaces. Zero may 
			 * be supplied as an argument to reset the indentation to zero.
			 *  
			 * @param {Number} [spaces] the number of spaces to increment or decrement, or zero to reset.
			 * @returns {String} the current indentation as a string.
			 */
			indent : function(spaces) {
				if (arguments.length !== 0) {
					if (spaces) {
						if (spaces < 0) {
							prefix = spaces < prefix.length? prefix.substring(0, prefix.length-spaces): '';
						} else {
							while (spaces > 0) {
								prefix += eightSpaces.substr(0, Math.min(spaces, 8));
								spaces-= 8;
							}
						}
					} else {
						prefix = '';
					}
				}
				
				return prefix;
			}
		};

	/**
	 * Logs a message at the "LEVEL.ERROR" level
	 * @name error
	 * @methodof aperture.log
	 * @param {String|Object} message a message or object to log.
	 * The message may be a plain string, may be a format string followed by
	 * values to inject into the string, or may be one or more objects that should
	 * be logged as is.
	 * @param {String|Object} [...] additional objects to log or parameters
	 * for the format string contained in the message parameter.
	 */

	/**
	 * Logs a message at the "LEVEL.WARN" level
	 * @name warn
	 * @methodof aperture.log
	 * @param {String|Object} message a message or object to log.
	 * The message may be a plain string, may be a format string followed by
	 * values to inject into the string, or may be one or more objects that should
	 * be logged as is.
	 * @param {String|Object} [...] additional objects to log or parameters
	 * for the format string contained in the message parameter.
	 */

	/**
	 * Logs a message at the "LEVEL.INFO" level
	 * @name info
	 * @methodof aperture.log
	 * @param {String|Object} message a message or object to log.
	 * The message may be a plain string, may be a format string followed by
	 * values to inject into the string, or may be one or more objects that should
	 * be logged as is.
	 * @param {String|Object} [...] additional objects to log or parameters
	 * for the format string contained in the message parameter.
	 */

	/**
	 * Logs a message at the "LEVEL.LOG" level
	 * @name log
	 * @methodof aperture.log
	 * @param {String|Object} message a message or object to log.
	 * The message may be a plain string, may be a format string followed by
	 * values to inject into the string, or may be one or more objects that should
	 * be logged as is.
	 * @param {String|Object} [...] additional objects to log or parameters
	 * for the format string contained in the message parameter.
	 */

	// Add a log method for each level to the api
	aperture.util.forEach(LEVEL, function(value, key) {
		// Create a method such as the following:
		// api.info = log(level.INFO, args...)
		api[value] = aperture.util.bind(api.logAtLevel, api, value);
	});

	// Expose 'abstract' base class
	api.Appender = Appender;

	// Expose the log level definition
	api.LEVEL = LEVEL;

	// Register for configuration events
	// Configuration options allow
	aperture.config.register('aperture.log', function(config) {
		var logConfig = config['aperture.log'];

		// Set the global level
		if( logConfig.level ) {
			api.level( logConfig.level );
		}

		// For all defined appenders...
		aperture.util.forEach( logConfig.appenders, function(value, key) {
			if (!key) {
				return;
			}
			// the function will be an add fn that follows a particular format
			key = 'add' + key.charAt(0).toUpperCase() + key.substr(1);

			// If an appender exists with the given key...
			if( aperture.util.isFunction(aperture.log[key]) ) {
				// Add it with the associated specification and start using it
				aperture.log[key]( value );
			}
		});
	});

	return api;
}());
/**
 * Source: Canvas.js
 * Copyright (c) 2014 Oculus Info Inc.
 * @fileOverview The base canvas classes.
 */

/**
 * @namespace The canvas package, where rendering is abstracted and implemented for
 * various platforms.
 * 
 * ${protected}
 */
aperture.canvas = (
/** @private */
function(namespace) {

	var plugins = {};

	// leave private for now
	namespace.handle = function( typeName, ctor ) {
		plugins[typeName] = ctor;
	};
	namespace.type = function( typeName ) {
		return plugins[typeName];
	};

	/**
	 * A simple div canvas type.
	 * 
	 * ${protected}
	 *
	 * @name aperture.canvas.DIV_CANVAS
	 * @constant
	 */
	namespace.DIV_CANVAS = 'DIV_CANVAS';

	/**
	 * A vector canvas type.
	 * 
	 * ${protected}
	 *
	 * @name aperture.canvas.VECTOR_CANVAS
	 * @constant
	 */
	namespace.VECTOR_CANVAS = 'VECTOR_CANVAS';


	/**
	 * @class
	 * The abstraction of an Aperture canvas. An
	 * Aperture canvas abstracts the surface being rendered to, for platform extensibility.
	 * This class is not used directly by end clients. It is used by
	 * layer implementations and is provided here for reference only.
	 * 
	 * ${protected}
	 *
	 * @extends aperture.Class
	 *
	 * @description
	 * This class is abstract and is not to be instantiated directly.
	 *
	 * @name aperture.canvas.Canvas
	 */
	namespace.Canvas = aperture.Class.extend( 'aperture.canvas.Canvas',
		{
			init : function ( root ) {
				this.root_ = root;
				this.canvases_ = [];
//				this.clients = 0;
			},

			/**
			 * Removes the canvas from its parent.
			 * 
			 * ${protected}
			 *
			 * @returns {aperture.canvas.Canvas}
			 *  This canvas
			 *
			 * @name aperture.canvas.Canvas.prototype.remove
			 * @function
			 */
			remove : function() {
//				this.clients--;

				// TODO: need to resolve shared layer destruction.
				// Reference counting in this manner is too fragile, since
				// clients could accidentally call remove more than once.
				return this;
			},

			/**
			 * Returns a member canvas of the requested type, constructing one if it does
			 * not already exist.
			 * 
			 * ${protected}
			 *
			 * @param type
			 *  An Aperture type constructor, such as aperture.canvas.VECTOR_CANVAS.
			 * @returns {aperture.canvas.Canvas}
			 *  A canvas
			 *
			 * @name aperture.canvas.Canvas.prototype.canvas
			 * @function
			 */
			canvas : function( type ) {
				if (!type || !(type = plugins[type]) || this.typeOf(type)) {
					return this;
				}

				// find any existing canvas of the right type
				var canvas = aperture.util.find( this.canvases_, function (canvas) {
					return canvas.typeOf(type);
				});

				// if not found, create a new one.
				if (!canvas) {
					canvas = new type( this.root() );
					this.canvases_.push(canvas);
				}

//				canvas.clients++;

				return canvas;
			},

			/**
			 * Returns the canvas root DOM element.
			 * 
			 * ${protected}
			 *
			 * @returns {DOMElement}
			 *  The root DOM element of this canvas
			 *
			 * @name aperture.canvas.Canvas.prototype.root
			 * @function
			 */
			root : function() {
				return this.root_;
			},

			/**
			 * Returns a new graphics interface implementation for this canvas.
			 * 
			 * ${protected}
			 *
			 * @param {aperture.canvas.Graphics} parentGraphics
			 *  The parent graphics context.
			 *
			 * @returns {aperture.canvas.Graphics}
			 *  A new graphics canvas
			 *
			 * @name aperture.canvas.Canvas.prototype.graphics
			 * @function
			 */
			graphics : function ( parentGraphics ) {
				return namespace.NO_GRAPHICS;
			},

			/**
			 * Called at the end of a canvas update, flushing any
			 * drawing operations, as necessary.
			 * 
			 * ${protected}
			 *
			 * @name aperture.canvas.Canvas.prototype.flush
			 * @function
			 */
			flush : function () {
				var i = 0, n;
				for (n = this.canvases_.length; i< n; i++) {
					this.canvases_[i].flush();
				}
			}
		}
	);

	namespace.handle( namespace.DIV_CANVAS, namespace.Canvas.extend( 'aperture.canvas.DivCanvas', {} ));


	// NOTE: We don't implement standard html graphics but we could

	/**
	 * @class
	 * The abstraction of an Aperture graphics implementation. An
	 * Aperture graphics interface abstracts basic rendering for platform extensibility.
	 * This class is not used directly by end clients. It is used by
	 * layer implementations and is provided here for reference only.
	 *
	 * ${protected}
	 * 
	 * @extends aperture.Class
	 *
	 * @description
	 * This class is abstract and is not to be instantiated directly.
	 *
	 * @name aperture.canvas.Graphics
	 */
	namespace.Graphics = aperture.Class.extend( 'aperture.canvas.Graphics',
		{
			init : function ( canvas ) {
				this.canvas = canvas;
			},

			/**
			 * Shows or hides this graphics instance.
			 * 
			 * ${protected}
			 *
			 * @param {Boolean} show
			 *  Whether or not to show this context.
			 *      
			 * @returns {Boolean} 
			 *  true if value was changed
			 * 
			 * @name aperture.canvas.Graphics.prototype.display
			 * @function
			 */
			display : function( show ) {
				return true;
			},

			/**
			 * Moves this graphics to the front of its container graphics,
			 * or a child element to the top.
			 * 
			 * ${protected}
			 * 
			 * @param element
			 *  An optional element to move, otherwise the entire graphics will be moved.
			 * 
			 * @returns {aperture.canvas.Graphics}
			 *  This graphics object
			 *
			 * @name aperture.canvas.Graphics.prototype.toFront
			 * @function
			 */
			toFront : function() {
				return this;
			},

			/**
			 * Moves this graphics to the back of its container graphics,
			 * or a child element to the bottom.
			 * 
			 * ${protected}
			 *
			 * @param element
			 *  An optional element to move, otherwise the entire graphics will be moved.
			 * 
			 * @returns {aperture.canvas.Graphics}
			 *  This graphics object
			 *
			 * @name aperture.canvas.Graphics.prototype.toBack
			 * @function
			 */
			toBack : function() {
				return this;
			},

			/**
			 * Removes the graphics instance from its parent.
			 * 
			 * ${protected}
			 *
			 * @returns {aperture.canvas.Graphics}
			 *  This graphics object
			 *
			 * @name aperture.canvas.Graphics.prototype.remove
			 * @function
			 */
			remove : function () {
				return this;
			},
			
			/**
			 * Adds a callback for a specific type of event.
			 * 
			 * ${protected}
			 * 
			 * @param eventType
			 *  The type of event to add a callback for
			 * @param layer
			 *  The client layer for events.
			 * 
			 * @returns {aperture.canvas.Graphics}
			 *  This graphics object
			 *
			 * @name aperture.canvas.Graphics.prototype.on
			 * @function
			 */
			on : function(eventType, layer) {
				return this;
			},
			
			/**
			 * Removes a callback for a specific type of event.
			 * 
			 * ${protected}
			 * 
			 * @param eventType
			 *  The type of event to remove a callback for
			 * 
			 * @returns {aperture.canvas.Graphics}
			 *  This graphics object
			 *
			 * @name aperture.canvas.Graphics.prototype.off
			 * @function
			 */
			off : function(eventType) {
				return this;
			},

			/**
			 * Sets or gets a data object [and index] associated with the specified element,
			 * or universally with this canvas.
			 * 
			 * ${protected}
			 * 
			 * @param {Object} [element]
			 *  The element to get/set data for, or the universal data object for the canvas.
			 *  If omitted the universal data object is returned.
			 * @param {Object} [data]
			 *  The data to associate.
			 * @param {Array} [index]
			 *  The index array to associate.
			 * 
			 * @returns {Object|aperture.canvas.Graphics}
			 *  The data object requested (if a get), otherwise this graphics object
			 *
			 * @name aperture.canvas.Graphics.prototype.data
			 * @function
			 */
			data : function(element, data, index) {
				return this;
			}
		}
	);

	// use this singleton as a noop.
	namespace.NO_GRAPHICS = new namespace.Graphics();
	
	// an abstraction which we don't both to document.
	namespace.VectorCanvas = namespace.Canvas.extend( 'aperture.canvas.VectorCanvas',
		{
			graphics : function ( parentGraphics ) {}
		}
	);

	/**
	 * @class
	 * The abstraction of an Aperture vector graphics implementation. An
	 * Aperture graphics interface abstracts basic rendering for platform extensibility.
	 * This class is not used directly by end clients. It is used by
	 * layer implementations and is provided here for reference only.
	 *
	 * ${protected}
	 * 
	 * @extends aperture.canvas.Graphics
	 *
	 * @description
	 * This class is abstract and is not to be instantiated directly.
	 *
	 * @name aperture.canvas.VectorGraphics
	 */
	namespace.VectorGraphics = namespace.Graphics.extend( 'aperture.canvas.VectorGraphics', {} );

	/**
	 * Sets the clipping region of this graphics canvas.
	 *
	 * ${protected}
	 * 
	 * @param {Array} rect
	 *  An array of [x, y, width, height], or
	 *  an empty or null clip if clearing.
	 *
	 * @returns {aperture.canvas.Graphics}
	 *  This graphics object
	 *
	 * @name aperture.canvas.VectorGraphics.prototype.clip
	 * @function
	 */

	/**
	 * Sets the position of this graphics canvas.
	 *
	 * ${protected}
	 * 
	 * @param {Number} x
	 *  The x position.
	 *
	 * @param {Number} y
	 *  The y position.
	 *
	 * @returns {aperture.canvas.Graphics}
	 *  This graphics object
	 *
	 * @name aperture.canvas.VectorGraphics.prototype.origin
	 * @function
	 */

	/**
	 * Adds a path given an svg path string.
	 *
	 * ${protected}
	 * 
	 * @param {String} [svg]
	 *  An optional path string in svg path format. If not specified here
	 *  the path is expected to be set later.
	 *
	 * @returns {Object}
	 *  A new path element
	 *
	 * @name aperture.canvas.VectorGraphics.prototype.path
	 * @function
	 */

	/**
	 * Adds a circle element.
	 *
	 * ${protected}
	 * 
	 * @param {Number} x
	 *  The x coordinate of the circle.
	 *
	 * @param {Number} y
	 *  The y coordinate of the circle.
	 *
	 * @param {Number} radius
	 *  The radius of the circle.
	 *
	 * @returns {Object}
	 *  A new circle element
	 *
	 * @name aperture.canvas.VectorGraphics.prototype.circle
	 * @function
	 */

	/**
	 * Adds a rectangle element.
	 *
	 * ${protected}
	 * 
	 * @param {Number} x
	 *  The x coordinate of the rectangle.
	 *
	 * @param {Number} y
	 *  The y coordinate of the rectangle.
	 *
	 * @param {Number} width
	 *  The width of the rectangle.
	 *
	 * @param {Number} height
	 *  The height of the rectangle.
	 *
	 * @returns {Object}
	 *  A new rectangle element
	 *
	 * @name aperture.canvas.VectorGraphics.prototype.rect
	 * @function
	 */

	/**
	 * Adds a text element.
	 *
	 * ${protected}
	 * 
	 * @param {Number} x
	 *  The x coordinate of the text.
	 *
	 * @param {Number} y
	 *  The y coordinate of the text.
	 *
	 * @param {String} text
	 *  The text of the element.
	 *
	 * @returns {Object}
	 *  A new text element
	 *
	 * @name aperture.canvas.VectorGraphics.prototype.text
	 * @function
	 */

	/**
	 * Adds an image element.
	 *
	 * ${protected}
	 * 
	 * @param {String} src
	 *  The source uri of the image.
	 *
	 * @param {Number} x
	 *  The x coordinate of the circle.
	 *
	 * @param {Number} y
	 *  The y coordinate of the circle.
	 *
	 * @param {Number} width
	 *  The width of the image in pixels.
	 *
	 * @param {Number} height
	 *  The height of the image in pixels.
	 *
	 * @returns {Object}
	 *  A new image element
	 *
	 * @name aperture.canvas.VectorGraphics.prototype.image
	 * @function
	 */

	/**
	 * Updates an element previously returned by one of the element
	 * constructors, optionally animating in the changes.
	 *
	 * ${protected}
	 * 
	 * @param {Object} element
	 *  The element to update, previously returned by an element
	 *  constructor.
	 *
	 * @param {Object} attributes
	 *  Property values to update in the element.
	 *
	 * @param {aperture.animate.Transition} [transition]
	 *  The optional animated transition to use.
	 *
	 * @name aperture.canvas.VectorGraphics.prototype.update
	 * @function
	 */

	/**
	 * If an element argument is supplied, removes the element 
	 * and destroys it, otherwise the graphics context itself is
	 * removed and destroyed.
	 *
	 * ${protected}
	 * 
	 * @param {Object} [element]
	 *  The element to remove, previously returned by an element
	 *  constructor.
	 *
	 * @returns {Object|aperture.canvas.Graphics}
	 *  The element removed.
	 *
	 * @name aperture.canvas.VectorGraphics.prototype.remove
	 * @function
	 */
	
	/**
	 * If an elements argument is supplied, removes the elements
	 * specified and destroys them, otherwise all elements are removed.
	 *
	 * ${protected}
	 * 
	 * @param {Array} [elements]
	 *  Optionally, the elements to remove, previously returned by element
	 *  constructors.
	 *      
	 * @name aperture.canvas.VectorGraphics.prototype.removeAll
	 * @function
	 */
	
	/**
	 * Applies an appearance transition to a new element, if
	 * supplied. If not supplied this method has no effect.
	 *
	 * ${protected}
	 * 
	 * @param {Object} element
	 *  The element to apparate, previously returned by an element
	 *  constructor.
	 *
	 * @param {aperture.animate.Transition} [transition]
	 *  The optional animated transition to use.
	 *
	 * @name aperture.canvas.VectorGraphics.prototype.apparate
	 * @function
	 */

	return namespace;

}(aperture.canvas || {}));

/**
 * Source: Animation.js
 * Copyright (c) 2014 Oculus Info Inc.
 * @fileOverview Aperture Animation APIs
 */

/**
 * @ignore
 * Ensure namespace exists
 */
aperture = (
/** @private */
function(namespace) {

	namespace.Transition = aperture.Class.extend( 'aperture.Transition',
	/** @lends aperture.Transition.prototype */
	{
		/**
		 * @class Represents an animated transition, consisting of
		 * an interpolation / easing / tween function, and a length
		 * of time over which the transition will occur. Transitions may
		 * be optionally passed into the Layer update function to animate
		 * any updates which will occur.
		 *
		 * @constructs
		 * @extends aperture.Class
		 *
		 * @param {Number} [milliseconds=300]
		 *      the length of time that the transition will take to complete.
		 *
		 * @param {String} [easing='ease']
		 *      the function that will be used to transition from one state to another.
		 *      The standard CSS options are supported:<br><br>
		 *      'linear' (constant speed)<br>
		 *      'ease' (default, with a slow start and end)<br>
		 *      'ease-in' (slow start)<br>
		 *      'ease-out' (slow end)<br>
		 *      'ease-in-out' (similar to ease)<br>
		 *      'cubic-bezier(n,n,n,n)' (a custom function, defined as a bezier curve)
		 *
		 * @param {Function} [callback]
		 *      a function to invoke when the transition is complete.
		 *
		 * @returns {this}
		 *      a new Transition
		 */
		init : function( ms, easing, callback ) {
			this.time = ms || 300;
			this.fn = easing || 'ease';
			this.end = callback;
		},

		/**
		 * Returns the timing property.
		 *
		 * @returns {Number}
		 *      the number of milliseconds over which to complete the transition
		 */
		milliseconds : function ( ) {
			return this.time;
		},

		/**
		 * Returns the easing property value.
		 *
		 * @returns {String}
		 *      the function to use to transition from one state to another.
		 */
		easing : function ( ) {
			return this.fn;
		},

		/**
		 * Returns a reference to the callback function, if present.
		 *
		 * @returns {Function}
		 *      the function invoked at transition completion.
		 */
		callback : function ( ) {
			return this.end;
		}
	});

	return namespace;

}(aperture || {}));
/**
 * Source: Layer.js
 * Copyright (c) 2014 Oculus Info Inc.
 * @fileOverview Aperture Abstract Layer Class Implementation
 */

/**
 * @namespace
 * @ignore
 * Ensure namespace exists
 */
aperture = (
/** @private */
function(namespace) {

	
	// PRIVATE REFERENCES AND FUNCTIONS
	/**
	 * Keep a running globally unique id set such that each render node can have a
	 * unique id.  This allows nodes to be easily hashed.
	 */
	var log = aperture.log,
		nextUid = 1,
		NO_GRAPHICS = aperture.canvas.NO_GRAPHICS,
		
		// util is always defined by this point
		util = aperture.util,
		forEach = util.forEach,
		indexOf = util.indexOf;

	/**
	 * Given a Node object create a derived node that shares the source's
	 * core features (such as position, anchor, etc) but has a fresh (empty) userData
	 * object
	 */
	function addNode( layer, parent, prev, data ) {
		// Derive an object
		var node = util.viewOf( parent ),
			sibs = parent.kids,
			luid = layer.uid;
		
		// append ourselves to existing siblings.
		(sibs[luid] || (sibs[luid] = [])).push(node);
		
		// Initialize unique properties.
		node.uid      = (nextUid++).toString();
		node.parent   = parent;
		node.layer    = layer;
		node.kids     = {};
		node.userData = {};
		node.graphics = NO_GRAPHICS;

		// Set data if given, otherwise it will inherit it.
		if( data ) {
			node.data = data;
			node.idFn = layer.idFunction;
		}
		
		// do graphics construction.
		updateVisibility(node);
		
		linkNode(node, prev);
		
		return node;
	}

	
	/**
	 * Default data item function
	 */
	var inherited = (function() {
		var copydata = [null], nodata = []; // use these once
		
		return function(data) {
			return data? copydata : nodata;
		};
	}());
	
	
	/**
	 * Updates the visibility, creating the graphics for the first time if necessary,
	 * and returns true if showing. 
	 */
	function updateVisibility( node ) {
		var layer = node.layer;
		
		if (layer) {
		
			var show = layer.valueFor('visible', node.data, true );

			// reset.
			node.appearing = false;
		
			if (show) {
				if (node.graphics === NO_GRAPHICS && node.parent.graphics) {
					var g = node.graphics = layer.canvas_.graphics( node.parent.graphics ).data( node );
				
					// event hooks for any current events
					forEach(layer.handlers_, function(h, key) {
						g.on(key, this);
					}, layer);
				
					node.appearing = true;
				
				} else {
					node.appearing = node.graphics.display(true);
				}
			
			} else if (node.graphics) {
				node.graphics.display(false);
			}
		
			return show;
		}
	}

	/**
	 * Link a new node into the list. For optimal traversal and removal nodes
	 * store references their adjacent nodes rather than externalizing that in a list structure.
	 */
	function linkNode( node, prev ) {
		
		// assume next is handled. this is a private and optimized process.
		if (prev == null) {
			node.layer.nodes_ = node;
			node.prev = null;
		} else {
			prev.next = node;
			node.prev = prev;
		}
	}

	/**
	 * Remove a node from the linked list.
	 */
	function unlinkNode( c ) {
		
		// stitch
		if (c.prev) {
			c.prev.next = c.next;
		} else {
			c.layer.nodes_ = c.next;
		}
		
		if (c.next) {
			c.next.prev = c.prev;
		} 
	}
	
	/**
	 * node removal function.
	 */
	function removeNode(c) {
		var sibs = c.parent.kids[c.layer.uid],
			ixMe = indexOf(sibs, c);
		
		if (ixMe !== -1) {
			sibs.splice(ixMe, 1);
		}
		
		// cleanup graphics.
		c.graphics.remove();
		c.graphics = null;
		c.layer = null;
	}
	
	/**
	 * Private function called by processChangeSet invoked if the layer has local data and
	 * is not being built up from scratch.
	 */
	function processDataChanges( myChangeSet, parentChangeSet ) {
		var chgs = myChangeSet.changed, c;

		
		// DATA CHANGED? SORT *ALL* CHANGES OUT AND RETURN
		// if our data changes, we have to execute a full pass through everything
		// to sort into add/changed/removed. EVERYTHING is touched. this could be made more
		// efficient if data models exposed chg fns to the user - i.e. w/ adds, joins etc.
		if (myChangeSet.dataChanged) {
			var allParents  = (this.parentLayer_ && this.parentLayer_.nodes_) || this.rootNode_,
				adds = myChangeSet.added,
				rmvs = myChangeSet.removed,
				myUid = this.uid,
				idFunction = this.idFunction,
				prev, dad, i;
			
			// form new ordered list of nodes.
			this.nodes_ = null;
			
			// re-compare EVERYTHING and sort into add/changed/removed
			for (dad = allParents; dad != null; dad = dad.next) {
				var existing = indexNodes(dad.kids[myUid]),
					newkids = dad.kids[myUid] = [];
				
				// for all my new items, look for matches in existing.
				forEach( this.dataItems( dad.data ), function( dataItem ) {
					c = null;
					
					var dataId = idFunction? idFunction.call(dataItem) : dataItem;
						
					for (i = existing.next; i != null; i = i.next) {
						c = i.node;

						// match?
						if ((c.idFn? c.idFn.call(c.data) : c.data) === dataId) {
							// remove it by stitching adjacent nodes together.
							// Makes subsequent searches faster and will leave
							// existing with only the things that have been removed.
							i.prev.next = i.next;
							if (i.next) {
								i.next.prev = i.prev;
							}
							
							break;
							
						} else {
							c = null;
						}
					}
					
					// found? process change
					if (c) {

						// readd to fresh kid list
						newkids.push(c);
						
						// link it back in.
						linkNode( c, prev );
					
						// update data reference.
						c.data = dataItem;
						c.idFn = idFunction;

						// only process further if showing.
						if (updateVisibility(c)) {
							chgs.push(c);
						}
						
					// else make new
					} else {
						adds.push( c = addNode( this, dad, prev, dataItem ) );
						
					}
					
					prev = c;
					
				}, this);

				
				// whatever is left is trash. these are already removed from our locally linked list.
				for (i = existing.next; i != null; i = i.next) {
					rmvs.push(i.node);
				}
			}
			
		// SHORTCUT: EVERYTHING IS A SIMPLE UPDATE AND I AM THE ROOT
		// last of the special cases. If we receive this we are the first in traversal
		// that contains data and our job is to simply to add everything visible as changed.
		// thereafter children will pick it up properly and interpret in the node of any data changes.
		// we know there are no removes or adds, since we are top in the chain and we already looked locally
		} else if (myChangeSet.updateAll) {
			for (c = this.nodes_; c != null; c = c.next) {
				if (updateVisibility(c)) {
					chgs.push(c);
				}
			}
			
		// else process parent changes as usual.
		} else {					
			return false;
		}
		
		// clear
		myChangeSet.updateAll = false;
		
		return true;
	}

	/**
	 * Creates and returns an iterable, modifiable snapshot of the specified node list,
	 * where the first element is a non-node reference to the first node.
	 */
	function indexNodes(nodes) {
		var h = {}; // head element is not a node.

		if (nodes) {
			var n = nodes.length, 
				i, it = h;
			
			for (i=0; i< n; ++i) {
				it = it.next = {
					prev : it,
					node : nodes[i]
				};
			}
		}
		
		return h;
	}
	
	/**
	 * Default idFunctions, if id's exist or not.
	 */
	function getId() {
		return this.id;
	}

	
	
	// LAYER CLASS
	var Layer = aperture.Class.extend( 'aperture.Layer',

		/** @lends aperture.Layer# */
		{
			/**
			 * @class A layer represents a set of like graphical elements which are mapped
			 * in a spatial node. Layer is the abstract base class of all graphical layers.
			 *
			 * Layer is abstract and not to be constructed directly.
			 * See {@link aperture.PlotLayer#addLayer addLayer} for an example of how to add layers
			 * to a vizlet.
			 *
			 * All layers observe the following mapping:

			 * @mapping {Boolean=true} visible
			 *   Whether or not a layer item should be displayed.
			 *
			 * @constructs
			 * @factoryMade
			 * @extends aperture.Class
			 *
			 * @param {Object} spec
			 *   A specification object that contains initial values for the layer.
			 *   
			 * @param {aperture.PlotLayer} spec.parent
			 *   The parent layer for this layer. May be null.
			 *   
			 * @param {aperture.canvas.Canvas} spec.parentCanvas
			 *   The parent's canvas, never null.
			 *   
			 * @param {Object} [spec.mappings]
			 *   Optional initial simple property : value mappings. More advanced
			 *   mappings can be defined post-construction using the {@link #map}
			 *   function.
			 */
			init : function( spec, mappings ) {

				spec = spec || {};

				/**
				 * ${protected}
				 * 
				 * A Unique layer id string.
				 */
				this.uid = (nextUid++).toString();

				/**
				 * @private
				 * This layer's parent layer
				 */
				this.parentLayer_ = spec.parent;

				/**
				 * @private
				 * This layer's root vizlet node.
				 */
				this.rootNode_ = spec.rootNode;
				
				/**
				 * @private
				 * This layer's root vizlet.
				 */
				this.vizlet_ = spec.vizlet || this;
				
				/**
				 * @private
				 * This layer's canvas
				 */
				this.canvas_ = spec.parentCanvas && spec.parentCanvas.canvas(this.canvasType);

				/**
				 * @private
				 * An object containing the currently mapped event handlers registered by
				 * a call to on().  This object is structured as a map of event types to
				 * an array of callback functions.
				 */
				this.handlers_ = {};

				/**
				 * @private
				 * Tracks switches between local and inherited only data.
				 */
				this.renderedLocalData_ = false;
				
				
				/**
				 * ${protected}
				 * An array of nodes rendered by this layer.  Generally this
				 * list should only be used by the internal logic responsible for the layer
				 * rendering management.
				 */
				this.nodes_ = undefined;

				/**
				 * @private
				 * A data accessor function which returns an array of data items. 
				 * The function will take a parent data item as an argument, which will be
				 * ignored if data values were set explicitly. Unless data is explicitly set
				 * for this layer this function will return an array containing a single local data
				 * element of undefined, reflecting an inheritance of the parent data item.
				 */
				this.dataItems = inherited;

				/**
				 * ${protected}
				 * True if the layer has locally defined data, false if inherited.
				 */
				this.hasLocalData = false;
				
				/**
				 * @private
				 * A hash of visualProperty names to mapping information objects.  Inherits parent's
				 * mappings if this layer has a parent.
				 */
				if( this.parentLayer_ && this.parentLayer_.mappings ) {
					// Inherit mappings from parent
					this.maps_ = util.viewOf( this.parentLayer_.mappings() );
				} else {
					// No parent mappings, no inherit
					this.maps_ = {};
				}

				// Add all initial mappings (order is important here)
				this.mapAll(spec.mappings);
				this.mapAll(mappings);
			},

			/**
			 * Removes a layer from its parent.
			 *
			 * @returns {aperture.Layer}
			 *      This layer.
			 */
			remove : function( ) {
				if (this.parentLayer_) {

					// Remove from layer list
					this.parentLayer_.layers_ = util.without( this.parentLayer_.layers_, this );
					this.parentLayer_ = null;

					// remove all graphics
					var c;
					for (c = this.nodes_; c != null; c = c.next) {
						removeNode(c);
					}
					
					this.nodes_ = null;
				}

				return this;
			},

			/**
			 * Returns a {@link aperture.Mapping Mapping} for a given graphic property
			 * to map it from source values. Map is a key function in layers, responsible
			 * for the transformation of data into visuals. Mappings inherit from parent
			 * mappings in the layer hierarchy unless cleared or overridden.
			 *
			 * @param {String} graphicProperty
			 *      The graphic property to return a map for.
			 *
			 * @returns {aperture.Mapping}
			 *      A mapping for this property.
			 */
			map : function ( graphicProperty ) {
				var maps = this.maps_;
				
				// If already have our own local mapping for this, return it
				if( maps.hasOwnProperty(graphicProperty) ) {
					return maps[graphicProperty];
				} 

				// Else must derive a mapping from the parent's mapping 
				// This allows us to first map 'x' in a child layer and then
				// map 'x' in the parent and the mappings will still be shared
				var mapping = this.parentLayer_? 
					util.viewOf(this.parentLayer_.map( graphicProperty )) :
						new namespace.Mapping(graphicProperty);

				return (maps[graphicProperty] = mapping);
			},

			/**
			 * Takes an object and maps all properties as simple values.
			 *
			 * @param {Object} propertyValues
			 *      The object with property values to map.
			 *
			 * @returns {aperture.Layer}
			 *      This layer.
			 */
			mapAll : function ( propertyValues ) {
				forEach(propertyValues, function(value,key) {
					this.map(key).asValue(value);
				}, this);
			},

			/**
			 * Returns an object with properties and their mappings.
			 * @returns {Object} An object with properties and their mappings.
			 */
			mappings : function( ) {
				return this.maps_;
			},

			/**
			 * ${protected}
			 * Returns the value for a supplied visual property given the  object that
			 * will be the source of the data for the mapping.  If the name of the property
			 * does not have a corresponding mapping undefined will be returned.
			 *
			 * @param {String} property
			 *      The name of the visual property for which a value is requested
			 * @param {Object} dataItem
			 *      The data item that will be used as the data source for the mapping
			 * @param {Object} [defaultValue]
			 *      An optional default value that will be returned if a mapping for this
			 *      visual property does not exist.
			 * @param {Number} [index]
			 *      One or more optional indexes to use if this is an array-based visual property
			 *
			 * @returns the value of the visual property based on the mapping or undefined
			 * if no mapping is defined.
			 */
			valueFor : function( property, dataItem, defaultValue, index ) {
				// NOTE TO SELF: would be more optimal if index... was an array rather than args

				var mapping = this.maps_[property];
				if( mapping ) {
					// Create arguments to pass to "valueFor" [layer, dataItem, index, ...]
					var args = Array.prototype.slice.call(arguments, 3);
					var value = mapping.valueFor( dataItem, args );

					if (value != null) {
						return value;
					}
				}
				// No parent, no value, use default (or return undefined)
				return defaultValue;
			},

			/**
			 * ${protected}
			 * Returns the values for a supplied set of visual properties given the object that
			 * will be the source of the data for the mapping.  If the name of the property
			 * does not have a corresponding mapping undefined will be returned.
			 *
			 * @param {Object} properties
			 *      The visual properties for which values are requested, with default
			 *      values.
			 * @param {Object} dataItem
			 *      The data item that will be used as the data source for the mapping
			 * @param {Object} [index]
			 *      An optional index to use if this is an indexed set of visual properties
			 *
			 * @returns {Object} the values of the visual properties based on the mappings.
			 *
			 */
			valuesFor : function( properties, dataItem, index ) {
				var property, mapping, value, values= {};

				for (property in properties) {
					if (properties.hasOwnProperty(property)) {
						values[property] = (mapping = this.maps_[property]) &&
							(value = mapping.valueFor( dataItem, index || [] )) != null?
									value : properties[property];
					}
				}

				return values;
			},

			/**
			 * ${protected}
			 * The type of canvas that this layer requires to render.  At minimum,
			 * the following types are supported:
			 * <ul>
			 * <li>aperture.canvas.DIV_CANVAS</li>
			 * <li>aperture.canvas.VECTOR_CANVAS</li>
			 * </ul>
			 * The canvasType property is used by parent layers to attempt to provide the
			 * desired {@link aperture.Layer.Node} to this layer during
			 * render.
			 */
			canvasType : aperture.canvas.DIV_CANVAS,


			/**
			 * Returns the logical set of all layer nodes, or (re)declares it by providing source data
			 * for each node to be mapped from. 
			 *
			 * @param {Array|Object|Function} data
			 *      the array of data objects, from which each node will be mapped.  May be an array 
			 *      of data objects, a single data object, or a function that subselects data objects
			 *      from each parent data object.  If an array of data is given a graphic
			 *      will be created for each item in the array.  If the parent layer has more than
			 *      one data item, data items will be rendered per parent data
			 *      item.  
			 *      
			 * @param {Function|String} [idFunction]
			 *      optionally a function or field name that supplies the id for a data item to
			 *      match items from one update to another. If a function is supplied it will be called 
			 *      with the item as a parameter. If not supplied, id functions will be remembered from 
			 *      previous calls to this method, but can be cleared by specifying null. If never
			 *      supplied, a best guess is made using item.id for matching if found in the data set 
			 *      supplied, or exact object instances if not.
			 *      
			 * @returns {aperture.Layer.NodeSet}
			 *      the logic set of all layer nodes.
			 */
			all : function ( data, idFunction ) {
				return this.join.apply( this, arguments ); // not implemented yet.
			},

			/**
			 * Merges in new data, returning the logical set of all layer nodes. This method differs
			 * from {@link #all} only in that it compares the new data set to the old data set and if 
			 * the same node is identified in both it will update the existing one rather than creating a
			 * new one. A common use case for joins is to animate transitions between data sets.
			 */
			join : function( data, idFunction ) {
				if (arguments.length !== 0) {
					this.dataItems = inherited;
					this.hasLocalData = false;
					
					// Set new data mapping/array if given
					if ( data ) {
						this.hasLocalData = true;
						
						// Mapping function for parent data
						if( util.isFunction(data) ) {
							this.dataItems = data;
							data = null;
							
						} else {
							// If not an array, assume a single data object, create an array of 1
							if ( !util.isArray(data) ) {
								data = [data];
							}
							this.dataItems = function() {
								return data;
							};
							this.dataItems.values = data;
						}
					}
					
					// handle simple field names as well as functions.
					if ( idFunction !== undefined ) {
						if ( util.isString(idFunction) ) {
							this.idFunction = idFunction === 'id'? getId : function() {
								return this[idFunction];
							};
						} else {
							this.idFunction = idFunction;
						}
						
					} else if (!this.idFunction) {
						// best guess: use id if it seems to be there, otherwise test the instance.
						this.idFunction = data && data.length && data[0].id? getId : null;
					}
					
					// mark changed for next render loop.
					this.dataChanged = true;
				}
				
				return new aperture.Layer.LogicalNodeSet(this);
			},
			
			/**
			 * TODO: implement. Adds to the logical set of all layer nodes, returning the set of added items.
			 * 
			 * @param {Array|Object} data
			 *      the array of data objects, from which each node will be mapped.  May be an array 
			 *      of data objects or a single data object, and may not be a data subselection function.  
			 *      If this layer already gets its data from a subselection function set through {@link #all} 
			 *      or {@link #join}, this function will fail.
			 *      
			 * @returns {aperture.Layer.NodeSet}
			 *      the set of added layer nodes.
			 */
			add : function( data ) {
				
			},
			
			/**
			 * ${protected}
			 * Forms the change list for this layer and returns it.  An object containing information about the data or
			 * visual changes as they pertain to the parent layer is provided.
			 * 
			 * @param {Object} parentChangeSet
			 * @param {Array} parentChangeSet.updates a list of visible Nodes that are added or changed.
			 * this is the list that a graphic child should redraw.
			 * @param {Array} parentChangeSet.added a list of new Node objects pertaining to added data
			 * @param {Array} parentChangeSet.changed a list of visible Node objects pertaining to changed data.
			 * @param {Array} parentChangeSet.removed a list of Nodes that should be removed due to the
			 * removal of the corresponding data
			 *
			 * @returns {Object}
			 *      the changeSet object that this layer may be given to render itself
			 */
			processChangeSet : function ( parentChangeSet ) {
				
				var myChangeSet = util.viewOf( parentChangeSet ),
					myUid = this.uid,
					hasLocalData = this.hasLocalData,
					c, i, n;

				// inherit most things but not these
				var chgs = myChangeSet.changed = [],
					adds = myChangeSet.added = [],
					rmvs = myChangeSet.removed = [];
				
				// is data changed locally or in a parent that we subselect from?
				myChangeSet.dataChanged = this.dataChanged || (parentChangeSet.dataChanged && hasLocalData && !this.dataItems.values);

				
				// reset now that we're going to process.
				this.dataChanged = false;

				
				// SHORTCUT REMOVAL
				// if we rendered local data last time and not this, or vice versa,
				// we need to destroy everything and rebuild to respect new orders of data.
				if (this.renderedLocalData_ != hasLocalData) {
					this.renderedLocalData_ = hasLocalData;
	
					for (c = this.nodes_; c != null; c = c.next) {
						rmvs.push(c); // don't need to unlink - we are resetting the list.
					}
					
					this.nodes_ = null; // trigger rebuild below
				}
				
				
				var prev, dad;
				
				// SHORTCUT REBUILD
				// if complete build, spawn all new nodes.
				if ( !this.nodes_ ) {
					for (dad = (this.parentLayer_ && this.parentLayer_.nodes_) || this.rootNode_; dad != null; dad = dad.next) {
						forEach( this.dataItems( dad.data ), function( dataItem ) {
							adds.push( prev = addNode( this, dad, prev, dataItem ) );
							
						}, this);
					}
					
					// if we are in the change set but have no nodes of our own, implicitly
					// select all children. set this flag for descendants to find.
					if ( !this.nodes_ && myChangeSet.rootSet.hasLayer(this) ) {
						myChangeSet.updateAll = true;
					}
					
					
				// NO DATA OF OUR OWN?
				// when we own data we maintain a node set PER parent node, else there is one per parent node.
				// if we didn't build have been this way before and we know we can trust the list of parent changes.
				} else if( !hasLocalData || !processDataChanges.call( this, myChangeSet, parentChangeSet )) {
					
					// REMOVE ALL MY CHILDREN OF REMOVED
					forEach( parentChangeSet.removed, function(dad) {
						var mine = dad.kids[myUid];
						
						n = mine && mine.length;
							
						// append to removals and remove from local linked list.
						if (n) {
							for (i=0; i<n; ++i) {
								rmvs.push( c = mine[i] );
								unlinkNode(c);
							}
						}
						
					}, this);
					
					// CHANGE ALL MY VISIBLE CHILDREN OF CHANGED
					// notice currently that a change to a parent means a change to all children.
					forEach( parentChangeSet.changed, function(dad) {
						var mine = dad.kids[myUid];
						n = mine && mine.length;
						
						if (n) {
							for (i=0; i<n; ++i) {
								if (updateVisibility( c = mine[i] )) {
									chgs.push(c);
								}
							}
						}
						
					}, this);

					// THEN ADD ALL CHILDREN OF ADDS.
					// then finally process all adds. we do this last so as not to search these in change matching
					forEach( parentChangeSet.added, function(dad) {
						var pk = dad.prev && dad.prev.kids[myUid];
						
						// insert in the same place locally as in parent, though it doesn't really matter.
						prev = pk && pk.length && pk[pk.length-1];
						
						forEach( this.dataItems( dad.data ), function( dataItem ) {
							adds.push(prev = addNode( this, dad, prev, dataItem ));
							
						}, this);
						
					}, this);
					
				}
	
				

				// CLEAN UP REMOVALS
				// finish processing all removals by destroying their graphics.
				forEach( rmvs, function(c) {
					removeNode(c);
				}, this);

			
				
				// ALSO ANY OF MY NODES MARKED INDEPENDENTLY AS CHANGED
				if (myChangeSet.rootSet.hasLayer(this)) {
					for (i = myChangeSet.rootSet.nodes(this); (c = i.next()) != null;) {
						// only add to list if showing (removals will not be showing either) and not already there
						// Add node to list of changes (if not already there)
						if( indexOf(chgs, c) === -1 && indexOf(adds, c) === -1 && updateVisibility(c) ) {
							chgs.push(c); // TODO: hash?
						}
					}
				}

				
				// FORM JOINED LIST OF UPDATED NODES
				// adds always propagate down, but not changes if they are not visible.
				// form the list here of everything that need drawing/redrawing.
				if (adds.length !== 0) {
					var draw = myChangeSet.updates = myChangeSet.changed.splice();
					
					// on construction we did not create graphics unless it was visible
					forEach(adds, function(c) {
						if (c.graphics !== NO_GRAPHICS) {
							draw.push(c);
						}
					}, this);
					
				} else {
					myChangeSet.updates = myChangeSet.changed;
					
				}
				
				
				// DEBUG - log all updates.
				if ( log.isLogging(log.LEVEL.DEBUG ) ) {
					var alist = ' + ', clist = ' * ', rlist= ' - ';
					
					forEach(myChangeSet.added, function(c){
						alist+= c.uid + ', ';
					});
					forEach(myChangeSet.changed, function(c){
						clist+= c.uid + ', ';
					});
					forEach(myChangeSet.removed, function(c){
						rlist+= c.uid + ', ';
					});
					
					log.debug('>> ' + this.typeOf());
					log.debug(alist);
					log.debug(clist);
					log.debug(rlist);
				}
				
				return myChangeSet;
			},
			

			/**
			 * Brings this layer to the front of its parent layer.
			 * 
			 * @returns {this}
			 *      this layer
			 */
			toFront : function () {
				if (this.parentLayer_) {
					var p = this.parentLayer_.layers_,
						i = indexOf(p, this),
						c;
					if (i !== p.length-1) {
						p.push(p.splice(i, 1)[0]);
						
						for (c = this.nodes_; c != null; c = c.next) {
							c.graphics.toFront();
						}
					}
				}
				return this;
			},
			
			/**
			 * Pushes this layer to the back of its parent layer.
			 * 
			 * @returns {this}
			 *      this layer
			 */
			toBack : function () {
				if (this.parentLayer_) {
					var p = this.parentLayer_.layers_,
						i = indexOf(p, this), 
						c;
					if (i !== 0) {
						p.splice(0,0,p.splice(i, 1)[0]);
						
						for (c = this.nodes_; c != null; c = c.next) {
							c.graphics.toBack();
						}
					}
				}
				return this;
			},
			
			/**
			 * ${protected}
			 * The render function is called by the default implementation of a parent layer render()
			 * should be implemented to actually perform this layer's render logic.
			 * The changeSet object will contain Node objects that pertain to this
			 * layer's data.
			 *
			 * If this layer is responsible for drawing visual items, this function should
			 * update all visuals as described by the adds, changes, and removes in the
			 * changeSet object.  The Node objects provided in the changeSet object are owned
			 * by this layer and not shared with any other layer.  This layer is free to
			 * modify the Node.userData object and store any data-visual specific
			 * objects.  The same Node object will be maintained through all calls
			 * to render thoughout the life of the associated data object.
			 *
			 * @param {Object} changeSet
			 * @param {Array} changeSet.updates a list of visible Nodes that are added or changed.
			 * this is the list that a graphic child should redraw.
			 * @param {Array} changeSet.added a list of new Node objects pertaining to added data
			 * @param {Array} changeSet.changed a list of visible Node objects pertaining to changed data
			 * @param {Array} changeSet.removed a list of Nodes that should be removed due to the
			 * removal of the corresponding data
			 */
			render : function( changeSet ) {
			},

			/**
			 * Registers a callback function for a given event type on the visuals
			 * drawn by this layer.  Valid event types include DOM mouse events plus some
			 * custom events:
			 * <ul>
			 * <li>click</li>
			 * <li>dblclick</li>
			 * <li>mousedown</li>
			 * <li>mousemove</li>
			 * <li>mouseout</li>
			 * <li>mouseover</li>
			 * <li>mouseup</li>
			 * <li>touchstart</li>
			 * <li>touchmove</li>
			 * <li>touchend</li>
			 * <li>touchcancel</li>
			 * <li>drag</li>
			 * <li>dragstart*</li>
			 * <li>dragend*</li>
			 * </ul>
			 *
			 * Returning a truthy value from a callback indicates that the event is consumed
			 * and should not be propogated further.<br><br>
			 * 
			 * *Note that registration for <code>drag</code> events will result in the drag
			 * handler being called for drag, dragstart, and dragend events, distinguishable
			 * by the eventType property of the Event object. Attempts to register for dragstart and
			 * dragend events individually will have no effect.
			 * 
			 * @param {String} eventType
			 *      the DOM event name corresponding to the event type for which this callback
			 *      will be registered
			 * @param {Function} callback
			 *      the callback function that will be called when this event is triggered.
			 *      The callback function will be called in the this-node of this layer.
			 *      The function will be passed an object of type {@link aperture.Layer.Event}
			 */
			on : function( eventType, callback ) {
				var h = this.handlers_, c;
				
				if (!h[eventType]) {
					h[eventType] = [callback];
					
					// need one hook only for all clients
					for (c = this.nodes_; c != null; c = c.next) {
						c.graphics.on(eventType, this);
					}
					
				} else {
					h[eventType].push(callback);
				}
			},

			/**
			 * Removes a registered callback(s) for the given event type.
			 *
			 * @param {String} eventType
			 *      the DOM event name corresponding to the event type to unregister
			 * @param {Function} [callback]
			 *      an optional callback function.  If given this specific callback
			 *      is removed from the event listeners on this layer.  If omitted,
			 *      all callbacks for this eventType are removed.
			 */
			off : function( eventType, callback ) {
				var h = this.handlers_, c;
				
				if( callback ) {
					h[eventType] = util.without( h[eventType], callback );
				} else {
					h[eventType] = [];
				}
				
				// all handlers gone for this? then remove hook.
				if (h[eventType].length === 0) {
					for (c = this.nodes_; c != null; c = c.next) {
						c.graphics.off(eventType, this);
					}
					
					h[eventType] = null;
				}
			},

			/**
			 * Fires the specified event to all handlers for the given event type.
			 *
			 * @param {String} eventType
			 *      the DOM event name corresponding to the event type to fire
			 * @param {aperture.Layer.Event} event
			 *      the event object that will be broadcast to all listeners
			 */
			trigger : function( eventType, e ) {
				var r = util.forEachUntil( this.handlers_[eventType], function( listener ) {
					return listener.call(this, e);
				}, true, this);
				
				if (r && e && e.source) {
					e = e.source;
					
					if (e.stopPropagation) {
						e.stopPropagation();
					} else {
						e.cancelBubble = true;
					}
				}

				return r;
			},

			/**
			 * Returns the parent (if it exists) of this layer.
			 */
			parent : function() {
				return this.parentLayer_;
			},


			/**
			 * Returns the containing vizlet for this layer.
			 */
			vizlet : function() {
				return this.vizlet_;
			}
			
		}
	);

	// expose item
	namespace.Layer = Layer;



	var PlotLayer = Layer.extend( 'aperture.PlotLayer',
	/** @lends aperture.PlotLayer# */
	{
		/**
		 * @class An extension of layer, Plot layers can contain child layers.
		 * @augments aperture.Layer
		 *
		 * @constructs
		 * @factoryMade
		 */
		init : function(spec, mappings) {
			aperture.Layer.prototype.init.call(this, spec, mappings);

			/**
			 * @private
			 * An array of child layer objects
			 */
			this.layers_ = [];

		},

		/**
		 * Creates and adds a child layer of the specified type.
		 * Child layers will inherit all mappings and data from their parent layer.
		 *
		 * @example
		 * plot.addLayer( aperture.LabelLayer, {
		 *      font-family: 'Segoe UI',
		 *      fill: 'white'
		 * });
		 *
		 * @param {aperture.Layer} layer
		 *      The type of layer to construct and add.
		 *
		 * @param {Object} [mappings]
		 *      Optional initial simple property : value mappings. More advanced
		 *      mappings can be defined post-construction using the {@link #map}
		 *      function.
		 *
		 * @param {Object} [spec]
		 *      An optional object containing specifications to pass to the layer constructor.
		 *      This specification will be extended with parent and canvas information.
		 *
		 * @returns {aperture.Layer}
		 *      the created child layer
		 */
		addLayer : function( layerCtor, mappings, spec ) {

			spec = spec || {};

			util.extend( spec, {
					parent : this,
					vizlet : this.vizlet_,
					rootNode : this.rootNode_,
					parentCanvas : this.canvas_
				}, this.layerSpec_ );

			var layer = new layerCtor(spec, mappings);

			// Add to layer list
			this.layers_.push( layer );

			return layer;
		},

		/**
		 * ${protected}
		 * 
		 * Overrides the base Layer implementation of render to draw children.
		 */
		render : function( changeSet ) {
			
			if ( log.isLogging(log.LEVEL.DEBUG ) ) {
				log.indent(4);
			}
			
			// invoke draw of all child layers after building a change set for each.
			forEach( this.layers_, function (layer) {
				this.renderChild( layer, layer.processChangeSet(changeSet) );
				
			}, this);

			if ( log.isLogging(log.LEVEL.DEBUG ) ) {
				log.indent(-4);
			}
			
		},

		/**
		 * ${protected}
		 * 
		 * Overridden to remove and clean up child layers.
		 */
		remove : function( ) {
			if (this.parentLayer_) {
				aperture.Layer.prototype.remove.call(this);
				
				// destroye all sublayers
				forEach( this.layers_, function (layer) {
					layer.remove();
				});
				
				this.layers_ = [];
			}

			return this;
		},

		/**
		 * ${protected}
		 * Subclasses of PlotLayer may override this function and update the provided
		 * Node objects that are bound for the given child layer before
		 * rendering it.
		 * For example, a plot layer that alters the size of the canvases
		 * that its children should render to can update the node position and
		 * width/height fields in the provided nodes before they are passed
		 * down to the child layer.
		 *
		 * Note that this is called for each child layer.  If all child layers should
		 * get the same modifications to their render nodes, changes can be made
		 * once to this layer's nodes in render.  These changes will be applied
		 * before the call to this function.
		 * 
		 * @param {aperture.Layer} layer
		 *      The child layer that for which this set of nodes is bound
		 * @param {Object} changeSet
		 *      The changeSet object destined for the given layer
		 * @param {Array} changeSet.updates 
		 *      a list of visible Nodes that are added or changed.
		 *      this is the list that a graphic child should redraw.
		 * @param {Array} changeSet.added
		 *      a list of new Node objects pertaining to added data
		 * @param {Array} changeSet.changed
		 *      a list of visible Node objects pertaining to changed data
		 * @param {Array} changeSet.removed
		 *      a list of Nodes that should be removed due to the
		 *      removal of the corresponding data
		 *
		 */
		renderChild : function( layer, changeSet ) {
			layer.render( changeSet );
		}
		
	});

	namespace.PlotLayer = PlotLayer;
	
	/* ******************************************************************************* */

	/**
	 * @name aperture.Layer.Node
	 * @class A Node object contains information and methods that layer implementations
	 * can use to obtain the constructs they need to render their content.  For example, the node
	 * provides a vector graphics interface which child layers may use to create and manage their
	 * visual representations.
	 * 
	 * ${protected}
	 */

	/**
	 * @name aperture.Layer.Node#data
	 * @type Object
	 * @description the data item that to which this node pertains.
	 * 
	 * ${protected}
	 */

	/**
	 * @name aperture.Layer.Node#parent
	 * @type aperture.Layer.Node
	 * @description an explicit reference to the parent render node for this node, if it
	 * exists.  Generally a node will inherit all properties of its parent but it is occasionally
	 * useful to be able to access the unadulterated values such as position.
	 * 
	 * ${protected}
	 */

	/**
	 * @name aperture.Layer.Node#userData
	 * @type Object
	 * @description an object that can be freely used by the rendering layer to store
	 * information.  Since the same node object will be given to the layer on subsequent
	 * renders of the same data item the layer can store information that allows rendering to
	 * be more efficient, for example visual objects that are created and can be reused.
	 * 
	 * ${protected}
	 */

	/**
	 * @name aperture.Layer.Node#width
	 * @type Number
	 * @description The width of the canvas in pixels.  If the child layer does not have a mapping
	 * that specifies the render width of its visuals, the canvas size should be used.
	 * 
	 * ${protected}
	 */

	/**
	 * @name aperture.Layer.Node#height
	 * @type Number
	 * @description The height of the canvas in pixels.  If the child layer does not have a mapping
	 * that specifies the render width of its visuals, the canvas size should be used.
	 * 
	 * ${protected}
	 */

	/**
	 * @name aperture.Layer.Node#position
	 * @type Array
	 * @description The [x,y] position in pixels within the canvas that the child visual should
	 * draw itself.  Typically top-level visuals will be positioned at [0,0] and will be expected
	 * to fill the entire canvas (as dictated by width/height).  Otherwise, child visuals should
	 * translate the local-coordinate point specified by {@link #anchorPoint} to this position
	 * within the canvas.
	 * 
	 * ${protected}
	 */

	/**
	 * @name aperture.Layer.Node#anchorPoint
	 * @type Array
	 * @description The anchor point is an [x,y] position in [0,1] space that specifies how the child
	 * layer should draw its visuals with respect to the provided canvas {@link #position}.  The x-anchor
	 * point is in the range [0,1] where 0 represents an anchor on the left edge of the visual and 1
	 * represents an anchor on the right edge.  The y-anchor point is also in the range [0,1] where
	 * 0 represents an anchor on the top edge and 1 represents an anchor on the bottom edge.  [0.5, 0.5]
	 * would mean the child visual is centered on the provided canvas {@link #position}.
	 * 
	 * ${protected}
	 */

	/**
	 * @name aperture.Layer.Node#graphics
	 * @type aperture.canvas.Graphics
	 * @description A graphics interface with which to create and update graphics, typically
	 * a {@link aperture.canvas.VectorGraphics VectorGraphics} object.
	 * 
	 * ${protected}
	 */

	/* ******************************************************************************* */

	/**
	 * @name aperture.Layer.Event
	 * @class An event object that is passed to handlers upon the trigger of a requested
	 * event.
	 */

	/**
	 * @name aperture.Layer.Event#eventType
	 * @type String
	 * @description the type of the event being triggered
	 */

	/**
	 * @name aperture.Layer.Event#source
	 * @type Object
	 * @description the source event
	 */

	/**
	 * @name aperture.Layer.Event#data
	 * @type Object
	 * @description the data object for the node that triggered the event
	 */

	/**
	 * @name aperture.Layer.Event#node
	 * @type aperture.Layer.NodeSet
	 * @description the node that triggered the event
	 */

	/**
	 * @name aperture.Layer.Event#index
	 * @type Array
	 * @description an optional property that contains the index into the data item
	 * in the case that the data item contains a sequence of values, such as a line
	 * series.  In the case of indexed values, this field will be an array
	 * of indicies in the order they are referred to in the mappings.  For example,
	 * ${a[].b[].c} will have two items in the index array.  Otherwise, undefined.
	 */

	/**
	 * @name aperture.Layer.Event#dx
	 * @type Number
	 * @description when dragging, the cumulative x offset, otherwise undefined.
	 */

	/**
	 * @name aperture.Layer.Event#dy
	 * @type Number
	 * @description when dragging, the cumulative y offset, otherwise undefined.
	 */


	return namespace;

}(aperture || {}));
/**
 * Source: Class.js
 * Copyright (c) 2014 Oculus Info Inc.
 * @fileOverview Implements the ability to wrap any root layer as a vizlet.
 *
 */

/**
 * @namespace
 * The API for wrapping any root layer as a vizlet for insertion into the DOM.
 */
aperture.vizlet = (
/** @private */
function(namespace) {

	var log = aperture.log;
	
	/**
	 * Takes a layer constructor and generates a constructor for a Vizlet version of
	 * the layer.  Unlike layers which can only be used as children of other layers,
	 * Vizlets can be used as root objects and connected to a DOM element.
	 *
	 * @param {Function} layerConstructor
	 *      The constructor function for the layer for which to generate a Vizlet view.
	 *
	 * @param {Function} [init]
	 *      An optional initialization function which will be called where this
	 *      will be the newly created layer.
	 *
	 * @returns {Function}
	 *      A constructor function for a new Vizlet version of the supplied layer
	 *
	 * @name aperture.vizlet.make
	 * @function
	 */

	var make = function( layerConstructor, init ) {

		// Return a constructor function for the vizlet-layer that takes an id + a spec
		// The constructed object will have all methods of the layer but will take an
		// additional DOM element id on construction and have custom update/animate
		// methods appropriate for a top-level vizlet
		return function( spec, mappings ) {
			var elem,
				elemId,
				// Create the node that will be given to the child layer
				// on every render.
				node = {
						uid: 0,
						width: 0,		// Set at render time
						height: 0,		// Set at render time
						position: [0,0],
						anchorPoint: [0,0],
						userData: {},
						graphics : aperture.canvas.NO_GRAPHICS,
						kids: []
					};

			// an actual element?
			if (spec && spec.nodeType == 1) {
				elem = spec;
				spec = {};

			// else must be an id, either a string or embedded in an object
			} else {
				if( aperture.util.isString(spec) ) {
					// Given an element (id) directly instead of spec obj
					elemId = spec;
					spec = {};
				} else {
					if ( !spec || !spec.id ) {
						return log.error('Cannot make a vizlet from object without an id.');
					}
					// Contained in a spec object
					elemId = spec.id;
				}

				if (elemId === 'body') {
					elem = document.body;
				} else {
					// TODO: we are taking id's but no longer allowing jquery selectors,
					// so only id's, without hashes, should be allowed.
					if (elemId.charAt(0) === '#') {
						elemId = elemId.substr(1);
					}
					elem = document.getElementById(elemId);
				}
			}

			var type = aperture.canvas.type( aperture.canvas.DIV_CANVAS );

			// Extend layer creation specification to include reference to this
			// and canvas
			aperture.util.extend( spec, {
				parent: null,
				rootNode: node,
				parentCanvas : new type( elem )
			});


			// Instantiate the vizlet
			// (Technically instantiating the layer that will look like a vizlet)
			var vizlet = new layerConstructor(spec, mappings);

			// Make top-level update function (will replace update in layer.prototype)
			// This is the key difference between a layer (calls parent's update) and
			// a vizlet (has a DOM element from which nodes are derived).
			var originalLayerUpdate = vizlet.update;

			/**
			 * @private
			 * 
			 * Updates layer graphics.
			 *
			 * @param {aperture.Layer.NodeSet} nodes
			 *      the scope of layer nodes to be updated.
			 *
			 * @param {aperture.Transition} [transition]
			 *      an optional animated transition to use to ease in the changes.
			 *
			 * @returns {this}
			 *      this vizlet
			 */
			vizlet.redraw = function( nodes, transition ) {
				if (log.isLogging(log.LEVEL.DEBUG)) {
					log.indent(0);
					log.debug('------------------------------');
					log.debug(' UPDATE');
					log.debug('------------------------------');
				}
				
				// The root has no data and the node is very basic.  The assumption is
				// that either the child layer or one of its children will eventually have
				// a data definition.
				// Set the node width/height (vizlet could have been resized since last render)
				node.width = elem.offsetWidth;
				node.height = elem.offsetHeight;

				// Top level just provides a node with the container's canvas/size
				// but never indicates that it's changed etc.  Root layer will
				// manage its own data-based add/change/remove
				var changeSet = {
					updates: [],
					changed: [],
					removed: [],
					properties: null, // TODO: refactor out.
					rootSet: nodes,
					transition: transition
				};
				
				// Render this (ie the vizlet-ized layer)
				this.render( this.processChangeSet(changeSet) );

				// flush all drawing ops.
				spec.parentCanvas.flush();

				return this;
			};

			if (init) {
				init.apply( vizlet, arguments );
			}

			// Return the vizlet we created (not "this")
			return vizlet;
		};
	};


	namespace.make = make;

	
	/**
	 * @class Plot is a {@link aperture.PlotLayer PlotLayer} vizlet, suitable for adding to the DOM.
	 *
	 * @augments aperture.PlotLayer
	 * @name aperture.Plot
	 *
	 * @constructor
	 * @param {String|Element} parent
	 *      A string specifying the id of the DOM element container for the vizlet or
	 *      a DOM element itself.
	 * @param {Object} [mappings]
	 *      An optional initial set of property mappings.
	 */
	aperture.Plot= make( aperture.PlotLayer );

	
	return namespace;
}(aperture.vizlet || {}));
/**
 * Source: BarLayer.js
 * Copyright (c) 2014 Oculus Info Inc.
 * @fileOverview Aperture Bar Layer
 */
aperture = (
/** @private */
function(namespace) {
	/**
	 * Given a spec object for describing a bar, this method 
	 * creates the corresponding visual representation.
	 */
	var DEFAULT_FILL = '#8aadec',
		renderBar = function(barSpec, index){
			var node = barSpec.node;
	
			var strokeWidth = this.valueFor('stroke-width', node.data, 1, index),
				lineStroke = this.valueFor('stroke', node.data, 'none', index),
				localFill = this.valueFor('fill', node.data, DEFAULT_FILL, index),
				localOpacity = this.valueFor('opacity', node.data, 1, index);
	
			var bar = node.graphics.rect(
						barSpec.x,
						barSpec.y,
						barSpec.size.width,
						barSpec.size.height);
	
			node.graphics.update(bar, {
				'fill':localFill,
				'stroke':lineStroke,
				'stroke-width':lineStroke==null?0:strokeWidth,
				'stroke-linejoin': 'round',
				'fill-opacity':localOpacity});
			
		return bar;
	};
	
	namespace.BarLayer = aperture.Layer.extend( 'aperture.BarLayer',
	/** @lends aperture.BarLayer# */
	{
		/**
		 * @augments aperture.Layer
		 * @class Given a data source, this layer plots simple, bar visual representations of that data 
		 * (e.g. on a timeline visualization). For more complex charting capabilities, refer to 
		 * {@link aperture.chart.BarSeriesLayer BarSeriesLayer}

		 * @mapping {Number=1} bar-count
		 *   The number of points in a given bar chart data series.
		 *   
		 * @mapping {Number=0} x
		 *   The base horizontal position of the bar.
         * @mapping {Number=0} offset-x
         *   An offset from base horizontal position of the bar, in pixels

		 * @mapping {Number=0} y
		 *   The base vertical position of the bar.
         * @mapping {Number=0} offset-y
         *   An offset from the base vertical position of the bar, in pixels

		 * @mapping {String='vertical'} orientation
		 *   Sets the orientation of the chart. Vertically oriented charts will have bars that expand along the y-axis,
		 *   while horizontally oriented charts will have bars expanding along the x-axis. By default, this property
		 *   is set to 'vertical'.

		 * @mapping {Number} width
		 *   Sets the width of each bar in the chart (i.e. the bar's thickness). For charts with a horizontal
		 *   orientation, the width is measured along the y-axis. Similarly, for vertically oriented charts,
		 *   the width is measured along the x-axis. For most conventional usages, the width will be the
		 *   lesser of the two values when compared against length.

		 * @mapping {Number} length
		 *   Mapping for determining the length of each bar in the chart. For charts with a horizontal
		 *   orientation, the length is measured along the x-axis. Similarly, for vertically oriented
		 *   charts, the length is measured along the y-axis.

		 * @mapping {Boolean=true} bar-visible
		 *   Property for toggling the visibility of individual bars in the chart. Setting the global property of  
		 *   'visible' to FALSE overrides the value of this property and will hide all the bar visuals.   

		 * @mapping {String='#8aadec'} fill 
		 *   Sets the fill colour of the bar.
		 *   
		 * @mapping {Number=1} opacity
		 *  The opacity of a bar. Values for opacity are bound with the range [0,1], with 1 being opaque.
		 * 
		 * @mapping {String='none'} stroke 
		 *   By default no stroke is used when drawing the bar charts, only the fill value is used.
		 *   Setting this value will draw a coloured outline around each bar in the chart.

		 * @mapping {Number=1} stroke-width
		 *   The width (in pixels) of the stroke drawn around each bar. This value is only used if the 'stroke'
		 *   property is set to a visible value. 

		 * @constructs
		 * @factoryMade
		 */
		init : function(spec, mappings) {
			aperture.Layer.prototype.init.call(this, spec, mappings);
		},

		canvasType : aperture.canvas.VECTOR_CANVAS,

		render : function(changeSet) {
			// Create a list of all additions and changes.
			// Determine how the bars should be laid out.
			var seriesSpec = this.applyLayout(changeSet.updates);
			// Render the bars.
			this.updateLayer.call(this, seriesSpec, changeSet.transition);
		},
		
		
		/**
		 * @private
		 * Calculate the layout of the bars, taking into account
		 * chart orientation, and visibility.
		 */
		applyLayout : function(dataObject) {
			var seriesSpec = [],
				seriesId,
				index;
			for (seriesId = 0; seriesId < dataObject.length; seriesId++){
				var barSpecs = [];			
				var node = dataObject[seriesId];

				var numBars = this.valueFor('bar-count', node.data, 1, seriesId);
				var orientation = this.valueFor('orientation', node.data, 'vertical', index);

				var maxLength = orientation == 'vertical'?node.height:node.width;
				
				for (index=0; index < numBars; index++){
					// Check if the point is visible.
					var isVisible = this.valueFor('bar-visible', node.data, true, index, seriesId);

					if (isVisible){
						var startValue = this.valueFor('x', node.data, 0, index),
							width = this.valueFor('width', node.data, 2, index),
							length = this.valueFor('length', node.data, 0, index),
							startPt = (startValue * node.width) + (node.position[0]||0),
							yPoint = (this.valueFor('y', node.data, 0, index) * node.height) + (node.position[1]||0),
					        offsetX = this.valueFor('offset-x', node.data, 0, index),
					        offsetY = this.valueFor('offset-y', node.data, 0, index);


						var barSpec = {
								id : index,
								x : startPt + offsetX,
								y : yPoint + offsetY,
								size : {
									width : orientation == 'vertical'?width:length,
									height : orientation == 'vertical'?length:width
								},
								strokeWidth : 1,
								orientation : orientation,
								visible : isVisible,
								node : node
						};
						barSpecs.push(barSpec);
					}
				}
				seriesSpec[seriesId] = barSpecs; 
			}
			return seriesSpec;
		},
		
		
		/**
		 * @private
		 * This method takes a collection of specs that describe the size and position
		 * of each bar (or bar segment in the case of stacked bars), applies
		 * additional styling properties if specified, and then passes the objects
		 * off for rendering.
		 * 
		 * If the bar element has already been rendered previously, retrieve the existing
		 * visual and update its visual attributes.
		 */
		updateLayer : function(seriesSpec, transition) {
			var seriesId,
				index;
			for (seriesId = 0; seriesId < seriesSpec.length; seriesId++){
				var barSpecs = seriesSpec[seriesId];
				var barCount = barSpecs.length;

				if (barCount > 0){
					for (index=0; index< barCount; index++){
						var barSpec = barSpecs[index];
						var node = barSpec.node;

						if (!node.userData.bars){
							node.userData.bars = {};
						}
						
						var barSeriesData = barSpec.node.data;
						var lineStroke = this.valueFor('stroke', barSeriesData, 'none', index);
						var barLayout =	this.valueFor('bar-layout', barSeriesData, null, index);

						// Check if this bar already exists for this node. If it does
						// we want to do an update. Otherwise we'll create a new graphic
						// object for it.
						var bar = node.userData.bars[index];

						// Check if the visual exceeds the current context size,
						// culling if necessary.
						// To prevent visuals from seeming to "pop" into the scene
						// when panning, we want to allow a buffer area of N bars,
						// at either end of the corresponding axis, when culling.
						var xPoint = barSpec.x,
							yPoint = barSpec.y,
							renderBarDim = barSpec.size,
							nBarOffset = 2; // Allows for a buffer of 1 bar.
						
						// Since we only support horizontal panning, we only need to cull along the x-axis.
						if (cullPoint = xPoint > node.width + node.position[0]|| xPoint + nBarOffset*renderBarDim.width < node.position[0]){
							if (bar) {
								node.graphics.remove(bar);
								delete node.userData.bars[index];
							}
							continue;
						}
						// If this bar already exists, update its visual
						// properties.
						if (bar){
							var localFill = this.valueFor('fill', node.data, DEFAULT_FILL, index);
							var localOpacity = this.valueFor('opacity', node.data, 1, index);
							node.graphics.update(bar, {
								fill:localFill, 
								stroke:lineStroke,
								x : xPoint,
								y : yPoint,
								opacity : localOpacity,
								"stroke-width":lineStroke==null?0:barSpec.strokeWidth,
								"stroke-linejoin": "round",
								"width" : renderBarDim.width,
								"height" : renderBarDim.height
							}, transition);
						}
						else {
							// This is a new bar so we'll create a new visual for it.
							bar = renderBar.call(this, barSpec, index);
							// Associate data with the bar visual.
							node.graphics.data(bar, barSeriesData, [index]);
							// Cache the visual for this bar.
							node.userData.bars[index] = bar;
						}
					}
				}
			}
		}
	});

	return namespace;
	
}(aperture || {}));
/**
 * Source: Color.js
 * Copyright (c) 2014 Oculus Info Inc.
 * @fileOverview Aperture Color APIs
 */

/**
 * @ignore
 * Ensure namespace exists
 */
aperture = (
/** @private */
function(namespace) {

	// PRIVATE

	var int = Math.round, // shortcut

		// base utility fn for parsing a color channel that handles percentages
		chAny = function ( str, pctF ) {

			// match number and optional pct
			var m = str.match( /([0-9\.]+)(%*)/ );

			// convert number
			var n = parseFloat( m[1] );

			// return result - if pct multiply by factor.
			return m[2]? n * pctF : n;
		},

		// derivativchAnyannel for ranges 0-255
		ch255 = function ( str ) {
			return int( chAny( str, 2.55 ));
		},

		// hsl to rgb conversion. h in degrees, s and l in 0 to 1.
		// because color is immutable we place this fn in private space.
		hsl = function( h, s, l, color ) {

			// clamp to legitimate ranges
			s = Math.min(1, Math.max(0, s));
			l = Math.min(1, Math.max(0, l));

			// shortcut for gray
			if (s == 0) {
				color.r = color.g = color.b = int( 255 * l );
				return;
			}

			// constants derived from s,l for calc below
			var q = (l <= 0.5) ? (l * (1 + s)) : (l + s - l * s);
			var p = 2 * l - q;

			// channel from offset in hue, subject to s+l
			function rgb1(h) {

				// clamp
				h-= Math.floor(h/360) * 360;

				// switch on four bands
				if (h < 60)  {
					return p + (q - p) * h / 60;
				} if (h < 180) {
					return q;
				} if (h < 240) {
					return p + (q - p) * (240 - h) / 60;
				}
				return p;
			}
			function rgb255( h ) {

				return int( 255 * rgb1( h ));
			}

			// push result to color
			color.r = rgb255( h + 120 );
			color.g = rgb255( h );
			color.b = rgb255( h - 120 );
			
			color.h = h;
			color.s = s;
			color.l = l;
			color.v = Math.max( color.r, color.g, color.b )/255;
		},

		// hsv to rgb conversion. h in degrees, s and v in 0 to 1.
		// because color is immutable we place this fn in private space.
		hsv = function( h, s, v, color ) {
			color.h = h;
			color.s = s;
			color.v = v;
		    
			h /= 60;
			
			var i = Math.floor(h),
				f = h - i,
				p = v * (1 - s),
				q = v * (1 - f * s),
				t = v * (1 - (1 - f) * s);

			switch(i % 6) {
				case 0: color.r = v; color.g = t; color.b = p; break;
				case 1: color.r = q; color.g = v; color.b = p; break;
				case 2: color.r = p; color.g = v; color.b = t; break;
				case 3: color.r = p; color.g = q; color.b = v; break;
				case 4: color.r = t; color.g = p; color.b = v; break;
				case 5: color.r = v; color.g = p; color.b = q; break;
			}
		    
			color.l = (Math.max(color.r, color.g, color.b) 
					+ Math.min(color.r, color.g, color.b)) / 2;
		    
			color.r = Math.round(color.r* 255);
			color.g = Math.round(color.g* 255);
			color.b = Math.round(color.b* 255);

		},

		// sets the hsl storage from the color's rgb.
		setHslv = function(color) {
			if (color.h != null) {
				return;
			}
			
			var r = color.r/ 255, 
				g = color.g/ 255,
				b = color.b/ 255;
		    
			var max = Math.max(r, g, b), min = Math.min(r, g, b);
			var h, s, l = (max + min) / 2;

			if (max == min) {
				h = s = 0; // grayscale
			} else {
				var d = max - min;
				s = l > 0.5 ? d / (2 - max - min) : d / (max + min);
				switch(max){
					case r: h = (g - b) / d; break;
					case g: h = (b - r) / d + 2; break;
					case b: h = (r - g) / d + 4; break;
				}
				h = ((h + 360) % 6) * 60;
			}

			color.h = h;
			color.s = s;
			color.l = l;
			color.v = max;
		},
		
		// two digit hexidecimalizer
		hex2 = function( num ) {
			num = num.toString(16);

			return num.length < 2? '0' + num : num;
		},

		// rgb[a] to string
		strFromVals = function ( r, g, b, a ) {
			// preserve alpha in the result color only if relevant.
			return ( a > 0.9999 )?
					('#'
						+ hex2( r )
						+ hex2( g )
						+ hex2( b ))
					: ('rgba('
						+ r.toString() + ','
						+ g.toString() + ','
						+ b.toString() + ','
						+ a.toString() + ')'
				);
		},

		// initialize the color string from the rgba values
		setStr = function ( color ) {
			color.color = strFromVals( color.r, color.g, color.b, color.a );
		},
		
		fromHSL, fromHSB;

		namespace.Color = aperture.Class.extend( 'aperture.Color',
		/** @lends aperture.Color.prototype */
		{
			/**
			 * @class Represents a color with support for runtime access of
			 * channel values. Since Aperture supports the use of CSS color string
			 * values, Color objects are used primarily for efficient manipulation
			 * of color, such as in mapping or filter operations.
			 *
			 * Colors are designed to be immutable.
			 * <br><br>
			 *
			 * Color values may be specified in hexadecimal, RGB, RGBA, HSL, HSLA,
			 * or named color form. Named colors include any colors configured in
			 * aperture.palette along with the standard 17 defined by CSS 2.1.<p>
			 *
			 * @constructs
			 * @extends aperture.Class
			 *
			 * @param {String} color
			 *   a name or css color value string.
			 *
			 * @returns {this}
			 *   a new Color
			 */
			init : function( color ) {

				// default in the no argument case is transparent black.
				if ( !color ) {
					this.r = this.g = this.b = this.a = 0;
					this.color = '#000000';
					return;
				}

				// case insensitive
				// TODO: consider always converting to rgb by calling setStr
				this.color = color = color.toLowerCase();

				// hexadecimal colors
				if (color.charAt(0) == '#') {

					// offset of second digit (covering #rgb and #rrggbb forms)
					var digit2 = (color.length === 7? 1:0),
						i = 0;

					// parse using base 16.
					this.r = parseInt( color.charAt( ++i ) + color.charAt( i+=digit2 ), 16 );
					this.g = parseInt( color.charAt( ++i ) + color.charAt( i+=digit2 ), 16 );
					this.b = parseInt( color.charAt( ++i ) + color.charAt( i+=digit2 ), 16 );
					this.a = 1;

					return;
				}

				var matchFn = color.match(/([a-z]+)\((.*)\)/i);

				// rgb, rgba, hsl, hsla
				if (matchFn) {

					// pull the three left chars and split up the arguments
					var func = matchFn[1].substring(0,3),
						args = matchFn[2].split(','),
						h,s,l;

					// alpha, or default opacity which is 1
					this.a = args.length > 3? chAny( args[3], 0.01 ) : 1;

					switch (func) {
					case 'rgb':
						this.r = ch255(args[0]);
						this.g = ch255(args[1]);
						this.b = ch255(args[2]);

						return;

					case 'hsl':
						// convert (leave hsl precision - we round post-op)
						h = chAny(args[0], 3.60);
						s = chAny(args[1], 0.01);
						l = chAny(args[2], 0.01);
						hsl( h, s, l, this );

						return;
					}
				}

				// assume named.
				color = aperture.palette.color( color );

				// log name lookups that are missing
				if ( !color ) {
					aperture.log.warn( 'unrecognized color ' + color );
				}

				// recurse once only to set from value
				this.init( color );
			},

			/**
			 * Blends this color with the supplied color and returns
			 * a resulting color. Blending provides comprehensive
			 * coverage of color derivation use cases in one function by
			 * intuitively specifying what the destination is and how much
			 * weight should be given the destination versus the source.
			 * For instance, rather than darken or lighten a foreground color blend it
			 * to the background color so it better adapts to a different
			 * color scheme.<p>
			 *
			 * If the color is supplied as a string value a Color object
			 * will be created for it, so in cases where this method is
			 * called frequently with the same color value but different weights
			 * it is better to pre-construct the color as an object and
			 * pass that in instead.
			 *
			 * @param {Color|String} color
			 *  the color to blend with.
			 *
			 * @param {Number} weight
			 *  the weighting of the supplied color in the blend
			 *  process, as a value from 0.0 to 1.0.
			 *
			 * @returns {aperture.Color}
			 *  a blended color
			 */
			blend : function ( color, weight ) {

				// convert to an object if isn't already
				if ( typeof color === 'string' ) {
					color = new namespace.Color( color );
				}

				var w1 = 1 - weight,
					c = new namespace.Color();
				
				c.r = int( w1 * this.r + weight * color.r );
				c.g = int( w1 * this.g + weight * color.g );
				c.b = int( w1 * this.b + weight * color.b );
				c.a = int((w1 * this.a + weight * color.a) * 1000 ) * 0.001;
				
				// initialize the color string
				setStr(c);
				
				return c;
			},

			/**
			 * Returns an array of interpolated colors between this color and the 
			 * toColor suitable for use in a map key. The first color will be 
			 * this color and the last will be the toColor.
			 * 
			 * @param {aperture.Color} toColor
			 *  the end color to interpolate to
			 * 
			 * @param {Number} bands
			 *  the number of colors to create
			 * 
			 * @returns {Array}
			 *  an array of colors, of length bands
			 */
			band : function ( toColor, bands ) {
				var a = [this];
				
				if (bands > 1) {
					a.length = bands;
	
					var base = bands-1, i;
					for (i=1; i< bands; i++) {
						a[i] = this.blend( toColor, i/base );
					}
				}
				return a;
			},
			
			/**
			 * Gets the hue as a value between 0 and 360, or if an
			 * argument is supplied returns a new color with the hue
			 * given but the same saturation and lightness.
			 * 
			 * @returns {Number|aperture.Color}
			 *  a value for hue, or a new color with the hue specified.
			 */
			hue : function( value ) {
				setHslv(this);
				
				if (value != null) {
					return fromHSL(value, this.s, this.l, this.a);
				}
				
				return this.h;
			},
			
			/**
			 * Gets the saturation as a value between 0 and 1, or if an
			 * argument is supplied returns a new color with the saturation
			 * given but the same hue and lightness as this color.
			 * 
			 * @returns {Number|aperture.Color}
			 *  a value for saturation, or a new color with the saturation specified.
			 */
			saturation : function( value ) {
				setHslv(this);
				
				if (value != null) {
					return fromHSL(this.h, value, this.l, this.a);
				}
				
				return this.s;
			},
			
			/**
			 * Gets the lightness as a value between 0 and 1, or if an
			 * argument is supplied returns a new color with the lightness
			 * given but the same hue and saturation as this color.
			 * 
			 * @returns {Number|aperture.Color}
			 *  a value for lightness, or a new color with the lightness specified.
			 */
			lightness : function( value ) {
				setHslv(this);
				
				if (value != null) {
					return fromHSL(this.h, this.s, value, this.a);
				}
				
				return this.l;
			},
			
			/**
			 * Gets the brightness as a value between 0 and 1, or if an
			 * argument is supplied returns a new color with the brightness
			 * given but the same hue and saturation as this color.
			 * 
			 * @returns {Number|aperture.Color}
			 *  a value for brightness, or a new color with the brightness specified.
			 */
			brightness : function( value ) {
				setHslv(this);
				
				if (value != null) {
					return fromHSB(this.h, this.s, value, this.a);
				}
				
				return this.v;
			},
			
			/**
			 * Returns the color value as a valid CSS color string.
			 *
			 * @returns {String}
			 *  a CSS color string.
			 */
			css : function () {
				return this.color;
			},

			/**
			 * Overrides Object.toString() to return the value of {@link css}().
			 *
			 * @returns {String}
			 *  a CSS color string.
			 */
			toString : function ( ) {
				// temp debug
				//return this.r + ',' + this.g + ',' + this.b + ',' + this.a;
				return this.color;
			}
		}
	);

	/**
	 * Constructs a new color from numeric hue/ saturation/ lightness values.
	 * Alternatively, the class constructor can be used to construct a color
	 * from an hsl[a] string.
	 *
	 * @param {Number} h
	 *  the hue as a number in degrees (0-360), or an object
	 *  with h,s,l[,a] properties.
	 * @param {Number} s
	 *  the saturation as a number from 0-1
	 * @param {Number} l
	 *  the lightness as a number from 0-1
	 * @param {Number} [a]
	 *  the alpha value as a number from 0-1
	 *
	 * @returns {aperture.Color}
	 *  the new color
	 * 
	 * @name aperture.Color.fromHSL
	 * @function
	 */
	namespace.Color.fromHSL = fromHSL = function (h, s, l, a) {
		var color = new namespace.Color();

		// object handler
		if (typeof h != 'number') {
			if (h.h == null) {
				return color;
			}

			s = h.s;
			l = h.l;
			a = h.a;
			h = h.h;
		}

		// assign alpha if present
		color.a = a != null? a : 1;

		// normalize percentages if specified as such
//		s > 1 && (s *= 0.01);
//		l > 1 && (l *= 0.01);

		// convert to rgb and store in color
		hsl(h, s, l, color);

		// initialize the color string
		setStr(color);

		return color;
	};

	/**
	 * Constructs a new color from numeric hue/ saturation/ brightness values.
	 *
	 * @param {Number} h
	 *  the hue as a number in degrees (0-360), or an object
	 *  with h,s,b[,a] properties.
	 * @param {Number} s
	 *  the saturation as a number from 0-1
	 * @param {Number} b
	 *  the brightness as a number from 0-1
	 * @param {Number} [a]
	 *  the alpha value as a number from 0-1
	 *
	 * @returns {aperture.Color}
	 *  the new color
	 * 
	 * @name aperture.Color.fromHSL
	 * @function
	 */
	namespace.Color.fromHSB = fromHSB = function (h, s, b, a) {
		var color = new namespace.Color();

		// object handler
		if (typeof h != 'number') {
			if (h.h == null) {
				return color;
			}

			s = h.s;
			b = h.b;
			a = h.a;
			h = h.h;
		}

		// assign alpha if present
		color.a = a != null? a : 1;

		// normalize percentages if specified as such
//		s > 1 && (s *= 0.01);
//		l > 1 && (l *= 0.01);

		// convert to rgb and store in color
		hsv(h, s, b, color);

		// initialize the color string
		setStr(color);

		return color;
	};

	/**
	 * Constructs a new color from numeric red/ green /blue values.
	 * Alternatively, the class constructor can be used to construct a color
	 * from an rgb[a] string.
	 *
	 * @param {Number} r
	 *  the red component as a number from 0-255, or an object
	 *  with r,g,b[,a] properties.
	 * @param {Number} g
	 *  the green component as a number from 0-255
	 * @param {Number} b
	 *  the blue component as a number from 0-255
	 * @param {Number} [a]
	 *  the alpha value as a number from 0-1
	 *
	 * @returns {aperture.Color}
	 *  the new color
	 * 
	 * @name aperture.Color.fromRGB
	 * @function
	 *
	 */
	namespace.Color.fromRGB = function (r, g, b, a) {
		var color = new namespace.Color();

		// object handler
		if (typeof r != 'number') {
			if (r.r == null) {
				return color;
			}

			g = r.g;
			b = r.b;
			a = r.a;
			r = r.r;
		}

		// assign
		color.r = r;
		color.g = g;
		color.b = b;
		color.a = a != null? a : 1;

		// initialize the color string
		setStr(color);

		return color;
	};

	/**
	 * Returns an array of interpolated colors between the first and 
	 * last color in the set of colors supplied, suitable for use in a 
	 * map key. This is a convenience function for rebanding colors
	 * (or banding colors from a scalar color map key) which simply
	 * calls the band function on the first color with the last color.
	 * 
	 * @param {Array} colors
	 *  the colors to band or reband between.
	 * 
	 * @param {Number} bands
	 *  the number of colors to create
	 * 
	 * @returns {Array}
	 *  an array of colors, of length bands
	 * 
	 * @name aperture.Color.band
	 * @function
	 */
	namespace.Color.band = function( colors, bands ) {
		if (colors && colors.length) {
			return colors[0].band(colors[colors.length-1], bands);
		}
	};
	
	return namespace;

}(aperture || {}));
/**
 * Source: Format.js
 * Copyright (c) 2014 Oculus Info Inc.
 * @fileOverview Formats values.
 */

/**
 * @namespace
 * @ignore
 * Ensure namespace exists
 */
aperture = (
/** @private */
function(namespace) {

	// TODO: extend these to take precise format specifications as a string.

	/**
	 * @class Format objects are used by {@link aperture.Scalar Scalars} for formatting
	 * values for display, but may be used independently as well.
	 *
	 * @extends aperture.Class
	 *
	 * @description
	 * The default implementation of Format does nothing other than use
	 * the String() function to coerce the value to a String. Default formats
	 * for numbers and times are provided by the appropriate static method.
	 *
	 * @name aperture.Format
	 */
	namespace.Format = namespace.Class.extend( 'aperture.Format',
		/** @lends aperture.Format.prototype */
		{
			/**
			 * Formats the specified value.
			 *
			 * @param value
			 *      The value to format.
			 *
			 * @returns {String}
			 *      The formatted value.
			 */
			format : function ( value ) {
				return String(value);
			},

			/**
			 * Given a level of precision in type specific form, returns
			 * the next (lesser) level of precision in type specific form,
			 * if and only if such orders of formatting are required for
			 * full expression of the value.
			 *
			 * This method is often used for a date axis and is best expressed
			 * by an example.
			 * <br><br>
			 * When an axis is labeled to the precision of
			 * hours for instance, best practice would dictate that each
			 * hour not be labeled repeatedly by date, month and year,
			 * even those exist in the data. However if the axis spanned
			 * days, it would be desirable to label the beginning of each
			 * day, secondarily to each hour. This method provides the means
			 * of doing so:
			 *
			 * @example
			 * var hourFormat = aperture.Format.getTimeFormat( 'Hours' );
			 *
			 * // displays 'Date'
			 * alert( hourFormat.nextOrder() );
			 *
			 * @returns
			 *      The next precision level, or undefined if there isn't one.
			 */
			nextOrder : function () {
			}
		}
	);


	/**
	 * @private
	 * @class A Format object that translates numbers to
	 * Strings. Format objects are used by {@link aperture.Scalar Scalars} for formatting
	 * values, but may be used independently as well.
	 *
	 * @extends aperture.Format
	 *
	 * @description
	 * Constructs a number format.
	 *
	 * @name aperture.NumberFormat
	 */
	namespace.NumberFormat = namespace.Format.extend( 'aperture.NumberFormat',
		{
			/**
			 * @private
			 *
			 * @param {Number} [precision]
			 *      The optional precision of the value to format. For numbers this
			 *      will be a base number to round to, such as 1 or 0.001.
			 *
			 * @returns {aperture.NumberFormat}
			 *      A new time format object.
			 */
			init : function ( precision ) {
				if (precision) {
					if (isNaN(precision)) {
						aperture.log.warn('Invalid precision "' + precision + '" in NumberFormat');
					} else {
						this.precision = precision;
					}
				}
			},

			/**
			 * @private
			 * Formats the specified value.
			 *
			 * @param {Number} value
			 *      The value to format.
			 *
			 * @returns {String}
			 *      The formatted value.
			 */
			format : function ( value ) {

				// precision based formatting?
				if ( value != null && this.precision ) {
					value = Math.round( value / this.precision ) * this.precision;
				} else {
					value = Number(value);
				}

				return String(value);
			}
		}
	);

	/**
	 * Returns a number format object, suitable for formatting numeric values.
	 *
	 * @param {Number} [precision]
	 *      The optional precision of the value to format. For numbers this
	 *      will be a base number to round to, such as 1 or 0.001.
	 *
	 * @returns {aperture.Format}
	 *      a number format object.
	 *
	 * @name aperture.Format.getNumberFormat
	 * @function
	 */
	namespace.Format.getNumberFormat = function( precision ) {
		return new namespace.NumberFormat( precision );
	};
	
	/**
	 * @private
	 * @class A Format object that translates numbers to currency
	 * Strings. Format objects are used by {@link aperture.Scalar Scalars} for formatting
	 * values, but may be used independently as well.
	 *
	 * @extends aperture.Format
	 *
	 * @description
	 * Constructs a number format.
	 *
	 * @name aperture.NumberFormat
	 */
	namespace.CurrencyFormat = namespace.Format.extend( 'aperture.CurrencyFormat',
		{
			/**
			 * @private
			 *
			 * @param {Number} [precision] [prefix] [suffix]
			 *      The optional precision of the value to format. For numbers this
			 *      will be a base number to round to, such as 1 or 0.01.
			 *      
			 *      The optional prefix is a string value for the currency (i.e. '$')
			 *      
			 *      The optional prefix is a string value for the currency (i.e. 'USD')
			 *
			 * @returns {aperture.NumberFormat}
			 *      A new time format object.
			 */
			init : function (precision, prefix, suffix) {
				if (precision) {
					if (isNaN(precision)) {
						aperture.log.warn('Invalid precision "' + precision + '" in NumberFormat');
					} else {
						this.precision = precision;
					}
				}
				this.prefix = (prefix) ? prefix : '';
				this.suffix = (suffix) ? suffix : '';
			},

			/**
			 * @private
			 * Formats the specified value.
			 *
			 * @param {Number} value
			 *      The value to format.
			 *
			 * @returns {String}
			 *      The formatted value.
			 */
			format : function (value) {

				value = Number(value);
				
				var numberSuffix = '';
				
				var number = Math.abs(value);
				
				if (number >= 1000000000000) {
					numberSuffix = 'T';
					number *= 0.000000000001;
				} else if (number >= 1000000000) {
					numberSuffix = 'B';
					number *= 0.000000001;
				} else if (number >= 1000000) {
					numberSuffix = 'M';
					number *= 0.000001;
				} else if (number >= 1000) {
					numberSuffix = 'K';
					number *= 0.001;
				}
				
				if (this.precision) {
					number = Math.round(number / this.precision) * this.precision;
				}
				
				var sign = (value < 0) ? '-' : '';
				
				var s = number.toString();
				
				for (var i = s.length-3; i > 0; i -= 3) {
					s = s.substring(0, i).concat(',').concat(s.substring(i));
				}
				
				return sign + this.prefix + s + numberSuffix + this.suffix;
			}
		}
	);

	/**
	 * Returns a number format object, suitable for formatting numeric values.
	 *
	 * @param {Number} [precision] [prefix] [suffix]
	 *      The optional precision of the value to format. For numbers this
	 *      will be a base number to round to, such as 1 or 0.01.
	 *      
	 *      The optional prefix is a string value for the currency (i.e. '$')
	 *      
	 *      The optional prefix is a string value for the currency (i.e. 'USD')
	 *
	 * @returns {aperture.Format}
	 *      a number format object.
	 *
	 * @name aperture.Format.getNumberFormat
	 * @function
	 */
	namespace.Format.getCurrencyFormat = function(precision, prefix, suffix) {
		return new namespace.CurrencyFormat(precision, prefix, suffix);
	};

	// create the hash of time orders.
	// use discrete format functions for speed but don't pollute our closure with them.
	var timeOrders = (function () {

		// DATE FORMATTING THINGS
		var months = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'],
			days = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'],
			y = 'FullYear', d = 'Date', m = 'Minutes';

		// time format functions.
		function pad2( num ) {
			return num < 10? '0' + num : String(num);
		}
		function hh12( date ) {
			var h = date.getHours();
			return h? (h < 13? String(h) : String(h - 12)) : '12';
		}
		function ampm( date ) {
			return date.getHours() < 12? 'am' : 'pm';
		}
		function millis( date ) {
			return ':' + ((date.getSeconds()*1000 + date.getMilliseconds())/1000) + 's';
		}
		function ss( date ) {
			return ':' + pad2(date.getSeconds()) + 's';
		}
		function hhmm( date ) {
			return hh12(date) + ':' + pad2(date.getMinutes()) + ampm(date);
		}
		function hh( date ) {
			return hh12(date) + ampm(date);
		}
		function mondd( date ) {
			return months[date.getMonth()] + ' '+ date.getDate();
		}
		function day( date ) {
			return days[date.getDay()] + ' ' + mondd(date);
		}
		function mon( date ) {
			return months[date.getMonth()];
		}
		function year( date ) {
			return String(date.getFullYear());
		}
		function yy( date ) {
			return "'" + String(date.getFullYear()).substring(start, end);
		}

		return {
			'FullYear'     : { format : year },
			'Year'         : { format : yy },
			'Month'        : { format : mon,    next : y },
			'Date'         : { format : mondd,  next : y },
			'Day'          : { format : mondd,  next : y },
			'Hours'        : { format : hh,     next : d },
			'Minutes'      : { format : hhmm,   next : d },
			'Seconds'      : { format : ss,     next : m },
			'Milliseconds' : { format : millis, next : m }
		};

	}());

	/**
	 * @private
	 * @class A Format object that translates times to
	 * Strings. Format objects are used by {@link aperture.Scalar Scalars} for formatting
	 * values, but may be used independently as well.
	 *
	 * @extends aperture.Format
	 *
	 * @description
	 * Constructs a time format.
	 *
	 * @name aperture.TimeFormat
	 */
	namespace.TimeFormat = namespace.Format.extend( 'aperture.TimeFormat',

		{
			/**
			 * @private
			 *
			 * @param {String} [precision]
			 *      The optional precision of the value to format. For times this
			 *      will be a Date field reference, such as 'FullYear' or 'Seconds'.
			 *
			 * @returns {aperture.TimeFormat}
			 *      A new time format object.
			 */
			init : function ( precision ) {
				if (precision) {
					this.order = timeOrders[precision];

					if (!this.order) {
						aperture.log.warn('Invalid precision "' + precision + '" in TimeFormat');
					}
				}
			},

			/**
			 * @private
			 * Formats the specified value.
			 *
			 * @param {Date|Number} value
			 *      The value to format, as a Date or time in milliseconds.
			 *
			 * @returns {String}
			 *      The formatted value.
			 */
			format : function ( value ) {

				// precision based formatting?
				if ( value != null ) {
					if (!value.getTime) {
						value = new Date(value);
					}
					if ( this.order ) {
						return this.order.format( value );
					}
				}

				return String(value);
			},

			/**
			 * @private
			 * @returns the next (lesser) logical level of precision, if and only if such
			 * orders of formatting are required for full expression of the value.
			 */
			nextOrder : function () {
				if ( this.order ) {
					return this.order.next;
				}
			}
		}
	);

	/**
	 * Returns a time format object, suitable for formatting dates and times.
	 *
	 * @param {String} [precision]
	 *      The optional precision of the value to format. For times this
	 *      will be a Date field reference, such as 'FullYear' or 'Seconds'.
	 *
	 * @returns {aperture.Format}
	 *      a time format object.
	 *
	 * @name aperture.Format.getTimeFormat
	 * @function
	 */
	namespace.Format.getTimeFormat = function( precision ) {
		return new namespace.TimeFormat( precision );
	};

	return namespace;

}(aperture || {}));

/**
 * Source: IconLayer.js
 * Copyright (c) 2014 Oculus Info Inc.
 * @fileOverview Aperture Icon Layer Implementation
 */

/**
 * @namespace
 * @ignore
 * Ensure namespace exists
 */
aperture = (
/** @private */
function(namespace) {

	var defaults = {
			'x' : 0,
			'y' : 0,
			'width' : 24,
			'height' : 24
		},
		ontoDefaults = {
			'ontology' : 'aperture-hscb',
			'type' : 'undefined',
			'attributes' : {},
			'format' : undefined
		};

	// assumes pre-existence of layer.
	namespace.IconLayer = aperture.Layer.extend( 'aperture.IconLayer',

		/** @lends aperture.IconLayer# */
		{
			/**
			 * @class Represents a layer of point located icons representing ontological
			 * types with attributes. Icons may vary in size.<br><br>
			 *
			 * In addition to core {@link aperture.Layer Layer} properties, icon layer properties include all icon 
			 * <a href='aperture.palette.html#.icon'>palette</a> properties, and the following:
			 * 
			 * @mapping {String} url
			 *   The url of the icon to use. This optional property is provided for situations when a
			 *   specific image is desired, outside of the ontological resolution of types to symbols.
			 *   
			 * @mapping {Number} anchor-x
			 *   The x-anchor point in the range [0,1] for the icon.
			 * 
			 * @mapping {Number} anchor-x
			 *   The y-anchor point in the range [0,1] for the icon.
			 *      
			 * @mapping {Number} icon-count
			 *   The number of icons to be drawn.
			 * 
			 * @constructs
			 * @factoryMade
			 * @extends aperture.Layer
			 * @requires a vector canvas
			 */
			init : function( spec, mappings ) {
				aperture.Layer.prototype.init.call(this, spec, mappings );
			},

			// type flag
			canvasType : aperture.canvas.VECTOR_CANVAS,

			/*
			 * Render implementation
			 */
			render : function( changeSet ) {

				// FOR NOW - process all changes INEFFICIENTLY as total rebuilds.
				var toProcess = changeSet.updates,
					nIcons = toProcess.length, i;

				// Handle adds
				for( i=nIcons-1; i>=0; i-- ) {
					var node = toProcess[i],
						data = node.data,
						gfx = node.graphics,
						w = node.width, 
						h = node.height,
						icons = node.userData.icons || (node.userData.icons = []),
						index;

					var numIcons = this.valueFor('icon-count', data, 1);
					var visiblePoints = 0;
					for (index = 0; index < numIcons; index++){
						rattrs = this.valuesFor(defaults, data, [index]);

						// either a hard-coded url or use the palette to resolve it
						rattrs.x = rattrs.x * w + node.position[0] - this.valueFor('anchor-x', data, 0.5, index) * rattrs.width;
						rattrs.y = rattrs.y * h + node.position[1] - this.valueFor('anchor-y', data, 0.5, index) * rattrs.height;
						rattrs.src = this.valueFor('url', data, '', index);
						if (!rattrs.src) {
							var oattrs = this.valuesFor(ontoDefaults, data);
							
							if (oattrs.format !== 'svg') {
								oattrs.width = rattrs.width;
								oattrs.height = rattrs.height;
							}
							rattrs.src = aperture.palette.icon(oattrs);
						}

						// culling
						if (rattrs.x > w || rattrs.x + rattrs.width < 0 ||
								rattrs.y > h || rattrs.y + rattrs.height < 0) {
							// Only draw points that are within the bounds of the current node.
							// TODO: this will repurpose existing but changes the visual to item match.
							continue;
						} else {
							var visual = icons[visiblePoints];

							// PROCESS GRAPHICS.
							if (visual) {
								gfx.update(visual, rattrs, changeSet.transition);
							} else {
								visual = gfx.image(
										rattrs.src,
										rattrs.x,
										rattrs.y,
										rattrs.width,
										rattrs.height);

								gfx.apparate(visual, changeSet.transition);
								icons.push(visual);								
							}

							gfx.data( visual, data );
							visiblePoints++;
						}
					}
					// Remove any obsolete visuals.
					if (icons.length > visiblePoints){
						gfx.removeAll(icons.splice(visiblePoints));
					}		
				}
			}
		}
	);

	return namespace;

}(aperture || {}));

/**
 * Source: LabelLayer.js
 * Copyright (c) 2014 Oculus Info Inc.
 * @fileOverview Aperture Text Layer
 */

aperture = (
/** @private */
function(namespace) {

	// predefined orientations.
	var orientations = {
		horizontal : 0,
		vertical: -90
	}, isString = aperture.util.isString;
	
	namespace.LabelLayer = namespace.Layer.extend( 'aperture.LabelLayer',
	/** @lends aperture.LabelLayer# */
	{
		/**
		 * @augments aperture.Layer
		 * @requires a vector canvas
		 * @class Creates a layer displaying text at specific locations.

		 * @mapping {Number=1} label-count
		 *   The number of labels to be drawn.

		 * @mapping {Number=1} label-visible
		 *   The visibility of a label.

		 * @mapping {String} text
		 *   The text to be displayed.

		 * @mapping {String='black'} fill
		 *   The color of a label.

		 * @mapping {Number=0} x
		 *   The horizontal position at which the label will be anchored.

		 * @mapping {Number=0} y
		 *   The vertical position at which the label will be anchored.

		 * @mapping {Number=0} offset-x
		 *   The offset along the x-axis by which to shift the text after it has been positioned at (x,y).

		 * @mapping {Number=0} offset-y
		 *   The offset along the y-axis by which to shift the text after it has been positioned at (x,y).

		 * @mapping {'middle'|'start'|'end'} text-anchor
		 *   How the label is aligned with respect to its x position.

		 * @mapping {'middle'|'top'|'bottom'} text-anchor-y
		 *   How the label is aligned with respect to its y position.

		 * @mapping {'horizontal'|'vertical'| Number} orientation
		 *   The orientation of the text as a counter-clockwise angle of rotation, or constants 'vertical'
		 *   or 'horizontal'.

		 * @mapping {String='Arial'} font-family
		 *   One or more comma separated named font families,
		 *   starting with the ideal font to be used if present.

		 * @mapping {Number=10} font-size
		 *   The font size (in pixels).

		 * @mapping {String='normal'} font-weight
		 *   The font weight as a valid CSS value.

		 * @mapping {String='none'} font-outline
		 *   The colour of the outline drawn around each character of text. 
		 *   
		 * @mapping {Number=3} font-outline-width
		 *   The width of the outline drawn around each character of text, if font-outline is not none.
		 *   
		 * @constructs
		 * @factoryMade
		 */
		init : function(spec, mappings){
			aperture.Layer.prototype.init.call(this, spec, mappings);
		},

		canvasType : aperture.canvas.VECTOR_CANVAS,

		render : function(changeSet) {
			var node, i, n, g, labels;

			// Create a list of all additions and changes.
			var toProcess = changeSet.updates;

			for (i=0; i < toProcess.length; i++){
				node = toProcess[i];

				labels = node.userData.labels = node.userData.labels || [];

				// Get the number of labels to be rendered.
				var index, itemCount= this.valueFor('label-count', node.data, 1);
				g = node.graphics;
				
				// remove any extraneous labels
				for (index=itemCount; index < labels.count; index++){
					g.remove(labels[index].back);
					g.remove(labels[index].front);
				}
				
				labels.length = itemCount;
				
				for (index=0; index < itemCount; index++) {
					var visible = !!this.valueFor('label-visible', node.data, true, index);
					var label = labels[index];
					
					if (!visible){
						if (label) {
							g.remove(label.back);
							g.remove(label.front);
							labels[index] = null;
						}
						// Since all the labels are re-rendered on each update, there is
						// nothing more to do if the label is not visible.
						continue;
					}

					// Make the outline and fill colour the same.
					var fillColor = this.valueFor('fill', node.data, '#000000', index);
					var outlineColor = this.valueFor('font-outline', node.data, 'none', index);
					var xPoint = (this.valueFor('x', node.data, 0, index) * node.width) + (node.position[0]||0);
					var yPoint = (this.valueFor('y', node.data, 0, index) * node.height) + (node.position[1]||0);
					var outlineWidth = outlineColor !== 'none' && this.valueFor('font-outline-width', node.data, 3, index);

					var connect = this.valueFor('connect', node.data, false, index);

					var str = this.valueFor('text', node.data, '', index);

					var fontFamily = this.valueFor('font-family', node.data, "Arial", index);
					var fontSize = this.valueFor('font-size', node.data, 10, index);
					var fontWeight = this.valueFor('font-weight', node.data, "normal", index);

					var moreLines = str.match(/\n/g),
						textHeight = fontSize *1.4 + fontSize* (moreLines? moreLines.length: 0);

					// Check to see if there are any transformations that need to be applied.
					// The expected format is a string following Raphael's convention for
					// defining transforms on an element.
					var transform = '';
					var rotate = this.valueFor('orientation', node.data, null, index);
					if (isString(rotate)) {
						rotate = orientations[rotate] || rotate;
					}
					if (rotate) {
						transform += 'r'+rotate;
					}

					var offsetX = this.valueFor('offset-x', node.data, 0, index);
					var offsetY = this.valueFor('offset-y', node.data, 0, index);
					var textAnchor = this.valueFor('text-anchor', node.data, 'middle', index);
					var vAlign  = this.valueFor('text-anchor-y', node.data, 'middle', index);

					// convert to a number
					vAlign = vAlign !== 'middle'? 0.5*textHeight * (vAlign === 'top'? 1: -1): 0;
					
					// If there are already elements in this transformation, add
					// a delimiter.
					if (transform){
						transform += ',t0,'+ vAlign;
					} else {
						offsetY += vAlign;
					}
					xPoint+= offsetX;
					yPoint+= offsetY;

					var attr = {
							'x': xPoint,
							'y': yPoint,
							'text': str,
							'stroke': 'none',
							'font-family': fontFamily,
							'font-size': fontSize,
							'font-weight': fontWeight,
							'text-anchor': textAnchor,
							'transform': transform
							};
					var fattr;

					if (!label) {
						label = labels[index] = {};
					}
					
					// if outlined we create geometry behind the main text.
					if (outlineWidth) {
						fattr = aperture.util.extend({
							'fill': fillColor
						}, attr);
						
						attr['stroke-width']= outlineWidth;
						attr['stroke']= outlineColor;
						attr['stroke-linecap']= 'round';
						attr['stroke-linejoin']= 'round';
					} else {
						if (label.front) {
							g.remove(label.front);
							label.front = null;
						}
						attr['stroke']= 'none';
						attr['fill']= fillColor;
					}
					
					index = [index];
					
					// always deal with the back one first.
					if (!label.back) {
						label.back = g.text(xPoint, yPoint, str);
						g.data(label.back, node.data, index);
						g.update(label.back, attr);
						g.apparate(label.back, changeSet.transition);
					} else {
						g.update(label.back, attr, changeSet.transition);
					}
					
					if (connect) {
						var connectX = this.valueFor('connect-x', node.data, 0, index);
						var connectY = this.valueFor('connect-y', node.data, 0, index);
						var pathStr = 'M'+(xPoint-offsetX+connectX)+' '+(yPoint-offsetY+connectY)+'L'+xPoint+' '+yPoint;
						if (!label.path) {
							label.path = g.path(pathStr);
						} else {
							var pathattr = {path:pathStr};
							g.update(label.path, pathattr, changeSet.transition);
						}
					} else {
						if (label.path) {
							g.remove(label.path);
						}
					}
					
					// then the front.
					if (outlineWidth) {
						if (!label.front) {
							label.front = g.text(xPoint, yPoint, str);
							
							g.data(label.front, node.data, index);
							g.update(label.front, fattr);
							g.apparate(label.front, changeSet.transition);
						} else {
							g.update(label.front, fattr, changeSet.transition);
						}
					}
				}
			}
		}
	});

	return namespace;

}(aperture || {}));
/**
 * Source: LinkLayer.js
 * Copyright (c) 2014 Oculus Info Inc.
 * @fileOverview Aperture Link Layer Implementation
 */

/**
 * @namespace
 * @ignore
 * Ensure namespace exists
 */
aperture = (
/** @private */
function(namespace) {

	// bow angle is based on the 2/4 control point ratio used below. cached for speed.
	var bowang = Math.atan(0.5),
		bowsin = Math.sin(bowang),
		bowcos = Math.cos(bowang);
	
	/**
	 * Given a link spec, calculate the links two endpoints, accounting
	 * for any offsets.
	 */
	function linkPath(linkSpec, linkStyle) {
		// Create a path connecting the source and target points.
		var sx = linkSpec.sx, sy = linkSpec.sy,
			tx = linkSpec.tx, ty = linkSpec.ty,
			dx = tx - sx, dy = ty - sy,
			len = Math.sqrt(dx*dx + dy*dy);

		if (len) {
			
			var sr = linkSpec.sr / len,
				tr = linkSpec.tr / len;

			// distance is long enough to draw a link?
			if (sr + tr < 1) {
				// offset vectors
				var srX = dx * sr,
					srY = dy * sr,
					trX =-dx * tr,
					trY =-dy * tr;

				// rotate offsets?
				if (linkStyle === 'arc') {
					sx += srX*bowcos + srY*bowsin;
					sy +=-srX*bowsin + srY*bowcos;
					
					tx += trX*bowcos - trY*bowsin;
					ty += trX*bowsin + trY*bowcos;
					
					var c1 = (sx + tx)/2 + (ty - sy)/4,
						c2 = (sy + ty)/2 + (sx - tx)/4;
					
					return 'M'+ sx + ',' + sy + 'Q' + c1 + ',' + c2 + ',' + tx + ',' + ty;
		
				} else {
					sx += srX;
					sy += srY;
					tx += trX;
					ty += trY;
					
					return 'M'+ sx + ',' + sy + 'L' + tx + ',' + ty;
				}
			}
		} 
		
		return '';
	}

	/**
	 * Processes some user constants, translating into dash array.
	 */
	function strokeStyle(attrs, style) {
		switch (style) {
		case 'none':
			attrs.opacity = 0;
		case '':
		case 'solid':
			return '';
		case 'dashed':
			return '- ';
		case 'dotted':
			return '. ';
		}
		
		return style;
	}
	
	// assumes pre-existence of layer.
	namespace.LinkLayer = aperture.Layer.extend( 'aperture.LinkLayer',

		/** @lends aperture.LinkLayer# */
		{
			/**
			 * @class A layer for rendering links between two layer nodes.
			 *
			 * @mapping {String='#aaa'} stroke
			 *  The color of the link.
			 * 
			 * @mapping {Number=1} stroke-width
			 *  The width of the link line.
			 * 
			 * @mapping {'solid'|'dotted'|'dashed'|'none'| String} stroke-style
			 *  The link line style as a predefined option or custom dot/dash/space pattern such as '--.-- '.
			 *  A 'none' value will result in the link not being drawn.
			 * 
			 * @mapping {'line'|'arc'} link-style
			 *  The type of line that should be used to draw the link, currently limited to
			 *  a straight line or clockwise arc of consistent degree.
			 * 
			 * @mapping {Boolean=true} visible
			 *  The visibility of a link.
			 * 
			 * @mapping {Number=1} opacity
			 *  The opacity of a link. Values for opacity are bound with the range [0,1], with 1 being opaque.
			 * 
			 * @mapping {Object} source
			 *  The source node data object representing the starting point of the link. The source node
			 *  data object is supplied for node mappings 'node-x', 'node-y', and 'source-offset' for
			 *  convenience of shared mappings.
			 * 
			 * @mapping {Number=0} source-offset
			 *  The distance from the source node position at which to begin the link. The source-offset
			 *  mapping is supplied the source node as a data object when evaluated.
			 * 
			 * @mapping {Object} target
			 *  The target node data object representing the ending point of the link. The target node
			 *  data object is supplied for node mappings 'node-x', 'node-y', and 'target-offset' for
			 *  convenience of shared mappings.
			 * 
			 * @mapping {Number=0} target-offset
			 *  The distance from the target node position at which to begin the link. The target-offset
			 *  mapping is supplied the target node as a data object when evaluated.
			 * 
			 * @mapping {Number} node-x
			 *  A node's horizontal position, evaluated for both source and target nodes.
			 * 
			 * @mapping {Number} node-y
			 *  A node's vertical position, evaluated for both source and target nodes.
			 * 
			 * @constructs
			 * @factoryMade
			 * @extends aperture.Layer
			 * @requires a vector canvas
			 *
			 */
			init : function( spec, mappings ) {
				aperture.Layer.prototype.init.call(this, spec, mappings );
			},

			// type flag
			canvasType : aperture.canvas.VECTOR_CANVAS,

			/*
			 * Render implementation
			 */
			render : function( changeSet ) {
				var i, 
					links = changeSet.updates, 
					n = links.length,
					transition = changeSet.transition;
				
				for (i=0; i<n; i++) {
					var link = links[i];
					var linkData   = link.data;
					var sourceData = this.valueFor('source', linkData, null);
					var targetData = this.valueFor('target', linkData, null);
					
					var endpoints = {
						'sx': this.valueFor('node-x', sourceData, 0, linkData),
						'sy': this.valueFor('node-y', sourceData, 0, linkData),
						'sr': this.valueFor('source-offset', sourceData, 0, linkData),
						'tx': this.valueFor('node-x', targetData, 0, linkData),
						'ty': this.valueFor('node-y', targetData, 0, linkData),
						'tr': this.valueFor('target-offset', targetData, 0, linkData)
					};
								
					// create a path.
					var path= linkPath(endpoints, this.valueFor('link-style', linkData, 'line'));

					var attrs = {
						'opacity': this.valueFor('opacity', linkData, 1),
						'stroke' : this.valueFor('stroke', linkData, 'link'),
						'stroke-width' : this.valueFor('stroke-width', linkData, 1)
					};
					
					// extra processing on stroke style
					attrs['stroke-dasharray'] = strokeStyle(attrs, this.valueFor('stroke-style', linkData, ''));

					// now render it.
					if (link.cache) {
						attrs.path = path;
						link.graphics.update(link.cache, attrs, transition);
						
					} else {
						link.cache = link.graphics.path(path).attr( attrs );
					}
				}
					
			}
		}
	);

	return namespace;

}(aperture || {}));

/**
 * Source: MapKey.js
 * Copyright (c) 2014 Oculus Info Inc.
 * @fileOverview Map Keys for mapping from one space (e.g. data) into another (e.g. visual)
 */

/**
 * @namespace
 * @ignore
 * Ensure namespace exists
 */
aperture = (
/** @private */
function(namespace) {

	namespace.MapKey = namespace.Class.extend( 'aperture.MapKey',

		/** @lends aperture.MapKey# */
		{
			/**
			 * @class A MapKey object maps from a Range object, representing a variable in
			 * data, to a color or numeric visual property such as a size or coordinate.
			 * MapKey is abstract. Instances are constructed by calling 
			 * {@link aperture.Range range.mapKey()}, and are used by {@link aperture.Mapping mappings}.
			 *
			 * @constructs
			 * @factoryMade
			 * @extends aperture.Class
			 */
			init : function(from, to) {
				this.fromRange = from;
				this.toArray = to;
			},

			/**
			 * A label for this map key reflecting the data property
			 * being mapped in readable form. This value is initialized
			 * from the label in the range but may be subsequently changed here.
			 *
			 * @param {String} value If a parameter given, acts as a setter and sets the label.
			 * @returns {String} If no parameter given, returns the MapKey's label. Otherwise sets and returns the label.
			 */
			label : function ( value ) {
				if (arguments.length) {
					// Set the value
					this.label = value;
				}
				return this.label;
			},

			/**
			 * Returns the Range object that this maps from.
			 *
			 * @returns {aperture.Range}
			 */
			from : function () {
				return this.fromRange;
			},

			/**
			 * Returns the set of values that this maps to.
			 *
			 * @returns {Array}
			 */
			to : function () {
				return this.toArray;
			}

			/**
			 * Returns a visual property value mapped from a data value.
			 * This method is abstract and implemented by different types of map keys.
			 *
			 * @name map
			 * @methodOf aperture.MapKey.prototype
			 *
			 * @param dataValue
			 *      the value to be mapped using the key.
			 *
			 * @returns
			 *      the result of the mapping.
			 */

			/**
			 * This method is mostly relevant to scalar mappings,
			 * where it can be used to set a non-linear mapping
			 * function. A string can be passed indicating a standard
			 * non linear-function, or a custom function may be supplied.
			 * Standard types include
			 * <span class="fixedFont">'linear'</span> and
			 * <span class="fixedFont">'area'</span>, for area based
			 * visual properties (such as a circle's radius).
			 * <br><br>
			 * An ordinal map key returns a type of
			 * <span class="fixedFont">'ordinal'</span>.
			 *
			 * @name type
			 * @methodOf aperture.MapKey.prototype
			 *
			 * @param {String|Function} [type]
			 *      if setting the value, the type of mapping function which
			 *      will map the progression of 0 to 1 input values to 0 to 1
			 *      output values, or a custom function.
			 *
			 * @returns {this|Function}
			 *		if getting the mapping function, the type or custom function, else
			 *		if setting the function a reference to <span class="fixedFont">this</span> is
			 *		returned for convenience of chaining method calls.
			 */
		}
	);

	/**
	 * @private
	 * Predefined interpolators for each type
	 */
	var blenders = {
		'number' : function( v0, v1, weight1 ) {
			return v0 + weight1 * ( v1-v0 );
		},

		// objects must implement a blend function
		'object' : function( v0, v1, weight1 ) {
			return v0.blend( v1, weight1 );
		}
	},

	/**
	 * @private
	 * Default interpolation tweens
	 */
	toTypes = {

		// useful for any visual property that is area forming, like a circle's radius,
		// and where the data range is absolute.
		'area' : function ( value ) {
			return Math.sqrt( value );
		}
	};

	/**
	 * Implements mappings for scalar ranges.
	 * We privatize this from jsdoc to encourage direct
	 * construction from a scalar range (nothing else
	 * makes sense) and b/c there is nothing else to
	 * document here.
	 *
	 * @private
	 */
	namespace.ScalarMapKey = namespace.MapKey.extend( 'aperture.ScalarMapKey',
	{
		/**
		 * Constructor
		 * @private
		 */
		init : function( fromRange, toArray ) {
			namespace.MapKey.prototype.init.call( this, fromRange, toArray );

			this.blend = blenders[typeof toArray[0]];
			this.toType  = 'linear';
		},

		/**
		 * Implements the mapping function
		 * @private
		 */
		map : function( source ) {
			var mv = this.fromRange.map( source ),
				to = this.toArray;

			switch ( mv ) {
			case 0:
				// start
				return to[0];

			case 1:
				// end
				return to[to.length-1];

			default:
				// non-linear?
				if ( this.tween ) {
					mv = this.tween( mv, source );
				}

				// interpolate
				var i = Math.floor( mv *= to.length-1 );

				return this.blend( to[i], to[i+1], mv - i );
			}
		},

		/**
		 * A string can be passed indicating a standard
		 * non linear-function, or a custom function may be supplied.
		 * @private
		 * [Documented in MapKey]
		 */
		type : function ( type ) {
			if ( type === undefined ) {
				return this.toType;
			}

			if ( aperture.util.isFunction(type) ) {
				if (type(0) != 0 || type(1) != 1) {
					throw Error('map key type functions must map a progression from 0 to 1');
				}

				this.tween = type;
			} else if ( aperture.util.isString(type) ) {
				this.tween = toTypes[type];
			}

			this.toType = type;

			return this;
		}
	});

	/**
	 * Implements mappings for ordinal ranges.
	 * We privatize this from jsdoc to encourage direct
	 * construction from an ordinal range (nothing else
	 * makes sense) and b/c there is nothing else to
	 * document here.
	 *
	 * @private
	 */
	namespace.OrdinalMapKey = namespace.MapKey.extend( 'aperture.OrdinalMapKey',
	{
		/**
		 * Implements the mapping function
		 * @private
		 */
		map : function( source ) {
			// Map index to index, mod to be safe (to could be smaller than range)
			// Missing in range array leads to -1, array[-1] is undefined as desired
			return this.toArray[ this.fromRange.map(source) % this.toArray.length ];
		},

		/**
		 * For completeness.
		 * @private
		 */
		type : function ( type ) {
			if ( type === undefined ) {
				return 'ordinal';
			}

			return this;
		}
	});

	return namespace;

}(aperture || {}));

/**
 * Source: Mapping.js
 * Copyright (c) 2014 Oculus Info Inc.
 * @fileOverview Aperture Mappings are used to define supply pipelines for visual
 * properties of layers.
 */

/**
 * @namespace
 * @ignore
 * Ensure namespace exists
 */
aperture = (
/** @private */
function(namespace) {

	var util = aperture.util,
		forEach = util.forEach;


	namespace.Mapping = aperture.Class.extend( 'aperture.Mapping',
	/** @lends aperture.Mapping# */
	{
		/**
		 * @class A Mapping is responsible for mapping value(s) for a visual property
		 * as a constant ({@link #asValue}) or {@link #from} a data source,
		 * {@link #using} an optional map key. Layer Mappings are
		 * accessed and defined by calling {@link aperture.Layer#map layer.map}.
		 *
		 * @constructs
		 * @factoryMade
		 * @extends aperture.Class
		 */
		init : function( property ) {
			/**
			 * The visual property to which this mapping pertains
			 * @private
			 */
			this.property = property;

			/**
			 * @private
			 */
			this.filters = [];

			/**
			 * @private
			 */
			this.dataAccessor = undefined;

			/**
			 * @private
			 */
			this.transformation = undefined;
		},

		/**
		 * Specifies that this mapping should not inherit
		 * from parent mappings.
		 *
		 * @returns {aperture.Mapping}
		 *      this mapping object
		 */
		only : function () {
			if (!this.hasOwnProperty('filters')) {
				this.filters = [];
			}
			if (!this.hasOwnProperty('dataAccessor')) {
				this.dataAccessor = undefined;
			}
			if (!this.hasOwnProperty('transformation')) {
				this.transformation = undefined;
			}

			return this;
		},

		/**
		 * Maps the graphic property from a source of values from the data object.
		 * A visual property may be mapped using one or more of the following constructs:
		 * <ul>
		 * <li>Field: A visual property may be mapped to a given field in the data.</li>
		 * <li>Function: A visual property may be mapped to a function that will be called and provided
		 * the data item and expected to return a value for the property.</li>
		 * </ul>
		 *
		 * @example
		 * // Map x to a field in the data object called 'xCoord'
		 * layer.map('x').from('xCoord');
		 * 
		 * // Map label to the value returned by the given function
		 * layer.map('label').from( function() { return 'Name: ' + this.name; } );
		 * 
		 * // Map label to the value returned by the given data object's prototype function
		 * layer.map('label').from( MyDataType.prototype.getName );
		 * 
		 * // Map x to a sequence of values and count to a static value of 20
		 * layer.map('x').from('xCoord[]');
		 * 
		 * // Map y to a function and count to the length of the array field 'points'
		 * layer.map('y').from( function(data, index) { return points[index].y; } );
		 * layer.map('count').from('points.length');
		 *
		 * @param {String|Function} source
		 *      the source of the data to map the graphic property.  May be a function that
		 *      maps a given data object to the desired source data in the form
		 *      <code>function(dataObject)</code>, or may be a data object field name
		 *      in the form <code>'a.b.c'</code> where the data will be sourced from
		 *      <code>dataObject.a.b.c</code>.  The length of an array field may be mapped
		 *      using <code>'fieldName.length'</code>.
		 *
		 * @returns {aperture.Mapping}
		 *      this mapping object
		 */
		from : function( source ) {
			// Preprocess the source to determine if it's a function, field reference, or constant
			if( util.isFunction(source) ) {
				/**
				 * @private
				 * Given a function, use it as the mapping function straight up
				 */
				this.dataAccessor = source;

			} else if( util.isString(source) ) {
				// Validate that this is a valid looking field definition
				var fieldChain = source.match(jsIdentifierRegEx);
				// Is a field definition?
				if( fieldChain ) {
					// Yes, create an array of field names in chain
					// Remove . from field names.  Leave []s
					fieldChain = util.map( fieldChain, function(field) {
						// Remove dots
						if( field.charAt(field.length-1) === '.' ) {
							return field.slice(0,field.length-1);
						} else {
							return field;
						}
					});

					/**
					 * @private
					 * Create a function that dereferences the given data item down the
					 * calculated field chain
					 */
					this.dataAccessor = function() {
						// Make a clone since the array will be changed
						// TODO Hide this need to copy?
						var chain = fieldChain.slice();
						// Pass in array of arguments = array of indexes
						return findFieldChainValue.call( this, chain, Array.prototype.slice.call(arguments) );
					};

					// TODO A faster version of the above for a single field
				} else {
					// String, but not a valid js field identifier
					// TODO logging
					throw new Error('Invalid object field "'+source+'" used for mapping');
				}
			} else {
				// Not a function, not a field
				// TODO log
				throw new Error('Mapping may only be done from a field name or a function');
			}

			return this;
		},

		/**
		 * Maps this property to a constant value.  The value may be a string, number, boolean
		 * array, or object.  A mapping to a constant value is an alternative to mapping do
		 * data using {@link #from}.
		 *
		 * @param {Object} value
		 *      The value to bind to this property.
		 *
		 * @returns {aperture.Mapping}
		 *      this mapping object
		 */
		asValue : function( value ) {
			/**
			 * @private
			 * Is just a static value string
			 */
			this.dataAccessor = function() {
				return value;
			};

			return this;
		},

		/**
		 * Provides a codified representational key for mapping between source data and the graphic
		 * property via a MapKey object. A MapKey object encapsulates the function of mapping from
		 * data value to graphic representation and the information necessary to express that mapping
		 * visually in a legend. Map keys can be created from Range objects, which describe
		 * the data range for a variable.
		 *
		 * A map key may be combined with a constant, field, or function provided data value source,
		 * providing the mapping from a variable source to visual property value for each data item, subject
		 * to any final filtering.
		 *
		 * The map key object will be used to translate the data value to an appropriate value
		 * for the visual property.  For example, it may map a numeric data value to a color.
		 *
		 * Calling this function without an argument returns the current map key, if any.
		 * 
		 * @param {aperture.MapKey} mapKey
		 *      The map key object to use in mapping data values to graphic property values.
		 *      Passing in null removes any existing key, leaving the source value untransformed,
		 *      subject to any final filtering.
		 *
		 * @returns {aperture.Mapping|aperture.MapKey}
		 *      this mapping object if setting the value, else the map key if getting.
		 */
		using : function( mapKey ) {
			if ( mapKey === undefined ) {
				return this.transformation;
			}
			this.transformation = mapKey;

			return this;
		},

		/**
		 * Applies a filter to this visual property, or clears all filters if no filter is supplied.
		 * A filter is applied after a visual value
		 * is calculated using the values passed into {@link #from}, {@link #asValue}, and
		 * {@link #using}.  Filters can be used to alter the visual value, for example, making
		 * a color brighter or overriding the stroke with on certain conditions.  A filter is a
		 * function in the form:
		 *
		 * @example
		 * function( value, etc... ) {
		 *     // value:  the visual value to be modified by the filter
		 *     // etc:    other values (such as indexes) passed in by the renderer
		 *     // this:   the data item to which this value pertains
		 *
		 *     return modifiedValue;
		 * }
		 *
		 * @param {Function} filter
		 *      A filter function in the form specified above, or nothing / null if clearing.
		 */
		filter : function( filter ) {
			if( filter ) {
				// only add to our own set of filters.
				if (!this.hasOwnProperty('filters')) {
					this.filters = [filter];
				} else {
					this.filters.push( filter );
				}
			} else {
				// Clear
				this.filters = [];
			}

			return this;
		},

		/**
		 * Removes a pre-existing filter, leaving any other filters intact.
		 *
		 * @param {Function} filter
		 *   A filter function to find and remove.
		 */
		filterWithout : function ( filter ) {
			this.filters = util.without(this.filters, filter);
		},

		/**
		 * Retrieves the visual property value for the given dataItem and optional indices.
		 *
		 * @param {Object} dataItem
		 *   The data object to retrieve a value for, which will be the value of <code>this</code> 
		 *   if evaluation involves calling a {@link #from from} and / or {@link #filter filter}function. 
		 *
		 * @param {Array} [index] 
		 *   An optional array of indices
		 *
		 *
		 */
		valueFor : function( dataItem, index ) {
			var value;

			// Get value (if no accessor, undefined)
			if( this.dataAccessor ) {
				// Get value from function, provide all arguments after dataItem
				value = this.dataAccessor.apply( dataItem, index || [] );
			}

			// Transform
			if( this.transformation ) {
				// If have a mapper, call it
				value = this.transformation.map( value );
			}

			// Filter
			if( this.filters.length ) {
				var args = [value].concat(index);
				forEach( this.filters, function(filter) {
					// Apply the filter
					value = filter.apply(dataItem, args);
					// Update value in args for next filter
					args[0] = value;
				});
			}

			return value;
		}
	});

	return namespace;

}(aperture || {}));
/**
 * Source: NodeLayer.js
 * Copyright (c) 2014 Oculus Info Inc.
 * @fileOverview Aperture Node Layer
 */

aperture = (
/** @private */
function(namespace) {

	/**
	 * @exports NodeLayer as aperture.NodeLayer
	 */
	var NodeLayer = aperture.PlotLayer.extend( 'aperture.NodeLayer',
	/** @lends NodeLayer# */
	{
		/**
		 * @augments aperture.PlotLayer
		 * @class Layer that takes in x/y visual mappings and draws all child layer
		 * items at the specified x/y.  Also allows mapping of x and y anchor positions.
		 * Supports DOM and Vector child layers.  The following data mappings are understood:

		 * @mapping {Number} node-x
		 *   The x-coordinate at which to locate the child layer visuals.
		 * 
		 * @mapping {Number} node-y
		 *   The y-coordinate at which to locate the child layer visuals.
		 * 
		 * @mapping {Number} width
		 *   The declared width of the node, which may factor into layout.
		 * 
		 * @mapping {Number} height
		 *   The declared height of the node, which may factor into layout.
		 * 
		 * @constructs
		 * @factoryMade
		 */
		init : function(spec, mappings) {
			aperture.PlotLayer.prototype.init.call(this, spec, mappings);
		},

		canvasType : aperture.canvas.VECTOR_CANVAS,

		render : function(changeSet) {
			var that = this,
				x, y, xAnchor, yAnchor, width, height, item;
			
			// Treat adds and modifies the same - just need to update positions
			aperture.util.forEach(changeSet.updates, function( node ) {
				item = node.data;
				// Discover the mapped visual properties
				x = this.valueFor('node-x', item, 0);
				y = this.valueFor('node-y', item, 0);
				width = this.valueFor('width', item , 1);
				height = this.valueFor('height', item , 1);

				// Update the given node in place with these values
				node.position = [x,y];

				node.userData.id = item.id;
				
				// Update width/height (if it matters?)
				node.width = width;
				node.height = height;
			}, this);
			
			
			// will call renderChild for each child.
			aperture.PlotLayer.prototype.render.call( this, changeSet );
		}

	});

	namespace.NodeLayer = NodeLayer;

	/**
	 * @class NodeLink is a {@link aperture.NodeLayer NodeLayer}  vizlet, suitable for adding to the DOM.
	 * @augments aperture.NodeLayer
	 * @name aperture.NodeLink
	 *
	 * @constructor
	 * @param {String|Element} parent
	 *      A string specifying the id of the DOM element container for the vizlet or
	 *      a DOM element itself.
	 * @param {Object} [mappings]
	 *      An optional initial set of property mappings.
	 *
	 * @see aperture.NodeLayer
	 */
	namespace.NodeLink = aperture.vizlet.make( NodeLayer );

	return namespace;

}(aperture || {}));
/**
 * Source: NodeSet.js
 * Copyright (c) 2014 Oculus Info Inc.
 * @fileOverview Node Sets refer to sets or subsets of layer nodes.
 */

/**
 * @namespace
 * @ignore
 * Ensure namespace exists
 */
aperture = (
/** @private */
function(namespace) {

	// default id function
	function getId() {
		return this.id;
	}
	
	
	var util = aperture.util,
		isArray = util.isArray,
		isString = util.isString,
		isFunction = util.isFunction,
		forEach = util.forEach,
		NO_GRAPHICS = aperture.canvas.NO_GRAPHICS,
		NEVER = function() {
			return false;
		},
		NO_ITER = {next: function() {
		}};
		
		

		// iterator options returned by node set below.
		var LogicalAllIter = aperture.Class.extend('[private].LogicalAllIter', {
			
			init : function(first) {
				this._cur = {next: first};
			},
			next : function() {
				var c = this._cur;
				return c && (this._cur = c.next);
			}
			
		}), LogicalMatchIter = aperture.Class.extend('[private].LogicalMatchIter', {
			
			init : function(nodeSet, cache) {
				this._nodeSet = nodeSet;
				this._where = nodeSet._where;
				this._cur = {next: nodeSet._layer.nodes_};
				this._cache = cache? [] : null;
			},			
			next : function() {
				var h = this._cur;
				
				if (h) {
					var where = this._where,
						cache = this._cache;
					
					// find the next valid node.
					while ((h = h.next) && !where.call(h.data));

					if (cache) {
						if (h) {
							cache.push(h);
						}
						
						// if reached the last, cache the filtered set IN NODE SET for next iter.
						if (!h || !h.next) {
							this._nodeSet._cache = this._nodeSet._cache || cache;
							cache = null;
						}
					}
					
					return this._cur = h;
				}
			}
			
		}), ArrayIter = aperture.Class.extend('[private].ArrayIter', {
			
			init : function(array) {
				this._array = array;
				this._ix = 0;
			},
			next : function() {
				var a = this._array;

				if (a) {
					var i = this._ix, c = a[i++];

					// if reached the end, clear ref
					if ((this._ix = i) === a.length) {
						this._array = null;
					}
					
					return c;
				}
			}
			
		}), SingleIter = aperture.Class.extend('[private].SingleIter', {
			
			init : function(node) {
				this._cur = node;
			},
			next : function() {
				var n = this._cur;
				if (n) {
					this._cur = null;
					return n;
				}
			}
			
		}), MultiSetIter = aperture.Class.extend('[private].MultiSetIter', {
			
			init : function(sets, layer) {
				this._sets = sets;
				this._ix = 0;
				this._layer = layer;
				this._cur = sets[0].nodes(layer);
			},
			next : function() {
				var c = this._cur;
				
				if (c) {
					var n = c.next(), s;
					while (!n) {
						if ((s = this._sets[this._ix+=1]) == null) {
							break;
						}
						
						this._cur = c= s.nodes(this._layer);
						n = c.next();
					}
					return n;
				}
			}
			
		}), DataIter = aperture.Class.extend('[private].DataIter', {
			
			init : function(nodeIter) {
				this._nodes = nodeIter;
				this._cur = nodeIter.next();
			},
			next : function() {
				var c = this._cur;
				
				if (c) {
					this._cur = this._nodes.next();
					return c.data;
				}
			}
		
		}), MultiSet;
		
		
	function toFrontOrBack( planes, planeProperty, gfxFn ) {
			
		var layer = this._layer;
		
		if( !layer.hasLocalData ) { // will have no effect if no local data.
			return;
		}
		
		var c, i, j, n, p = planeProperty || 'plane';

		// if a sort function, do a heavyweight sort.
		if (util.isFunction(planes)) {
			
			var a = [];
			
			for (i = this.nodes(); (c = i.next()) != null;) {
				a.push({
					key: layer.valueFor(p, c.data, null),
					gfx: c.graphics
				});
			}

			a.sort(function(a, b) {
				return planes(a.key, b.key);
			});

			n = a.length;
			
			for (j = 0; j< n; ++j) {
				a[j].gfx[gfxFn]();
			}

		// else if anything, assume a set of planes and pull those to front in order.
		} else if (planes) {
			if (!util.isArray(planes)) {
				planes = [planes];
			}

			n = planes.length;
			
			for (j = 0; j< n; ++j) {
				for (i = this.nodes(); (c = i.next()) != null;) {
					if (c.graphics !== NO_GRAPHICS && planes[j] === layer.valueFor(p, c.data, null)) {
						c.graphics[gfxFn]();
					}
				}
			};

		// else simply order by node order.
		} else {
			for (i = this.nodes(); (c = i.next()) != null;) {
				c.graphics[gfxFn]();
			}
		}

		return this;
	}

	aperture.Layer.NodeSet = aperture.Class.extend( 'aperture.Layer.NodeSet',
	/** @lends aperture.Layer.NodeSet# */
	{
		/**
		 * @class Represents a set or subset of layer nodes, defined through subsequent
		 * calls to selector methods. NodeSet is abstract. A new node set is retrieved with a call
		 * to layer.nodes(), or is retrieved from an event.
		 * 
		 * @param {aperture.Layer} layer
		 *   the associated layer.
		 *   
		 * @constructs
		 * @factoryMade
		 * @extends aperture.Class
		 */
		init : function( layer ) {
			this._layer = layer;
			this._vizlets = [layer.vizlet()];
		},

		/**
		 * Applies a selection criteria on this node set where node data must pass a conditional test.
		 * 
		 * @param {Function|String} [test]
		 *   A test to be executed for each node's data. If a function is supplied it will be called 
		 *   for each node with this = data and the return value will be evaluated according to the
		 *   match criteria. If a string value is supplied the value of that data field name will be
		 *   evaluated instead. The test parameter may be excluded if the match parameter provides
		 *   a set of data objects to match against.
		 *   
		 * @param {Array|Object} [match]
		 *   Optionally one or more matches to evaluate the results of the test against, or if the test
		 *   is omitted, one or more data objects to match against. If match is omitted the test will
		 *   pass if it returns any 'truthy' value.
		 *   
		 * @example
		 *   // redraw data nodes with id C4501
		 *   layer.all().where('id', 'C4501').redraw();
		 *   
		 *   // redraw data nodes with id C4501, C4502
		 *   layer.all().where('id', ['C4501', 'C4502']).redraw();
		 *   
		 *   // redraw data nodes data0, data1
		 *   layer.all().where([data0, data1]).redraw();
		 *   
		 *   // redraw nodes which pass a filter function
		 *   function big(data) {
		 *      return data.size > 100000000;
		 *   }
		 *   layer.all().where(big).redraw();
		 *   
		 * @returns {this}
		 *   this set
		 */
		where : function ( test, match ) {
			this.revalidate();
			
			// PROCESS TEST
			// string test arg? a field name.
			if (isString(test)) {
				test = test === 'id'? getId : function() {
					return this[test];
				};
						
			// no test arg? shift args.
			} else if (!isFunction(test)) {
				if (test) {
					match = test;
					test = null;
				} else {
	 				this._where = NEVER;
					return this;
				}
			}

			
			// PROCESS MATCH.
			// no match? basic truthy test
			if (!match) {
				this._where = test;
				return this;

			// set of matches? match test results
			} else if (isArray(match)) {
				switch (match.length) {
				
				// unless no matches: shortcut to never
				case 0:
					this._where = NEVER;
					return this;
					
				// unless 1 match: shortcut to single match test defined later.
				case 1:
					match = match[0];
					break;
					
				default:
					if (test) {
						this._where = function() {
							var i, n = match.length,
								id = test.call(this);
							
							for (i=0; i< n; ++i) {
								if (match[i] === id) {
									return true;
								}
							}
						};
					} else {
						this._where = function() {
							var i, n = match.length;
							
							for (i=0; i< n; ++i) {
								if (match[i] === this) {
									return true;
								}
							}
						};
					}
					return this;
				}
			}

			// single match test.
			if (test) {
				this._where = function() {
					return match === test.call(this);
				};
			} else {
				this._where = function() {
					return match === this;
				};
			}
			
			return this;
		},
		
		/**
		 * Unions this node set with another and returns the result.
		 * 
		 * @returns {aperture.Layer.NodeSet} 
		 *   the union set of nodes
		 */
		and : function ( nodeSet ) {
			// TODO: hash it if haven't already, to exclude duplicates?
			return new MultiSet( [this, nodeSet] );
		},
		
//		/**
//		 * Returns the explicit set of parent nodes as a new set.
//		 * 
//		 * @returns {aperture.Layer.NodeSet} 
//		 *   the set of parent nodes
//		 */
//		parents : function ( ) {
//		},
		
		/**
		 * Returns true if the specified layer is included in this node set.
		 * 
		 * @returns {Boolean}
		 *   true if has this layer
		 */
		hasLayer : function ( layer ) {
			return layer === this._layer;
		},

		/**
		 * Returns a new data iterator for this node set. The iterator will be a simple object with
		 * a next() method that will return data for the next node in the set until there are no more to return.
		 * 
		 * @example
		 * var data,
		 *     iter = layer.all().data();
		 * 
		 * for (data = iter.next(); data != null; data = iter.next()) {
		 * 
		 * @returns {Object}
		 *   iterator object with method next()
		 */
		data : function( ) {
			return new DataIter( this.nodes() );
		},
		
		/**
		 * TODO
		 */
		inside : function( left, top, right, bottom ) {
			//this.revalidate();
		},
		
		/**
		 * Brings layer nodes successively to the front of their parent node(s), 
		 * using lighter or heavier weight techniques as desired. 
		 * Ordering at the layer level rather than in data is typically used for state 
		 * based changes like popping selected nodes to the top. Note that ordering nodes of a layer that 
		 * inherits its data from a parent layer has no effect, since there will be only one
		 * layer node per parent node.<br><br>
		 * 
		 * Usage examples:
		 *
		 * @example
		 * // bring any layer node with a 'plane' value of 'selected' to the front,
		 * // leaving others as they are.
		 * nodes.toFront( 'selected' );
		 *
		 * // bring any layer node with a 'selected' value of true to the front,
		 * // leaving unselected as they are.
		 * nodes.toFront( true, 'selected' );
		 *
		 * // all in a set to front by data order
		 * nodes.toFront( );
		 *
		 * // bring all 'unfiltered's to front, then all 'selected's above those
		 * nodes.toFront( ['unfiltered', 'selected'] );
		 *
		 * // call a sort function on the 'z-index' property value of layer nodes
		 * nodes.toFront( function(a,b) {return a-b;}, 'z-index' );
		 *
		 * @param {Array|Object|Function} [planes]
		 *      an array specifying a set of planes to
		 *      bring forward (in back to front order); or one such plane; or a function
		 *      to sort based on plane value. If planes is omitted all nodes are assumed
		 *      to be in the same plane and are sorted in the order in which they appear
		 *      in the data. See the examples for more information.
		 *
		 * @param {String} [planeProperty]
		 *      optionally, the name of the property that supplies the plane value for
		 *      layer nodes. If omitted it is assumed to be 'plane'.
		 *
		 * @returns {this}
		 *   this set
		 */
		toFront : function ( planes, planeProperty ) {
			return toFrontOrBack.call(this, planes, planeProperty, 'toFront');
		},

		/**
		 * Sends layer nodes successively to the back of their parent node(s), 
		 * using the same instruction api as {@link #toFront}.
		 */
		toBack : function ( planes, planeProperty ) {
			return toFrontOrBack.call(this, planes, planeProperty, 'toBack');
		},
		
		/**
		 * TODO
		 */
		layout : function( ) {
			
		},
		
		/**
		 * TODO
		 */
		remove : function( ) {
			
		},
		
		/**
		 * Invokes a visual layer update of the node set.
		 * 
		 * @param {aperture.Transition} [transition]
		 *   an optional animated transition to use to phase in the changes.
		 *
		 * @returns {this}
		 *   this set
		 */
		redraw : function ( transition ) {
			this._vizlets[0].redraw(this, transition);
			
			return this;
		},
		
		/**
		 * @private
		 * returns a private vizlet nodes for use in redraw
		 */
		vizlets : function() {
			return this._vizlets;
		},
		
		/**
		 * ${protected}
		 * Returns a new iterator for this node set, for all layers or the optionally specified layer.
		 * Returns null if the specified layer is not included. The iterator will be a simple object with
		 * a next() method that will return the next node in the set until there are no more to return.
		 * This method returns direct access to the nodes and is for framework use only.
		 * 
		 * @param {aperture.Layer} [layer]
		 *   optionally the layer to create an iterator for, relevant if a joined aggregate node set.
		 */
		nodes : function( layer ) {
		},

		/**
		 * ${protected}
		 * If this is a logical node set, invalidate any cacheing.
		 * 
		 * @returns {this}
		 *   this set
		 */
		revalidate : function() {
			if (this._cache) {
				this._cache = null;
			}
		}
		
	});

	
	
	var SnS = aperture.Layer.SingleNodeSet = aperture.Layer.NodeSet.extend( 'aperture.Layer.SingleNodeSet',
	/** @lends aperture.Layer.SingleNodeSet# */
	{
		/**
		 * ${protected}
		 * @class Represents a single constant node as a set.
		 * 
		 * @param {aperture.Layer.Node} node
		 *   
		 * @constructs
		 * @extends aperture.Layer.NodeSet
		 */
		init : function( node ) {
			aperture.Layer.NodeSet.prototype.init.call( this, node? node.layer : null );
			
			/**
			 * @private
			 */
			this._node = node;
		},
	
		/**
		 * @private
		 * override
		 */
//		parents : function ( ) {
//			var n = this._node;
//			
//			if (n && n.parent) {
//				return new SnS( n.parent );
//			}
//		},
		
		/**
		 * @private
		 * override
		 */
		nodes : function( layer ) {
			var where = this._where,
				node = this._node;
			
			return !node || !node.layer || (layer && layer !== this._layer) || (where && !where.call(node.data))? NO_ITER : 
				new SingleIter(node);
		}
		
	});

	
	aperture.Layer.LogicalNodeSet = aperture.Layer.NodeSet.extend( 'aperture.Layer.LogicalNodeSet',
	/** @lends aperture.Layer.LogicalNodeSet# */
	{
		/**
		 * ${protected}
		 * @class Represents a set defined logically but not evaluated until the point of iteration.
		 * 
		 * @param {aperture.Layer} layer
		 *   the associated layer.
		 *   
		 * @constructs
		 * @extends aperture.Layer.NodeSet
		 */
		init : function( layer ) {
			aperture.Layer.NodeSet.prototype.init.call( this, layer );
		},
		
		/**
		 * @private
		 * override
		 */
//		parents : function ( ) {
//			// TODO:
//		},
		
		/**
		 * @private
		 * override
		 */
		nodes : function( layer ) {
			if ((layer && layer !== this._layer) || this._where === NEVER) {
				return NO_ITER;
			}

			// if no filter, return everything.
			if (!this._where) {
				return new LogicalAllIter(this._layer.nodes_);
			}
			
			// cacheing filtered set.
			if (!this._cache) {
				return new LogicalMatchIter(this, true);
			}
			
			// iterate over cached.
			return new ArrayIter(this._cache);

		}

	});
	
	
	MultiSet = aperture.Layer.MultiSet = aperture.Layer.NodeSet.extend( 'aperture.Layer.MultiSet',
	/** @lends aperture.Layer.MultiSet# */
	{
		/**
		 * ${protected}
		 * @class Represents several sets as one.
		 * 
		 * @param {Array} sets
		 *   
		 * @constructs
		 * @extends aperture.Layer.NodeSet
		 */
		init : function(sets) {
			this._sets = sets;
			
			// populate vizlet list.
			var i, n = sets.length,
				j, v, setv, lenv, hash = {}, unique= this._vizlets = [];
			
			for (i=0; i<n; i++) {
				setv = sets[i].vizlets();
				lenv = setv.length;
				
				for (j=0; j<lenv; j++) {
					v = setv[j];
					if (!hash[v.uid]) {
						unique.push(hash[v.uid]= v);
					}
				}
			}
		},
	
		/**
		 * @private
		 * override
		 */
		nodes : function( layer ) {
			var i, sets = this._sets, n = sets.length, s, lsets;
			
			for (i=0; i<n; i++) {
				s = sets[i];

				if (!layer || s.hasLayer(layer)) {
					(lsets || (lsets= [])).push(s);
				}
			}
			
			return lsets? new MultiSetIter(lsets) : NO_ITER;
		},
		
		/**
		 * @private
		 * override
		 */
		hasLayer : function ( layer ) {
			var i, sets = this._sets, n = sets.length;
			
			for (i=0; i<n; i++) {
				if (sets[i].hasLayer(layer)) {
					return true;
				}
			}
			
			return false;
		},
		
		/**
		 * @private
		 * override
		 */
		where : NEVER,
		
		/**
		 * @private
		 * override
		 */
		toFront : function () {
			var i, sets = this._sets, n = sets.length;
			
			for (i=0; i<n; i++) {
				sets[i].toFront.apply( sets[i], arguments );
			}
			
			return this;
		},
		
		/**
		 * @private
		 * override
		 */
		toBack : function () {
			var i, sets = this._sets, n = sets.length;
			
			for (i=0; i<n; i++) {
				sets[i].toBack.apply( sets[i], arguments );
			}
			
			return this;
		},
		
		/**
		 * @private
		 * override
		 */
		redraw : function ( transition ) {
			var i, vizlets = this._vizlets, n = vizlets.length;
			
			for (i=0; i<n; i++) {
				vizlets[i].redraw(this, transition);
			}
			
			return this;
		},
		
		/**
		 * @private
		 * override
		 */
		revalidate : function() {
			var i, sets = this._sets, n = sets.length;
			
			for (i=0; i<n; i++) {
				sets[i].revalidate();
			}
		}
		
	});
	
			
	return namespace;

}(aperture || {}));
/**
 * Source: RadialLayer.js
 * Copyright (c) 2014 Oculus Info Inc.
 * @fileOverview The Radial Layer Implementation
 */

/**
 * @namespace
 * @ignore
 * Ensure namespace exists
 */
aperture = (
/** @private */
function(namespace) {

	// precalculate a few factors for speed.
	var degreesToRadians = Math.PI / 180,
		petalArcRadial = Math.SQRT2 - 1,
		petalTanFactor = 1 / Math.tan( degreesToRadians * 45 ),
		petalStemFactor = Math.SQRT1_2,

		/**
		 * @private
		 */
		rotateProto = function ( xy ) {
			var x= xy.x,
				y= xy.y;

			xy.x = x*this.cos - y*this.sin;
			xy.y = x*this.sin + y*this.cos;
		},

		/**
		 * @private
		 */
		noop = function () {
		},

		/**
		 * @private
		 * Creates a rotation element for efficient
		 * repeated calls to rotate.
		 */
		rotation = function ( angle ) {
			if (!angle) {
				return { angle: 0, rotate : noop };
			}
			var rad= degreesToRadians * angle;

			return {
				angle: angle,
				cos: Math.cos( rad ),
				sin: Math.sin( rad ),
				rotate : rotateProto
			};
		},

		/**
		 * @private
		 */
		arcFns = {

			/**
			 * @private
			 * Returns vertices and properties for a petal arc.
			 * Method signature is intentionally the same as for pies.
			 *
			 * @param spread
			 *  spread of the petal as an angle in degrees (will cap at 90)
			 *  
			 * @param length
			 *  the radial length
			 *  
			 * @param rotation
			 *  an optional rotation
			 */
			bloom : function ( length, spread, rotation ) {

					// y arc radial
				var ry = length * petalArcRadial,

					// x arc radial
					rx = ry * (spread < 90? Math.tan( degreesToRadians * 0.5 * spread )
											* petalTanFactor : 1),

					// proto point offsets
					px = rx * petalStemFactor,
					py = ry * petalStemFactor,

					// create the return object.
					arc = {
						rx : rx,
						ry : ry,
						rotation : rotation.angle,
						largeArcFlag : 1,

						// pre-rotation
						points : [{ x : -px, y : -py },
											{ x :  px, y : -py }]
					};

				// apply rotation
				rotation.rotate( arc.points[0] );
				rotation.rotate( arc.points[1] );

				return arc;
			},

			/**
			 * @private
			 * Returns vertices and properties for a wedge.
			 * Method signature is intentionally the same as for petals.
			 *
			 * @param spread
			 *  spread of the wedge as an angle
			 *  
			 * @param length
			 *  the radial length
			 *  
			 * @param rotation0
			 *  the rotation of the first arm of the sector
			 *  
			 * @param rotation1
			 *  the rotation of the second arm of the sector
			 */
			pie : function ( length, spread, rotation0, rotation1 ) {

				// create the return object.
				var arc = {
					rx : length,
					ry : length,
					rotation : 0,
					largeArcFlag : (spread > 180? 1 : 0),

					// start with identity points, then rotate them below.
					points : [{ x : 0, y : -length },
										{ x : 0, y : -length }]
				};

				// apply rotations
				rotation0.rotate( arc.points[0] );
				rotation1.rotate( arc.points[1] );

				return arc;
			}
		},

		/**
		 * @private
		 * Creates a forward or backward path from an arc definition.
		 *
		 * @param arc
		 *  the arc object as created by one of the arc functions (bloom, pie)
		 *
		 * @param prefix
		 *  the move to (M,m) or line to (L,l) prefix, depending on whether this is the beginning
		 *  or middle of a path.
		 *
		 * @param sweep-flag
		 *  the sweep flag value, 0 or 1.
		 */
		arcPath = function ( arc, prefix, sweepFlag ) {
			var i1 = sweepFlag,
				i0 = sweepFlag? 0:1;

			return prefix + ' '
				+ arc.points[i0].x + ','
				+ arc.points[i0].y + ' A'
				+ arc.rx + ','
				+ arc.ry + ' '
				+ arc.rotation + ' '
				+ arc.largeArcFlag + ','
				+ sweepFlag + ' '
				+ arc.points[i1].x + ','
				+ arc.points[i1].y;
		},

		/**
		 * @private
		 * Creates the path for a circle given the radius and the stroke direction
		 * @param radius
		 * @param sweep direction (alternate for inner vs. outer arcs)
		 */
		circlePath = function( radius, direction ) {
			var pt1 = -radius + ',0',
				pt2 = radius + ',0',
				radiusSpec = radius+','+radius;

			return 'M'+pt1+' A'+radiusSpec+' 0,0,'+direction+' '+
					pt2+' A'+radiusSpec+' 0,0,'+direction+' '+pt1+' z';
		},

		/**
		 * @private
		 * Series sorter.
		 */
		sortByRadialLength = function( a, b ) {
			return a.radius - b.radius;
		},

		none = 'none',

		// property defaults
		defaults = {
			'sector-count' : undefined,
			'start-angle' : 0,
			'form' : 'pie',
			'base-radius': 0,
			'outline': null,
			'outline-width': 3
		},
		seriesDefaults = {
			'radius' : 20,
			'fill' : none,
			'opacity' : 1,
			'stroke' : none,
			'stroke-width' : 1
		},


		// assumes pre-existence of layer.
		RadialLayer = aperture.Layer.extend( 'aperture.RadialLayer',

		/** @lends aperture.RadialLayer# */
		{
			/**
			 * @class
			 * Represents a layer of point located radial indicators.
			 * Radial layers are capable of representing simple circles, but may also
			 * be subdivided in the form of pies, donuts, or bloom indicators with discrete
			 * petals. They may also represent concentric series, which is particularly
			 * good at showing change or difference. A pie form with series creates
			 * a polar area diagram (also known as a coxcomb or rose), the most
			 * historically famous of which may be Florence Nightingale's visualization of
			 * mortality causes in the Crimean War.<br><br>
			 *
			 * If the sector-count property is mapped, each item in the first order data array
			 * represents a subdivision into discrete wedges or petals (sectors), whereas the second
			 * order represents concentric series for each. If left unmapped, the series is assumed
			 * to be the first order in the data.
			 * Series data will always be drawn concentrically from inside to outside with intersections
			 * removed, no matter what the order of size in the data.
			 * 
			 * @mapping {Number=0} x
			 *  The horizontal offset from the origin the layer will be drawn, as a value from 0 to 1. This value
			 *  is normalized against the width of the layer.
			 *  <i>Evaluated for each radial node.</i>
			 *
			 * @mapping {Number=0} y
			 *  The vertical offset from the origin the layer will be drawn, as a value from 0 to 1. This value
			 *  is normalized against the height of the layer.
			 *  <i>Evaluated for each radial node.</i>
			 *
			 * @mapping {'pie'|'bloom'} form
			 *  The form of layer elements. A
			 *  <span class="fixedFont">'pie'</span>
			 *  form can be used for pie, donut, or coxcomb indicators,
			 *  suitable for partitioned data, whereas a
			 *  <span class="fixedFont">'bloom'</span>
			 *  form can be used for discrete multi-variate data, similar to a radar chart.
			 *  If a single data element is provided, a circle is produced and this property
			 *  will have no effect.
			 *  <i>Evaluated for each radial node.</i>
			 *
			 * @mapping {Number=0} start-angle
			 *  The start angle of the first sector, in degrees. <i>Evaluated for each radial node.</i>
			 * 
			 * @mapping {Number=0} base-radius
			 *  The inner radius from which to start drawing. All other
			 *  radius values are relative to this value. This value is ignored for bloom forms.
			 *  <i>Evaluated for each radial node.</i>
			 * 
			 * @mapping {Number=1} sector-count
			 *  The number of sectors into which the layer element is subdivided. Note that for
			 *  convenience, if this value is left unmapped, the data will be assumed to NOT be
			 *  indexed by sectors, meaning that series may be indexed in data without having to
			 *  parent them with a single 'fake' sector.
			 *  <i>Evaluated for each radial node.</i>
			 * 
			 * @mapping {String='none'} outline
			 *  The color of an optional outline drawn behind the full perimeter of each layer element, separate from the stroke
			 *  properties of each segment.
			 *  <i>Evaluated for each radial node.</i>
			 * 
			 * @mapping {Number=3} outline-width
			 *  The width of the outline, if an outline is not 'none'.
			 *  <i>Evaluated for each radial node.</i>
			 * 
			 * @mapping {Number=1} outline-opacity
			 *  The opacity of the outline, if an outline is not 'none'.
			 *  <i>Evaluated for each radial node.</i>
			 * 
			 * @mapping {Number} sector-angle
			 *  The spread of the element, as an angle in degrees. If unset this value is
			 *  calculated automatically to be an equal fraction of 360 degrees for each sector.
			 *  <i>Evaluated for each sector of each radial node.</i>
			 * 
			 * @mapping {Number=1} series-count
			 *  The number of series for each sector.
			 *  <i>Evaluated for each sector of each radial node.</i>
			 * 
			 * @mapping {Number=20} radius
			 *  The radial length of the wedge or petal.
			 *  <i>Evaluated for each series of each sector of each radial node.</i>
			 * 
			 * @mapping {String='none'} fill
			 *  The fill color.
			 *  <i>Evaluated for each series of each sector of each radial node.</i>
			 *
			 * @mapping {String='none'} stroke
			 *  The outline stroke color.
			 *  <i>Evaluated for each series of each sector of each radial node.</i>
			 *
			 * @mapping {Number=1} stroke-width
			 *  The outline stroke width.
			 *  <i>Evaluated for each series of each sector of each radial node.</i>
			 * 
			 * @mapping {Number=1} opacity
			 *  The opacity as a value from 0 to 1.
			 *  <i>Evaluated for each series of each sector of each radial node.</i>
			 *
			 * @constructs
			 * @factoryMade
			 * @extends aperture.Layer
			 * @requires a vector canvas
			 */
			init : function( spec, mappings ) {
				aperture.Layer.prototype.init.call(this, spec, mappings );
			},

			// type flag
			canvasType : aperture.canvas.VECTOR_CANVAS,

			/**
			 * @private
			 * Render implementation
			 */
			render : function( changeSet ) {

				// FOR NOW - process all changes INEFFICIENTLY as total rebuilds.
				var toProcess = changeSet.updates,
					i, iSegment, iSeries, node, visuals, graphics,
					transition = changeSet.transition;

				// Handle adds
				for( i=toProcess.length-1; i>=0; i-- ) {
					node = toProcess[i];
					var data = node.data,
						p = this.valuesFor(defaults, data),
						numSegments = p['sector-count'],
						segmented = 0,
						rotation0 = rotation(p['start-angle']),
						innerRadius = p['base-radius'],
						outlineWidth = p['outline-width'],
						outline = outlineWidth && p.outline !== 'none' && p.outline,
						outlinePath = '',
						maxRadius = 0,
						arc = arcFns.pie,
						strokes = [],
						shapes = [],
						dimSkip = false,
						path,
						j;

					if (numSegments == undefined) {
						numSegments = 1;
						dimSkip = true;
					}
					// use a different arc function for blooms,
					// and we don't currently support the inner radius so reset it to zero.
					if (p.form === 'bloom') {
						innerRadius = 0;
						arc = arcFns.bloom;
					}

					var defSpread = 360 / numSegments;
					
					// For each radial, build
					for( iSegment = 0; iSegment < numSegments; iSegment++ ) {
						if (this.valueFor( 'sector-angle', data, defSpread, iSegment ) && ++segmented === 2) {
							break;
						}
					}
					
					segmented = segmented > 1;

					// For each radial, build
					for( iSegment = 0; iSegment < numSegments; iSegment++ ) {

						var numSeries = this.valueFor( 'series-count', data, 1, iSegment ),
							spread = this.valueFor( 'sector-angle', data, defSpread, iSegment ),
							rotation1 = rotation( rotation0.angle + spread ),
							innerArc = innerRadius,
							singleSeries = numSeries === 1,
							radialData = [],
							outerArc,
							outerPath,
							seriesIndex;

						if (!spread) {
							continue;
						}

						// Collect all series data for sorting
						for( iSeries = 0; iSeries < numSeries; iSeries++ ) {
							seriesIndex = dimSkip? [iSeries] : [iSegment, iSeries];
							radialData.push(this.valuesFor(seriesDefaults, data, seriesIndex));
						}

						// Sort by increasing radial length
						radialData.sort( sortByRadialLength );

						// start somewhere?
						if ( innerRadius && segmented ) {
							innerArc = arc( innerRadius, spread, rotation0, rotation1 );
						}

						// Iterate from inner to outer-most series
						for( iSeries = 0; iSeries < numSeries; iSeries++ ) {

							var radius = radialData[iSeries].radius,
								stroke = radialData[iSeries].stroke,
								strokeWidth = radialData[iSeries]['stroke-width'],
								fill   = radialData[iSeries].fill,
								outlineSeries = outline && iSeries == numSeries-1;

							// skip items with no radius.
							if ( radius <= 0 ) {
								continue;
							}

							maxRadius = Math.max( maxRadius, radius );

							// has radial segments?
							if( segmented ) {

								// Create radial petal
								outerArc = arc( innerRadius + radius, spread, rotation0, rotation1 );

								// form the arc to begin the path.
								path = outerPath = arcPath( outerArc, 'M', 1 );

								// append to the outline.
								if (outlineSeries) {
									if (outlinePath) {
										outlinePath += arcPath( outerArc, ' L', 1 );
									} else {
										outlinePath = outerPath;
									}
								}
								
								// the complete shape, tapered to point 0,0 (strokes use this as well)
								outerPath += ' L0,0 Z';

								if( innerArc ) {
									// outer arc plus inner arc reversed, then closed
									path += arcPath( innerArc, 'L', 0 ) + ' Z';

								} else {
									path = outerPath;
								}

							// else create a circle.
							} else {

								// outerArc is the outer radius
								outerArc = innerRadius + radius;

								// start with the outer circle.
								path = outerPath = circlePath( outerArc, 1 );

								// then if there is a cutout, add that.
								if( innerArc ) {

									// Create the inner path of the ring using the innerArc (radius)
									path += circlePath( innerArc, 0 );
								}
								
								// form the outline.
								if (outlineSeries) {
									outlinePath = outerPath;
								}
							}


							// add the filled part, if there is something visible here.
							if ( fill || (singleSeries && stroke) ) {
								shapes.push( {
									graphic: {
										'path': path,
										'fill': fill,
										'opacity': radialData[iSeries].opacity,
										'stroke-width': singleSeries? strokeWidth : 0,
										'stroke': singleSeries? stroke : none
									},
									series: iSeries,
									segment: iSegment
								});
							}

							// have to draw the stroke separately in all
							// multi-series cases because:
							// a) it needs to define the outer edge only and
							// b) it needs to sit on top.
							if ( !singleSeries && stroke !== none ) {
								strokes.push( {
									graphic: {
										// arc plus a tapered point to 0,0
										'path': outerPath,
										'fill': none,
										'opacity': 1,
										'stroke-width': strokeWidth,
										'stroke': stroke
									},
									series: iSeries,
									segment: iSegment
								});
							}

							// This one's outer becomes the next one's inner
							innerArc = outerArc;
						}

						// increment angle.
						rotation0 = rotation1;
					}

					// add the strokes in reverse order.
					for ( j = strokes.length; j-- > 0; ) {
						shapes.push(strokes[j]);
					}


					// NOW PROCESS ALL SHAPES INTO GRAPHICS.
					// There is one node per radial visual.
					var xCoord = node.position[0]+ (this.valueFor('x', node.data, 0))*node.width,
						yCoord = node.position[1]+ ((this.valueFor('y', node.data, 0)))*node.height,
						ud = node.userData;

					visuals = ud.visuals;
					graphics  = node.graphics;

					var nShapes = shapes.length,
						transform = 't' + xCoord + ',' + yCoord;

					// insert perimeter outline?
					if (outline) {
						
						// Create the inner path of the ring
						if (innerRadius) {
							outlinePath += ' ' + circlePath( innerRadius, 0 );
						}
						outlinePath += ' Z';
						
						var attrs = {
							'fill': none,
							'stroke' : outline,
							'stroke-width' : outlineWidth,
							'opacity' : this.valueFor('outline-opacity', node.data, 1),
							'transform' : transform
						};
						
						path = ud.outline;
						
						if (path) {
							attrs.path = outlinePath;
							graphics.update(path, attrs, transition);
						} else {
//								console.log(outlinePath);
							
							path = ud.outline = graphics.path(outlinePath);
							graphics.toBack(path);
							graphics.update(path, attrs);
						}
					} else if (ud.outline) {
						graphics.remove(ud.outline);
						ud.outline = null;
					}
						
					// get visuals, create storage if not already there
					if (!visuals) {
						visuals = ud.visuals = [];
					}

					// sync our set of path visuals.
					if (visuals.length > nShapes) {
						graphics.removeAll(visuals.splice(nShapes, visuals.length - nShapes));
					} else {
						while (visuals.length < nShapes) {
							path = graphics.path();
							visuals.push(path);
						}
					}

					// Turn it into graphics.
					for (j = 0; j < shapes.length; j++) {

						// add transform attribute as well.
						shapes[j].graphic.transform = transform;

						// apply all attributes.
						graphics.update(visuals[j], shapes[j].graphic, transition);

						// Set the data associated with this visual element
						graphics.data( visuals[j], data, [shapes[j].segment, shapes[j].series] );
					}
				}

			}
		}
	);

	// expose item
	namespace.RadialLayer = RadialLayer;

	return namespace;

}(aperture || {}));

/**
 * Source: Range.js
 * Copyright (c) 2014 Oculus Info Inc.
 * @fileOverview The Range implementation
 */

/**
 * @namespace
 * @ignore
 * Ensure namespace exists
 */
aperture = (
/** @private */
function(namespace) {

	// util is always defined by this point
	var util = aperture.util;

	// we use this to check for VALID numbers (NaN not allowed)
	function isValidNumber( value ) {
		return !isNaN( value );
	}

	namespace.Range = aperture.Class.extend( 'aperture.Range',

		/** @lends aperture.Range.prototype */
		{
			/**
			 * @class Represents an abstract model property range. Range is
			 * implemented for both scalar and ordinal properties by
			 * {@link aperture.Scalar Scalar} and
			 * {@link aperture.Ordinal Ordinal}.
			 * <p>
			 *
			 * @constructs
			 * @description
			 * This constructor is abstract and may not be called.
			 * @extends aperture.Class
			 *
			 * @param {String} name
			 *      the label of the property described.
			 *
			 * @returns {this}
			 *      a new Range
			 */
			init : function( name ) {

				// views may allow the user to override label.
				this.label = name;

				// default formatting.
				this.formatter_ = new namespace.Format();

			},

			/**
			 * Gets or sets the value of the name for this property.
			 * If this is a view, the base range will be left untouched.
			 * To delete a view's name and once again fallback to the base
			 * label set the view's name value to null.
			 *
			 * @param {String} [text]
			 *      the value, if setting it rather than getting it
			 *
			 * @returns {String|this}
			 *      the label of this property if a get, or this if a set.
			 */
			name : function( text ) {

				// get
				if ( text === undefined ) {
					return this.label;
				}
				// set
				if ( text === null ) {
					delete this.label;
				} else {
					this.label = text;
				}

				return this;
			},

			/**
			 * Expands the property range to encompass the value, if necessary.
			 * This method is abstract and implemented by specific types of ranges.
			 *
			 * @param {Array|Number|String} value
			 *      a case or set of cases to include in the property range.
			 *
			 * @returns {this}
			 *      a reference to this property.
			 *
			 * @name aperture.Range.prototype.expand
			 * @function
			 */

			/**
			 * Clears the property range, then optionally expands
			 * it with new values.
			 * This method is abstract and implemented by specific types of ranges.
			 *
			 * @param {Array|Number} [values]
			 *
			 * @returns {this}
			 *      a reference to this property.
			 *
			 * @name aperture.Range.prototype.reset
			 * @function
			 */

			/**
			 * Returns a new banded scalar view of this range. Banded views are used for
			 * axis articulation, and for scalars can also be used for quantizing values
			 * to labeled ordinal bands of values.
			 * Banded views are live, meaning subsequent range changes are allowed through either
			 * the view or its source.
			 * This method is abstract and implemented by specific types of ranges.
			 *
			 * @returns {aperture.Range}
			 *      a new scalar view of this Range.
			 *
			 * @name aperture.Range.prototype.banded
			 * @function
			 */

			/**
			 * Creates a key for mapping from this model range to a visual property
			 * range. This method is abstract and implemented by specific types
			 * of ranges.
			 *
			 * @param {Array} to
			 *      the ordered set of colors or numbers to map to, from this property's
			 *      range.
			 *
			 * @returns {aperture.MapKey}
			 *      a new map key.
			 *
			 * @name aperture.Range.prototype.mapKey
			 * @function
			 */

			/**
			 * Returns the value's position within the Range object.
			 * Ranges implement this function to map data values into the range.
			 * This method is abstract and implemented by specific types of ranges.
			 * @param value
			 *      the value to map within the Range
			 *
			 * @returns the mapped value.
			 *
			 * @name aperture.Range.prototype.map
			 * @function
			 */

			/**
			 * Retrieves the contents of this range as an array.  The content of the array
			 * depends on the type of range (e.g. Scalar, Ordinal, etc). Ordinals return
			 * the sum set of cases in the order added, whereas Scalars return a two element
			 * array of min, max or undefined if the range is yet unset.
			 * This method is abstract and implemented by specific types of ranges.
			 *
			 * @returns {Array} array with the contents of the range
			 */
			get : function() {

				if (!this.range.values || !this.range.values.length) {
					return this.range.values;
				}
				// if range has been revised but not view, refresh view now.
				if (this.revision !== this.range.revision) {
					this.revision = this.range.revision;
					this.view = this.doView();
				}

				return this.view;
			},

			/**
			 * Formats a value as a String using the current formatter.
			 *
			 * @param value
			 *      the value to format into a string
			 *
			 * @returns {String}
			 *      the formatted value.
			 */
			format : function ( value ) {
				return this.formatter_.format( value );
			},

			/**
			 * Gets or sets the current formatter as a function.
			 * The default formatter simply uses the JavaScript String function.
			 *
			 * @param {aperture.Format} [formatter]
			 *      if setting the formatter, a Format object which
			 *      will format values.
			 *
			 * @returns {aperture.Format|this}
			 *      if getting the formatter, it will be returned,
			 *      otherwise a reference to this,
			 *      convenient for chained method calls.
			 */
			formatter : function ( f ) {
				// get
				if ( f == null ) {
					return this.formatter_;
				}
				if ( !f.typeOf || !f.typeOf(namespace.Format) ) {
					throw new Error('Range formatter must be a Format object');
				}

				this.formatter_ = f;

				return this;
			},

			/**
			 * Returns a displayable string which
			 * includes the property label and extent of the property.
			 *
			 * @returns {String}
			 *      a string.
			 */
			toString : function ( ) {
				var range = this.get();

				return this.label + range? (' [' + range.toString() + ']') : '';
			}
		}
	);

	/**
	 * @private
	 * Increment revision so views have a quick dirty check option.
	 * Used by both scalars and ordinals, on themselves.
	 */
	var revise = function () {
		this.revision++;

		if (this.revision === Number.MAX_VALUE) {
			this.revision = 0;
		}
	},

	/**
	 * @private
	 * Throw an error for this case.
	 */
	noBandedViews = function () {
		throw new Error('Cannot create a scalar view of a banded scalar!');
	},

	/**
	 * @private
	 * The range factory function for scalars.
	 */
	range = (

		/**
		 * @private
		 */
		function() {

			/**
			 * @private
			 * Modify range
			 */
			var set = function ( min, max ) {

				var rv = this.values;

				if( rv ) {
					// Have an existing range, expand
					if( min < rv[0] ) {
						rv[0] = min;
						this.revise();
					}
					if( max > rv[1] ) {
						rv[1] = max;
						this.revise();
					}
				} else {
					// No range set yet, set with min/max
					this.values = [min, max];
					this.revise();
				}
			},

			/**
			 * @private
			 * Clear any existing values.
			 */
			reset = function() {
				this.values = null;
				this.revise();
			};

			/**
			 * @private
			 * Factory method.
			 */
			return function () {
				return {
					values : null,
					revision : 0,
					revise : revise,
					set : set,
					reset : reset
				};
			};
	}());


	namespace.Scalar = namespace.Range.extend( 'aperture.Scalar',

		/** @lends aperture.Scalar.prototype */
		{
			/**
			 * @class Represents a scalar model property range. Unlike
			 * in the case of Ordinals, Scalar property map keys
			 * use interpolation when mapping values to visual
			 * properties. If the desired visual mapping of a raw scalar value
			 * is ordinal rather than scalar (for instance a change value
			 * where positive is an 'up' color and negative is a 'down' color), call
			 * <span class="fixedFont">quantized</span> to derive an ordinal view of
			 * the Scalar.
			 * <p>
			 *
			 * @augments aperture.Range
			 * @constructs
			 * @description
			 * Constructs a new scalar range.
			 *
			 * @param {String} name
			 *      the name of the property described.
			 * @param {Array|Number|String|Date} [values]
			 *      an optional array of values (or a single value) with which to
			 *      populate the range. Equivalent to calling {@link #expand} after construction.
			 *
			 * @returns {this}
			 *      a new Scalar
			 */
			init : function( name, values ) {
				namespace.Range.prototype.init.call(this, name);

				// create a range object,
				// shareable and settable by both base and view
				this.range = range();

				// starting view revision is 0
				// this gets checked later against range revision
				this.revision = 0;

				// handle initial value expansion
				if( values != null ) {
					this.expand(values);
				}
			},

			/**
			 * Expands the property range to encompass the value, if necessary.
			 *
			 * @param {Array|Number|String|Date} value
			 *      a case or set of cases to include in the property range. Each must
			 *      be a Number, a String representation of a number, or a
			 *      Date.
			 *
			 * @returns {this}
			 *      a reference to this property.
			 */
			expand : function ( value ) {
				var min, max, rv = this.range.values;

				if( util.isArray(value) ) {
					// Ensure they're all valid numbers
					var numbers = util.filter(util.map(value,Number), isValidNumber);
					if (!numbers.length) {
						return this;
					}
					// Find the min/max
					min = Math.min.apply(Math,numbers);
					max = Math.max.apply(Math,numbers);
				} else {
					// A single value
					min = max = Number(value);
					if (isNaN(min)) {
						return this;
					}
				}

				this.range.set( min, max );

				return this;
			},

			/**
			 * Clears the property range, then optionally expands
			 * it with new values.
			 *
			 * @param {Array|Number|String|Date} [values]
			 *
			 * @returns {this}
			 *      a reference to this property.
			 */
			reset : function ( values ) {
				this.range.reset();

				if ( values != null ) {
					this.expand ( values );
				}

				return this;
			},

			/**
			 * Returns the value's normalized position within the Range
			 * object.  The return value will be in the range of [0,1].
			 *
			 * @param {Number} value
			 *      the value to normalize by the Range
			 *
			 * @return {Number} the normalized value of the input in the range [0,1]
			 */
			map : function( value ) {
				// call function in case extended.
				var d = this.get();

				// if anything is invalid (null or NaN(!==NaN)), return 0 to keep our clamped contract.
				if( !d || value == null || (value = Number(value)) !== value) {
					return 0;
				}

				// return limit or interpolate
				return value <= d[0]? 0
						: value >= d[1]? 1
								: (value-d[0]) / (d[1]-d[0]);
			},

			/**
			 * Creates and returns a key for mapping from this model range to a visual property
			 * range. Mappings are evaluated dynamically, meaning subsequent
			 * range changes are allowed. Multiple map keys may be generated from the
			 * same range object.
			 *
			 * @param {Array} to
			 *      the ordered set of colors or numbers to map to, from this property's
			 *      range.
			 *
			 * @returns {aperture.MapKey}
			 *      a new map key.
			 */
			mapKey : function ( to ) {

				// allow for array wrapping or not.
				if (arguments.length > 1) {
					to = Array.prototype.slice.call(arguments);
				}
				// diagnose problems early so they don't cascade later
				if ( to.length === 0 || (util.isNumber(to[0]) && isNaN(to[0])) || (!util.isNumber(to[0]) && !to[0].blend) ) {
					aperture.log.error('Mappings of Scalar ranges must map to numbers or objects with a blend function.');
					return;
				}

				return new namespace.ScalarMapKey( this, to );
			},

			/**
			 * @private
			 *
			 * Views override this to chain updates together. This will never
			 * be called if the range is empty / null. The default implementation
			 * returns a single copy of the source range which is subsequently transformed
			 * by downstream views, in place.
			 */
			doView : function() {
				return this.range.values.slice();
			}
		}
	);

	/**
	 * Returns a new scalar view of this range which is symmetric about zero.
	 * Views are dynamic, adapting to any subsequent changes
	 * in the base range.
	 *
	 * @returns {aperture.Scalar}
	 *      a new view of this Range.
	 *
	 * @name aperture.Scalar.prototype.symmetric
	 * @function
	 */
	namespace.Scalar.addView( 'symmetric',
		{
			init : function ( ) {
				this.revision = 0;
			},

			doView : function ( ) {

				// start by copying our upstream view.
				var v = this._base.doView();

				// then balance around zero
				if( Math.abs(v[0]) > Math.abs(v[1]) ) {
					v[1] = -v[0];
				} else {
					v[0] = -v[1];
				}

				// return value for downstream views.
				return v;
			}
		}
	);

	/**
	 * Returns a new scalar view which ranges from zero to the greatest absolute
	 * distance from zero and which maps the absolute magnitude of values.
	 * Views are dynamic, adapting to any subsequent changes
	 * in the base range.
	 *
	 * @returns {aperture.Scalar}
	 *      a new view of this Range.
	 *
	 * @name aperture.Scalar.prototype.absolute
	 * @function
	 */
	namespace.Scalar.addView( 'absolute',
		{
			init : function ( ) {
				this.revision = 0;
			},

			doView : function ( ) {

				// start by copying our upstream view.
				var v = this._base.doView();

				v[1] = Math.max ( Math.abs(v[0]), Math.abs(v[1]) );
				v[0] = 0;

				// return value for downstream views.
				return v;
			},

			// Override of map function for absolute cases.
			map : function ( value ) {

				// error check (note that math.abs below will take care of other invalid cases)
				if( value == null ) {
					return 0;
				}
				return this._base.map.call( this, Math.abs(value) );
			}
		}
	);

	// used in banding
	function roundStep( step ) {
		var round = Math.pow( 10, Math.floor( Math.log( step ) * Math.LOG10E ) );

		// round steps are considered 1, 2, or 5.
		step /= round;

		if (step <= 2) {
			step = 2;
		} else if (step <= 5) {
			step = 5;
		} else {
			step = 10;
		}

		return step * round;
	}

	/**
	 * Returns a new banded scalar view of this range based on the specification
	 * supplied. Bands are used for axis articulation, or
	 * for subsequently quantizing scalars into labeled ordinals
	 * (e.g. up / down, or good / bad) for visual mapping (e.g. up color, down color).
	 * A banded view returns multiple band object values for
	 * <span class="fixedFont">get()</span>, where each object has a
	 * <span class="fixedFont">min</span>,
	 * <span class="fixedFont">label</span>, and
	 * <span class="fixedFont">limit</span> property.
	 * <br><br>
	 * Banded views are live, meaning subsequent range changes are allowed. Multiple
	 * bands may be generated from the same range object for different visual
	 * applications. Scalar bands may be specified simply by supplying a desired
	 * approximate count, appropriate to the visual range available, or by specifying
	 * predefined labeled value bands based on the domain of the values, such
	 * as 'Very Good' or 'Very Poor'. Bounds are always evaluated by a
	 * minimum threshold condition and must be contiguous.
	 * <br><br>
	 * Banded or quantized views must be the last in the chain of views -
	 * other optional views such as logarithmic, absolute, or symmetric can be
	 * the source of a banded view but cannot be derived from one.For example:
	 *
	 * @example
	 *
	 * // default banded view
	 * myTimeRange.banded();
	 *
	 * // view with around five bands, or a little less
	 * myTimeRange.banded(5);
	 *
	 * // view with around five bands, and don't round the edges
	 * myTimeRange.banded(5, false);
	 *
	 * // or, view banded every thousand
	 * myTimeRange.banded({ span: 1000 });
	 *
	 * // or, view with these exact bands
	 * myTimeRange.banded([{min: 0}, {min: 500}]);
	 *
	 * // or, using shortcut for above.
	 * myTimeRange.banded([0, 500]);
	 *
	 * @param {Number|Object|Array} [bands=1(minimum)]
	 *      the approximate count of bands to create, OR a band specification object
	 *      containing a span field indicating the regular interval for bands, OR an
	 *      array of predefined bands supplied as objects with min and label properties,
	 *      in ascending order. If this value is not supplied one band will be created,
	 *      or two if the range extents span zero.
	 *
	 * @param {boolean} [roundTo=true]
	 *      whether or not to round the range extents to band edges
	 *
	 * @returns {aperture.Scalar}
	 *      a new view of this Range, with limitations on further view creation.
	 *
	 * @name aperture.Scalar.prototype.banded
	 * @function
	 */
	namespace.Scalar.addView( 'banded',
		{
			init : function ( bands, roundTo ) {
				this.revision = 0;

				// prevent derivation of more views. would be nice to support secondary
				// banding, but not supported yet.
				this.abs = this.log = this.symmetric = noBandedViews;
				this.bandSpec = {roundTo : this.bandSpec? false : roundTo === undefined? true : roundTo};

				// predefined bands? validate labels.
				if ( util.isNumber(bands) ) {
					if ( isNaN(bands) || bands < 1 ) {
						bands = 1;
					}
				} else if ( bands && bands.span && !isNaN(bands.span)) {
					this.bandSpec.autoBands = bands;
				} else if ( util.isArray(bands) ) {
					// don't continue as array if not valid...
					if ( bands.length < 1 ) {
						bands = 1;
					} else {
						var band, limit, i;

						// copy (hmm, but note this does not deep copy)
						this.bandSpec.bands = bands = bands.slice();

						// process in descending order.
						for ( i = bands.length; i-->0; ) {
							band = bands[i];

							// replace numbers with objects.
							if ( util.isNumber(band) ) {
								band = { min : band };
								bands.splice( i, 1, band );
							}

							// set limit for convenience if not set.
							if (band.limit === undefined && limit !== undefined) {
								band.limit = limit;
							}

							limit = band.min;
						}
					}

				} else {
					bands = 1;
				}

				// a number. generate them from the source data.
				if (!this.bandSpec.bands) {
					this.bandSpec.autoBands = bands;
				}
			},

			doView : function ( ) {

				var v = this._base.doView(),
					bands = this.bandSpec.bands;

				// second order bands
				if (!util.isNumber(v[0])) {
					v = this._base.extents;
				}
				if (this.bandSpec.autoBands) {

					// TODO: cover log case.

					// first get extents, forcing an update.
					// note end and start here may vary from the actual v[0] and v[1] values
					var spec = this.bandSpec.autoBands,
						start = v[0],
						end = v[1];

					// if zero range, handle problem case by bumping up the end of range by a tenth (or 1 if zero).
					if (end === start) {
						end = (end? end + 0.1* Math.abs(end) : 1);
					}

					// delegate to any specialized class/view, or handle standard cases here.
					if (this.doBands) {
						bands = this.doBands(start, end, spec);
					} else {
						bands = [];

						var step = spec.span;

						if (!step || step < 0) {
							// if range spans zero, want an increment to fall on zero,
							// so use the larger half to calculate the round step.
							if (end * start < 0) {
								// cannot properly create only one band if it spans zero.
								if (spec === 1) {
									spec = 2;
								}
								// use the greater absolute.
								if (end > -start) {
									spec *= end / (end-start);
									start = 0;

								} else {
									spec *= -start / (end-start);
									end = 0;
								}
							}

							step = roundStep((end - start) / spec);
						}

						var next = Math.floor( v[0] / step ) * step,
							min;

						// build the range.
						do {
							min = next;
							next += step;
							bands.push({
								min : min,
								limit : next
							});

						} while (next < v[1]);
					}
				} else {
					var first = 0, last = bands.length;

					while ( --last > 0 ) {
						if ( v[1] > bands[last].min ) {
							first = last+ 1;
							while ( --first > 0 ) {
								if ( v[0] >= bands[first].min ) {
									break;
								}
							}
							break;
						}
					}

					// take a copy of the active subset
					bands = bands.slice(first, last+1);
				}

				// if not rounded, replace any partial bands with unbounded bands,
				// signaling that the bottom should not be ticked.
				if ( !this.bandSpec.roundTo ) {
					// Only do this if there is more than 1 band, otherwise
					// both the band min and limit values will be unbounded
					// and there will not be a top or bottom tick.
					if ( v[0] !== bands[0].min && bands.length > 1) {
						bands[0] = {
							min : -Number.MAX_VALUE,
							limit : bands[0].limit,
							label : bands[0].label
						};
					}

					var e = bands.length - 1;

					if ( v[1] !== bands[e].limit ) {
						bands[e] = {
							min : bands[e].min,
							limit : Number.MAX_VALUE,
							label : bands[e].label
						};
					}

				} else {
					// else revise the extents for update below.
					if (bands[0].min != null) {
						v[0] = bands[0].min;
					}
					if (bands[bands.length-1].limit != null) {
						v[1] = bands[bands.length-1].limit;
					}
				}

				// store extents for mapping
				this.extents = v;

				return bands;
			},

			// override to use extents instead of the result of get.
			map : function( value ) {

				// call function to update if necessary.
				this.get();

				var d = this.extents;

				// if anything is invalid, return 0 to keep our clamped contract.
				if( !d || value == null || isNaN(value = Number(value)) ) {
					return 0;
				}
				// return limit or interpolate
				return value <= d[0]? 0
						: value >= d[1]? 1
								: (value-d[0]) / (d[1]-d[0]);
			}
		}
	);

	/**
	 * Returns a quantized ordinal view of a banded scalar view range.
	 * Quantized views map ordinally (and produce ordinal mappings)
	 * and format scalar values by returning the ordinal band they fall into.
	 *
	 * @returns {aperture.Scalar}
	 *      a new view of this Range, with ordinal mapping.
	 *
	 * @name aperture.Scalar.prototype.quantized
	 * @function
	 */
	namespace.Scalar.addView( 'quantized',
		{
			init : function ( ) {
				if ( !this.typeOf( namespace.Scalar.prototype.banded ) ) {
					throw new Error('Only banded scalars can be quantized.');
				}
				this.banded = noBandedViews;
				this.revision = 0;
			},

			// In our view implementation we add labels to a copy of the
			// banded set.
			doView : function ( ) {
				var src = this._base.doView();

				if (this.bandSpec.autoBands) {
					var label,
						band,
						bands = [],
						i = src.length;

					// process in descending order. make sure we have labels etc.
					while ( i-- > 0 ) {
						band = src[i];

						if (band.min !== -Math.MAX_VALUE) {
							label = view.format( band.min );

							if (band.limit !== Math.MAX_VALUE) {
								label += ' - ' + view.format( band.limit );
							} else {
								label += ' +';
							}

						} else {
							if (band.limit !== Math.MAX_VALUE) {
								label = '< ' + view.format( band.limit );
							} else {
								label = 'all';
							}
						}

						// push new def
						bands.push({
							min : band.min,
							limit : band.limit,
							label : label
						});
					}

					return bands;
				}

				return src;
			},

			// Implemented to create an ordinal mapping.
			mapKey : function ( to ) {

				// co-opt this method from ordinal
				return namespace.Ordinal.prototype.mapKey.call( this, to );
			},

			// Implemented to map a scalar value to an ordinal value by finding its band.
			map : function ( value ) {
				var v = this.get();

				// if anything is invalid, return 0 to keep our clamped contract. otherwise...
				if( v && value != null && !isNaN(value = Number(value)) ) {
					var i = v.length;

					while (i-- > 0) {
						if ( value >= v[i].min ) {
							return i;
						}
					}
				}

				return 0;
			},

			/**
			 * Implemented to return the band label for a value
			 */
			format : function ( value ) {
				return this.get()[this.map( value )].label;
			}
		}
	);

	/**
	 * Returns a new scalar view which maps the order of magnitude of source values.
	 * Log views are constructed with a <span class="fixedFont">zero</span>
	 * threshold specifying the absolute value under which values should be no longer
	 * be mapped logarithmically, even if in range. Specifying this value enables
	 * a range to safely approach or span zero and still map effectively.
	 * Log views can map negative or positive values and are
	 * dynamic, adapting to any subsequent changes in the base range.
	 *
	 * @param zero
	 *      the minimum absolute value above which to map logarithmically.
	 *      if not supplied this value will default to 0.1.
	 *
	 * @returns {aperture.Scalar}
	 *      a new view of this Range.
	 *
	 * @name aperture.Scalar.prototype.logarithmic
	 * @function
	 */
	namespace.Scalar.addView( 'logarithmic',
		{
			init : function ( zero ) {
				this.revision = 0;
				this.absMin = zero || 0.1;
			},

			doView : function ( ) {

				// start by copying our upstream view.
				var v = this._base.doView();

				// constraint the range boundaries based on the
				// log minimum configured.
				if ( v[0] < 0 ) {
					v[0] = Math.min( v[0], -this.absMin );
					v[1] = ( v[1] < 0 )?
						Math.min( v[1], -this.absMin ): // both neg
						Math.max( v[1],  this.absMin ); // spans zero
				} else {
					// both positive
					v[0] = Math.max( v[0], this.absMin );
					v[1] = Math.max( v[1], this.absMin );
				}

				// cache derived constants for fast map calculations.
				var log0 = Math.log(Math.abs( v[0] ))* Math.LOG10E;
				var log1 = Math.log(Math.abs( v[1] ))* Math.LOG10E;

				// find our abs log min and max - if spans, the zeroish value, else the smaller
				this.logMin = v[0]*v[1] < 0? Math.log(this.absMin)* Math.LOG10E : Math.min( log0, log1 );
				this.mappedLogMin = 0;
				this.oneOverLogRange = 0;

				// establish the range
				var logRange = log0 - this.logMin + log1 - this.logMin;

				if (logRange) {
					this.oneOverLogRange = 1 / logRange;

					// now find mapped closest-to-zero value (between 0 and 1)
					this.mappedLogMin = v[0] >= 0? 0: v[1] <= 0? 1:
						(log0 - this.logMin) * this.oneOverLogRange;
				}

				// return value for downstream views.
				return v;
			},

			// Override of map function for logarithmic cases.
			map : function ( value ) {

				// call base map impl, which also updates view if necessary.
				// handles simple edge cases, out of bounds, bad value check, etc.
				switch (this._base.map.call( this, value )) {
				case 0:
					return 0;
				case 1:
					return 1;
				}

				var absValue = Math.abs( value = Number(value) );

				// otherwise do a log mapping
				return this.mappedLogMin +

					// zero(ish)?
					( absValue <= this.absMin? 0 :
						// or - direction * mapped log value
						( value > 0? 1 : -1 ) *
							( Math.log( absValue )* Math.LOG10E - this.logMin ) * this.oneOverLogRange );
			}
		}
	);

	// time banding has specialized rules for rounding.
	// band options here are broken into hierarchical orders.
	var timeOrders = (function () {

		function roundY( date, base ) {
			date.setFullYear(Math.floor(date.getFullYear() / base) * base, 0,1);
			date.setHours(0,0,0,0);
		}
		function roundM( date, base ) {
			date.setMonth(Math.floor(date.getMonth() / base) * base, 1);
			date.setHours(0,0,0,0);
		}
		function roundW( date, base ) {
			date.setDate(date.getDate() - date.getDay());
			date.setHours(0,0,0,0);
		}
		function roundD( date, base ) {
			date.setDate(1 + Math.floor((date.getDate() - 1) / base) * base);
			date.setHours(0,0,0,0);
		}
		function roundF( date, base ) {
			this.setter.call(date, Math.floor(this.getter.call(date) / base) * base, 0,0,0);
		}
		function add( date, value ){
			this.setter.call(date, this.getter.call(date) + value);
		}

		// define using logical schema...
		var orders = [
				// above one year, normal scalar band rules apply
				{ field: 'FullYear', span: /*366 days*/316224e5, round: roundY, steps: [ 1 ] },
				{ field: 'Month', span: /*31 days*/26784e5, round: roundM, steps: [ 3, 1 ] },
				{ field: 'Date', span: 864e5, round: roundW, steps: [ 7 ] },
				{ field: 'Date', span: 864e5, round: roundD, steps: [ 1 ] },
				{ field: 'Hours', span:36e5, round: roundF, steps: [ 12, 6, 3, 1 ] },
				{ field: 'Minutes', span: 6e4, round: roundF, steps: [ 30, 15, 5, 1 ] },
				{ field: 'Seconds', span: 1e3, round: roundF, steps: [ 30, 15, 5, 1 ] },
				{ field: 'Milliseconds', span: 1, round: roundF, steps: [ 500, 250, 100, 50, 25, 10, 5, 1 ] }
				// below seconds, normal scalar band rules apply
		], timeOrders = [], last, dateProto = Date.prototype;

		// ...then flatten for convenience.
		util.forEach( orders, function( order ) {
			util.forEach( order.steps, function( step ) {
				timeOrders.push(last = {
					name   : order.field,
					span   : order.span * step,
					next   : last,
					base   : step,
					field  : {
						round  : order.round,
						add    : add,
						getter : dateProto['get' + order.field],
						setter : dateProto['set' + order.field]
					},
					format : new namespace.TimeFormat( order.field )
				});
			});
		});

		return timeOrders;
	}());

	// log warning if using unsupported view functions
	function noTimeView() {
		aperture.log.warn('Absolute, logarithmic or symmetric views are inappropriate for time scalars and are intentionally excluded.');
	}

	var fieldAliases = { 'Year' : 'FullYear', 'Day' : 'Date' };

	namespace.TimeScalar = namespace.Scalar.extend( 'aperture.TimeScalar',

		/** @lends aperture.TimeScalar.prototype */
		{
			/**
			 * @class Extends a scalar model property range with
			 * modest specialization of formatting and banding for
			 * JavaScript Dates. Dates are mappable by time by simple
			 * scalars as well, however this class is more appropriate
			 * for determining and labeling bands within a scalar range.
			 * When banded, default date formatting is used
			 * (for the purposes of axis labeling) unless explicitly
			 * overridden in the banded view.
			 * <p>
			 *
			 * @augments aperture.Scalar
			 * @constructs
			 * @description
			 * Constructs a new scalar time range.
			 *
			 * @param {String} name
			 *      the name of the property described.
			 * @param {Array|Number|String|Date} [values]
			 *      an optional array of values (or a single value) with which to
			 *      populate the range. Equivalent to calling {@link #expand} after construction.
			 *
			 * @returns {this}
			 *      a new Scalar
			 */
			init : function( name, values ) {
				namespace.Scalar.prototype.init.call(this, name, values);
				this.formatter_ = new namespace.TimeFormat();
			},

			/**
			 * Overrides the implementation in {@link aperture.Scalar Scalar}
			 * to expect a units field if the band specification object option
			 * is exercised. The units field in that case will be a
			 * string for the span, corresponding to the exact name of a
			 * common field in the Date class. For example:
			 *
			 * @example
			 * // band every three years
			 * myTimeRange.banded( {
			 *     span: 3,
			 *     units: 'FullYear',
			 * };
			 *
			 * @param {Number|Object|Array} [bands=1(minimum)]
			 *      the approximate count of bands to create, OR a band specification object
			 *      containing a span field indicating the regular interval for bands, OR an
			 *      array of predefined bands supplied as objects with min and label properties,
			 *      in ascending order. If this value is not supplied one band will be created,
			 *      or two if the range extents span zero.
			 *
			 * @param {boolean} [roundTo=true]
			 *      whether or not to round the range extents to band edges
			 *
			 * @returns {aperture.TimeScalar}
			 *      a new view of this Range, with limitations on further view creation.
			 *
			 */
			banded : function( bands, roundTo ) {
				var view = namespace.Scalar.prototype.banded.call(this, bands, roundTo);

				// update unless overridden.
				view.autoFormat = true;
				view.get(); // Force the view to populate all its properties.
				return view;
			},

			// unregister these view factories
			absolute : noTimeView,
			logarithmic : noTimeView,
			symmetric : noTimeView,

			// band specialization - only called by banded views.
			doBands : function(start, end, spec) {
				var order, base, i = 0;

				// is span predetermined?
				if (spec.span) {
					base = !isNaN(spec.span) && spec.span > 1? spec.span : 1;

					if (spec.units) {
						var units = fieldAliases[spec.units] || spec.units;

						// find appropriate order (excluding week, unless matched exactly)
						for (len = timeOrders.length; i < len; i++) {
							if (timeOrders[i].name === units) {
								if ((order = timeOrders[i]).base <= base
										&& (order.base !== 7 || base === 7) ) {
									break;
								}
							}
						}
					}
					if (!order) {
						aperture.log.error('Invalid units in band specification: ' + units);
						spec = 1;
						i = 0;
					}
				}
				if (!order) {
					var interval = Math.max(1, (end - start) / spec), len;

					// find minimum order.
					for (len = timeOrders.length; i < len; i++) {
						if ((order = timeOrders[i]).span <= interval) {
							break;
						}
					}

					// pick closer order of the min and next up
					if (order.next && order.next.span - interval < interval - order.span) {
						order = order.next;
					}

					// step in base units. in years? use multiple of base then.
					base = order.next? order.base : Math.max(1, roundStep( interval / 31536e6 )); // in years (/365 day yr)
				}

				// only auto update format if we haven't had it overridden.
				if (this.autoFormat) {
					this.formatter_ = order.format;
				}

				// round the start date
				var date = new Date(start), field = order.field, band, bands = [];
				field.round(date, base);

				// stepping function for bands, in milliseconds
				// (this arbitrary threshold limit kills any chance of an infinite loop, jic.)
				while (i++ < 1000) {
					var next = date.getTime();

					// last limit is this
					if (band) {
						band.limit = next;
					}

					// break once we're at or past the end
					if (next >= end) {
						break;
					}

					// create band (set limit next round)
					bands.push(band = {min: next});

					field.add(date, base);
				}

				return bands;
			}
		}
	);

	namespace.Ordinal = namespace.Range.extend( 'aperture.Ordinal',

		/** @lends aperture.Ordinal.prototype */
		{
			/**
			 * @class Represents an ordinal model property range. Unlike Scalar
			 * property mappings, which interpolate, Ordinals map ordered
			 * model cases to order visual property options: for instance
			 * series colors, or up / down indicators.
			 * <p>
			 *
			 * @augments aperture.Range
			 * @constructs
			 * @description
			 * Constructs a new ordinal range.
			 *
			 * @param {String} name
			 *      the name of the property described.
			 * @param {Array|Object} [values]
			 *      an optional array of ordinal values/cases with witch to populate
			 *      the object, or a single such value.
			 *
			 * @returns {this}
			 *      a new Ordinal
			 */
			init : function( name, values ) {
				namespace.Range.prototype.init.call(this, name);

				/**
				 * @private
				 * a record of the values (and their order) that we've seen so far
				 */
				this.range = {values : [], revision : 0, revise : revise};

				// starting view revision is 0
				// this gets checked later against range revision
				this.revision = 0;

				if( values != null ) {
					this.expand(values);
				}
			},

			/**
			 * Removes a property value from the set of ordinal cases.
			 *
			 * @param value
			 *      a case to remove from the property.
			 *
			 * @returns
			 *      the value removed, or null if not found.
			 */
			revoke : function ( value ) {
				if( util.isArray(value) ) {
					// Revoking an array of things
					var args = [this.range.values];
					this.range.values = util.without.apply(util, args.concat(value));
				} else {
					// Revoking a single thing
					this.range.values = util.without(this.range.values, value);
				}

				this.range.revise();

				return this;
			},

			/**
			 * Clears the property range, then optionally expands
			 * it with new values.
			 *
			 * @param {Array|Object} [values]
			 *      an optional array of ordinal values/cases with witch to repopulate
			 *      the object, or a single such value.
			 *
			 * @returns {this}
			 *      a reference to this property.
			 */
			reset : function ( values ) {
				this.range.values = [];
				this.range.revise();

				if ( values != null ) {
					this.expand ( values );
				}

				return this;
			},


			/**
			 * Expands the property range to encompass the value, if necessary.
			 *
			 * @param {Array|Object} value
			 *      a case or set of cases to include in the property range.
			 *
			 * @returns {this}
			 *      a reference to this property.
			 */
			expand : function ( value ) {
				var values = this.range.values,
					size = values.length,
					changed, i= 0, n;

				if ( util.isArray(value) ) {
					for (n= value.length; i< n; i++) {
						changed = ( util.indexOf(values, value[i]) === -1 && values.push(value[i])) || changed;
					}
				} else {
					changed = ( util.indexOf(values, value) === -1 && values.push(value));
				}

				if (changed) {
					this.range.revise();
				}
				return this;
			},

			/**
			 * Creates a key for mapping from this model range to a visual property
			 * range.
			 *
			 * @param {Array} to
			 *      the ordered set of colors or numbers to map to, from this property's
			 *      range.
			 *
			 * @returns {aperture.MapKey}
			 *      a new map key.
			 */
			mapKey : function ( to ) {

				// allow for array wrapping or not.
				if (arguments.length > 1) {
					to = Array.prototype.slice.call(arguments);
				}
				// diagnose problems early so they don't cascade later
				if ( to.length === 0 ) {
					return;
				}

				return new namespace.OrdinalMapKey( this, to );
			},

			/**
			 * Returns the mapped index of the specified value, adding
			 * it if it has not already been seen.
			 */
			map : function ( value ) {
				var values = this.range.values,
					i = util.indexOf( values, value );

				// add if have not yet seen.
				if (i < 0) {
					i = values.length;

					values.push( value );
				}

				return i;
			},

			/**
			 * Returns the index of the specified value, or -1 if not found.
			 */
			indexOf : function ( value ) {
				return util.indexOf( this.range.values, value );
			},

			/**
			 * @private
			 *
			 * Views override this to chain updates together. This will never
			 * be called if the range is empty / null. The default implementation
			 * returns the source range (not a copy) which is subsequently transformed
			 * by downstream views, in place.
			 */
			doView : function() {
				return this.range.values;
			}
		}
	);

	/**
	 * Returns a new banded scalar view of this range which maps to the normalized
	 * center of band. Banded views are used for axis articulation.
	 * Banded views are live, meaning subsequent range changes are allowed.
	 *
	 * @returns {aperture.Ordinal}
	 *      a new scalar view of this Range, with limitations on further view creation.
	 *
	 * @name aperture.Ordinal.prototype.banded
	 * @function
	 */
	namespace.Ordinal.addView( 'banded',
		{
			init : function ( view ) {
				this.revision = 0;
			},

			// implemented to return a banded version.
			doView : function ( ) {

				// start by copying our upstream view.
				var v = this._base.doView(),
					bands = [],
					i= v.length,
					limit = '';

				bands.length = i;

				while (i-- > 0) {
					bands[i]= {
						min: v[i],
						label: v[i].toString(),
						limit: limit
					};
					limit = v[i];
				}

				return bands;
			},

			// Implemented to create an ordinal mapping.
			mapKey : function ( to ) {

				// co-opt this method from scalar
				return namespace.Scalar.prototype.mapKey.call( this, to );
			},

			// Implemented to map an ordinal value to a scalar value.
			map : function ( value ) {

				var n = this.get().length;

				// normalize.
				return n === 0? 0: this._base.map( value ) / n;
			},

			// would be nice to support this for aggregated bins, but not right now.
			banded : function () {
				throw new Error('Cannot create a view of a banded ordinal!');
			}
		}
	);

	return namespace;

}(aperture || {}));
/**
 * Source: SankeyPathLayer.js
 * Copyright (c) 2014 Oculus Info Inc.
 * @fileOverview Aperture Link Layer Implementation
 */

/**
 * @namespace
 * @ignore
 * Ensure namespace exists
 */
aperture = (
/** @private */
function(namespace) {
	
	var _sankeyCache = {};
	/**
	 * Processes some user constants, translating into dash array.
	 */
	function strokeStyle(attrs, style) {
		switch (style) {
		case 'none':
			attrs.opacity = 0;
		case '':
		case 'solid':
			return '';
		case 'dashed':
			return '- ';
		case 'dotted':
			return '. ';
		}
		
		return style;
	}

	function removeSankeys(links){
		for (i=0; i<links.length; i++) {
			var link = links[i];
			var linkData   = link.data;
			if(_sankeyCache[linkData.id]){
				delete _sankeyCache[linkData.id];
			}
		}
	}
	function preProcessor(links){
		var sourceMap = {},
			targetMap = {};
		var n = links.length;
		
		for (i=0; i<n; i++) {
			var link = links[i];
			var linkData   = link.data;
			var sourceData = this.valueFor('source', linkData, null);
			var targetData = this.valueFor('target', linkData, null);
			var sankeyAnchor = this.valueFor('sankey-anchor', linkData, 'top');

			var flowSpec = {
				'source': {
					'id' : sourceData.id,
					'x' : this.valueFor('node-x', sourceData, 0, linkData),
					'y': this.valueFor('node-y', sourceData, 0, linkData) ,
					'r': this.valueFor('source-offset', sourceData, 0, linkData),
					'links' : sourceData.links
				},
				'target' : {
					'id' : targetData.id,
					'x': this.valueFor('node-x', targetData, 0, linkData),
					'y': this.valueFor('node-y', targetData, 0, linkData),
					'r': this.valueFor('target-offset', targetData, 0, linkData),
					'links' : targetData.links
				},
				'link' : link,
				'sankey-anchor' : sankeyAnchor
			};
			
			if (sourceMap[sourceData.id] == null){
				sourceMap[sourceData.id] = {
						'outflows':[]};
			}
			sourceMap[sourceData.id].outflows.push(flowSpec);
			
			if (targetMap[targetData.id] == null){
				targetMap[targetData.id] = {
						'inflows':[]};
			}
			targetMap[targetData.id].inflows.push(flowSpec);
		}
		// Order the source endpoints based on the target endpoints's y-position.
		var flows, key;
		for (key in sourceMap){
			if (sourceMap.hasOwnProperty(key)) {
				flows = sourceMap[key].outflows;
				flows.sort(function(a, b) {
					return a.target.y <= b.target.y? -1 : 1;
				});
			}
		}

		// Order the incoming flows of each target node by the flow's target y-position.
		for (key in targetMap){
			if (targetMap.hasOwnProperty(key)) {
				flows = targetMap[key].inflows;
				flows.sort(function(a, b) {
					return a.source.y <= b.source.y? -1 : 1;
				});
			}
		}		
		return {'sourceMap' : sourceMap, 'targetMap' : targetMap};
	}
	
	function getFlowOffset(flowSpec){
		// Find the matching link.
		var matchLink = null,
			target = flowSpec.target;

		for (var i=0; i < target.links.length; i++){
			if (target.links[i].id == flowSpec.link.data.id){
				matchLink = target.links[i];
				break;
			}
		}
		return Math.round(this.valueFor('stroke-width', matchLink, 0));		
	}
	
	function calcFlowPath(source, target){
		//TODO: Account for different flow styles and layout orientations.

		// Now calculate the control points for the curve.
		var midPt = {
				'x' : 0.5*(target.x + source.x),
				'y' : 0.5*(target.y + source.y)
		};
		
		var path = 'M' + source.x + ',' + source.y;
		// Calculate the control points.
		path += 'C' + midPt.x + ',' + source.y + ',' + midPt.x + ',' + target.y + ',' + target.x + ',' + target.y;
		
		return path;
	}

	function getStackedWidth(links, sankeyAnchor){
		var width = 0;
		if (sankeyAnchor == 'middle'){
			for (var i=0; i < links.length; i++){
				width += this.valueFor('stroke-width', links[i], 0);
			}
			return width;
		}
		// 'top' is the default anchor.
		return 0;
	}
	// assumes pre-existence of layer.
	namespace.SankeyPathLayer = aperture.Layer.extend( 'aperture.SankeyPathLayer',

		/** @lends aperture.SankeyPathLayer# */
		{
			/**
			 * @class A layer for rendering links between two layer nodes.
			 *
			 * @mapping {String='#aaa'} stroke
			 *  The color of the link.
			 * 
			 * @mapping {Number=1} stroke-width
			 *  The width of the link line.
			 * 
			 * @mapping {'solid'|'dotted'|'dashed'|'none'| String} stroke-style
			 *  The link line style as a predefined option or custom dot/dash/space pattern such as '--.-- '.
			 *  A 'none' value will result in the link not being drawn.
			 * 
			 * @mapping {'line'|'arc'} link-style
			 *  The type of line that should be used to draw the link, currently limited to
			 *  a straight line or clockwise arc of consistent degree.
			 * 
			 * @mapping {'top'|'middle'| String='top'} sankey-anchor
			 *  The relative position that the Sankey flows will start drawing from. 'top' will draw the flows top-down starting from the given node location.
			 *  'middle' will center the flows about the given node position.
			 *  
			 * @mapping {Boolean=true} visible
			 *  The visibility of a link.
			 * 
			 * @mapping {Number=1} opacity
			 *  The opacity of a link. Values for opacity are bound with the range [0,1], with 1 being opaque.
			 * 
			 * @mapping {Object} source
			 *  The source node data object representing the starting point of the link. The source node
			 *  data object is supplied for node mappings 'node-x', 'node-y', and 'source-offset' for
			 *  convenience of shared mappings.
			 * 
			 * @mapping {Number=0} source-offset
			 *  The distance from the source node position at which to begin the link. The source-offset
			 *  mapping is supplied the source node as a data object when evaluated.
			 * 
			 * @mapping {Object} target
			 *  The target node data object representing the ending point of the link. The target node
			 *  data object is supplied for node mappings 'node-x', 'node-y', and 'target-offset' for
			 *  convenience of shared mappings.
			 * 
			 * @mapping {Number=0} target-offset
			 *  The distance from the target node position at which to begin the link. The target-offset
			 *  mapping is supplied the target node as a data object when evaluated.
			 * 
			 * @mapping {Number} node-x
			 *  A node's horizontal position, evaluated for both source and target nodes.
			 * 
			 * @mapping {Number} node-y
			 *  A node's vertical position, evaluated for both source and target nodes.
			 * 
			 * @constructs
			 * @factoryMade
			 * @extends aperture.Layer
			 * @requires a vector canvas
			 *
			 */
			init : function( spec, mappings ) {
				aperture.Layer.prototype.init.call(this, spec, mappings );
			},

			// type flag
			canvasType : aperture.canvas.VECTOR_CANVAS,

			/*
			 * Render implementation
			 */
			render : function( changeSet ) {
				var i, 
					links = changeSet.updates, 
					n = links.length,
					transition = changeSet.transition;
				
				//DEBUG
				// TODO: Parameterize these later.
				var offset=0;

				
				// Remove any obsoelete visuals.
				if (changeSet.removed.length > 0){
					removeSankeys(changeSet.removed);
				}
				// PRE-PROCESSING
				// Iterate through each link and create a map describing the
				// source and target endpoints for each flow.
				var specMap = preProcessor.call(this, links);
				var sourceMap = specMap.sourceMap;
				var targetMap = specMap.targetMap;
				
				// Iterate through each source node and create the flows.
				var nIndex=0;
				var paths = [];
				
				var width=0, totalOffset=0;

				// For each target node, iterate over all the incoming flows
				// and determine the stacked, flow endpoint positions.
				for (var key in targetMap){
					var targetSpecList = targetMap[key].inflows;
					
					width=totalOffset=0;
					for (nIndex = 0; nIndex < targetSpecList.length; nIndex++){
						var flowSpec = targetSpecList[nIndex];
						
						width = getStackedWidth.call(this, flowSpec.target.links, flowSpec['sankey-anchor']);
						var flowWidth = getFlowOffset.call(this, flowSpec);
						totalOffset += flowWidth*0.5;
						
						targetPt = {
									'x' : flowSpec.target.x - flowSpec.target.r,
									'y' : flowSpec.target.y + totalOffset - 0.5*width
						};
						if (targetMap[flowSpec.target.id]['stackPts'] == null){
							targetMap[flowSpec.target.id] = {'stackPts' : {}};
						}
						targetMap[flowSpec.target.id].stackPts[flowSpec.source.id] = targetPt;
						totalOffset += flowWidth*0.5;
					}
				}
				
				// For each source node, iterate overall all the outgoing flows
				// and determine the stacked, flow endpoint positions.
				// Then couple these source endpoints with the target endpoints
				// from above and calculate the bezier path for that flow.
				for (var key in sourceMap){
					var sourceSpecList = sourceMap[key].outflows;
					
					width=totalOffset=0;
					for (nIndex = 0; nIndex < sourceSpecList.length; nIndex++){
						var flowSpec = sourceSpecList[nIndex];
						width = getStackedWidth.call(this, flowSpec.source.links, flowSpec['sankey-anchor']);
						var flowWidth = getFlowOffset.call(this, flowSpec);
						totalOffset += flowWidth*0.5;

						sourcePt = {
									'x' : flowSpec.source.x + flowSpec.source.r,
									'y' : flowSpec.source.y + totalOffset - 0.5*width
						};
						
						var path = calcFlowPath(sourcePt, targetMap[flowSpec.target.id].stackPts[flowSpec.source.id]);
						
						paths.push({
							'link': flowSpec.link,
							'path' : path
						});
						totalOffset += flowWidth*0.5;
					}
				}
				
				// Iterate over the list of flow paths and render.
				for (var i=0; i < paths.length; i++){
					var link = paths[i].link;
					var linkData   = link.data;
					var path = paths[i].path;

					var attrs = {
						'opacity': this.valueFor('opacity', linkData, 1),
						'stroke' : this.valueFor('stroke', linkData, 'link'),
						'stroke-width' : this.valueFor('stroke-width', linkData, 1),
					};

					// extra processing on stroke style
					attrs['stroke-dasharray'] = strokeStyle(attrs, this.valueFor('stroke-style', linkData, ''));

					// now render it.
					if (_sankeyCache[linkData.id]){
						attrs.path = path;
						var updateLink = _sankeyCache[linkData.id];
						link.graphics.update(updateLink, attrs, transition);
					} else {
						_sankeyCache[linkData.id] = link.graphics.path(path).attr( attrs );
					}
				}
			}
		}
	);

	return namespace;

}(aperture || {}));/**
 * Source: Set.js
 * Copyright (c) 2014 Oculus Info Inc.
 * @fileOverview The Set implementation
 */

/**
 * @namespace
 * @ignore
 * Ensure namespace exists
 */
aperture = (
/** @private */
function(namespace) {

	// util is always defined by this point
	var util = aperture.util;

	var Set = aperture.Class.extend( 'aperture.Set',
	/** @lends aperture.Set# */
	{
		/**
		 * @class A Set contains a collection of values/objects.  Elements of the set
		 * can be added, removed, toggles, and checked for containment.  Sets maintain
		 * a notion of converting between data objects (used by layers) and the contents
		 * of the set.  For example, a set may contain city names and a way to extract
		 * the city name from a give data element.
		 *
		 * TODO The notion of converting from data->set contents could be extracted into
		 * a separate object.  This object could be reused elsewhere and would make the
		 * Set simpler.
		 *
		 * @constructs
		 * @extends aperture.Class
		 *
		 * @param {String|Function} [id]
		 *      An optional conversion directive that allows a set to convert data items
		 *      to set contents.  The conversion will be used for creating filter functions
		 *      on calls to functions such as {@link #scale}.
		 */
		init : function( id ) {
			var that = this,
				fieldChain;

			// Create the idFunction if specified by user
			if( util.isString(id) && !!(fieldChain = id.match(jsIdentifierRegEx)) ) {
				// Yes, create an array of field names in chain
				// Remove . from field names.  Leave []s
				fieldChain = util.map( fieldChain, function(field) {
					// Remove dots
					if( field.charAt(field.length-1) === '.' ) {
						return field.slice(0,field.length-1);
					} else {
						return field;
					}
				});

				this.idFunction = function() {
					// Make a clone since the array will be changed
					// TODO Hide this need to copy?
					// Pass in array of arguments = array of indexes
					return findFieldChainValue.call( this, fieldChain.slice(0),
							Array.prototype.slice.call(arguments) );
				};
			} else if( util.isFunction(id) ) {
				this.idFunction = id;
			}

			// The filter function takes parameters in the form provided by layer
			// mapping filters, and calls "contains" bound to this object
			this.filterFn = function( value, etc ) {
				value = that.translateData.call(that, this, Array.prototype.slice.call(arguments, 1));
				return that.contains(value);
			};

			this.contents = [];
		},

		/**
		 * Adds an element to the Set.  If the set already contains the element it will
		 * not be added.
		 * @param object
		 *      the element to be added
		 * @returns {boolean}
		 *      true if the element is added, else undefined
		 */
		add : function( object ) {
			if( !this.contains(object) ) {
				this.contents.push( object );
				return object;
			}
		},

		/**
		 * Clears the set leaving it empty.
		 * 
		 * @returns {Array}
		 *      the array of removed elements.
		 */
		clear : function() {
			var r= this.contents;
			this.contents = [];
			
			return r;
		},

		/**
		 * Removes an item from the Set.
		 *
		 * @param object
		 *      the element to be removed
		 */
		remove : function( object ) {
			this.contents = util.without(this.contents, object);
		},

		/**
		 * Executes a function for each element in the set, where
		 * the arguments will be the value and its index, in that
		 * order.
		 */
		forEach : function ( fn ) {
			util.forEach(this.contents, fn);
		},

		/**
		 * Returns the set element at index.
		 *
		 * @param index
		 *      the integer index of the element to return
		 *
		 * @returns {object}
		 *      the element at index.
		 */
		get : function( index ) {
			return this.contents[index];
		},

		/**
		 * Returns the number of elements in the set.
		 *
		 * @returns {number}
		 *      the count of elements in the set.
		 */
		size : function () {
			return this.contents.length;
		},

		/**
		 * Returns the set contents in a new array.
		 *
		 * @returns {Array}
		 *      the set as a new array.
		 */
		toArray : function () {
			return this.contents.slice();
		},

		/**
		 * Toggles the membership of a given element in the set.  If the set contains
		 * the element it will be removed.  If it does not contain the element, it will
		 * be added.
		 * @param object
		 *      the element to be toggled
		 */
		toggle : function( object ) {
			if( this.contains(object) ) {
				this.remove(object);
			} else {
				this.add(object);
			}
		},

		/**
		 * Determines whether a given element is contained in this set.
		 *
		 * @param object
		 *      the element to be checked
		 *
		 * @returns {boolean}
		 *      true if the element is in the set, false otherwise
		 */
		contains : function( object ) {
			return util.indexOf( this.contents, object ) >= 0;
		},


		/**
		 * Given a data object and optional indices returns the element value that could
		 * be or is included in this set.  For example, if this set contains city name
		 * strings and was given an id directive for how to extract a city name from a
		 * given data item, this function will do exactly that.
		 *
		 * @param {Object} data
		 *      The data item to translate
		 *
		 * @param {Array} [etc]
		 *      An optional set of indexes
		 *
		 * @returns {Object}
		 *      The element that would be contained in this Set given the id translation
		 *      directive given on Set construction.  If no id directive, returns the
		 *      data object as given.
		 */
		translateData : function( data, etc ) {
			if( this.idFunction ) {
				// Map data to a field value using idFunction
				// Call id function in context of data object with parameters "etc"
				return this.idFunction.apply(data, etc);
			} else {
				// Just use the data straight up
				return data;
			}
		},


		// XXX It would be nice if methods for all filters were automatically
		// added here since each one involves trivial code
		/**
		 * Creates a filter function that can be used on a layer mapping
		 * (see {@link aperture.Layer#map}).  The filter function supplied
		 * will be called with the visual property value to be transformed
		 * and returned, but only for data elements that are within this set.
		 *
		 * @param {Function} filter
		 *      The transformation to apply, in the form function( value ) {return xvalue;}
		 *
		 * @returns {Function}
		 *      A filter function that can be applied to a visual property mapping of
		 *      a layer.
		 */
		filter : function ( filter ) {
			return namespace.filter.conditional( this.filterFn, filter );
		},

		/**
		 * Creates a scaling filter function that can be used on a layer mapping
		 * (see {@link aperture.Layer#map}).  The given amount will be used to
		 * scale the filtered numeric visual property value but only for data elements
		 * that are within this set.
		 *
		 * @param {Number} amount
		 *      A scaling factor to apply to the mapped visual property for data elements
		 *      that are within this set.
		 *
		 * @returns {Function}
		 *      A filter function that can be applied to a visual property mapping of
		 *      a layer.
		 */
		scale : function(amount) {
			return namespace.filter.conditional(
					this.filterFn,
					aperture.filter.scale(amount)
				);
		},


		/**
		 * Creates a constant value filter function that can be used on a layer mapping
		 * (see {@link aperture.Layer#map}).  The filter will use the given constant
		 * value in place of the mapped value for all elements within this set.
		 *
		 * @returns {Function}
		 *      A filter function that can be applied to a visual property mapping of
		 *      a layer.
		 */
		constant : function(val) {
			return namespace.filter.conditional(
					this.filterFn,
					aperture.filter.constant(val)
				);
		}
	});

	/**
	 * @methodof aperture.Set#
	 * @name not
	 *
	 * @description Creates an inverted view of the Set (its complement) specifically 
	 * aimed at creating filter functions that apply when an element is <b>not</b> in 
	 * the set. The returned set will have an inverted {@link #contains} behavior and will 
	 * create inverted filter functions.  Changes to the set using methods such as {@link #add},
	 * {@link #remove}, and {@link #clear} will work on the core set, and do not exhibit
	 * inverted behavior.
	 * 
	 * @returns {Function}
	 *      A filter function in the form function( value, layer, data, ... )
	 */
	Set.addView( 'not', {
		init : function() {
			var that = this;

			// Must create a
			this.filterFn = function( value, etc ) {
				value = that.translateData.call(that, this, Array.prototype.slice.call(arguments, 1));
				return that.contains(value);
			};
		},

		contains : function(object) {
			// Invert containment check result
			return !this._base.contains(object);
		}
	});

	namespace.Set = Set;






	return namespace;

}(aperture || {}));
/**
 * Source: Animation.js
 * Copyright (c) 2014 Oculus Info Inc.
 * @fileOverview Aperture Animation APIs
 */

/**
 * @ignore
 * Ensure namespace exists
 */
aperture = (
/** @private */
function(namespace) {

	namespace.Transition = aperture.Class.extend( 'aperture.Transition',
	/** @lends aperture.Transition.prototype */
	{
		/**
		 * @class Represents an animated transition, consisting of
		 * an interpolation / easing / tween function, and a length
		 * of time over which the transition will occur. Transitions may
		 * be optionally passed into the Layer update function to animate
		 * any updates which will occur.
		 *
		 * @constructs
		 * @extends aperture.Class
		 *
		 * @param {Number} [milliseconds=300]
		 *      the length of time that the transition will take to complete.
		 *
		 * @param {String} [easing='ease']
		 *      the function that will be used to transition from one state to another.
		 *      The standard CSS options are supported:<br><br>
		 *      'linear' (constant speed)<br>
		 *      'ease' (default, with a slow start and end)<br>
		 *      'ease-in' (slow start)<br>
		 *      'ease-out' (slow end)<br>
		 *      'ease-in-out' (similar to ease)<br>
		 *      'cubic-bezier(n,n,n,n)' (a custom function, defined as a bezier curve)
		 *
		 * @param {Function} [callback]
		 *      a function to invoke when the transition is complete.
		 *
		 * @returns {this}
		 *      a new Transition
		 */
		init : function( ms, easing, callback ) {
			this.time = ms || 300;
			this.fn = easing || 'ease';
			this.end = callback;
		},

		/**
		 * Returns the timing property.
		 *
		 * @returns {Number}
		 *      the number of milliseconds over which to complete the transition
		 */
		milliseconds : function ( ) {
			return this.time;
		},

		/**
		 * Returns the easing property value.
		 *
		 * @returns {String}
		 *      the function to use to transition from one state to another.
		 */
		easing : function ( ) {
			return this.fn;
		},

		/**
		 * Returns a reference to the callback function, if present.
		 *
		 * @returns {Function}
		 *      the function invoked at transition completion.
		 */
		callback : function ( ) {
			return this.end;
		}
	});

	return namespace;

}(aperture || {}));
/**
 * Source: filter.js
 * Copyright (c) 2014 Oculus Info Inc.
 * @fileOverview Aperture Filter API Implementations
 */

/**
 * @namespace Aperture filter APIs
 */
aperture.filter = (function(namespace) {

	var effects =
	/** @lends aperture.filter */
	{

		/**
		 * Returns a function that always returns the supplied constant value
		 */
		constant : function(value) {
			return function() {
				return value;
			};
		},

		/**
		 * Returns a effect function that scales a provided number by the given
		 * scalar value.
		 */
		scale : function(amount) {
			return function( value ) {
				return value * amount;
			};
		},
		
		/**
		 * Returns a effect function that shifts a provided number by the given
		 * scalar value.
		 */
		shift : function(amount) {
			return function( value ) {
				return value + amount;
			};
		},
		brighter : function(color) {
			// TODO
		},

		/**
		 * Takes a conditional function and a effect function and returns a function
		 * that will apply the given effect to the supplied arguments only when the truth
		 * function returns a truthy value when called with the supplied arguments.  For
		 * example:
		 * <code>
		 * var makeBigBigger = conditional(
		 *      function(value) { return value > 1000; },
		 *      aperture.filter.scale( 2 )
		 * );
		 *
		 * var makeRedBlue = conditional(
		 *      function(value) { return value === 'red'; },
		 *      function() { return 'blue'; }
		 * );
		 * </code>
		 *
		 * @param {Function} checkFunction
		 * @param {Function} filterFunction
		 */
		conditional : function( checkFunction, filterFunction ) {
			return function(value) {
				// If supplied conditional...
				if( checkFunction.apply(this, arguments) ) {
					// Apply effect
					return filterFunction.apply(this, arguments);
				} else {
					return value;
				}
			};
		}
	};

	// Mix in effect definitions into provided namespace
	aperture.util.extend(namespace, effects);

	return namespace;

}(aperture.filter || {}));
/**
 * Source: io.js
 * Copyright (c) 2014 Oculus Info Inc.
 * @fileOverview APIs for client / server interaction.
 */

/*
 * TODO Remove jQuery dependency?
 */

/**
 * @namespace APIs for client / server interaction.
 * @requires jQuery
 * @requires json2 as a JSON shim if running old browsers
 */
aperture.io = (function() {

	var id = 0,
		securityFn,
		restEndpoint = "%host%/rest";


	// Register to receive RPC endpoint url from config
	aperture.config.register("aperture.io", function(config) {
		// Get out endpoint location
		restEndpoint = config["aperture.io"].restEndpoint;
	});


	return {

		/**
		 * Resolves a relative uri to a rest url
		 *
		 * TODO handle non-relative URIs
		 */
		restUrl : function ( uri ) {

			// XXX This is fragile and should be fixed!
			var origin = // Have origin, use it
				document.location.origin ||
				// Don't have origin, construct protocol//host
				(document.location.protocol + '//' + document.location.host);

			return restEndpoint.replace("%host%", origin) + uri;
		},

		/**
		 * Makes a REST call to the server for the given URI, method, and posted data
		 * block.  Callback for onSuccess and onError may be provided.
		 *
		 * @param {String} uri The URI to which to make the ajax
		 *
		 * @param {String} method The HTTP method to use, must be a valid HTTP verb such as GET or POST
		 *
		 * @param {Function(data,Object)} callback A callback function to be used when the ajax call returns.
		 * Will be called on both success and error conditions.  The first parameter will contain a data
		 * payload: if JSON, will automatically be converted into an object.  The second parameter is an
		 * object that contains various ajax response values including: success - a boolean, true when the
		 * ajax call was successful; xhr - the XHR object; status - a string containing the HTTP call status;
		 * errorThrown - on error, the error that was thrown.
		 *
		 * @param {Object} opts an options object
		 * @param {String} opts.postData data to post if the POST verb is used, will be automatically
		 * converted to a string if given an object
		 * @param {String|Object} opts.params parameters to include in the URL of a GET request.  Will
		 * automatically be converted to a string if given a hash
		 * @param {Object} opts.headers additional headers to set as key:value pairs
		 * @param {String} opts.contentType explicit content type used when POSTing data to the server.
		 */
		rest : function( uri, method, callback, opts ) {
			var restUrl = aperture.io.restUrl( uri ),

				// Success callback processes response and calls user's callback
				innerSuccess = function(results, textStatus, jqXHR) {
					if( callback ) {
						// Return results data object plus a hash of
						// other available data.  Also include a success
						// parameter to indicate that the request succeeded
						callback( results, {
								success: true,
								status: textStatus,
								xhr: jqXHR
							});
					}
				},

				// Error callback processes response and calls user's callback
				innerError = function(jqXHR, textStatus, errorThrown) {
					if( callback ) {
						// Provide callback with data returned by server, if any
						var responseData = jqXHR.responseText;

						// Check content-type for json, parse if json
						var ct = jqXHR.getResponseHeader( "content-type" );
						if( responseData && ct && ct.indexOf('json') > -1 ) {
							try {
								responseData = jQuery.parseJSON( responseData );
							} catch( e ) {
								// Error parsing JSON returned by HTTP error... go figure
								// TODO log
								responseData = null;
							}
						}

						// Return error data object plus a hash of
						// other available data.  Also include a success
						// parameter to indicate that the request failed
						callback( responseData, {
								success: false,
								status: textStatus,
								xhr: jqXHR,
								errorThrown: errorThrown
							});
					}
				},

				params = {
					url: restUrl,
					type: method,
					success: innerSuccess,
					error: innerError
				};

			// Augment REST url as needed to add security tokens
			if( securityFn ) {
				restUrl = securityFn( restUrl );
			}

			// POST or GET params
			if( opts ) {
				if( opts.contentType ) {
					params.contentType = opts.contentType;
				}
				
				if( opts.postData && method === "POST" ) {
					params.data = opts.postData;
					
					if (params.contentType && params.contentType.toLowerCase().indexOf('json') > -1) {
						params.contentType = 'application/json; charset=UTF-8';
						
						if (!aperture.util.isString(params.data)) {
							params.data = JSON.stringify(params.data);
						}
					}
				}

				if( opts.params && method === "GET" ) {
					params.data = opts.params;
				}

				if( opts.headers ) {
					params.headers = opts.headers;
				}
			}

			//  Make the AJAX call using jQuery
			$.ajax( params );
		},

		/**
		 * Sets a function that can be used to add security information
		 * to a URL before it is used to make an RPC call to the server.
		 * This permits implementation-specific (token-based) authentication.
		 */
		setUrlAuthenticator : function( fn ) {
			if( fn && typeof fn !== "function") {
				// TODO exception
				return;
			}

			securityFn = fn;
		}
	};
}());
/**
 * Source: palette.js
 * Copyright (c) 2014 Oculus Info Inc.
 * @fileOverview Defines the palette functions for Aperture.
 */

/**
 * @namespace Aperture exposes a global base palette in order to promote
 *	and ease use of coordinated, complementary, cognitively effective visual
 *  attributes in a display context. Palettes support derivation,
 *  providing a concise and systematic method of defining properties
 *  through the articulation of relationships.
 *  <br><br>
 *
 *  The base palette is populated at load time from the aperture.palette
 *  configuration provider. Its prototype includes the 17 named colors
 *  defined by CSS 2.1 for runtime access only. Support for exporting
 *  <a href="http://www.lesscss.org">LESS CSS</a> compatible variable
 *  lists for import is also provided for deriving CSS style sheet values
 *  from base palette entries. LESS provides a concise and systematic
 *  method of defining CSS properties for style sheets, however
 *  if use of LESS is not an option, export of
 *  named CSS classes for specifically targeted style sheet properties
 *  (e.g. color, border-color, etc) is also supported.
 */
aperture.palette = (function(ns) {

	var // CSS 2.1 colors
		baseColors = {
			'white'  : '#ffffff',
			'silver' : '#c0c0c0',
			'gray'	 : '#808080',
			'black'  : '#000000',
			'red'    : '#FF0000',
			'maroon' : '#800000',
			'orange' : '#ffa500',
			'yellow' : '#ffff00',
			'olive'  : '#808000',
			'lime'   : '#00ff00',
			'green'  : '#008000',
			'aqua'   : '#00ffff',
			'teal'   : '#008080',
			'blue'   : '#0000ff',
			'navy'   : '#000080',
			'fuchsia': '#ff00ff',
			'purple' : '#800080'
		},

		// TODO: this is the protovis ten - pick a good ten that don't include something too
		// close to our indicative colors like straight red and green, or the highlight color.
		baseColorSets = {
			'default' : [
				'#1f77b4',
				'#ff7f0e',
				'#2ca02c',
				'#d62728',
				'#9467bd',
				'#8c564b',
				'#e377c2',
				'#7f7f7f',
				'#bcbd22',
				'#17becf'
				]
		},

		// inheritance makes it easier to distinguish overrides from
		// standard entries but still use the same lookup.
		colors = aperture.util.viewOf(baseColors),
		colorSets = baseColorSets,
		restUrl = aperture.io.restUrl;
	
	/**
	 * @private
	 * 
	 * enforce implemented value constraints already on client side so that we
	 * are not hitting the server or storing more images client side than we need to.
	 */
	function constrain100(value) {
		if (value == null) {
			return 100;
		} 
		
		return 20* Math.round(5* Math.max(0, Math.min(1, value)));
	}


	// parchment color constants.
	var rgb = aperture.Color.fromRGB,
		c00 = rgb(248,210,158),
	    c10 = rgb(253,231,192),
	    c01 = rgb(202,202,202),
	    c11 = rgb(255,255,255),
	    black = rgb(0,0,0);
	
	/**
	 * @private
	 * 
	 * returns a color for the parchment by bilinear interpolation. roughly the same as the image.
	 */
	function parchmentColor(v_, _v) {
		return c00.blend(c10, v_).blend(c01.blend(c11, v_), _v);
	}
	
	/**
	 * @private
	 * 
	 * returns the url for the parchment background.
	 */
	function parchmentUrl(confidence, currency) {
		return restUrl('/parchment/' + confidence + '/' + currency);
	}
	
	/**
	 * @name aperture.palette.color
	 * @function
	 *
	 * @param {String} id
	 *      the identifying name of the palette entry.
	 *
	 * @returns {aperture.Color}
	 *		a color object
	 */
	ns.color = function(id) {
		return colors[id];
	};

	/**
	 * @name aperture.palette.colors
	 * @function
	 *
	 * @param {String} id
	 *      the identifying name of the palette entry.
	 *
	 * @returns {Array}
	 *		an Array of color objects
	 */
	ns.colors = function (id) {
		return colorSets[id || 'default'];
	};

	/**
	 * @name aperture.palette.size
	 * @function
	 *
	 * @param {String} id
	 *      the identifying name of the palette entry.
	 *
	 * @returns {Number}
	 *		a Number
	 */
	ns.size = function (id) {
		return sizes[id];
	};

	/**
	 * @name aperture.palette.sizes
	 * @function
	 *
	 * @param {String} id
	 *      the identifying name of the palette entry.
	 *
	 * @returns {Array}
	 *		an array of Numbers
	 */
	ns.sizes = function (id) {
		return sizeSets[id];
	};

	/**
	 * @name aperture.palette.parchmentCSS
	 * @function
	 * 
	 * @description
	 * Returns CSS properties to reflect confidence using background texture. Confidence of information is 
	 * indicated by how pristine the parchment is, and currency of information is indicated by
	 * how white the parchment is. Dated information yellows with time.
	 * 
	 * @param {Number} confidence 
	 *      an indication of confidence, as a value between 0 and 1 (confident).
	 *
	 * @param {Number} [currency] 
	 *      an indication of how current the information is, as a value between 0 and 1 (current).
	 * 
	 * @returns {Object}
	 *      an object with CSS properties, that includes background image and border color.
	 */
	ns.parchmentCSS = function(confidence, currency) {
		
		// constrain these
		confidence = constrain100(confidence);
		currency = constrain100(currency);

		var color = parchmentColor(0.01*confidence, 0.01*currency);
		
		return {
			'background-color': color.toString(),
			'background-image': confidence < 99? 'url('+ parchmentUrl(confidence, currency)+ ')' : '',
			'border-color': color.blend(black, 0.25).toString()
		};
	};
	
	/**
	 * @name aperture.palette.parchmentClass
	 * @function
	 * 
	 * @description
	 * Returns a CSS class used to reflect confidence using background texture. Confidence of information is 
	 * indicated by how pristine the parchment is, and currency of information is indicated by
	 * how white the parchment is. Dated information yellows with time.
	 * 
	 * @param {Number} confidence 
	 *      an indication of confidence, as a value between 0 and 1 (confident).
	 *
	 * @param {Number} [currency] 
	 *      an indication of how current the information is, as a value between 0 and 1 (current).
	 * 
	 * @returns {String}
	 *      the name of a CSS class defined in rest/parchment.css.
	 */
	ns.parchmentClass = function(confidence, currency) {
		return 'aperture-parchment-' + constrain100(confidence) + '-' + constrain100(currency);
	};

	/**
	 * @name aperture.palette.asLESS
	 * @function
	 *
	 * @returns {String}
	 *		a string representation in LESS CSS variable format.
	 *
	 * @see <a href="http://lesscss.org">lesscss.org</a>
	 */
	ns.asLESS = function () {

		// header and color block
		var str = '// Generated Aperture Palette Export for LESS CSS\n\n// COLORS (excluding CSS 2.1)\n';

		// write each
		aperture.util.forEach( colors, function ( color, key ) {
			str += '@' + key + ': ' + color + ';\n';
		});

		// color set block
		str += '\n// COLOR SETS\n';

		// write each
		aperture.util.forEach( colorSets, function ( colorSet, key ) {
			var prefix = '@' + key + '-';

			aperture.util.forEach( colorSet, function ( color, index ) {
				str += prefix + index + ': '+ color + ';\n';
			});
		});

		return str;
	};

	//
	// Register to receive style information
	//
	aperture.config.register('aperture.palette', function(cfg) {
		var paletteExts = cfg['aperture.palette'];

		if( paletteExts ) {
			var c = paletteExts.color, p;
			
			// extend colors with configured colors
			if ( c ) {
				for ( p in c ) {
					if (c.hasOwnProperty(p)) {
						if (p.toLowerCase) {
							p = p.toLowerCase();
							colors[p] = c[p];
						}
					}
				}
			}

			// extend default color sets with clones of configured color sets.
			if ( paletteExts.colors ) {
				aperture.util.forEach( paletteExts.colors, function ( value, key ) {
					colorSets[key] = value.slice();
				});
			}

			// TODO: sizes etc.
		}
	});

	// used in the function below.
	var u = encodeURIComponent,
		supportsSvg = !!(window.SVGAngle || document.implementation.hasFeature('http://www.w3.org/TR/SVG11/feature#BasicStructure', '1.1')),
		logSvgWarn = true;

	/**
	 * @name aperture.palette.icon
	 * @function
	 *
	 * @requires an Aperture icon service
	 * @requires jQuery
	 *
	 * @param {Object} properties
	 *      the specification of the icon
	 *
	 * @param {String} properties.type
	 *      The ontological type for the icon service to resolve within the namespace of the ontology.
	 *
	 * @param {String} [properties.ontology='aperture-hscb']
	 *      Refers to the namespace used by any active icon services to resolve types with attributes to icons.
	 *      Note that the mapping of ontology to symbology is a function of the icon services configured and
	 *      running. The default symbology is a core set of icons with a specific focus on
	 *      socio-cultural themes and media artifacts of socio-cultural analysis.

	 * @param {Object} [properties.attributes]
	 *      The optional attributes of the ontological type, for interpretation by the icon service.
	 *
	 * @param {Number} [properties.width=24]
	 *      The width of the icon. Defaults to 24.
	 *
	 * @param {Number} [properties.height=24]
	 *      The height of the icon. Defaults to 24.
	 *
	 * @param {String} [properties.format]
	 *      The image format of the icon. When absent the format is left to the discretion of the
	 *      icon service. Support for specific formats is service dependent, however the default
	 *      aperture-hscb ontology supports
	 *      <span class="fixedFont">png</span> (a lossless compressed raster format with transparency),
	 *      <span class="fixedFont">svg</span> (a vector format that can draw nicely at any scale), and
	 *      <span class="fixedFont">jpeg</span> (an opaque, lossy but highly compressible format).
	 *
	 * @returns {String}
	 *		a url
	 */
	ns.icon = function (properties) {
		var frmt = properties.format,
			attr = properties.attributes,
			path = '/icon/' +
				u(properties.ontology || 'aperture-hscb') + '/' +
				u(properties.type) +
				'?iconWidth=' + (properties.width || 24) +
				'&iconHeight=' + (properties.height || 24);

		if (frmt) {
			// check - can't use svg if running in vml.
			if (!supportsSvg && frmt.toLowerCase() === 'svg') {

				if (logSvgWarn) {
					aperture.log.warn("SVG icon format requested but this browser doesn't support it. Using PNG.");

					// only warn once
					logSvgWarn = false;
				}

				frmt = 'png';
			}

			path += '&iconFormat=' + frmt;
		}

		aperture.util.forEach( attr, function ( value, name ) {
			path += '&' + u(name) + '=' + u(value);
		});

		return restUrl(path);
	};

	return ns;

}(aperture.palette || {}));

/**
 * Source: AjaxAppender.js
 * Copyright (c) 2014 Oculus Info Inc.
 * @fileOverview Aperture Logging AJAX Appender Implementation
 */

/**
 * @namespace
 * @ignore
 * Ensure namespace exists
 */
aperture.log = (function(ns) {

	var AjaxAppender = aperture.log.Appender.extend(
	{
		init : function( spec ) {
			spec = spec || {};

			aperture.log.Appender.prototype.init.call(this, spec.level || aperture.log.LEVEL.WARN);
			this.url = spec.url;
			this.buffer = [];

			// Force the scope of postData to this, no matter
			// how it's usually called.
			this.postData = aperture.util.bind(this.postData, this);
			
			// Post data at requested interval
			setInterval(this.postData, spec.timeout || 3000 );

			// Also post if navigating away from the page
			$(window).unload( this.postData );
		},

		/** @private */
		logString : function( level, message ) {
			// Push a log record onto the stack
			this.buffer.push( {
				level: level,
				message: message,
				when: new Date()
			} );
		},

		/** @private */
		logObjects : function( level, objs ) {
			// Push a log record onto the stack
			this.buffer.push( {
				level: level,
				data: objs,
				when: new Date()
			} );
		},

		/**
		 * @private
		 * Causes the appender to post any queued log messages to the server
		 */
		postData : function() {
			if( buffer.length ) {
				// Simple fire and forget POST of the data
				$.ajax( {
					url: this.url,
					type: 'POST',
					data: this.buffer
				});

				// Clear buffer
				this.buffer = [];
			}
		}
	});

	/**
	 * @name aperture.log.addAjaxAppender
	 * @function
	 * @description
	 * <p>Creates and adds an AJAX appender object.
	 * The AJAX Appender POSTs log messages to a provided end-point URI
	 * using a JSON format.  Log messages are buffered on the client side
	 * and only sent to the server once every N seconds where N is settable
	 * upon construction.</p>
	 * <p>The data will be posted with the following format:</p>
	 * <pre>
	 * [
	 * { level:"warn", message:"A log message", when:"2011-09-02T17:57:33.692Z" },
	 * { level:"error", data:{some:"data"}, when:"2011-09-02T17:57:34.120Z" }
	 * ]
	 * </pre>
	 *
	 * @param {Object} spec specification object describing the properties of
	 * the ajax appender to build
	 * @param {String} spec.url the server endpoint to which log messages will be
	 * POSTed.
	 * @param {Number} [spec.timeout] period in milliseconds between when collected
	 * log messages are sent to the server.  Defaults to 3000
	 * @param {aperture.log.LEVEL} [spec.level] initial appender logging threshold level, defaults to WARN
	 *
	 * @returns {aperture.log.Appender} a new AJAX appender object that has been added
	 * to the logging system
	 */
	ns.addAjaxAppender = function(spec) {
		return ns.addAppender( new AjaxAppender(spec) );
	};

	return ns;
}(aperture.log || {}));
/**
 * Source: AlertBoxAppender.js
 * Copyright (c) 2014 Oculus Info Inc.
 * @fileOverview Aperture Logging Alert Box Appender Implementation
 */

/**
 * @namespace
 * @ignore
 * Ensure namespace exists
 */
aperture.log = (function(ns) {


	var AlertBoxAppender = aperture.log.Appender.extend(
	{

		init : function(spec) {
			spec = spec || {};
			// Default to only popping up an alertbox for errors
			aperture.log.Appender.prototype.init.call(this, spec.level || aperture.log.LEVEL.ERROR);
		},

		logString : function( level, message ) {
			// Simply
			alert( level.toUpperCase() + ':\n' + message );
		}
	});

	/**
	 * @name aperture.log.addAlertBoxAppender
	 * @function
	 * @description Creates and adds an alert box implementation of a logging Appender to
	 * the logging system.  Pops up an alert box for every log message that passes the
	 * appender's threshold.  By default the threshold is set to ERROR to ensure alert boxes
	 * rarely appear.
	 *
	 * @param {Object} [spec] specification object describing the properties of the appender
	 * @param {aperture.log.LEVEL} [spec.level] initial appender logging threshold level, defaults to ERROR
	 *
	 * @return {aperture.log.Appender} a new alert box appender instance
	 */
	ns.addAlertBoxAppender = function(spec) {
		return ns.addAppender( new AlertBoxAppender(spec) );
	};

	return ns;
}(aperture.log || {}));
/**
 * Source: BufferingAppender.js
 * Copyright (c) 2014 Oculus Info Inc.
 * @fileOverview Aperture Logging Buffering Appender Implementation
 */

/**
 * @namespace
 * @ignore
 * Ensure namespace exists
 */
aperture.log = (function(ns) {

	/*
	 * TODO A buffering appender may be much more useful if it decorates another
	 * appender.  A call to flush the buffer will log all buffered messages to
	 * the decorated appender.
	 */
	var BufferingAppender = aperture.log.Appender.extend(
	{
		init : function(spec) {
			spec = spec || {};

			aperture.log.Appender.prototype.init.call(this, spec.level || aperture.log.LEVEL.INFO);
			this.depth = spec.bufferDepth || 100;
			this.buffer = [];
		},

		logString : function( level, message ) {
			this.buffer.push( {level:level, message:message, when:new Date()} );
			if( this.buffer.length > this.depth ) {
				this.buffer.shift();
			}
		},

		logObjects : function( level, objs ) {
			this.buffer.push( {level:level, objects:objs, when:new Date()} );
			if( this.buffer.length > this.depth ) {
				this.buffer.shift();
			}
		},

		getBuffer : function( keepBuffer ) {
			var returnValue = this.buffer;
			if( !keepBuffer ) {
				this.buffer = [];
			}
			return returnValue;
		}
	});

	/**
	 * @name aperture.log.addBufferingAppender
	 * @function
	 *
	 * @description Creates and adds a buffering appender to the logging system.
	 * This appender stores most recent N log messages internally
	 * and provides a list of them on demand via a 'getBuffer' function.
	 *
	 * @param {Object} [spec] specification object describing how this appender
	 * should be constructed.
	 * @param {Number} [spec.bufferDepth] maximum number of log records to keep in
	 * the buffer, defaults to 100
	 * @param {aperture.log.LEVEL} [spec.level] initial appender logging threshold level, defaults to INFO
	 *
	 * @returns {aperture.log.Appender} a new buffering appender instance
	 */
	ns.addBufferingAppender = function(spec) {
		return ns.addAppender( new BufferingAppender(spec) );
	};

	return ns;
}(aperture.log || {}));
/**
 * Source: ConsoleAppender.js
 * Copyright (c) 2014 Oculus Info Inc.
 * @fileOverview Aperture Logging Console Appender Implementation
 */

/*
 * console is a tricky thing to get working cross browser.  Potential
 * problems:
 * <ul>
 * <li>IE 7: No console object</li>
 * <li>IE 8: 'console' only exists after dev tools are open.  console.log functions
 * are not true JS Function functions and do not support 'call' or 'apply'.  A work
 * around using Function.prototype.bind to make an applyable version of the functions
 * (http://whattheheadsaid.com/2011/04/internet-explorer-9s-problematic-console-object)
 * is not possible due to missing Function.prototype.bind.</li>
 * <li>IE 9: 'console' only exists after dev tools are open.  console.log functions
 * are not true JS Function functions and do not support 'call' or 'apply'.
 * Function.prototype.bind does exist.</li>
 * </ul>
 *
 * Ben Alman / Paul Irish (see attribution below) wrote a nice bit of code that will
 * gracefully fallback if console.error, etc are not found.  Craig Patik addressed issues
 * in IE where the console is not available until the dev tools are opened as well as
 * calling the native console functions using .apply.  .apply calls are more desirable than
 * Alman/Irish solution since the browser may nicely format the passed in data instead of
 * logging everything as an array (like Alman/Irish do).
 *
 * @see Bits and pieces of Paul Irish and Ben Alman's
 * <a href="http://benalman.com/projects/javascript-debug-console-log/">console wrapper</a>
 * code was copied and modified below.
 * Original copyright message:
	 * JavaScript Debug - v0.4 - 6/22/2010
	 * http://benalman.com/projects/javascript-debug-console-log/
	 *
	 * Copyright (c) 2010 "Cowboy" Ben Alman
	 * Dual licensed under the MIT and GPL licenses.
	 * http://benalman.com/about/license/
	 *
	 * With lots of help from Paul Irish!
	 * http://paulirish.com/
 *
 * @see Craig Patik's <a href="http://patik.com/blog/complete-cross-browser-console-log/">original post</a>
 * inspired a number of the tweaks included below.
 */

/**
 * @namespace
 * @ignore
 * Ensure namespace exists
 */
aperture.log = (function(ns) {

	var ConsoleAppender = aperture.log.Appender.extend(
	{
		init : function(spec) {
			spec = spec || {};
			aperture.log.Appender.prototype.init.call(this, spec.level || aperture.log.LEVEL.INFO);

			this.map = [];
			// create a map of log level to console function to invoke
			// if console doesn't have the function, use log
			// level values actually map to console methods (conveniently enough)
			aperture.util.forEach(aperture.log.LEVEL, function(level, key) {
				this.map[level] = function() {
					var con = window.console;

					if ( typeof con == 'undefined' ) {
						return;
					}

					if (typeof con.log == 'object') {
						// IE 8/9, use Andy E/Craig Patik/@kangax call.call workaround
						// since the console.x functions will not support .apply directly
						// Note: Could call F.p.apply.call to truly emulate calling console.log(a,b,c,...)
						// but IE concatenates the params with no space, no ',' so kind of ugly
						if (con[ level ]) {
							Function.prototype.apply.call(con[level], con, arguments);
						} else {
							Function.prototype.apply.call(con.log, con, arguments);
						}
					} else {
						// Modern browser
						if (con.firebug) {
							con[ level ].apply( window, arguments );
						} else if (con[ level ]) {
							con[ level ].apply( con, arguments );
						} else {
							con.log.apply( con, arguments );
						}
					}
				};
			},
			this );
		},

		logString : function( level, message ) {
			// Simply log the string to the appropriate logger
			this.map[level]( message );
		},

		logObjects : function( level, objArray ) {
			// Call the appropriate logger function with all the arguments
			this.map[level].apply( null, objArray );
		}
	});

	/**
	 * @name aperture.log.addConsoleAppender
	 * @function
	 * @description Creates and adds a console implementation of a logging Appender to
	 * the logging system.  This appender works as follows:
	 * <ol>
	 * <li>If firebug exists, it will be used</li>
	 * <li>If console.error, console.warn, console.info and console.debug exist, they will
	 * be called as appropriate</li>
	 * <li>If they do not exist console.log will be called</li>
	 * <li>If console.log or console do not exist, this appender does nothing</li>
	 * </ol>
	 *
	 * @param {Object} [spec] specification object describing the properties of the appender
	 * @param {aperture.log.LEVEL} [spec.level] initial appender logging threshold level, defaults to INFO
	 *
	 * @returns {aperture.log.Appender} a new console appender instance
	 */
	ns.addConsoleAppender = function(spec) {
		return ns.addAppender( new ConsoleAppender(spec) );
	};

	return ns;
}(aperture.log || {}));
/**
 * Source: DOMAppender.js
 * Copyright (c) 2014 Oculus Info Inc.
 * @fileOverview Aperture Logging DOM Appender Implementation
 */

/**
 * @namespace
 * @ignore
 * Ensure namespace exists
 */
aperture.log = (function(ns) {

	var DOMAppender = aperture.log.Appender.extend(
	{
		init : function(spec) {
			spec = spec || {};

			aperture.log.Appender.prototype.init.call(this, spec.level || aperture.log.LEVEL.INFO);

			// Add the list
			var list = this.list = $('<ol class="aperture-log-display"></ol>')
				.appendTo( spec.container );

			// Add a clear button
			$('<button class="aperture-log-clear" type="button">Clear</button>')
				.click( function() { list.empty(); } )
				.appendTo( spec.container );
		},

		logString : function( level, message ) {
			// Append a list item styled by the log level to the list
			$('<li></li>')
				.text('[' + level + '] ' + message)
				.addClass('aperture-log-'+level)
				.appendTo(this.list);
		}
	});

	/**
	 * @name aperture.log.addDomAppender
	 * @function
	 *
	 * @description Creates and adds a DOM appender to the logging system. The DOM Appender
	 * logs all messages to a given dom element.  The given DOM
	 * element will have an ordered list of log messages and a "Clear" button added to it.
	 * Log messages will be styled 'li' tags with the class 'aperture-log-#' where # is the
	 * log level, one of 'error', 'warn', 'info', 'debug', or 'log'.  The list itself
	 * will have the class 'aperture-log-display' and the button will have the class
	 * 'aperture-log-clear'.
	 *
	 * @param {Object} spec specification object describing the properties of the appender
	 * @param {Element} spec.container the DOM element or selector string to the DOM element
	 * that should be used to log all messages.
	 * @param {aperture.log.LEVEL} [spec.level] initial appender logging threshold level, defaults to INFO
	 *
	 * @returns {aperture.log.Appender} a new DOM appender instance
	 */
	ns.addDomAppender = function(spec) {
		return ns.addAppender( new DOMAppender(spec) );
	};

	return ns;
}(aperture.log || {}));
/**
 * Source: capture.js
 * Copyright (c) 2014 Oculus Info Inc.
 * @fileOverview Aperture Capture Service API
 */

/**
 * @namespace Functions used to capture snapshot images of a url or DOM element.
 * @requires an Aperture image capture service
 * @requires jQuery
 */
aperture.capture = (function() {

	/*
	 * Wraps the given callback, extracts the image location and returns.
	 */
	var callbackWrapper = function( callback ) {
		return function(result, info) {
			if( info.success ) {
				// Success, get the location ref header
				var image = info.xhr && info.xhr.getResponseHeader && info.xhr.getResponseHeader("Location");
				callback( image );
			} else {
				// TODO Support an error callback?
				callback( null );
			}
		};
	};

	// From: http://www.phpied.com/relative-to-absolute-links-with-javascript/
	// https://github.com/stoyan/etc
	function toAbs(link, host) {

		var lparts = link.split('/');
		if (/http:|https:|ftp:/.test(lparts[0])) {
			// already abs, return
			return link;
		}

		var i, hparts = host.split('/');
		if (hparts.length > 3) {
			hparts.pop(); // strip trailing thingie, either scriptname or
							// blank
		}

		if (lparts[0] === '') { // like "/here/dude.png"
			host = hparts[0] + '//' + hparts[2];
			hparts = host.split('/'); // re-split host parts from scheme and
										// domain only
			delete lparts[0];
		}

		for (i = 0; i < lparts.length; i++) {
			if (lparts[i] === '..') {
				// remove the previous dir level, if exists
				if (typeof lparts[i - 1] !== 'undefined') {
					delete lparts[i - 1];
				} else if (hparts.length > 3) { // at least leave scheme and
												// domain
					hparts.pop(); // stip one dir off the host for each /../
				}
				delete lparts[i];
			}
			if (lparts[i] === '.') {
				delete lparts[i];
			}
		}

		// remove deleted
		var newlinkparts = [];
		for (i = 0; i < lparts.length; i++) {
			if (typeof lparts[i] !== 'undefined') {
				newlinkparts[newlinkparts.length] = lparts[i];
			}
		}

		return hparts.join('/') + '/' + newlinkparts.join('/');
	}


	/** 
	 * @private
	 * NOT CURRENTLY FUNCTIONAL - this is left here for later reinstatement. probably store()
	 * would take either a url or element, and then call into here if an element.
	 * 
	 * Initiates an image capture of a snippet of HTML.
	 *
	 * @param {DOMElement} element
	 *            DOM element to capture
	 * @param {Function(String)} callback
	 *            the callback to call when the image (or error) is ready. On error will
	 *            be called with null.
	 * @param {Object} settings
	 *            A set of key/value pairs that configure the image
	 *            capture
	 * @param {boolean} settings.noStyles
	 *            set to true if the current page's CSS
	 *            styles should *not* be used in the capture process
	 * @param {Number} settings.captureWidth
	 *            if set, the virtual "screen" width
	 *            of content in which to render page in a virtual browser (in pixels).
	 *            If not set, the element's width will be used.
	 * @param {Number} settings.captureHeight
	 *            if set, the virtual "screen" height
	 *            of content in which to render page in a virtual browser (in pixels).
	 *            If not set, the element's height will be used.
	 * @param {String} settings.format
	 *            if set, specifies the image format to request.  May be one of
	 *            "JPEG", "PNG", or "SVG"
	 */
	function storeFromElement( element, callback, settings ) {
		// TODO: UPDATE THIS.
		
		// Get HTML
		var html = $(element).clone().wrap('<div></div>').parent().html();

		// Make URLs absolute
		var absHost = document.location.href;
		var regex = /<\s*img [^\>]*src\s*=\s*(["\'])(.*?)\1/ig;
		var match;
		while( !!(match = regex.exec(html)) ) {
			var url = match[2];
			var fullUrl = toAbs(url, absHost);
			if( url !== fullUrl ) {
				// Drop in the replacement URL
				// This is a lousy version of string.replace but here we specify exact indices of the replacement
				// An alternative would be do to a replace all but would require converting "url" to a regex
				var before = html.slice(0, match.index);
				var after = html.slice(match.index + match[0].length);
				html = before + match[0].replace(url, fullUrl) + after;
				// Advance the index to account for the fullUrl being longer
				match.lastIndex += (fullUrl.length - url.length);
			}
		}

		html = '<body>' + html + '</body>';

		// Add styles
		var styles = '';
		if( !settings || !settings.noStyles ) {
			// Get raw styles
			$('style').each( function(idx,style) {
				styles += '<style type="text/css">'+$(this).html()+'</style>';
			});

			// Get links to styles
			$('link[rel="stylesheet"]').each( function(idx,style) {
				var href = $(this).attr('href');
				styles += '<link rel="stylesheet" type="text/css" href="'+toAbs(href,absHost)+'" />';
			});

			html = '<head>' + styles + '</head>' + html;
		}

		// Add enclosing html tags
		html = '<!DOCTYPE html><html>' + html + '</html>';


		// POST to capture service URL, encode settings as query string
		var query = settings ? $.param(settings) : "";

		aperture.io.rest("/capture?"+query, "POST", callbackWrapper(callback),
			{
				postData : html,
				contentType : "text/html"
			}
		);
	}


	return {
		
		/**
		 * On startup this can be called to initialize server side rendering for
		 * later use. This otherwise happens on the first request, causing a 
		 * significant delay.
		 */
		initialize : function() {
			aperture.io.rest('/capture/start', "GET");
		},
		
		/**
		 * Initiates an image capture of a given URL which will be stored in the aperture
		 * CMS. A callback is invoked with the URL that may be used to GET the image.
		 *
		 * @param {String} url 
		 *            URL of the page to be captured
		 * @param {Function(String)} callback
		 *            The callback to call when the image is ready.  On error will
		 *            be called with null.
		 * @param {Object} settings
		 *            A set of key/value pairs that configure the image
		 *            capture
		 * @param {Number} settings.captureWidth
		 *            If set, the virtual "screen" width
		 *            of content in which to render page in a virtual browser (in pixels).
		 *            If not set, the element's width will be used.
		 * @param {Number} settings.captureHeight
		 *            If set, the virtual "screen" height
		 *            of content in which to render page in a virtual browser (in pixels).
		 *            If not set, the element's height will be used.
		 * @param {String} settings.format
		 *            If set, specifies the image format to request.  May be one of
		 *            "JPEG", "PNG", or "SVG"
		 * @param {Object} authenticationSettings
		 *            A set of key/value pairs that configure the authentication of an 
		 *            image capture. If not set, no authentication will be used.
		 * @param {String} settings.username
		 *            The user name used for authentication.
		 * @param {String} settings.password
		 *            The password used for authentication.
		 */
		store : function( url, settings, authenticationSettings, callback ) {

			// create rest call	
			var params = '';
			
			if (settings) {
				params += '&'+ $.param(settings);
			}
			if (authenticationSettings) {
				params += '&'+ $.param(authenticationSettings);
			}
			
			var restCall = '/capture/store?page=' + encodeURIComponent(url) + params;
			
			aperture.io.rest(restCall, "GET", callbackWrapper(callback));
		},
		
		/**
		 * Creates a rest url that may be used to GET an image capture of a given URL.
		 *
		 * @param {String} url 
		 *            URL of the page to be captured
		 * @param {Function(String)} callback
		 *            The callback to call when the image is ready.  On error will
		 *            be called with null.
		 * @param {Object} settings
		 *            A set of key/value pairs that configure the image
		 *            capture
		 * @param {Number} settings.captureWidth
		 *            If set, the virtual "screen" width
		 *            of content in which to render page in a virtual browser (in pixels).
		 *            If not set, the element's width will be used.
		 * @param {Number} settings.captureHeight
		 *            If set, the virtual "screen" height
		 *            of content in which to render page in a virtual browser (in pixels).
		 *            If not set, the element's height will be used.
		 * @param {String} settings.format
		 *            If set, specifies the image format to request.  May be one of
		 *            "JPEG", "PNG", or "SVG"
		 * @param {Object} authenticationSettings
		 *            A set of key/value pairs that configure the authentication of an 
		 *            image capture. If not set, no authentication will be used.
		 * @param {String} settings.username
		 *            The user name used for authentication.
		 * @param {String} settings.password
		 *            The password used for authentication.
		 */
		inline : function( url, settings, authenticationSettings) {

			// create rest call	
			var params = '';
			
			if (settings) {
				params += '&'+ $.param(settings);
			}
			if (authenticationSettings) {
				params += '&'+ $.param(authenticationSettings);
			}
			
			return aperture.io.restUrl(
				'/capture/inline?page=' + encodeURIComponent(url) + params
			);
		}
	};
}());
/**
 * Source: layout.js
 * Copyright (c) 2014 Oculus Info Inc.
 * @fileOverview Aperture Layout API Implementations
 */

/**
 * @class Aperture layout APIs. Provides access to a server-side layout service. Depending on the layout type specified in the parameters,
 * the corresponding layout service and algorithm is chosen.<br>
 * <p>
 *  Each layout accepts a data object containing two arrays. The first contains a list of nodes, the second, a list of links between the nodes (if applicable). 
 *  <pre>{
 *   nodes : [],
 *   links : [],
 *  };
 *  </pre>
 *<span class="fixedFont"><span class="light">{Array} </span><b>nodes</b></span><br>
 *  An array of Node objects to be arranged by the layout service.<br>
 *  An example point object would be defined as follows:
 *<pre>{
 *   id: 'Node_1',
 *   width: 20, //The width of given node in the layout.
 *   height: 20 //The height of given node in the layout.
 *}</pre>
 *
 *<span class="fixedFont"><span class="light">{Array} </span><b>links</b></span><br>
 *  An array of of objects containing a source and target node id.<br>
 *  An example link object would be defined as follows:
 *<pre>{
 *   sourceId: 'Node_1',
 *   targetId: 'Node_2
 *}</pre><br>
 *  Additionally, an optional 'options' object, containing any customizations of layout attributes.
 *</p>
 * @requires an Aperture layout service, jQuery, json2 as a JSON shim if running old browsers<br>
*/
aperture.layout = (function(namespace) {
	var u = aperture.util,
	
	// common handler
	doLayout = function(type, data, extents, options, callback) {

		// in array, main layout options are first, then any other layout processes.
		if (u.isFunction(options)) {
			callback = options;
			options = undefined;
			
		// just the regular object form
		} else if (!u.isArray(options)) {
			if (options) {
				options.type = type;
				options = [options];
			} else {
				options = {type: type};
			}
		}
		
		aperture.io.rest('/layout', 'POST', callback, {
			postData : {
				nodes: data.nodes,
				links: data.links,
				extents: extents,
				layout: options
			},
			contentType: 'application/json'
		});			
	};
	
	
	/**
	 * @name aperture.layout.circle
	 * @function
	 * @description Arranges nodes around a central, circular path.		 
	 * @param {Object} data
	 *  The object containing the list of nodes and links.
	 * @param {Object} data.nodes
	 *  An array of node objects, with id and optional Number properties x, y, width, height and weight (indicating its scale of importance).
	 * @param {Object} data.links
	 *  An array of link objects, with sourceId and targetId properties.
	 * @param {Object} extents
	 *  The {width, height} extents of the layout space
	 * @param {Object} [options]
	 *  Object containing any customizations of various layout attributes.
	 * @param {Number} [options.linkLength]
	 *  The ideal minimum length of a given link in the layout
	 * @param {Function} callback
	 *  The callback for handling the response from the layout service.
	 *@returns
	 *  Node and link data, as well as additional
	 *  properties about the layout.
	 **/
	namespace.circle = function(data, extents, options, callback){
		doLayout('circle', data, extents, options, callback);
	};
	
	/**
	 * @name aperture.layout.radial
	 * @function
	 * @description Similar to the 'circle' layout, this arranges nodes around a circular path, however,
	 * nodes with high connectivity are made more visually prominent by isolating and positioning
	 * them as separate, satellite clusters around the central path.  
	 * @param {Object} data
	 *  The object containing the list of nodes and links.
	 * @param {Object} data.nodes
	 *  An array of node objects, with id and optional Number properties x, y, width, height and weight (indicating its scale of importance).
	 * @param {Object} data.links
	 *  An array of link objects, with sourceId and targetId properties.
	 * @param {Object} extents
	 *  The {width, height} extents of the layout space
	 * @param {Object} [options]
	 *  Object containing any customizations of various layout attributes.
	 * @param {Number} [options.linkLength]
	 *  The ideal minimum length of a given link in the layout
	 * @param {Function} callback
	 *  The callback for handling the response from the layout service.
	 *@returns
	 *  Node and link data, as well as additional
	 *  properties about the layout.
	 **/
	namespace.radial = function(data, extents, options, callback){
		doLayout('radial', data, extents, options, callback);
	};
	
	/**
	 * @name aperture.layout.organic
	 * @function
	 * @description The organic layout style is based on the force-directed layout paradigm. Nodes are 
	 * given mutually repulsive forces, and the connections between nodes are considered to be springs 
	 * attached to the pair of nodes. The layout algorithm simulates physical forces and rearranges the 
	 * positions of the nodes such that the sum of the forces emitted by the nodes and the links reaches
	 * a (local) minimum.
	 * <br> 
	 * Resulting layouts often expose the inherent symmetric and clustered structure of a graph, and have
	 * a well-balanced distribution of nodes with few edge crossings.
	 * @param {Object} data
	 *  The object containing the list of nodes and links.
	 * @param {Object} data.nodes
	 *  An array of node objects, with id and optional Number properties x, y, width, height and weight (indicating its scale of importance).
	 * @param {Object} data.links
	 *  An array of link objects, with sourceId and targetId properties.
	 * @param {Object} extents
	 *  The {width, height} extents of the layout space
	 * @param {Object} [options]
	 *  Object containing any customizations of various layout attributes.
	 * @param {Number} [options.nodeDistance]
	 *  The ideal minimum spacing between nodes
	 * @param {Number} [options.linkLength]
	 *  The ideal minimum length of a given link in the layout
	 * @param {Function} callback
	 *  The callback for handling the response from the layout service.
	 *@returns
	 *  Node and link data, as well as additional
	 *  properties about the layout.
	 **/
	namespace.organic = function(data, extents, options, callback){
		doLayout('organic', data, extents, options, callback);
	};
	
	/**
	 * @name aperture.layout.vtree
	 * @function
	 * @description Arranges the nodes top-down, as a hierarchical, vertical tree.
	 * @param {Object} data
	 *  The object containing the list of nodes and links.
	 * @param {Object} data.nodes
	 *  An array of node objects, with id and optional Number properties x, y, width, height and weight (indicating its scale of importance).
	 * @param {Object} data.links
	 *  An array of link objects, with sourceId and targetId properties.
	 * @param {Object} extents
	 *  The {width, height} extents of the layout space
	 * @param {Object} [options]
	 *  Object containing any customizations of various layout attributes.
	 * @param {String} [options.bottomToTop]
	 *  If true, reverses the layout direction of the tree
	 * @param {Number} [options.nodeDistance]
	 *  The ideal minimum spacing between nodes
	 * @param {Number} [options.treeLevelDistance]
	 *  The ideal distance between levels of the tree
	 * @param {Function} callback
	 *  The callback for handling the response from the layout service.
	 *@returns
	 *  Node and link data, as well as additional
	 *  properties about the layout.
	 **/
	namespace.vtree = function(data, extents, options, callback){
		doLayout('vtree', data, extents, options, callback);
	};
	
	/**
	 * @name aperture.layout.htree
	 * @function
	 * @description Arranges the nodes left to right, as a hierarchical, horizontal tree.
	 * @param {Object} data
	 *  The object containing the list of nodes and links.
	 * @param {Object} data.nodes
	 *  An array of node objects, with id and optional Number properties x, y, width, height and weight (indicating its scale of importance).
	 * @param {Object} data.links
	 *  An array of link objects, with sourceId and targetId properties.
	 * @param {Object} extents
	 *  The {width, height} extents of the layout space
	 * @param {Object} [options]
	 *  Object containing any customizations of various layout attributes.
	 * @param {String} [options.rightToLeft]
	 *  If true, reverses the layout direction of the tree
	 * @param {Number} [options.nodeDistance]
	 *  The ideal minimum spacing between nodes
	 * @param {Number} [options.treeLevelDistance]
	 *  The ideal distance between levels of the tree
	 * @param {Function} callback
	 *  The callback for handling the response from the layout service.
	 * @returns
	 *  Node and link data, as well as additional
	 *  properties about the layout.
	 **/
	namespace.htree = function(data, extents, options, callback){
		doLayout('htree', data, extents, options, callback);
	};

	/**
	 * @name aperture.layout.tag
	 * @function
	 * @description Executes a deconflicted layout of node tags. Tag layout
	 *  can be used to strategically label (or otherwise graphically annotate) only 
	 *  the most important nodes in a dense display at a readable scale without occlusion. 
	 *  If the nodes have not yet been laid out, an alternative
	 *  to using this method is to use the multipass layout method. 
	 * 
	 *  The alignments that the implementation may consider for the annotation
	 *  may be specified by the alignments option. Alignments are: any, 
	 *  topAny, bottomAny, leftAny, rightAny, 
	 *  bottomLeft, bottomCenter, bottomRight, middleLeft, middleRight,
	 *  topLeft, topCenter, or topRight.
	 *
	 * @param {Object} data
	 *  The object containing the list of nodes.
	 * @param {Object} data.nodes
	 *  An array of node objects, with id and Number properties x, y, and weight (indicating its scale of importance).
	 * @param {Object} extents
	 *  The {width, height} extents of the layout space
	 * @param {Object} [options]
	 *  Object containing any customizations of various layout attributes.
	 * @param {Number} [options.tagWidth=100]
	 *  The width reserved for each annotation.
	 * @param {Number} [options.tagHeight=15]
	 *  The height reserved for each annotation.
	 * @param {String= 'any'} [options.alignments='any']
	 *  The alignments that the implementation may choose from.
	 * @param {String= 'default'} [options.defaultAlignment='default']
	 *  The default alignment, used only when a node's annotation will be obscured and
	 *  is thus flagged with visible = false. This option is only useful when the caller
	 *  uses deconfliction to find the optimal position for annotations but still wishes
	 *  to always display all of them.
	 * 
	 * @param {Function} callback
	 *  The callback for handling the response from the layout service.
	 * @returns
	 *  Node and link data, as well as additional
	 *  properties about the layout.
	 **/
	namespace.tag = function(data, extents, options, callback){
		doLayout('tag', data, extents, options, callback);
	};

	/**
	 * @name aperture.layout.multipass
	 * @function
	 * @description 
	 * 
	 * Executes a series of layouts, such as a node layout followed by a tag layout.
	 *
	 * @param {Object} data
	 *  The object containing the list of nodes.
	 * @param {Object} data.nodes
	 *  An array of node objects, with id and Number properties x, y, and weight (indicating its scale of importance).
	 * @param {Object} [data.links]
	 *  An array of link objects, with sourceId and targetId properties.
	 * @param {Object} extents
	 *  The {width, height} extents of the layout space
	 * @param {Array} layouts
	 *  An array of layout objects, each with at minimum a field of name type indicating
	 *  the type of layout, and optionally any other fields indicating options.
	 * 
	 * @param {Function} callback
	 *  The callback for handling the response from the layout service.
	 * @returns
	 *  Node and link data, as well as additional
	 *  properties about the layout.
	 **/
	namespace.multipass = function(data, extents, layouts, callback){
		doLayout(null, data, extents, layouts, callback);
	};


	return namespace;
	
}(aperture.layout || {}));/**
 * Source: pubsub.js
 * Copyright (c) 2014 Oculus Info Inc.
 * @fileOverview Aperture Publish / Subscribe (PubSub) API implementation
 */

/**
 * @namespace Aperture Publish / Subscribe (PubSub) API.
 * @requires OpenAjax Hub
 */
aperture.pubsub = (function() {

	/**
	 * @private
	 * Used for all propogated calls.
	 */
	function callHub( method, args ) {
		if (window.OpenAjax && OpenAjax.hub) {
			return OpenAjax.hub[method].apply( OpenAjax.hub, Array.prototype.slice.call(args, 0) );
		}
		else {
			aperture.log.error('An OpenAjax hub is required for aperture.pubsub, such as that provided by OpenAjaxUnmanagedHub.js.');
		}
	}
	
	/**
	 * Use the OpenAjax Hub for aperture pub sub
	 */
	return {
		
		/**
		 * Publishes a message
		 * @param {String} topic
		 *      The named message topic
		 * 
		 * @param message 
		 *      The payload of the message
		 * 
		 * @name aperture.pubsub.publish
		 * @function
		 */
		publish : function () {
			return callHub( 'publish', arguments );
		},

		/**
		 * Subscribes to a message topic
		 * @param {String} topic
		 *      The named message topic
		 * 
		 * @param {Function} handler 
		 *      The message handler function.
		 * 
		 * @param {Object} [context]
		 *      The optional context that will be the value of this when 
		 *      the handler is invoked.
		 * 
		 * @param {*} [subscriberData]
		 *      The optional data to pass as an argument to the handler.
		 * 
		 * @returns {String}
		 *      A subscription id to use for unsubscription.
		 * 
		 * @name aperture.pubsub.subscribe
		 * @function
		 */
		subscribe : function () {
			return callHub( 'subscribe', arguments );
		},
		
		/**
		 * Unsubscribes from a previous subscription.
		 * 
		 * @param {String} subscriptionId
		 *      A subscription id returned by a call to subscribe.
		 * 
		 * @name aperture.pubsub.unsubscribe
		 * @function
		 */
		unsubscribe : function () {
			return callHub( 'unsubscribe', arguments );
		}
	};
}());
/**
 * Source: store.js
 * Copyright (c) 2014 Oculus Info Inc.
 * @fileOverview Aperture Content Service API
 */

/**
 * @namespace Functions used to store, get, and delete documents in a content store.
 * @requires an Aperture CMS service
 * @requires jQuery
 */
aperture.store = (function() {


	return {
		/**
		 * Store a data item in the CMS.
		 * @param {String|Object} data the data item to store.  Can be a string or a javascript object.
		 * If a string it will be stored as is.  If an object, it will be converted to JSON
		 * and stored.
		 *
		 * @param {Object} [descriptor] an optional descriptor object that specifies the cms
		 * store, document id, and document revision.
		 * @param {String} [descriptor.store] the name of the content store in which to store the
		 * document.  If not provided, the default will be used.
		 * @param {String} [descriptor.id] the id of the document to store.  If this is a new document
		 * this will try and use this id for the document when storing.  If this is an existing document
		 * being updated this id specifies the id of the document to update.
		 * @param {String} [descriptor.rev] the revision of the document to store.  If updating a document
		 * this must be set to the current revision to be allowed to perform the update.  This prevents
		 * updating a document with out of date information.
		 *
		 * @param {Function(descriptor)} [callback] a function to be called after the store command completes on the server.  The
		 * callback will be given a descriptor object in the same format as the descriptor to the store function
		 * on success.  The descriptor describes the successfully stored document.
		 */
		store : function(data, descriptor, callback) {
			var innerCallback = callback && function( result, info ) {
				if( info.success ) {
					var location = info.xhr && info.xhr.getResponseHeader && info.xhr.getResponseHeader("Location");
					// Call the callback with a hash that describes the stored document
					// and provides a URL to it
					callback( {
							id: result.id,
							rev: result.rev,
							store: result.store,
							url: location
						});
				} else {
					// Failure
					// TODO Provide reason why?
					callback( null );
				}
			};

			// Extend descriptor defaults
			descriptor = aperture.util.extend({
				// TODO Get from config
				store: 'aperture'
				// id: none
				// rev: none
			}, descriptor);

			// Use the given content type or try to detect
			var contentType = descriptor.contentType ||
				// String data
				(aperture.util.isString(data) && 'text/plain') ||
				// JS Object, use JSON
				'application/json';

			// TODO URI pattern from config service?
			// Construct the uri
			var uri = '/cms/'+descriptor.store;
			// Have a given id?  Use it
			if( descriptor.id ) {
				uri += '/'+descriptor.id;
			}
			// Have a rev?  Use it
			if( descriptor.rev ) {
				uri += '?rev='+descriptor.rev;
			}

			// Make the call
			aperture.io.rest(uri, "POST", innerCallback, {
				postData: data,
				contentType: contentType
			});
		},

		/**
		 * Gets the url of a document in the store given a descriptor.
		 *
		 * @param {Object} descriptor an object describing the document to get
		 * @param {String} [descriptor.store] the name of the content store to use.  If not
		 * provided the default will be used.
		 * @param {String} descriptor.id the id of the document to get
		 * @param {String} [descriptor.rev] the revision of the document to get.  If not
		 * provided, the most recent revision will be retrieved.
		 */
		getURL : function(descriptor) {
			if( !descriptor || descriptor.id == null || descriptor.id === '' ) {
				aperture.log.error('get from store must specify an id');
				return;
			}

			// TODO Get from config
			descriptor.store = descriptor.store || 'aperture';

			// Construct the url
			var url = '/cms/'+descriptor.store+'/'+descriptor.id;
			// Have a rev?  Use it
			if( descriptor.rev ) {
				url += '?rev='+descriptor.rev;
			}

			return url;
		},
		
		/**
		 * Gets a document from the server given a descriptor.
		 *
		 * @param {Object} descriptor an object describing the document to get
		 * @param {String} [descriptor.store] the name of the content store to use.  If not
		 * provided the default will be used.
		 * @param {String} descriptor.id the id of the document to get
		 * @param {String} [descriptor.rev] the revision of the document to get.  If not
		 * provided, the most recent revision will be retrieved.
		 *
		 * @param {Function(data,descriptor)} [callback] a callback to be called when the document
		 * data is available.  The callback will be provided with the data and a hash of the
		 * document descriptor.
		 */
		get : function(descriptor, callback) {
			var url = getURL(descriptor);
			
			if (url) {
				var innerCallback = callback && function( result, info ) {
					if( info.success ) {
						// Call user's callback with the document data
						// TODO Get the latest revision via ETAG
						callback( result, descriptor );
					} else {
						// TODO Better error handling?
						callback(null, descriptor);
					}
				};

				// Make the call
				aperture.io.rest(uri, "GET", innerCallback);
				
			} else {
				callback(null, descriptor);
			}
		}
	};

}());/**
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

aperture.tooltip = (function(ns) {

	var tooltipExists = false;
	var tooltipDiv = null;
	var tooltipInnerDiv = null;
	var tooltipID = "apertureTooltip";
	var tooltipTimer = null;
	var tooltipPending = false;
	var tooltipVisible = false;
	
	var overridingMouseMove = false;
	var oldMouseMove = null;
	
	var assertTooltip = function() {
		if (!tooltipExists) {
			tooltipDiv = document.createElement("div");
			tooltipDiv.style.zIndex = '999999999';
			tooltipDiv.style.position = 'absolute';
			tooltipDiv.id = tooltipID;
			tooltipDiv.style.display = "none";
			
			tooltipInnerDiv = document.createElement("div");
			tooltipInnerDiv.setAttribute("class", "apertureTooltip");

			tooltipDiv.appendChild(tooltipInnerDiv);
			
			window.document.body.appendChild(tooltipDiv);
			tooltipExists = true;
		}
	};
	
	var positionTooltip = function(posx,posy) {
		var w = $(window).width();
		var h = $(window).height();
		var ew = 'E';
		var ns = 'S';
		if (posx<w/2) {
			tooltipDiv.style.left = (posx-30-2) + "px";
			tooltipDiv.style.right = '';
			ew = 'E';
		} else {
			tooltipDiv.style.left = '';
			tooltipDiv.style.right = (w-(posx+30-2)) + "px";
			ew = 'W';
		}
		if (posy>h/2) {
			tooltipDiv.style.top = "";
			tooltipDiv.style.bottom = (h-posy+10) + "px";
			ns = 'N';
		} else {
			tooltipDiv.style.top = (posy+10+2) + "px";
			tooltipDiv.style.bottom = "";
			ns = 'S';
		}
		tooltipInnerDiv.setAttribute("class", "apertureTooltip"+ns+ew);
	}
	
	var setTooltipVisible = function(spec, posx, posy) {
		positionTooltip(posx, posy);
		tooltipDiv.style.display = "";
		tooltipPending = false;
		tooltipVisible = true;
	};
	
	var getEventXY = function(e) {
		var posx=0, posy=0;
		if (e.pageX || e.pageY) 	{
			posx = e.pageX;
			posy = e.pageY;
		} else if (e.clientX || e.clientY) 	{
			posx = e.clientX + document.body.scrollLeft
				+ document.documentElement.scrollLeft;
			posy = e.clientY + document.body.scrollTop
				+ document.documentElement.scrollTop;
		}
		return [posx,posy];
	};
	
	var overrideMouseMove = function(target) {
		if (!overridingMouseMove) {
			oldMouseMove = document.onmousemove;
			document.onmousemove = function(event) {
				var pos = getEventXY(event);
				positionTooltip(pos[0], pos[1]);
				return true;
			};
			overridingMouseMove = true;
		}
	};

	var cancelMouseMoveOverride = function() {
		if (overridingMouseMove) {
			document.onmousemove = oldMouseMove;
			overridingMouseMove = false;
		}
	};
	
	var cancelTooltip = function() {
		if (tooltipPending) {
			clearTimeout(tooltipTimer);
			tooltipPending = false;
		}
		tooltipDiv.style.display = "none";
		tooltipVisible = false;
		cancelMouseMoveOverride();
	};

	ns.showTooltip = function(spec) {
		var pos = getEventXY(spec.event.source);
		
		assertTooltip();
		if (tooltipVisible) {
			if (tooltipInnerDiv.innerHTML==spec.html) {
				return;
			}
		}
		cancelTooltip();
		tooltipInnerDiv.innerHTML = spec.html;
		if (spec.delay) {
			tooltipPending = true;
			tooltipTimer = setTimeout(function(){setTooltipVisible(spec, pos[0], pos[1]);}, spec.delay);
		} else {
			setTooltipVisible(spec, pos[0], pos[1]);
		}
		overrideMouseMove(spec.event.source.target);
	};
	
	ns.hideTooltip = function() {
		assertTooltip();
		cancelTooltip();
	};
	
	return ns;
	
}(aperture.tooltip || {}));/**
 * Source: map.js
 * Copyright (c) 2014 Oculus Info Inc.
 * @fileOverview Aperture Map APIs
 */

/**
 * @namespace Geospatial vizlet layers. If not used the geospatial package may be excluded.
 * @requires OpenLayers
 */
aperture.geo = (
/** @private */
function(ns) {

	// util is always defined by this point
	var util = aperture.util, ol = 'OPEN_LAYERS_CANVAS';

	// Searchers through a set of layers to find
	// the base layer's index.
	var getBaseLayerIndex = function(map) {
	    var i, layers = map.layers;
	    for(i=0; i < layers.length; i++){
	        if(layers[i].isBaseLayer==true){
	            return(i);
	        }
	    }
	};
	
	// if basic Canvas ever implements stuff for real we should override where it makes sense
	var OpenLayersCanvas = aperture.canvas.Canvas.extend( 'aperture.geo.OpenLayersCanvas', {
			init : function(root, map) {
				aperture.canvas.Canvas.prototype.init.call(this, root);
				this.olMap_ = map;
			}
		}
	);

	aperture.canvas.handle( ol, OpenLayersCanvas );

    /**
     * @private
     * Base of Map Layer classes
     */
	var OpenLayersMapLayer = aperture.Layer.extend( '[private].OpenLayersMapLayer', {
		init : function(spec, mappings) {
			aperture.Layer.prototype.init.call(this, spec, mappings);

			if (spec.extent) {
				spec.extent = OpenLayers.Bounds.fromArray(spec.extent);
			}
			if ( !this.canvas_ ) {
				throw new Error('Map layer must be constructed by a parent layer through an addLayer call');
			}
		},

		/**
		 * OpenLayers layer
		 */
		olLayer_ : null, // Assumption that a single OpenLayers layer can be used for all rendering


		/**
		 * Canvas type is OpenLayers
		 */
		canvasType : ol,

		/**
		 * @private
		 */
		data : function(value) {
			if( value ) {
				throw new Error('Cannot add data to a base map layer');
			}
		},

		/**
		 * @private
		 */
		render : function(changeSet) {
			// Must force no render logic so the layer doesn't try to monkey around with data
		},
        
		/**
		 * @private
		 */
		remove : function() {
			aperture.Layer.prototype.remove.call(this);

			// hook into open layers to remove
			this.canvas_.olMap_.removeLayer(this.olLayer_);
		}
	});
    
	
	// deprecated
	var tileTypeAliases = {
			tms : 'TMS',
			wms : 'WMS'
		};
    
    
	var MapTileLayer = OpenLayersMapLayer.extend( 'aperture.geo.MapTileLayer', 
	/** @lends aperture.geo.MapTileLayer# */
	{
		/**
		 * @class The base class for Aperture Map layers that displays one or more image tiles 
		 * from one of a variety of standards based sources.
		 *
		 * @augments aperture.Layer
		 * @constructs
		 * @factoryMade
		 */
		init : function(spec, mappings) {
			OpenLayersMapLayer.prototype.init.call(this, spec, mappings);

			spec.options = spec.options || {};
			
			if (spec.options.isBaseLayer == null) {
				spec.options.isBaseLayer = false;
			}
		}		
	});

    ns.MapTileLayer = MapTileLayer;

	ns.MapTileLayer.TMS = MapTileLayer.extend( 'aperture.geo.MapTileLayer.TMS', 
	/** @lends aperture.geo.MapTileLayer.TMS# */
	{
		/**
		 * @class A Tile Mapping Service (TMS) specification. TMS relies on client information to
		 * be supplied about extents and available zoom levels but can be simply stood up
		 * as a service by deploying a static set of named tiles.
		 *
		 * @augments aperture.geo.MapTileLayer
		 * @constructs
		 *
		 * @description MapTileLayers may be configured as base layers in 
		 * {@link aperture.geo.Map Map} construction,
		 * or later added as overlay layers by calling
		 * {@link aperture.PlotLayer#addLayer addLayer} on a parent layer.
		 * This layer constructor is never called directly.
		 * 
		 * @example
		 * var spec = {
		 *     name : 'My TMS Layer',
		 *     url : 'http://mysite/mytiles/',
		 *     options : {
		 *         layername: 'mynamedlayer',
		 *         type: 'png'
		 *     }
		 * };
		 * 
		 * // EXAMPLE ONE: create a map and explicitly set the base tile layer
		 * var map = new Map({
		 *      options : {
		 *          'projection': 'EPSG:900913',
		 *          'displayProjection': 'EPSG:900913',
		 *          'units': 'm',
		 *          'numZoomLevels': 9,
		 *          'maxExtent': [
		 *              -20037500,
		 *              -20037500,
		 *              20037500,
		 *              20037500
		 *           ]
		 *      },
		 *      baseLayer : {
		 *          TMS: spec
		 *      }
		 * });
		 * 
		 * // EXAMPLE TWO: overlay a layer on a map with an existing base layer
		 * map.addLayer( aperture.geo.MapTileLayer.TMS, {}, spec );
		 * 
		 * @param {Object} spec
		 *      a specification object
		 *      
		 * @param {String} spec.name
		 *      the local name to give the layer.
		 *      
		 * @param {String} spec.url
		 *      the source url for the tiles.
		 *      
		 * @param {Object} spec.options
		 *      implementation specific options.
		 *      
		 * @param {String} spec.options.layername
		 *      required name of the served layer to request of the source tile data.
		 *      
		 * @param {String} spec.options.type
		 *      required type of the images in the source tile data.

		 */
		init : function(spec, mappings) {
			MapTileLayer.prototype.init.call(this, spec, mappings);

			this.olLayer_ = new OpenLayers.Layer.TMS(
				spec.name || 'TMS ' + this.uid,
                [spec.url],
                spec.options
			);
			
			this.canvas_.olMap_.addLayer(this.olLayer_);
		}		
	});
	

	ns.MapTileLayer.WMS = MapTileLayer.extend( 'aperture.geo.MapTileLayer.WMS', 
	/** @lends aperture.geo.MapTileLayer.WMS# */
	{
		/**
		 * @class A Web Map Service (WMS) specification. TMS relies on client information to
		 * be supplied about extents and available resolutions but are simple to stand
		 * up as a service by deploying a static set of named tiles.
		 *
		 * @augments aperture.geo.MapTileLayer
		 * @constructs
		 *
		 * @description MapTileLayers may be configured as base layers in 
		 * {@link aperture.geo.Map Map} construction,
		 * or later added as overlay layers by calling
		 * {@link aperture.PlotLayer#addLayer addLayer} on a parent layer.
		 * This layer constructor is never called directly.
		 * 
		 * @example
		 * var spec = {
		 *     name: 'OSGeo WMS',
		 *     url:  'http://vmap0.tiles.osgeo.org/wms/vmap0',
		 *     options: {
		 *         layers : 'basic',
		 *         projection : 'EPSG:4326',
		 *         displayProjection : 'EPSG:4326'
		 *     }
		 * };
		 * 
		 * // EXAMPLE ONE: create a map and explicitly set the base tile layer
		 * var map = new Map({
		 *      baseLayer : {
		 *          WMS: spec
		 *      }
		 * });
		 * 
		 * // EXAMPLE TWO: overlay a layer on a map with an existing base layer
		 * map.addLayer( aperture.geo.MapTileLayer.WMS, {}, spec );
		 * 
		 * @param {Object} spec
		 *      a specification object
		 *      
		 * @param {String} spec.name
		 *      the local name to give the layer.
		 *      
		 * @param {String} spec.url
		 *      the source url for the tiles.
		 *      
		 * @param {Object} spec.options
		 *      implementation specific options.
		 *      
		 * @param {String} spec.options.layers
		 *      a single layer name or comma separated list of served layer names to request.
		 */
		init : function(spec, mappings) {
			MapTileLayer.prototype.init.call(this, spec, mappings);

			this.olLayer_ = new OpenLayers.Layer.WMS(
				spec.name || 'WMS ' + this.uid,
                spec.url,
                spec.options
			);
			
			this.canvas_.olMap_.addLayer(this.olLayer_);
		}		
	});
	
	
	
	ns.MapTileLayer.Google = MapTileLayer.extend( 'aperture.geo.MapTileLayer.Google', 
	/** @lends aperture.geo.MapTileLayer.Google# */
	{
		/**
		 * @class A Google Maps service. Use of this layer requires the inclusion of the
		 * <a href="https://developers.google.com/maps/documentation/javascript/" target="_blank">Google Maps v3 API</a> script
		 * and is subject to its terms of use. Map options include dynamically 
		 * <a href="https://developers.google.com/maps/documentation/javascript/styling" target="_blank">styled maps</a>.
		 * 
		 * @augments aperture.geo.MapTileLayer
		 * @constructs
		 *
		 * @description MapTileLayers may be configured as base layers in 
		 * {@link aperture.geo.Map Map} construction,
		 * or later added as overlay layers by calling
		 * {@link aperture.PlotLayer#addLayer addLayer} on a parent layer.
		 * This layer constructor is never called directly.
		 * 
		 * @example
		 * var spec = {
		 *     name: 'My Layer',
		 *     options: {
		 *          type: google.maps.MapTypeId.TERRAIN
		 *     }
		 * };
		 * 
		 * // EXAMPLE ONE: create a map and explicitly set the base tile layer
		 * var map = new Map({
		 *      baseLayer : {
		 *          Google: spec
		 *      }
		 * });
		 * 
		 * // EXAMPLE TWO: overlay a layer on a map with an existing base layer
		 * map.addLayer( aperture.geo.MapTileLayer.Google, {}, spec );
		 * 
		 * // EXAMPLE THREE: create a styled map
		 * var map = new Map({
		 *      baseLayer : {
		 *          Google: {
		 *              name: 'My Layer',
		 *              options: {
		 *                  type: 'styled',
		 *                  style: [{
		 *                      stylers: [
		 *                          { saturation: -80 }
		 *                      ]
		 *                  }]
		 *              }
		 *          }
		 *      }
		 * });
		 * 
		 * @param {Object} spec
		 *      a specification object
		 *      
		 * @param {String} spec.name
		 *      the local name to give the layer.
		 *      
		 * @param {Object} spec.options
		 *      implementation specific options.
		 *      
		 * @param {google.maps.MapTypeId|'styled'} spec.options.type
		 *      a Google defined layer type to request.
		 *      
		 * @param {Array} spec.options.style
		 *      a list of Google defined
		 *      <a href="https://developers.google.com/maps/documentation/javascript/styling" target="_blank">style rules</a>.
		 */
		init : function(spec, mappings) {
			MapTileLayer.prototype.init.call(this, spec, mappings);

			this.olLayer_ = new OpenLayers.Layer.Google(
				spec.name || 'Google ' + this.uid,
				spec.options
			);
			
			this.canvas_.olMap_.addLayer(this.olLayer_);
			
			if (spec.options.type == 'styled') {
				var styledMapType = new google.maps.StyledMapType(spec.options.style, {name: 'Styled Map'});

				this.olLayer_.mapObject.mapTypes.set('styled', styledMapType);
				this.olLayer_.mapObject.setMapTypeId('styled');
			}			
		}		
	});
			
	
	
	ns.MapTileLayer.Bing = MapTileLayer.extend( 'aperture.geo.MapTileLayer.Bing', 
	/** @lends aperture.geo.MapTileLayer.Bing# */
	{
		/**
		 * @class A Bing (Microsoft) map service. Use of a Bing map layer 
		 * <a href="http://bingmapsportal.com/" target="_blank">requires a key</a>.

		 * @augments aperture.geo.MapTileLayer
		 * @constructs
		 *
		 * @description MapTileLayers may be configured as base layers in 
		 * {@link aperture.geo.Map Map} construction,
		 * or later added as overlay layers by calling
		 * {@link aperture.PlotLayer#addLayer addLayer} on a parent layer.
		 * This layer constructor is never called directly.
		 * 
		 * @example
		 * var spec = {
		 *     name: 'My Layer',
		 *     options: {
		 *          type: 'Road',
		 *          key: 'my-license-key-here'
		 *     }
		 * };
		 * 
		 * // EXAMPLE ONE: create a map and explicitly set the base tile layer
		 * var map = new Map({
		 *      baseLayer : {
		 *          Bing: spec
		 *      }
		 * });
		 * 
		 * // EXAMPLE TWO: overlay a layer on a map with an existing base layer
		 * map.addLayer( aperture.geo.MapTileLayer.Bing, {}, spec );
		 * 
		 * @param {Object} spec
		 *      a specification object
		 *      
		 * @param {String} spec.name
		 *      the local name to give the layer.
		 *      
		 * @param {Object} spec.options
		 *      implementation specific options.
		 *      
		 * @param {String='Road'|'Aerial'|...} spec.options.type
		 *      the name of a Bing defined layer type to request.
		 *      
		 * @param {String} spec.options.key
		 *      a client license key, obtained from Microsoft.
		 */
		init : function(spec, mappings) {
			MapTileLayer.prototype.init.call(this, spec, mappings);

			spec.options.name = spec.options.name || spec.name || 'Bing ' + this.uid;
			
			this.olLayer_ = new OpenLayers.Layer.Bing(
				spec.options
			);
			
			this.canvas_.olMap_.addLayer(this.olLayer_);
		}		
	});

	
	ns.MapTileLayer.Image = MapTileLayer.extend( 'aperture.geo.MapTileLayer.Image', 
	/** @lends aperture.geo.MapTileLayer.Image# */
	{
		/**
		 * @class A single image.

		 * @augments aperture.geo.MapTileLayer
		 * @constructs
		 *
		 * @description MapTileLayers may be configured as base layers in 
		 * {@link aperture.geo.Map Map} construction,
		 * or later added as overlay layers by calling
		 * {@link aperture.PlotLayer#addLayer addLayer} on a parent layer.
		 * This layer constructor is never called directly.
		 * 
		 * @example
		 * var spec = {
		 *     name: 'My Layer',
		 *     url: 'http://mysite/myimage.png',
		 *     size: [1024, 1024], // width and height in pixels
		 *     extent: [
		 *        -20037500, // left
		 *        -20037500, // bottom
		 *         20037500, // right
		 *         20037500  // top
		 *     ]
		 * };
		 * 
		 * // EXAMPLE: overlay a layer on a map with an existing base layer
		 * map.addLayer( aperture.geo.MapTileLayer.Image, {}, spec );
		 * 
		 * @param {Object} spec
		 *      a specification object
		 *      
		 * @param {String} spec.name
		 *      the local name to give the layer.
		 *      
		 * @param {String} spec.url
		 *      the source url for the image.
		 *      
		 * @param {Array(Number)} spec.size
		 *      an array of two numbers specifying width and height
		 *      of the image in pixels.
		 *      
		 * @param {Array(Number)} spec.extent
		 *      an array of numbers specifying the geographical
		 *      bounding region of the image. The expected order is: [left, bottom, right, top]
		 * };
		 */
		init : function(spec, mappings) {
			MapTileLayer.prototype.init.call(this, spec, mappings);

			var options = spec.options;
			
			if (spec.size) {
				spec.size = new OpenLayers.Size(spec.size[0], spec.size[1]);
			}
	        
			if (!options.isBaseLayer) {

				// clone from base layer
				if (!options.resolutions) {
					options.resolutions = this.canvas_.olMap_.layers[getBaseLayerIndex(this.canvas_.olMap_)].resolutions;
				}
				if (!options.maxResolution) {
					options.maxResolution = options.resolutions[0];
				}
		
				if (spec.projection) {
					var tmpFromProjection = new OpenLayers.Projection(spec.projection);
					var tmpToProjection = new OpenLayers.Projection(this.canvas_.olMap_.projection.projCode);
					spec.extent = spec.extent.clone().transform(tmpFromProjection, tmpToProjection);
				}
	        }
	        
			this.olLayer_ = new OpenLayers.Layer.Image(
	            spec.name || 'Image ' + this.uid,
	            spec.url,
	            spec.extent,
	            spec.size,
	            options
	        );
	        
			this.canvas_.olMap_.addLayer(this.olLayer_);
		}		
	});
	
    /**
     * @private
     * Blank Layer
     *
     * Needed an option to have an empty baselayer, especially good if the
     * tiles are not geographically-based.
     * This layer is not exposed right now, may never be.  Used internally by map layer
     */
	var BlankMapLayer = OpenLayersMapLayer.extend( '[private].BlankMapLayer', {
		init : function(spec, mappings) {
			OpenLayersMapLayer.prototype.init.call(this, spec, mappings);

			this.olLayer_ = new OpenLayers.Layer('BlankBase');
			this.olLayer_.isBaseLayer = true; // OpenLayers.Layer defaults to false.
			this.olLayer_.extent = spec.baseLayer.extent || spec.options.extent || spec.options.maxExtent;

			this.canvas_.olMap_.addLayer(this.olLayer_);
		}
	});


	/**********************************************************************/
	/*
	 * The list of OpenLayers vector layer styles that can be mapped in Aperture
	 */
	var availableStyles = {
			'fillColor' : 'fill',
			'fillOpacity': 'opacity',
			'strokeColor': 'stroke',
			'strokeOpacity': 'stroke-opacity',
			'strokeWidth': 'stroke-width',
			'strokeLinecap': 'stroke-linecap',
			'strokeDashstyle': 'stroke-style', // needs translation?
//			'graphicZIndex', ??
			'label': 'label',
			'pointRadius': 'radius',
			'cursor': 'cursor',
			'externalGraphic': '' // overridden below
	};

	/*
	 * Default values for all settable styles (used if not mapped)
	 * TODO Allow this to be overridden by configuration
	 */
	var vectorStyleDefaults = {
		fillColor: '#999999',
		fillOpacity: '1',
		strokeColor: '#333333',
		strokeOpacity: '1',
		strokeWidth: 1,
		strokeLinecap: 'round',
		strokeDashstyle: 'solid',
		graphicZIndex: 0,
		// Must have a non-undefined label or else OpenLayers writes "undefined"
		label: '',
		// Must have something defined here or IE throws errors trying to do math on "undefined"
		pointRadius: 0,
		cursor: ''
	};

	/*
	 * Styles that are fixed and cannot be altered
	 * TODO Allow this to be overridden by configuration
	 */
	var fixedStyles = {
		fontFamily: 'Arial, Helvetica, sans-serif',
		fontSize: 10

		// If we allow the following to be customizable by the user
		// this prevents us from using the default of the center of the image!
		//graphicXOffset:
		//graphicYOffset:
	};

	// returns private function for use by map external layer
	var makeHandler = (function() {
		
		// event hooks for features.
		function makeCallback( type ) {
			var stopKey;
			
			switch (type) {
			case 'click':
			case 'dblclick':
				stopKey = 'stopClick';
				break;
			case 'mousedown':
			case 'touchstart': // ?
				stopKey = 'stopDown';
				break;
			case 'mouseup':
				stopKey = 'stopUp';
				break;
			}
			if (stopKey) {
				return function(feature) {
					this.handler_[stopKey] = this.trigger(type, {
						data: feature.attributes,
						eventType: type
					});
				};
			} else {
				return function(feature) {
					this.trigger(type, {
						data: feature.attributes,
						eventType: type
					});
				};
			}
		}
	
		var featureEvents = {
			'mouseout' : 'out',
			'mouseover' : 'over'
		};
		
		return function (events) {
			var handlers = {}, active;
			
			if (this.handler_) {
				this.handler_.deactivate();
				this.handler_= null;
			}
			
			aperture.util.forEach(events, function(fn, event) {
				handlers[ featureEvents[event] || event ] = makeCallback(event);
				active = true;
			}); 

			if (active) {
				this.handler_ = new OpenLayers.Handler.Feature(
					this, this._layer, handlers,
					{ map: this.canvas_.olMap_, 
						stopClick: false,
						stopDown: false,
						stopUp: false
					}
				);
				this.handler_.activate();
			}
		};
	}());
	
	var MapGISLayer = aperture.Layer.extend( 'aperture.geo.MapGISLayer',
	/** @lends aperture.geo.MapGISLayer# */
	{
		/**
		 * @class An Aperture Map layer that sources GIS data from an external data source such
		 * as KML, GML, or GeoRSS.  Visual properties of this layer are mapped like any
		 * other layer where the data available for mapping are attributes of the features
		 * loaded from the external source.
		 *
		 * @mapping {String} fill
		 *   The fill color of the feature
		 * 
		 * @mapping {String} stroke
		 *   The line color of the feature
		 *   
		 * @mapping {Number} stroke-opacity
		 *   The line opacity of the feature as a value from 0 (transparent) to 1.
		 *   
		 * @mapping {Number} stroke-width
		 *   The line width of the feature.
		 *   
		 * @mapping {String} label
		 *   The label of the feature.
		 *   
		 * @mapping {Number} radius
		 *   The radius of the feature.
		 *   
		 * @mapping {String} cursor
		 *   The hover cursor for the feature.
		 *   
		 * @augments aperture.Layer
		 * @constructs
		 *
		 * @description Layer constructors are invoked indirectly by calling
		 *  {@link aperture.PlotLayer#addLayer addLayer} on a parent layer with the following specifications...
		 * 
		 * @param {Object} spec
		 *      a specification object describing how to construct this layer
		 *      
		 * @param {String} spec.url
		 *      the URL of the external data source to load
		 *      
		 * @param {String='KML'|'GML'|'GeoRSS'} spec.format
		 *      indicates the type of data that will be loaded from the	provided URL.
		 *      One of 'KML', 'GML', or 'GeoRSS'.
		 *      
		 * @param {String} [spec.projection]
		 *      an optional string specifying the projection of the data contained in
		 *      the external data file.  If not provided, WGS84 (EPSG:4326) is assumed.
		 *      
		 */
		init : function(spec, mappings) {
			aperture.Layer.prototype.init.call(this, spec, mappings);

			var name = spec.name || 'External_' + this.uid;

			// Create layer for KML, GML, or GeoRSS formats.
			var options = {
	            strategies: [new OpenLayers.Strategy.Fixed()],
				projection: spec.projection || apiProjection.projCode,
	            protocol: new OpenLayers.Protocol.HTTP({
	                url: spec.url,
	                format: new OpenLayers.Format[spec.format]({
	                    extractAttributes: true,
	                    maxDepth: 2
	                })
	            })
			};
			
			this._layer = new OpenLayers.Layer.Vector( name, options );	
			if( this.canvas_ ) {
				this.canvas_.olMap_.addLayer(this._layer);
			}

			//
			// Ensure Openlayers defers to Aperture for all style queries
			// Creates an OpenLayers style map that will call the Aperture layer's "valueFor"
			// function for all styles.
			//
			// Create a base spec that directs OpenLayers to call our functions for all properties
			var defaultSpec = util.extend({}, fixedStyles);

			// plus any set properties
			util.forEach(availableStyles, function(property, styleName) {
				defaultSpec[styleName] = '${'+styleName+'}';
			});

			// Create a cloned version for each item state
			var selectedSpec = util.extend({}, defaultSpec);
			var highlighedSpec = util.extend({}, defaultSpec);

			// Override some properties for custom styles (e.g. selection bumps up zIndex)
			//util.extend(selectedSpec, customStyles.select);
			//util.extend(highlighedSpec, customStyles.highlight);

			// Create context object that provides feature styles
			// For each available style create a function that calls "valueFor" giving the
			// feature as the data value
			var styleContext = {},
				that = this;

			util.forEach(availableStyles, function(property, styleName) {
				styleContext[styleName] = function(feature) {
					// Value for the style given the data attributes of the feature
					return that.valueFor(property, feature.attributes, vectorStyleDefaults[styleName]);
				};
			});
			styleContext.externalGraphic = function(feature) {
				// Must have a non-undefined externalGraphic or else OpenLayers tries
				// to load the URL "undefined"
				if (feature.geometry.CLASS_NAME === 'OpenLayers.Geometry.Point') {
					return that.valueFor('icon-url', feature.attributes, '');
				}
				return that.valueFor('fill-pattern', feature.attributes, '');
			};

			// Create the style map for this layer
			styleMap = new OpenLayers.StyleMap({
				'default' : new OpenLayers.Style(defaultSpec, {context: styleContext}),
				'select' : new OpenLayers.Style(selectedSpec, {context: styleContext}),
				'highlight' : new OpenLayers.Style(highlighedSpec, {context: styleContext})
			});

			this._layer.styleMap = styleMap;
		},

		canvasType : ol,

		/**
		 * @private not supported
		 */
		data : function(value) {
			// Not supported
			if( value ) {
				throw new Error('Cannot add data to a layer with an external data source');
			}
		},

		/**
		 * @private monitor adds and removes.
		 */
		on : function( eventType, callback ) {
			var hadit = this.handlers_[eventType];
			
			aperture.Layer.prototype.on.call(this, eventType, callback);
			
			if (!hadit) {
				makeHandler.call(this, this.handlers_);
			}
		},
		
		/**
		 * @private monitor adds and removes.
		 */
		off : function( eventType, callback ) {
			aperture.Layer.prototype.off.call(this, eventType, callback);
			
			if (!this.handlers_[eventType]) {
				makeHandler.call(this, this.handlers_);
			}
		},
		
		/**
		 * @private
		 */
		render : function(changeSet) {
			// No properties or properties and intersection with our properties
			// Can redraw
			this._layer.redraw();
		}
	});

	ns.MapGISLayer = MapGISLayer;



	/**********************************************************************/

	/**
	 * @private
	 * OpenLayers implementation that positions a DIV that covers the entire world
	 * at the current zoom level.  This provides the basis for the MapNodeLayer
	 * to allow child layers to render via DOM or Vector graphics.
	 */
	var DivOpenLayer = OpenLayers.Class(OpenLayers.Layer,
	{

		/**
		 * APIProperty: isBaseLayer
		 * {Boolean} Markers layer is never a base layer.
		 */
		isBaseLayer : false,

		/**
		 * @private
		 */
		topLeftPixelLocation : null,

		/**
		 * @private constructor
		 *
		 * Parameters:
		 * name - {String}
		 * options - {Object} Hashtable of extra options to tag onto the layer
		 */
		initialize : function(name, options) {
			OpenLayers.Layer.prototype.initialize.apply(this, arguments);

			// The frame is big enough to contain the entire world
			this.contentFrame = document.createElement('div');
			this.contentFrame.style.overflow = 'hidden';
			this.contentFrame.style.position = 'absolute';
			// It is contained in the 'div' element which is fit exactly
			// to the map's main container layer
			this.div.appendChild(this.contentFrame);
		},

		/**
		 * APIMethod: destroy
		 */
		destroy : function() {
			OpenLayers.Layer.prototype.destroy.apply(this, arguments);
		},

		/**
		 * Method: moveTo
		 *
		 * Parameters:
		 * bounds - {<OpenLayers.Bounds>}
		 * zoomChanged - {Boolean}
		 * dragging - {Boolean}
		 */
		moveTo : function(bounds, zoomChanged, dragging) {
			var extent, topLeft, bottomRight;

			OpenLayers.Layer.prototype.moveTo.apply(this, arguments);

			// Panning operation simply moves the layer's DIVs around and shouldn't
			// need any adjustment

			if (zoomChanged) {
				// Zoom has changed so by definition has the required size of the content
				// div since it represents the entire world in pixel coordinates
				
				var maxB = this.map.getMaxExtent();
				
				// Calculate pixel bounds (in layer DIV coord system) of the world
				topLeft = this.map.getLayerPxFromLonLat(new OpenLayers.LonLat(maxB.left, maxB.top));
				bottomRight = this.map.getLayerPxFromLonLat(new OpenLayers.LonLat(maxB.right, maxB.bottom));

				// Move the content frame relative to the static DIV to correctly place the
				// content w.r.t. the viewport
				OpenLayers.Util.modifyDOMElement(this.contentFrame, null, topLeft,
								new OpenLayers.Size(bottomRight.x - topLeft.x,
												bottomRight.y - topLeft.y), 'absolute');

				// Store the pixel location of -180,90 lat/lon.  We need this because when
				// a feature wants to render at (lon,lat), open layers will return a pixel
				// position in the coordinate space of the viewport.  We need to convert from
				// the viewport-relative pixel position to the content DIV's pixel position.
				this.topLeftPixelLocation = topLeft;

				// Callback into the aperture-style layer to tell it that the content glasspanes
				// will have to redraw with a new scale/offset
				if (this.renderCallback) {
					this.renderCallback();
				}
			}

			// Generally, do nothing
		},

		getContentPixelForLonLat : function( lon, lat ) {
			// Convert from lon/lat to pixel space, account for projection
			var pt = new OpenLayers.Geometry.Point(lon, lat);
			// Reproject to map's projection
			if( this.map.projection != apiProjection ) {
				pt.transform(apiProjection, this.map.projection);
			}
			// Get layer pixel
			var px = this.map.getLayerPxFromLonLat(new OpenLayers.LonLat(pt.x, pt.y));
			// Transform pixel to contentFrame space
			px.x -= this.topLeftPixelLocation.x;
			px.y -= this.topLeftPixelLocation.y;

			return px;
		},

		getLonLatExtent: function() {
			var extent = this.map.getExtent();
			var p0 = new OpenLayers.Geometry.Point(extent.left, extent.top);
			var p1 = new OpenLayers.Geometry.Point(extent.right, extent.bottom);
			if( this.map.projection != apiProjection ) {
				p0.transform(this.map.projection, apiProjection);
				p1.transform(this.map.projection, apiProjection);
			}
			return {
				left: p0.x,
				top: p0.y,
				right: p1.x,
				bottom: p1.y
			};
		},

		drawFeature : function(feature, style, force) {
			// Called by OpenLayers to force this feature to redraw (e.g. if some state changed
			// such as selection that could affect the visual.  Not needed for a container layer
		},

		CLASS_NAME : 'DivOpenLayer'
	});


	// default property values for map nodes.
	var mapNodeProps = {
		'longitude' : 0,
		'latitude' : 0
	};

	/*
	 * TODO: Create a generic container layer that just creates a canvas for children
	 * to use.  Map lat/lon to [0,1] ranges and then renderers can scale x/y based on
	 * size of canvas.  Then can make MapNodeLayer derive from this layer.  This layer
	 * could be used as parent for a layer drawing a series of points/labels, for
	 * example.
	 */

	var MapNodeLayer = aperture.PlotLayer.extend( 'aperture.geo.MapNodeLayer',
	/** @lends aperture.geo.MapNodeLayer# */
	{
		/**
		 * @class A layer that draws child layer items at point locations.
		 * 
		 * @mapping {Number} longitude
		 *   The longitude at which to locate a node
		 *   
		 * @mapping {Number} latitude
		 *   The latitude at which to locate a node
		 *
		 * @augments aperture.PlotLayer
		 * @constructs
		 * @factoryMade
		 */
		init: function(spec, mappings) {
			aperture.PlotLayer.prototype.init.call(this, spec, mappings);

			// because we declare ourselves as an open layers canvas layer this will be 
			// the parenting open layers canvas, which holds the map reference. Note however that
			// since we are really a vector canvas layer we override that a ways below.
			var mapCanvas = this.canvas_;

			if (!mapCanvas.olMap_) {
				aperture.log.error('MapNodeLayer must be added to a map.');
				return;
			}

			// create the layer and parent it
			this._layer = new DivOpenLayer(spec.name || ('NodeLayer_' + this.uid), {});
			mapCanvas.olMap_.addLayer(this._layer);
			this._layer.setZIndex(999); // Change z as set by OpenLayers to be just under controls

			// because we parent vector graphics but render into a specialized open layers
			// canvas we need to help bridge the two by pre-creating this canvas with the
			// right parentage.
			var OpenLayersVectorCanvas = aperture.canvas.type(aperture.canvas.VECTOR_CANVAS);

			this.canvas_ = new OpenLayersVectorCanvas( this._layer.contentFrame );
			mapCanvas.canvases_.push( this.canvas_ );

			var that = this;
			this._canvasWidth = this.canvas_.root_.offsetWidth;
			this._canvasHeight = this.canvas_.root_.offsetHeight;
			this._layer.renderCallback = function(x,y) {
				// The OpenLayers layer has changed the canvas, must redraw all contents
				that._canvasWidth = that.canvas_.root_.offsetWidth;
				that._canvasHeight = that.canvas_.root_.offsetHeight;

				// This layer has changed, must rerender
				// TODO Pass in appropriate "change" hint so only translation need be updated
				that.all().redraw();
			};
		},

		/**
		 * @private
		 */
		canvasType : ol,

		/**
		 * @private
		 */
		render : function( changeSet ) {

			// just need to update positions
			aperture.util.forEach(changeSet.updates, function( node ) {
				// If lon,lat is specified pass the position to children
				// Otherwise let the children render at (x,y)=(0,0)
				var lat = this.valueFor('latitude', node.data, null);
				var lon = this.valueFor('longitude', node.data, null);

				// Find pixel x/y from lon/lat
				var px = {x:0,y:0};
				if (lat != null && lon != null) {
					px = this._layer.getContentPixelForLonLat(lon,lat);
				}

				// Update the given node in place with these values
				node.position = [px.x,px.y];

				// Update width/height
				node.userData.width = this._canvasWidth;
				node.userData.height = this._canvasHeight;

			}, this);
			
			
			// will call renderChild for each child.
			aperture.PlotLayer.prototype.render.call(this, changeSet);

		},

		/**
		 * @private
		 */
		renderChild : function(layer, changeSet) {
			// Pass size information to children (so LineSeriesLayer can render correctly)
			aperture.util.forEach( changeSet.updates, function (node) {
				if (node) {
					node.width = node.parent.userData.width;
					node.height = node.parent.userData.height;
				}
			});
			layer.render( changeSet );
		},

		/**
		 * Given a location returns its pixel coordinates in container space.
		 */
		getXY: function(lon,lat) {
			var px = this._layer.getContentPixelForLonLat(lon,lat);
			px.x = px.x/this._canvasWidth;
			px.y = px.y/this._canvasHeight;
			return px;
		},

		getExtent: function() {
			return this._layer.getLonLatExtent();
		}
	});

	ns.MapNodeLayer = MapNodeLayer;


	/************************************************************************************/



	/*
	 * The projection that the API expects unless instructed otherwise.  All layers
	 * and data are to be expressed in this projection.
	 */
	var apiProjection = new OpenLayers.Projection('EPSG:4326');


	/*
	 * Default map options
	 */
	var defaultMapConfig = {
		options : {
			projection : apiProjection,
			displayProjection : apiProjection
		}
	};

	/**
	 * Call on zoom completion.
	 */
	function notifyZoom() {
		this.trigger('zoom', {
			eventType : 'zoom',
			layer : this
		});
	}

	function notifyPan() {
		this.trigger('panend', {
			eventType : 'panend',
			layer : this
		});
	}
	
	var MapVizletLayer = aperture.PlotLayer.extend( 'aperture.geo.MapVizletLayer',
	// documented as Map, since it currently cannot function as a non-vizlet layer.
	/**
	 * @lends aperture.geo.Map#
	 */
	{
		/**
		 * @class A map vizlet is capable of showing geographic and geographically located data.  It
		 * contains a base map and additional child geo layers can be added. The base map is
		 * typically configured as a system-wide default, although can be overridden via the
		 * spec object passed into this constructor.  This layer does not require or support any
		 * mapped properties. 
		 *
		 *
		 * @constructs
		 * @augments aperture.PlotLayer
		 *
		 * @param {Object|String|Element} spec
		 *      A specification object detailing options for the map construction, or
		 *      a string specifying the id of the DOM element container for the vizlet, or
		 *      a DOM element itself. A
		 *      specification object, if provided, includes optional creation options for the
		 *      map layer.  These options can include base map configuration, map projection settings,
		 *      zoom level and visible area restrictions, and initial visible bounds.  Other than an id,
		 *      the following options do not need to be included if they are already configured via the 
		 *      aperture.config system.
		 * @param {String|Element} spec.id
		 *      If the spec parameter is an object, a string specifying the id of the DOM
		 *      element container for the vizlet or a DOM element itself.
		 * @param {Object} [spec.options]
		 *      Object containing options to pass directly to the Openlayers map.
		 * @param {String} [spec.options.projection]
		 *      A string containing the EPSG projection code for the projection that should be
		 *      used for the map.
		 * @param {String} [spec.options.displayProjection]
		 *      A string containing the EPSG projection code for the projection that should be
		 *      used for displaying map data to the user, for example mouse hover coordinate overlays.
		 * @param {String} [spec.options.units]
		 *      The units used by the projection set above
		 * @param {Array} [spec.options.maxExtent]
		 *      A four-element array containing the maximum allowed extent (expressed in units of projection
		 *      specified above) of the map given the limits of the projection.
		 * @param {Object} [spec.baseLayer]
		 *      Object containing information about the base map layer that should be created.
		 * @param {Object} spec.baseLayer.{TYPE}
		 *      The base layer specification where {TYPE} is the class of MapTileLayer 
		 *      (e.g. {@link aperture.geo.MapTileLayer.TMS TMS}) and
		 *      its value is the specification for it.
		 * @param {Object} [mappings]
		 *      An optional initial set of property mappings.
		 */
		init : function(spec, mappings) {

			// clone - we will be modifying, filling in defaults.
			this.spec = spec = util.extend({}, spec);

			// pass clone onto parent.
			aperture.PlotLayer.prototype.init.call(this, spec, mappings);


			// PROCESS SPEC
			// Clone provided options and fill in defaults
			spec.options = util.extend({}, defaultMapConfig.options || {}, spec.options);

			// Ensure projections are in OpenLayers class format
			if( util.isString(spec.options.projection) ) {
				spec.options.projection = new OpenLayers.Projection(spec.options.projection);
			}
			if( util.isString(spec.options.displayProjection) ) {
				spec.options.displayProjection = new OpenLayers.Projection(spec.options.displayProjection);
			}

			// Ensure maxExtent is an OpenLayer bounds object
			if( util.isArray(spec.options.maxExtent) ) {
				spec.options.maxExtent = OpenLayers.Bounds.fromArray(spec.options.maxExtent);
			}
			
			// If map to have no controls, initialize with new empty array, not array from defaultMapConfig
			if(util.isString(spec.options.controls)||(util.isArray(spec.options.controls)&&(spec.options.controls.length==0))){
				spec.options.controls = [];  
			}

			// Clone provided base layer information and fill in defaults
			spec.baseLayer = util.extend({}, defaultMapConfig.baseLayer || {}, spec.baseLayer);

			// CREATE MAP
			// Create the map without a parent
			this.olMap_ = new OpenLayers.Map( spec.options );
			this.canvas_.canvases_.push(new OpenLayersCanvas( this.canvas_.root(), this.olMap_ ) );

			
			var type = '', config = null;
			
			for (type in spec.baseLayer) {
				if (spec.baseLayer.hasOwnProperty(type)) {
					config = spec.baseLayer[type];
					break;
				}
			}
 
			if (!config) {
				this.addLayer( BlankMapLayer, {}, spec );
				
			} else {
				config.options = config.options || {};
				config.options.isBaseLayer = true;
				
				var resolvedType = tileTypeAliases[type] || type;

				if (MapTileLayer[resolvedType]) {
					this.addLayer( MapTileLayer[resolvedType], {}, config );
				} else {
					aperture.log.warn('WARNING: unrecognized map base layer type: '+ type);
					this.addLayer( BlankMapLayer, {}, spec );
				}
			}
			
			// Add mouse event handlers that pass click and dblclick events
			// through to layer event handers
			var that = this,
				handler = function( event ) {
					that.trigger(event.type, {
						eventType: event.type
					});
				},
				mouseHandler_ = new OpenLayers.Handler.Click(
					this,
					{
						'click' : handler,
						'dblclick' : handler
					},
					{ map: this.olMap_ }
				);
			mouseHandler_.activate();

			// XXX Set an initial viewpoint so OpenLayers doesn't whine
			// If we don't do this OpenLayers dies on nearly all lat/lon and pixel operations
			this.zoomTo(0,0,1);

			this.olMap_.events.register('zoomend', this, notifyZoom);
			this.olMap_.events.register('moveend', this, notifyPan);
			this.olMap_.render(this.canvas_.root());
		},

		/**
		 * @private
		 * The map requires a DOM render context
		 */
		canvasType : aperture.canvas.DIV_CANVAS,

		/**
		 * Zooms to the max extent of the map.
		 */
		zoomToMaxExtent: function() {
			this.olMap_.zoomToMaxExtent();
		},

		/**
		 * Zooms in one zoom level, keeps center the same.
		 */
		zoomIn: function() {
			this.olMap_.zoomIn();
		},

		/**
		 * Zooms out one zoom level, keeps center the same (if possible).
		 */
		zoomOut: function() {
			this.olMap_.zoomOut();
		},

		/**
		 * Returns the zoom level as an integer.
		 */
		getZoom: function() {
			return this.olMap_.getZoom();
		},

		/**
		 * Sets the map extents give a center point in lon/lat and a zoom level
		 * Always accepts center as lon/lat, regardless of map's projection
		 * @param lat latitude to zoom to
		 * @param lon longitude to zoom to
		 * @param zoom zoom level (map setup dependent)
		 */
		zoomTo : function( lat, lon, zoom ) {
			var center = new OpenLayers.LonLat(lon,lat);
			if( this.olMap_.getProjection() !== apiProjection.projCode ) {
				center.transform(apiProjection, this.olMap_.projection);
			}
			this.olMap_.setCenter( center, zoom );
		},

		/**
		 * Sets visible extents of the map in lat/lon (regardless of current coordinate
		 * system)
		 * @param left left longitude of extent
		 * @param top top latitude of extent
		 * @param right right longitude of extent
		 * @param bottom bottom latitude of extent
		 */
		setExtents : function( left, top, right, bottom ) {
			var bounds = new OpenLayers.Bounds(left,bottom,right,top);
			if( this.olMap_.getProjection() !== apiProjection.projCode ) {
				bounds.transform(apiProjection, this.olMap_.projection);
			}
			this.olMap_.zoomToExtent( bounds );
		}
	});

	/**
	 * @private
	 */
	// MapVizletLayer is currently documented as Map, since it does not currently function as a non-vizlet layer.
	var Map = aperture.vizlet.make( MapVizletLayer );
	ns.Map = Map;


	/*
	 * Register for config notification
	 */
	aperture.config.register('aperture.map', function(config) {
		if( config['aperture.map'] ) {
			if( config['aperture.map'].defaultMapConfig ) {
				// override local defaults with any configured defaults.
				util.extend( defaultMapConfig, config['aperture.map'].defaultMapConfig );
			}

			aperture.log.info('Map configuration set.');
		}
	});

	return ns;
}(aperture.geo || {}));
/**
 * Source: AxisLayer.js
 * Copyright (c) 2014 Oculus Info Inc.
 * @fileOverview Aperture Axis Layer
 */

/**
 * @namespace
 * The chart visualization package. If not used, the chart package may be excluded.
 */
aperture.chart = (
/** @private */
function(namespace) {
	/**
	 * @private
	 * Creates the {@link aperture.LabelLayer} used to
	 * display a title for this axis.
	 */
	var DEFAULT_TICK_LENGTH = 4,
		DEFAULT_TICK_WIDTH = 1,
		palette = aperture.palette.color,
	
	createTitleLayer = function(node){
		// Lazy creation of the title LabelLayer.
		this.titleLayer = this.addLayer(aperture.LabelLayer);
		// Detach the title of the axis from inheriting any parent x-mappings.
		// We don't want the label to be able to pan horizontally.
		this.titleLayer.map('x').from('x').only().using(this.DEFAULT_RANGE.mapKey([0,1]));
		this.titleLayer.map('y').from('y').using(this.DEFAULT_RANGE.mapKey([1,0]));
		this.titleLayer.map('text').from('text');
		this.titleLayer.map('text-anchor').asValue('middle');

		// Setup optional mappings.
		this.titleLayer.map('orientation').from('orientation');
		this.titleLayer.map('offset-x').from('offset-x');
		this.titleLayer.map('offset-y').from('offset-y');
		this.titleLayer.map('font-family').asValue(this.valueFor('font-family', node.data, null));
		this.titleLayer.map('font-size').asValue(this.valueFor('font-size', node.data,null));
		this.titleLayer.map('font-weight').asValue(this.valueFor('font-weight', node.data, null));
	},

	setDefaultValues = function(){
		var type = this.valueFor('axis');
		var vAlign = this.valueFor('text-anchor-y');
		var textAlign = this.valueFor('text-anchor');
		var layout = this.valueFor('layout');
		if (type === 'x'){
			if (!layout){
				this.map('layout').asValue('bottom');
			}
			if (!vAlign){
				this.map('text-anchor-y').asValue('top');
			}
			if (!textAlign){
				this.map('text-anchor').asValue('middle');
			}
		}
		else{
			if (!layout){
				this.map('layout').asValue('left');
			}
			if (!vAlign){
				this.map('text-anchor-y').asValue('middle');
			}
			if (!textAlign){
				this.map('text-anchor').asValue('end');
			}
		}
	},

	getLabelPadding = function(type, layout, vAlign, textAlign){
		var xPadding=0, yPadding=0;
		var labelOffsetX = this.valueFor('label-offset-x', null, 0);
		var labelOffsetY = this.valueFor('label-offset-y', null, 0);
		if (type === 'x') {
			if (layout === 'top'){
				yPadding = -labelOffsetY;
			}
			else if (layout === 'bottom'){
				yPadding = labelOffsetY;
			}

			if (textAlign === 'start'){
				xPadding = labelOffsetX;
			}
			else if (textAlign === 'end'){
				xPadding = -labelOffsetX;
			}
		}
		else {
			if (layout === 'left'){
				xPadding = -labelOffsetX;
			}
			else if (layout === 'right'){
				xPadding = labelOffsetX;
			}
			if (vAlign === 'bottom'){
				yPadding = labelOffsetY;
			}
			else if (vAlign === 'top'){
				yPadding = -labelOffsetY;
			}
		}
		return {'x':xPadding, 'y':yPadding};
	},

	/**
	 * @private
	 * Renders the tick marks for this label and creates a list of
	 * tick mark labels that will be passed on to the child {@link aperture.LabelLayer}
	 * for rendering.
	 * @param {Object} node Render node for this layer
	 */
	createAxis = function(node){
		var w,
			h,
			left = node.position[0],
			top = node.position[1],
			right = left + node.width,
			bottom = top + node.height,
			type = this.valueFor('axis', node.data, null);

		node.userData[type] = node.userData[type] || {};
		node.graphics.removeAll(node.userData[type].axis);

		// The margin defines the width of the axis for vertical AxisLayers;
		// the height of the axis for horizontal AxisLayers.
		var axisMargin = this.valueFor('margin',node.data,0),
			ruleWidth = this.valueFor('rule-width',node.data,0);

		setDefaultValues.call(this);

		var tickWidth = this.valueFor('tick-width',node.data, DEFAULT_TICK_WIDTH);
		var tickLength = this.valueFor('tick-length',node.data, DEFAULT_TICK_LENGTH);
		// The offset of the tick mark from the chart layer.
		var offset = this.valueFor('tick-offset',node.data,0);

		// Check the type of axis we are drawing.
		// x-axis = horizontal
		// y-axis = vertical

		var vAlign = (this.valueFor('text-anchor-y', null, 'bottom')).toLowerCase();
		var layout = null;

		if (type == 'x') {
			w = node.width;
			h = axisMargin || node.height;
			layout = (this.valueFor('layout', null, 'bottom')).toLowerCase();
			if (layout === 'top'){
				tickLength *= -1;
				offset *= -1;
			}
		}
		else {
			w = axisMargin || node.width;
			h = node.height;
			layout = (this.valueFor('layout', null, 'left')).toLowerCase();
			if (layout === 'right'){
				tickLength *= -1;
				offset *= -1;
			}
		}

		//TODO:
		// Create set for storing tick visuals for updating/removal.

		// Now we render the ticks at the specifed intervals.
		var path = '';

		var min=0, max=0;
		var mapKey = node.data;
		var axisRange = node.data.from();

		// no range? show no ticks.
		if (axisRange.get()) {
			var tickArray = {ticks:axisRange.get()};
			var xPos=0,yPos=0;
			var tickLabels = [];
	
			// Check if the label layer is visible.
			if (this.labelLayer){
				if (type === 'y'){
					// We use a default mapper for the x-coordinate of the labels so that we can
					// align the vertical labels with the left side of the chart by mapping them
					// to zero.
					this.labelLayer.map('x').from('labels[].x').using(this.DEFAULT_RANGE.mapKey([0,1]));
					this.labelLayer.map('y').from('labels[].y');
					//this.labelLayer.map('text-anchor').asValue('end');
				}
				else if (type === 'x'){
					this.labelLayer.map('x').from('labels[].x');
					// We use a default mapper for the y-coordinate of the labels so that we can
					// align the horizontal labels with the bottom of the chart by mapping them
					// to zero.
					this.labelLayer.map('y').from('labels[].y').using(this.DEFAULT_RANGE.mapKey([1,0]));
					//this.labelLayer.map('text-anchor').asValue('middle');
				}
	
				// Setup optional font attribute mappings. Default values are provided by label layer
				// if no explicit value is provided locally.
				this.labelLayer.map('font-family').asValue(this.valueFor('font-family', node.data, null));
				this.labelLayer.map('font-size').asValue(this.valueFor('font-size',node.data,null));
				this.labelLayer.map('font-weight').asValue(this.valueFor('font-weight', node.data, null));
			}
	
			// Draw the tick marks for a banded or ordinal range.
			var hasBands = axisRange.typeOf(/banded/),
				mappedValue, tickId, tick, tickMin, tickLimit,
				bandwidth = 0;
			if (tickArray.ticks.length > 1){
				// Calculate the distance between bands by sampling the first 2 intervals.
				bandwidth = (this.valueFor('x', tickArray, 0, 1)-this.valueFor('x', tickArray, 0, 0))*node.width;
			}
	
			if (hasBands || axisRange.typeOf(aperture.Ordinal)){
				for (tickId=0; tickId < tickArray.ticks.length; tickId++){
					tick = tickArray.ticks[tickId];
					if (!tick) {
						continue;
					}
					tickMin = hasBands?tick.min:tick;
					tickLimit = hasBands?tick.limit:tick;
					if (type === 'x'){
						mappedValue = this.valueFor('x', tickArray, 0, tickId);
						xPos = (mappedValue*node.width) + left;
						yPos = bottom + offset;
						if (xPos < left || xPos > right){
							continue;
						}
	
						path += 'M' + xPos + ',' + yPos + 'L' + xPos + ',' + (yPos+tickLength);
						tickLabels.push({'x':tickMin,'y':0, 'text':axisRange.format(tickMin)});
						// If we're on the last tick, and there is a bounded upper limit,
						// include a tick mark for the upper boundary value as well.
						if (tickId == tickArray.ticks.length-1 && tickLimit != Number.MAX_VALUE){
							// Create a fake data source so that the mapped value will account
							// for any filters.
							mappedValue = this.valueFor('x', {'ticks':[{'min':tickLimit}]}, 0, 0);
							xPos = (mappedValue*node.width) + left;
							path += 'M' + xPos + ',' + yPos + 'L' + xPos + ',' + (yPos+tickLength);
							// If this is a banded scalar, we want to show the label.
							if (axisRange.typeOf(aperture.Scalar)){
								tickLabels.push({'x':tickLimit,'y':0, 'text':axisRange.format(tickLimit)});
							}
						}
					}
					else if (type === 'y'){
						mappedValue = this.valueFor('y', tickArray, 0, tickId);
						xPos = left - (tickLength + offset) - (0.5*ruleWidth);
						yPos = (mappedValue*h) + top;
						path += 'M' + xPos + ',' + yPos + 'L' + (xPos+tickLength) + ',' + yPos;
	
						// If we're on the last tick, and there is a bounded upper limit,
						// include a tick mark for the upper boundary value as well.
						if (tickId == tickArray.ticks.length-1 && tickLimit != Number.MAX_VALUE){
							mappedValue = this.valueFor('y', {'ticks':[{'min':tickLimit}]}, 0, 0);
							yPos = (mappedValue*h) + top;
							path += 'M' + xPos + ',' + yPos + 'L' + (xPos+tickLength) + ',' + yPos;
							tickLabels.push({'x':0,'y':tickLimit, 'text':axisRange.format(tickLimit)});
						}
						tickLabels.push({'x':0,'y':tickMin, 'text':axisRange.format(tickMin)});
					}
				}
			}
	
			// Draw the tick marks for a scalar range.
			else {
				for (tickId=0; tickId < tickArray.ticks.length; tickId++){
					tick = tickArray.ticks[tickId];
					if (tick !== -Number.MAX_VALUE){
						mappedValue = axisRange.map(tick);
						if (type === 'x'){
							xPos = mappedValue*node.width + left;
							if (xPos < left || xPos > right){
								continue;
							}
							// Calculate the axis position in a top-down fashion since the origin
							// is in the top-left corner.
							yPos = bottom + offset;
							path += 'M' + xPos + ',' + yPos + 'L' + xPos + ',' + (yPos+tickLength);
							tickLabels.push({'x':tick,'y':0, 'text':axisRange.format(tick)});
						}
	
						else if (type === 'y'){
							xPos = left - (tickLength + offset) - ruleWidth;
							// Calculate the axis position in a top-down fashion since the origin
							// is in the top-left corner.
							yPos = mappedValue*node.height + top;
							if (yPos < top || yPos > bottom){
								continue;
							}
							
							path += 'M' + xPos + ',' + yPos + 'L' + (xPos+tickLength) + ',' + yPos;
							tickLabels.push({'x':0,'y':tick, 'text':axisRange.format(tick)});
						}
					}
				}
			}

			this.labelLayer.all({'labels':tickLabels});
		}
		// Unless specifically overridden, the axis colour will be the same as the
		// border colour of the parent ChartLayer.
		var axisColor = this.valueFor('stroke') || palette('rule');

		var axisSet = [];
		if (!!ruleWidth){
			var rulePath;
			if (type === 'y'){
				rulePath = 'M'+left + ',' + top + 'L' + left + ',' + bottom;
			}
			else if (type === 'x'){
				yPos = bottom + offset;
				rulePath = 'M'+left + ',' + yPos + 'L' + right + ',' + yPos;
			}
			var axisRule = node.graphics.path(rulePath).attr({fill:null, stroke:axisColor, 'stroke-width':ruleWidth});
			axisSet.push(axisRule);
		}

		// Take the path of the ticks and create a Raphael visual for it.
		var axisTicks = node.graphics.path(path).attr({fill:null, stroke:axisColor, 'stroke-width':tickWidth});
		axisSet.push(axisTicks);
		node.userData[type].axis = axisSet;

		// Check to see if this axis layer has a title.
		var title = this.valueFor('title', node.data, null);
		if (title){
			var axisTitle;
			if (!this.titleLayer){
				createTitleLayer.call(this, node);
			}
			// For vertical titles, we need to rotate the text so that it is aligned
			// parallel to the axis.
			if (type === 'y'){
				axisTitle = {'x':0, 'y':0.5, 'text':title, 'orientation':-90, 'offset-x': -axisMargin};
			}
			else if (type === 'x'){
				axisTitle = {'x':0.5, 'y':0, 'text':title, 'offset-y': axisMargin};
			}
			this.titleLayer.all(axisTitle);
		}
	};

	/**
	 * @exports AxisLayer as aperture.chart.AxisLayer
	 */
	var AxisLayer = aperture.PlotLayer.extend( 'aperture.chart.AxisLayer',
	/** @lends AxisLayer# */
	{
		/**
		 * @augments aperture.PlotLayer
		 * @class An AxisLayer provides visual representation of a single axis 
		 * for its parent {@link aperture.chart.ChartLayer ChartLayer}. AxisLayers are not added 
		 * to a chart in conventional layer fashion, rather they are instantiated the first time
		 * they are referenced via chart.{@link aperture.chart.ChartLayer#xAxis xAxis} or
		 * chart.{@link aperture.chart.ChartLayer#yAxis yAxis}

		 * @mapping {String} stroke
		 *   The color of the axis rule and ticks.
		 * 
		 * @mapping {Number=0} rule-width
		 *   The width of the line (in pixels) used to visually represent the baseline of an axis. Typically, the
		 *   parent {@link aperture.chart.ChartLayer ChartLayer} will have a border visible, which subsequently provides
		 *   the baseline for the axis, thus 'rule-width' is set to zero by default. If no border is present in
		 *   the parent chart, then this property should be assigned a non-zero value.
		 *   Tick marks will extend perpendicularly out from this line.
		 * 
		 * @mapping {Number=0} tick-length
		 *   The length of a tick mark on the chart axis.
		 * 
		 * @mapping {Number=0} tick-width
		 *   The width of a tick mark on the chart axis.
		 * 
		 * @mapping {Number=0} tick-offset
		 *   The gap (in pixels) between the beginning of the tick mark and the axis it belongs too.
		 * 
		 * @mapping {Number=0} label-offset-x
		 *   The horizontal gap (in pixels) between the end of a tick mark, and the beginning of the tick mark's label.
		 * 
		 * @mapping {Number=0} label-offset-y
		 *   The vertical gap (in pixels) between the end of a tick mark, and the beginning of the tick mark's label.
		 * 
		 * @mapping {Number} margin
		 *   The space (in pixels) to allocate for this axis.
		 *   For vertical (y) axes, this refers to the width reserved for the axis.
		 *   For horizontal (x) axes, this refers to the height reserved for the axis.
		 * 
		 * @mapping {String} title
		 *   The text of the axis title.
		 * 
		 * @mapping {String='Arial'} font-family
		 *   The font family used to render all the text of this layer.
		 * 
		 * @mapping {Number=10} font-size
		 *   The font size (in pixels) used to render all the text of this layer.
		 * 
		 * @mapping {String='normal'} font-weight
		 *   The font weight used to render all the text of this layer.
		 * 
		 * @constructs
		 * @factoryMade
		 */
		init : function(spec, mappings) {
			aperture.PlotLayer.prototype.init.call(this, spec, mappings);
			// Add a LabelLayer for rendering the tick mark labels.
			this.labelLayer = this.addLayer(aperture.LabelLayer);
			// Set up the expected mappings for the LabelLayer.
			this.labelLayer.map('text').from('labels[].text');
			this.labelLayer.map('label-count').from('labels.length');

			// Setup optional mappings.
			this.labelLayer.map('orientation').from('orientation');
			this.labelLayer.map('offset-x').from('offset-x');
			this.labelLayer.map('offset-y').from('offset-y');
			this.labelLayer.map('font-family').from('font-family');
			this.labelLayer.map('font-size').from('font-size');
			this.labelLayer.map('font-weight').from('font-weight');

			this.DEFAULT_RANGE = new aperture.Scalar('default_range', [0,1]);
		},

		canvasType : aperture.canvas.VECTOR_CANVAS,

		render : function(changeSet) {

			// Process the modified components.
			aperture.util.forEach(changeSet.updates, function(node) {
				createAxis.call(this, node);
			}, this);

			
			// will call renderChild for each child.
			aperture.PlotLayer.prototype.render.call(this, changeSet);
		},

		/**
		 * @private
		 * Before the child {@link aperture.LabelLayer} is rendered, we need to adjust
		 * the position of the child layer to account for the tick marks of the axis.
		 */
		renderChild : function(layer, changeSet) {
			// The type of axis we are drawing.
			// Vertical axes are drawn on the left (typically the y-axis)
			// Horizontal axes are drawn at the bottom of the graph (typically the x-axis).

			var i;

			// Before we create the child LabelLayer, we may need to change the starting
			// position and anchor point of the layer depending on the type of axis
			// we are drawing, and how the parent ChartLayer is oriented.
			var toProcess = changeSet.updates;
			for (i=0; i < toProcess.length; i++){
				var node = toProcess[i],
					left = node.position[0],
					top = node.position[1];

				var ruleWidth = this.valueFor('rule-width', null, 1);

				var type = this.valueFor('axis', null, null);

				// (may be better to use these constant values instead).
				var anchorMap = {left: 'start', right: 'end', middle: 'middle'};
						
				if (layer === this.labelLayer){
					var vAlign = this.valueFor('text-anchor-y', null, 'bottom');
					var textAlign = this.valueFor('text-anchor', null, null);
					var layout = this.valueFor('layout', null, null);

					// Set the anchor position of the label based on
					// the alignment properties of this axis.
					layer.map('text-anchor-y').asValue(vAlign);

					// Handle the case of a banded or ordinal range.

					//TODO: Re-work the handling of the changed
					// nodes. This seems inefficient.
					// If this node is an existing one, just leave
					// it where it is. The label is already positioned
					// in the correct location.
					if (aperture.util.has(changeSet.changed, node)){
						continue;
					}

					var tickWidth = this.valueFor('tick-width', node.data, DEFAULT_TICK_WIDTH);
					var tickLength = this.valueFor('tick-length', node.data, DEFAULT_TICK_LENGTH);
					var offset = this.valueFor('tick-offset', node.data, 0);
					var axisMargin = this.valueFor('margin', node.data, 0); // TODO: smart default based on what's showing

					var mapKey = node.parent.data;
					var axisRange = mapKey.from();
					var tickArray = axisRange.get();
					var childPos, bandWidth;

					// If the axis ticks/labels are being drawn in the interior
					// of the plot area, flip these values to correspond with
					// that.
					if ((type === 'x' && layout === 'top')||
							(type === 'y' && layout === 'right')){
						tickLength *= -1;
						offset *= -1;
					}

					if (axisRange.typeOf(/banded|Ordinal/)){
						if (type === 'x'){
							// If this is a banded scalar value, we want to align the tick labels
							// with the tick marks. For ordinals, the labels are shifted to fall
							// between tick marks.
							if (axisRange.typeOf(aperture.Scalar)){
								childPos = [left, top + (tickLength+offset)];
							}
							else {
								bandWidth = (node.width-(2*tickWidth))/tickArray.length;
								childPos = [left + (0.5*bandWidth), top + (tickLength+offset)];
							}
							node.position = childPos;
						}
						else if (type === 'y'){
							// Get the orientation of the parent chart layer.
							// The default orientation is vertical.
							// e.g For the case of a bar chart, a vertical orientation draws
							// the bars top-to-bottom. Whereas a horizontal orientation would
							// draw the bars left-to-right.
							var orientation = this.valueFor('orientation', node.data, 'vertical');
							// Handle a horizontal orientation.
							if (orientation == 'horizontal'){
								// Special case for a banded scalar range.
								// If this is a vertical axis (e.g. Y-axis) but the chart is oriented
								// horizontally, we want the label of any scalar tick marks to
								// align with the tick itself, and not be slotted in between tick marks
								// as we do for ordinal tick marks.
								if (axisRange.typeOf(aperture.Scalar)){
									childPos = [left-(tickLength+offset), top];
								}
								else {
									bandWidth = (node.height-(2*tickWidth))/tickArray.length;
									childPos = [left-(tickLength+offset), top-(0.5*bandWidth)];
								}
							}
							// Default assumption is a vertical orientation.
							else {
								childPos = [left-(tickLength+offset), top];
							}
							node.position = childPos;
						}
					}
					// Handle the default case of an unbanded or scalar range.
					else {
						if (type === 'x'){
							childPos = [left, top + (tickLength+offset)];
							node.position = childPos;
						}
						else if (type === 'y'){
							childPos = [left-(tickLength+offset), top];
							node.position = childPos;
						}
					}
					var padding = getLabelPadding.call(this, type, layout, vAlign, textAlign);
					node.position[0] = node.position[0] + padding.x;
					node.position[1] = node.position[1] + padding.y;
				}
				else if (layer.uid == (this.titleLayer && this.titleLayer.uid)){
					childPos = [left, top];
					node.position = childPos;

					this.titleLayer.map('text-anchor-y').asValue(type === 'y'? 'top': 'bottom');
				}
			}
			layer.render( changeSet );
		}
	});

	namespace.AxisLayer = AxisLayer;
	return namespace;
}(aperture.chart || {}));
/**
 * Source: BarSeriesLayer.js
 * Copyright (c) 2014 Oculus Info Inc.
 * @fileOverview Aperture Bar Chart Layer
 */

/**
 * @namespace
 * The chart visualization package. If not used, the chart package may be excluded.
 */
aperture.chart = (
/** @private */
function(namespace) {

	/**
	 * @private
	 * Creates a simple identifier for a given bar segment. For
	 * unstacked bars, there will only ever be one segment.
	 * @param {Object} Spec used to generate the identifier.
	 */
	var getBarId = function (barSpec){
		return barSpec.x.toString() + barSpec.y.toString();
	},

	/**
	 * @private
	 * Calculates the actual dimensions to render a bar, accounting for chart
	 * orientation and border width.
	 * @param {String} Orientation of the chart (i.e. 'bar-layout').
	 * @param {Number[0,1]} Normalized value of the x-coordinate data value .
	 * @param {Number[0,1]} Normalized value of the y-coordinate data value.
	 * @param {Number} Width of a bar visual.
	 * @param {Number} Width of the border around the chart.
	 */
	getBarRect = function(orientation, w, h, barWidth, borderWidth, seriesId, index, barOffset, footprint, node){
		var barSeriesData = node.data,
			xValue = this.valueFor('x', barSeriesData, 0, index),
			yValue = this.valueFor('y', barSeriesData, 0, index),
			strokeWidth = this.valueFor('stroke-width', barSeriesData, 1),
			orientation = this.valueFor('orientation', null, 'vertical'),
			borderWidth = this.valueFor('border-width', barSeriesData, 1),
			barLayout = this.valueFor('bar-layout', null, this.DEFAULT_BAR_LAYOUT),
			canvasY = yValue*h;
		
		var localBarWidth=0,localBarHeight=0;
		var xPoint=0, yPoint=0;

		if (orientation === 'horizontal'){
			localBarHeight = barWidth-borderWidth;

			
			// position
			yPoint = canvasY + node.position[1];
			// Account for the bar offset and the height of the
			// top/bottom borders of the bar.
			yPoint += barOffset - 0.5*(footprint);
			if (seriesId > 0 && barLayout !== 'stacked'){
				yPoint -= 0.5*strokeWidth;
			}
			
			
			// MAP VALUE
			var x1 = xValue*w;
			
			// map zero
			var x0 = this.map('x').using()? w*this.map('x').using().map(0) : 0;
			
			
			if (x1 > x0) {
				xPoint = node.position[0] + x0+ borderWidth;
				localBarWidth = Math.max(x1-x0, 0);
			} else {
				xPoint = node.position[0] + x1- borderWidth;
				localBarWidth = Math.max(x0-x1, 0);
			}
		}
		else {
			var canvasY = yValue*h;
			// Take the y-point and calculate the height of the corresponding bar.
			// We subtract the stroke width of the top and bottom borders so that
			// the bar doesn't blead over the border.
			localBarWidth = barWidth;
			
			
			// position
			xPoint = (xValue*w) + node.position[0];
			
			// Adjust the positioning of the bar if there are multiple series
			// and center the width of the bar wrt the data point.
			xPoint += barOffset - 0.5*(footprint);
			if (seriesId > 0 && barLayout !== 'stacked'){
				xPoint += 0.5*strokeWidth;
			}

			// MAP VALUE
			var y1 = Math.max(canvasY - borderWidth, 0);
			
			// map zero
			var y0 = this.map('y').using()? h*this.map('y').using().map(0) : 0;
			
			if (y1 < y0) {
				yPoint = node.position[1] + y1- 0.5*(borderWidth+strokeWidth);
				localBarHeight = Math.max(y0-y1, 0);
			} else {
				yPoint = node.position[1] + y0+ 0.5*(borderWidth+strokeWidth);
				localBarHeight = Math.max(y1-y0, 0);
			}
		}
		
		return {'x': xPoint, 'y': yPoint, 'width':localBarWidth, 'height':localBarHeight};
	},

	/**
	 * @private
	 * Calculates the width of a bar visual.
	 * @param {Object} Node object
	 * @param {Number} Width of the chart
	 * @param {Number} Height of the chart
	 * @param {Number} Number of series.
	 */
	getBarWidth = function(node, canvasWidth, canvasHeight, seriesCount){
		var barSeriesData = node.data,
			strokeWidth = this.valueFor('stroke-width', barSeriesData, 1),
			numPoints = this.valueFor('point-count', barSeriesData, 0),
			spacer = this.valueFor('spacer', null, 0),
			orientation = this.valueFor('orientation', null, 'vertical'),
			bandWidth = (orientation==='horizontal'?canvasHeight:canvasWidth)/numPoints,
			maxBarWidth = (bandWidth-((seriesCount-1)*spacer)-(seriesCount*2*strokeWidth))/seriesCount;

		// See if a value has been provided for the bar width, if there isn't
		// we'll use the one we calculated. If the user provided a bar width,
		// make sure it doesn't exceed the max bar width.
		var barWidth = Math.min(this.valueFor('width', node.data, maxBarWidth), maxBarWidth);

		// If the bar width is less than or equal to zero, return a bar width of 1 pixel anyways.
		// The user should understand that the chart is too crowded and either reduce the number
		// of bars to plot or increase the chart's dimensions.
		if (barWidth <= 0){
			return 1;
		}

		return barWidth;
	};

	/**
	 * @exports BarSeriesLayer as aperture.chart.BarSeriesLayer
	 */
	var BarSeriesLayer = aperture.BarLayer.extend( 'aperture.chart.BarSeriesLayer',
	/** @lends BarSeriesLayer# */
	{
		/**
		 * @augments aperture.BarLayer
		 * 
		 * @class A layer that takes a list of data points and plots a bar chart. This layer
		 * is capable of handling data with multiple series, as well as producing stacked bar charts.
		 * For plotting simpler bar visuals, refer to {@link aperture.BarLayer}

		 * @mapping {Number} point-count
		 *   The number of points in a given bar chart data series.
		 * 
		 * @mapping {String='vertical'} orientation
		 *   Sets the orientation of the chart. Vertically oriented charts will have bars that expand along the y-axis,
		 *   while horizontally oriented charts will have bars expanding along the x-axis. By default, this property
		 *   is set to 'vertical'.
		 *  
		 * @mapping {Number} width
		 *   Sets the width of each bar in the chart (i.e. the bar's thickness). If no mapping for this attribute is
		 *   provided, the width of the bars will be automatically calculated. For charts with a
		 *   horizontal orientation, the width is measured along the y-axis. Similarly, for vertically
		 *   oriented charts, the width is measured along the x-axis.
		 * 
		 * @mapping {Number} length
		 *   Mapping for determining the length of each bar in the chart. For charts with a horizontal
		 *   orientation, the length is measured along the x-axis. Similarly, for vertically oriented
		 *   charts, the length is measured along the y-axis.
		 * 
		 * @mapping {'clustered'|'stacked'} bar-layout
		 *   Determines how the bar series of the chart are positioned, either 
		 *   adjacent or stacked on top of each other.
		 * 
		 * @mapping {Number=0} spacer
		 *   Sets the space between bars from different bands<br>
		 *   i.e. the gap between the last bar of Band#0 and the first bar of Band#1.
		 * 
		 * @mapping {String} fill
		 *   Sets the fill colour of the bar.<br>

		 * @constructs
		 * @factoryMade
		 */
		init : function(spec, mappings){
			aperture.BarLayer.prototype.init.call(this, spec, mappings);

			this.DEFAULT_BAR_LAYOUT = 'clustered';
		},

		canvasType : aperture.canvas.VECTOR_CANVAS,

		render : function(changeSet) {
			this.updateLayer(this.applyLayout(changeSet.updates), changeSet.transition);
		}
		
	});
	
	
	/**
	 * Overrides the layout methods of BarLayer. This method provides logic
	 * for handling multiples series as well as stacked bar charts.
	 */
	BarSeriesLayer.prototype.applyLayout = function(dataObjects){
		
		var seriesId= -1;
		var masterBarWidth = 0,
			barOffset = 0;

		var seriesSpec = [],
			barSpecs;
		
		var totalBarLength = {};

		aperture.util.forEach( dataObjects, function (node) {
			seriesId++;
			barSpecs = [];
			if (!node.userData.bars){
				node.userData.bars = {};
			}

			var barSeriesData = node.data;
			if (barSeriesData.length != 0) {

				// If the barchart style is stacked, we can treat this chart as if it
				// only contained a single series.
				var barLayout = this.valueFor('bar-layout', barSeriesData, this.DEFAULT_BAR_LAYOUT);
	
				// FIX: this count is incorrect if added in multiple steps.
				var seriesCount = barLayout === 'stacked'?1:dataObjects.length;
	
				var strokeWidth = this.valueFor('stroke-width', barSeriesData, 1);
	
				var w = node.width;
				var h = node.height;
	
				// For calculating the x-axis scale, we need to take into account
				// how many series are being plotted.
				// For multiple series, the bars of subsequent series are placed
				// adjacent to each other. This needs to be accounted for in the
				// x-axis scale, otherwise they will get clipped and not visible.
				var orientation = this.valueFor('orientation', barSeriesData, 'vertical');
				var numPoints = this.valueFor('point-count', barSeriesData, 0);
				var borderWidth = this.valueFor('border-width', barSeriesData, 0);
				if (numPoints > 0) {
					// If no bar width is provided, calculate one based on the
					// the number of points.
					if (!masterBarWidth) {
						masterBarWidth = getBarWidth.call(this, node, w, h, seriesCount);
					}
	
					// Calculate the total effective footprint of all the bars in a given band.
					var footprint = (seriesCount*masterBarWidth) + (seriesCount-1)*(strokeWidth);
					// Now shift each bar an appropriate distance such that all the bars for a
					// given band are (as a group) centered on the band's midpoint.
					// If the bar is stacked, we treat it as if it was a chart with only
					// 1 series.
					var seriesIndex = barLayout==='stacked'?0:seriesId;
					barOffset = seriesIndex*(masterBarWidth + strokeWidth);
				}

				for (index=0; index< numPoints; index++){
					var renderBarDim = getBarRect.call(this, orientation, w, h, masterBarWidth, borderWidth, 
							seriesId, index, barOffset, footprint, node);
	
					var xPoint = renderBarDim.x,
						yPoint = renderBarDim.y;

					// If we are dealing with stacked bars, we need to account for the length of the previous
					// bar for a given bin.
					if (barLayout === 'stacked'){
						var lOffset = 0;
						if (!totalBarLength ){
							totalBarLength  = {};
						}
						if (!totalBarLength[index]){
							totalBarLength[index] = 0;
							totalBarLength[index] = orientation === 'vertical'?-renderBarDim.height:renderBarDim.width;
						}
						else {
							lOffset = totalBarLength[index];
							totalBarLength[index] += orientation === 'vertical'?-renderBarDim.height:renderBarDim.width;
						}
						if (orientation === 'vertical') {
							yPoint += lOffset;
						} else {
							xPoint += lOffset;
						}
					}
					
					var barSpec = {
						node : node,
						x : xPoint,
						y : yPoint,
						size : renderBarDim,
						strokeWidth : strokeWidth,
						orientation : orientation
					};
					barSpecs.push(barSpec);
				}
				seriesSpec.push(barSpecs);
			}
		}, this);
		return seriesSpec; 
	};
	
	namespace.BarSeriesLayer = BarSeriesLayer;
	return namespace;
	
}(aperture.chart || {}));
/**
 * Source: ChartLayer.js
 * Copyright (c) 2014 Oculus Info Inc.
 * @fileOverview Aperture Chart Layer
 */

/**
 * @namespace
 * The chart visualization package. If not used, the chart package may be excluded.
 */
aperture.chart = (
function(namespace) {

	var palette = aperture.palette.color,
		updatePlotVisual = function(node){
		var fill = this.valueFor('background-color', node.data, '#fff');
		var borderStroke = this.valueFor('border-stroke', node.data, palette('border'));
		var borderWidth = this.valueFor('border-width', node.data, 1);
		var opacity = this.valueFor('opacity', node.data, 1);
		var plotWidth = node.userData.plotBounds.width;
		var plotHeight = node.userData.plotBounds.height;
		var borderXPos = node.userData.plotBounds.position[0];
		var borderYPos = node.userData.plotBounds.position[1];
		// Subtract the width of the border to get the
		// actual dimensions of the chart.
		var chartWidth = plotWidth - borderWidth;
		var chartHeight = plotHeight - borderWidth;
		var chart = node.userData.plotVisual;
		if (!chart){
			chart = node.graphics.rect(borderXPos,
				borderYPos,
				chartWidth,
				chartHeight);
			node.userData.plotVisual = chart;
		}
		chart.attr({'stroke':borderWidth?borderStroke:null,
				'stroke-width':borderWidth,
				'opacity':opacity,
				'x': borderXPos,
				'y': borderYPos,
				'fill':fill, //Fill can't be a null value otherwise the chart will not be pannable.
				'width':chartWidth,
				'height':chartHeight});
		chart.data('width', chartWidth);
	},

	calculateChartSpecs = function(node){
		var chart = node.userData.chart;
		var innerCanvas = node.userData.chart.innerCanvas = {};

		var titleSpec = this.valueFor('title-spec', node.data, null);

		// Check if any axes margins have been specified.
		var yMargin = this.valueFor('y-margin', node.data, 0),
			xMargin = this.valueFor('x-margin', node.data, 0);

		if (!yMargin && this.axisArray.y[0]){
			yMargin = this.axisArray.y[0].valueFor('margin', node.data, 0);
		}
		if (!xMargin && this.axisArray.x[0]){
			xMargin = this.axisArray.x[0].valueFor('margin', node.data, 0);
		}

		// Get the size of the title margin, if any. There are 2 scenarios to consider:
		// 1. If a title has been specified, use the associated title margin value.
		// If none has been provided, use the minimum default title margin value.
		// 2. If the y-axis is visible, we want to make sure there is a little space
		// reserved for the topmost tick label to bleed into. Since the labels are typically
		// aligned with the centre of a tick mark, if the topmost tick falls inline with the
		// top of the chart border, then the top half of the accompanying label will actually
		// be positioned above the chart. We leave a little space so that the top of the label
		// doesn't get clipped.
		var yVisible = this.axisArray.y[0] && this.axisArray.y[0].valueFor('visible', null, false);
		var	titleMargin = (titleSpec && this.valueFor('title-margin', node.data, this.MIN_TITLE_MARGIN))
			|| (yVisible && yMargin && this.MIN_TITLE_MARGIN)||0;

		// If the axis layer is not visible AND no axis margin has been
		// allocated, we can shortcut the rest of the chart dimension
		// calculations.
		if (yMargin === 0 && xMargin === 0){
			node.userData.plotBounds = {width:chart.width,
				height:chart.height-titleMargin,
				position:[chart.position[0], chart.position[1]+titleMargin]};

			innerCanvas.width = chart.width;
			innerCanvas.height = chart.height-titleMargin;
			innerCanvas.position = [chart.position[0], chart.position[1]+titleMargin];
			return;
		}

		var borderWidth = chart.width-yMargin;
		var borderHeight = chart.height-xMargin-titleMargin;
		var borderXPos = yMargin + chart.position[0];
		var borderYPos = titleMargin + chart.position[1];

		node.userData.plotBounds = {width:borderWidth, height:borderHeight,
				position:[borderXPos, borderYPos]};

		innerCanvas.width = borderWidth;
		innerCanvas.height = borderHeight;
		innerCanvas.position = [borderXPos, borderYPos];
	},

	//TODO: Expose this with a getter like how the axes logic has been done.
	configureTitle = function(node){
		var titleSpec = this.valueFor('title-spec', node.data, null);
		if (titleSpec){
			// Check to see if we have a text layer for the title, if not
			// we'll lazily create it.
			if (!this.titleLayer){
				this.titleLayer = this.addLayer(aperture.LabelLayer);
				this.titleLayer.map('text-anchor').asValue('middle');
				this.titleLayer.map('x').from('x').only().using(this.DEFAULT_RANGE.mapKey([0,1]));
				this.titleLayer.map('y').from('y').only().using(this.DEFAULT_RANGE.mapKey([1,0]));
				this.titleLayer.map('text').from('text');
				this.titleLayer.map('text-anchor-y').asValue('top');
				this.titleLayer.map('orientation').asValue(null);

				// Setup optional font attribute mappings. Default values are provided
				// if no explicit value is provided.
				this.titleLayer.map('font-family').asValue(titleSpec['font-family']||null);
				this.titleLayer.map('font-size').asValue(titleSpec['font-size']||null);
				this.titleLayer.map('font-weight').asValue(titleSpec['font-weight']||null);
			}
			this.titleLayer.all({'x':0.5, 'y':1,'text': titleSpec.text});
		}
	},

	configureAxes = function(){
		var hasAxis = false;

		aperture.util.forEach(this.axisArray.x, function(xAxisLayer){
			if (xAxisLayer.valueFor('visible') == true){
				// Makes sure a data object has been supplied since
				// the user may have created an axis through the getter
				// and not assigned a data source.
				var mapKeyX = this.mappings().x.transformation;
				var rangeX = mapKeyX.from();
				if (xAxisLayer == this.axisArray.x[0] || !xAxisLayer.hasLocalData) {
					xAxisLayer.all(mapKeyX);
				}

				if (rangeX.typeOf(/banded/)){
					xAxisLayer.map('x').from('ticks[].min');
				}
				else {
					xAxisLayer.map('x').from('ticks[]');
				}

				// Make sure that the value for the margin of the primary axis and
				// the value allocated by the chart match.
				var chartMargin = this.valueFor('x-margin', null, 0);
				var axisMargin = this.axisArray.x[0].valueFor('margin', null, 0);
				if (axisMargin != chartMargin){
					this.map('x-margin').asValue(axisMargin);
				}
			}
		}, this);

		// Check if the y-axis is enabled.
		aperture.util.forEach(this.axisArray.y, function(yAxisLayer){
			if (yAxisLayer.valueFor('visible') == true){
				// Set the y-range object as the data source for the y axislayer.
				var mapKeyY = this.mappings().y.transformation;
				var rangeY = mapKeyY.from();
				if (yAxisLayer == this.axisArray.y[0] || !yAxisLayer.hasLocalData) {
					yAxisLayer.all(mapKeyY);
				}

				if (rangeY.typeOf(/banded/)){
					yAxisLayer.map('y').from('ticks[].min');
				}
				else {
					yAxisLayer.map('y').from('ticks[]');
				}

				// Make sure that the value for the margin of the primary axis and
				// the value allocated by the chart match.
				var chartMargin = this.valueFor('y-margin', null, 0);
				var axisMargin = this.axisArray.y[0].valueFor('margin', null, 0);
				if (axisMargin != chartMargin){
					this.map('y-margin').asValue(axisMargin);
				}
			}
		},this);
	},

	//TODO: Add min-ticklength for other examples.
	isManagedChild = function( layer ) {
		if (layer == this.titleLayer || layer.typeOf(namespace.AxisLayer)) {
			return true;
		}
		return false;
	},

	// validate and apply a change to the center.
	doCenter = function( c ) {
		if ( c == null) {
			return this.center;
		}

		c = Math.max(0.5 / this.zoomValue, Math.min(1 - 0.5 / this.zoomValue, c));
		if (this.center != c) {
			this.center = c;
			return true;
		}
	};


	/**
	 * @exports ChartLayer as aperture.chart.ChartLayer
	 */
	var ChartLayer = aperture.PlotLayer.extend( 'aperture.chart.ChartLayer',
	/** @lends ChartLayer# */
	{
		/**
		 * @augments aperture.PlotLayer
		 * 
		 * @class The underlying base layer for charts. Type-specific
		 * charts are created by adding child layers (e.g. {@link aperture.chart.LineSeriesLayer LineSeriesLayer},
		 * {@link aperture.chart.BarSeriesLayer BarSeriesLayer}) to this layer. Axes and "rules" / grid lines
		 * can be constructed and configured using the {@link #xAxis xAxis}, {@link #yAxis yAxis}
		 * and {@link #ruleLayer ruleLayer} methods.
		 *
		 * @mapping {Number} width
		 *   The width of the chart.
		 *   
		 * @mapping {Number} height
		 *   The height of the chart.
		 *   
		 * @mapping {String} stroke
		 *   The line colour used to plot the graph.
		 * 
		 * @mapping {Number=1} stroke-width
		 *   The width of the line used to plot the graph.
		 *   
		 * @mapping {Number=1} border-width 
		 *   The width of the border (if any) around the chart. Setting this value to zero will hide the
		 *   chart borders.
		 *   
		 * @mapping {String='border'} border-stroke
		 *   The line colour used to draw the chart border.
		 * 
		 * @mapping {'vertical', 'horizontal'} orientation
		 *   The direction that data points are plotted.
		 *   E.g. A bar chart with a <span class="fixedFont">'vertical'</span> orientation will have bars drawn top-down.
		 *   A bar chart with a <span class="fixedFont">'horizontal'</span> orientation will have bars drawn left-right
		 *   
		 * @mapping {Number} title-margin 
		 *   The vertical space allocated to the chart title (in pixels).
		 *   
		 * @mapping {Object} title-spec 
		 *   Defines the attributes of the chart's main title. For example:<br>
		 *<pre>{
		 *   text: 'Main Chart Title',
		 *   font-family: 'Arial',
		 *   font-size: 25
		 *}</pre></br>
		 *
		 * @constructs
		 * @factoryMade
		 */
		init : function(spec, mappings) {
			aperture.PlotLayer.prototype.init.call(this, spec, mappings);
			this.specData = spec.data;
			this.DEFAULT_XMARGIN = 40;
			this.DEFAULT_YMARGIN = 50;
			this.DEFAULT_RANGE = new aperture.Scalar('default_range', [0,1]);
			this.DEFAULT_BANDS = 5;
			this.MIN_TITLE_MARGIN = 10;
			// Default values.
			this.map('border-width').asValue(1);
			this.map('border-stroke').asValue(palette('border'));
			this.map('orientation').asValue('vertical');
			this.axisArray = {'x':[], 'y':[]};
		},

		canvasType : aperture.canvas.VECTOR_CANVAS,

		render : function(changeSet) {
			// process the changed components.
			var toProcess = changeSet.updates;

			var that = this;

			if (toProcess.length > 0){
				var i;
				for (i=0; i < toProcess.length; i++){
					node = toProcess[i];

					// Cache the true canvas dimensions.
					node.userData.chart = {};
					this.width = node.userData.chart.width = this.valueFor('width', node.data, node.width);
					this.height = node.userData.chart.height = this.valueFor('height', node.data, node.height);
					node.userData.chart.position = node.position;

					calculateChartSpecs.call(this,node);
					updatePlotVisual.call(this, node);
					configureTitle.call(this,node);
				}
				configureAxes.apply(this);
			}

			// Iterate through all the children and render them.
			aperture.PlotLayer.prototype.render.call(this, changeSet);

		},

		renderChild : function(layer, changeSet) {
			// Before we create any child layers, we want to apply any chart
			// margins and axes compensations to the chart width/height.

			// If the range is banded, we need to shift the data points
			// so that they fall between the tick marks.
			// Tick marks for ordinal ranges indicate the bounds of a band.
			aperture.util.forEach( changeSet.updates, function (node) {
				var parentData = node.parent.userData.chart.innerCanvas;
				// Get the width of the border around the chart, if any.
				var borderWidth = this.valueFor('border-width', node.data, 1);
				node.width = parentData.width;
				node.height = parentData.height;
				node.position = [parentData.position[0], parentData.position[1]];

				// If this is the title layer we want to change the anchor point.
				if (layer === this.titleLayer){
					node.position = [parentData.position[0], node.parent.position[1]];
				}

				// If the range is banded, we may need to apply a shift the starting position
				// of any sub layers.
				// This is only for layers that are not an axis layer or rule layer.
				else if (!isManagedChild.call(this, layer)){
					node.position = [node.position[0], node.position[1]];

					var orientation = this.valueFor('orientation', node.data, 'vertical');
					if (orientation == 'horizontal'){
						var mapKeyY = this.mappings().y.transformation;
						var rangeY = mapKeyY.from();
						if (rangeY.typeOf(aperture.Ordinal)){
							var bandHeight = (node.height)/(rangeY.get().length);
							// Visuals are rendered top-down (i.e. (0,0) is in the upper-left
							// corner of the canvas) so we need to subtract half the band height
							// from the y-position so that our bars begin drawing from the
							// midpoint between tick marks.
							node.position = [node.position[0], node.position[1]-(0.5*bandHeight)];
						}
					}

					else {
						// If the range is ordinal or banded, we need to shift all the data
						// points by half the width of a band. Tick marks indicate the bounds between
						// bands, but we want the data point to be centered within the band, so to
						// compensate we use this offset.
						var mapKeyX = this.mappings().x.transformation;
						var rangeX = mapKeyX.from();
						// If this is banded, we need to check if this band
						// was derived from a scalar range, we only want to do
						// this shift for bands derived from an ordinal range.
						if (rangeX.typeOf(aperture.Ordinal)){
							var bandWidth = (node.width)/rangeX.get().length;
							node.position = [node.position[0] + (0.5*bandWidth), node.position[1]];
						}
					}

					// Set the clip region.
					node.graphics.clip(this.valueFor('clipped', node.data, true)?
							[parentData.position[0], parentData.position[1],
							parentData.width, parentData.height] : null);
				}
			}, this);
			
			
			layer.render( changeSet );
		},

		/**
		 * This method retrieves the {@link aperture.chart.RuleLayer} with the given index.
		 * @param {Number} index
		 *  the index of the RuleLayer to retrieve. If no index is provided, a list of all 
		 *  RuleLayers is returned. 
		 *
		 * @returns {aperture.chart.RuleLayer|Array}
		 *  the RuleLayer for the given index. If no order is specified, a list of all RuleLayer is returned.
		 * @function
		 */		
		ruleLayer : function(index) {
			var ruleLayers = this.ruleLayers || [];
			if (index == undefined) {
				return ruleLayers;
			}
			else if (index == null) {
				index = 0;
			}
			var layer = ruleLayers[index];
			
			if (!layer) {
				layer = ruleLayers[index] = this.addLayer(aperture.chart.RuleLayer);
				// Since we only allow panning along the x-axis, we only want to allow 
				// rule layers for the x-axis to pan.
				var that = this;
				layer.map('rule').filter(function( value ){
						if (layer.valueFor('axis', this, null) == 'x'){
							return that.panfilter(value);
						}
						else {
							return value;
						}
					}
				);
				
				this.ruleLayers = ruleLayers;
				layer.toBack(); // Send the rule layer to the back.
			}
			return layer;
		},
		
		/**
		 * This method retrieves the {@link aperture.chart.AxisLayer} of the given order for the X axis.
		 * 
		 * @param {Number} [order] 
		 *  the order of the axis to be retrieved (e.g. the primary axis would be order=0), or
		 *  -1 to retrieve an array of all axes. If no order is provided, the primary axis is returned. 
		 *
		 * @returns {aperture.chart.AxisLayer|Array}
		 *  the AxisLayer for the given order, or a list of all X AxisLayers.
		 */		
		xAxis : function (order) {
			if (order === -1) {
				return this.axisArray.x;
			} else if (!order || order > 1) {
				// Currently, charts only support secondary axes.
				order = 0;
			}
			
			var axisLayer = this.axisArray.x[order];
			if (!axisLayer){
				axisLayer = this.addLayer( aperture.chart.AxisLayer );
				axisLayer.map('visible').asValue(true);
				axisLayer.map('axis').asValue('x');
				this.axisArray.x[order] = axisLayer;
			}
			return axisLayer;
		},
		
		/**
		 * This method retrieves the {@link aperture.chart.AxisLayer} of the given order for the Y axis.
		 * 
		 * @param {Number} [order] 
		 *  the order of the axis to be retrieved (e.g. the primary axis would be order=0), or
		 *  -1 to retrieve an array of all axes. If no order is provided, the primary axis is returned. 
		 *
		 * @returns {aperture.chart.AxisLayer|Array}
		 *  the AxisLayer for the given order, or a list of all Y axis AxisLayers.
		 */		
		yAxis : function (order) {
			if (order === -1) {
				return this.axisArray.y;
			} else if (!order || order > 1) {
				// Currently, charts only support secondary axes.
				order = 0;
			}

			var axisLayer = this.axisArray.y[order];
			if (!axisLayer){
				axisLayer = this.addLayer( aperture.chart.AxisLayer );
				axisLayer.map('visible').asValue(true);
				axisLayer.map('axis').asValue('y');
				this.axisArray.y[order] = axisLayer;
				// We don't want the y-AxisLayer to pan horizontally
				// so we use the only() method to prevent it from
				// inheriting any x-mappings from its parent.
				var mapX = this.mappings().x;
				if (mapX){
					var mapKeyX = mapX.transformation;
					this.axisArray.y[order].map('x').only().using(mapKeyX);
				}
			}
			return axisLayer;
		}
	});

	namespace.ChartLayer = ChartLayer;

	/**
	 * @class Chart is a {@link aperture.chart.ChartLayer ChartLayer} vizlet, suitable for adding to the DOM.
	 * See the layer class for a list of supported mappings.
	 * 
	 * @augments aperture.chart.ChartLayer
	 * @name aperture.chart.Chart
	 *
	 * @constructor
	 * @param {Object|String|Element} spec
	 *  A specification object detailing how the vizlet should be constructed or
	 *  a string specifying the id of the DOM element container for the vizlet or
	 *  a DOM element itself.
	 * @param {String|Element} spec.id
	 *  If the spec parameter is an object, a string specifying the id of the DOM
	 *  element container for the vizlet or a DOM element itself.
	 *
	 * @see aperture.chart.ChartLayer
	 */
	namespace.Chart = aperture.vizlet.make( ChartLayer, function(spec){
		// Default values for zooming and panning logic.
		this.zoomValue = 1;
		this.center = 0.5;
		this.panning = false;
		this.startCenter = {};

		// set up the drag handler that applies the panning.
		this.on('drag', function(event) {
			switch (event.eventType) {
			case 'dragstart':
				this.startCenter = this.center;
				this.panning = false;
				break;

			case 'drag':
				// don't start unless movement is significant.
				this.panning = this.panning || Math.abs(event.dx) > 3;

				if (this.panning) {
					this.zoomTo(this.startCenter - event.dx / this.width / this.zoomValue );
				}
				break;
			}
			return true;
		});

		// the x filter function - applies a final transform on the x mapping based on pan/zoom
		var that = this;
		this.panfilter = function(value) {
			return (value - that.center) * that.zoomValue + 0.5;
		};

		// Support panning along the x-axis.
		this.map('x').filter(this.panfilter);
		this.updateAxes = function(center){
			var bandCount = this.width / 100;
			// update bands
			if (this.axisArray.x[0]){
				var mapKeyX = this.mappings().x.transformation;
				var rangeX = mapKeyX.from();
				// Reband the range to reflect the desired zoom level.
				var bandedX = rangeX.banded(bandCount*this.zoomValue);
				mapKeyX = bandedX.mapKey([0,1]);
				this.map('x').using(mapKeyX);
				this.xAxis(0).all(mapKeyX);
				// Update the rule layers of the x-axis, if any.
				aperture.util.forEach(this.ruleLayer(), function(layer){
					if (layer.valueFor('axis')==='x'){
						layer.all(bandedX.get());
						layer.map('rule').using(mapKeyX);
					}
				});
			}

			// TODO: Does secondary axis logic belong here?
			if (this.axisArray.x[1]){
				var nextOrder = bandedX.formatter().nextOrder();
				if (nextOrder){
					var secBandedX = bandedX.banded({
						'span' : 1,
						'units' : nextOrder
					});
					this.xAxis(1).all(secBandedX.mapKey([0,1]));
				}
				// If the next time order is undefined, hide the secondary axis.
				this.xAxis(1).map('visible').asValue(!!nextOrder);
			}
		};

		// EXPOSE A ZOOM FUNCTION
		// apply a zoom, revalidating center as necessary, and update
		this.zoomTo = function(x, y, z) {
			var changed;

			z = Math.max(z || this.zoomValue, 1);

			if (this.zoomValue != z) {
				this.zoomValue = z;
				changed = true;

				// update bands
				this.updateAxes();
			}
			if (doCenter.call(this, x ) || changed) {
				this.trigger('zoom', {
					eventType : 'zoom',
					layer : this
				});
				this.updateAxes(x);
				this.all().redraw(); // todo: not everything.
			}
		};

		// expose getter, and setter of zoom only.
		this.zoom = function(z) {
			if ( z == null ) {
				return {
					zoom : this.zoomValue,
					x : this.center,
					y : 0.5
				};
			}
			this.zoomTo(this.center, null, z);
		};
	} );



	return namespace;

}(aperture.chart || {}));/**
 * Source: LineSeriesLayer.js
 * Copyright (c) 2014 Oculus Info Inc.
 * @fileOverview Aperture LineSeriesLayer Layer
 */

/**
 * @namespace
 * The chart visualization package. If not used, the chart package may be excluded.
 */
aperture.chart = (
/** @private */
function(namespace) {
	/**
	 * @private
	 * converts a stroke style to an svg dash pattern.
	 */
	function strokeStyleToDash(strokeStyle) {
		// Determine dash array
		switch (strokeStyle) {
		case 'dashed':
			return '- ';
		case 'dotted':
			return '. ';
		case 'none':
			return null;
		case '':
		case 'solid':
			return '';
		}
		return strokeStyle;
	}
	
	/**
	 * @private
	 * Tokenize the data points into line path segments, using
	 * changes in line style as the delimiter. For a series with
	 * a homogeneous line style, there will only be one token.
	 * @param {Array} pointList An array of js objects describing the points of this line chart.
	 * @returns An array of path segments.
	 */
	var tokenizeSeries = function(pointList){
		var pathSegments = [];

		var pathStartPos = 0;
		var lastStrokeStyle = strokeStyleToDash(this.valueFor('stroke-style', pointList, 'solid', 0));
		var lastStroke = this.valueFor('stroke', pointList,'#000000', 0);

		// The style of each segment is defined by the rightmost
		// point. We assume the points in a given series are sorted.
		var numPoints = this.valueFor('point-count', pointList, 1, 0);
		if (numPoints<2){
			return pathSegments;
		}
		var segmentPoints = [{x: this.valueFor('x', pointList, 0, 0),
			y: this.valueFor('y', pointList, 0, 0)}],
			i;

		// We want to collect all points that share the same color and stroke style
		// and render them together as a single path.
		for (i=1; i < numPoints; i++) {
			// Get the stroke style and color of this line segment.
			var strokeStyle = strokeStyleToDash(this.valueFor('stroke-style', pointList, 'solid', i));
			var lineStroke = this.valueFor('stroke', pointList, '#000000', i);

			var hasSegmentChange = (strokeStyle !== lastStrokeStyle)||(lineStroke !== lastStroke);

			var hasMorePoints = i < numPoints - 1;
			// Check to see if the x-value is ordinal.
			var xPoint = this.valueFor('x', pointList, 0, i);
			var yPoint = this.valueFor('y', pointList, 0, i);

			segmentPoints.push({x: xPoint, y: yPoint});
			// If the point is part of the same line segment, continue
			// collecting the points.
			if (!hasSegmentChange && hasMorePoints) {
				continue;
			}
			pathSegments.push({'points' : segmentPoints, 'stroke-style' : lastStrokeStyle, 'stroke' : lastStroke});
			segmentPoints = [{x: xPoint, y: yPoint}];
			pathStartPos = i - 1;
			lastStrokeStyle = strokeStyle;
			lastStroke = lineStroke;
		}

		return pathSegments;
	},

	/**
	 * @private
	 * Construct a SVG path from the specified data points.
	 * @param {Array} pathSpec An array of js objects that describe the path segments
	 * (i.e. tokenized points) of this line series.
	 */
	constructPath = function(pathSpec){
		var path, point, xPoint, yPoint, i,
			chartPosition = pathSpec.node.position;
		for (i=0; i < pathSpec.points.length; i++){
			point = pathSpec.points[i];
			xPoint = (point.x * pathSpec.node.width)
				+ (chartPosition[0]||0);
			yPoint = (point.y * pathSpec.node.height)
				+ (chartPosition[1]||0);

			if (i==0){
				path = "M" + xPoint + "," + yPoint;
			}
			path += "L" + xPoint + "," + yPoint;
		}
		return path;
	};

	/**
	 * @exports LineSeriesLayer as aperture.chart.LineSeriesLayer
	 */
	var LineSeriesLayer = aperture.Layer.extend( 'aperture.chart.LineSeriesLayer',
	/** @lends LineSeriesLayer# */
	{
		/**
		 * @augments aperture.Layer
		 * 
		 * @class A layer that takes sets of points and graphs a line for each.
		 * 
		 * @mapping {Number=1} point-count
		 *   The number of points in a line series. 
		 *   
		 * @mapping {String} stroke
		 *   Color of a line series.
		 *   
		 * @mapping {Number=1} stroke-width
		 *  The width of a line series.
		 * 
		 * @mapping {'solid'|'dotted'|'dashed'|'none'| String} stroke-style
		 *  The line style as a predefined option or custom dot/dash/space pattern such as '--.-- '.
		 *  A 'none' value will result in the line not being drawn.
		 * 
		 * @mapping {Number=1} opacity
		 *  The opacity of a line series. Values for opacity are bound with the range [0,1], with 1 being opaque.
		 * 
		 * @constructs
		 * @factoryMade
		 */
		init : function(spec, mappings){
			aperture.Layer.prototype.init.call(this, spec, mappings);
		},

		canvasType : aperture.canvas.VECTOR_CANVAS,

		render : function(changeSet) {

			// Create a list of all additions and changes.
			var i, toProcess = changeSet.updates;
			for (i=toProcess.length-1; i >= 0; i--){
				var node = toProcess[i];

				// Make sure we have a proper canvas node.
				if (node.data.length == 0) {
					continue;
				}
				if (!node.userData.pathSegments) {
					node.userData.pathSegments = [];
				}

				// Get the visual properties of the chart.
				var strokeWidth = this.valueFor('stroke-width', node.data, 1);
				var strokeOpacity = this.valueFor('opacity', node.data, 1);

				// Tokenize the series into line segments.
				var lines = this.valueFor('lines', node.data, null);
				var pathSegments = [], lineNo, segNo;
				if (lines) {
					for (lineNo=0; lineNo<lines.length; lineNo++) {
						var newSegs = tokenizeSeries.call(this, lines[lineNo]);
						for (segNo=0;segNo<newSegs.length;segNo++) {
							pathSegments.push(newSegs[segNo]);
						}
					}
				} else {
					pathSegments = tokenizeSeries.call(this, node.data);
				}


				var path, pathSpec, segmentInfo, strokeStyle,
					points, point, index, segment, 
					oldn = node.userData.pathSegments.length, n = pathSegments.length;

				// Remove any extra previously rendered segments
				if ( oldn > n ) {
					node.graphics.removeAll(node.userData.pathSegments.splice(n, oldn-n));
				}
				
				// Iterate through the current segments and update or re-render.
				for (index=0; index < n; index++){
					segmentInfo = pathSegments[index];
					points = segmentInfo.points;
					pathSpec = {
							points:points,
							node:node
					};

					// Construct the SVG path for this line segment.
					path = constructPath.call(this, pathSpec);

					segment = node.userData.pathSegments[index];

					// Determine dash array
					strokeStyle = segmentInfo['stroke-style'];

					if (strokeStyle === null) {
						strokeStyle = '';
						strokeOpacity = 0;
					}

					var attrSet = {
							'stroke':segmentInfo.stroke,
							'stroke-width':strokeWidth,
							'stroke-linejoin': 'round',
							'stroke-dasharray':strokeStyle,
							'stroke-opacity':strokeOpacity};							
					
					// If this path has already exists, we don't need to render
					// it again. We just need to check if it's visual properties
					// have changed.
					var hasDataChange = true;
					if (segment){
						var prevPath = segment.attr('path').toString();
						if (path === prevPath){
							// No data change, update attributes and continue
							if (segment.attr('stroke') != segmentInfo.stroke){
								attrSet['stroke'] = segmentInfo.stroke;
							}
							if (segment.attr('stroke-width') != strokeWidth){
								attrSet['stroke-width'] = strokeWidth;
							}
							if (segment.attr('stroke-dasharray') != strokeStyle){
								attrSet['stroke-dasharray'] = strokeStyle;
							}
							if (segment.attr('stroke-opacity') != strokeOpacity){
								attrSet['stroke-opacity'] = strokeOpacity;
							}
							hasDataChange = false;
						} else {
							// Data has changed, update the line's path.
							attrSet['path'] = path;
						}
					}

					else {
						// Create a visual for the new path segment.
						segment = node.graphics.path(path);
					}
					// Apply attributes to the segment.
					node.graphics.update(segment, 
							attrSet, 
							changeSet.transition);
					// If the data has changed, update the
					// corresponding references.
					if (hasDataChange){
						node.graphics.data( segment, node.data );

						// Store the visuals associated with this node.
						node.userData.pathSegments[index] = segment;
					}
				}
			}
		}
	});
	namespace.LineSeriesLayer = LineSeriesLayer;

	return namespace;
}(aperture.chart || {}));
/**
 * Source: RuleLayer.js
 * Copyright (c) 2014 Oculus Info Inc.
 * @fileOverview Aperture Rule Layer
 */

/**
 * @namespace
 * The chart visualization package. If not used, the chart package may be excluded.
 */
aperture.chart = (
/** @private */
function(namespace) {

	var palette = aperture.palette.color;
	
	/**
	 * @private
	 * Renders a single horizontal or vertical rule.
	 * @param {Object} node
	 */
	var createRule = function(node){
		if (!(node)) {
			return;
		}

		var borderWidth = this.valueFor('border-width', node.data, 0);
		var opacity = this.valueFor('opacity', node.data, 1);

		var axis = this.valueFor('axis', node.data, 'x');
		var value = this.valueFor('rule', node.data, 0);
		
		var lineColor = this.valueFor('stroke', node.data, palette('rule'));
		var lineWidth = this.valueFor('stroke-width', node.data, 1);
		
		// Get the stroke-style, if any, and translate into the corresponding
		// stroke dasharray value.
		var lineStyle = this.valueFor('stroke-style', node.data, '');
		switch (lineStyle) {
		case 'dashed':
			lineStyle = '- ';
			break;
		case 'dotted':
			lineStyle = '. ';
			break;
		case 'none':
			opacity = 0;
		case '':
		case 'solid':
			lineStyle = '';
		}

		var path = '';

		var xPos=0,yPos=0, xOffset=0, yOffset=0;
		var rangeX = this.mappings().x.transformation.from();
		var rangeY = this.mappings().y.transformation.from();
		
		// If this is a banded range, we need to offset the x-position by
		// half the bandwidth.
		if (rangeX.typeOf(aperture.Ordinal)){
			xOffset = 0.5*(node.width/rangeX.get().length);	
		}
		if (rangeY.typeOf(aperture.Ordinal)){
			yOffset = 0.5*(node.height/rangeY.get().length);	
		}
		// Check if the rule line orientation is vertical.
		if (axis === 'x'){
			xPos = (value*node.width) + node.position[0] + xOffset;
			yPos = node.position[1] + yOffset;
			path += 'M' + xPos + ',' + yPos + 'L' + xPos + ',' + (yPos+node.height-borderWidth);
		}
		// Default rule line orientation is horizontal.
		else {

			xPos = node.position[0] - xOffset;
			yPos = (value*node.height) + node.position[1] + yOffset;

			path += 'M' + xPos + ',' + yPos + 'L' + (xPos+node.width-borderWidth) + ',' + yPos;
		}

		//TODO: Add logic for caching rule lines for reuse.
		var line = node.graphics.path(path).attr({
				'fill':null, 
				'stroke':lineColor, 
				'stroke-width':lineWidth,
				'stroke-dasharray':lineStyle,
				'opacity':opacity
			});
		node.userData.rulelines.push(line);
	};

	/**
	 * @exports RuleLayer as aperture.chart.RuleLayer
	 */
	var RuleLayer = aperture.Layer.extend( 'aperture.chart.RuleLayer',
	/** @lends RuleLayer# */
	{
		/**
		 * @augments aperture.Layer
		 * @class A layer that renders horizontal or vertical lines
		 * across a {@link aperture.chart.ChartLayer ChartLayer}. RuleLayers are not added 
		 * to a chart in conventional layer fashion, rather they are instantiated the first time
		 * they are referenced via chart.{@link aperture.chart.ChartLayer#ruleLayer ruleLayer}
		 * 
		 * @mapping {'x'|'y'} axis
		 *   Specifies whether the line is vertically or horizontally aligned.
		 * 
		 * @mapping {Number} rule
		 *   Raw data value to be mapped to a pixel position on the chart.
		 * 
		 * @mapping {Number=1} opacity
		 *   The opacity of the rule line.
		 * 
		 * @mapping {String='rule'} stroke
		 *   The colour used to draw the rule lines.
		 * 
		 * @mapping {Number=1} stroke-width
		 *   The width of the rule line.
		 *   
		 * @mapping {'solid'|'dotted'|'dashed'|'none'| String} stroke-style
		 *  The line style as a predefined option or custom dot/dash/space pattern such as '--.-- '.
		 *  A 'none' value will result in the rule not being drawn.
		 * 
		 * @constructs
		 * @factoryMade
		 */
		init : function(spec, mappings) {
			aperture.Layer.prototype.init.call(this, spec, mappings);
		},

		canvasType : aperture.canvas.VECTOR_CANVAS,

		render : function(changeSet) {
			// Get the changed components.
			aperture.util.forEach( changeSet.updates, function (node) {
				node.graphics.removeAll(node.userData.rulelines);
				node.userData.rulelines = [];
				createRule.call(this, node);
			}, this);
		}
	});

	namespace.RuleLayer = RuleLayer;
	return namespace;
}(aperture.chart || {}));
/**
 * Source: Raphael.js
 * Copyright (c) 2014 Oculus Info Inc.
 * @fileOverview A Raphael canvas implementation.
 */

/**
 * @namespace
 * @ignore
 * Ensure namespace exists
 */
aperture.canvas = (
/** @private */
function(namespace) {

	// nothing we can do in this case.
	if (!window.Raphael) {
		aperture.log.error('raphael.js must be included before aperture.');

		return;
	}
	
	var util = aperture.util;

//	function noop() {
//		aperture.log.warn('Clip and origin operations are not supported for the root Raphael canvas.');
//	}

	// this is here to overcome this problem:
	// http://stackoverflow.com/questions/7448468/why-cant-i-reliably-capture-a-mouseout-event
	var MouseOutTracker = aperture.Class.extend( '[private].MouseOutTracker', {
		
		// callback for all document move events. only listening when over / tracking something.
		mouseMoveCallback : function (e) {
			if (this.trackEvent) {
				var node = e.target;
				
				// climb the hierarchy and look here only for wander outside of our target scope.
				// outs within our target scope will be handled by the track call below
				while (node != null) {
					if (node === this.trackTarget) {
						return;
					}
					node = node.parentNode;
				}
				
				// if made it this far we are outside
				this.trigger(e);
				this.untrack();
			}
		},
		
		// tracks an event
		track : function (e, client) {
			var tracking = false;
			var sourceEvent = e.source;
			
			// already tracking?
			if (this.trackEvent) {
				tracking = true;

				// switching targets?
				// browser will fire mouse outs for all child outs (unfortunately) even if currentTarget is the same, so replicate.
				if (this.trackEvent.source.target !== sourceEvent.target) {
					this.trigger(sourceEvent);
				}
			}
				
			this.trackEvent = e;
			this.trackTarget = sourceEvent.currentTarget;
			this.client = client;
			
			// looking at raphael tells us that this tracks at the document level if not called on a raphael el.
			// args are the 'element', the callback function, and the context to be 'this' in the callback
			if (!tracking) {
				Raphael.el.mousemove.call(this, this.mouseMoveCallback, this);
			}
		},

		// stop tracking
		untrack : function () {
			if (this.trackEvent) {
				this.trackEvent = null;
				this.trackTarget = null;
				this.client = null;
				
				Raphael.el.unmousemove.call(this, this.mouseMoveCallback);
			}
		},
		
		// trigger mouse out
		trigger : function(curEvent) {
			
			// extend old event with updated properties without changing the original event.
			var e = util.viewOf(this.trackEvent);
			
			e.source = util.viewOf(e.source);
			
			//  update with cur position
			e.source.clientX = curEvent.clientX;
			e.source.clientY = curEvent.clientY;
			e.source.screenX = curEvent.screenX;
			e.source.screenY = curEvent.screenY;

			if (curEvent.pageX != null) {
				e.source.pageX = curEvent.pageX;
				e.source.pageY = curEvent.pageY;
			}
			
			if (curEvent.offsetX != null) {
				e.source.offsetX = curEvent.offsetX;
				e.source.offsetY = curEvent.offsetY;
			}
			
			this.client.trigger(e.eventType = "mouseout", e);
		}
		
	});
	
	// graphics class
	var cg = namespace.RaphaelGraphics = namespace.VectorGraphics.extend( 'aperture.canvas.RaphaelGraphics',
		{
			init : function( parent ) {
				this.canvas = parent;
				this.paper = parent.paper;
				this.animation = parent.animation;
				this.container = parent.paper.container();
				this.showing = true;
				this.events = {};
				
				if (parent.container) {
					parent.container.push(this.container);
					this.canvas = parent.canvas;
					this.parent = parent;
				}
			},

			path : function ( svg ) {
				var el = this.paper.path( svg );
				this.container.push(el);

				return el;
			},
			
			circle : function ( x, y, radius ) {
				var el = this.paper.circle( x, y, radius );
				this.container.push(el);

				return el;
			},

			rect : function ( x, y, width, height ) {
				var el = this.paper.rect( x, y, width, height );
				this.container.push(el);

				return el;
			},

			text : function ( x, y, text ) {
				var el = this.paper.text( x, y, text );
				el.attr('cursor', 'default'); // don't typically want the text cursor
				
				this.container.push(el);

				return el;
			},

			image : function ( src, x, y, width, height ) {
				var el = this.paper.image( src, x, y, width, height );
				this.container.push(el);

				return el;
			},

			clip : function( rect ) {
				this.container.clip( rect );
			},

			origin : function( x, y ) {
				this.container.origin( x, y );
			},

			display : function( show ) {
				if (this.showing !== show) {
					this.container.show( this.showing = show );
					return true;
				}
			},

			toFront : function( e ) {
				if (e) {
					e.toFront();
				} else {
					var me = this.container.node;
					me.parentNode.appendChild(me);
				}
	
				return this;
			},

			toBack : function( e ) {
				if (e) {
					e.toBack();
				} else {
					var me = this.container.node, pops = me.parentNode;
					if (!pops.firstChild.isEqualNode(me)) {
						pops.insertBefore(me, pops.firstChild);
					}	
				}

				return this;
			},

			remove : function( e ) {
				// remove self?
				if (arguments.length === 0) {
					if (this.mouseOutTracker != null) {
						this.mouseOutTracker.untrack();
					}
					
					this.container.remove();
					
					return this;
				}
				
				if ( e ) {
					var k = this.container.kids,
						i = util.indexOf(k, e);

					if (i >= 0) {
						k.splice(i, 1);
					}
					
					e.remove();
					
					return e;
				}
			},
			
			removeAll : function ( array ) {
				var i, n, k = this.container.kids;//, r= [];
				
				if ( array != null ) {
					
					for (i=array.length; i-->0;) {
						if ((n = util.indexOf(k, array[i])) >= 0) {
							//r.push(k[ia]);
							k.splice(n, 1);
						}
						array[i].remove();						
					}
					
					//return r;
					
				} else {
					for (i=0, n=k.length; i<n; ++i) {
						k[i].remove();
					}
					
					this.container.kids.length = 0;
					
					//return k;
				}
			},

			data : function(element, data, index) {
				var n = arguments.length;
				
				switch (n) {
				case 0:
					return this.dataObj;
					
				case 1:
					if (!element.raphael) {
						this.dataObj = element;
						return this;
					} 
					return element.data('data');
					
				default:
					element.data('data', {
						data : data,
						index: index
					});
					return this;
				}
			},
			
			dataFromEvent : function(e) {
				var elem = e.target = (e.srcElement || e.target) || document, myNode = this.container.node;
				
				if (elem) {
					var data;
					
					// look up node tree from target for raphael elem
					while ((!elem.raphael || !(data = this.paper.getById(elem.raphaelid).data('data')))) {
						if (elem == myNode) {
							break;
						}
						elem = elem.parentNode;
	
						// will never happen?
						if (elem == null) {
							return;
						}
					}
	
					return data || this.dataObj;
				}
			},
			
			on : function(eventType, notifier) {
				// already added.
				if (this.events[eventType] || !Raphael.el[eventType]) {
					return;
				}
				
				var that = this;

				// everything but drag (a special case)
				if (eventType !== 'drag') {
					Raphael.el[eventType].call(
						this.container,
						
						this.events[eventType] = function(e) {
							var data = that.dataFromEvent(e);
							
							if (data) {
								var theEvent = {
									data: data.data,
									node: new aperture.Layer.SingleNodeSet(that.dataObj),
									index: data.index? data.index.slice() : undefined, // clone
									eventType: eventType,
									source : e
								};

								// this is here to overcome this problem:
								// http://stackoverflow.com/questions/7448468/why-cant-i-reliably-capture-a-mouseout-event
								switch (eventType) {
								case 'mouseover':
								case 'mousemove':
									if (this.mouseOutTracker == null) {
										this.mouseOutTracker = new MouseOutTracker();
									}
									this.mouseOutTracker.track(theEvent, notifier);
									break;
									
								case 'mouseout':
									if (this.mouseOutTracker != null) {
										this.mouseOutTracker.untrack();
									}
								}
								
								notifier.trigger(eventType, theEvent);
							}
						}
					);
					
				} else {
					var data;
					
					Raphael.el.drag.call(
						this.container,
						
						function(dx,dy,x,y,e) {
							if (data) {
								notifier.trigger('drag', {
									data: data.data,
									node: new aperture.Layer.SingleNodeSet(that.dataObj),
									index: data.index? data.index.slice() : undefined, // clone
									eventType: 'drag', // ***
									source : e,
									dx: dx,
									dy: dy,
									x : x,
									y : y
								});
							}
						},
						function(x,y,e) {
							data= that.dataFromEvent(e);
							
							if (data) {
								notifier.trigger('drag', {
									data: data.data,
									node: new aperture.Layer.SingleNodeSet(that.dataObj),
									index: data.index? data.index.slice() : undefined, // clone
									eventType: 'dragstart', // ***
									source : e,
									x : x,
									y : y
								});
							}
						},
						function(e) {
							if (data) {
								notifier.trigger('drag', {
									data: data.data,
									node: new aperture.Layer.SingleNodeSet(that.dataObj),
									index: data.index? data.index.slice() : undefined, // clone
									eventType: 'dragend', // ***
									source : e
								});
							}
						}
					);
				}
			},
			
			off : function(eventType) {
				if (eventType === 'drag') {
					Raphael.el.undrag.call(this.container);
					
				} else {
					var fn = Raphael.el['un'+ eventType], cb = events[eventType];
					
					if (fn && cb) {
						fn.call(this.container, cb);
					}
				}
			},
			
			apparate : function(element, transition) {
				if (transition) {
					var op = element.attr('opacity');

					// fade in
					element.attr('opacity', 0);
					this.update(element, {'opacity': op}, transition);
				}
			},

			update : function(element, attrs, transition) {
				if (!transition) {
					element.attr(attrs);

				} else {

					// filter. note this is a cheap method of trying to weed down what has
					// actually changed.
					var aset = {}, sset = {}, hasa, hass, attr, nv, cv;
					for (attr in attrs) {
						if (attrs.hasOwnProperty(attr)) {
							cv = element.attr(attr);
							nv = attrs[attr];
							
							// transform and path are returned from raphael as arrays, so need to convert to compare
							if (cv != null && !cv.toFixed) {
								cv = cv.toString();
							}
							// new values can only be numbers or strings, so do the conversion here for comparison.
							if (nv != null && !nv.toFixed) {
								nv = nv.toString();
							}
							if (cv !== nv) {
								// can't animate strings except for these.
								if (cv && cv.charCodeAt) {
									switch(attr) {
									case 'clip-rect':
									case 'fill':
									case 'stroke':
									case 'path':
									case 'transform':
										break;
									default:
										cv = null;
									}
								}
								
								if (cv) {
									hasa = true;
									aset[attr] = nv;
								} else {
									hass = true;
									sset[attr] = nv;
								}
							}
						}
					}

					if (hass) {
						element.attr(sset);
					}
					
					if (hasa) {
						var dummy = this.animation.anim;
	
						// create sync element?
						if (!dummy) {
	
							// this is unfortunate but otherwise we risk the tracked element being removed
							dummy = this.animation.anim = {element: this.paper.rect(-9999, -9999, 1, 1)};
							dummy.animation = Raphael.animation({width:2},
									transition.milliseconds(),
									transition.easing(),
	
									function() {
										dummy.element.remove();
	
										// one callback is enough
										if (transition.callback()) {
											transition.callback()();
										}
								});
	
							// this dummy animation serves no purpose other than to be a
							// reference for syncing other elements, due to the way Raphael works.
							dummy.element.animate(dummy.animation);
	
						}
	
						// link with dummy.
						element.animateWith(
							dummy.element,
							dummy.animation,
							aset,
							transition.milliseconds(),
							transition.easing()
							);
					}
				}
			}
		}
	);

	namespace.RaphaelCanvas = namespace.VectorCanvas.extend( 'aperture.canvas.RaphaelCanvas',
		{
			init : function( root ) {
				namespace.VectorCanvas.prototype.init.call(this, root);
				this.paper = Raphael( root, '100%', '100%' );
				this.animation = {};
			},

			remove : function() {
				return this.paper.remove();
			},

			graphics : function( parent ) {
				if (! parent || ! parent.typeOf(namespace.RaphaelGraphics) ) {
					return new cg( this );
				}

				return new cg( parent );
			},

			// signals the end of a frame
			flush : function() {
				this.animation.anim = null;

				// recurse if necessary.
				namespace.VectorCanvas.prototype.flush.call(this);
			}

		}
	);


	namespace.handle( namespace.VECTOR_CANVAS, namespace.RaphaelCanvas );


	// RAPHAEL.JS SUPPORT FOR STANDARD EASINGS

	// these are not actually the same, but similar.
	Raphael.easing_formulas['ease'] = Raphael.easing_formulas['ease-in-out'];

	// CSS3:
	//The ease function is equivalent to cubic-bezier(0.25, 0.1, 0.25, 1.0).
	//The linear function is equivalent to cubic-bezier(0.0, 0.0, 1.0, 1.0).
	//The ease-in function is equivalent to cubic-bezier(0.42, 0, 1.0, 1.0).
	//The ease-out function is equivalent to cubic-bezier(0, 0, 0.58, 1.0).
	//The ease-in-out function is equivalent to cubic-bezier(0.42, 0, 0.58, 1.0)

	// RAPHAEL.JS EXTENSION FOR CONTAINER SUPPORT

	// utility remove function
	function removeThis( nodeField ) {
		var n = this[nodeField];

		if (n && n.parentNode) {
			n.parentNode.removeChild( n );
		}
		if (this.hasOwnProperty(nodeField)) {
			delete this[nodeField];
		}
	}


	var origin, clip, elem, R = Raphael;


	// VML?
	if (R.vml) {

		// element factory
		elem = function() {
			var el = document.createElement('div');
			el.style.position = 'absolute';
			el.style.left = el.style.top = '0';

			return el;
		};

		// public origin fn
		origin = function ( x, y ) {
			var css = this.node.style;

			css.left = x + 'px';
			css.top = y + 'px';

			return this;
		};

		// public clip fn
		clip = function ( rect ) {
			var css = this.node.style;

			if (rect && rect.length == 4) {

				css.clip = 'rect('
					+ rect[1] + 'px '
					+(rect[0] + rect[2]) + 'px '
					+(rect[1] + rect[3]) + 'px '
					+ rect[0] + 'px)';

			} else if (css.clip) {

				css.clip = '';
			}

			return this;
		};

	// else SVG
	} else {

		// element factory
		elem = function ( el ) {
			el = document.createElementNS("http://www.w3.org/2000/svg", el);
			return el;
		};

		// public origin fn
		origin = function( x, y ) {
			this.node.setAttribute('transform', 'translate(' + x + ', ' + y + ')');

			return this;
		};

		// public clip fn
		clip = function ( rect ) {

			// remove existing clip path
			removeThis.call(this, 'clipNode');

			var ref = '';

			// create?
			if (rect && rect.length == 4) {

				// new clip path
				var el = this.clipNode = elem('clipPath'),
					rc = elem('rect');

				rc.setAttribute('x', String(rect[0]));
				rc.setAttribute('y', String(rect[1]));
				rc.setAttribute('width', String(rect[2]));
				rc.setAttribute('height', String(rect[3]));

				// give it an id to reference below
				el.id = R.createUUID();
				el.appendChild(rc);

				this.paper.defs.appendChild(el);

				ref= 'url(#' + el.id + ')';
			}

			this.node.setAttribute('clip-path', ref);

			return this;
		};

	}

	// note this addition method for containers reparents the node but has no
	// choice but to leave the graphic where it is in the paper's private linked list
	// until removed. this seems to be relatively free of negative impact.
	function push( child ) {

		var i = 0, n;
		// recurse for sets
		if ( child.type === 'set' ) {
			for ( n = child.length; i < n; ++i ) {
				push.call( this, child[i] );
			}

		// at the leaf level, append
		} else {
			this.kids.push(child);
			this.node.appendChild( child.node );
		}

		return this;
	}


	// also expose the remove function (important)
	function remove() {
		removeThis.call(this, 'node');
		removeThis.call(this, 'clipNode');
		
		var i= 0, n, k= this.kids;
		
		for ( n = k.length; i < n; ++i ) {
			k[i].remove();
		}
		
		this.kids.length = 0;
	}

	// show / hide
	function show( b ) {
		this.node.style.display = b? '' : 'none';

		return this;
	}

	/**
	 * add the container to the function set, for use as paper.container().
	 * @private
	 */
	R.fn.container = function() {

		var node = elem( 'g' );
		
		// The container MUST have a UNIQUE ID, otherwise the
		// mouse events will not be triggered properly.
		// (e.g. if id=undefined, ALL drag move listeners will
		// be fired instead of the listener associated with
		// the source element)
		var uid = R.createUUID();
//		node.raphael = true;
//		node.raphaelid = uid;

		// add to vml canvas
		this.canvas.appendChild( node );

		return {
			id : uid,
			node : node,
			paper : this,
			type : 'container',
			push : push,
			kids : [],
			show : show,
			remove : remove,
			origin : origin,
			clip : clip,
			// should maybe look at using the element as a prototype?
			mousedown : R.el.mousedown // this is here for raphael's dragging to work I believe?
		};
	};

	return namespace;

}(aperture.canvas || {}));



return aperture;
}(aperture || {}));