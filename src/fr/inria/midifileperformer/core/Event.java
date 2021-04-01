package fr.inria.midifileperformer.core;

import java.util.Comparator;
import java.util.Vector;

import fr.inria.bps.base.Vecteur;

public class Event<T> {
	public long time;
	public T value;	

	public Event(long time, T value) {
		this.time = time;
		this. value = value;
	}

	public static <T> Event<T> make(long time, T value) {
		return(new Event<T>(time, value));
	}

	public static <T> Event<T> NULL() {
		return(make(-1, null));
	}

	/*
	 * Convert to S-Event
	 */
	Event<Vector<T>> sing() {
		return(Event.make(time, Vecteur.sing(value)));
	}

	/*
	 * Comparator
	 */
	public static Comparator<Event<?>> comparator = new Comparator<Event<?>>() {
		public int compare(Event<?> ev1, Event<?> ev2) {
			return(Long.compare(ev1.time, ev2.time));
		}
	};

	/*
	 * toString
	 */
	public String toString() {
		return(time + ":" + value);
	}
}
