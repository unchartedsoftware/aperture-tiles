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

import org.json.JSONException;
import org.json.JSONObject;

public class PropertyRelocationMutator extends JsonMutator {
    private String _name;
	private String[] _fromPath;
	private String[] _toPath;
	private boolean  _removeOld;

	public PropertyRelocationMutator (String name,
	                                  String[] fromPath,
	                                  String[] toPath,
	                                  boolean removeOld) {
	    _name = name;
		_fromPath = fromPath;
		_toPath = toPath;
		_removeOld = removeOld;
	}

	@Override
	public void mutateJson (JSONObject json) throws JSONException {
		for (LocationInformation fromTree: getTree(json, _fromPath, null, 0, false)) {
    		int size = fromTree.size();
    		String fromElt = fromTree.getFullMatch(size-1);
    		if (_fromPath.length == size && fromTree.get(size-1).has(fromElt)) {
    			JSONObject from = fromTree.get(size-1);
    			LocationInformation toTree = getTree(json, _toPath, fromTree._matches, 0, true).get(0);
    			String toElt = toTree.getFullMatch(_toPath.length-1);
    			if (_removeOld) {
                    toTree.get(toTree.size()-1).put(toElt, from.remove(fromElt));
                    cleanTree(fromTree, _fromPath);
    			} else {
                    toTree.get(toTree.size()-1).put(toElt, from.get(fromElt));
    			}
    		}
		}
	}

	@Override
	public String toString () {
	    return "Relocation("+_name+")";
	}
}
