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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.io.serialization.DefaultTileSerializerFactoryProvider;
import com.oculusinfo.binning.io.serialization.TileSerializer;
import com.oculusinfo.factory.ConfigurationException;
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

				JSONArray rawLevels = props.getJSONArray("levels");
				List<Integer> levels = new ArrayList<>();
				for (int i=0; i<rawLevels.length(); ++i) {
					levels.add(rawLevels.getInt(i));
				}

				String sourceId = props.getString("sourceId");
				String destinationId = props.getString("destinationId");
				int width = props.optInt("tileWidth", 256);
				int height = props.optInt("tileHeight", 256);

				CopyPyramid<?> copier = new CopyPyramid(source, destination, serializer);
				copier.copy(sourceId, destinationId, levels, width, height, 100);
			} catch (ConfigurationException e) {
				System.err.println("Error reading configuration:");
				e.printStackTrace();
				System.exit(1);
			} catch (JSONException e) {
				System.err.println("Error reading levels from configuration:");
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
			System.out.println("tileWidth and tileHeight may also be specified, but default to 256.");
		}
	}


	private PyramidIO _source;
	private PyramidIO _destination;
	private TileSerializer<T> _serializer;
	public CopyPyramid (PyramidIO source, PyramidIO destination, TileSerializer<T> serializer) {
		_source = source;
		_destination = destination;
		_serializer = serializer;
	}

	public void copy(String sourceId, String destinationId, List<Integer> levels, int width, int height, int maxBlock) throws IOException {
		_source.initializeForRead(sourceId, width, height, null);
		_destination.initializeForWrite(destinationId);
		_destination.writeMetaData(destinationId, _source.readMetaData(sourceId));
		List<TileIndex> toCopy = new ArrayList<>();
		for (int level: levels) {
			long pow2 = 1 << level;
			for (int x=0; x<pow2; ++x) {
				for (int y=0; y<pow2; ++y) {
					toCopy.add(new TileIndex(level, x, y, width, height));
					if (toCopy.size() >= maxBlock) {
						doCopy(sourceId, destinationId, toCopy);
						toCopy = new ArrayList<>();
					};
				}
			}
		}
		doCopy(sourceId, destinationId, toCopy);
	}

	private void doCopy(String sourceId, String destinationId, List<TileIndex> toCopy) throws IOException {
		List<TileData<T>> tiles = _source.readTiles(sourceId, _serializer, toCopy);
		_destination.writeTiles(destinationId, _serializer, tiles);
	}
}
