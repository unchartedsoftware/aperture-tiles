/**
 * Copyright (c) 2013 Oculus Info Inc. http://www.oculusinfo.com/
 * 
 * Released under the MIT License.
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oculusinfo.tile;



import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletContextEvent;

import com.google.inject.Singleton;

import oculus.aperture.ApertureServerConfig;



@Singleton
public class TileServiceConfiguration extends ApertureServerConfig {
    private List<ServletLifecycleListener> _lifecycleListeners;

    public TileServiceConfiguration () {
        _lifecycleListeners = new ArrayList<>();
    }

    public void addLifecycleListener (ServletLifecycleListener listener) {
        _lifecycleListeners.add(listener);
    }

    public void removeLifecycleListener (ServletLifecycleListener listener) {
        _lifecycleListeners.remove(listener);
    }

    @Override
    public void contextInitialized (ServletContextEvent event) {
        // Notify any listeners
        if (null != _lifecycleListeners) {
            for (ServletLifecycleListener listener: _lifecycleListeners) {
                listener.onServletInitialized(event);
            }
        }

        super.contextInitialized(event);
    }

    @Override
    public void contextDestroyed (ServletContextEvent event) {
        // Notify any listeners
        if (null != _lifecycleListeners) {
            for (ServletLifecycleListener listener: _lifecycleListeners) {
                listener.onServletDestroyed(event);
            }
        }

        super.contextDestroyed(event);
    }
}
