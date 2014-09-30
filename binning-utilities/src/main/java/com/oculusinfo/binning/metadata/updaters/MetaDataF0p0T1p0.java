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

import java.util.Collections;
import java.util.List;

import com.oculusinfo.binning.metadata.AddPropertyMutator;
import com.oculusinfo.binning.metadata.PropertyIndexToArrayMutator;
import com.oculusinfo.binning.metadata.PropertyRelocationMutator;
import com.oculusinfo.binning.metadata.PyramidMetaDataVersionMutator;
//import com.oculusinfo.binning.metadata.RemovePropertyMutator;

public class MetaDataF0p0T1p0 extends PyramidMetaDataVersionMutator {
    public static void register () {
        ALL_MUTATORS.add(new MetaDataF0p0T1p0());
    }
    private static class ZoomLevelMutator extends PropertyIndexToArrayMutator<Integer> {
        ZoomLevelMutator () {
            super(new String[] {"meta", "levelMaximums", "(\\d+)"},
                      new String[] {"zoomlevels"},
                      "\\2.1");
        }

        @Override
        protected Integer mutateValue (String value) {
            return Integer.parseInt(value);
        }

        @Override
        protected void sort (List<Integer> values) {
            Collections.sort(values);
        }
    }
    public MetaDataF0p0T1p0 () {
        super("0.0", "1.0",
              new ZoomLevelMutator(),
              new PropertyRelocationMutator("global minimum",
                                            new String[] {"meta", "levelMinimums", "0"},
                                            new String[] {"meta", "global", "minimum"}, false),
              new PropertyRelocationMutator("global maximum",
                                            new String[] {"meta", "levelMaximums", "0"},
                                            new String[] {"meta", "global", "maximum"}, false),
              new PropertyRelocationMutator("level minimum",
                                            new String[] {"meta", "levelMinimums", "(\\d+)"},
                                            new String[] {"meta", "\\2.0", "minimum"}, true),
              new PropertyRelocationMutator("level maximum",
                                            new String[] {"meta", "levelMaximums", "(\\d+)"},
                                            new String[] {"meta", "\\2.0", "maximum"}, true),
              new PropertyRelocationMutator("X tile size",
                                            new String[] {"tilesize"},
                                            new String[] {"tilesizex"}, false),
              new PropertyRelocationMutator("Y tile size",
                                            new String[] {"tilesize"},
                                            new String[] {"tilesizey"}, true),
              //new RemovePropertyMutator(new String[] {"minzoom"}),
              //new RemovePropertyMutator(new String[] {"maxzoom"}),
              new AddPropertyMutator("1.0", new String[] {"version"})
                );
    }
}
