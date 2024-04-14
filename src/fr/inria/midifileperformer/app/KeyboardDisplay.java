package fr.inria.midifileperformer.app;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;

import fr.inria.bps.base.Event;
import fr.inria.bps.base.Line;
import fr.inria.lognet.sos.Shape;
import fr.inria.lognet.sos.SosColor;
import fr.inria.midi.MidiLib;
import fr.inria.midifileperformer.impl.MidiMsg;
import fr.inria.midifileperformer.impl.OutputDevice;

public class KeyboardDisplay extends Shape {
	int[] pressed = new int[128];
	long oldTime = -1;

	public KeyboardDisplay(Configurator master) {
		this.width = 400;
		this.height = 10;
		for(int i=0; i<128; i++) pressed[i] = 0;
		KeyboardDisplay me = this;
		new OutputDevice("Keyboard Display") {
			public void accept(Event<MidiMsg> value) {
				super.accept(value);
				MidiMessage msg = value.value.msg;
				long time = value.time;
				if(MidiLib.isBegin(msg) || MidiLib.isEnd(msg)) {
					ShortMessage m = (ShortMessage) msg;
					int k = m.getData1();
					int v = m.getData2();
					me.pressed[k] = v;
					//System.out.println("KD "+k+" "+v);
					//me.picture.root.FullRepaint();
					if(oldTime != time) {
						me.dirty();
						picture.dorepaint();
						oldTime = time;
					}
				} else if(MidiLib.isAllNoteOff(msg)) {
					for(int i=0; i<pressed.length; i++) pressed[i] = 0;
					if(oldTime != time) {
						me.dirty();
						picture.dorepaint();
						oldTime = time;
					}
				}
			}
		};
	}

	static SosColor colors[] = SosColor.degrade(SosColor.blue, SosColor.red, 128);
	public void repaint(int cx, int cy, int w, int h) {
		for(int i=0; i<128; i++) {
			int velocity = pressed[i];
			if(velocity > 0) {
				int xn = new Line(0, 0, 128, width).iget(i);
				SosColor c = colors[velocity % colors.length];
				fillrectangle(c, xn, 0, 10, 10);
			}
		}
	}
}
