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
package com.oculusinfo.sparktile.spark;


import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletContextEvent;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.spark.SparkConf;
import org.apache.spark.SparkContext;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.SQLContext;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.oculusinfo.tile.servlet.ServletLifecycleListener;
import com.oculusinfo.tile.TileServiceConfiguration;



/**
 * <p>
 * Simple implementation of a {@link SparkContextProvider}.
 * </p>
 *
 * <p>
 * This class provides a spark context based on four properties in the global properties file (tile.properties):
 * <dl>
 * <dt>org.apache.spark.master</dt>
 * <dd>The location of the spark master node.  This can be found at the top of the Spark web UI page</dd>
 *
 * <dt>org.apache.spark.jobName</dt>
 * <dd>The name with which the web server spark connection should be labeled on the Spark web UI page</dd>
 *
 * <dt>org.apache.spark.home</dt>
 * <dd>The home directory of spark on the cluster machines.</dd>
 *
 * <dt>org.apache.spark.jars</dt>
 * <dd>A :-separated list of jars to add to the spark job.  Binning-utilities, tile-generation, and hbase
 * are automatically added; anything else (such as custom tiling jars) must be added here.</dd>
 * </dl>
 * </p>
 *
 * @author nkronenfeld
 */
@Singleton
public class SparkContextProviderImpl implements SparkContextProvider {
	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(SparkContextProviderImpl.class);

	private String   _master;
	private String   _jobName;
	private String   _sparkHome;
	private String[] _jars;

	private JavaSparkContext _context;
	private SQLContext _sqlContext;

	@Inject
	public SparkContextProviderImpl (@Named("org.apache.spark.master") String master,
	                                 @Named("org.apache.spark.jobName") String jobName,
	                                 @Named("org.apache.spark.home") String sparkHome,
	                                 @Named("org.apache.spark.jars") String extraJars,
	                                 TileServiceConfiguration config) {
		_master = master;
		_jobName = jobName;
		_sparkHome = sparkHome;

		// Construct our jarlist
		List<String> jarList = new ArrayList<>();
		// First, get our known needed jars from our own classpath
		// Include binning-utilities
		jarList.add(getJarPathForClass(com.oculusinfo.binning.TilePyramid.class));
		// Include tile-generation
		jarList.add(getJarPathForClass(com.oculusinfo.tilegen.tiling.TileIO.class));
		// Include the HBase jar
		jarList.add(getJarPathForClass(org.apache.hadoop.hbase.HBaseConfiguration.class));
		// Include any additionally configured jars
		if (null != extraJars && !extraJars.isEmpty()) {
			for (String extraJar: extraJars.split(":")) {
				extraJar = extraJar.trim();
				if (!extraJar.isEmpty())
					jarList.add(extraJar);
			}
		}
		_jars = jarList.toArray(new String[jarList.size()]);

		config.addLifecycleListener(new ServletLifecycleListener() {
				@Override
				public void onServletInitialized (ServletContextEvent event) {
				}

				@Override
				public void onServletDestroyed (ServletContextEvent event) {
					shutdownSparkContext();
				}
			});
	}

	private String getJarPathForClass (Class<?> type) {
	    String location = type.getProtectionDomain().getCodeSource().getLocation().getPath();
	    // This whole "classes" case is to account for when we're trying to
	    // run from an IDE, which automatically adds projects to the class
	    // path.  we need class directories replaced with jars.  Note that
	    // this means the program won't work unless the project as been
	    // packaged; this was always the case, but is more explicitly so now.
	    if (location.endsWith("classes/")) {
	        File target = new File(location).getParentFile();
	        File[] children = target.listFiles(new FilenameFilter() {
                @Override
                public boolean accept (File dir, String name) {
                    return name.endsWith(".jar") && !name.endsWith("sources.jar") && !name.endsWith("javadoc.jar") && !name.endsWith("tests.jar");
                }
            });
	        if (null != children && 1 == children.length)
	            location = children[0].toURI().getPath();
	    }
	    return location;
	}

	@Override
	public SparkContext getSparkContext (JSONObject configuration) {
		return JavaSparkContext.toSparkContext(getJavaSparkContext(configuration));
	}

	@Override
	synchronized public JavaSparkContext getJavaSparkContext (JSONObject configuration) {
		if (null == _context) {
			// Thin out the log of spark spam
			Logger.getLogger("org.eclipse.jetty").setLevel(Level.WARN);
			Logger.getLogger("org.apache.spark").setLevel(Level.WARN);
			Logger.getLogger("org.apache.hadoop").setLevel(Level.WARN);
			Logger.getLogger("akka").setLevel(Level.WARN);


			SparkConf config = new SparkConf();
			config.setMaster(_master);
			config.setAppName(_jobName);
			config.setSparkHome(_sparkHome);
			config.setJars(_jars);
			config.set("spark.logConf", "true");

			// Copy in configuration properties that begin with "akka." and "spark."
			if (null != configuration) {
				for (String key: JSONObject.getNames(configuration)) {
					if (key.toLowerCase().startsWith("akka.") || key.toLowerCase().startsWith("spark.")) {
						try {
							String value = configuration.getString(key);
							config.set(key, value);
						} catch (JSONException e) {
							LOGGER.warn("Error getting value for key {}", key, e);
						}
					}
				}
			}
			_context = new JavaSparkContext(config);
		}
		return _context;
	}

	@Override
	public SQLContext getSQLContext (JSONObject configuration) {
		if (null == _sqlContext) {
			SparkContext sc = getSparkContext(configuration);
			_sqlContext = new SQLContext(sc);
		}
		return _sqlContext;
	}

	@Override
	synchronized public void shutdownSparkContext () {
		if (null != _context) {
			_context.stop();
			_context = null;
		}
	}
}
