package fr.inria.midifileperformer.app;

import java.util.Vector;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;

import fr.inria.lognet.sos.Shape;
import fr.inria.lognet.sos.Sos;
import fr.inria.lognet.sos.SosColor;
import fr.inria.lognet.sos.event.SelectEvent;
import fr.inria.lognet.sos.shape.Slicer;

public class ChangeInput extends ChangeConfig {
	Slicer selectable;
	Slicer selected;
	Vector<InputDevice> addable;
	Vector<InputDevice> removable;

	public ChangeInput(MetaPlayer master) {
		super(master);
		removable = new Vector<InputDevice>(config.inputs);
		addable = others(removable, master);
		selectable = Sos.slicer(1, 5, addable, -1);
		selected = Sos.slicer(1, 5, removable, -1);
		shape = Sos.column(10, new Shape[] {
				header,
				Sos.row(4, new Shape[] {
						Sos.namedshape("Input devices available", SosColor.red, 230,
								Sos.listen(SelectEvent.event, selectable, e -> {})),
						Sos.namedshape("Selected input devices", SosColor.red, 230,
								Sos.listen(SelectEvent.event, selected, e -> {})),
				}),
				Sos.row(4, new Shape[] {
						Sos.button("Add in selection", (s -> add())),
						Sos.button("remove in selection", (s -> remove())),
				}),
		});
	}

	void reset() {
		selectable.reset(addable);
		selected.reset(removable);
	}

	void add() {
		InputDevice selection = (InputDevice) selectable.selected();
		if(selection != null) {
			addable.remove(selection);
			removable.add(selection);
			reset();
		}
	}

	void remove() {
		InputDevice selection = (InputDevice) selected.selected();
		if(selection != null) {
			removable.remove(selection);
			addable.add(selection);
			reset();
		}
	}

	void changeConfig() {
		config.inputs = removable;
	}

	static Vector<InputDevice> others(Vector<InputDevice> done, MetaPlayer master) {
		Vector<InputDevice> r = new Vector<InputDevice>();
		for(InputDevice dev : master.standardInputDevice()) {
			if(!done.contains(dev)) r.add(dev);
		}
		MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
		for(int i=0; i<infos.length; i++) {
			try {
				MidiDevice md = MidiSystem.getMidiDevice(infos[i]);
				if(md.getMaxTransmitters() != 0) {
					InputDevice dev = new MidiInputDevice(md);
					if(!done.contains(dev)) r.add(dev);
				}
			} catch(Exception e) {}
		}
		return(r);
	}

	public static InputDevice byName(String name) {
		MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
		for(int i=0; i<infos.length; i++) {
			try {
				MidiDevice md = MidiSystem.getMidiDevice(infos[i]);
				if(md.getMaxTransmitters() != 0) {
					if(md.getDeviceInfo().getName().equals(name))
						return(new MidiInputDevice(md));
				}
			} catch(Exception e) {}
		}
		return(null);
	}
}

