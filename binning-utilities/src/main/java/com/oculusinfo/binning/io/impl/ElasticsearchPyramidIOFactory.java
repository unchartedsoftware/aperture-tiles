package com.oculusinfo.binning.io.impl;

import com.oculusinfo.binning.io.PyramidIO;
import com.oculusinfo.binning.util.JsonUtilities;
import com.oculusinfo.factory.ConfigurableFactory;
import com.oculusinfo.factory.ConfigurationException;
import com.oculusinfo.factory.SharedInstanceFactory;
import com.oculusinfo.factory.properties.IntegerProperty;
import com.oculusinfo.factory.properties.JSONArrayProperty;
import com.oculusinfo.factory.properties.StringProperty;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by cmenezes on 2015-06-04.
 */
public class ElasticsearchPyramidIOFactory extends SharedInstanceFactory<PyramidIO> {

	private static final Logger LOGGER = LoggerFactory.getLogger(ElasticsearchPyramidIOFactory.class);

	// connection properties
	public static StringProperty ES_CLUSTER_NAME = new StringProperty("es.cluster.name", "The elasticsearch cluster name for connection", null);
	public static StringProperty ES_TRANSPORT_ADDRESS = new StringProperty("es.transport.address", "Elasticsearch transport address", null);
	public static IntegerProperty ES_TRANSPORT_PORT = new IntegerProperty("es.transport.port", "Elasticsearch transport port", 9300);

	// data properties
	public static StringProperty ES_INDEX = new StringProperty("es.index","The es index",null);
	public static StringProperty ES_FIELD_X = new StringProperty("es.field.x","Elasticsearch field for x values",null);
	public static StringProperty ES_FIELD_Y = new StringProperty("es.field.y","Elasticsearch field for y values",null);
	public static JSONArrayProperty AOI_BOUNDS = new JSONArrayProperty("aoi.bounds", "elasticsearch AOI bounds", null);



	public ElasticsearchPyramidIOFactory(ConfigurableFactory<?> parent, List<String> path) {
		super("elasticsearch", PyramidIO.class, parent, path);

		addProperty(ES_CLUSTER_NAME);
		addProperty(ES_TRANSPORT_ADDRESS);
		addProperty(ES_TRANSPORT_PORT);

		addProperty(ES_INDEX);
		addProperty(ES_FIELD_X);
		addProperty(ES_FIELD_Y);
		addProperty(AOI_BOUNDS);
	}

	@Override
	protected PyramidIO createInstance() {
		try{
			LOGGER.info("ES pyramid factory.");

			String cluster_name = getPropertyValue(ES_CLUSTER_NAME);
			String transport_address = getPropertyValue(ES_TRANSPORT_ADDRESS);
			int transport_port = getPropertyValue(ES_TRANSPORT_PORT);

			String elastic_index = getPropertyValue(ES_INDEX);
			String es_field_x = getPropertyValue(ES_FIELD_X);
			String es_field_y = getPropertyValue(ES_FIELD_Y);

			JSONArray aoi_array = getPropertyValue(AOI_BOUNDS);
			List<Double> bounds = new ArrayList<>();
			try{
				if (aoi_array != null){
					List<Object> aoi_bounds = JsonUtilities.jsonArrayToList(aoi_array);
					for(int i = 0; i < aoi_bounds.size(); i++){
						bounds.add( (Double) aoi_bounds.get(i));
					}
				}
			} catch (Exception e){
				throw new ConfigurationException();
			}

			return new ElasticsearchPyramidIO(cluster_name, elastic_index, es_field_x, es_field_y, bounds,
				transport_address,transport_port );

		}catch (Exception e){
			LOGGER.error("Error creating ES pyramidio", e);
		}

		return null;
	}
}
