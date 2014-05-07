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



import java.util.List;
import java.util.Properties;

import org.apache.spark.SparkContext;

import com.oculusinfo.factory.ConfigurableFactory;
import com.oculusinfo.factory.SharedInstanceFactory;
import com.oculusinfo.tile.util.JsonUtilities;
import com.oculusinfo.tilegen.datasets.CSVDataset;



/**
 *  Very simple factory to create datasets. It's a bit weird as factories go, in
 *  that it doesn't interpret its data itself, but leaves that to the dataset it
 *  is creating.
 *  
 *  @author nkronenfeld
 */
public class DatasetFactory extends SharedInstanceFactory<CSVDataset> {
	private SparkContext _context;
	protected DatasetFactory (SparkContext context, ConfigurableFactory<?> parent, List<String> path) {
		this(context, null, parent, path);
	}

	protected DatasetFactory (SparkContext context, String name, 
	                          ConfigurableFactory<?> parent, List<String> path) {
		super(name, CSVDataset.class, parent, path);
		_context = context;
	}

	@Override
	protected CSVDataset createInstance () {
		Properties datasetProps = JsonUtilities.jsonObjToProperties(getConfigurationNode());
		// Width and height are irrelevant for record queries, so we just set them to 1.
		CSVDataset dataset = new CSVDataset(datasetProps, 1, 1);
		dataset.initialize(_context, false, true, false);


		return dataset;
	}
}
