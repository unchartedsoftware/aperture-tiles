package com.oculusinfo.binning.io.impl;

import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.io.PyramidIO;
import com.oculusinfo.binning.io.serialization.TileSerializer;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.node.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

/**
 * Created by cmenezes on 2015-06-04.
 */
public class ElasticsearchPyramidIO implements PyramidIO {

	private static final Logger LOGGER = LoggerFactory.getLogger(ElasticsearchPyramidIO.class);

	private Node node;
	private Client client;

	private Client getClient(){
		return this.client;
	}

	public ElasticsearchPyramidIO(Node node, Client client){
		this.node = node;
		this.client = client;

		SearchResponse searchResponse = this.getClient().prepareSearch("sift_test_4")
			.setTypes("datum")
			.setSearchType(SearchType.DEFAULT)
			.setQuery(QueryBuilders.matchAllQuery())
			.setPostFilter(FilterBuilders.andFilter(
				FilterBuilders.rangeFilter("locality_bag.dateBegin")
					.gte("2014-01-01")
					.lte("2015-01-02")
			))
			.execute()
			.actionGet();

//		searchResponse.getHits();
		LOGGER.warn(searchResponse.getHits().toString());
	}

	public void shutdown(){
		try {
			//kill the elasticsearch connection
		}catch(Exception e){
			LOGGER.error("Couldn't close the elasticsearch connection", e);
		}
	}

	@Override
	public void initializeForWrite(String pyramidId) throws IOException {

	}

	@Override
	public <T> void writeTiles(String pyramidId, TileSerializer<T> serializer, Iterable<TileData<T>> data) throws IOException {

	}

	@Override
	public void writeMetaData(String pyramidId, String metaData) throws IOException {

	}

	@Override
	public void initializeForRead(String pyramidId, int width, int height, Properties dataDescription) {


	}

	@Override
	public <T> List<TileData<T>> readTiles(String pyramidId, TileSerializer<T> serializer, Iterable<TileIndex> tiles) throws IOException {
		return null;
	}

	@Override
	public <T> InputStream getTileStream(String pyramidId, TileSerializer<T> serializer, TileIndex tile) throws IOException {
		return null;
	}

	@Override
	public String readMetaData(String pyramidId) throws IOException {
		return null;
	}

	@Override
	public void removeTiles(String id, Iterable<TileIndex> tiles) throws IOException {

	}
}
