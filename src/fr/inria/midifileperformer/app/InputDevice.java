package fr.inria.midifileperformer.app;

import fr.inria.midifileperformer.MidiMsg;
import fr.inria.midifileperformer.core.Producer;

public abstract class InputDevice implements Producer<MidiMsg> {
	public abstract void close();
	public abstract boolean same(InputDevice dev);

	public boolean equals(Object o) {
		if(o instanceof InputDevice) return(same((InputDevice) o));
		return(false);
	}

	public static InputDevice byName(PlayerZone player, String name) {
		InputDevice r = ChangeInput.byName(name);
		if(r != null) return(r);
		r = PlayerInputDevice.byName(player, name);
		if(r != null) return(r);
		return(null);
	}
}
