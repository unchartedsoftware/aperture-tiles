package com.oculusinfo.binning.io.serialization;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.util.Pair;
import com.oculusinfo.binning.util.TypeDescriptor;

public class GenericKryoSerializer<T> implements TileSerializer<T> {
    // Per-thread info: a Kryo instance for registration, and the last update of classes to register 
    // that was used.
	private static class ThreadInfo {
	    Kryo _kryo;
	    int _lastUpdate;
	    ThreadInfo () {
	        _kryo = new Kryo();
	        _lastUpdate = -1;
	    }
	}

	private static ThreadLocal<ThreadInfo> __threadInfo = new ThreadLocal<ThreadInfo>(){
		protected ThreadInfo initialValue () {
		    ThreadInfo info = new ThreadInfo();
		    register(info._kryo);
			return info;
		}
	};

	private static Kryo kryo () {
	    return __threadInfo.get()._kryo;
	}

	// ToDo: Registry of classes to register
	public static void register (Kryo kryo) {
		kryo.register(TileData.class);
		// Standard primitives
		kryo.register(Boolean.class);
		kryo.register(Byte.class);
		kryo.register(Short.class);
		kryo.register(Integer.class);
		kryo.register(Long.class);
		kryo.register(Float.class);
		kryo.register(Double.class);
		kryo.register(String.class);
		kryo.register(java.util.UUID.class);

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
	}



	private TypeDescriptor _typeDesc;
	public GenericKryoSerializer (TypeDescriptor typeDesc) {
		_typeDesc = typeDesc;
	}

	@Override
	public TypeDescriptor getBinTypeDescription() {
		return _typeDesc;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public TileData<T> deserialize(TileIndex index, InputStream stream)
			throws IOException {
		Input input = new Input(stream);
		try {
	
			Object data = kryo().readClassAndObject(input);
			if (data instanceof TileData) return (TileData) data;
			else return null;
		} finally {
			input.close();
		}
	}

	@Override
	public void serialize(TileData<T> data, OutputStream stream)
			throws IOException {
		Output output = new Output(stream);

		kryo().writeClassAndObject(output, data);

		output.flush();
		output.close();
	}
}
