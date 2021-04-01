package fr.inria.midifileperformer;

import java.util.Vector;

import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;

import fr.inria.midifileperformer.core.Interval;
import fr.inria.midifileperformer.core.Merger;
import fr.inria.midifileperformer.core.SameEvent;

public class MidiMsg implements Interval, SameEvent<MidiMsg> {
	public MidiMessage msg;

	public MidiMsg(MidiMessage msg) {
		this.msg = msg;
	}

	public static MidiMsg NoteOn(int pitch, int velocity) {
		try {
			ShortMessage msg = new ShortMessage();
			msg.setMessage(ShortMessage.NOTE_ON, 1, pitch, velocity);
			return(new MidiMsg(msg));
		} catch(Exception e) {
			throw(new RuntimeException(e));
		}
	}

	public static MidiMsg NoteOff(int pitch, int velocity) {
		try {
			ShortMessage msg = new ShortMessage();
			msg.setMessage(ShortMessage.NOTE_ON, 1, pitch, 0);
			return(new MidiMsg(msg));
		} catch(Exception e) {
			throw(new RuntimeException(e));
		}
	}

	public static MidiMsg allNoteOff(int channel) {
		try {
			ShortMessage msg = new ShortMessage();
			msg.setMessage(ShortMessage.CONTROL_CHANGE, channel, 123, 0);
			return(new MidiMsg(msg));
		} catch(Exception e) {
			throw(new RuntimeException(e));
		}
	}

	public static Vector<MidiMsg> allNotesOff = new Vector<MidiMsg>(16);
	static {
		for(int i=0; i<16; i++) allNotesOff.add(allNoteOff(i));
	};


	public boolean isBegin() {
		if(msg instanceof ShortMessage) {
			return((((ShortMessage) msg).getCommand() == 0x90) && getVelocity() != 0);	    
		} else {
			return(false);
		}
	}

	public boolean isEnd() {
		if(msg instanceof ShortMessage) {
			ShortMessage sm = (ShortMessage) msg;
			return(((sm.getCommand() == 0x80) || ((sm.getCommand() == 0x90) ) && getVelocity()==0));	    
		} else {
			return(false);
		}
	}

	public int changeTempo() {
		if(msg instanceof MetaMessage) {
			MetaMessage m = (MetaMessage) msg;
			int type = m.getType();
			byte[] bytes = m.getData();
			if(type == 0x51) return(readInt(bytes));
		}
		return(-1);
	}

	public boolean isPedal() {
		if(msg instanceof ShortMessage) {
			ShortMessage sm = (ShortMessage) msg;
			return(sm.getCommand() == 0xB0 && sm.getData1()==0x40);	    
		} else {
			return(false);
		}
	}

	public boolean isMeta() {
		if(msg instanceof ShortMessage) {
			ShortMessage sm = (ShortMessage) msg;
			return((sm.getCommand() != 0x80) && (sm.getCommand() != 0x90));
		} else {
			return(true);
		}
	}

	public boolean correspond(MidiMsg m2) {
		MidiMessage msg2 = m2.msg;
		if((!(msg instanceof ShortMessage)) || (!(msg2 instanceof ShortMessage))) 
			return(false);
		ShortMessage sm = (ShortMessage) msg;
		ShortMessage sm2 = (ShortMessage) msg2;
		return(sm.getData1() == sm2.getData1());
	}


	public static Merger<MidiMsg,MidiMsg,MidiMsg> merge = 
			new Merger<MidiMsg,MidiMsg,MidiMsg>() {
		public MidiMsg merge(MidiMsg x1, MidiMsg x2) {
			if(!x1.isBegin()) return(x1);
			return(NoteOn(x1.getKey(), x2.getVelocity()));
		}
		public MidiMsg left(MidiMsg x1) {
			return(x1);
		}
		public MidiMsg right(MidiMsg x2) {
			return(x2);
		}
	};



	public String info() {
		if(msg instanceof MetaMessage) {
			MetaMessage m = (MetaMessage) msg;
			int type = m.getType();
			byte[] bytes = m.getData();
			if(type == 0x01) return("TEXT("+text(bytes)+")");
			if(type == 0x02) return("COPYRIGHT("+text(bytes)+")");
			if(type == 0x03) return("TRACKNAME("+text(bytes)+")");
			if(type == 0x51) return("TEMPO("+readInt(bytes)+")");
			return("META " + hex((byte) type) + " " + hex(bytes));
		} 
		if(msg instanceof SysexMessage) return("SYS");
		int n = msg.getStatus();
		if(n >= 128 && n<=143) return("OFF(" + (n-127) + "," + note(getKey()) + "," + getVelocity()+")"); 
		if(n >= 144 && n<=159) return((getVelocity()==0 ? "OFF(" : "ON(") + (n-143) + "," + note(getKey()) + "," + getVelocity()+")");
		if(n >= 160 && n<=175) return("PAFT c=" + (n-159) + " n=" + getKey() + " v=" + getVelocity());
		if(n >= 176 && n<=191) return("CC(" + (n-175) + "," + hex(getKey())+","+hex(getVelocity())+")");
		if(n >= 192 && n<=207) return("PC c=" + (n-191) + " n=" + getKey());
		if(n >= 208 && n<=223) return("AFT c=" + (n-207) + " v=" + getKey());
		if(n >= 224 && n<=239) return("WHL c=" + (n-223) + " l=" + getKey() + " m=" + getVelocity());
		return(n + " " + getData());
	}

	public static final String[] NOTE_ENAMES = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
	public static final String[] NOTE_FNAMES = {"Do", "Do#", "Re", "Mib", "Mi", "Fa", "Fa#", "Sol", "Sol#", "La", "Sib", "Si"};
	static String note(int k) {
		int octave = (k / 12)-1;
		int note = k % 12;
		String noteName = NOTE_FNAMES[note];
		return(noteName + octave);
	}

	String getData() {
		return(hex(msg.getMessage()));
	}

	String text(byte[] b) {
		return(new String(b));
	}

	int readInt(byte[] b) {
		int n = b.length;
		int r = 0;
		for(int i=0; i<n; i++) r = (r << 8) + (int) (b[i] & 0xFF);
		return(r);
	}

	static String HEXA[] = new String[]{"0","1","2","3","4","5","6","7","8","9","A","B","C","D","E","F"};

	String hex(byte[] data) {
		String r = "";
		for(int i=0; i<data.length; i++) r = r + (i==0 ? "" : " ") + hex(data[i]);
		return("["+r+"]");
	}

	static String hex(byte b) {
		return(HEXA[(b>>4) & 0xF]+HEXA[b&0xF]);
	}


	static String hex(int b) {
		return(HEXA[(b>>4) & 0xF]+HEXA[b&0xF]);
	}

	String hexInt(int n) {
		return(Integer.toUnsignedString(n, 16));
	}

	public int getKey() {
		return(((ShortMessage) msg).getData1());
	}

	int getVelocity() {
		if(!(msg instanceof ShortMessage)) return(0);
		return(((ShortMessage) msg).getData2());
	}

	public String toString() {
		return(info());
	}

}
