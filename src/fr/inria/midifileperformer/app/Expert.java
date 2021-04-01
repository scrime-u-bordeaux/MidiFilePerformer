package fr.inria.midifileperformer.app;

import java.util.Vector;

import fr.inria.bps.base.Pair;
import fr.inria.bps.base.Vecteur;
import fr.inria.lognet.sos.Shape;
import fr.inria.lognet.sos.Sos;
import fr.inria.lognet.sos.SosColor;
import fr.inria.lognet.sos.event.SelectEvent;
import fr.inria.lognet.sos.shape.Label;
import fr.inria.lognet.sos.shape.Slicer;
import fr.inria.lognet.sos.shape.Wrapper;
import fr.inria.midifileperformer.Lib;
import fr.inria.midifileperformer.Midi;
import fr.inria.midifileperformer.MidiMsg;
import fr.inria.midifileperformer.core.C;
import fr.inria.midifileperformer.core.Event;
import fr.inria.midifileperformer.core.Record;

public class Expert extends Wrapper {
    MetaPlayer master;
    //PlayerConfig config;
    Vector<MidiMsg> midiEvents;
    Vector<Vector<MidiMsg>> stepEvents;
    Slicer midis;
    Slicer steps;
    Slicer step;
    Vector<M1> vmidi;
    Vector<M2> vstep;
    Label startLabel;
    Label stopLabel;

    public Expert(MetaPlayer master, PlayerConfig config) {
	this.master = master;
	//this.config = config.copy();
	Pair<Vector<M1>,Vector<M2>> objs = getSteps(config);
	vmidi = objs.car;
	vstep = objs.cdr;
	midis = Sos.slicer(1, 20, objs.car, 0);
	steps = Sos.slicer(1, 20, objs.cdr, 0);
	step = Sos.slicer(1, 20, new Vector<M1>(), 0);
	startLabel = Sos.label(""+config.start);
	stopLabel = Sos.label(""+config.stop);
	shape = Sos.column(10, new Shape[] {
		Sos.row(8, new Shape[] {
			Sos.button("Accept", (s -> accept())),
			Sos.button("Abort", (s -> abort())),
		}),
		Sos.row(8, new Shape[]{
			Sos.namedshape("Midi Event", SosColor.red, 230,
				Sos.listen(SelectEvent.event, midis, e -> getMidi(e))),
			Sos.namedshape("Steps ", SosColor.red, 230,
				Sos.listen(SelectEvent.event, steps, e -> getStep(e))),
			Sos.namedshape("Step ", SosColor.red, 230,
				Sos.listen(SelectEvent.event, step, e -> getInsideStep(e))),
		}),
		Sos.row(8, new Shape[] {
			Sos.label("starting step"),
			Sos.pencolor(SosColor.blue, startLabel),
			Sos.wscaler(100, Sos.space()),
			Sos.button("Get it from steps", (s -> startStep())),
		}),
		Sos.row(8, new Shape[] {
			Sos.label("stoppint step"),
			Sos.pencolor(SosColor.blue, stopLabel),
			Sos.wscaler(100, Sos.space()),
			Sos.button("Get it from steps", (s -> stopStep())),
		}),
	});
    }

    Pair<Vector<M1>,Vector<M2>> getSteps(PlayerConfig config) {
	C<MidiMsg> cin = Midi.readMidi(config.filename);
	Record<MidiMsg> rin = new Record<MidiMsg>(cin);
	C<Vector<MidiMsg>> partition = Lib.stdAnalysis(rin, config.unmeet);
	Record<Vector<MidiMsg>> rp = new Record<Vector<MidiMsg>>(partition);
	rp.force();
	return(Pair.cons(cvt1(rin.recorded), cvt2(rp.recorded)));
    }

    Vector<M1> cvt1(Vector<Event<MidiMsg>> v) {
	Vector<M1> r = new Vector<M1>(v.size());
	for(Event<MidiMsg> e : v) r.add(new M1(e));
	return(r);
    }

    Vector<M2> cvt2(Vector<Event<Vector<MidiMsg>>> v) {
	Vector<M2> r = new Vector<M2>(v.size());
	for(Event<Vector<MidiMsg>> e : v) r.add(new M2(e));
	return(r);
    }

    public void accept() {
	abort();
    }

    public void abort() {
	picture.root.quit();
    }

    void startStep() {
	M2 o = (M2) steps.selected();
	startLabel.reset(""+o.e.time);
    }

    void stopStep() {
	M2 o = (M2) steps.selected();
	stopLabel.reset(""+o.e.time);
    }

    void getMidi(fr.inria.lognet.sos.Event e) {
	SelectEvent event = ((SelectEvent) e);
	M1 o = (M1) event.obj;
	steps.reselect(find2(vstep, o.e.time));
    }

    void getStep(fr.inria.lognet.sos.Event e) {
	SelectEvent event = ((SelectEvent) e);
	M2 o = (M2) event.obj;
	midis.reselect(find1(vmidi, o.e.time));
	step.reset(o.convert());
    }

    void getInsideStep(fr.inria.lognet.sos.Event e) {
    }

    int find1(Vector<M1> v, long time) {
	return(Vecteur.dicoSearch(v, (x -> (double) x.e.time), time));
    }

    int find2(Vector<M2> v, long time) {
	return(Vecteur.dicoSearch(v, (x -> (double) x.e.time), time));
    }
}

class M1 {
    Event<MidiMsg> e;
    public M1(Event<MidiMsg> e) {
	this.e = e;
    }
    public String toString() {
	return(e.toString());
    }
}

class M2 {
    Event<Vector<MidiMsg>> e;
    public M2(Event<Vector<MidiMsg>> e) {
	this.e = e;
    }
    public Vector<M1> convert() {
	long time = e.time;
	Vector<MidiMsg> v = e.value;
	int n = v.size();
	Vector<M1> r = new Vector<M1>(n);
	for( MidiMsg m : v ) r.add(new M1(Event.make(time, m)));
	return(r);

    }
    public String toString() {
	long time = e.time;
	Vector<MidiMsg> v = e.value;
	String vals = "";
	int n = v.size();
	if(n > 0) {
	    vals += v.get(0);
	    if(n > 1) {
		vals += " " + v.get(1);
		if(n > 2) {
		    vals += "...";
		}
	    }
	}
	return(time+":"+vals);
    }
}
