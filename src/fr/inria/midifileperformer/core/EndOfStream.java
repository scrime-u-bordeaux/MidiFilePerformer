package fr.inria.midifileperformer.core;

public class EndOfStream extends Exception {
	private static final long serialVersionUID = 1L;
	
	public EndOfStream(String msg) {
		super(msg);
	}
}
