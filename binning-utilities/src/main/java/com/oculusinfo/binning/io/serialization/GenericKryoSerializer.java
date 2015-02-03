package com.oculusinfo.binning.io.serialization;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.util.Pair;
import com.oculusinfo.binning.util.TypeDescriptor;

public class GenericKryoSerializer<T> implements TileSerializer<T> {
	private static final long serialVersionUID = -8858900185834667720L;



	private static ThreadLocal<Kryo> __kryo = new ThreadLocal<Kryo>(){
		protected Kryo initialValue () {
			Kryo kryo = new Kryo();
			register(kryo);
			return kryo;
		}
	};
	private static Kryo kryo () {
		return __kryo.get();
	}
	public static void register (Kryo kryo) {
		kryo.register(TileData.class);
		kryo.register(Integer.class);
		kryo.register(Long.class);
		kryo.register(Float.class);
		kryo.register(Double.class);
		kryo.register(String.class);
		kryo.register(HashMap.class);
		kryo.register(ArrayList.class);
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
