package fr.inria.midifileperformer.app;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.util.Vector;

import fr.inria.bps.base.Event;
import fr.inria.lognet.sos.Shape;
import fr.inria.lognet.sos.Sos;
import fr.inria.lognet.sos.shape.Label;
import fr.inria.lognet.sos.shape.Wrapper;
import fr.inria.midifileperformer.core.Consumer;
import fr.inria.midifileperformer.impl.Config;
import fr.inria.midifileperformer.impl.MidiInputDevice;
import fr.inria.midifileperformer.impl.MidiMsg;
import fr.inria.midifileperformer.impl.MidiOutputDevice;
import fr.inria.midifileperformer.impl.MidiRendering;

public class Configurator extends Wrapper {
	Config config;
	Label console;
	
	Label file = Sos.label("filetoplay           ");
	Label input = Sos.label("input               ");
	Label output = Sos.label("output             ");
	Label start = Sos.label("     ");
	Label step = Sos.label ("     ");
	Label stop = Sos.label ("     ");
	
	public Configurator(Label console) {
		this.console = console;
	}

	static String[] inDir() {
		String[] files = new File(".").list(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return(name.endsWith(".mid") || name.endsWith(".midi"));
			}
		});
		return(files);
	}

	static Vector<String> toVector(String[] a) {
		int n = a.length;
		Vector<String> r = new Vector<String>(n);
		for(int i=0; i<n; i++) r.add(a[i]);
		return(r);
	}
	

	void makeConfig() {
		config = new Config();
		MidiInputDevice.reset();
		MidiOutputDevice.reset();
		restoreConfig();
	}

	void saveConfig() {
		try {
			config.saveConfig();
		} catch (Exception e) {
			console.reset("Error when saving config " + e);
		}
	}

	void restoreConfig() {
		try {
			config.restoreConfig(false);
		} catch (FileNotFoundException e) {
			config.addInput("Keyboard", false);
		} catch (Exception e) {
			console.reset("Error when restoring config " + e);
		}
		configChanged();
	}
	
	void changeInput() {
		int n = config.inputs.size();
		input.reset(n == 0 ? "no input" :
			n == 1 ? ""+config.inputs.get(0) :
				n == 2 ? config.inputs.get(0) + "," + config.inputs.get(1) :
					n + " inputs selected" );
		input.dirty();
	}
	
	void changeOutput() {
		int n = config.outputs.size();
		output.reset(n == 0 ? "no output" :
			n == 1 ? ""+config.outputs.get(0) :
				n == 2 ? config.outputs.get(0) + "," + config.outputs.get(1) :
					n + " outputs selected" );
		output.dirty();
	}
	
	void changeFilename(File f) {
		file.reset(f.getName());
	}
	
	void changeStartTime(long time) {
		config.start = time;
		start.reset(""+time);
	}
	
	void changeStopTime(long time) {
		config.stop = time;
		stop.reset(""+time);
	}

	void showTime(long time) {
		//System.out.println("SHOWTIME");
		step.reset(""+time);
		// this repaint doesn't return when input == PlayerZone && ouput != Midi
		//picture.root.FullRepaint();
		//System.out.println("SHOWED");
	}

	public void configChanged() {
		if(config.player != null) {
			config.player.interrupt();
			config.player = null;
		}
		changeInput();
		changeOutput();
		changeFilename(config.filename);
		changeStartTime(config.start);
		config.tempo = config.initTempo;
		MidiRendering.launch(config, t -> showTime(t));
	}

	Vector<Event<MidiMsg>> src() {
		/*
		C<MidiMsg> cin = MidiRendering.readAndFilter(config, config.filename);
		Record<MidiMsg> rin = new Record<MidiMsg>(cin);
		rin.force();
		return(rin.recorded);
		*/
		return(MidiRendering.readv(config.filename.toString()));
	}
	
	Vector<Event<MidiMsg>> record() {
		Vector<Event<MidiMsg>> src = config.record.recorded;
		int n = src.size();
		System.out.println("record of "+n);
		Vector<Event<MidiMsg>> r = new Vector<Event<MidiMsg>>(n);
		if(n == 0) return(r);
		long t0 = src.get(0).time;
		for(Event<MidiMsg> e : src)
			r.add(Event.make(e.time-t0, e.value));
		return(r);
	}

	void popup(Shape s, String header) {
		Sos.frame(Sos.border(20, s), 100, 20, header);
	}

	public void msg(String msg) {
		console.reset(msg);
	}

	void expert() {
		Expert a = new Expert(this, config);
		Sos.frame(Sos.border(20, a), 100, 20, "Advanced options");
	}

	void panic() {
		Vector<MidiMsg> v = MidiMsg.allNotesOff;
		int n = v.size();
		for(int i=0; i<n; i++) {
			MidiMsg msg = v.get(i);
			for(Consumer<Event<MidiMsg>> cons : config.outputs)
				cons.accept(Event.make(System.currentTimeMillis(), msg));
		}
	}
}
