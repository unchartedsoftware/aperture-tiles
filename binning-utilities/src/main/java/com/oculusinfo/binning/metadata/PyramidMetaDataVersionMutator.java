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

import com.oculusinfo.binning.metadata.updaters.MetaDataF0p0T1p0;
import com.oculusinfo.factory.util.Pair;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;



/**
 * This class handles mutation of the raw JSON from one metadata version to
 * another.
 * 
 * @author nkronenfeld
 */
public class PyramidMetaDataVersionMutator {
	public static final String CURRENT_VERSION = "1.0";
    protected static Collection<PyramidMetaDataVersionMutator> ALL_MUTATORS = new ArrayList<>();
	static {
	    MetaDataF0p0T1p0.register();
	}

	public static List<PyramidMetaDataVersionMutator> getMutators (String startVersion, String endVersion) {
		return getMutators(startVersion, endVersion, new ArrayList<>(ALL_MUTATORS));
	}

	public static void updateMetaData (JSONObject rawMetaData, String targetVersion) throws JSONException {
	    String sourceVersion = getMetaDataVersion(rawMetaData);
        List<PyramidMetaDataVersionMutator> mutators = getMutators(sourceVersion, targetVersion);
        if (null != mutators) {
            for (PyramidMetaDataVersionMutator mutator: mutators) {
                mutator.apply(rawMetaData);
            }
        }
	}

	private static String getMetaDataVersion (JSONObject rawMetaData) {
		String version = rawMetaData.optString("version", "0.0");

		if (null == version) {
			version = "0.0";
			try {
				JSONObject metaMeta = rawMetaData.getJSONObject("meta");
				if (metaMeta.has("global")) {
					version = "1.0";
				}
			} catch (Exception e) {
			}
		}
		return version;
	}

	private static List<PyramidMetaDataVersionMutator> getMutators (String startVersion, String endVersion,
	                                                                List<PyramidMetaDataVersionMutator> possibleMutators) {
		List<PyramidMetaDataVersionMutator> mutationPath;
		for (int i=0; i<possibleMutators.size(); ++i) {
			PyramidMetaDataVersionMutator mutator = possibleMutators.get(i);
			if (mutator._endVersion.equals(endVersion)) {
				if (mutator._startVersion.equals(startVersion)) {
					mutationPath = new ArrayList<>();
					mutationPath.add(mutator);
					return mutationPath;
				} else {
					possibleMutators.remove(i);
					try {
						mutationPath = getMutators(startVersion, mutator._startVersion, possibleMutators);
						if (null != mutationPath) {
							mutationPath.add(mutator);
							return mutationPath;
						}
					} finally {
						possibleMutators.add(i, mutator);
					}
				}
			}
		}
		return null;
	}

	private String        _startVersion;
	private String        _endVersion;
	private JsonMutator[] _mutations;



	public PyramidMetaDataVersionMutator (String startVersion, String endVersion, JsonMutator... mutations) {
		_startVersion = startVersion;
		_endVersion = endVersion;
		_mutations = mutations;
	}

	public Pair<String, String> getVersionBounds () {
		return new Pair<>(_startVersion, _endVersion);
	}
	public void apply (JSONObject rawMetaData) throws JSONException {
		for (JsonMutator mutation: _mutations) {
			mutation.mutateJson(rawMetaData);
		}
	}
}
