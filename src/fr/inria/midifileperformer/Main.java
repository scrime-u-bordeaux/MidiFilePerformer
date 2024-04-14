package fr.inria.midifileperformer;

import java.io.File;
import java.util.Vector;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Track;

import fr.inria.bps.base.Event;
import fr.inria.bps.base.Mains;
import fr.inria.midi.MidiLib;
import fr.inria.midifileperformer.app.MainApp;
import fr.inria.midifileperformer.core.C;
import fr.inria.midifileperformer.core.EndOfStream;
import fr.inria.midifileperformer.core.Rendering;
import fr.inria.midifileperformer.impl.Config;
import fr.inria.midifileperformer.impl.Midi;
import fr.inria.midifileperformer.impl.MidiMsg;
import fr.inria.midifileperformer.impl.SimpleMidiFile;
import fr.inria.midifileperformer.impl.StringInterval;

public class Main {
	public static void main(String[] args) {
		if(args.length == 0) {
			MainApp.launch();
		} else {
			Main me = new Main();
			Mains.launch(me, args, 0);
		}
	}
	
	/*
	 * Entry points
	 */
	public void addDir(String dir) {
		File file = new File(dir);
		Config config = new Config();
		try {
			if(file.isDirectory()) {
				config.restoreConfig(true);
				config.directories.add(new File(dir));
				config.saveConfig();
			} else {
				System.out.println(dir + " is not a directory");
			}
		} catch(Exception e) {
			System.out.println("Error " + e);
		}
	}
	
	/*
	 * Export
	 */
	public static void tt() {
		export("abc.mid", abc);
	}
	public static void export(String file, String[] notes) {
		Vector<Event<MidiMsg>> v = SimpleMidiFile.convert(abc);
		try {
			Sequence s = new Sequence(javax.sound.midi.Sequence.PPQ, 500);
			int n = v.size();
			Track t = s.createTrack();
			for(int i=0; i<n; i++) {
				Event<MidiMsg> event = v.get(i);
				MidiEvent ee = new MidiEvent(event.value.msg, (event.time));
				t.add(ee);
			}
			File f = new File(file);
			MidiSystem.write(s, 1, f);
		} catch (Exception e) {
			System.out.println("cannot save : " + e);
		}
	}
	
	
	static String[] abc = new String[] {
			"0 La3-", "100 La3+", "100 Si3-", "200 Si3+", "200 La3-", "300 La3+"
	};

	/*
	 * Example
	 */

	static Vector<String> dataString = new Vector<String>();
	static {
		dataString.add("da");
		dataString.add("re");
		dataString.add("sofl");
	}
	static void stepString(int ms) {
		C<String> c1 = C.make(dataString, 50);
		C<Void> c2 = C.clock(ms, null);
		C<String> r = Lib.trace(Rendering.stepBy(c1, c2));
		r.force();
	}

	static void stepMidi(String filename, int ms, int out) {
		C<MidiMsg> c1 = Midi.readMidi(filename);
		C<Void> c2 = C.clock(ms, null);
		Midi.synthesize(Rendering.stepBy(c1, c2), out);
	}

	/*
	 * beginEnd
	 */   
	static void beginEnd(String filename, int in, int out) {

		System.out.println("Need to be refixed");
		/*
		C<MidiMsg> cin = Midi.readMidi(filename);
		C<Vector<MidiMsg>> c1 = Lib.stdAnalysis(cin, true);

		C<MidiMsg> c2 = Midi.keyboard(in);

		C<Vector<MidiMsg>> r = Rendering.mergeBegin(c1, c2, MidiMsg.merge);

		Midi.synthesize(C.unfold(r), out);
		*/
		System.exit(0);
	}

	static void schedule_sev(String filename, int in) {
		C<Vector<StringInterval>> c1 = StringInterval.read(filename);
		C<MidiMsg> c2 = Midi.keyboard(in);
		Lib.trace(Rendering.mergeBegin(c1, c2, StringInterval.mergeMidi)).force();
		//System.exit(0);
	}

	static void midiInOut(int in, int out) {
		Midi.synthesize(Lib.trace(Midi.keyboard(in)), out);
	}

	static void convertSimple(String filename) {
		C<MidiMsg> c = Midi.readMidi(filename);
		try {
			while(true) {
				Event<MidiMsg> e = c.get();
				MidiMessage msg = e.value.msg;
				if(MidiLib.isBegin(msg)) {
					System.out.println(e.time + " " + MidiLib.note(msg) + "-");
				} else if(MidiLib.isEnd(msg)) {
					System.out.println(e.time + " " + MidiLib.note(msg) + "+");
				}
			}
		} catch (EndOfStream e) {
		}
	}

}
