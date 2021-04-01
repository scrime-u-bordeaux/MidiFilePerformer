package fr.inria.midifileperformer.app;

import fr.inria.midifileperformer.MidiMsg;
import fr.inria.midifileperformer.core.Event;

public class PlayerOutputDevice extends OutputDevice {
    static String myName = "Player area";
    PlayerZone player;
    
    public PlayerOutputDevice(PlayerZone player) {
	this.player = player;
    }
    
    public static OutputDevice byName(PlayerZone player, String name) {
	if(name.compareTo(myName) == 0) return(new PlayerOutputDevice(player));
	return(null);
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
