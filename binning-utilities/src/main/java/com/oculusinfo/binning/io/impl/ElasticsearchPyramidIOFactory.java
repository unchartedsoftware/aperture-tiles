package com.oculusinfo.binning.io.impl;

import com.oculusinfo.binning.io.PyramidIO;
import com.oculusinfo.factory.ConfigurableFactory;
import com.oculusinfo.factory.SharedInstanceFactory;
import com.oculusinfo.factory.properties.StringProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by cmenezes on 2015-06-04.
 */
public class ElasticsearchPyramidIOFactory extends SharedInstanceFactory<PyramidIO> {

	private static final Logger LOGGER = LoggerFactory.getLogger(ElasticsearchPyramidIOFactory.class);

	public static StringProperty ES_INDEX = new StringProperty("es.index","The es index",null);
	public static StringProperty ES_CLUSTER_NAME = new StringProperty("es.cluster.name", "The elasticsearch cluster name for connection", null);

	public static StringProperty ES_FILTER_FIELD = new StringProperty("es.filter.field","Elasticsearch filter field for this layer",null);
	public static StringProperty ES_FILTER_TYPE = new StringProperty("es.filter.type","Elasticsearch filter type for this layer",null);

	public ElasticsearchPyramidIOFactory(ConfigurableFactory<?> parent, List<String> path) {
		super("elasticsearch", PyramidIO.class, parent, path);

		addProperty(ES_INDEX);
		addProperty(ES_CLUSTER_NAME);
		addProperty(ES_FILTER_FIELD);
		addProperty(ES_FILTER_TYPE);
	}

	@Override
	protected PyramidIO createInstance() {
		try{
			LOGGER.info("ES pyramid factory.");

			String es_cluster_name_prop = getPropertyValue(ES_CLUSTER_NAME);
			String es_index_prop = getPropertyValue(ES_INDEX);
			String es_filter_field_prop = getPropertyValue(ES_FILTER_FIELD);
			String es_filter_type_prop = getPropertyValue(ES_FILTER_TYPE);

			return new ElasticsearchPyramidIO(es_cluster_name_prop, es_index_prop, es_filter_field_prop, es_filter_type_prop);

		}catch (Exception e){
			LOGGER.error("Error creating ES pyramidio", e);
		}

		return null;
	}
}
