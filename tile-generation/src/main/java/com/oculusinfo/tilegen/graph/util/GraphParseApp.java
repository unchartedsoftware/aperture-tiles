/*
 * Copyright (c) 2013 Oculus Info Inc.
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

package com.oculusinfo.tilegen.graph.util;

import java.util.HashMap;

/**
* This is an example application for the GraphmlParser utility for converting a graphml file
* to tab-delimited format.
* 
* Command line arguments are as follows:
* 
* -in -- Path and filename of graphML input file [required].
* 
* -out -- Path and filename of tab-delimited output file [required].
* 
* -longIDs -- [boolean, optional] If == true, then nodes will be assigned a unique Long number ID,
* 			regardless of the node ID format in the original graphML file.  Note, this ID convention is
* 			needed for data processing with Spark's GraphX library.  Default == false.    
* 
* -nAttr -- Node attributes to parse (attribute ID tags separated by commas) [optional].
* 			Default is to parse ALL node attributes.
* 
* -eAttr -- Edge attributes to parse (attribute ID tags separated by commas) [optional].
* 			Default is to parse ALL edge attributes.  
* 
* -nCoordAttr -- Node attributes to use for node co-ordinates (separated by commas) [optional].
* 				Default is NO co-ordinate data will be associated with a given node.
*   
* -nCoordConvert --  Node co-ordinate conversion [optional; may be used with 'nCoordAttr' property above]
*					Choices are: zorder2xy (z-order to x-y), zorder2xyz (z-order to x,y,z -- note: z-axis data will be discarded!)
*	    			Default is no conversion
*
**/

public class GraphParseApp {

	public static void main(String[] args) {

		HashMap<String, String> argMap = new HashMap<String, String>();
		
        int i = 0;
        String propValue = "";
        String propName = "";

        //------ parse command line arguments and store in a Map
        while (i < args.length) {
        	
        	String newarg = args[i++];
        	if (newarg.startsWith("-")) {
        		//start of a new property, so save previous one
        		if ((propName != "") && (propName != null)) {
        			argMap.put(propName, propValue.trim());
        		}
        		propName = newarg.substring(1);	// to remove the "-" at start
        		propValue = "";
        	}
        	else {
        		propValue += " " + newarg;
        	}
        }
        if ((propName != "") && (propName != null)) {	// save last argument
			argMap.put(propName, propValue.trim());
		}
        //------------------
	
        GraphmlParser graphParser = new GraphmlParser(argMap);
        
        graphParser.parseGraphML();
        
        System.out.println("Done!");
        
	}
}
