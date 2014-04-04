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
package com.oculusinfo.factory;



import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;



/**
 * A wrapper around a JSON object's parent and key that allows one to get node
 * values in a type-independent way.
 * 
 * The problem is that with a normal JSONObject, you call
 * parent.getInt(childName), or parent.getString(childName), or
 * parent.getJSONObject(childName), etc. And with a JSONArray, you do
 * array.getInt(childIndex). So the same interface doesn't work on each. This
 * wrapper lets you get essentially a node, but in a way that doesn't depend on
 * the type of the node.
 * 
 * @author nkronenfeld
 */
public class JSONNode {
	// We either get one or the other of these initializations; our goal is to
	// allow a user to use either interchangably.

	// Representation 1: child node of an object.
	private JSONObject _parentNode;
	private String     _childNodeName;

	// Representation 2: element of an array.
	private JSONArray  _parentArray;
	private int        _index;


	public JSONNode (JSONObject parent, String child) {
		_parentNode = parent;
		_childNodeName = child;
		_parentArray = null;
		_index = -1;
	}

	public JSONNode (JSONArray parent, int index) {
		_parentNode = null;
		_childNodeName = null;
		_parentArray = parent;
		_index = index;
	}

	public boolean getAsBoolean () throws JSONException {
		if (null != _parentNode) {
			return _parentNode.getBoolean(_childNodeName);
		} else {
			return _parentArray.getBoolean(_index);
		}
	}

	public void setAsBoolean (boolean value) throws JSONException {
		if (null != _parentNode) {
			_parentNode.put(_childNodeName, value);
		} else {
			_parentArray.put(_index, value);
		}
	}

	public int getAsInt () throws JSONException {
		if (null != _parentNode) {
			return _parentNode.getInt(_childNodeName);
		} else {
			return _parentArray.getInt(_index);
		}
	}

	public void setAsInt (int value) throws JSONException {
		if (null != _parentNode) {
			_parentNode.put(_childNodeName, value);
		} else {
			_parentArray.put(_index, value);
		}
	}

	public long getAsLong () throws JSONException {
		if (null != _parentNode) {
			return _parentNode.getLong(_childNodeName);
		} else {
			return _parentArray.getLong(_index);
		}
	}

	public void setAsLong (long value) throws JSONException {
		if (null != _parentNode) {
			_parentNode.put(_childNodeName, value);
		} else {
			_parentArray.put(_index, value);
		}
	}

	public double getAsDouble () throws JSONException {
		if (null != _parentNode) {
			return _parentNode.getDouble(_childNodeName);
		} else {
			return _parentArray.getDouble(_index);
		}
	}

	public void setAsDouble (double value) throws JSONException {
		if (null != _parentNode) {
			_parentNode.put(_childNodeName, value);
		} else {
			_parentArray.put(_index, value);
		}
	}

	public String getAsString () throws JSONException {
		if (null != _parentNode) {
			return _parentNode.getString(_childNodeName);
		} else {
			return _parentArray.getString(_index);
		}
	}

	public void setAsString (String value) throws JSONException {
		if (null != _parentNode) {
			_parentNode.put(_childNodeName, value);
		} else {
			_parentArray.put(_index, value);
		}
	}

	public JSONObject getAsJSONObject () throws JSONException {
		if (null != _parentNode) {
			return _parentNode.getJSONObject(_childNodeName);
		} else {
			return _parentArray.getJSONObject(_index);
		}
	}

	public void setAsJSONObject (JSONObject value) throws JSONException {
		if (null != _parentNode) {
			_parentNode.put(_childNodeName, value);
		} else {
			_parentArray.put(_index, value);
		}
	}

	public JSONArray getAsJSONArray () throws JSONException {
		if (null != _parentNode) {
			return _parentNode.getJSONArray(_childNodeName);
		} else {
			return _parentArray.getJSONArray(_index);
		}
	}

	public void setAsJSONArray (JSONArray value) throws JSONException {
		if (null != _parentNode) {
			_parentNode.put(_childNodeName, value);
		} else {
			_parentArray.put(_index, value);
		}
	}

	public JSONNode getNamedChildNode (String childNodeName) throws JSONException {
		return new JSONNode(getAsJSONObject(), childNodeName);
	}

	public JSONNode getIndexedChildNode (int index) throws JSONException {
		return new JSONNode(getAsJSONArray(), index);
	}
}
