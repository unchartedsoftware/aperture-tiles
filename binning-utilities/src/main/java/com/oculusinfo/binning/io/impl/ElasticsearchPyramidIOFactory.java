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

	public static StringProperty ES_INDEX = new StringProperty("es.index","",null);

	public ElasticsearchPyramidIOFactory(ConfigurableFactory<?> parent, List<String> path) {
		super("elasticsearch", PyramidIO.class, parent, path);
		addProperty(ES_INDEX);
	}

	@Override
	protected PyramidIO createInstance() {
		try{
			LOGGER.info("ES pyramid factory.");
			return new ElasticsearchPyramidIO();
		}catch (Exception e){
			LOGGER.error("Error creating ES pyramidio", e);
		}

		return null;
	}
}
