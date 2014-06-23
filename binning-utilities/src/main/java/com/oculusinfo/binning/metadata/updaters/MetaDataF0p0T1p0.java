/*
 * Copyright (c) 2014 Oculus Info Inc. http://www.oculusinfo.com/
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
package com.oculusinfo.binning.metadata.updaters;

import com.oculusinfo.binning.metadata.PropertyRelocationMutator;
import com.oculusinfo.binning.metadata.PyramidMetaDataVersionMutator;
import com.oculusinfo.binning.metadata.RemovePropertyMutator;

public class MetaDataF0p0T1p0 extends PyramidMetaDataVersionMutator {
    public static void register () {
        ALL_MUTATORS.add(new MetaDataF0p0T1p0());
    }
    public MetaDataF0p0T1p0 () {
        super("0.0", "1.0",
              new PropertyRelocationMutator(new String[] {"meta", "levelMinimums", "(\\d+)"},
                                            new String[] {"meta", "\\2.0", "minimum"}),
              new PropertyRelocationMutator(new String[] {"meta", "levelMaximums", "(\\d+)"},
                                            new String[] {"meta", "\\2.0", "maximum"}),
              new PropertyRelocationMutator(new String[] {"meta", "levelMinimums", "0"},
                                            new String[] {"meta", "global", "minimum"}),
              new PropertyRelocationMutator(new String[] {"meta", "levelMaximums", "0"},
                                            new String[] {"meta", "global", "maximum"}),
              new RemovePropertyMutator(new String[] {"minzoom"}),
              new RemovePropertyMutator(new String[] {"maxzoom"}),
              new PropertyIndexToArrayMutator(new String[] {"meta", "levelMaximums", "(\\d+)"},
                                              new String[] {"zoomlevels"}) 
                );
    }
}
