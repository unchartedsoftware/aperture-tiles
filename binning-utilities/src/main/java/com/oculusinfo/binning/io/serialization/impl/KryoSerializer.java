package com.oculusinfo.binning.io.serialization.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.oculusinfo.binning.DenseTileData;
import com.oculusinfo.binning.SparseTileData;
import com.oculusinfo.binning.TileData;
import com.oculusinfo.binning.TileIndex;
import com.oculusinfo.binning.io.serialization.TileSerializer;
import com.oculusinfo.factory.util.Pair;
import com.oculusinfo.binning.util.TypeDescriptor;



/**
 * A generic Kryo serializer capable of serializing all sorts of tile data.
 * 
 * @author nkronenfeld
 *
 * @param <T> The type of data this instance of the serializer intends to serialize.
 */
public class KryoSerializer<T> implements TileSerializer<T> {
	private static final long serialVersionUID = -1983162656743117212L;



	// Per-thread info: a Kryo instance for registration, and an indicator of what classes will 
	// need to be registered.
	private static class ThreadInfo {
		Kryo _kryo;
		int _nextUpdate;
		ThreadInfo () {
			_kryo = new Kryo();
			_nextUpdate = 0;
		}
		// Get the kryo instance for this thread, updating it with new classes if necessary.
		Kryo getKryo () {
			__classRegistryLock.readLock().lock();
			try {
				int nextUpdate = __classesToRegister.size();
				if (nextUpdate > _nextUpdate) {
					for (int update = _nextUpdate; update < nextUpdate; ++update) {
						for (Class<?> newClass: __classesToRegister.get(update)) {
							_kryo.register(newClass);
						}
					}
				}
				_nextUpdate = nextUpdate;
			} finally {
				__classRegistryLock.readLock().unlock();
			}

			return _kryo;
		}
	}

	// Store a kryo instance per thread
	private static ThreadLocal<ThreadInfo> __threadInfo = new ThreadLocal<ThreadInfo>(){
		protected ThreadInfo initialValue () {
			return new ThreadInfo();
		}
	};

	// Get the kryo instance for this thread.
	private static Kryo kryo () {
		return __threadInfo.get().getKryo();
	}



	// Handle registering extra classes
	//
	// Classes are registered a set at a time.  Each thread keeps track of the latest set it's 
	// registered, and registers the new ones since then as necessary.
	//
	// The key in this map is the set number.  This number will be sequential, starting at 0 - 
	// so the next key to be added is always the current size of the set.
	private static Map<Integer, Collection<Class<?>>> __classesToRegister = new ConcurrentHashMap<>();
	// And we need a lock to prevent collisions
	private static ReadWriteLock __classRegistryLock = new ReentrantReadWriteLock();

	/**
	 * Register a set of classes for serialization/deserialization via Kryo.
	 * 
	 * Classes so registered <em>must</em> have a zero-argument constructor.
	 * 
	 * Trying to register the same class twice should be a no-op the second time.  However, this 
	 * is not a trivial test - and is less trivial the more classes are registered - so 
	 * re-registrations should be kept to a minimum.
	 * 
	 * Also, any classes registered via this class are stored for the life of the application -
	 * so don't overuse for that reason too.
	 * 
	 * @param classes The classes the user knows will be needed to serialize a tile set.
	 */
	public static void registerClasses (Class<?>... classes) {
		// Get the set of classes that need to be registered that haven't been.
		Set<Class<?>> newClasses = new HashSet<>(Arrays.asList(classes));
		int lastChecked = -1;
		__classRegistryLock.readLock().lock();
		try {
			for (Entry<Integer, Collection<Class<?>>> entry: __classesToRegister.entrySet()) {
				newClasses.removeAll(entry.getValue());
				if (newClasses.isEmpty()) break;
				lastChecked = Math.max(lastChecked, entry.getKey());
			}
		} finally {
			__classRegistryLock.readLock().unlock();
		}

		if (!newClasses.isEmpty()) {
			__classRegistryLock.writeLock().lock();

			try {
				// Make sure there are still more classes to register
				int update = lastChecked+1;
				while (update < __classesToRegister.size()) {
					newClasses.remove(__classesToRegister.get(update));
				}
    
				if (!newClasses.isEmpty()) {
					__classesToRegister.put(__classesToRegister.size(), Collections.unmodifiableSet(newClasses));
				}
			} finally {
				__classRegistryLock.writeLock().unlock();
			}
		}
	}

	// Our initial registration of classes we expect, by default, to need to register is handled 
	// using the same mechanism as other registration class updates - so should be update 0 on all
	// threads.
	static {
		Kryo testKryo = kryo();
		List<Class<?>> toRegister = new ArrayList<>();
		for (Class<?> initialClass: new Class<?>[] {
				DenseTileData.class,
					SparseTileData.class,

					// Standard primitives
					Boolean.class,
					Byte.class, Short.class, Integer.class, Long.class,
					Float.class, Double.class,
					String.class,
					java.util.UUID.class,

					// Standard collection types
					java.util.ArrayDeque.class,
					java.util.ArrayList.class,
					java.util.BitSet.class,
					java.util.HashMap.class,
					java.util.HashSet.class,
					java.util.Hashtable.class,
					java.util.IdentityHashMap.class,
					java.util.LinkedHashMap.class,
					java.util.LinkedHashSet.class,
					java.util.PriorityQueue.class,
					java.util.TreeMap.class,
					java.util.TreeSet.class,
					java.util.Vector.class,

					// Our own standard types
					Pair.class
					}) {
			// Don't re-register anything Kryo already registers by default - it should be 
			// harmless to do so (I checked the Kryo code), but just in case they ever change 
			// things, we wouldn't want to overwrite a class registered with a specific serializer.
			if (null == testKryo.getClassResolver().getRegistration(initialClass))
				toRegister.add(initialClass);
		}

		registerClasses(toRegister.toArray(new Class<?>[toRegister.size()]));
	}



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
	public KryoSerializer (TypeDescriptor typeDesc) {
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
