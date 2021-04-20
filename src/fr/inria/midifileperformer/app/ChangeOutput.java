package fr.inria.midifileperformer.app;

import java.util.Vector;

import fr.inria.lognet.sos.Shape;
import fr.inria.lognet.sos.Sos;
import fr.inria.lognet.sos.SosColor;
import fr.inria.lognet.sos.event.SelectEvent;
import fr.inria.lognet.sos.shape.Slicer;
import fr.inria.midifileperformer.impl.OutputDevice;

public class ChangeOutput extends ChangeConfig {
	Slicer selectable;
	Slicer selected;
	Vector<OutputDevice> addable;
	Vector<OutputDevice> removable;

	public ChangeOutput(MetaPlayer master) {
		super(master);
		removable = new Vector<OutputDevice>(config.outputs);
		addable = others(removable);
		selectable = Sos.slicer(1, 5, addable, -1);
		selected = Sos.slicer(1, 5, removable, -1);
		shape = Sos.column(10, new Shape[] {
				header,
				Sos.row(4, new Shape[] {
						Sos.namedshape("Output devices available", SosColor.red, 230,
								Sos.listen(SelectEvent.event, selectable, e -> {})),
						Sos.namedshape("Selected output devices", SosColor.red, 230,
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
		OutputDevice selection = (OutputDevice) selectable.selected();
		if(selection != null) {
			addable.remove(selection);
			removable.add(selection);
			reset();
		}
	}

	void remove() {
		OutputDevice selection = (OutputDevice) selected.selected();
		if(selection != null) {
			removable.remove(selection);
			addable.add(selection);
			reset();
		}
	}

	void changeConfig() {
		config.outputs = removable;
	}

	Vector<OutputDevice> others(Vector<OutputDevice> done) {
		Vector<OutputDevice> r = new Vector<OutputDevice>();
		for( OutputDevice dev : OutputDevice.all() ) {
			if(!done.contains(dev)) r.add(dev);		
		}
		return(r);
	}
}

