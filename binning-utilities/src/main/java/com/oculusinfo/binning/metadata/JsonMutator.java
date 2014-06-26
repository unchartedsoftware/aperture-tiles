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

import java.util.ArrayList;
import java.util.List;



/**
 * Simple class to encapsulate a json Mutation, for use by the versioning code.
 * JsonMutators mutate the passed-in JSON object, in place.
 */
public abstract class JsonMutator {
	abstract public void mutateJson (JSONObject json) throws JSONException;

	protected List<JSONObject> getTree (JSONObject root, String[] path,
	                                    int pathIndex, boolean createPath) throws JSONException {
		List<JSONObject> tree;
		if (pathIndex == path.length - 1) {
			tree = new ArrayList<>();
			tree.add(root);
		} else {
			if (createPath) {
				if (!root.has(path[pathIndex])) {
					root.put(path[pathIndex], new JSONObject());
				}
			}
			JSONObject branch = root.getJSONObject(path[pathIndex]);
			tree = getTree(branch, path, pathIndex+1, createPath);
			tree.add(0, root);
		}
		return tree;
	}

	protected void cleanTree (List<JSONObject> tree, String[] path) {
		int size = tree.size();
		if (size == path.length) {
			for (int i=size-1; i>0 && 0 == tree.get(i).length(); --i) {
				tree.get(i-1).remove(path[i-1]);
			}
		}
	}
}
