package fr.inria.midifileperformer.app;

import java.util.Vector;

import fr.inria.bps.base.Vecteur;
import fr.inria.lognet.sos.Picture;
import fr.inria.lognet.sos.Shape;
import fr.inria.lognet.sos.Sos;
import fr.inria.lognet.sos.SosColor;
import fr.inria.lognet.sos.shape.Slicer;
import fr.inria.midifileperformer.core.C;
import fr.inria.midifileperformer.core.Event;
import fr.inria.midifileperformer.core.Record;
import fr.inria.midifileperformer.impl.Config;
import fr.inria.midifileperformer.impl.MidiMsg;
import fr.inria.midifileperformer.impl.MidiRendering;

public class ChangeStart extends ChangeConfig {
	Vector<Event<MidiMsg>> vmidi;
	Slicer midis;

	public ChangeStart(MetaPlayer master) {
		super(master);
		vmidi = getSteps(config);
		midis = Sos.slicer(1, 20, vmidi, 0);
		shape = Sos.column(10, new Shape[] {
				header,
				Sos.namedshape("Midi Event", SosColor.red, 230, midis),
		});
	}

	public void init(Picture p, Shape father) {
		super.init(p, father);
		midis.reselect(find1(vmidi, config.start));

	}

	void changeConfig() {
		Event<?> o = (Event<?>) midis.selected();
		config.start = o.time;
	}

	int find1(Vector<Event<MidiMsg>> v, long time) {
		return(Vecteur.dicoSearch(v, (x -> (double) x.time), time));
	}

	Vector<Event<MidiMsg>> getSteps(Config config) {
		C<MidiMsg> cin = MidiRendering.readfile(config.filename).filter(e -> e.value.isBegin());
		Record<MidiMsg> rin = new Record<MidiMsg>(cin);
		rin.force();
		return(rin.recorded);
	}
}
