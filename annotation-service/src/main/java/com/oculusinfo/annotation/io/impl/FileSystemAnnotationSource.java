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
package com.oculusinfo.annotation.io.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import com.oculusinfo.annotation.AnnotationData;
import com.oculusinfo.annotation.io.serialization.AnnotationSerializer;
import com.oculusinfo.factory.util.Pair;


public class FileSystemAnnotationSource implements AnnotationSource {
	private String _rootPath;
	private String _extension;
	public final static String ANNOTATIONS_FOLDERNAME  = "data";

	public FileSystemAnnotationSource (String rootPath, String extension){
		//if there's no root path, then it should be based on a relative path, so make sure to set root path to '.'
		if (rootPath == null || rootPath.trim().length() == 0) {
			rootPath = "./";
		}
		
		//make sure the root path ends with a slash
		_rootPath = (rootPath.trim().endsWith("/"))? rootPath : rootPath.trim() + "/";
		_extension = extension;
	}

	private File getAnnotationFile (String basePath, Pair<String, Long> certificate) {
		return new File(String.format("%s/" + ANNOTATIONS_FOLDERNAME
		                              + "/%s." + _extension,
		                              _rootPath + basePath,
		                              certificate.getFirst() ) );
	}

	@Override
	public void initializeForWrite (String basePath) throws IOException {
	}

	@Override
	public void writeData (String basePath, AnnotationSerializer serializer,
	                            Iterable<AnnotationData<?>> data) throws IOException {
		for (AnnotationData<?> d: data) {
			File annotationFile = getAnnotationFile( basePath, d.getCertificate() );
			File parent = annotationFile.getParentFile();
			if (!parent.exists()) parent.mkdirs();

			FileOutputStream fileStream = new FileOutputStream(annotationFile);
			serializer.serialize(d, fileStream);
			fileStream.close();
		}
	}

	@Override
	public void initializeForRead(String basePath) {
		// Noop
	}
	
	@Override
	public List<AnnotationData<?>> readData (String basePath,
	                                        AnnotationSerializer serializer,
	                                        Iterable<Pair<String, Long>> certificates) throws IOException {
		List<AnnotationData<?>> results = new LinkedList<>();
		for (Pair<String, Long> certificate: certificates) {
			if (certificate == null) {
				continue;
			}
			File annotationFile = getAnnotationFile(basePath, certificate);

			if (annotationFile.exists() && annotationFile.isFile()) {
				FileInputStream stream = new FileInputStream(annotationFile);
				AnnotationData<?> data = serializer.deserialize(stream);
				results.add(data);
				stream.close();
			}
		}
		return results;
	}

	@Override
	public void removeData (String basePath, Iterable<Pair<String, Long>> certificates ) throws IOException {
		
		for (Pair<String, Long> certificate: certificates) {
			File annotationFile = getAnnotationFile(basePath, certificate);
			annotationFile.delete();
		}
	}
    
}
