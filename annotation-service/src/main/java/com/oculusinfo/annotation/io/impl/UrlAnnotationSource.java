package com.oculusinfo.annotation.io.impl;

import com.oculusinfo.annotation.AnnotationData;
import com.oculusinfo.annotation.io.AnnotationIO;
import com.oculusinfo.annotation.io.serialization.AnnotationSerializer;
import com.oculusinfo.binning.io.PyramidIO;
import com.oculusinfo.factory.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

public class UrlAnnotationSource implements AnnotationSource {
	private final Logger LOGGER = LoggerFactory.getLogger(getClass());
	private String _rootPath;
	private String _extension;

	public UrlAnnotationSource (String rootPath, String extension){
		_rootPath = rootPath;
		_extension = extension;
	}

	@Override
	public void initializeForRead(String host) {}

	@Override
	public List<AnnotationData<?>> readData (String basePath,
											 AnnotationSerializer serializer,
											 Iterable<Pair<String, Long>> certificates) throws IOException {
		List<AnnotationData<?>> results = new LinkedList<>();
		for (Pair<String, Long> certificate: certificates) {
			if (certificate == null) {
				continue;
			}

			String location = String.format(
				"http://%s/" + FileSystemAnnotationSource.ANNOTATIONS_FOLDERNAME + "/%s." + _extension,
				_rootPath + "/" + basePath, certificate.getFirst());

			InputStream stream = null;
			try {
				stream = new URL(location).openStream();
				AnnotationData<?> data = serializer.deserialize(stream);
				results.add(data);
			} catch (MalformedURLException e){
				LOGGER.error("Malformed URL supplied", e);
				return results;
			} catch (IOException e){
				LOGGER.error("Unable to open URL stream", e);
				return results;
			}
			if (stream != null) {
				stream.close();
			}
		}
		return results;
	}

	@Override
	public void initializeForWrite(String tableName) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void writeData(String id, AnnotationSerializer serializer, Iterable<AnnotationData<?>> data) throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void removeData(String id, Iterable<Pair<String, Long>> data) throws IOException {
		throw new UnsupportedOperationException();
	}
}
