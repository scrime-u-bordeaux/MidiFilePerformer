package fr.inria.midifileperformer.core;

import java.util.Vector;

import fr.inria.bps.base.Event;

public class Record<T> {
	public Vector<Event<T>> recorded = new Vector<Event<T>>();
	// We don't need master anymore
	//C<T> master;

	public Record() {
	}

	public static <T> Record<T> make() {
		return(new Record<T>());
	}
	
	public void addNow(T value) {
		//System.out.println("record "+value);
		recorded.add(Event.now(value));
	}

}
