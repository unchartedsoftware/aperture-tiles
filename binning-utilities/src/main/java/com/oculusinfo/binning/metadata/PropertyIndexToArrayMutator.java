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
package com.oculusinfo.binning.metadata;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

abstract public class PropertyIndexToArrayMutator<T> extends JsonMutator {
    private String[] _fromPath;
    private String[] _toPath;
    private String _value;
    public PropertyIndexToArrayMutator (String[] fromPath, String[] toPath, String value) {
        _fromPath = fromPath;
        _toPath = toPath;
        _value = value;
    }

    @Override
    public void mutateJson (JSONObject json) throws JSONException {
        JSONArray values = new JSONArray();

        // get the values to put in the array
        List<LocationInformation> sources = getTree(json, _fromPath, null, 0, false);
        List<T> sortedValues = new ArrayList<>();
        for (LocationInformation source: sources) {
            String value = JsonMutator.substitute(_value, source._matches);
            sortedValues.add(mutateValue(value));
        }
        sort(sortedValues);
        for (T value: sortedValues)
            values.put(value);

        LocationInformation toTree = getTree(json, _toPath, null, 0, true).get(0);
        toTree.get(toTree.size()-1).put(_toPath[_toPath.length-1], values);
    }


    abstract protected T mutateValue (String value);
    abstract protected void sort (List<T> values);
}
