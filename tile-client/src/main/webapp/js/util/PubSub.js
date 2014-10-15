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
    A hierarchical publish and subscribe object. Channels consist of strings, sub-channels
    are separated with a period ('.'). Publishing to a target channel will propagate the message
    breadth first as follows:
        1) from the root of the hierarchy to the target channel,
        2) from the target channel to all existing sub-channels

    Ex. Hierarchy:

            a -> [ a.a, a.ab ] -> [ a.a.a, a.a.b, a.a.c ]

        Publishing to a.a will publish to

            1) from root to target

                a -> a.a

            2) from target to all sub-channels

                a.a -> a.a.a -> a.a.b -> a.a.c

        Publishing to a.a.c will publish

            1) from root to target

                a -> a.a -> a.a.c

            2) from target to all sub-channels

                a.a.c -> null
*/

define(function (require) {

    "use strict";

    return {

        /**
         * Subscribe a listener function to the specific channel path.
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
                channel.children[ path ] = channel.children[ path ] || {
                    path : currentPath,
                    subscribers : [],
                    children : {}
                };
                channel = channel.children[ path ];
            }
            channel.subscribers.push( subscriber );
        },


        /**
         * Publish a message to a channel path. All sub-channels will also
         * received message.
         */
        publish: function( channelPath, message ) {

            var paths = channelPath.split('.'),
                children,
                subscribers,
                queue = [],
                path, i, sub,
                leafChannel,
                channel = this.channels || {
                    subscribers : [],
                    children : {}
                };

            // find channel
            while ( paths.length > 0 ) {
                path = paths.shift();
                if ( channel.children[ path ] === undefined ) {
                    return;
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
});
