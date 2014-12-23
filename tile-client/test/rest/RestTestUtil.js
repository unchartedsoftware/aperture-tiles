(function() {

    "use strict";

    var assert = require("assert");

    module.exports = {

        errorFunctionMock: function( method ) {
            var mock = {};
            mock[ method ] = function() {
                return {
                    then: function( success, error ) {
                        assert( error && typeof error === "function" );
                    }
                };
            };
            return mock;
        },

        successFunctionMock: function( method, isSuccessProvided ) {
            var mock = {};
            mock[ method ] = function() {
                return {
                    then: function( success ) {
                        assert( ( success !== null ) === isSuccessProvided );
                    }
                };
            };
            return mock;
        },

        withSuccessMock: function( method ) {
            var mock = {};
            mock[ method ] = function() {
                return {
                    then: function( success ) {
                        assert( success );
                    }
                };
            };
            return mock;
        },

        urlRequestMock: function( method, testUrl ) {
            var mock = {};
            mock[ method ] = function( url ) {
                assert( url === testUrl );
                return {
                    then: function() {
                    }
                };
            };
            return mock;
        }
    };

}());