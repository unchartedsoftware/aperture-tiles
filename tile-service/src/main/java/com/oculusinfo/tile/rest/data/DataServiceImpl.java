/*
 * Copyright (c) 2014 Oculus Info Inc. http://www.oculusinfo.com/
 * 
 * Released under the MIT License.
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oculusinfo.tile.rest.data;



import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.spark.SparkContext;
import org.apache.spark.api.java.JavaRDD;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scala.Function1;
import scala.collection.immutable.List;
import scala.util.Failure;
import scala.util.Try;

import com.google.inject.Inject;
import com.oculusinfo.math.statistics.StatTracker;
import com.oculusinfo.tile.spark.SparkContextProvider;
import com.oculusinfo.tile.util.JsonUtilities;
import com.oculusinfo.tilegen.datasets.CSVDataset;
import com.oculusinfo.tilegen.datasets.FilterFunctions;
import com.oculusinfo.tilegen.tiling.ValueOrException;



public class DataServiceImpl implements DataService {
	private static final Logger LOGGER = LoggerFactory.getLogger(DataServiceImpl.class);



	// Build in some simple profiling of data calls
	private static enum TimingStats {
		TotalTime,
		TotalRequests,
		DataRequests,
		DataTime,
		DataTimePerRecord,
		DataBytes,
		DataBytesPerRecord,
		RequestedRecords,
		ReceivedRecords,
		CountRequests,
		CountTime
	}

	private static Map<TimingStats, StatTracker> _timingStats = new HashMap<TimingStats, StatTracker>() {
		private static final long serialVersionUID = 1L;

		{
			for (TimingStats stat: TimingStats.values()) {
				put(stat, new StatTracker());
			}
		}
	};
	private static void addTimingStat (TimingStats stat, double value) {
		_timingStats.get(stat).addStat(value);
	}
	private static void debugTimingStats (PrintStream out) {
		for (TimingStats stat: TimingStats.values()) {
			StatTracker tracker = _timingStats.get(stat);
			String statDesc = String.format("sample size: %d, total value: %d, avg. value: %.2f, std. dev: %.2f",
			                                tracker.sampleSize(), (long) tracker.total(),
			                                tracker.mean(), tracker.populationStandardDeviation());
			out.println(stat+": "+statDesc);
		}
	}



	private SparkContext _context;

	@Inject
	public DataServiceImpl (SparkContextProvider sparkSource) {
		_context = sparkSource.getSparkContext();
	}



	@Override
	public JSONObject getData (JSONObject datasetDescription,
	                           JSONObject query, boolean getCount,
	                           boolean getData, int requestCount) {
		long startTime = System.currentTimeMillis();

		JSONObject result = new JSONObject();

		// Create our dataset
		Properties datasetProps = JsonUtilities.jsonObjToProperties(datasetDescription);
		// Width and height are irrelevant for record queries, so we just set them to 1.
		CSVDataset dataset = new CSVDataset(datasetProps, 1, 1);
		dataset.initialize(_context, true, false);

		// Create our query filter
		Try<Function1<ValueOrException<List<Object>>, Object>> filterAttempt =
			FilterFunctions.parseQuery(query, dataset);
		if (filterAttempt.isFailure()) {
			LOGGER.warn("Bad query {}", query, ((Failure<?>)filterAttempt).exception());
			return null;
		}
		Function1<ValueOrException<List<Object>>, Object> filter = filterAttempt.get();

		// Query Spark for our data
		JavaRDD<String> filteredData = dataset.getRawFilteredJavaData(filter);
		if (getData) {
			try {
				long dataStartTime = System.currentTimeMillis();
				java.util.List<String> rawData = filteredData.take(requestCount);
				JSONArray dataResults = new JSONArray();
				int totalSize = 0;
				for (int i=0; i<rawData.size(); ++i) {
					String record = rawData.get(i);
					dataResults.put(i, record);

					// And track some stats on record sizes
					addTimingStat(TimingStats.DataBytesPerRecord, record.length());
					if (null != record) totalSize += record.length();
				}
				result.put("data", dataResults);
				long dataEndTime = System.currentTimeMillis();
				addTimingStat(TimingStats.DataRequests, 1);
				addTimingStat(TimingStats.DataTime, dataEndTime-dataStartTime);
				addTimingStat(TimingStats.DataTimePerRecord, (dataEndTime-dataStartTime)/(double)rawData.size());
				addTimingStat(TimingStats.RequestedRecords, requestCount);
				addTimingStat(TimingStats.ReceivedRecords, rawData.size());
				addTimingStat(TimingStats.DataBytes, totalSize);
			} catch (JSONException e) {
				LOGGER.warn("Error fulfilling data request {}", query, e);
			}
		}
		if (getCount) {
			try {
				long countStartTime = System.currentTimeMillis();
				long count = filteredData.count();
				result.put("resultCount", count);
				long countEndTime = System.currentTimeMillis();

				addTimingStat(TimingStats.CountRequests, 1);
				addTimingStat(TimingStats.CountTime, countEndTime-countStartTime);
			} catch (JSONException e) {
				LOGGER.warn("Error fulfilling count request {}", query, e);
			}
		}

		long endTime = System.currentTimeMillis();
		addTimingStat(TimingStats.TotalRequests, 1);
		addTimingStat(TimingStats.TotalTime, endTime-startTime);

		if (0 == ((int) _timingStats.get(TimingStats.TotalRequests).total()) % 10) {
			debugTimingStats(System.out);
		}
        
		return result;
	}
}
