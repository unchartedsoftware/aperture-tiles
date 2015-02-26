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
package com.oculusinfo.binning.util;



import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;



/**
 * Simple class to aid in matching generic types
 * 
 * @author nkronenfeld
 */
public class TypeDescriptor implements Serializable {
	private static final long serialVersionUID = -8442365125923967447L;



	private Class<?>             _mainType;
	private List<TypeDescriptor> _genericTypes;

	public TypeDescriptor (Class<?> mainType) {
		_mainType = mainType;
		_genericTypes = null;
	}

	public TypeDescriptor (Class<?> mainType,
	                       TypeDescriptor... genericTypes) {
		_mainType = mainType;
		_genericTypes = Arrays.asList(genericTypes);
	}

	public Class<?> getMainType () {return _mainType;}
	public List<TypeDescriptor> getGenericTypes () {
		if (null == _genericTypes) return null;
		else return Collections.unmodifiableList(_genericTypes);
	}

	@Override
	public boolean equals (Object obj) {
		if (this == obj)
			return true;
		if (null == obj)
			return false;
		if (!(obj instanceof TypeDescriptor))
			return false;

		TypeDescriptor that = (TypeDescriptor) obj;
		if (!_mainType.equals(that._mainType))
			return false;

		int thisSize = (null == this._genericTypes ? 0 : this._genericTypes.size());
		int thatSize = (null == that._genericTypes ? 0 : that._genericTypes.size());
		if (thisSize != thatSize)
			return false;
		for (int i = 0; i < thisSize; ++i) {
			if (!this._genericTypes.get(i).equals(that._genericTypes.get(i)))
				return false;
		}
		return true;
	}

	@Override
	public String toString () {
		String result = _mainType.getSimpleName();
		if (null != _genericTypes && !_genericTypes.isEmpty()) {
			result += "<";
			for (int i=0; i<_genericTypes.size(); ++i) {
				if (i>0) result += ", ";
				result += _genericTypes.get(i).toString();
			}
			result += ">";
		}

		return result;
	}
}
