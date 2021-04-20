package fr.inria.midifileperformer.core;

import java.util.Vector;

public class Record<T> extends C<T> {
	public Vector<Event<T>> recorded = new Vector<Event<T>>();
	C<T> master;

	public Record(C<T> master) {
		this.master = master;
	}

	public static <T> Record<T> make(C<T> master) {
		return(new Record<T>(master));
	}

	public Event<T> get() throws EndOfStream {
		Event<T> r = master.get();
		recorded.add(r);
		return(r);
	}

}
