package fr.inria.midifileperformer.core;

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

	public Event<T> get() {
		if(!aheaded) return(master.get());
		aheaded = false;
		return(ahead);
	}

	public Event<T> peek() {
		if(!aheaded) { 
			aheaded = true;
			ahead = master.get();
		}
		return(ahead);
	}
}
