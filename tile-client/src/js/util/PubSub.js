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
 * @namespace PubSub
 * @classdesc A hierarchical publish and subscribe namespace. Channels consist of strings, sub-channels
 * are separated with a period ('.'). Publishing to a target channel will propagate the message
 * breadth first from the root of the hierarchy to the target channel, then from the target channel to
 * all its existing sub-channels.
 * <pre>
 * Ex.
 *
 *     Hierarchy:
 *
 *         layer -> server -> abc28d05-9b9d-4e03-9f53-9f88cf7078c7
 *                            fb943cca-cac3-4bbf-ba03-91a1559fee28
 *                            67fd55b5-dc3a-40cf-8adc-9e634b82d474
 *                  client -> 3392103f-7f50-4422-ae59-2c2c0971951f
 *                            7bc33c81-c347-4bea-9cfb-c22328bcb648
 *
 *     Publishing to 'layer.server' will publish:
 *
 *         From root to target:
 *
 *             1) layer
 *
 *     From target to all sub-channels:
 *
 *             2) layer.server
 *             3) layer.server.abc28d05-9b9d-4e03-9f53-9f88cf7078c7
 *             4) layer.server.fb943cca-cac3-4bbf-ba03-91a1559fee28
 *             5) layer.server.67fd55b5-dc3a-40cf-8adc-9e634b82d474
 *
 *     Publishing to 'layer.client.3392103f-7f50-4422-ae59-2c2c0971951f' will publish:
 *
 *         From root to target:
 *
 *             1) layer
 *             2) layer.client
 *
 *         From target to all sub-channels:
 *
 *             3) layer.client.3392103f-7f50-4422-ae59-2c2c0971951f
 * </pre>
 */
( function() {

    "use strict";

    function createChannel( path ) {
        return {
            path : path,
            subscribers : [],
            children : {}
        };
    }

    module.exports = {

        /**
         * Subscribe a listener function to the specific channel path.
         * @memberof PubSub
         *
         * @param channelPath {string}   A '.' delimited channel path.
         * @param subscriber  {Function} The subscriber function associated with the provided path.
         */
        subscribe: function( channelPath, subscriber ) {

            var paths = channelPath.split('.'),
                path,
                currentPath = '',
                channel;

            this.channels = this.channels || {
                subscribers : [],
                children : {}
            };

            channel = this.channels;

            while ( paths.length > 0 ) {
                path = paths.shift();
                currentPath += ( currentPath.length > 0 ) ? '.' + path : path;
                if ( !channel.children[ path ] ) {
                    channel.children[ path ] = createChannel( currentPath );
                }
                channel = channel.children[ path ];
            }
            channel.subscribers.push( subscriber );
        },

        /**
         * Unsubscribe a listener function from the specific channel path.
         * @memberof PubSub
         *
         * @param channelPath {string}   A '.' delimited channel path.
         * @param subscriber  {Function} The subscriber function associated with the provided path.
         */
        unsubscribe: function( channelPath, subscriber ) {

            var paths = channelPath.split('.'),
                path,
                currentPath = '',
                channel;

            this.channels = this.channels || {
                subscribers : [],
                children : {}
            };

            channel = this.channels;

            while ( paths.length > 0 ) {
                path = paths.shift();
                currentPath += ( currentPath.length > 0 ) ? '.' + path : path;
                if ( !channel.children[ path ] ) {
                    channel.children[ path ] = createChannel( currentPath );
                }
                channel = channel.children[ path ];
            }
            var index = channel.subscribers.indexOf( subscriber );
            if ( index !== -1 ) {
                channel.subscribers.splice( index, 1 );
            }
        },

        /**
         * Publish a message to a channel path. Publishing to a target channel will propagate the message
         * breadth first from the root of the hierarchy to the target channel, then from the target channel
         * to all existing sub-channels
         * @memberof PubSub
         *
         * @param channelPath {string}   A '.' delimited channel path.
         * @param message  {*}       The messsage to be published.
         */
        publish: function( channelPath, message ) {

            var paths = channelPath.split('.'),
                children,
                subscribers,
                queue = [],
                path, i, sub,
                leafChannel,
                currentPath = '',
                channel = this.channels || {
                    subscribers : [],
                    children : {}
                };

            // find channel
            while ( paths.length > 0 ) {
                path = paths.shift();
                currentPath += ( currentPath.length > 0 ) ? '.' + path : path;
                if ( !channel.children[ path ] ) {
                    channel.children[ path ] = createChannel( currentPath );
                }
                channel = channel.children[ path ];
                queue.push( channel );
            }

            leafChannel = queue.pop();

            // breadth first publishing from root to target
            while ( queue.length > 0 ) {
                channel = queue.shift();
                subscribers = channel.subscribers;
                // publish to current channel
                for ( i=0; i<subscribers.length; i++ ) {
                    subscribers[i]( message, channelPath );
                }
            }

            queue = [ leafChannel ];

            // breadth first publishing from target to all children
            while ( queue.length > 0 ) {
                channel = queue.shift();
                subscribers = channel.subscribers;
                children = channel.children;
                // publish to current channel
                for ( i=0; i<subscribers.length; i++ ) {
                    subscribers[i]( message, channelPath );
                }
                // queue up children for leaf channel
                for ( sub in children ) {
                    if ( children.hasOwnProperty( sub ) ) {
                        queue.push( children[ sub ] );
                    }
                }
            }
        }
    };
}());
