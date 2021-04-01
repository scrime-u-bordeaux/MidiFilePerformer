package fr.inria.midifileperformer.app;

import fr.inria.midifileperformer.MidiMsg;
import fr.inria.midifileperformer.core.Consumer;
import fr.inria.midifileperformer.core.Event;

public abstract class OutputDevice implements Consumer<Event<MidiMsg>> {
	public abstract void open();
	//public abstract void playall(C<MidiMsg> c);
	public abstract boolean same(OutputDevice dev);
	public abstract void close();

	public boolean equals(Object o) {
		if(o instanceof OutputDevice) return(same((OutputDevice) o));
		return(false);
	}

	public static OutputDevice byName(PlayerZone player, String name) {
		OutputDevice r = ChangeOutput.byName(name);
		if(r != null) return(r);
		r = PlayerOutputDevice.byName(player, name);
		if(r != null) return(r);
		return(null);
	}
}
