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
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletContextEvent;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.oculusinfo.binning.util.JsonUtilities;
import com.oculusinfo.factory.util.Pair;
import org.apache.avro.generic.GenericData;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileContext;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.spark.SparkConf;
import org.apache.spark.SparkContext;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.SQLContext;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;

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

	private String     _master;
	private String     _jobName;
	private String     _sparkHome;
	private Properties _connectionProperties;
	private String[]   _sparkConfigurations;
	private String[]   _hadoopConfigurations;
	private String[]   _jars;

	private JavaSparkContext _context;
	private SQLContext _sqlContext;

	@Inject
	public SparkContextProviderImpl (@Named("org.apache.spark.master") String master,
	                                 @Named("org.apache.spark.jobName") String jobName,
	                                 @Named("org.apache.spark.home") String sparkHome,
	                                 @Named("org.apache.spark.jars") String extraJars,
									 @Named("org.apache.spark.properties") String connectionProperties,
									 @Named("org.apache.spark.configurations") String sparkConfigurations,
									 @Named("org.apache.hadoop.configurations") String hadoopConfigurations,
									 @Named("org.apache.spark.tmpDir") String tmpDir,
	                                 TileServiceConfiguration config) {
		_master = master;
		_jobName = jobName;
		_sparkHome = sparkHome;
		_sparkConfigurations = sparkConfigurations.split(",");
		_hadoopConfigurations = hadoopConfigurations.split(",");

		try {
			_connectionProperties = JsonUtilities.jsonObjToProperties(new JSONObject(connectionProperties));
		} catch (JSONException e) {
			LOGGER.warn("Error reading spark connection configuration", e);
			_connectionProperties = new Properties();
		}

		// Construct our jarlist
		List<Class<?>> jarClasses = new ArrayList<>();
		// First, get our known needed jars from our own classpath
		// Include binning-utilities
		jarClasses.add(com.oculusinfo.binning.TilePyramid.class);
		// Include tile-generation
		jarClasses.add(com.oculusinfo.tilegen.tiling.TileIO.class);
		// Include the HBase jar
		// jarClasses.add(org.apache.hadoop.hbase.HBaseConfiguration.class);
		// INclude the Yarn jar
		// jarClasses.add(org.apache.spark.scheduler.cluster.YarnScheduler.class);
		// Include any additionally configured jars
		if (null != extraJars && !extraJars.isEmpty()) {
			for (String extraJar: extraJars.split(":")) {
				extraJar = extraJar.trim();
				if (!extraJar.isEmpty()) {
					try {
						Class<?> jarRepresentative = Class.forName(extraJar);
						jarClasses.add(jarRepresentative);
					} catch (ClassNotFoundException e) {
						LOGGER.warn("Couldn't find class "+extraJar, e);
					}
				}
			}
		}
		_jars = getJarLocations(jarClasses, tmpDir);

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

	private Configuration readHadoopConfiguration (boolean loadDefaults, String... configurations) {
		Configuration conf = new Configuration(loadDefaults);
		for (String configFile : configurations) {
			InputStream configStream = getClass().getResourceAsStream(configFile.trim());
			conf.addResource(configStream);
		}
		return conf;
	}

	private List<Pair<String, String>> configurationProperties (String... configurations) {
		Configuration conf = readHadoopConfiguration(false, configurations);
		List<Pair<String, String>> results = new ArrayList<>();
		for (Map.Entry<String, String> entry: conf) {
			String key = entry.getKey();
			String value = entry.getValue();
			results.add(new Pair<>(key, value));
		}
		return results;
	}

	private String[] getJarLocations (List<Class<?>> representativeClasses, String tmpDir) {
		// Get an hdfs connection
		Configuration conf = readHadoopConfiguration(true, _hadoopConfigurations);

		List<String> jars = new ArrayList<>();
		try {
			FileSystem fs = FileSystem.get(conf);
			// Create a temporary application directory.
			Path tmp = new Path(tmpDir);
			if (!fs.exists(tmp)) {
				fs.mkdirs(tmp);
				fs.deleteOnExit(tmp);
			}

			for (Class<?> repClass: representativeClasses) {
				Path localJarPath = new Path(getJarPathForClass(repClass));
				Path hdfsJarPath = new Path(tmp, localJarPath.getName());
				fs.copyFromLocalFile(false, true, localJarPath, hdfsJarPath);
				fs.deleteOnExit(hdfsJarPath);
				jars.add(hdfsJarPath.toString());
			}
		} catch (IOException e) {
			LOGGER.warn("Error copying jars to HDFS", e);
		}

		return jars.toArray(new String[0]);
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
	public SparkContext getSparkContext () {
		return JavaSparkContext.toSparkContext(getJavaSparkContext());
	}

	@Override
	synchronized public JavaSparkContext getJavaSparkContext () {
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

			// Copy in configuration properties from our spark configuration files
			for (Pair<String, String> entry: configurationProperties(_sparkConfigurations)) {
				config.set(entry.getFirst(), entry.getSecond());
			}

			// Copy in configuration properties from our hadoop configuration files (but with "spark.hadoop." prepended
			// to them, so that spark will treat them as hadoop configuration properties)
			for (Pair<String, String> entry: configurationProperties(_hadoopConfigurations)) {
				config.set("spark.hadoop."+entry.getFirst(), entry.getSecond());
			}

			// Copy in configuration properties that begin with "akka." and "spark.", overriding those from config files
			for (Object keyObj: _connectionProperties.keySet()) {
				String key = keyObj.toString();
				if (key.toLowerCase().startsWith("akka.")
					|| key.toLowerCase().startsWith("spark.")
					|| key.toLowerCase().startsWith("yarn.")) {
					String value = _connectionProperties.get(key).toString();
					config.set(key, value);
				}
			}

			_context = new JavaSparkContext(config);
		}
		return _context;
	}

	@Override
	public SQLContext getSQLContext () {
		if (null == _sqlContext) {
			SparkContext sc = getSparkContext();
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
