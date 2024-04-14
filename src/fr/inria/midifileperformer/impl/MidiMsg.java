package fr.inria.midifileperformer.impl;

import java.util.Vector;

import javax.sound.midi.MidiMessage;

import fr.inria.bps.base.Event;
import fr.inria.midi.MidiLib;
import fr.inria.midifileperformer.core.Interval;
import fr.inria.midifileperformer.core.Merger;
import fr.inria.midifileperformer.core.SameEvent;

public class MidiMsg implements Interval, SameEvent<MidiMsg> {
	// Do we have tested extends MidiMessage ?!
	public MidiMessage msg;

	public MidiMsg(MidiMessage msg) {
		this.msg = msg;
	}

	/*
	 * Interval
	 */
	public boolean isBegin() {
		return(MidiLib.isBegin(msg));
	}

	public boolean isEnd() {
		return(MidiLib.isEnd(msg));
	}

	/*
	 * SameEvent
	 */
	public boolean correspond(MidiMsg m2) {
		return(MidiLib.correspond(msg, m2.msg));
	}

	/*
	 * nextOn
	 */
	public static long firstOn(Vector<Event<MidiMsg>> toPlay) {
		int n = toPlay.size();
		for(int i=0; i<n; i++) {
			Event<MidiMsg> e = toPlay.get(i);
			if(e.value.isBegin()) return(e.time);
		}
		return(toPlay.get(0).time);
	}

	public static long nextOn(Vector<Event<MidiMsg>> toPlay, int cur) {
		int n = toPlay.size();
		for(int i=cur; i<n; i++) {
			Event<MidiMsg> e = toPlay.get(i);
			if(e.value.isBegin()) return(e.time);
		}
		return(toPlay.get(n-1).time);
	}


	/*
	 * 
	 */

	public static MidiMsg NoteOn(int chan, int pitch, int velocity) {
		return(new MidiMsg(MidiLib.NoteOn(chan, pitch, velocity)));
	}

	public static MidiMsg NoteOff(int chan, int pitch, int velocity) {
		return(new MidiMsg(MidiLib.NoteOff(chan, pitch, velocity)));
	}

	public static MidiMsg allNoteOff(int channel) {
		return(new MidiMsg(MidiLib.allNoteOff(channel)));
	}

	public static Vector<MidiMsg> allNotesOff = new Vector<MidiMsg>(16);
	static {
		for(int i=0; i<16; i++) allNotesOff.add(allNoteOff(i));
	};

	public static Merger<MidiMsg,MidiMsg,MidiMsg> merge = 
			new Merger<MidiMsg,MidiMsg,MidiMsg>() {
		public MidiMsg merge(MidiMsg x1, MidiMsg x2) {
			if(!MidiLib.isBegin(x1.msg)) return(x1);
			MidiMsg r = NoteOn(MidiLib.getChannel(x1.msg), MidiLib.getKey(x1.msg), MidiLib.getVelocity(x2.msg));
			//System.out.println("Merge "+x1+" "+x2+"="+r);
			return(r);
		}
		public MidiMsg left(MidiMsg x1) {
			return(x1);
		}
		public MidiMsg right(MidiMsg x2) {
			return(x2);
		}
	};

	public String toString() {
		return(MidiLib.info(msg));
	}

}
