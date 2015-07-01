package com.oculusinfo.binning.io.serialization.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.impl.DenseTileData;
import com.oculusinfo.binning.impl.SparseTileData;
import com.oculusinfo.binning.io.serialization.TileSerializer;
import com.oculusinfo.binning.util.TypeDescriptor;
import com.oculusinfo.factory.util.Pair;



/**
 * A generic Kryo serializer capable of serializing all sorts of tile data.
 *
 * @author nkronenfeld
 *
 * @param <T> The type of data this instance of the serializer intends to serialize.
 */
public class KryoSerializer<T> implements TileSerializer<T> {
	private static final long serialVersionUID = 611839716702420914L;
	public static enum Codec {DEFLATE, BZIP, GZIP};
	public static final Set<Class<?>> PRIMITIVE_TYPES =
		Collections.unmodifiableSet(new HashSet<Class<?>>() {
			private static final long serialVersionUID = 1L;

			{
				addAll(PrimitiveAvroSerializer.PRIMITIVE_TYPES);
				add(Short.class);
			}
		});



	// Store a kryo instance per thread
	transient private LocalizedKryo  _localKryo;
	// A list of classes each kryo instance must register
	private Class<?>[]     _classesToRegister;
	// Our type description
	private TypeDescriptor _typeDesc;
	private Codec _codec;

	/**
	 * Create a serializer.
	 *
	 * @param typeDesc
	 *            A detailed description of our fully expanded generic type.
	 *            This should, of course, match the generic type with which the
	 *            class was created; there is no way of enforcing this, but
	 *            violating it will cause a host of problems.
	 */
	public KryoSerializer (TypeDescriptor typeDesc, Class<?>... classesToRegister) {
		this(typeDesc, Codec.GZIP, classesToRegister);
	}
	public KryoSerializer (TypeDescriptor typeDesc, Codec codec, Class<?>... classesToRegister) {
		_typeDesc = typeDesc;
		_codec = codec;
		_classesToRegister = classesToRegister;
	}

	// Get the kryo instance for this thread.
	private Kryo kryo () {
		if (null == _localKryo)
			_localKryo = new LocalizedKryo();
		return _localKryo.get();
	}

	@Override
	public TypeDescriptor getBinTypeDescription() {
		return _typeDesc;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public TileData<T> deserialize(TileIndex index, InputStream stream)
		throws IOException {
		InputStream compressionStream;
		switch (_codec) {
		case BZIP:
			compressionStream = new BZipInputStreamWrapper(new BZip2CompressorInputStream(stream));
			break;
		case GZIP:
			compressionStream = new GzipCompressorInputStream(stream);
			break;
		case DEFLATE:
		default:
			compressionStream = new InflaterInputStream(stream);
			break;
		}
		Input input = new Input(compressionStream);
		try {

			Object data = kryo().readClassAndObject(input);
			if (data instanceof TileData) return (TileData) data;
			else return null;
		} finally {
			compressionStream.close();
			input.close();
		}
	}

	@Override
	public void serialize(TileData<T> data, OutputStream stream)
		throws IOException {
		OutputStream compressionStream;
		switch (_codec) {
		case BZIP:
			compressionStream = new BZip2CompressorOutputStream(stream);
			break;
		case GZIP:
			compressionStream = new GzipCompressorOutputStream(stream);
			break;
		case DEFLATE:
		default:
			compressionStream = new DeflaterOutputStream(stream);
			break;
		}
		Output output = new Output(compressionStream);
		try {
			kryo().writeClassAndObject(output, data);

			output.flush();
			compressionStream.flush();
		} finally {
			output.close();
			compressionStream.close();
		}
	}



	private class LocalizedKryo extends ThreadLocal<Kryo> {
		protected Kryo initialValue () {
			Kryo kryo = new Kryo();

			kryo.register(TileIndex.class);
			kryo.register(DenseTileData.class);
			kryo.register(SparseTileData.class);

			// Standard collection types
			kryo.register(java.util.ArrayDeque.class);
			kryo.register(java.util.ArrayList.class);
			kryo.register(java.util.BitSet.class);
			kryo.register(java.util.HashMap.class);
			kryo.register(java.util.HashSet.class);
			kryo.register(java.util.Hashtable.class);
			kryo.register(java.util.IdentityHashMap.class);
			kryo.register(java.util.LinkedHashMap.class);
			kryo.register(java.util.LinkedHashSet.class);
			kryo.register(java.util.PriorityQueue.class);
			kryo.register(java.util.TreeMap.class);
			kryo.register(java.util.TreeSet.class);
			kryo.register(java.util.Vector.class);

			// Our own standard types
			kryo.register(Pair.class);

			// And our custom classes
			for (Class<?> ctr: _classesToRegister) {
				kryo.register(ctr);
			}
			return kryo;
		}
	}

	/**
	 * This is just a quick wrapping InputStream to get around a bug in apache commons bzip
	 * uncompression, where when it is passed a buffer into which to read, and told to offset by
	 * the length of the buffer, it returns that the file is done, rather than that the buffer is
	 * done.
	 *
	 * The bug is Commons Compress / COMPRESS-300
	 * (https://issues.apache.org/jira/browse/COMPRESS-309)
	 *
	 * @author nkronenfeld
	 */
	public static class BZipInputStreamWrapper extends InputStream {
		private InputStream _source;
		public BZipInputStreamWrapper (InputStream source) {
			_source = source;
		}


		// This is the one we care about, the one with the bug in apache commons bzip compression
		@Override
		public int read (byte[] buffer, int offset, int length) throws IOException {
			if (offset == buffer.length) {
				return 0;
			}
			return _source.read(buffer, offset, length);
		}

		// The rest are just pass-throughs to _source
		@Override
		public int available () throws IOException {
			return _source.available();
		}

		@Override
		public void close () throws IOException {
			_source.close();
		}

		@Override
		public synchronized void mark (int readlimit) {
			_source.mark(readlimit);
		}

		@Override
		public boolean markSupported () {
			return _source.markSupported();
		}

		@Override
		public synchronized void reset () throws IOException {
			_source.reset();
		}

		@Override
		public int read () throws IOException {
			return _source.read();
		}

		@Override
		public int read (byte[] buffer) throws IOException {
			return _source.read(buffer);
		}

		@Override
		public long skip (long n) throws IOException {
			return _source.skip(n);
		}
	}
}
