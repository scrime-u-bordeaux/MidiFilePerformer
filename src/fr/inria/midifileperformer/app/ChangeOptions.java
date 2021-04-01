package fr.inria.midifileperformer.app;

import fr.inria.lognet.sos.Shape;
import fr.inria.lognet.sos.Sos;
import fr.inria.lognet.sos.shape.CheckBox;

public class ChangeOptions extends ChangeConfig {
	CheckBox unmeet;
	CheckBox loop;
	CheckBox keepTempo;
	CheckBox keepPedal;

	public ChangeOptions(MetaPlayer master) {
		super(master);
		unmeet = Sos.checkbox(config.unmeet, Sos.contour(1,Sos.box(9, 9)), e -> {});
		loop = Sos.checkbox(config.loop, Sos.contour(1,Sos.box(9, 9)), e -> {});
		keepTempo = Sos.checkbox(config.keepTempo, Sos.contour(1,Sos.box(9, 9)), e -> {});
		keepPedal = Sos.checkbox(config.keepPedal, Sos.contour(1,Sos.box(9, 9)), e -> {});
		shape = Sos.column(10, new Shape[] {
				header,
				Sos.row(8, new Shape[]{
						Sos.label("Do the unmeet transformation"),
						Sos.space(),
						Sos.wscaler(1, unmeet),
				}),
				Sos.row(8, new Shape[]{
						Sos.label("Loop at the end of performance"),
						Sos.space(),
						Sos.wscaler(1, loop),
				}),
				Sos.row(8, new Shape[]{
						Sos.label("keep set SetTempo messages in the output"),
						Sos.space(),
						Sos.wscaler(1, keepTempo),
				}),
				Sos.row(8, new Shape[]{
						Sos.label("keep foot pedal messages in the input"),
						Sos.space(),
						Sos.wscaler(1, keepPedal),
				}),
		});
	}

	void changeConfig() {
		config.unmeet = unmeet.state;
		config.loop = loop.state;
		config.keepTempo = keepTempo.state;
		config.keepPedal = keepPedal.state;
	}
}

