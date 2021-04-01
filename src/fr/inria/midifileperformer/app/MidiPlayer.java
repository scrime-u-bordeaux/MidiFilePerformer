package fr.inria.midifileperformer.app;

import java.io.File;
import java.util.Vector;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Track;

import fr.inria.lognet.sos.Shape;
import fr.inria.lognet.sos.Sos;
import fr.inria.lognet.sos.shape.Wrapper;
import fr.inria.midifileperformer.MidiMsg;
import fr.inria.midifileperformer.core.C;
import fr.inria.midifileperformer.core.Event;

public class MidiPlayer extends Wrapper {
    MetaPlayer master;
    Thread player = null;

    public MidiPlayer(MetaPlayer master, Vector<Event<MidiMsg>> src, Vector<Event<MidiMsg>> record) {
	this.master = master;
	shape = Sos.column(10, new Shape[] {
		Sos.row(4, new Shape[] {
			Sos.button("Quit", (s -> picture.root.quit())),
			Sos.button("Save record", (s -> save(record))),
			Sos.button("Play src", (s -> play(src))),
			Sos.button("Play record", (s -> play(record))),
			Sos.button("Stop", (s -> stop())),
		}),
	});
    }

    public void msg(String msg) {
	master.console.reset(msg);
    }

    void play(Vector<Event<MidiMsg>> v) {
	player = MetaPlayer.launchPlayer(master.config.outputs, C.make(v));
    }

    void stop() {
	if(player != null) {
	    player.interrupt();
	    player = null;
	}
    }

    void save(Vector<Event<MidiMsg>> v) {
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
	    File f = new File("midifile.mid");
	    MidiSystem.write(s,1,f);
	    msg("recorded file saved in midifile.mid");
	} catch (Exception e) {
	    msg("cannot save : " + e);
	}
    }
}
