package com.oculusinfo.binning.io.impl;

import com.oculusinfo.binning.BinIndex;
import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.TilePyramid;
import com.oculusinfo.binning.impl.SparseTileData;
import com.oculusinfo.binning.io.PyramidIO;
import com.oculusinfo.binning.io.serialization.TileSerializer;
import com.oculusinfo.binning.util.JsonUtilities;
import org.apache.commons.lang.StringEscapeUtils;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.index.query.BoolFilterBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeFilterBuilder;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.histogram.Histogram;
import org.elasticsearch.search.aggregations.metrics.max.Max;
import org.elasticsearch.search.aggregations.metrics.min.Min;
import org.json.JSONArray;
import org.json.JSONException;
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
	public static final int BINS = 256;

	private Client client;

	private String index;
	private String xField;
	private String yField;
	private TilePyramid tilePyramid;

	private final int numZoomlevels;
	private List<Double> maxValues;

	public ElasticsearchPyramidIO(
		String esClusterName,
		String esIndex,
		String xField,
		String yField,
		String esTransportAddress,
		int esTransportPort,
		TilePyramid tilePyramid,
		int zoomLevelPrecompute) {

		this.index = esIndex;
		this.xField = xField;
		this.yField = yField;
		this.tilePyramid = tilePyramid;
		this.numZoomlevels = zoomLevelPrecompute;

		if ( this.client == null ) {
			LOGGER.debug("Existing node not found.");
			try{
				Settings settings = ImmutableSettings.settingsBuilder()
					.put("cluster.name", esClusterName)
					.put("client.transport.sniff", false)
					.put("sniffOnConnection", true).build();
				this.client = new TransportClient(settings)
					.addTransportAddress(new InetSocketTransportAddress(esTransportAddress, esTransportPort));

			}catch (IllegalArgumentException e){
				LOGGER.debug("Illegal arguments to Elasticsearch node builder.");
			}
		}
		else {
			LOGGER.debug("An Elasticsearch client already exists.");
		}
	}

	private SearchRequestBuilder baseQuery(FilterBuilder filter){

		return this.client.prepareSearch(this.index)
			.setTypes("datum")
			.setSearchType(SearchType.COUNT)
			.setQuery(QueryBuilders.filteredQuery(
				QueryBuilders.matchAllQuery(),
				filter
			));
	}

	private SearchResponse timeFilteredRequest(double startX, double endX, double startY, double endY, JSONObject filterJSON){

		// the first filter added excludes everything outside of the tile boundary
		// on both the xField and the yField
		BoolFilterBuilder boundaryFilter = FilterBuilders.boolFilter();

		boundaryFilter.must(
			FilterBuilders.rangeFilter(this.xField)
				.gte(startX) //startx is min val
				.lte(endX),
			FilterBuilders.rangeFilter(this.yField)
				.gte(endY) //endy is min val
				.lte(startY)
		);

		Map<String, Object> filterMap = null;
		// transform filter list json to a map
		try {
			filterMap = JsonUtilities.jsonObjToMap(filterJSON);
		} catch (Exception e){
			filterMap = null;
		}

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
						List<String> termsList = new ArrayList<>();
						for (Object key : termsMap.keySet()){
							termsList.add((String)termsMap.get(key));
						}
						boundaryFilter.must(FilterBuilders.termsFilter(filterPath, termsList).execution("or"));
						break;
					case "range":
						// Note range filter requires a numeric value to filter on,
						// doesn't work if passing in formatted date strings like "2015-03-01"
						// check for existence of from OR to or FROM and TO
						RangeFilterBuilder rangeFilterBuilder = FilterBuilders.rangeFilter(filterPath);

						if (filter.containsKey("from") && filter.get("from") != null){
							rangeFilterBuilder.from(filter.get("from"));
						}
						if (filter.containsKey("to") && filter.get("to") != null){
							rangeFilterBuilder.to(filter.get("to"));
						}
						boundaryFilter.must(rangeFilterBuilder);
						break;
					case "UDF":
						// build a user defined facet
						BoolQueryBuilder boolQuery = new BoolQueryBuilder();
						boolQuery.must(QueryBuilders.queryStringQuery(StringEscapeUtils.escapeJavaScript((String) filter.get("query"))).field("body.en"));
						boundaryFilter.must(FilterBuilders.queryFilter(boolQuery));
						break;
					default:
						LOGGER.error("Unsupported filter type");
				}
			}
		}
		SearchRequestBuilder searchRequest =
			baseQuery(boundaryFilter)
			.addAggregation(
				AggregationBuilders.histogram("xField")
					.field(this.xField)
					.interval(getHistogramIntervalFromBounds(startX, endX))
					.minDocCount(1)
					.subAggregation(
						AggregationBuilders.histogram("yField")
							.field(this.yField)
							.interval(getHistogramIntervalFromBounds(endY, startY))
							.minDocCount(1)
					)
			);

		return searchRequest
				.execute()
				.actionGet();
	}

	private Long getHistogramIntervalFromBounds(double start, double end) {
		long interval = ((long) Math.floor((end - start) / BINS));
		// the calculated interval can be less than 1 if the data is sparse
		// we cannot pass elasticsearch a histogram interval less than 1
		// so set it to 1
		if (interval < 1) {
			interval = 1;
		}
		return interval;
	};

	public void shutdown(){
		try {
			LOGGER.debug("Shutting down the ES client");
			this.client.close();
		}catch(Exception e){
			LOGGER.error("Couldn't close the elasticsearch connection", e);
		}
	}

	@Override
	public void initializeForWrite(String pyramidId) throws IOException {

	}

	@Override
	public <T> void writeTiles(String pyramidId, TileSerializer<T> serializer, Iterable<TileData<T>> data) throws IOException {}

	@Override
	public void writeMetaData(String pyramidId, String metaData) throws IOException {

	}

	@Override
	public void initializeForRead(String pyramidId, int width, int height, Properties dataDescription) {

	}

	private Map<Integer, Map> parseAggregations(Histogram date_agg, TileIndex tileIndex) {
		List<? extends Histogram.Bucket> dateBuckets = date_agg.getBuckets();

		Map<Integer, Map> result = new HashMap<>();

		long maxval = 0;

		for (Histogram.Bucket dateBucket : dateBuckets) {
			Histogram cluster_agg = dateBucket.getAggregations().get("yField");
			List<? extends Histogram.Bucket> clusterBuckets = cluster_agg.getBuckets();

			BinIndex xBinIndex = tilePyramid.rootToBin(dateBucket.getKeyAsNumber().doubleValue(), 0, tileIndex);
			int xBin = xBinIndex.getX();
			Map<Integer,Long> intermediate = new HashMap<>();
			result.put(xBin, intermediate);

			for( Histogram.Bucket clusterBucket : clusterBuckets) {
				//given the bin coordinates, see if there's any data in those bins, add values to existing bins
				BinIndex binIndex = tilePyramid.rootToBin(dateBucket.getKeyAsNumber().doubleValue(), clusterBucket.getKeyAsNumber().doubleValue(), tileIndex);
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

	@Override
	public <T> List<TileData<T>> readTiles(String pyramidId, TileSerializer<T> serializer, Iterable<TileIndex> tiles, JSONObject properties) throws IOException{

		List<TileData<T>> results = new LinkedList<TileData<T>>();

		// iterate over the tile indices
		for (TileIndex tileIndex: tiles) {
			Rectangle2D rect = tilePyramid.getTileBounds(tileIndex);

			// get minimum/start time, max/end time
			double startX = rect.getX();
			double endX = rect.getMaxX();
			double startY = rect.getMaxY();
			double endY = rect.getY();

			SearchResponse sr = timeFilteredRequest(startX, endX, startY, endY, properties);
			if (responseHasData(sr)) {
				Histogram date_agg = sr.getAggregations().get("xField");
				Map<Integer, Map> tileMap = parseAggregations(date_agg, tileIndex);
				SparseTileData tileData = new SparseTileData(tileIndex,tileMap, 0);
				results.add(tileData);
			}
		}

		return results;
	}

	protected boolean responseHasData(SearchResponse sr) {
		SearchHits hits = sr.getHits();
		long totalHits = hits.getTotalHits();
		return (totalHits > 0);
	}

	@Override
	public <T> List<TileData<T>> readTiles(String pyramidId, TileSerializer<T> serializer, Iterable<TileIndex> tiles) throws IOException {
		return readTiles(pyramidId,serializer,tiles);
	}

	@Override
	public <T> InputStream getTileStream(String pyramidId, TileSerializer<T> serializer, TileIndex tile) throws IOException {
		return null;
	}

	/*
	* Get the boundaries for the X and Y fields configured for this PyramidIO
	* could be used to get the AOI tile pyramid bounds
	*
	* */
	private Map<String, Double> getFieldBoundaries(){

		SearchRequestBuilder boundsRequest = this.client.prepareSearch(this.index)
			.setTypes("datum")
			.setSearchType(SearchType.COUNT)
			.addAggregation(
				AggregationBuilders.min("minX").field(this.xField)
			).addAggregation(
				AggregationBuilders.min("minY").field(this.yField)
			).addAggregation(
				AggregationBuilders.max("maxX").field(this.xField)
			).addAggregation(
				AggregationBuilders.max("maxY").field(this.yField)
			);

		SearchResponse searchResponse = boundsRequest.execute().actionGet();
		Aggregations aggregations = searchResponse.getAggregations();

		Min minX = aggregations.get("minX");
		Min minY = aggregations.get("minY");
		Max maxX = aggregations.get("maxX");
		Max maxY = aggregations.get("maxY");

		Map<String, Double> boundsMap = new HashMap<>();
		boundsMap.put("minX", minX.getValue());
		boundsMap.put("minY", minY.getValue());

		boundsMap.put("maxX", maxX.getValue());
		boundsMap.put("maxY", maxY.getValue());

		return boundsMap;
	}

	private double searchForMaxBucketValue(double intervalX, double intervalY) {

		// build a query with a 2d aggregation on the xField and yField
		// change the interval
		SearchRequestBuilder metaDataQuery = this.client.prepareSearch(this.index)
			.setTypes("datum")
			.setSearchType(SearchType.COUNT)
			.addAggregation(
				AggregationBuilders.histogram("xAgg")
					.field(this.xField)
					.interval((long) intervalX)
					.order(Histogram.Order.COUNT_DESC)
					.subAggregation(
						AggregationBuilders.histogram("yAgg")
							.field(this.yField)
							.interval((long) intervalY)
							.order(Histogram.Order.COUNT_DESC)
					)
			);

		SearchResponse searchResponse = metaDataQuery.execute().actionGet();
		Histogram agg = searchResponse.getAggregations().get("xAgg");

		return getMaxValueFrom2DHistogram(agg);
	}

	// iterate through the aggregation, get the max doc count
	private double getMaxValueFrom2DHistogram(Histogram agg) {
		double maxValue = 1;
		for (Histogram.Bucket bucket : agg.getBuckets()) {
			Histogram yHistogram = bucket.getAggregations().get("yAgg");
			// there could be no buckets, in which case
			// there will be no effect on the max value
			if (yHistogram.getBuckets().size() < 1){
				continue;
			}
			// don't need to iterate over yAgg buckets because the query has ordered aggregations
			// take the first one if it's greater than maxValue
			if (maxValue < yHistogram.getBuckets().get(0).getDocCount() )
				maxValue = yHistogram.getBuckets().get(0).getDocCount();
		}
		return maxValue;
	}

	private List<Double> readMetaMaxFromElasticsearch() {

		Rectangle2D bounds = tilePyramid.getBounds();
		List<Double> maxCountList = new ArrayList<>();
		double pixelsPerTile = 256.0;

		for ( int i = 0; i < this.numZoomlevels ; i++ ){

			// calculated by dividing the entirety of the dataset bounds
			// by the number of pixels in a tile and the number of tiles at that Zoom level
			double tilesAtZoomLevel = Math.pow(2, i);
			double xInterval = bounds.getWidth() / (pixelsPerTile * tilesAtZoomLevel);
			double yInterval = bounds.getHeight() / (pixelsPerTile * tilesAtZoomLevel);
			if (xInterval < 1){
				xInterval = 1;
			}
			if (yInterval < 1){
				yInterval = 1;
			}
			// search ES based off the calculated interval
			double maxDocCount = searchForMaxBucketValue(xInterval, yInterval);
			maxCountList.add(i, maxDocCount);
		}
		while(maxCountList.size() < 15){
			maxCountList.add(maxCountList.get(maxCountList.size()-1)*0.75);
		}

		return maxCountList;
	}

	private JSONObject formatMaxValuesAsMetadataJSONString(List<Double> maxValues) throws JSONException {

		JSONObject metaDataJSON = new JSONObject();
		JSONObject levelMinMap = new JSONObject();
		JSONObject levelMaxMap = new JSONObject();
		for ( int i = 0; i < maxValues.size(); i++ ) {
			levelMinMap.put(""+i, String.valueOf(0));
			levelMaxMap.put(""+i, String.valueOf(maxValues.get(i)));
		}
		metaDataJSON.put("levelMinimums", levelMinMap);
		metaDataJSON.put("levelMaximums", levelMaxMap);
		return metaDataJSON;
	}

	@Override
	public String readMetaData(String pyramidId) throws IOException {

		Rectangle2D bounds = tilePyramid.getBounds();
		try {
			// read meta max values from Elasticsearch
			List<Double> maxValues = readMetaMaxFromElasticsearch();
			this.maxValues = maxValues;

			JSONObject metaDataJSON = new JSONObject();

			JSONArray boundsJSON = new JSONArray();
			boundsJSON.put(0, bounds.getMinX());
			boundsJSON.put(1, bounds.getMinY());
			boundsJSON.put(2, bounds.getMaxX());
			boundsJSON.put(3, bounds.getMaxY());

			metaDataJSON.put("bounds", boundsJSON);
			metaDataJSON.put("maxzoom", 1);
			metaDataJSON.put("description", "Elasticsearch rendered layer");
			metaDataJSON.put("scheme", "TMS");
			metaDataJSON.put("projection", "EPSG:4326");
			metaDataJSON.put("name", "ES_PLOT");
			metaDataJSON.put("minzoom", 1);
			metaDataJSON.put("tilesize", 256);
			metaDataJSON.put("meta", formatMaxValuesAsMetadataJSONString(maxValues));

			return metaDataJSON.toString();
		} catch (JSONException e) {
			LOGGER.error("Couldn't build JSON metadata. Defaulting to arbitrary max/mins.");
			return "{\"bounds\":["+
				bounds.getMinX() + "," +
				bounds.getMinY() + "," +
				bounds.getMaxX() + "," +
				bounds.getMaxY() + "],"+
				"\"maxzoom\":1,\"scheme\":\"TMS\","+
				"\"description\":\"Elasticsearch test layer\","+
				"\"projection\":\"EPSG:4326\","+
				"\"name\":\"ES_SIFT_CROSSPLOT\","+
				"\"minzoom\":1,\"tilesize\":256,"+
				"\"meta\":{\"levelMinimums\":{\"1\":\"0.0\", \"0\":\"0\"},\"levelMaximums\":{\"1\":\"174\", \"0\":\"2560\"}}}";
		}
	}

	@Override
	public void removeTiles(String id, Iterable<TileIndex> tiles) throws IOException {

	}
}
