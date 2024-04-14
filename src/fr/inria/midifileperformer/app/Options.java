package fr.inria.midifileperformer.app;

import java.io.File;
import java.util.Vector;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Track;

import fr.inria.bps.base.Event;
import fr.inria.fun.Fun0;
import fr.inria.fun.Proc1;
import fr.inria.lognet.edit.LineEditor;
import fr.inria.lognet.sos.Shape;
import fr.inria.lognet.sos.Sos;
import fr.inria.lognet.sos.SosColor;
import fr.inria.lognet.sos.shape.CheckBox;
import fr.inria.lognet.sos.shape.Wrapper;
import fr.inria.midifileperformer.impl.Config;
import fr.inria.midifileperformer.impl.InputDevice;
import fr.inria.midifileperformer.impl.MidiMsg;
import fr.inria.midifileperformer.impl.OutputDevice;

public class Options extends Wrapper {
	Configurator master;
	CheckBox unmeet;
	CheckBox loop;
	CheckBox keepTempo;
	CheckBox keepPedal;
	LineEditor edit;
	
	public Options(Configurator master) {
		this.master = master;
		Vector<Shape> options = generalOptions();
		Vector<Shape> analysis = analysisOptions();
		edit = Sos.editor(80, "midifile.mid", s->save());
		shape = Sos.column(4, new Shape[] {
				Sos.label("In/Out devices", SosColor.blue),
				Sos.row(20, Sos.box(10, 10), inOut(master.config)),
				Sos.row(10, 
						Sos.column(4, 
								Sos.label("General options", SosColor.blue),
								Sos.row(20, Sos.box(10, 10), Sos.column(4, options)) ),
						Sos.column(4, 
								Sos.label("analysis options", SosColor.blue),
								Sos.row(20, Sos.box(10, 10), Sos.column(4, analysis)) )),
				Sos.label("Save played events", SosColor.blue),
				Sos.row(3, new Shape[] {
						Sos.box(30, 10),
						Sos.button("Save", x->save()),
						Sos.label("in", SosColor.blue),
						Sos.contour(1, edit),
						Sos.eol(),
				}),
				Sos.eoc(),
				Sos.row(4, new Shape[] {
						Sos.button("Save Config", (s -> master.saveConfig())),
						Sos.button("Restore Config", (s -> master.restoreConfig())),
						Sos.button("Panic", (s -> master.panic())),
				}),
				Sos.row(4,  new Shape[] {
						Sos.button("Expert", (s -> master.expert()))
				}),
		});
		
	}
	
	Vector<Shape> generalOptions() {
		Vector<Shape> r = new Vector<Shape>(8);
		Config c = master.config;
		r.add(makeSlice("keyboard is qwerty", 
				() -> c.qwerty, b -> {c.qwerty=b;}) );
		r.add(makeSlice("Merge synchronous events", 
				() -> c.doSync, b -> {c.doSync=b;}) );
		r.add(makeSlice("Do the unmeet transformation", 
				() -> c.unmeet, b -> {c.unmeet=b;}) );
		r.add(makeSlice("Detach sub-partitions", 
				() -> c.detachPart, b -> {c.detachPart=b;}) );
		r.add(makeSlice("Loop at the end of performance", 
				() -> c.loop, b -> {c.loop=b;}) );
		r.add(makeSlice("Keep set SetTempo messages in the output", 
				() -> c.keepTempo, b -> {c.keepTempo=b;}) );
		r.add(makeSlice("Set Tempo by User", 
				() -> c.setTempoByOn, b -> {c.setTempoByOn=b;}) );
		LineEditor initTempo = Sos.editor(10,  ""+c.initTempo, s->{c.initTempo = Double.parseDouble(s);});
		r.add(Sos.row(3, Sos.label("Initial tempo", SosColor.blue), Sos.contour(1, initTempo)));
		LineEditor incTempo = Sos.editor(10,  ""+c.maxIncreaseTempo, s->{c.maxIncreaseTempo = Double.parseDouble(s);});
		r.add(Sos.row(3, Sos.label("max increase tempo", SosColor.blue), Sos.contour(1, incTempo)));
		r.add(makeSlice("Keep foot pedal messages from the midifile", 
				() -> c.keepPedal, b -> {c.keepPedal=b;}) );
		r.add(makeSlice("Filter adjacent key input", 
				() -> c.adjacentKeyFilter, b -> {c.adjacentKeyFilter=b;}) );
		r.add(makeSlice("Show channels in display", 
				() -> c.showChannel, b -> {c.showChannel=b;}) );
		LineEditor delay = Sos.editor(10, ""+c.max_delay, s->{c.max_delay = Integer.parseInt(s);});
		r.add(Sos.row(3, Sos.label("Max delay to share Ons", SosColor.blue), Sos.contour(1, delay)));
		r.add(Sos.eoc());
		return(r);
	}
	
	Vector<Shape> analysisOptions() {
		Vector<Shape> r = new Vector<Shape>(8);
		Config c = master.config;
		r.add(makeSlice("old analysis", () -> c.analysis_sync_off, b -> {c.analysis_sync_off=b;}));
		//r.add(makeSlice("std analysis", () -> c.analysis_unsync_off, b -> {c.analysis_unsync_off=b;}));
		LineEditor time = Sos.editor(10, ""+c.slice_size, s->{c.slice_size = Integer.parseInt(s);});
		LineEditor stime = Sos.editor(10, ""+c.slice_start, s->{c.slice_start = Integer.parseInt(s);});
		r.add(Sos.row(3, 
				makeSlice("slice analysis", () -> c.analysis_slice, b -> {c.analysis_slice=b;}),
				Sos.column(2, new Shape[] {
						makeSlice("Strict analysis",() -> c.slice_strict, b -> {c.slice_strict=b;}),
						Sos.row(3, Sos.label("size slice", SosColor.blue), Sos.contour(1, time)),
						Sos.row(3, Sos.label("time start", SosColor.blue), Sos.contour(1, stime)) })));
		LineEditor nbOn = Sos.editor(10, ""+c.nb_on, s->{c.nb_on = Integer.parseInt(s);});
		r.add(Sos.row(3, 
				makeSlice("count on analysis", () -> c.analysis_count_on, b -> {c.analysis_count_on=b;}),
				Sos.label("nb on", SosColor.blue),
				Sos.contour(1, nbOn) ));
		LineEditor chan = Sos.editor(10, ""+c.selected_channel, s->{c.selected_channel = Integer.parseInt(s);});
		r.add(Sos.row(3, 
				makeSlice("channel analysis", () -> c.analysis_channel, b -> {c.analysis_channel=b;}),
				Sos.label("channel", SosColor.blue),
				Sos.contour(1, chan) ));
		r.add(Sos.eoc());
		return(r);
	}
	
	Shape makeSlice(String lab, Fun0<Boolean> get, Proc1<Boolean> set) {
		CheckBox b = Sos.checkbox(get.operation(), Sos.contour(1,Sos.box(9, 9)), 
				() -> set.operation(true),
				() -> set.operation(false));
		return(Sos.row(2, new Shape[] {
				b,
				Sos.label("  " + lab),
				Sos.eol()
		}));
	}
	
	Shape inOut(Config config) {
		return(Sos.row(10, selectIns(config), selectOuts(config), Sos.eol()));
	}

	Shape selectIns(Config config) {
		return(new SelectIO<InputDevice>(InputDevice.devices, config.inputs) {
			public void add(String name) {
				config.addInput(name, false);
				master.changeInput();
			}
			public void remove(String name) {
				config.removeInput(name);
				master.changeInput();
			}
		});
	}

	Shape selectOuts(Config config) {
		return(new SelectIO<OutputDevice>(OutputDevice.devices, config.outputs) {
			public void add(String name) {
				config.addOutput(name, false);
				master.changeOutput();
			}
			public void remove(String name) {
				config.removeOutput(name);
				master.changeOutput();
			}
		});
	}

	void save() {
		Vector<Event<MidiMsg>> v = master.record();
		String file = edit.currentLine();
		long t0 = v.get(0).time;
		try {
			Sequence s = new Sequence(javax.sound.midi.Sequence.PPQ, 500);
			int n = v.size();
			Track t = s.createTrack();
			for(int i=0; i<n; i++) {
				Event<MidiMsg> event = v.get(i);
				MidiEvent ee = new MidiEvent(event.value.msg, (event.time-t0));
				t.add(ee);
			}
			File f = new File(file);
			MidiSystem.write(s, 1, f);
			master.msg("recorded file saved in " + file);
		} catch (Exception e) {
			master.msg("cannot save : " + e);
		}
	}

}
