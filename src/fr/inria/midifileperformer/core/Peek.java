package fr.inria.midifileperformer.core;

import fr.inria.bps.base.Event;

public class Peek<T> extends C<T> {
	C<T> master;
	boolean aheaded = false;
	Event<T> ahead = null;

	public Peek(C<T> master) {
		this.master = master;
	}

	public static <T> Peek<T> make(C<T> master) {
		return(new Peek<T>(master));
	}

	/*
	 * The contract is that if peek() not fails, then
	 * get() must not fails 
	 */
	public Event<T> get() throws EndOfStream  {
		if(!aheaded) return(master.get());
		aheaded = false;
		return(ahead);
	}

	public Event<T> peek() throws EndOfStream {
		if(!aheaded) { 
			ahead = master.get();
			aheaded = true;
		}
		return(ahead);
	}
	
	public Event<T> nextPeek() throws EndOfStream {
		get();
		return(peek());
	}
}
