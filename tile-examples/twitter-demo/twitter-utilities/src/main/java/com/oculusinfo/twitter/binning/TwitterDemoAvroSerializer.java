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
package com.oculusinfo.twitter.binning;



import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.avro.Schema;
import org.apache.avro.file.CodecFactory;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;

import com.oculusinfo.binning.io.serialization.GenericAvroArraySerializer;
import com.oculusinfo.binning.util.Pair;
import com.oculusinfo.binning.util.TypeDescriptor;



public class TwitterDemoAvroSerializer
	extends GenericAvroArraySerializer<TwitterDemoRecord> {
	private static final long serialVersionUID = 6362108254078960320L;
	private static final TypeDescriptor TYPE_DESCRIPTOR = new TypeDescriptor(TwitterDemoRecord.class);



	public TwitterDemoAvroSerializer (CodecFactory compressionCodec) {
		super(compressionCodec, TYPE_DESCRIPTOR);
	}

	@Override
	protected String getEntrySchemaFile () {
		return "twitterDemoEntry.avsc";
	}

	private List<Pair<String, Long>> recentListTOJava (GenericRecord entry) {
		@SuppressWarnings("unchecked")
		GenericData.Array<GenericRecord> values = (GenericData.Array<GenericRecord>) entry.get("recent");
		List<Pair<String, Long>> results = new ArrayList<>();
		for (GenericRecord value: values) {
			results.add(new Pair<String, Long>(value.get("tweet").toString(),
			                                   (Long) value.get("time")));
		}
		return results;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected TwitterDemoRecord getEntryValue (GenericRecord entry) {
		return new TwitterDemoRecord(entry.get("tag").toString(),
		                             (Integer) entry.get("count"),
		                             (List<Integer>) entry.get("countByTime"),
		                             (Integer) entry.get("positive"),
		                             (List<Integer>) entry.get("positiveByTime"),
		                             (Integer) entry.get("neutral"),
		                             (List<Integer>) entry.get("neutralByTime"),
		                             (Integer) entry.get("negative"),
		                             (List<Integer>) entry.get("negativeByTime"),
		                             recentListTOJava(entry));
	}

	private List<GenericRecord> recentListToAvro (Schema mainSchema, List<Pair<String, Long>> elts) {
		Schema eltSchema = mainSchema.getField("recent").schema().getElementType();
		List<GenericRecord> result = new ArrayList<>();
		for (int i=0; i < elts.size(); ++i) {
			GenericRecord elt = new GenericData.Record(eltSchema);
			Pair<String, Long> rawElt = elts.get(i);
			elt.put("tweet", rawElt.getFirst());
			elt.put("time", rawElt.getSecond());
			result.add(elt);
		}
		return result;
	}

	@Override
	protected void setEntryValue (GenericRecord avroEntry,
	                              TwitterDemoRecord rawEntry) {
		try {
			Schema entrySchema = getEntrySchema();

			avroEntry.put("tag", rawEntry.getTag());
			avroEntry.put("count", rawEntry.getCount());
			avroEntry.put("countByTime", rawEntry.getCountBins());
			avroEntry.put("positive", rawEntry.getPositiveCount());
			avroEntry.put("positiveByTime", rawEntry.getPositiveCountBins());
			avroEntry.put("neutral", rawEntry.getNeutralCount());
			avroEntry.put("neutralByTime", rawEntry.getNeutralCountBins());
			avroEntry.put("negative", rawEntry.getNegativeCount());
			avroEntry.put("negativeByTime", rawEntry.getNegativeCountBins());
			avroEntry.put("recent",
			              recentListToAvro(entrySchema,
			                               rawEntry.getRecentTweets()));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
