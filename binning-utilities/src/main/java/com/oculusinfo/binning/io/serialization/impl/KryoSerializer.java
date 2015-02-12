package com.oculusinfo.binning.io.serialization.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.oculusinfo.binning.DenseTileData;
import com.oculusinfo.binning.SparseTileData;
import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.TileIndex;
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



    // Store a kryo instance per thread
    transient private LocalizedKryo  _localKryo;
    // A list of classes each kryo instance must register
    private Class<?>[]     _classesToRegister;
    // Our type description
    private TypeDescriptor _typeDesc;
	
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
		_typeDesc = typeDesc;
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
        Input input = new Input(new InflaterInputStream(stream));
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
        Output output = new Output(new DeflaterOutputStream(stream));

		kryo().writeClassAndObject(output, data);

		output.flush();
		output.close();
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
}
