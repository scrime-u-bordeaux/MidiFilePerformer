package fr.inria.midifileperformer.core;

import java.util.Vector;

import fr.inria.bps.base.Event;
import fr.inria.bps.base.Vecteur;
import fr.inria.fun.Fun1;

public class H<T> {
	public Vector<Event<T>> values;
	
	
	public H(Vector<Event<T>> values) {
		this.values = values;
	}
	
	public H() {
		this(new Vector<Event<T>>());
	}
	
	public H(int n) {
		this(new Vector<Event<T>>(n));
	}
	
	public int size() {
		return(values.size());
	}
	
	public Event<T> get(int i) {
		return(values.get(i));
	}
	
	public void add(Event<T> e) {
		values.add(e);
	}
	
	public void addAll(H<T> c) {
		values.addAll(c.values);
	}
	
	public H<T> merge(H<T> h) {
		int n1 = values.size();
		Vector<Event<T>> right = h.values;
		int n2 = right.size();
		Vector<Event<T>> r = new Vector<Event<T>>(n1+n2);
		int i1 = 0;
		int i2 = 0;
		while(i1 < n1 && i2 < n2) {
			Event<T> e1 = values.get(i1++);
			Event<T> e2 = right.get(i2++);
			r.add((e1.time <= e2.time) ? e1 : e2);
		}
		while(i1 < n1) r.add(values.get(i1++));
		while(i2 < n2) r.add(right.get(i2++));
		return(new H<T>(r));
	}
	
	public H<T> filter(Fun1<Event<T>,Boolean> f) {
		return(new H<T>(Vecteur.filter(values, f)));
	}
}
