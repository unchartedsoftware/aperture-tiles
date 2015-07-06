package com.oculusinfo.binning.io.impl;

import com.oculusinfo.binning.BinIndex;
import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.impl.AOITilePyramid;
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
import org.elasticsearch.search.aggregations.AggregationBuilders;
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

	private AOITilePyramid AOIP; //

	private Client client;

	private String index;
	private String xField;
	private String yField;

	private List<Double> bounds;

	public ElasticsearchPyramidIO(String esClusterName, String esIndex, String xField, String yField, List<Double> aoi_bounds) {

		this.index = esIndex;
		this.xField = xField;
		this.yField = yField;
		this.bounds = aoi_bounds;
		this.AOIP = new AOITilePyramid(bounds.get(0),bounds.get(1),bounds.get(2),bounds.get(3));

		if ( this.client == null ) {
			LOGGER.debug("Existing node not found.");
			try{
				Settings settings = ImmutableSettings.settingsBuilder()
					.put("cluster.name",esClusterName).build();
				this.client = new TransportClient(settings)
					.addTransportAddress(new InetSocketTransportAddress("memex1",9300))
					.addTransportAddress(new InetSocketTransportAddress("memex2", 9300))
					.addTransportAddress(new InetSocketTransportAddress("memex3", 9300));

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

						boolQuery.must(QueryBuilders.matchQuery("body.en", StringEscapeUtils.escapeJavaScript((String) filter.get("query"))));
						boundaryFilter.must(FilterBuilders.queryFilter(boolQuery));
						break;
					default:
						LOGGER.error("Unsupported filter type");
				}
			}
		}
		SearchRequestBuilder searchRequestBuilder =
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

		return searchRequestBuilder
				.execute()
				.actionGet();
	}

	private Long getHistogramIntervalFromBounds(double start, double end) {
		long interval = ((long) Math.floor((end - start) / TILE_PIXEL_DIMENSION));
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
//			this.node.stop();
			this.client.close();
		}catch(Exception e){
			LOGGER.error("Couldn't close the elasticsearch connection", e);
		}
	}

	@Override
	public void initializeForWrite(String pyramidId) throws IOException {}

	@Override
	public <T> void writeTiles(String pyramidId, TileSerializer<T> serializer, Iterable<TileData<T>> data) throws IOException {}

	@Override
	public void writeMetaData(String pyramidId, String metaData) throws IOException {}

	@Override
	public void initializeForRead(String pyramidId, int width, int height, Properties dataDescription) {}

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
		return null;
	}

	@Override
	public String readMetaData(String pyramidId) throws IOException {
		//TODO find a better way to get bounds + level metadata
		return "{\"bounds\":["+
			bounds.get(0) + "," +
			bounds.get(1) + "," +
			bounds.get(2) + "," +
			bounds.get(3) + "],"+
			"\"maxzoom\":1,\"scheme\":\"TMS\","+
			"\"description\":\"Elasticsearch test layer\","+
			"\"projection\":\"EPSG:4326\","+
			"\"name\":\"ES_SIFT_CROSSPLOT\","+
			"\"minzoom\":1,\"tilesize\":256,"+
			"\"meta\":{\"levelMinimums\":{\"1\":\"0.0\", \"0\":\"0\"},\"levelMaximums\":{\"1\":\"174\", \"0\":\"2560\"}}}";
	}

	@Override
	public void removeTiles(String id, Iterable<TileIndex> tiles) throws IOException {

	}
}
