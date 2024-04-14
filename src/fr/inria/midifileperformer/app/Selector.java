package fr.inria.midifileperformer.app;

import java.util.Vector;

import fr.inria.bps.base.Pair;
import fr.inria.fun.Fun0;
import fr.inria.lognet.sos.Shape;
import fr.inria.lognet.sos.Sos;
import fr.inria.lognet.sos.event.SosMouse;
import fr.inria.lognet.sos.shape.Wrapper;

public class Selector extends Wrapper {
	// To put in Sos
	public Vector<Pair<Shape,Fun0<Shape>>> descr;
	public Vector<Onglet> onglets;
	public Wrapper master;
	public int selected = -1;
	
	public Selector(int w, int h, Vector<Pair<Shape,Fun0<Shape>>> descr) {
		this.width = w;
		this.height = h;
		int n = descr.size();
		this.descr = descr;
		//this.master = new Wrapper(descr.get(0).cdr.operation());
		this.master = new Wrapper(new Shape(w, h));
		this.shape =
				Sos.column(0,
						makeOnglets(),
						masterDeco()
				);
		//onglets.get(selected).state = true;
		for(int i=0; i<n; i++) {
			int index = i;
			Sos.listen(SosMouse.up, descr.get(i).car, e->change(index));
		}
		//change(0);
	}
	
	public void translate(int nx, int ny) {
		//System.out.println("Selector translate " + nx + " " + ny);
		super.translate(nx, ny);
	}

	public void grow(int w, int h) {
		//System.out.println("Selector grow " + w + " " + h);
		super.grow(w, h);
	}
	
	Shape masterDeco() {
		return(Sos.contour(2, master));
	}
	
	Shape makeOnglets() {
		int n = descr.size();
		Vector<Onglet> row = new Vector<Onglet>(n);
		onglets = row;
		for(int i=0; i<n; i++) row.add(ongletDeco(descr.get(i).car));
		return(Sos.row(0, row.toArray(new Onglet[]{})));
	}
	
	Onglet ongletDeco(Shape s) {
		return(new Onglet(Sos.contour(1, s)));
	}

	void change(int i) {
		if(i != selected) {
			if(selected != -1) onglets.get(selected).state = false;
			selected = i;
			master.reshapeSEE(descr.get(i).cdr.operation());
			onglets.get(selected).state = true;
			dirty();
		}
	}
}

class Onglet extends Wrapper {
	public boolean state = false;
	public Onglet(Shape s) {
		super(s);
	}

    public void repaint(int cx, int cy, int cw, int ch) {
	if(state)
	    shape.reverserepaint(cx, cy, cw, ch);
	else
	    shape.cliprepaint(cx, cy, cw, ch);
    }
}
