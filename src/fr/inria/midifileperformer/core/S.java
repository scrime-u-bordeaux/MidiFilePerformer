package fr.inria.midifileperformer.core;

import java.util.Vector;
import java.util.concurrent.LinkedBlockingQueue;

import fr.inria.fun.Fun0;
import fr.inria.midifileperformer.Lib;

public abstract class S<T> {
	public abstract T get();

	/*
	 * Generic Chronology construction based on functions
	 */
	public static <T> S<T> makeS(Fun0<T> get) {
		return(new S<T>() {
			public T get() {
				return(get.operation());
			};
		});
	}

	/*
	 * force
	 */
	public void force() {
		while(get() != null);
	}

	/*
	 * Infinite Stream
	 */
	public static <T> S<T> infinite(T value) {
		return(makeS(() -> value));
	}

	/*
	 * Metronome
	 */
	public static <T> S<T> clockS(int ms, T value) {
		return(makeS(() -> {
			Lib.sleep(ms);
			return(value);
		}));
	}

	/*
	 * collect
	 */
	public static <T1,T extends Producer<T1>> S<T1> collect(Vector<T> inputs) {
		LinkedBlockingQueue<T1> hub = new LinkedBlockingQueue<T1>();
		for( Producer<T1> in : inputs ) in.accept(hub);
		return(new S<T1>() {
			public T1 get() {
				try {
					return(hub.take());
				} catch (Exception e) {
					return(null);
				}
			}
		});
	}

	/*
	 * Transform to a Chronology where the "present" is inserted at "get"
	 */
	public C<T> live() {
		S<T> me = this;
		return(new C<T>() {
			public Event<T> get() {
				T value = me.get();
				if(value == null) return(null);
				return(Event.make(System.currentTimeMillis(), value));
			};
		});
	}

}
