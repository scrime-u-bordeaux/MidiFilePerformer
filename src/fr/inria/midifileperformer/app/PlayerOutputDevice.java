package fr.inria.midifileperformer.app;

import fr.inria.midifileperformer.core.Event;
import fr.inria.midifileperformer.impl.MidiMsg;
import fr.inria.midifileperformer.impl.OutputDevice;

public class PlayerOutputDevice extends OutputDevice {
	static String myName = "Player area";
	PlayerZone player;

	public PlayerOutputDevice(PlayerZone player) {
		this.player = player;
	}
	
	public static void launch(PlayerZone player) {
		devices.put(myName, new PlayerOutputDevice(player));
	}

	public boolean same(OutputDevice dev) {
		if(dev instanceof PlayerOutputDevice) return(true);
		return(false);
	}

	public void open() {
	}

	public void accept(Event<MidiMsg> value) {
		player.display(value);
	}

	public void close() {
	}

	public String toString() {
		return(myName);
	}
}
