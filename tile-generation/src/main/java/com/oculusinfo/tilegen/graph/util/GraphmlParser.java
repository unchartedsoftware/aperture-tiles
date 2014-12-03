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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
* This class handles reading in a graphml file, parses nodes and edges as desired,
* and re-saves the data in tab-delimited format.
*
* The class constructor takes in an argument map of key/values that is used to configure
* how the data is parsed:
* 
* in -- Path and filename of graphML input file [required].
* 
* out -- Path and filename of tab-delimited output file [required].
* 
* -longIDs -- [boolean, optional] If == true, then nodes will be assigned a unique Long number ID,
* 			regardless of the node ID format in the original graphML file.  Note, this ID convention is
* 			needed for data processing with Spark's GraphX library.  Default == false.  
* 
* nAttr -- Node attributes to parse (attribute ID tags separated by commas) [optional].
* 			Default is to parse all node attributes.
* 
* eAttr -- Edge attributes to parse (attribute ID tags separated by commas) [optional].
* 			Default is to parse all edge attributes.  
* 
* nCoordAttr -- Node attributes to use for node co-ordinates (separated by commas) [optional].
* 				Default is NO co-ordinate data will be associated with a given node.
*   
* nCoordConvert --  Node co-ordinate conversion [optional, may be used with 'nCoordAttr' property above]
*					Choices are: zorder2xy (z-order to x-y), zorder2xyz (z-order to x,y,z -- note: z-axis data will be discarded!)
*	    			Default is no conversion
*
**/

public class GraphmlParser {
	
    private List<String> _nodeAttributes = null;
    private List<String> _edgeAttributes = null;
    private List<String> _nodeCoordAttr = null;
    private String _graphmlInput = null;
    private String _filenameOut = null;
    private String _filenameOutReadme = null;
    private String _nodeCoordConvert = null;
    
    private HashMap<String, double[]> _nodemap = new HashMap<String, double[]>();
    private long _numNodes = 0;
    private long _numEdges = 0;
    
    private boolean _bLongIDs = false;
    
    private BufferedWriter _outBuffWriter;
    private BufferedWriter _outBuffWriterReadme;
    
    private ArrayList<String> nodeAttrList = new ArrayList<String>();
    private ArrayList<String> edgeAttrList = new ArrayList<String>();    
	
    //-----------
	public GraphmlParser(HashMap<String, String> argMap) {
			    
	    _graphmlInput = argMap.get("in");	 //in, Path and filename of graphML input file
	    _filenameOut = argMap.get("out");	 //out, Path and filename of tab-delimited output file
	    _filenameOutReadme = _filenameOut + "_readme";
	    
	    String stringTemp = argMap.get("nAttr"); //nAttr, "Node attributes to parse (attribute ID tags separated by commas)"
	    if (stringTemp!=null) {					 // Default = parse all existing attributes.
	    	_nodeAttributes = Arrays.asList(stringTemp.split(","));
	    	for (int n=0; n<_nodeAttributes.size(); n++) {
	    		_nodeAttributes.set(n, _nodeAttributes.get(n).trim());
	    	}
	    }
	    
	    stringTemp = argMap.get("eAttr"); 		 //eAttr, "Edge attributes to parse (attribute ID tags separated by commas)."	    										
	    if (stringTemp!=null) {					 // Default = parse all existing attributes.	
	    	_edgeAttributes = Arrays.asList(stringTemp.split(","));
	    	for (int n=0; n<_edgeAttributes.size(); n++) {
	    		_edgeAttributes.set(n, _edgeAttributes.get(n).trim());
	    	}
	    }
	    
	    stringTemp = argMap.get("nCoordAttr");	 //nCoordAttr, "Node attributes to use for node coords (separated by commas)"	    										 	
	    if (stringTemp!=null) { 	
	    	_nodeCoordAttr = Arrays.asList(stringTemp.split(","));
	    	for (int n=0; n<_nodeCoordAttr.size(); n++) {
	    		_nodeCoordAttr.set(n, _nodeCoordAttr.get(n).trim());
	    	}
	    }
	    
	    _nodeCoordConvert = argMap.get("nCoordConvert");	//"nCoordConvert", co-ordinate conversion
																// Choices are: zorder2xy, zorder2xyz (Note: z-axis data will be discarded!)
	    														// Default = no conversion
	    
	    _bLongIDs = argMap.get("longIDs").equals("true");	//"longIDs", assign unique Long IDs for each node
	}
	
	//-----------
	public void parseGraphML() {
	    
		try {
		
			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser saxParser = factory.newSAXParser();
			
			FileWriter fstream;
		    fstream = new FileWriter(_filenameOut);
		    _outBuffWriter = new BufferedWriter(fstream);
							    
		    DefaultHandler handler = new DefaultHandler() {
		    			    	
			    ArrayList<String> nodeAttrValues = new ArrayList<String>();
			    ArrayList<String> edgeAttrValues = new ArrayList<String>();
						    	
				boolean bInNode = false;
				boolean bInEdge = false;
				boolean bInData = false;
				String nodeID;
				String edgeSource;
				String edgeTarget;
				String dataKey;
	
				//--------------
				public void startElement(String uri, String localName, String qName, Attributes attributes)
						throws SAXException {
					
					if (qName.equalsIgnoreCase("key")) {
						
						if (attributes.getValue("for").equalsIgnoreCase("node")) {	// store all attribute ID's for nodes
							
							if (_nodeAttributes==null) {
								nodeAttrList.add(attributes.getValue("id"));
								nodeAttrValues.add("");
							} else {
								if (_nodeAttributes.contains(attributes.getValue("id"))) {
									nodeAttrList.add(attributes.getValue("id"));
									nodeAttrValues.add("");
								}
							}							
						}
						else if  (attributes.getValue("for").equalsIgnoreCase("edge")) {	// store all attribute ID's for edges
							if (_edgeAttributes==null) {
								edgeAttrList.add(attributes.getValue("id"));
								edgeAttrValues.add("");
							} else {
								for (int n=0; n<_edgeAttributes.size(); n++) {
									if (_edgeAttributes.contains(attributes.getValue("id"))) {
										edgeAttrList.add(attributes.getValue("id"));
										edgeAttrValues.add("");
									}
								}
							}						
						}
					}
					else if (qName.equalsIgnoreCase("node")) {
						bInNode = true;
						nodeID = attributes.getValue("id");
					}
					else if (qName.equalsIgnoreCase("edge")) {
						bInEdge = true;
						edgeSource = attributes.getValue("source");
						edgeTarget = attributes.getValue("target");					
					}
					else if (qName.equalsIgnoreCase("data")) {
						bInData = true;
						dataKey = attributes.getValue("key");
					}	
				}
				
				//--------------
				public void endElement(String uri, String localName, String qName) throws SAXException {
					
					if (qName.equalsIgnoreCase("node")) {
						bInNode = false;
						
						double[] nodeData = new double[3];
						if (_nodeCoordAttr!=null) {
							
							for (int n=0; n<_nodeCoordAttr.size(); n++) {
								String coordTemp = nodeAttrValues.get(nodeAttrList.indexOf(_nodeCoordAttr.get(n)));
								nodeData[n] = Double.parseDouble(coordTemp);
							}
							
							if (_nodeCoordConvert!=null) {
								if (_nodeCoordConvert.equals("zorder2xy")) {
									Integer[] coordsTmp = mortonXY((long)nodeData[0]);	//assume z-order value has been parsed into coords[0]
									nodeData[0] = (double)coordsTmp[0];
									nodeData[1] = (double)coordsTmp[1];
								}
								else if (_nodeCoordConvert.equals("zorder2xyz")) {
									Integer[] coordsTmp = mortonXYZ((long)nodeData[0]);	//assume z-order value has been parsed into coords[0]
									nodeData[0] = (double)coordsTmp[0];
									nodeData[1] = (double)coordsTmp[1];
									//discard z-axis coordinates for now ...
									//coords[2] = (double)coordsTmp[2];
								}
							}
							
							if (_bLongIDs) {
								nodeData[2] = (double)_numNodes;	// also save current _numNodes values as a unique Long ID for this node
							}

							_nodemap.put(nodeID, nodeData);
						}
						else if (_bLongIDs) {
							nodeData[2] = (double)_numNodes;	// save current _numNodes values as a unique Long ID for this node
							_nodemap.put(nodeID, nodeData);							
						}
						
						_numNodes++;
													
						//write an output line here (tab-delimited)
						try {
							if (_nodeCoordAttr!=null) {
								if (_bLongIDs) {
									// write out the unique Long ID for this node as well as the 'original' nodeID
									_outBuffWriter.write("node" + "\t" + ((long)nodeData[2]) + "\t" + nodeID + "\t" + nodeData[0] + "\t" + nodeData[1]);
								}
								else {
									_outBuffWriter.write("node" + "\t" + nodeID + "\t" + nodeData[0] + "\t" + nodeData[1]);
								}
									
								for (int i=0; i<nodeAttrValues.size(); i++) {
									if (!_nodeCoordAttr.contains(nodeAttrList.get(i))) {
										_outBuffWriter.write("\t" + nodeAttrValues.get(i));	// write this attribute if not already written out as coords above
									}
									nodeAttrValues.set(i, "");
								}
								_outBuffWriter.write("\n");
							}
							else {
								if (_bLongIDs) {
									// write out the unique Long ID for this node as well as the 'original' nodeID
									_outBuffWriter.write("node" + "\t" + ((long)nodeData[2]) + "\t" + nodeID);
								}
								else {
									_outBuffWriter.write("node" + "\t" + nodeID);
								}
								
								for (int i=0; i<nodeAttrValues.size(); i++) {
									_outBuffWriter.write("\t" + nodeAttrValues.get(i));
									nodeAttrValues.set(i, "");
								}
								_outBuffWriter.write("\n");																
							}
							
						} catch (IOException e) {
							e.printStackTrace();
						}	
						
						if ((_numNodes % 100000L == 0) && (_numNodes != 0)) {	// print message every 100,000 iterations
							System.out.println("Number of nodes = " + _numNodes);
						}	
					}
					else if (qName.equalsIgnoreCase("edge")) {
						bInEdge = false;
						_numEdges++;
																			
						//write an output line here (tab-delimited)
						try {
							if (_nodeCoordAttr!=null) {
								double[] coordSrc = _nodemap.get(edgeSource);
								double[] coordTar = _nodemap.get(edgeTarget);
								if (_bLongIDs) {
									// write out unique Long IDs for edge source and dest for edges instead of 'original' node IDs
									_outBuffWriter.write("edge" + "\t" + (long)coordSrc[2] + "\t" + coordSrc[0] + "\t" + coordSrc[1] + "\t" + 
											(long)coordTar[2] + "\t" + coordTar[0] + "\t" + coordTar[1]);
								}
								else {
									_outBuffWriter.write("edge" + "\t" + edgeSource + "\t" + coordSrc[0] + "\t" + coordSrc[1] + "\t" + 
											edgeTarget + "\t" + coordTar[0] + "\t" + coordTar[1]);									
								}
								for (int i=0; i<edgeAttrValues.size(); i++) {
									_outBuffWriter.write("\t" + edgeAttrValues.get(i));
									edgeAttrValues.set(i, "");
								}
								_outBuffWriter.write("\n");
							}
							else {								
								if (_bLongIDs) {
									// write out unique Long IDs for edge source and dest for edges instead of 'original' node IDs
									double[] dataSrc = _nodemap.get(edgeSource);
									double[] dataTar = _nodemap.get(edgeTarget);
									_outBuffWriter.write("edge" + "\t" + (long)dataSrc[2] + "\t" + (long)dataTar[2]);
								}
								else {
									_outBuffWriter.write("edge" + "\t" + edgeSource + "\t" + edgeTarget);
								}
								for (int i=0; i<edgeAttrValues.size(); i++) {
									_outBuffWriter.write("\t" + edgeAttrValues.get(i));
									edgeAttrValues.set(i, "");
								}
								_outBuffWriter.write("\n");
							}
							
						} catch (IOException e) {
							
							e.printStackTrace();
						}	
													
						if ((_numEdges % 100000L == 0) && (_numEdges != 0)) {	// print message every 100,000 iterations
							System.out.println("Number of edges = " + _numEdges);
						}	
					}
					else if (qName.equalsIgnoreCase("data")) {
						bInData = false;
					}					
				}
				
				//--------------
				public void characters(char ch[], int start, int length)
						throws SAXException {
					
					if (bInData) {
						String strData = new String(ch, start, length);
						if (bInNode) {
							if (nodeAttrList.contains(dataKey))		//save node attribute value to map
								nodeAttrValues.set(nodeAttrList.indexOf(dataKey), strData);
						}
						else if (bInEdge) {
							if (edgeAttrList.contains(dataKey))		//save edge attribute value to map
								edgeAttrValues.set(edgeAttrList.indexOf(dataKey), strData);
						}
					}
				}
								
		    };
		    
			saxParser.parse(_graphmlInput, handler);
			
			_outBuffWriter.close();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		try {
			// save column labels to a readme file
			FileWriter fstream;
		    fstream = new FileWriter(_filenameOutReadme);
		    _outBuffWriterReadme = new BufferedWriter(fstream);
		    
		    _outBuffWriterReadme.write("This readme file contains column labels for tab-delimited graph data in \n" + _filenameOut + "\n\n");
		    _outBuffWriterReadme.write("Column labels for nodes are as follows:\n\n");
			
			if (_bLongIDs) {
				// write out the unique Long ID for this node as well as the 'original' nodeID
				_outBuffWriterReadme.write("node\tnode ID\toriginal node ID");
			}
			else {
				_outBuffWriterReadme.write("node\tnode ID");
			}
			
			for (int i=0; i<nodeAttrList.size(); i++) {
				_outBuffWriterReadme.write("\t" + nodeAttrList.get(i));
			}
			_outBuffWriterReadme.write("\n\n");				

		    _outBuffWriterReadme.write("Column labels for edges are as follows:\n\n");
		    
			_outBuffWriterReadme.write("edge\tsource ID\tdestination ID");

			for (int i=0; i<edgeAttrList.size(); i++) {
				_outBuffWriterReadme.write("\t" + edgeAttrList.get(i));
			}
			_outBuffWriterReadme.write("\n");
			
			_outBuffWriterReadme.close();
		    
		} catch (Exception e) {
			e.printStackTrace();
		}
	
		System.out.println("");
		System.out.println("Total number of nodes = " + _numNodes);
		System.out.println("Total number of edges = " + _numEdges);
		System.out.println("");
		System.out.println("Results saved at " + _filenameOut);
		System.out.println("Column labels saved at " + _filenameOutReadme);
		
		
	}
	
	//--------------------------------------------
	// Decode Morton z-order number to X, Y, Z co-ordinates
	// (from http://fgiesen.wordpress.com/2009/12/13/decoding-morton-codes/)
	private Integer[] mortonXYZ(long zOrder) {
		Integer[] coords = new Integer[3];
		
		coords[0] = compact1By2(zOrder);
		coords[1] = compact1By2(zOrder>>1);
		coords[2] = compact1By2(zOrder>>2);
				
		return coords;
	}
	
	//--------------------------------------------
	// Decode Morton z-order number to X and Y co-ordinates
	// (from http://fgiesen.wordpress.com/2009/12/13/decoding-morton-codes/)
	private Integer[] mortonXY(long zOrder) {
		Integer[] coords = new Integer[2];
		
		coords[0] = compact1By1(zOrder);
		coords[1] = compact1By1(zOrder>>1);
				
		return coords;
	}	
	
	private int compact1By2(long x)
	{
	  x &= 0x09249249;                  // x = ---- 9--8 --7- -6-- 5--4 --3- -2-- 1--0
	  x = (x ^ (x >>  2)) & 0x030c30c3; // x = ---- --98 ---- 76-- --54 ---- 32-- --10
	  x = (x ^ (x >>  4)) & 0x0300f00f; // x = ---- --98 ---- ---- 7654 ---- ---- 3210
	  x = (x ^ (x >>  8)) & 0xff0000ff; // x = ---- --98 ---- ---- ---- ---- 7654 3210
	  x = (x ^ (x >> 16)) & 0x000003ff; // x = ---- ---- ---- ---- ---- --98 7654 3210
	  return (int)x;
	}	
	
	private int compact1By1(long x)
	{
	  x &= 0x55555555;                  // x = -f-e -d-c -b-a -9-8 -7-6 -5-4 -3-2 -1-0
	  x = (x ^ (x >>  1)) & 0x33333333; // x = --fe --dc --ba --98 --76 --54 --32 --10
	  x = (x ^ (x >>  2)) & 0x0f0f0f0f; // x = ---- fedc ---- ba98 ---- 7654 ---- 3210
	  x = (x ^ (x >>  4)) & 0x00ff00ff; // x = ---- ---- fedc ba98 ---- ---- 7654 3210
	  x = (x ^ (x >>  8)) & 0x0000ffff; // x = ---- ---- ---- ---- fedc ba98 7654 3210
	  return (int)x;
	}
}
