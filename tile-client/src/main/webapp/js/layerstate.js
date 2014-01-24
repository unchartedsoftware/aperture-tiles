/**
 * Created by Chris Bethune on 23/01/14.
 */

/*global $, define */

define(['class'], function (Class) {
    "use strict";

    var LayerState, notify;

    notify = function (fieldName, callbacks) {
        var i;
        for (i = 0; i < callbacks.length; i += 1) {
            callbacks[i](fieldName);
        }
    };

    LayerState = Class.extend({
        ClassName: "LayerState",

        init: function (id) {
            this.id = id;
            this.name = id;
            this.enabled = false;
            this.opacity = 1.0;
            this.filterRange = [0.0, 1.0];
            this.rampType = "ware";
            this.rampFunction = "linear";
            this.rampImageUrl = "";
            this.callbacks = [];
        },

        addCallback: function (callback) {
            this.callbacks.push(callback);
        },

        removeCallback: function (observer) {
            var index = this.callbacks.indexOf(observer);
            if (index > -1) {
                this.callbacks = this.callbacks.splice(index, 1);
            }
        },

        getId: function () {
            return this.id;
        },

        getName: function () {
            return this.name;
        },

        setName: function (name) {
            if (this.name !== name) {
                this.name = name;
                notify("name", this.callbacks);
            }
        },

        isEnabled: function () {
            return this.enabled;
        },

        setEnabled: function (enabled) {
            if (this.enabled !== enabled) {
                this.enabled = enabled;
                notify("enabled", this.callbacks);
            }
        },

        getOpacity: function () {
            return this.opacity;
        },

        setOpacity: function (opacity) {
            if (this.opacity !== opacity) {
                this.opacity = opacity;
                notify("opacity", this.callbacks);
            }
        },

        getFilterRange: function () {
            return this.filterRange;
        },

        setFilterRange: function (filterRange) {
            if (this.filterRange[0] !== filterRange[0] && this.filterRange[1] !== filterRange[1]) {
                this.filterRange = filterRange;
                notify("filterRange", this.callbacks);
            }
        },

        getRampType: function () {
            return this.rampType;
        },

        setRampType: function (rampType) {
            if (this.rampType !== rampType) {
                this.rampType = rampType;
                notify("rampType", this.callbacks);
            }
        },

        getRampFunction: function () {
            return this.rampFunction;
        },

        setRampFunction: function (rampFunction) {
            if (this.rampFunction !== rampFunction) {
                this.rampFunction = rampFunction;
                notify("rampFunction", this.callbacks);
            }
        },

        getRampImageUrl: function() {
            return this.rampImageUrl;
        },

        setRampImageUrl: function(url) {
            if (this.rampImageUrl !== url) {
                this.rampImageUrl = url;
                notify("rampImageUrl", this.callbacks);
            }
        }
    });
    return LayerState;
});