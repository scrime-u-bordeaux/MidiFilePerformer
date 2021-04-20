package fr.inria.midifileperformer.core;

import java.util.Vector;
import java.util.concurrent.LinkedBlockingQueue;

import fr.inria.midifileperformer.Lib;

public abstract class S<T> {
	public abstract T get() throws EndOfStream;

	/*
	 * Infinite Stream
	 */
	public static <T> S<T> infinite(T value) {
		return(new S<T>() {
			public T get() {
				return(value);
			};
		});
	}

	/*
	 * Metronome
	 */
	public static <T> S<T> clockS(int ms, T value) {
		return(new S<T>() {
			public T get() {
				Lib.sleep(ms);
				return(value);
			};
		});
	}

	/*
	 * force
	 */
	public void force() {
		try {
			while(true) get();
		} catch (EndOfStream e) {
		}
	}

	/*
	 * Transform to a Chronology where the "present" is inserted at "get"
	 */
	public C<T> live() {
		S<T> me = this;
		return(new C<T>() {
			public Event<T> get() throws EndOfStream {
				T value = me.get();
				return(Event.make(System.currentTimeMillis(), value));
			};
		});
	}

	/*
	 * collect
	 */
	public static <T1,T extends Producer<T1>> S<T1> collect(Vector<T> inputs) {
		LinkedBlockingQueue<T1> hub = new LinkedBlockingQueue<T1>();
		for( Producer<T1> in : inputs ) in.accept(hub);
		return(new S<T1>() {
			public T1 get() throws EndOfStream {
				try {
					return(hub.take());
				} catch (Exception e) {
					throw(new EndOfStream("Queue is broken"));
				}
			}
		});
	}

}
