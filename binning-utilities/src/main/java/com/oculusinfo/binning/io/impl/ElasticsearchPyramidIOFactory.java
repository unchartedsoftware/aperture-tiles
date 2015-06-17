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

	public static StringProperty ES_FILTER_FIELD = new StringProperty("es.filter.field","",null);
	public static StringProperty ES_FILTER_TYPE = new StringProperty("es.filter.type","",null);

	public static StringProperty ES_FILTER_MAX = new StringProperty("es.filter.max","",null);
	public static StringProperty ES_FILTER_MIN = new StringProperty("es.filter.min","",null);

//	public static JSONProperty ES_FILTER_PROP = new JSONProperty("es.filter","",null);

	public ElasticsearchPyramidIOFactory(ConfigurableFactory<?> parent, List<String> path) {
		super("elasticsearch", PyramidIO.class, parent, path);
		addProperty(ES_INDEX);
		addProperty(ES_FILTER_FIELD);
		addProperty(ES_FILTER_TYPE);
		addProperty(ES_FILTER_MAX);
		addProperty(ES_FILTER_MIN);
//		addProperty(ES_FILTER_PROP);
	}

	@Override
	protected PyramidIO createInstance() {
		try{
			LOGGER.info("ES pyramid factory.");
//			JSONObject propertyValue = getPropertyValue(ES_FILTER_PROP);


			return new ElasticsearchPyramidIO();
		}catch (Exception e){
			LOGGER.error("Error creating ES pyramidio", e);
		}

		return null;
	}
}
