/**
 * Copyright (c) 2014 Oculus Info Inc. 
 * http://www.oculusinfo.com/
 * 
 * Released under the MIT License.
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:

 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.

 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.oculusinfo.binning.io.serialization.impl;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.avro.generic.GenericRecord;

import com.oculusinfo.binning.io.serialization.GenericAvroSerializer;

public class IntegerAvroSerializer extends GenericAvroSerializer<Integer> {
    private static final long serialVersionUID = 3241009200123502169L;



    public static final Map<String,String> META;
    static {
        Map<String,String> map = new HashMap<String, String>();
        map.put("source", "Oculus Binning Utilities");
        map.put("data-type", "int");
        META = Collections.unmodifiableMap(map);
    }



    public IntegerAvroSerializer () {
        super();
    }

    @Override
    protected String getRecordSchemaFile () {
        return "integerData.avsc";
    }

    @Override
    protected Map<String, String> getTileMetaData () {
        return META;
    }

    @Override
    protected Integer getValue (GenericRecord bin) {
        return (Integer) bin.get("value");
    }

    @Override
    protected void setValue (GenericRecord bin, Integer value) throws IOException {
        if (null == value) throw new IOException("Null value for bin");
        bin.put("value", value);
    }
}
