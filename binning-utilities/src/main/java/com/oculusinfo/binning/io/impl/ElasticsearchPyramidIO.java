package com.oculusinfo.binning.io.impl;

import com.oculusinfo.binning.BinIndex;
import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.impl.AOITilePyramid;
import com.oculusinfo.binning.impl.SparseTileData;
import com.oculusinfo.binning.io.PyramidIO;
import com.oculusinfo.binning.io.serialization.TileSerializer;
import com.oculusinfo.binning.util.JsonUtilities;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.AndFilterBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogram;
import org.elasticsearch.search.aggregations.bucket.histogram.Histogram;
import org.json.JSONObject;
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
import java.util.Set;

/**
 * Created by cmenezes on 2015-06-04.
 */
public class ElasticsearchPyramidIO implements PyramidIO {

	private static final Logger LOGGER = LoggerFactory.getLogger(ElasticsearchPyramidIO.class);

	public static final int TILE_PIXEL_DIMENSION = 256; //used to calculate histogram aggregation interval to fit into 256x256 pixels

	private AOITilePyramid AOIP;

	private Node node;
	private Client client;

	private String index;
	private String xField;
	private String yField;

	private List<Double> bounds;

	public ElasticsearchPyramidIO(String esClusterName, String esIndex, String xField, String yField, List<Double> aoi_bounds) {

		LOGGER.debug("ES custom config constructor ?");

		this.index = esIndex;
		this.xField = xField;
		this.yField = yField;
		this.bounds = aoi_bounds;

		if ( this.node == null ) {
			LOGGER.debug("Existing node not found.");
			try{
				Node node = NodeBuilder.nodeBuilder()
					.loadConfigSettings(false)
					.data(false)
					.client(true)
					.clusterName(esClusterName)
					.node();
				this.node = node;
			}catch (IllegalArgumentException e){
				LOGGER.debug("Illegal arguments to Elasticsearch node builder.");
			}

			Client client = node.client();
			this.client = client;
			this.AOIP = new AOITilePyramid(bounds.get(0),bounds.get(1),bounds.get(2),bounds.get(3));

		}
		else {
			LOGGER.debug("A node (probably) already exists.");
		}
	}

	private Client getClient(){
		return this.client;
	}

	private SearchRequestBuilder baseQuery(FilterBuilder filter){

		return this.getClient().prepareSearch(this.index)
			.setTypes("datum")
			.setSearchType(SearchType.COUNT)
			.setQuery(QueryBuilders.filteredQuery(
				QueryBuilders.matchAllQuery(),
				filter
			));
	}

	// note about the start/end/x/y confusion
	// x,y
	private SearchResponse timeFilteredRequest(double startX, double endX, double startY, double endY, JSONObject filterJSON){

		AndFilterBuilder boundaryFilter = FilterBuilders.andFilter(
			FilterBuilders.rangeFilter(this.xField)
				.gt(startX) //startx is min val
				.lte(endX),
			FilterBuilders.rangeFilter(this.yField)
				.gte(endY) //endy is min val
				.lte(startY)
		);

		Map<String, Object> filterMap = null;
		// transform filter list json to a map
		try{
			filterMap = JsonUtilities.jsonObjToMap(filterJSON);
		}catch (Exception e){
			filterMap = null;
		}

		// now need to transform the map to a more useful format
		// e.g. make a list from the ordinal keys

		if (filterMap != null) {
			Set<String> strings = filterMap.keySet();
			List<Map> filterList = new ArrayList<>();
			for (String str : strings) {
				filterList.add((Map) filterMap.get(str));
			}

			for (Map filter : filterList) {
				String type = (String) filter.get("type");
				String filterPath = (String)filter.get("path");

				switch (type){
					case "terms":
						Map termsMap = (Map)filter.get("terms");
						List<String> termsList = new ArrayList();
						for (Object key : termsMap.keySet()){
							termsList.add((String)termsMap.get(key));
						}
						boundaryFilter.add(FilterBuilders.termsFilter(filterPath, termsList).execution("or"));
						break;
					case "range":
//						range filter temporarily disabled because this needs a double/long rather than the
//						date text string that we're giving it
//						either get the client filter service to pass along a numeric date or do some transformation
//						here
						boundaryFilter.add(FilterBuilders.rangeFilter(filterPath).from(filter.get("from")).to(filter.get("to")));
						break;
					default:
						LOGGER.error("Unsupported filter type");
				}
			}
		}
		SearchRequestBuilder searchRequestBuilder =
			baseQuery(
				boundaryFilter
			)
			.addAggregation(
				AggregationBuilders.histogram("xField")
					.field(this.xField)
					.interval(getIntervalFromBounds(startX, endX))
					.minDocCount(1)
					.subAggregation(
						AggregationBuilders.histogram("yField")
							.field(this.yField)
							.interval(getIntervalFromBounds(endY, startY))
							.minDocCount(1)
					)
			);

//		LOGGER.debug(searchRequestBuilder.toString());

		return searchRequestBuilder
				.execute()
				.actionGet();
	}

	private Long getIntervalFromBounds(double start, double end) {

		return (long) Math.floor((end - start )/ TILE_PIXEL_DIMENSION);
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
			Histogram cluster_agg = bucket.getAggregations().get("yField");
			List<? extends Histogram.Bucket> buckets1 = cluster_agg.getBuckets();

			for( Histogram.Bucket bucket1 : buckets1 ) {
				result.add(bucket1.getDocCount());
			}
		}
		return result;
	}

	private Map<Integer, Map> aggregationMapParse(Histogram date_agg, TileIndex tileIndex) {
		List<? extends Histogram.Bucket> dateBuckets = date_agg.getBuckets();

		Map<Integer, Map> result = new HashMap<>();

		long maxval = 0;

		for (Histogram.Bucket dateBucket : dateBuckets) {
			Histogram cluster_agg = dateBucket.getAggregations().get("yField");
			List<? extends Histogram.Bucket> clusterBuckets = cluster_agg.getBuckets();

			BinIndex xBinIndex = AOIP.rootToBin(dateBucket.getKeyAsNumber().doubleValue(), 0, tileIndex);
			int xBin = xBinIndex.getX();
			Map<Integer,Long> intermediate = new HashMap<>();
			result.put(xBin, intermediate);

			for( Histogram.Bucket clusterBucket : clusterBuckets) {
				//given the bin coordinates, see if there's any data in those bins, add values to existing bins
				BinIndex binIndex = AOIP.rootToBin(dateBucket.getKeyAsNumber().doubleValue(), clusterBucket.getKeyAsNumber().doubleValue(), tileIndex);
				int yBin = binIndex.getY();

				if (result.containsKey(xBin) && result.get(xBin).containsKey(yBin)) {
					intermediate.put(yBin, (long) intermediate.get(yBin) + clusterBucket.getDocCount());
				}
				else if (result.containsKey(xBin) && !(intermediate.containsKey(yBin))) {
					intermediate.put(yBin, clusterBucket.getDocCount());
				}
				if (maxval < clusterBucket.getDocCount()){
					maxval = clusterBucket.getDocCount();
				}
			}
		}

		return result;
	}

	public <T> List<TileData<T>> readTiles(String pyramidId, TileSerializer<T> serializer, Iterable<TileIndex> tiles, JSONObject properties) throws IOException {
		List<TileData<T>> results = new LinkedList<TileData<T>>();

		// iterate over the tile indices

		for (TileIndex tileIndex: tiles) {
			Rectangle2D rect = AOIP.getTileBounds(tileIndex);

			// get minimum/start time, max/end time
			double startX = rect.getX();
			double endX = rect.getMaxX();
			double startY = rect.getMaxY();
			double endY = rect.getY();


			SearchResponse sr = timeFilteredRequest(startX, endX, startY, endY, properties);

			Histogram date_agg = sr.getAggregations().get("xField");

			Map<Integer, Map> tileMap = aggregationMapParse(date_agg, tileIndex);

			SparseTileData tileData = new SparseTileData(tileIndex,tileMap, 0);
			results.add(tileData);
		}

		return results;
	}

	@Override
	public <T> List<TileData<T>> readTiles(String pyramidId, TileSerializer<T> serializer, Iterable<TileIndex> tiles) throws IOException {
		return readTiles(pyramidId,serializer,tiles,null);
	}

	@Override
	public <T> InputStream getTileStream(String pyramidId, TileSerializer<T> serializer, TileIndex tile) throws IOException {

		LOGGER.debug( "Call to getTileStream" );

		return null;
	}

	@Override
	public String readMetaData(String pyramidId) throws IOException {
		LOGGER.debug("Pretending to read metadata");
		// faking the bounds and zoom level right now, but this should be retrievable at least from the config file,
		// and parts of it from an Elasticsearch boundary query.
		return "{\"bounds\":[1417459397000,0,1430881122000,1481798],\"maxzoom\":1,\"scheme\":\"TMS\",\"description\":\"Elasticsearch test layer\",\"projection\":\"EPSG:4326\",\"name\":\"ES_SIFT_CROSSPLOT\",\"minzoom\":1,\"tilesize\":256,\"meta\":{\"levelMinimums\":{\"1\":\"0.0\", \"0\":\"0\"},\"levelMaximums\":{\"1\":\"174\", \"0\":\"2560\"}}}";
	}

	@Override
	public void removeTiles(String id, Iterable<TileIndex> tiles) throws IOException {

	}
}
