package com.oculusinfo.binning.io.impl;

import com.oculusinfo.binning.TilePyramid;
import com.oculusinfo.binning.io.PyramidIO;
import com.oculusinfo.factory.ConfigurableFactory;
import com.oculusinfo.factory.ConfigurationException;
import com.oculusinfo.factory.SharedInstanceFactory;
import com.oculusinfo.factory.properties.IntegerProperty;
import com.oculusinfo.factory.properties.StringProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by cmenezes on 2015-06-04.
 */
public class ElasticsearchPyramidIOFactory extends SharedInstanceFactory<PyramidIO> {

	private static final Logger LOGGER = LoggerFactory.getLogger(ElasticsearchPyramidIOFactory.class);

	// connection properties
	public static StringProperty ES_CLUSTER_NAME = new StringProperty(
		"es.cluster.name",
		"The elasticsearch cluster name for connection",
		null);
	public static StringProperty ES_TRANSPORT_ADDRESS = new StringProperty(
		"es.transport.address",
		"Elasticsearch transport address",
		null);
	public static IntegerProperty ES_TRANSPORT_PORT = new IntegerProperty(
		"es.transport.port",
		"Elasticsearch transport port",
		9300);
	public static IntegerProperty NUM_ZOOM_LEVELS = new IntegerProperty(
		"es.num.zoom.levels",
		"Number of levels to precompute when configuring the layer",
		3);

	// data properties
	public static StringProperty ES_INDEX = new StringProperty(
		"es.index",
		"The es index",
		null);
	public static StringProperty ES_FIELD_X = new StringProperty(
		"es.field.x",
		"Elasticsearch field for x values",
		null);
	public static StringProperty ES_FIELD_Y = new StringProperty(
		"es.field.y",
		"Elasticsearch field for y values",
		null);

	public ElasticsearchPyramidIOFactory(ConfigurableFactory<?> parent, List<String> path) {
		super("elasticsearch", PyramidIO.class, parent, path);

		addProperty(ES_CLUSTER_NAME);
		addProperty(ES_TRANSPORT_ADDRESS);
		addProperty(ES_TRANSPORT_PORT);

		addProperty(ES_INDEX);
		addProperty(ES_FIELD_X);
		addProperty(ES_FIELD_Y);
		addProperty(NUM_ZOOM_LEVELS);
	}

	@Override
	protected PyramidIO createInstance() throws ConfigurationException {
		LOGGER.info("ES pyramid factory.");

		String clusterName = getPropertyValue(ES_CLUSTER_NAME);
		String transportAddress = getPropertyValue(ES_TRANSPORT_ADDRESS);
		int transportPort = getPropertyValue(ES_TRANSPORT_PORT);

		Integer numZoomLevels = getPropertyValue(NUM_ZOOM_LEVELS);

		String elasticIndex = getPropertyValue(ES_INDEX);
		String esFieldX = getPropertyValue(ES_FIELD_X);
		String esFieldY = getPropertyValue(ES_FIELD_Y);
		TilePyramid tilePyramid = getRoot().produce(TilePyramid.class);

		return new ElasticsearchPyramidIO(clusterName, elasticIndex, esFieldX, esFieldY, transportAddress, transportPort, tilePyramid, numZoomLevels);
	}
}
