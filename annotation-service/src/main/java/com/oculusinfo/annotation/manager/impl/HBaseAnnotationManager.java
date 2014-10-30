/*
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
package com.oculusinfo.annotation.manager.impl;


import com.oculusinfo.annotation.data.AnnotationData;
import com.oculusinfo.annotation.io.AnnotationIO;
import com.oculusinfo.annotation.io.impl.HBaseAnnotationIO;
import com.oculusinfo.annotation.io.serialization.AnnotationSerializer;
import com.oculusinfo.annotation.io.serialization.impl.JSONAnnotationDataSerializer;
import com.oculusinfo.annotation.manager.AnnotationManager;
import com.oculusinfo.binning.util.Pair;
import java.io.IOException;
import java.util.List;


public class HBaseAnnotationManager implements AnnotationManager {
	
	private String _layer = null;	
	private AnnotationIO _dataIO = null;
	private AnnotationSerializer _dataSerializer = null;
	
	
	public HBaseAnnotationManager(	String layer, 
									String zookeeperQuorum,  
									String zookeeperPort, 
									String hbaseMaster) {
		
		try {
			_dataIO = new HBaseAnnotationIO( zookeeperQuorum,
											 zookeeperPort,
											 hbaseMaster );
		} catch (Exception e) {    		
			System.out.println("Error: " + e.getMessage());			
		}
		_dataSerializer = new JSONAnnotationDataSerializer();
		_layer = layer;
	}
	
	@Override
	public void writeAnnotations(List<AnnotationData<?>> annotations) throws IOException {
		try {  
			if ((_dataIO != null) && (_layer != null) && (_dataSerializer != null)) {
				_dataIO.writeData(_layer, _dataSerializer, annotations );
			}
		} catch (Exception e) {   		
			System.out.println("Error: " + e.getMessage());			
		} 
	}	
	
	@Override
	public List<AnnotationData<?>> readAnnotations(Iterable<Pair<String,Long>> dataIndices) throws IOException {
		
		List<AnnotationData<?>> results = null;
		
		try {  	
			results = _dataIO.readData( _layer, _dataSerializer, dataIndices );	
		} catch (Exception e) {   		
			System.out.println("Error: " + e.getMessage());			
		} 
		
		return results;
	}	
	
	@Override
    public void removeAnnotations(Iterable<Pair<String,Long>> dataIndices) throws IOException {
		try {  	
			_dataIO.removeData( _layer, dataIndices );	
		} catch (Exception e) {   		
			System.out.println("Error: " + e.getMessage());			
		} 		
	}

	@Override
	public void initializeForWrite() throws IOException {
		try {  	
			_dataIO.initializeForWrite(_layer);	
		} catch (Exception e) {   		
			System.out.println("Error: " + e.getMessage());			
		} 		
	}

}
