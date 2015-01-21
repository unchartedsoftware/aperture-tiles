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


import com.oculusinfo.annotation.AnnotationData;
import com.oculusinfo.annotation.io.AnnotationIO;
import com.oculusinfo.annotation.io.impl.HBaseAnnotationIO;
import com.oculusinfo.annotation.io.serialization.AnnotationSerializer;
import com.oculusinfo.annotation.io.serialization.JSONAnnotationDataSerializer;
import com.oculusinfo.annotation.manager.AnnotationManager;
import com.oculusinfo.factory.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * 	This class implements the AnnotationManager interface for use with an HBase data store
 */
public class HBaseAnnotationManager implements AnnotationManager {
	private static final Logger LOGGER = LoggerFactory.getLogger(HBaseAnnotationManager.class);
	
	private String _layer = null;	
	private AnnotationIO _dataIO = null;
	private AnnotationSerializer _dataSerializer = null;
	
	
	/**
	 * Constructor - initializes internal members as needed to write annotations
	 *               into HBase
	 * 
	 * @param layer
	 *            layer/table to be used for annotation operations within the HBase data store
	 *            
	 * @param zookeeperQuorum
	 *            An HBase configuration parameter, this should match the similar value in hbase-site.xml
	 *            
	 * @param zookeeperPort
	 *            An HBase configuration parameter, this should match the similar value in hbase-site.xml
	 *
	 * @param hbaseMaster
	 *            An HBase configuration parameter, this should match the similar value in hbase-site.xml   
	 *
	 */
	public HBaseAnnotationManager(	String layer,
	                                String zookeeperQuorum,
	                                String zookeeperPort,
	                                String hbaseMaster) {
		
		try {
			_dataIO = new HBaseAnnotationIO( zookeeperQuorum,
			                                 zookeeperPort,
			                                 hbaseMaster );
		} catch (Exception e) {    		
			LOGGER.error("Error creating internal HBase IO member", e);
		}
		_dataSerializer = new JSONAnnotationDataSerializer();
		_layer = layer;
	}
	
	
	/**
	 * Initialize the HBase layer/table for writing.
	 */
	@Override
	public void initializeForWrite() throws IOException {
		try {  	
			_dataIO.initializeForWrite(_layer);	
		} catch (Exception e) {   		
			LOGGER.error("Error trying to initialize HBase table", e);
		} 		
	}
	
	
	/**
	 * Write the collection of annotations to HBase
	 * 
	 * @param annotations
	 *            This is a list of AnnotationData objects to write into HBase
	 */
	@Override
	public void writeAnnotations(List<AnnotationData<?>> annotations) throws IOException {
		try {  
			if ((_dataIO != null) && (_layer != null) && (_dataSerializer != null)) {
				_dataIO.writeData(_layer, _dataSerializer, annotations );
			}
		} catch (Exception e) {   		
			LOGGER.error("Error trying to write annotations to HBase", e);
		} 
	}	
	
	
	/**
	 * Remove the collection of annotations from HBase
	 * 
	 * @param dataIndices
	 *            This is a list of Pairs that contain the timestamp(String) and UUID(Long) that correspond to 
	 *            the annotations to be removed from HBase
	 */
	@Override
    public void removeAnnotations(Iterable<Pair<String,Long>> dataIndices) throws IOException {
		try {  	
			_dataIO.removeData( _layer, dataIndices );	
		} catch (Exception e) {   		
			LOGGER.error("Error trying to remove annotations from HBase", e);
		} 		
	}
	
	
	/**
	 * Read a collection of annotations from HBase based on the indices passed in
	 * 
	 * @param dataIndices
	 *            This is a list of Pairs that contain the timestamp(String) and UUID(Long) that correspond to 
	 *            the annotations to be read in
	 * 
	 * @return a list of AnnotationData objects retrieved from HBase based on the indices passed in
	 */
	@Override
	public List<AnnotationData<?>> readAnnotations(Iterable<Pair<String,Long>> dataIndices) throws IOException {
		
		List<AnnotationData<?>> results = null;
		
		try {  	
			results = _dataIO.readData( _layer, _dataSerializer, dataIndices );	
		} catch (Exception e) {   		
			LOGGER.error("Error trying to read annotations from HBase", e);
		} 
		
		return results;
	}	

}
