package fr.inria.midifileperformer.core;

import java.util.concurrent.LinkedBlockingQueue;

import fr.inria.bps.base.Event;
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
				//System.out.println("main get "+value);
				return(Event.make(System.currentTimeMillis(), value));
			};
		});
	}

	/*
	 * collect
	 */
	public static <T> S<T> collect(LinkedBlockingQueue<T> hub) {
		return(new S<T>() {
			public T get() throws EndOfStream {
				try {
					//System.out.println("collect");
					T obj = hub.take();
					//System.out.println("collected "+obj);
					return(obj);
				} catch (Exception e) {
					throw(new EndOfStream("Queue is broken"));
				}
			}
		});
	}

}
