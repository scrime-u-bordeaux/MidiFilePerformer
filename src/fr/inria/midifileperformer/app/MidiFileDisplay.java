package fr.inria.midifileperformer.app;

import java.io.File;
import java.util.Comparator;
import java.util.Vector;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Patch;
import javax.sound.midi.Sequence;
import javax.sound.midi.Track;

import fr.inria.lognet.sos.Shape;
import fr.inria.lognet.sos.Sos;
import fr.inria.lognet.sos.SosColor;
import fr.inria.lognet.sos.event.SelectEvent;
import fr.inria.lognet.sos.shape.Label;
import fr.inria.lognet.sos.shape.Slicer;
import fr.inria.lognet.sos.shape.Wrapper;
import fr.inria.midifileperformer.impl.Midi;
import fr.inria.midifileperformer.impl.MidiMsg;

public class MidiFileDisplay extends Wrapper {
	Label trackSize;
	Label trackTicks;
	Slicer trackMsgs;


	public static void main(String[] args) {
		if(args.length != 1) {
			System.out.println("need a midi file name");
			return;
		}
		String filename = args[0];
		MidiFileDisplay me = new MidiFileDisplay(filename);
		Sos.frame(Sos.border(20, me), 10, 10, "Midi File Player");
	}

	public MidiFileDisplay(String filename) {
		Sequence seq = Midi.getSequence(new File(filename));
		Patch[] patches = seq.getPatchList();
		Slicer seePatch = Sos.slicer(1, 5, patches, 0);
		Track[] tracks = seq.getTracks();
		Slicer seeTrack = Sos.slicer(1, 5, tracks, 0);
		trackSize = new Label(tracks.length == 0 ? "" : ""+tracks[0].size());
		trackTicks = new Label(tracks.length == 0 ? "" : ""+tracks[0].ticks());
		Vector<TrackMsg> tmsgs = (tracks.length == 0) ? new Vector<TrackMsg>() : getTrack(0, tracks[0]);
		trackMsgs = Sos.slicer(1, 25, tmsgs);
		Vector<TrackMsg> all = getEvents(seq);
		Slicer seeAll = Sos.slicer(1, 25, all);

		shape = Sos.column(10, new Shape[] {
				Sos.row(8, new Shape[] {
						Sos.button("Quit", (s -> System.exit(0))),
				}),
				Sos.row(8, new Shape[] {
						value("division type", seq.getDivisionType()),
						value("ticks per beat", seq.getResolution()),
				}),
				Sos.row(8, new Shape[] {
						value("duration in ticks", seq.getTickLength()),
						value("duration in s", (seq.getMicrosecondLength()/1000000.0)),
				}),
				Sos.row(8, new Shape[] {
						value("nb patches", patches.length),
						value("nb tracks", tracks.length),
				}),
				Sos.row(8, new Shape[] {
						Sos.namedshape("Tracks ", SosColor.red, 230,
								Sos.listen(SelectEvent.event, seeTrack, e -> seeTrack(e))),
						Sos.namedshape("Patches ", SosColor.red, 230,
								Sos.listen(SelectEvent.event, seePatch, e -> seePatch(e))),
				}),
				Sos.row(8, new Shape[] {
						labShape("nb events", trackSize),
						labShape("duration in ticks", trackTicks),
				}),
				Sos.row(8, new Shape[] {
						Sos.namedshape("Track messages ", SosColor.red, 400,
								Sos.listen(SelectEvent.event, trackMsgs, e -> seeAll(e))),
						Sos.namedshape("All messages ", SosColor.red, 400,
								Sos.listen(SelectEvent.event, seeAll, e -> seeAll(e))),
				}),
		});
	}

	public static Vector<TrackMsg> getEvents(Sequence sequence) {
		Vector<TrackMsg> r = new Vector<TrackMsg>();
		Track[] tracks = sequence.getTracks();
		int n = tracks.length;
		for(int i=0; i<n; i++) r.addAll(getTrack(i, tracks[i]));
		r.sort(TrackMsg.comparator);
		return(r);
	}


	static Vector<TrackMsg> getTrack(int nbtrack, Track track) {
		Vector<TrackMsg> r = new Vector<TrackMsg>();
		int n = track.size();
		for(int i=0; i<n; i++) {
			MidiEvent event = track.get(i);
			r.add(new TrackMsg(nbtrack, event.getTick(), event.getMessage()));
		}
		return(r);
	}
	void seeAll(fr.inria.lognet.sos.Event e) {
		//SelectEvent event = ((SelectEvent) e);
		//M1 o = (M1) event.obj;
	}

	void seePatch(fr.inria.lognet.sos.Event e) {
		//SelectEvent event = ((SelectEvent) e);
		//M1 o = (M1) event.obj;
	}

	void seeTrack(fr.inria.lognet.sos.Event e) {
		SelectEvent event = ((SelectEvent) e);
		int nbtrack = event.x;
		Track track = (Track) event.obj;
		seeTrack(nbtrack, track);
	}

	void seeTrack(int nbtrack, Track track) {
		trackSize.reset(""+track.size());
		trackTicks.reset(""+track.ticks());
		trackMsgs.reset(getTrack(nbtrack, track));

	}

	Shape labShape(String name, Shape s) {
		return(Sos.row(4, new Shape[] {
				Sos.label(name),
				Sos.pencolor(SosColor.blue, s),
		}));
	}

	Shape value(String name, Object value) {
		return(labShape(name, Sos.label(""+value)));
	}

}

class TrackMsg {
	int nbtrack;
	long tick;
	MidiMsg msg;
	public TrackMsg(int nbtrack, long tick, MidiMessage msg) {
		this.nbtrack = nbtrack;
		this.tick = tick;
		this.msg = new MidiMsg(msg);
	}
	public static Comparator<TrackMsg> comparator = new Comparator<TrackMsg>() {
		public int compare(TrackMsg ev1, TrackMsg ev2) {
			return(Long.compare(ev1.tick, ev2.tick));
		}
	};
	public String toString() {
		return(nbtrack+" "+tick+" "+msg);
	}

}
