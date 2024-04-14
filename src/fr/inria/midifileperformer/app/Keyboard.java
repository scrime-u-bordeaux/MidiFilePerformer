package fr.inria.midifileperformer.app;

import fr.inria.bps.base.Line;
import fr.inria.lognet.sos.Shape;
import fr.inria.lognet.sos.Sos;
import fr.inria.lognet.sos.event.SosMouse;
import fr.inria.midi.MidiLib;
import fr.inria.midifileperformer.impl.InputDevice;

public class Keyboard extends InputDevice {
	Configurator master;
	Shape shape;
	int[] pressed = new int[128];
	
	public Keyboard(Configurator master, Shape shape) {
		super("Keyboard");
		this.master = master;
		this.shape = shape;
		for(int i=0; i<128; i++) pressed[i] = 0;
		Sos.listen(SosMouse.enter, shape, e -> shape.picture.root.requestFocus());
		Sos.listen(fr.inria.lognet.sos.Event.keyboard, shape, e -> down());
		Sos.listen(fr.inria.lognet.sos.Event.unkeyboard, shape, e -> up());
	}

	void down() {
		int key = fr.inria.lognet.sos.Event.key;
		int velocity = getVelocity(key);
		if(key >= 0 && key < 128) {
			if(pressed[key] == 0) {
				pressed[key]=velocity;
				send(MidiLib.NoteOn(1, key, velocity));
			}
		}
	}

	static String[] qwerty = {
			"zxcvbnm,./",
			"asdfghjkl;'",
			"qwertyuiop[]\\",
			"1234567890-=",
	};

	static String[] azerty = {
			"wxcvbn,;:=",
			"qsdfghjklmù",
			"azertyuiop^$<",
			"@&é\"'{{è!à)-",
	};
	
	

	int getVelocity(int n) {
		if(master.config.velocityOnHeight) {
			Line d = new Line(shape.y, 128, shape.y+shape.height, 40);
			return(d.iget(SosMouse.y));
		}
		String[] map = master.config.qwerty ? qwerty : azerty;
		for(int i=0; i<map.length; i++) {
			int k = map[i].indexOf(n);
			if(k >= 0) return(30+i*15+(k/2));
		}
		return(100);
	}

	void up() {
		int key = fr.inria.lognet.sos.Event.key;
		if(key >= 0 && key < 128) {
			if(pressed[key] != 0) {
				pressed[key]=0;
				send(MidiLib.NoteOff(1, key, 0));
			}
		}
	}

}
