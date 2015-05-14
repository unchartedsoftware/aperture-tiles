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
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.file.CodecFactory;

import com.oculusinfo.binning.io.serialization.GenericAvroArraySerializer;
import com.oculusinfo.binning.util.TypeDescriptor;



public class TwitterTopicAvroSerializer
extends GenericAvroArraySerializer<TwitterDemoTopicRecord> {

	private static final long serialVersionUID = 3643598270516282371L;
    private static final TypeDescriptor TYPE_DESCRIPTOR = new TypeDescriptor(TwitterDemoTopicRecord.class);

    public TwitterTopicAvroSerializer (CodecFactory compressionCodec) {
        super(compressionCodec, TYPE_DESCRIPTOR);
    }

    @Override
    protected String getEntrySchemaFile () {
        return "twitterTopicEntry.avsc";
    }

    private List<RecentTweet> recentListTOJava(GenericRecord entry) {
        @SuppressWarnings("unchecked")
        GenericData.Array<GenericRecord> values = (GenericData.Array<GenericRecord>) entry.get("recentTweets");
        List<RecentTweet> results = new ArrayList<>();
        for (GenericRecord value: values) {
            results.add(new RecentTweet(value.get("tweet").toString(),
		            (Long) value.get("time"), value.get("user").toString(), value.get("sentiment").toString()));
        }
        return results;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected TwitterDemoTopicRecord getEntryValue (GenericRecord entry) {
        return new TwitterDemoTopicRecord(entry.get("topic").toString(),
        								entry.get("topicEnglish").toString(),
        								(Integer)entry.get("countMonthly"),
        								(List<Integer>)entry.get("countDaily"),
        								(List<Integer>)entry.get("countPer6hrs"),
        								(List<Integer>)entry.get("countPerHour"),
        								recentListTOJava(entry),
        								(Long)entry.get("endTimeSecs"));
    }

    private List<GenericRecord> recentListToAvro (Schema mainSchema, List<RecentTweet> elts) {
        Schema eltSchema = mainSchema.getField("recentTweets").schema().getElementType();
        List<GenericRecord> result = new ArrayList<>();
        for (int i=0; i < elts.size(); ++i) {
            GenericRecord elt = new GenericData.Record(eltSchema);
            RecentTweet rawElt = elts.get(i);
            elt.put("tweet", rawElt.getText());
            elt.put("time", rawElt.getTime());
	        elt.put("user", rawElt.getUser());
	        elt.put("sentiment", rawElt.getSentiment());
            result.add(elt);
        }
        return result;
    }

    @Override
    protected void setEntryValue (GenericRecord avroEntry,
                                  TwitterDemoTopicRecord rawEntry) {
        try {
            Schema entrySchema = getEntrySchema();

            avroEntry.put("topic", rawEntry.getTopic());
            avroEntry.put("topicEnglish", rawEntry.getTopicEnglish());
            avroEntry.put("countMonthly", rawEntry.getCountMonthly());
            avroEntry.put("countDaily", rawEntry.getCountDaily());
            avroEntry.put("countPer6hrs", rawEntry.getCountPer6hrs());
            avroEntry.put("countPerHour", rawEntry.getCountPerHour());
            avroEntry.put("recentTweets",
                          recentListToAvro(entrySchema,
                                           rawEntry.getRecentTweets()));
            avroEntry.put("endTimeSecs", rawEntry.getEndTime());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
