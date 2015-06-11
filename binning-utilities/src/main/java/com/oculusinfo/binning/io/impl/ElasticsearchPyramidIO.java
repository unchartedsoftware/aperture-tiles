package com.oculusinfo.binning.io.impl;

import com.oculusinfo.binning.BinIndex;
import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.impl.AOITilePyramid;
import com.oculusinfo.binning.impl.SparseTileData;
import com.oculusinfo.binning.io.PyramidIO;
import com.oculusinfo.binning.io.serialization.TileSerializer;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogram;
import org.elasticsearch.search.aggregations.bucket.histogram.Histogram;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Created by cmenezes on 2015-06-04.
 */
public class ElasticsearchPyramidIO implements PyramidIO {

	private static final Logger LOGGER = LoggerFactory.getLogger(ElasticsearchPyramidIO.class);

	private static final double[] bounds = new double[]{1417459397000.0, 0, 1426219029000.0, 1481798};
	private AOITilePyramid AOIP;

	private Node node;
	private Client client;

	private Client getClient(){
		return this.client;
	}

	public ElasticsearchPyramidIO(){

		LOGGER.debug("ES pyramid constructor");

		if ( this.node == null ) {
			LOGGER.debug("Existing node not found.");
			Node node = NodeBuilder.nodeBuilder()
				.data(false)
				.client(true)
				.clusterName("memex_es_dev")
				.node();
			Client client = node.client();
			this.node = node;
			this.client = client;
			this.AOIP = new AOITilePyramid(bounds[0],bounds[1],bounds[2],bounds[3]);

		}
		else {
			LOGGER.debug("A node (probably) already exists.");
		}
	}

	private SearchRequestBuilder baseQuery(FilterBuilder filter){

		return this.getClient().prepareSearch("sift_test_6")
			.setTypes("datum")
			.setSearchType(SearchType.COUNT)
			.setQuery(QueryBuilders.filteredQuery(
				QueryBuilders.matchAllQuery(),
				filter
			));
	}

	// note about the start/end/x/y confusion
	// x,y
	private SearchResponse timeFilteredRequest(double startX, double endX, double startY, double endY){

//		LOGGER.debug("--X--" + String.valueOf((long) Math.floor(endX - startX)));
//		LOGGER.debug("--Y--" + String.valueOf((long) Math.floor(startY- endY) ));

		SearchRequestBuilder searchRequestBuilder =
			baseQuery(
				FilterBuilders.andFilter(
					FilterBuilders.rangeFilter("locality_bag.dateBegin")
						.gt(startX)
						.lte(endX),
					FilterBuilders.rangeFilter("cluster_tellfinder")
						.gte(endY)
						.lte(startY)
				)
			)
			.addAggregation(
				AggregationBuilders.dateHistogram("date_agg")
					.field("locality_bag.dateBegin")
					.interval(getIntervalFromBounds(startX, endX))
					.minDocCount(1)
					.subAggregation(
						AggregationBuilders.histogram("cluster_range")
							.field("cluster_tellfinder")
							.interval(getIntervalFromBounds(endY, startY))
							.minDocCount(1)
					)
			);

		return searchRequestBuilder
				.execute()
				.actionGet();
	}

	private Long getIntervalFromBounds(double start, double end) {

		return (long) Math.floor((end - start )/256);
	};


	public void shutdown(){
		try {
			LOGGER.debug("Shutting down the ES client");
			this.node.stop();
			this.node.close();
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
		LOGGER.debug("Initialize for read");
	}

	private List<Long> aggregationParse(DateHistogram aggregation) {
		List<? extends DateHistogram.Bucket> buckets = aggregation.getBuckets();
		List<Long> result = new ArrayList<>();

		for(DateHistogram.Bucket bucket : buckets) {
			Histogram cluster_agg = bucket.getAggregations().get("cluster_range");
			List<? extends Histogram.Bucket> buckets1 = cluster_agg.getBuckets();

			for( Histogram.Bucket bucket1 : buckets1 ) {
				result.add(bucket1.getDocCount());
			}
		}
		return result;
	}

	private Map<Integer, Map> aggregationMapParse(DateHistogram date_agg, TileIndex tileIndex) {
		List<? extends DateHistogram.Bucket> dateBuckets = date_agg.getBuckets();

		Map<Integer, Map> result = new HashMap<>();

		// initialize the map
		// might not need to do this?
		for (int i = 0; i < 256; i++) {
			Map <Integer, Long> intermediate = new HashMap<>();
			for (int j = 0; j < 256; j++){
				intermediate.put(j, (long) 0);
			}
			result.put(i,intermediate);
		}

		for (DateHistogram.Bucket dateBucket : dateBuckets) {
			Histogram cluster_agg = dateBucket.getAggregations().get("cluster_range");
			List<? extends Histogram.Bucket> clusterBuckets = cluster_agg.getBuckets();

			for( Histogram.Bucket clusterBucket : clusterBuckets) {

				// transform from root coordinates into bin coordinates
				BinIndex binIndex = AOIP.rootToBin(dateBucket.getKeyAsNumber().doubleValue(), clusterBucket.getKeyAsNumber().doubleValue(), tileIndex);
				int xBin = binIndex.getX();
				int yBin = binIndex.getY();

				//given the bin coordinates, see if there's any data in those bins, add values to existing bins

				if (result.containsKey(xBin) && result.get(xBin).containsKey(yBin)) {
					result.get(xBin).put(yBin, (long)result.get(xBin).get(yBin) + clusterBucket.getDocCount());
				}

//				intermediate.put((long) clusterBucket.getKeyAsNumber(), clusterBucket.getDocCount());
//				result.put((long) dateBucket.getKeyAsNumber(), intermediate);

			}

		}
		return result;
	}



	@Override
	public <T> List<TileData<T>> readTiles(String pyramidId, TileSerializer<T> serializer, Iterable<TileIndex> tiles) throws IOException {

		LOGGER.debug("read Tiles");
		List<TileData<T>> results = new LinkedList<TileData<T>>();

		// iterate over the tile indices

		for (TileIndex tileIndex: tiles) {
			Rectangle2D rect = AOIP.getTileBounds(tileIndex);

			// get minimum/start time, max/end time
			double startX = rect.getX();
			double endX = rect.getMaxX();
			double startY = rect.getMaxY();
			double endY = rect.getY();

			SearchResponse sr = timeFilteredRequest(startX, endX, startY, endY);

			DateHistogram date_agg = sr.getAggregations().get("date_agg");

			Map<Integer, Map> tileMap = aggregationMapParse(date_agg, tileIndex);

			SparseTileData tileData = new SparseTileData(tileIndex,tileMap, 0);
			results.add(tileData);
		}

		return results;
	}

	private void word (){

//			List<Long> longs = aggregationParse(date_agg);

//			DenseTileData tileData = new DenseTileData(tile,longs);
		//			TileData<Map<Integer, Map<Integer, Double>>> tileData = new SparseTileData<Map<Integer, Map<Integer, Double>>>( tile , integerMapMap);

//			XContentBuilder builder = XContentFactory.jsonBuilder();
//			builder.startObject();
//			sr.toXContent(builder, ToXContent.EMPTY_PARAMS);
//			builder.endObject();
//			byte[] bytesArray = builder.bytesStream().bytes().toBytes();
//			builder.close();
//			ByteArrayInputStream bais = new ByteArrayInputStream(bytesArray);

//			TileData tileData = (TileData) bais;

//			LOGGER.debug("Num hits " + String.valueOf(searchResponse.getHits().totalHits()));

//			LOGGER.debug(searchResponse.getAggregations().asMap().toString());
	}


	@Override
	public <T> InputStream getTileStream(String pyramidId, TileSerializer<T> serializer, TileIndex tile) throws IOException {

		LOGGER.debug( "Call to getTileStream" );

		return null;
	}

	@Override
	public String readMetaData(String pyramidId) throws IOException {
		LOGGER.debug("Pretending to read metadata");
		return "{\"bounds\":[1417459397000,0,1430881122000,1481798],\"maxzoom\":1,\"scheme\":\"TMS\",\"description\":\"Elasticsearch test layer\",\"projection\":\"EPSG:4326\",\"name\":\"ES_SIFT_CROSSPLOT\",\"minzoom\":1,\"tilesize\":256,\"meta\":{\"levelMinimums\":{\"1\":\"0.0\", \"0\":\"0\"},\"levelMaximums\":{\"1\":\"469.0\", \"0\":\"700\"}}}";
	}

	@Override
	public void removeTiles(String id, Iterable<TileIndex> tiles) throws IOException {

	}
}
