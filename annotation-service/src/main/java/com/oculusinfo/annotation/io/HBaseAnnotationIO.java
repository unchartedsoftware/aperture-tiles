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
package com.oculusinfo.annotation.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;

import com.oculusinfo.annotation.*;

public class HBaseAnnotationIO {
	
    private static final String      META_DATA_INDEX      = "metadata";
    private static final byte[]      EMPTY_BYTES          = new byte[0];
    private static final byte[]      ANNOTATION_FAMILY_NAME = "annotationData".getBytes();
    public static final HBaseColumn  ANNOTATION_COLUMN    = new HBaseColumn(ANNOTATION_FAMILY_NAME, EMPTY_BYTES);
    private static final byte[]      METADATA_FAMILY_NAME = "metaData".getBytes();
    public static final HBaseColumn  METADATA_COLUMN      = new HBaseColumn(METADATA_FAMILY_NAME, EMPTY_BYTES);

    
    public static class HBaseColumn {
        byte[] family;
        byte[] qualifier;
        HBaseColumn (byte[] family, byte[] qualifier) {
            this.family = family;
            this.qualifier = qualifier;
        }
	    public byte[] getFamily    () { return family; }
	    public byte[] getQualifier () { return qualifier; }
    }

    
    private Configuration       _config;
    private HBaseAdmin          _admin;

    
    public HBaseAnnotationIO (String zookeeperQuorum, 
    						  String zookeeperPort, 
    						  String hbaseMaster) throws IOException {
        _config = HBaseConfiguration.create();
        _config.set("hbase.zookeeper.quorum", zookeeperQuorum);
        _config.set("hbase.zookeeper.property.clientPort", zookeeperPort);
        _config.set("hbase.master", hbaseMaster);
        _admin = new HBaseAdmin(_config);
    }


    public HBaseAdmin getAdmin() {
    	return _admin;
    }

    
    public void createTable( String tableName ) {
    	
    	HTableDescriptor tableDesc = new HTableDescriptor(TableName.valueOf( tableName ));            
        HColumnDescriptor metadataFamily = new HColumnDescriptor(METADATA_FAMILY_NAME);
        tableDesc.addFamily(metadataFamily);
        HColumnDescriptor tileFamily = new HColumnDescriptor(ANNOTATION_FAMILY_NAME);
        tableDesc.addFamily(tileFamily);
        
        try {
        	_admin.createTable(tableDesc);
        } catch (Exception e) {}
    }
    
    
    public void dropTable( String tableName ) {
    	
    	try {
    		_admin.disableTable( TableName.valueOf( tableName ) );
    		_admin.deleteTable( TableName.valueOf( tableName ) );
        } catch (Exception e) {}
    	
    	
    }
    
    
	/**
	 * Get the configuration used to connect to HBase.
	 */
	public Configuration getConfiguration () {
		return _config;
	}


    /*
     * Gets an existing table (without creating it)
     */
    private HTable getTable (String tableName) throws IOException {
        return new HTable(_config, tableName);
    }

    
    /**
	 * Determine the row ID we use in HBase for a given tile index
	 */
	public static byte[] rowIdFromAnnotation (AnnotationIndex annotation) {
        // Use the minimum possible number of digits for the tile key
        return annotation.getBytes();
    }


    
    /*
     * Given a put request (a request to put data into a table), add a single
     * entry into the request
     * 
     * @param existingPut
     *            The existing request. If null, a request will be created for
     *            the given row. If non-null, no check will be performed to make
     *            sure the put request is for the right row - this is the
     *            responsibility of the caller.
     * @param rowId
     *            The id of the row to put. This is only used if the existingPut
     *            is null.
     * @param column
     *            The column defining the entry in this row into which to put
     *            the data
     * @param data
     *            the data to put into the described entry.
     * @return The put request - the same as is passed in, or a new request if
     *         none was passed in.
     */
    private Put addToPut (Put existingPut, byte[] rowId, HBaseColumn column, byte[] data) {
        if (null == existingPut) {
            existingPut = new Put(rowId);
        }

        existingPut.add(column.family, column.qualifier, data);

        return existingPut;
    }

    /*
     * Write a series of rows out to the given table
     * 
     * @param table
     *            The table to which to write
     * @param rows
     *            The rows to write
     */
    private void writeRows (String tableName, List<Row> rows) throws InterruptedException, IOException {
        HTable table = getTable(tableName);
        table.batch(rows);
        table.flushCommits();
        table.close();
    }

    
    private Map<HBaseColumn, byte[]> decodeRawResult (Result row, HBaseColumn[] columns) {
        Map<HBaseColumn, byte[]> results = null;
        for (HBaseColumn column: columns) {
            if (row.containsColumn(column.family, column.qualifier)) {
                if (null == results) results = new HashMap<HBaseColumn, byte[]>(); 
                results.put(column, row.getValue(column.family, column.qualifier));
            }
        }
        return results;
    }

    /*
     * Read several rows of data.
     * 
     * @param table
     *            The table to read
     * @param rows
     *            The rows to read
     * @param columns
     *            The columns to read
     * @return A list, in the same order as the input rows of maps from column
     *         id to value. Columns missing from the data are also missing from
     *         the map. Rows which returned no data have a null instead of a
     *         map.
     */
    private List<Map<HBaseColumn, byte[]>> readRows (String tableName, List<byte[]> rows, HBaseColumn... columns) throws IOException {
        HTable table = getTable(tableName);

        List<Get> gets = new ArrayList<Get>(rows.size());
        for (byte[] rowId: rows) {
            Get get = new Get(rowId);
            for (HBaseColumn column: columns) {
                get.addColumn(column.family, column.qualifier);
            }
            gets.add(get);
        }

        Result[] results = table.get(gets);
        List<Map<HBaseColumn, byte[]>> allResults = new ArrayList<Map<HBaseColumn,byte[]>>(rows.size());
        for (Result result: results) {
            allResults.add(decodeRawResult(result, columns));
        }
        return allResults;
    }

     
    private List<Map<HBaseColumn, byte[]>> scanRange(String tableName, byte[] startRow, byte[] stopRow, HBaseColumn... columns) throws IOException {
        HTable table = getTable(tableName);

        Scan scan;
        if ( startRow.length > 0 && stopRow.length > 0) {
        	scan = new Scan( startRow, stopRow );
        } else {
        	scan = new Scan();
        }
	        
	    ResultScanner rs = table.getScanner(scan);
	    
	    List<Map<HBaseColumn, byte[]>> allResults = new ArrayList<Map<HBaseColumn,byte[]>>();

	    try {
	    	// process results
	    	for (Result r = rs.next(); r != null; r = rs.next()) {
	    		allResults.add(decodeRawResult(r, columns));
	    	}
	        
	    } finally {
	    	rs.close();  // always close the ResultScanner!
	    }
	    
	    table.close();
        return allResults;
    }

    
    public void initializeForWrite (String tableName) throws IOException {
        if (!_admin.tableExists(tableName)) {
            HTableDescriptor tableDesc = new HTableDescriptor(TableName.valueOf(tableName));            
            HColumnDescriptor metadataFamily = new HColumnDescriptor(METADATA_FAMILY_NAME);
            tableDesc.addFamily(metadataFamily);
            HColumnDescriptor tileFamily = new HColumnDescriptor(ANNOTATION_FAMILY_NAME);
            tableDesc.addFamily(tileFamily);
            _admin.createTable(tableDesc);
        }
    }


    public void writeAnnotations (String tableName, List<AnnotationData> annotations) throws IOException {
        
    	List<Row> rows = new ArrayList<Row>();
        for (AnnotationData annotation : annotations) {
        	byte [] bytes = annotation.getBytes();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write(bytes, 0, bytes.length);
            rows.add( addToPut(null, 
            				   rowIdFromAnnotation( annotation.getIndex() ),
                               ANNOTATION_COLUMN, 
                               baos.toByteArray()) );
        }
        
        try {
            writeRows(tableName, rows);
        } catch (InterruptedException e) {
            throw new IOException("Error writing annotations to HBase", e);
        }
    }

    
    public void writeMetaData (String tableName, String metaData) throws IOException {
        try {
            List<Row> rows = new ArrayList<Row>();
            rows.add(addToPut(null, META_DATA_INDEX.getBytes(), METADATA_COLUMN, metaData.getBytes()));
            Put put = new Put(META_DATA_INDEX.getBytes());
            put.add(METADATA_FAMILY_NAME, EMPTY_BYTES, metaData.getBytes());
            writeRows(tableName, rows);
        } catch (InterruptedException e) {
            throw new IOException("Error writing metadata to HBase", e);
        }
    }

    
    private List<AnnotationData> convertResults( List<Map<HBaseColumn, byte[]>> rawResults ) 
			throws IOException {
    	
    	List<AnnotationData> results = new LinkedList<AnnotationData>();

        Iterator<Map<HBaseColumn, byte[]>> iData = rawResults.iterator();

        while (iData.hasNext()) {
	        Map<HBaseColumn, byte[]> rawResult = iData.next();

            if (null != rawResult) {
                byte[] rawData = rawResult.get(ANNOTATION_COLUMN);
                ByteArrayInputStream bais = new ByteArrayInputStream(rawData);
                
                // TEMP
                int length = bais.available();
                byte [] buff = new byte[length];
                bais.read(buff);
                AnnotationData data = new AnnotationData(buff);
                results.add(data);
            }
        }

        return results;
    }
    
    /*
    private List<AggregatedAnnotation> aggregateAnnotations(List<AnnotationData> annotations, double clusterThreshold) {
    	 
    	List<AggregatedAnnotation> aggregates = new  LinkedList<AggregatedAnnotation>();	 
    	Map<Long, Boolean> alreadyAggregated = new  HashMap<Long, Boolean>();

    	for (int i=0; i < annotations.size(); i++) {    		
    		
    		AnnotationIndex annoI = annotations.get(i).getIndex();  
    		Long keyI = annoI.getIndex();
    		
    		// make sure not already aggregated
    		if ( alreadyAggregated.get( keyI ) != true ) {
    			
    		
    			for (int j=i+1; j < annotations.size(); j++) {
    			
	    			AnnotationIndex annoJ = annotations.get(j).getIndex();
	    			Long keyJ = annoJ.getIndex();
	    			
	    			// make sure not already aggregated
	        		if ( alreadyAggregated.get( keyJ ) != true ) {
	        			
	        			double distance = annoI.squaredDistanceFrom( annoJ );
		    			
		    			if ( distance < clusterThreshold ) {
		    				
		    				alreadyAggregated.put( keyI, true );
		    				alreadyAggregated.put( keyJ, true );
		    				aggregates.
		    			}
    				}
    			}
    	
    		}
    	
    	
    	
    	for (int i=0; i < annotations.size(); i++) {    		
    		
    		//AnnotationIndex index = annotations.get(i).getIndex();  
    		
    		for (int j=i+1; j < annotations.size(); j++) {
    			  
    			AnnotationData annoI = annotations.get(i);
    			AnnotationData annoJ = annotations.get(j);
    			
    			double distance = annoI.getIndex().squaredDistanceFrom( annoJ.getIndex() );
   			  			
    			if ( distance < clusterThreshold ) {
    				 Long keyI = annotations.get(i).getIndex().getIndex();
    				 Long keyJ = annotations.get(j).getIndex().getIndex();
    				 boolean clusterExists = false;
    				 
    				 AggregatedAnnotation agg;
    				 if (aggregates.containsKey( keyJ )) {
    					 // j annotation is already part of an aggregation
    					 agg = aggregates.get( keyJ );
    					 aggregates.put( keyI, agg );
    					 clusterExists = true;
    				 } else {	 
    					 if (aggregates.containsKey( keyI )) {
    						 agg = aggregates.get( keyI );
    						 aggregates.put( keyJ, agg );
    						 clusterExists = true;
    					 } else {
    						 agg = new AggregatedAnnotation();
    						 
    					 }
    				 }
    				 
    				 if (clusterExists) {
    					 // ensure this point is within mean
    					 double distance = annoI.getIndex().squaredDistanceFrom( annoJ.getIndex() );
    					 
    				 } else {
    					 // add to empty cluster
    					 agg.add( annoI );
    					 agg.add( annoJ );
    				 }
    					 
    				 
    				 
    				 
    				 
    			}
    		}
    	}
    	 
    	 return aggregates;
    }
    */
    
    public List<AnnotationData> scanAnnotations (String tableName, 
											 	 AnnotationIndex bottomLeft,
											 	 AnnotationIndex topRight) 
														   throws IOException {
    	
    	List<Map<HBaseColumn, byte[]>> rawResults = scanRange(tableName, 
    														  bottomLeft.getBytes(), 
    														  topRight.getBytes(),
    														  ANNOTATION_COLUMN);

    	return convertResults( rawResults );

    }
    
    
    public List<AnnotationData> scanAnnotations (String tableName) throws IOException {
					
    	byte [] empty = {};
		List<Map<HBaseColumn, byte[]>> rawResults = scanRange(tableName, empty, empty, ANNOTATION_COLUMN);		
		return convertResults( rawResults );
		
	}

    
    public List<AnnotationData> readAnnotations (String tableName,
												 List<AnnotationIndex> annotations) 
														throws IOException {
        List<byte[]> rowIds = new ArrayList<byte[]>();
        for (AnnotationIndex annotation: annotations) {
            rowIds.add(rowIdFromAnnotation(annotation));
        }
        
        List<Map<HBaseColumn, byte[]>> rawResults = readRows(tableName, rowIds, ANNOTATION_COLUMN);

        return convertResults( rawResults );
    }

    /*
    @Override
    public InputStream getTileStream (String tableName, TileIndex tile) throws IOException {
        List<String> rowIds = new ArrayList<String>();
        rowIds.add(rowIdFromTileIndex(tile));
        
        List<Map<HBaseColumn, byte[]>> rawResults = readRows(tableName, rowIds, TILE_COLUMN);
        Iterator<Map<HBaseColumn, byte[]>> iData = rawResults.iterator();

        if (iData.hasNext()) {
            Map<HBaseColumn, byte[]> rawResult = iData.next();
            if (null != rawResult) {
                byte[] rawData = rawResult.get(TILE_COLUMN);
                return new ByteArrayInputStream(rawData);
            }
        }

        return null;
    }
	*/

    public String readMetaData (String tableName) throws IOException {
        List<Map<HBaseColumn, byte[]>> rawData = readRows(tableName, 
        												  Collections.singletonList(META_DATA_INDEX.getBytes()),
        												  METADATA_COLUMN);

		if (null == rawData) return null;
		if (rawData.isEmpty()) return null;
		if (null == rawData.get(0)) return null;
	    if (!rawData.get(0).containsKey(METADATA_COLUMN)) return null;

        return new String(rawData.get(0).get(METADATA_COLUMN));
    }
    
}
