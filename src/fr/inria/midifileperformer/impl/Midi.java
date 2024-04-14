package fr.inria.midifileperformer.impl;

import java.io.File;
import java.net.URL;
import java.util.Vector;
import java.util.concurrent.LinkedBlockingQueue;

import javax.sound.midi.Instrument;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.SysexMessage;
import javax.sound.midi.Track;
import javax.sound.midi.Transmitter;

import fr.inria.bps.base.Event;
import fr.inria.bps.base.Pair;
import fr.inria.bps.base.Vecteur;
import fr.inria.fun.Proc1;
import fr.inria.midi.MidiLib;
import fr.inria.midifileperformer.core.C;
import fr.inria.midifileperformer.core.EndOfStream;

public class Midi {
	
	public static void main(String[] args) {
		MidiLib.seeInfo();
	}

	/*
	 * General Info
	 */

	public static Vector<Pair<Integer,MidiDevice>> getIns() {
		Vector<Pair<Integer,MidiDevice>> r = new Vector<Pair<Integer,MidiDevice>>();
		MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
		for(int i=0; i<infos.length; i++) {
			try {
				MidiDevice md = MidiSystem.getMidiDevice(infos[i]);
				if(md.getMaxTransmitters() != 0) r.add(Pair.cons(i, md));
			} catch(Exception e) {}
		}
		return(r);
	}

	public static Vector<Pair<Integer,MidiDevice>> getOuts() {
		Vector<Pair<Integer,MidiDevice>> r = new Vector<Pair<Integer,MidiDevice>>();
		MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
		for(int i=0; i<infos.length; i++) {
			try {
				MidiDevice md = MidiSystem.getMidiDevice(infos[i]);
				if(md.getMaxReceivers() != 0) r.add(Pair.cons(i, md));
			} catch(Exception e) {}
		}
		return(r);
	}

	/*
	 * Controllers
	 */
	public static Receiver getOut(int out) {
		try{
			MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
			MidiDevice mdout = MidiSystem.getMidiDevice(infos[out]);
			mdout.open();
			Receiver receiver = mdout.getReceiver();
			return(receiver);
		} catch (Exception e) {
			throw(new RuntimeException(e));
		}
	}

	public static void checkOud(int out) { 
		try{
			MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
			MidiDevice mdout = MidiSystem.getMidiDevice(infos[out]);
			mdout.open();
			ShortMessage on = new ShortMessage();
			on.setMessage(ShortMessage.NOTE_ON, 0, 60, 100);
			ShortMessage off = new ShortMessage();
			off.setMessage(ShortMessage.NOTE_OFF, 0, 60, 100);
			Receiver receiver = mdout.getReceiver();
			receiver.send(on, 0);
			try { Thread.sleep(1000); // wait time in milliseconds to control duration
			} catch( InterruptedException e ) { }
			receiver.send(off, 0);
		} catch (Exception e) {
			throw(new RuntimeException(e));

		}
	}

	public static void checkOut() { 
		try{
			/* Create a new Sythesizer and open it. Most of 
			 * the methods you will want to use to expand on this 
			 * example can be found in the Java documentation here: 
			 * https://docs.oracle.com/javase/7/docs/api/javax/sound/midi/Synthesizer.html
			 */
			Synthesizer midiSynth = MidiSystem.getSynthesizer(); 
			midiSynth.open();

			//get and load default instrument and channel lists
			Instrument[] instr = midiSynth.getDefaultSoundbank().getInstruments();
			MidiChannel[] mChannels = midiSynth.getChannels();

			midiSynth.loadInstrument(instr[0]);//load an instrument


			mChannels[0].noteOn(60, 100);//On channel 0, play note number 60 with velocity 100 
			try { Thread.sleep(1000); // wait time in milliseconds to control duration
			} catch( InterruptedException e ) { }
			mChannels[0].noteOff(60);//turn of the note


		} catch (MidiUnavailableException e) {}
	}

	public static Transmitter getIn(int in) {
		try{
			MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
			MidiDevice mdin = MidiSystem.getMidiDevice(infos[in]);
			Transmitter transmitter = mdin.getTransmitter();
			mdin.open();
			return(transmitter);
		} catch (Exception e) {
			System.out.println("Error in getIn " + e);
			throw(new RuntimeException(e));
		}
	}

	/*
	 * File
	 */
	public static Vector<Event<MidiMsg>> getEvents(Sequence sequence) {
		Vector<Event<MidiMessage>> r = MidiLib.getEventsMs(sequence);
		return(Vecteur.map(r, e -> Event.make(e.time, new MidiMsg(e.value))));
	}

	public static Vector<Event<MidiMsg>> getEvents(File file) {
		return(getEvents(MidiLib.getSequence(file)));
	}

	public static Vector<Event<MidiMsg>> getEvents(URL url) {
		return(getEvents(MidiLib.getSequence(url)));
	}

	public static C<MidiMsg> readMidi(File file) {
		return(C.make(Midi.getEvents(file)));
	}

	public static C<MidiMsg> readMidi(URL file) {
		// http://www.jsbach.net/midi/bwv29sin.mid
		return(C.make(Midi.getEvents(file)));
	}

	public static C<MidiMsg> readMidi(String filename) {
		return(C.make(Midi.getEvents(new File(filename))));
	}

	/*
	 * Keyboard
	 */
	public static C<MidiMsg> keyboard(int in) {
		return(keyboard(Midi.getIn(in)));
	}
	public static C<MidiMsg> keyboard(Transmitter trans) {
		return(new C<MidiMsg>() {
			boolean closed = false;
			private LinkedBlockingQueue<Event<MidiMsg>> pool = new LinkedBlockingQueue<Event<MidiMsg>>();

			{
				trans.setReceiver(new Receiver() {
					public void close() {
						System.out.println("Closing receiver");
						trans.close();
						closed = true;
					}
					public void send(MidiMessage message, long timeStamp) {
						try {
							System.out.println("transmit " + message);
							pool.put(Event.make(System.currentTimeMillis(), new MidiMsg(message)));
						} catch (Exception e) {
							System.out.println("cannot put the message " + message);
							close();
						}
					}
				});
			}
			public Event<MidiMsg> get() {
				try {
					return(pool.take());
				} catch (Exception e) {
					if(e instanceof InterruptedException) {
						// Thread is dying
						throw(new RuntimeException(e));
					}
					System.out.println("cannot takee the message " + closed + " " + e);
					closed = true;
					return(Event.make(-1, new MidiMsg(Midi.STOP)));
				}
			}
		});
	}

	public static void transmit(Transmitter trans, Proc1<MidiMessage> f) {
		trans.setReceiver(new Receiver() {
			public void close() {
				System.out.println("Closing receiver");
				trans.close();
			}
			public void send(MidiMessage message, long timeStamp) {
				//System.out.println("transmit send");
				f.operation(message);
			}
		});
	}

	/*
	 * Synthesizer
	 */
	public static void synthesize(C<MidiMsg> c, int out) {
		Receiver rcv = Midi.getOut(out);
		long delay = -1;
		try {
		while(true) {
			Event<MidiMsg> event = c.get();
			if(delay == -1) {
				delay = System.currentTimeMillis() - event.time;
			}
			long toSleep = event.time + delay - System.currentTimeMillis();
			//System.out.println(event.time + " + "  + delay + " - " + System.currentTimeMillis() + " = " + toSleep);
			if(toSleep > 10) {
				try {
					Thread.sleep(toSleep);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			//System.out.println("MIDI " + event);
			rcv.send(event.value.msg, -1);
		}
		} catch (EndOfStream e) {
		}
		rcv.close();
	}

	/*
	 * Message
	 */
	public static final MidiMessage STOP = stopMessage();

	static MidiMessage stopMessage() {
		ShortMessage r = new ShortMessage();
		try {
			r.setMessage(ShortMessage.STOP);
		} catch (Exception e) {
			throw(new RuntimeException(e));
		}
		return(r);
	}

	/*
	 * Test for writing a Midi file from Karl Brown
	 */
	public static void testwrite() {
		System.out.println("midifile begin ");
		try
		{
			//****  Create a new MIDI sequence with 24 ticks per beat  ****
			Sequence s = new Sequence(javax.sound.midi.Sequence.PPQ,24);

			//****  Obtain a MIDI track from the sequence  ****
			Track t = s.createTrack();

			//****  General MIDI sysex -- turn on General MIDI sound set  ****
			byte[] b = {(byte)0xF0, 0x7E, 0x7F, 0x09, 0x01, (byte)0xF7};
			SysexMessage sm = new SysexMessage();
			sm.setMessage(b, 6);
			MidiEvent me = new MidiEvent(sm,(long)0);
			t.add(me);

			//****  set tempo (meta event)  ****
			MetaMessage mt = new MetaMessage();
			byte[] bt = {0x02, (byte)0x00, 0x00};
			mt.setMessage(0x51 ,bt, 3);
			me = new MidiEvent(mt,(long)0);
			t.add(me);

			//****  set track name (meta event)  ****
			mt = new MetaMessage();
			String TrackName = new String("midifile track");
			mt.setMessage(0x03 ,TrackName.getBytes(), TrackName.length());
			me = new MidiEvent(mt,(long)0);
			t.add(me);

			//****  set omni on  ****
			ShortMessage mm = new ShortMessage();
			mm.setMessage(0xB0, 0x7D,0x00);
			me = new MidiEvent(mm,(long)0);
			t.add(me);

			//****  set poly on  ****
			mm = new ShortMessage();
			mm.setMessage(0xB0, 0x7F,0x00);
			me = new MidiEvent(mm,(long)0);
			t.add(me);

			//****  set instrument to Piano  ****
			mm = new ShortMessage();
			mm.setMessage(0xC0, 0x00, 0x00);
			me = new MidiEvent(mm,(long)0);
			t.add(me);

			//****  note on - middle C  ****
			mm = new ShortMessage();
			mm.setMessage(0x90,0x3C,0x60);
			me = new MidiEvent(mm,(long)1);
			t.add(me);

			//****  note off - middle C - 120 ticks later  ****
			mm = new ShortMessage();
			mm.setMessage(0x80,0x3C,0x40);
			me = new MidiEvent(mm,(long)121);
			t.add(me);

			//****  set end of track (meta event) 19 ticks later  ****
			mt = new MetaMessage();
			byte[] bet = {}; // empty array
			mt.setMessage(0x2F,bet,0);
			me = new MidiEvent(mt, (long)140);
			t.add(me);

			//****  write the MIDI sequence to a MIDI file  ****
			File f = new File("midifile.mid");
			MidiSystem.write(s,1,f);
		} //try
		catch(Exception e)
		{
			System.out.println("Exception caught " + e.toString());
		} //catch
		System.out.println("midifile end ");
	}


}
