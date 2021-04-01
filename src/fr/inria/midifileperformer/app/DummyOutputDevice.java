package fr.inria.midifileperformer.app;

import fr.inria.midifileperformer.MidiMsg;
import fr.inria.midifileperformer.core.Event;

public class DummyOutputDevice extends OutputDevice {

	public boolean same(OutputDevice dev) {
		if(dev instanceof DummyOutputDevice) return(true);
		return(false);
	}

	public void open() {
		System.out.println("open");
	}

	public void accept(Event<MidiMsg> value) {
		System.out.println("accept " + value);
	}

	public void close() {
		System.out.println("close");
	}

}
