package fr.inria.midifileperformer.app;

import java.util.concurrent.LinkedBlockingQueue;

import fr.inria.midifileperformer.MidiMsg;

public class PlayerInputDevice extends InputDevice {
    static String myName = "Computer keyboard";
    PlayerZone player;
    
    public PlayerInputDevice(PlayerZone player) {
	this.player = player;
    }
    
    public static InputDevice byName(PlayerZone player, String name) {
	if(name.compareTo(myName) == 0) return(new PlayerInputDevice(player));
	return(null);
    }
    
    public boolean same(InputDevice dev) {
	if(dev instanceof PlayerInputDevice) return(true);
	return(false);
    }

    public void accept(LinkedBlockingQueue<MidiMsg> queue) {
	player.accept(queue);
    }
    
    public void close() {
	player.close();
    }
    
    public String toString() {
	return(myName);
    }
}
