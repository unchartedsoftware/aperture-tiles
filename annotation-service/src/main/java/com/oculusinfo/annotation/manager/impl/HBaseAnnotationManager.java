package com.oculusinfo.annotation.manager.impl;

import java.io.IOException;
import java.util.List;

import com.oculusinfo.annotation.data.AnnotationData;
import com.oculusinfo.annotation.io.AnnotationIO;
import com.oculusinfo.annotation.io.impl.HBaseAnnotationIO;
import com.oculusinfo.annotation.io.serialization.AnnotationSerializer;
import com.oculusinfo.annotation.io.serialization.impl.JSONAnnotationDataSerializer;
import com.oculusinfo.annotation.manager.AnnotationManager;
import com.oculusinfo.binning.util.Pair;


public class HBaseAnnotationManager implements AnnotationManager {
	
	private String _layer = null;	// _layer is equivalent to layer for Cyber
	
	private AnnotationIO _dataIO = null;
	private AnnotationSerializer _dataSerializer = null;
	
	public HBaseAnnotationManager(	String layer, 
									String zookeeperQuorum,  
									String zookeeperPort, 
									String hbaseMaster) {
		_layer = layer;
		
		try {
			_dataIO = new HBaseAnnotationIO( zookeeperQuorum,
											 zookeeperPort,
											 hbaseMaster );
		} catch (Exception e) {    		
			System.out.println("Error: " + e.getMessage());			
		}
		_dataSerializer = new JSONAnnotationDataSerializer();
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
