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



import java.util.Properties;

import org.apache.spark.SparkContext;
import org.json.JSONObject;

import com.google.inject.Inject;
import com.oculusinfo.tile.spark.SparkContextProvider;
import com.oculusinfo.tile.util.JsonUtilities;
import com.oculusinfo.tilegen.datasets.CSVDataset;



public class DataServiceImpl implements DataService {
    private SparkContext _context;



    @Inject
    public DataServiceImpl (SparkContextProvider sparkSource) {
        _context = sparkSource.getSparkContext();
    }



    @Override
    public JSONObject getData (JSONObject datasetDescription,
                               JSONObject query, boolean getCount,
                               boolean getData, int requestCount) {
        JSONObject result = new JSONObject();

        Properties datasetProps = JsonUtilities.jsonObjToProperties(datasetDescription);
        // Width and height are irrelevant for record queries, so we just set them to 1.
        CSVDataset dataset = new CSVDataset(datasetProps, 1, 1);
        dataset.initialize(_context, true, false);

        return null;
    }
}
