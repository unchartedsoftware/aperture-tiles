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
package com.oculusinfo.binning.io;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oculusinfo.binning.io.impl.FileSystemPyramidIO;
import com.oculusinfo.binning.io.impl.HBasePyramidIO;
import com.oculusinfo.binning.io.impl.JDBCPyramidIO;
import com.oculusinfo.binning.io.impl.PyramidStreamSource;
import com.oculusinfo.binning.io.impl.ResourcePyramidStreamSource;
import com.oculusinfo.binning.io.impl.ResourceStreamReadOnlyPyramidIO;
import com.oculusinfo.binning.io.impl.SQLitePyramidIO;
import com.oculusinfo.binning.io.impl.ZipResourcePyramidStreamSource;
import com.oculusinfo.binning.util.Pair;
import com.oculusinfo.factory.ConfigurableFactory;
import com.oculusinfo.factory.properties.StringProperty;



/**
 * Factory class to create the standard types of PyramidIOs
 * 
 * @author nkronenfeld
 */
public class PyramidIOFactory extends ConfigurableFactory<PyramidIO> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PyramidIOFactory.class);



    public static StringProperty PYRAMID_IO_TYPE        = new StringProperty("type",
                                                                             "The location to and from which to read tile pyramids",
                                                                             "hbase",
                                                                             new String[] { "hbase", "file-system", "file", "jdbc", "resource", "zip", "sqlite"});
    public static StringProperty HBASE_ZOOKEEPER_QUORUM = new StringProperty("hbase.zookeeper.quorum",
                                                                             "Only used if type=\"hbase\".  An HBase configuration parameter, this should match the similar value in hbase-site.xml.  There is no default for this property.",
                                                                             null);
    public static StringProperty HBASE_ZOKEEPER_PORT    = new StringProperty("hbase.zookeeper.port",
                                                                             "Only used if type=\"hbase\".  An HBase configuration parameter, this should match the similar value in hbase-site.xml.",
                                                                             "2181");
    public static StringProperty HBASE_MASTER           = new StringProperty("hbase.master",
                                                                             "Only used if type=\"hbase\".  An HBase configuration parameter, this should match the similar value in hbase-site.xml.  There is no default for this property.",
                                                                             null);
    public static StringProperty ROOT_PATH              = new StringProperty("root.path",
                                                                             "Unused with type=\"hbase\".  Indicates the root path of the tile pyramid - either a directory (if \"file-system\"), a package name (if \"resource\"), the full path to a .zip file (if \"zip\"), the database path (if \"sqlite\"), or the URL of the database (if \"jdbc\").  There is no default for this property.",
                                                                             null);
    public static StringProperty EXTENSION              = new StringProperty("extension",
                                                                             "Used with type=\"file-system\", \"resource\", or \"zip\".  The file extension which the serializer should expect to find on individual tiles.",
                                                                             "avro");
    public static StringProperty JDBC_DRIVER            = new StringProperty("jdbc.driver",
                                                                             "Only used if type=\"jdbc\".  The full class name of the JDBC driver to use.  There is no default for this property.",
                                                                             null);

	// We meed a global cache of zip stream sources - zip files are very slow to
	// read, so the source is slow to initialize, so creating a new one each
	// time we run isn't feasible.
    private static Map<Pair<String, String>, ZipResourcePyramidStreamSource> _zipfileCache = new HashMap<>();
    private static ZipResourcePyramidStreamSource getZipSource (String rootpath, String extension) {
    	Pair<String, String> key = new Pair<>(rootpath, extension);
    	if (!_zipfileCache.containsKey(key)) {
    		synchronized (_zipfileCache) {
    			if (!_zipfileCache.containsKey(key)) {
    				URL zipFile = PyramidIOFactory.class.getResource(rootpath);
    				ZipResourcePyramidStreamSource source = new ZipResourcePyramidStreamSource(zipFile.getFile(), extension);
    				_zipfileCache.put(key, source);
    			}
    		}
    	}
    	return _zipfileCache.get(key);
    }


    public PyramidIOFactory (ConfigurableFactory<?> parent, List<String> path) {
        this(null, parent, path);
    }

    public PyramidIOFactory (String name, ConfigurableFactory<?> parent,
                             List<String> path) {
        super(name, PyramidIO.class, parent, path);

        addProperty(PYRAMID_IO_TYPE);
        addProperty(HBASE_ZOOKEEPER_QUORUM);
        addProperty(HBASE_ZOKEEPER_PORT);
        addProperty(HBASE_MASTER);
        addProperty(ROOT_PATH);
        addProperty(EXTENSION);
        addProperty(JDBC_DRIVER);
    }



    @Override
    protected PyramidIO create () {
        String pyramidIOType = getPropertyValue(PYRAMID_IO_TYPE);

        try {
            if ("hbase".equals(pyramidIOType)) {
                String quorum = getPropertyValue(HBASE_ZOOKEEPER_QUORUM);
                String port = getPropertyValue(HBASE_ZOKEEPER_PORT);
                String master = getPropertyValue(HBASE_MASTER);
                return new HBasePyramidIO(quorum, port, master);
            } else if ("file-system".equals(pyramidIOType) || "file".equals(pyramidIOType)) {
                String rootPath = getPropertyValue(ROOT_PATH);
                String extension = getPropertyValue(EXTENSION);
                return new FileSystemPyramidIO(rootPath, extension);
            } else if ("jdbc".equals(pyramidIOType)) {
                String driver = getPropertyValue(JDBC_DRIVER);
                String rootPath = getPropertyValue(ROOT_PATH);
                return new JDBCPyramidIO(driver, rootPath);
            } else if ("resource".equals(pyramidIOType)) {
                String rootPath = getPropertyValue(ROOT_PATH);
                String extension = getPropertyValue(EXTENSION);
                PyramidStreamSource source = new ResourcePyramidStreamSource(rootPath, extension);
                return new ResourceStreamReadOnlyPyramidIO(source);
            } else if ("zip".equals(pyramidIOType)) {
            	// We need a cache of zip sources - they are slow to read.
            	PyramidStreamSource source = getZipSource(getPropertyValue(ROOT_PATH), getPropertyValue(EXTENSION));
                return new ResourceStreamReadOnlyPyramidIO(source);
            } else if ("sqlite".equals(pyramidIOType)) {
                String rootPath = getPropertyValue(ROOT_PATH);
                return new SQLitePyramidIO(rootPath);
            } else {
                return null;
            }
        } catch (Exception e) {
            LOGGER.error("Error trying to create PyramidIO", e);
            return null;
        }
    }
}
