/*
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
package com.oculusinfo.binning.util;



import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.*;

import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.io.serialization.DefaultTileSerializerFactoryProvider;
import com.oculusinfo.binning.io.serialization.TileSerializer;
import com.oculusinfo.binning.properties.TileIndexProperty;
import com.oculusinfo.factory.ConfigurationException;
import com.oculusinfo.factory.ConfigurationProperty;
import com.oculusinfo.factory.properties.IntegerProperty;
import com.oculusinfo.factory.properties.ListProperty;
import com.oculusinfo.factory.properties.StringProperty;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.oculusinfo.binning.io.DefaultPyramidIOFactoryProvider;
import com.oculusinfo.binning.io.PyramidIO;
import com.oculusinfo.factory.ConfigurableFactory;
import com.oculusinfo.factory.UberFactory;
import com.oculusinfo.factory.providers.FactoryProvider;
import com.oculusinfo.factory.providers.StandardUberFactoryProvider;



/**
 * Copy a tile pyramid from one location to another.
 */
public class CopyPyramid<T> {
	public static void main (String[] args) {
		if (args.length > 0 && new File(args[0]).exists()) {
			File propFile = new File(args[0]);
			JSONObject props = null;
			try {
				props = readJSONFile(propFile);
			} catch (IOException | JSONException e1) {
				// Try to read as a properties file.
				try {
					Properties rawProps = new Properties();
					rawProps.load(new FileInputStream(propFile));
					props = JsonUtilities.propertiesObjToJSON(rawProps);
				} catch (FileNotFoundException e2) {
					System.err.println("Can't open properties file " + propFile);
					System.exit(1);
				} catch (IOException e2) {
					System.err.println("Error reading properties file " + propFile);
					System.exit(1);
				}
			}

			try {
				FactoryProvider<PyramidIO> pioFactoryProvider = getPyramidIOFactoryProvider();
				FactoryProvider<TileSerializer<?>> tsFactoryProvider = getTileSerializerFactoryProvider();

				ConfigurableFactory<? extends PyramidIO> sourceFactory = pioFactoryProvider.createFactory(Arrays.asList("source"));
				sourceFactory.readConfiguration(props);
				PyramidIO source = sourceFactory.produce(PyramidIO.class);

				ConfigurableFactory<? extends PyramidIO> destinationFactory = pioFactoryProvider.createFactory(Arrays.asList("destination"));
				destinationFactory.readConfiguration(props);
				PyramidIO destination = destinationFactory.produce(PyramidIO.class);

				ConfigurableFactory<? extends TileSerializer<?>> serializerFactory = tsFactoryProvider.createFactory(Arrays.asList("serializer"));
				serializerFactory.readConfiguration(props);
				TileSerializer<?> serializer = serializerFactory.produce(TileSerializer.class);

				ConfigurableFactory<CopyParameters> paramFactory = new CopyParametersFactory(null, Arrays.asList("copy"));
				paramFactory.readConfiguration(props);
				CopyParameters params = paramFactory.produce(CopyParameters.class);

				CopyPyramid<?> copier = new CopyPyramid(source, destination, serializer, params);
				System.out.println("Starting pyramid copy at "+new Date());
				copier.copy(params._indices);
				System.out.println("Pyramid copy finished at "+new Date());
			} catch (ConfigurationException e) {
				System.err.println("Error reading configuration:");
				e.printStackTrace();
				System.exit(1);
			} catch (IOException e) {
				System.err.println("Error copying tile pyramid:");
				e.printStackTrace();
				System.exit(1);
			}
		} else {
			System.out.println("Usage:");
			System.out.println("\tCopyPyramid <property file>");
			System.out.println("The property file should contain four root nodes:");
			System.out.println("\tsource: The source pyramid's characteristics");
			System.out.println("\tdestination: The destination pyramid's characteristics");
			System.out.println("\tserializer: The tile serializer's characteristics");
			System.out.println("\tlevels: A list of levels to copy.");
			System.out.println("\tindices: An optional list of tile indices to use to restrict the "+
			                   "area of copied tiles.   Tiles that don't intersect any of the indicated indices are "+
			                   "not exported.");
			System.out.println("tileWidth and tileHeight may also be specified, but default to 256.");
		}
	}


	private PyramidIO         _source;
	private PyramidIO         _destination;
	private TileSerializer<T> _serializer;
	private CopyParameters    _parameters;
	public CopyPyramid (PyramidIO source, PyramidIO destination, TileSerializer<T> serializer, CopyParameters parameters) {
		_source = source;
		_destination = destination;
		_serializer = serializer;
		_parameters = parameters;
	}

	public void copy (List<TileIndex> rootTiles) throws IOException {
		_source.initializeForRead(_parameters._sourceId, _parameters._width, _parameters._height, null);
		_destination.initializeForWrite(_parameters._destinationId);
		String metaData = _source.readMetaData(_parameters._sourceId);
		if (null != metaData) _destination.writeMetaData(_parameters._destinationId, metaData);
		List<TileIndex> toCopy = new ArrayList<>();
		for (TileIndex root: rootTiles) {
			toCopy = copyUp(root, toCopy);
			toCopy = copyDown(root, toCopy);
		}
		doCopy(toCopy);
	}

	private List<TileIndex> copyUp (TileIndex root, List<TileIndex> indexBuffer) throws IOException {
		if (_parameters._minLevel <= root.getLevel()) {
			indexBuffer.add(root);
			indexBuffer = checkBuffer(indexBuffer);
			indexBuffer = copyUp(TileIndex.getParent(root), indexBuffer);
		}
		return indexBuffer;
	}

	private List<TileIndex> copyDown (TileIndex root, List<TileIndex> indexBuffer) throws IOException {
		if (_parameters._maxLevel >= root.getLevel()) {
			indexBuffer.add(root);
			indexBuffer = checkBuffer(indexBuffer);
			for (TileIndex child: TileIndex.getChildren(root)) {
				indexBuffer = copyDown(child, indexBuffer);
			}
		}
		return indexBuffer;
	}

	private List<TileIndex> checkBuffer (List<TileIndex> indexBuffer) throws IOException {
		if (indexBuffer.size() >= _parameters._blockSize) {
			doCopy(indexBuffer);
			return new ArrayList<>();
		} else {
			return indexBuffer;
		}
	}

	private void doCopy(List<TileIndex> toCopy) throws IOException {
		List<TileData<T>> tiles = _source.readTiles(_parameters._sourceId, _serializer, toCopy);
		_destination.writeTiles(_parameters._destinationId, _serializer, tiles);
	}

	private static FactoryProvider<PyramidIO> getPyramidIOFactoryProvider () {
		Set<FactoryProvider<PyramidIO>> subFactories = new HashSet<>();
		for (FactoryProvider<PyramidIO> subFactory: DefaultPyramidIOFactoryProvider.values())
			subFactories.add(subFactory);
		final String defaultType = DefaultPyramidIOFactoryProvider.values()[0].name();
		return new StandardUberFactoryProvider<PyramidIO>(subFactories) {
			@Override
			public ConfigurableFactory<? extends PyramidIO> createFactory(String name, ConfigurableFactory<?> parent, List<String> path) {
				return new UberFactory<PyramidIO>(name, PyramidIO.class, parent, path, createChildren(parent, path), defaultType);
			}
		};
	}

	private static FactoryProvider<TileSerializer<?>> getTileSerializerFactoryProvider () {
		Set<FactoryProvider<TileSerializer<?>>> subFactories = new HashSet<>();
		for (FactoryProvider<TileSerializer<?>> subFactory: DefaultTileSerializerFactoryProvider.values())
			subFactories.add(subFactory);
		final String defaultType = DefaultPyramidIOFactoryProvider.values()[0].name();
		final Class<TileSerializer<?>> factoryType = (Class) TileSerializer.class;
		return new StandardUberFactoryProvider<TileSerializer<?>>(subFactories) {
			@Override
			public ConfigurableFactory<? extends TileSerializer<?>> createFactory(String name, ConfigurableFactory<?> parent, List<String> path) {
				return new UberFactory<TileSerializer<?>>(name, factoryType, parent, path, createChildren(parent, path), defaultType);
			}
		};
	}

	private static JSONObject readJSONFile (File file) throws IOException, JSONException {
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String line;
		StringBuffer whole = new StringBuffer();
		while ((line = reader.readLine()) != null) {
			whole.append(line);
		}
		reader.close();
		return new JSONObject(whole.toString());
	}

	static class CopyParameters {
		String _sourceId;
		String _destinationId;
		List<TileIndex> _indices;
		int _width;
		int _height;
		int _minLevel;
		int _maxLevel;
		int _blockSize;
		CopyParameters (String sourceId, String destinationId, List<TileIndex> indices,
		                int width, int height, int minLevel, int maxLevel, int blockSize) {
			_sourceId = sourceId;
			_destinationId = destinationId;
			if (null == indices || indices.isEmpty()) {
				_indices = Arrays.asList(new TileIndex(0, 0, 0, width, height));
			} else {
				_indices = indices;
			}
			_width = width;
			_height = height;
			_minLevel = minLevel;
			_maxLevel = maxLevel;
			_blockSize = blockSize;

		}
	}
	private static class CopyParametersFactory extends ConfigurableFactory<CopyParameters> {
		private static StringProperty SOURCE_ID = new StringProperty("sourceId", "The id of the source pyramid", "");
		private static StringProperty DESTINATION_ID = new StringProperty("destinationId", "The id of the destination pyramid", "");
		private static ConfigurationProperty<List<TileIndex>> INDICES =
			new ListProperty<TileIndex>(
			                            new TileIndexProperty(
			                                                  "index",
			                                                  "The index of a root tile from which to look up and down the pyramid",
			                                                  new TileIndex(0, 0, 0)
			                                                  ),
			                            "indices",
			                            "A list of root tiles that describe a necessary containment area of what is to be "+
			                            "exported.  Default is to export the whole pyramid."
			                            );
		private static IntegerProperty WIDTH = new IntegerProperty("width", "The tile width, in bins", 256);
		private static IntegerProperty HEIGHT = new IntegerProperty("height", "The tile height, in bins", 256);
		private static IntegerProperty MIN_LEVEL = new IntegerProperty("minimum", "The numerically minimum level to copy", 0);
		private static IntegerProperty MAX_LEVEL = new IntegerProperty("maximum", "The numerically maximum level to copy", 18);
		private static IntegerProperty BLOCK_SIZE = new IntegerProperty("blockSize", "The number of tiles to copy at a time", 100);

		protected CopyParametersFactory(ConfigurableFactory<?> parent, List<String> path) {
			this(null, parent, path);
		}

		protected CopyParametersFactory(String name, ConfigurableFactory<?> parent, List<String> path) {
			super(name, CopyParameters.class, parent, path);
			addProperty(SOURCE_ID);
			addProperty(DESTINATION_ID);
			addProperty(INDICES);
			addProperty(WIDTH);
			addProperty(HEIGHT);
			addProperty(MIN_LEVEL, Arrays.asList("level"));
			addProperty(MAX_LEVEL, Arrays.asList("level"));
			addProperty(BLOCK_SIZE);
		}

		@Override
		protected CopyParameters create() throws ConfigurationException {
			return new CopyParameters(
			                          getPropertyValue(SOURCE_ID),
			                          getPropertyValue(DESTINATION_ID),
			                          getPropertyValue(INDICES),
			                          getPropertyValue(WIDTH),
			                          getPropertyValue(HEIGHT),
			                          getPropertyValue(MIN_LEVEL),
			                          getPropertyValue(MAX_LEVEL),
			                          getPropertyValue(BLOCK_SIZE)
			                          );
		}
	}
}
