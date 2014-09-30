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
package com.oculusinfo.tilegen.graph.analytics;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.file.CodecFactory;

import com.oculusinfo.binning.io.serialization.GenericAvroArraySerializer;
import com.oculusinfo.binning.util.Pair;
import com.oculusinfo.binning.util.TypeDescriptor;

public class GraphAnalyticsAvroSerializer
extends GenericAvroArraySerializer<GraphAnalyticsRecord> {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8081628045640189463L;
    private static final TypeDescriptor TYPE_DESCRIPTOR = new TypeDescriptor(GraphAnalyticsRecord.class);

    public GraphAnalyticsAvroSerializer (CodecFactory compressionCodec) {
        super(compressionCodec, TYPE_DESCRIPTOR);
    }

    @Override
    protected String getEntrySchemaFile () {
        return "graphAnalyticsEntry.avsc";
    }
    
    private List<GraphCommunity> communityListTOJava (GenericRecord entry) {
		@SuppressWarnings("unchecked")
		GenericData.Array<GenericRecord> values = (GenericData.Array<GenericRecord>) entry.get("communities");
		List<GraphCommunity> results = new ArrayList<>();
		for (GenericRecord value: values) {
			results.add(new GraphCommunity((Integer)value.get("hierLevel"),
											(Long)value.get("id"),
	                                        new Pair<Double, Double>((Double)value.get("x"), (Double)value.get("y")),
	                                        (Double)value.get("r"),
	                                        (Integer)value.get("degree"),
	                                        (Long)value.get("numNodes"),
	                                        value.get("metadata").toString(),
	                                        (Boolean)value.get("isPrimaryNode"),
	                                        (Long)value.get("parentID"),
	                                        new Pair<Double, Double>((Double)value.get("parentX"), (Double)value.get("parentY")),
	                                        (Double)value.get("parentR")));
		}
		return results;
	}

    @SuppressWarnings("unchecked")
    @Override
    protected GraphAnalyticsRecord getEntryValue (GenericRecord entry) {
    	  return new GraphAnalyticsRecord((Integer)entry.get("numCommunities"),
    			  						communityListTOJava(entry));
    }
    
    private List<GenericRecord> communityListToAvro (Schema mainSchema, List<GraphCommunity> elts) {
        Schema eltSchema = mainSchema.getField("communities").schema().getElementType();
        List<GenericRecord> result = new ArrayList<>();
        for (int i=0; i < elts.size(); ++i) {
            GenericRecord elt = new GenericData.Record(eltSchema);
            GraphCommunity rawElt = elts.get(i);
            elt.put("hierLevel", rawElt.getHierLevel());
            elt.put("id", rawElt.getID());
            elt.put("x", rawElt.getCoords().getFirst());
            elt.put("y", rawElt.getCoords().getSecond());
            elt.put("r", rawElt.getRadius());
            elt.put("degree", rawElt.getDegree());
            elt.put("numNodes", rawElt.getNumNodes());
            elt.put("metadata", rawElt.getMetadata());
            elt.put("isPrimaryNode", rawElt.isPrimaryNode());
            elt.put("parentID", rawElt.getParentID());
            elt.put("parentX", rawElt.getParentCoords().getFirst());
            elt.put("parentY", rawElt.getParentCoords().getSecond());
            elt.put("parentR", rawElt.getParentRadius());            
            result.add(elt);
        }
        return result;
    }
  
    @Override
    protected void setEntryValue (GenericRecord avroEntry,
    								GraphAnalyticsRecord rawEntry) {
        try {
            Schema entrySchema = getEntrySchema();

            avroEntry.put("numCommunities", rawEntry.getNumCommunities());
            avroEntry.put("communities",
                          communityListToAvro(entrySchema,
                                           rawEntry.getCommunities()));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }    
}
