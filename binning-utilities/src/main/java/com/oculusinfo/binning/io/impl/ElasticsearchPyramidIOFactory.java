package com.oculusinfo.binning.io.impl;

import com.oculusinfo.binning.io.PyramidIO;
import com.oculusinfo.factory.ConfigurableFactory;
import com.oculusinfo.factory.properties.StringProperty;
import org.elasticsearch.client.Client;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by cmenezes on 2015-06-04.
 */
public class ElasticsearchPyramidIOFactory extends ConfigurableFactory<PyramidIO> {

	private static final Logger LOGGER = LoggerFactory.getLogger(ElasticsearchPyramidIOFactory.class);

	public static StringProperty ES_INDEX = new StringProperty("es.index",
		"",
	  null
	);

	public ElasticsearchPyramidIOFactory(ConfigurableFactory<?> parent, List<String> path) {
		super("elasticsearch", PyramidIO.class, parent, path);
		addProperty(ES_INDEX);
	}

	@Override
	protected PyramidIO create() {
		try{
			LOGGER.warn("seriously");
			Node node = NodeBuilder.nodeBuilder()
				.data(false)
				.client(true)
				.clusterName("memex_es_dev")
				.node();
			Client client = node.client();
			return new ElasticsearchPyramidIO(node, client);
		}catch (Exception e){
			LOGGER.error("Error creating ES pyramidio", e);
		}

		return null;
	}
}
