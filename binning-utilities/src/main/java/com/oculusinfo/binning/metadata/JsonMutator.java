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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONException;
import org.json.JSONObject;



/**
 * Simple class to encapsulate a json Mutation, for use by the versioning code.
 * JsonMutators mutate the passed-in JSON object, in place.
 */
public abstract class JsonMutator {
	abstract public void mutateJson (JSONObject json) throws JSONException;

	protected static class LocationInformation {
        List<JSONObject> _tree;
        List<List<String>> _matches;
        LocationInformation () {
            _tree = new ArrayList<>();
            _matches = new ArrayList<>();
        }
        public int size () {
            return _tree.size();
        }
        public JSONObject get (int index) {
            return _tree.get(index);
        }
        public String getFullMatch (int i) {
            return _matches.get(i).get(0);
        }
	}
	
    private static Pattern GROUP_PATTERN= Pattern.compile("\\\\([0-9]+)\\.([0-9]+)");

    protected static String substitute (String text, List<List<String>> matchGroups) {
        Matcher matcher = GROUP_PATTERN.matcher(text);
        if (matcher.find()) {
            int start = matcher.start(0);
            int end = matcher.end(0);
            int index1 = Integer.parseInt(matcher.group(1));
            int index2 = Integer.parseInt(matcher.group(2));
            text = text.substring(0, start)+matchGroups.get(index1).get(index2)+text.substring(end);
            text = substitute(text, matchGroups);
        }

        return text;
    }

    /**
     * Search for the given path in the given JSON object. If createPath is
     * false, then the path can contain regular expressions; if true, it can
     * not, but may contain group indicators (in the form "\\a.b", where a is
     * the index in the outer list of matchGroups, and b, the index in the
     * inner)
     * 
     * @param root The JSON object to search
     * @param path The path elements to search for a given node
     * @param matchGroups Match elements to substitute into the path if
     *            createPath is true
     * @param pathIndex The current index being searched in the path array
     * @param createPath if the path sought should be created if not there (in
     *            which case group substitutions will be allowed)
     */
	protected List<LocationInformation> getTree (JSONObject root, String[] path, List<List<String>> matchGroups,
	                                             int pathIndex, boolean createPath) throws JSONException {
	    List<LocationInformation> infos = new ArrayList<>();
	    boolean last = (pathIndex == path.length-1);

	    if (!last && createPath) {
            String pathElt = substitute(path[pathIndex], matchGroups);
            if (!root.has(pathElt)) {
                root.put(pathElt, new JSONObject());
            }
	    }

	    String[] branches = JSONObject.getNames(root);
        String pathElt = substitute(path[pathIndex], matchGroups);
	    if (null != branches) {
            Pattern pathPattern = Pattern.compile(pathElt);
            for (String branch: branches) {
                Matcher matcher = pathPattern.matcher(branch);
                if (matcher.matches()) {
                    List<String> groups = new ArrayList<>();
                    for (int i=0; i<=matcher.groupCount(); ++i) {
                        groups.add(matcher.group(i));
                    }

                    if (last) {
                        LocationInformation info = new LocationInformation();
                        info._tree.add(root);
                        info._matches.add(groups);
                        infos.add(info);
                    } else {
                        JSONObject branchObject = root.getJSONObject(pathElt);
                        List<LocationInformation> subInfos = getTree(branchObject, path, matchGroups, pathIndex+1, createPath);
                        for (LocationInformation info: subInfos) {
                            info._tree.add(0, root);
                            info._matches.add(0, groups);
                            infos.add(info);
                        }
                    }
                }
            }
	    }
	    if (last && infos.isEmpty() && createPath) {
            LocationInformation info = new LocationInformation();
            info._tree.add(root);
            List<String> matches = new ArrayList<>();
            matches.add(pathElt);
            info._matches.add(matches);
            infos.add(info);
	    }
	    return infos;
	}

	    /*
	    if (pathIndex == path.length - 1) {
		    infos = new ArrayList<>();
		    
            String[] branches = JSONObject.getNames(root);
            if (null != branches) {
                Pattern pathPattern = Pattern.compile(path[pathIndex]);
                for (String branch: branches) {
                    Matcher matcher = pathPattern.matcher(branch);
                    if (matcher.matches()) {
                        List<String> groups = new ArrayList<>();
                        for (int i=0; i<=matcher.groupCount(); ++i) {
                            groups.add(matcher.group(i));
                        }
                        LocationInformation info = new LocationInformation();
                        info._tree.add(root);
                        info._matches.add(groups);
                        infos.add(info);
                    }
                }

                if (infos.isEmpty() && createPath) {
                    LocationInformation info = new LocationInformation();
                    info._tree.add(root);
                    info._matches.add(new ArrayList<String>());
                    infos.add(info);
                }
            }
		} else {
			if (createPath) {
			    String pathElt = substitute(path[pathIndex], matchGroups);
				if (!root.has(pathElt)) {
					root.put(pathElt, new JSONObject());
				}
			}
			String[] branches = JSONObject.getNames(root);
			if (null != branches) {
			    Pattern pathPattern = Pattern.compile(path[pathIndex]);
			    for (String branch: branches) {
			        Matcher matcher = pathPattern.matcher(branch);
			        if (matcher.matches()) {
			            List<String> groups = new ArrayList<>();
			            for (int i=0; i<=matcher.groupCount(); ++i) {
			                groups.add(matcher.group(i));
			            }

			            JSONObject branchObject = root.getJSONObject(path[pathIndex]);
                        infos = getTree(branchObject, path, matchGroups, pathIndex+1, createPath);

                        for (LocationInformation info: infos) {
                            info._tree.add(0, root);
                            info._matches.add(0, groups);
                        }
			        }
			    }
			}
		}
		return infos;
	}
	*/

    protected void cleanTree (LocationInformation tree, String[] path) {
		int size = tree.size();
		if (size == path.length) {
			for (int i=size-1; i>0 && 0 == tree.get(i).length(); --i) {
				tree.get(i-1).remove(path[i-1]);
			}
		}
	}
}
