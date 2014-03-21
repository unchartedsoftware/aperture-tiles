/**
 * Copyright (c) 2013 Oculus Info Inc.
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
package com.oculusinfo.tile;

import java.util.ArrayList;
import java.util.List;

import org.apache.spark.api.java.JavaSparkContext;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.name.Named;

public class SparkModule extends AbstractModule {
    @Override
    protected void configure () {
    }

    private String getJarPathForClass (Class<?> type) {
        return type.getProtectionDomain().getCodeSource().getLocation().getPath();
    }

    @Provides
    JavaSparkContext provideSparkContext (@Named("org.apache.spark.master")  String master,
                                          @Named("org.apache.spark.jobName") String jobName,
                                          @Named("org.apache.spark.home")    String sparkHome,
                                          @Named("org.apache.spark.jars")    String extraJars) {
        // Construct our jarlist
        List<String> jarList = new ArrayList<>();
        // First, get our known needed jars from our own classpath
        // Include binning-utilities
        jarList.add(getJarPathForClass(com.oculusinfo.binning.TilePyramid.class));
        // Include tile-generation
        jarList.add(getJarPathForClass(com.oculusinfo.tilegen.tiling.TileIO.class));
        // Include any additionally configured jars
        if (null != extraJars && !extraJars.isEmpty()) {
            for (String extraJar: extraJars.split(":")) {
                extraJar = extraJar.trim();
                if (!extraJar.isEmpty())
                    jarList.add(extraJar);
            }
        }
        String[] jars = jarList.toArray(new String[jarList.size()]);

        return new JavaSparkContext(master, jobName, sparkHome, jars);
    }
}
