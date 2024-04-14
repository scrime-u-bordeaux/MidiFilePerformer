package fr.inria.midifileperformer.app;

import java.util.Hashtable;
import java.util.Vector;

import fr.inria.lognet.sos.Shape;
import fr.inria.lognet.sos.Sos;
import fr.inria.lognet.sos.shape.CheckBox;
import fr.inria.lognet.sos.shape.Wrapper;

public class SelectIO<T> extends Wrapper {
	public SelectIO(Hashtable<String,T> devices, Vector<T> selected) {
		Vector<String> names = new Vector<String>(devices.keySet());
		int n = names.size();
		Vector<Shape> slices = new Vector<Shape>(n);
		for(int i=0; i<n; i++) {
			String name = names.get(i);
			slices.add(makeSlice(name, devices.get(name), selected));
		}
		slices.add(Sos.eoc());
		shape = Sos.column(2, slices);
	}
	
	Shape makeSlice(String name, T dev, Vector<T> selected) {
		boolean on = selected.contains(dev);
		CheckBox b = Sos.checkbox(on, Sos.contour(1,Sos.box(9, 9)), 
				() -> add(name),
				() -> remove(name) );
		return(Sos.row(2, new Shape[] {
				b,
				Sos.label("  " + name),
				Sos.eol()
		}));
	}
	
	public void add(String name) {
	}
	
	public void remove(String name) {
	}
}
