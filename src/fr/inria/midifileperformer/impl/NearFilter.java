package fr.inria.midifileperformer.impl;

import javax.sound.midi.MidiMessage;

import fr.inria.midi.MidiLib;

public class NearFilter {
	int[] pressed = new int[128];
	
	public NearFilter() {
		for(int i=0; i<128; i++) pressed[i] = 0;
	}
	
	public boolean filter(MidiMsg value) {
		//System.out.println("FF");
		MidiMessage msg = value.msg;
		if(MidiLib.isBegin(msg)) {
			int k = MidiLib.getKey(msg);
			int fake = fake(k);
			pressed[k] = fake;
			return(fake == 1);
		} else if(MidiLib.isEnd(msg)) {
			int k = MidiLib.getKey(msg);
			int fake = pressed[k];
			pressed[k] = 0;
			return(fake == 1);
		} else {
			return(true);
		}
	}
	
	int fake(int k) {
		//System.out.println("fake "+k);
		for(int i=Math.max(0, k-2); i<Math.min(128, k+3); i++) {
			//System.out.println(i+" => "+pressed[i]);
			if(pressed[i] == 1) return(2);
		}
		return(1);
	}
}
