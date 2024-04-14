package fr.inria.midifileperformer.impl;

import java.util.Vector;

import fr.inria.bps.base.Event;
import fr.inria.bps.base.Pair;
import fr.inria.bps.base.Vecteur;
import fr.inria.midi.MidiLib;
import fr.inria.midifileperformer.core.C;
import fr.inria.midifileperformer.core.EndOfStream;
import fr.inria.midifileperformer.core.H;
import fr.inria.midifileperformer.core.Peek;

public class Analysis {
	// All functions that "preprocess" the partition
	H<H<MidiMsg>> offOn;
	
	public Analysis(H<H<MidiMsg>> offOn) {
		this.offOn = offOn;
	}
	
	public Analysis(int n) {
		this(new H<H<MidiMsg>>(n));
	}
	
	public int size() {
		return(offOn.size());
	}
	
	public void add(Event<H<MidiMsg>> e) {
		offOn.add(e);
	}
	
	public void add(long t, H<MidiMsg> h) {
		offOn.add(Event.make(t, h));
	}
	
	public Event<H<MidiMsg>> get(int i) {
		return(offOn.get(i));
	}
	
	/*
	 * SplitChannel
	 */
	public static Pair<H<MidiMsg>,H<MidiMsg>> splitChannel(H<MidiMsg> h, int chan) {
		H<MidiMsg> in = new H<MidiMsg>();
		H<MidiMsg> out = new H<MidiMsg>();
		for( Event<MidiMsg> e : h.values ) {
			if(MidiLib.getChannel(e.value.msg) == chan) {
				in.add(e);
			} else {
				out.add(e);
			}
		}
		return(Pair.cons(in, out));
	}
	
	public Analysis unsplit(H<MidiMsg> h) {
		int n = offOn.size();
		Analysis r = new Analysis(n);
		Event<H<MidiMsg>> nextOn = (n > 1) ? offOn.get(1) : null; 
		int curh = r.mergeUntil(h, 0, offOn.get(0), (nextOn == null) ? -1 : nextOn.time);
		for(int i=1; i<n; i+=2) {
			Event<H<MidiMsg>> on = nextOn;
			Event<H<MidiMsg>> off = offOn.get(i+1);
			nextOn = (i+2 < n) ? offOn.get(i+2) : null;
			curh = r.mergeUntil(h, curh, on, (nextOn == null) ? -1 : nextOn.time);
			r.add(off);
		}
		return(r);
	}
	
	int mergeUntil(H<MidiMsg> h, int i, Event<H<MidiMsg>> cur, long nextime) {
		H<MidiMsg> on = cur.value;
		long time =  cur.time;
		int n = on.size();
		H<MidiMsg> dst = new H<MidiMsg>(n);
		add(time, dst);
		int j = 0;
		while(i < h.size()) {
			Event<MidiMsg> e = h.get(i);
			if(e.time < nextime) {
				while(j<on.size() && on.get(j).time <= e.time) dst.add(on.get(j++));
				dst.add(e);
				i++;
			} else {
				for(int k=j; k<on.size(); k++) dst.add(on.get(k));
				return(i);
			}
		}
		return(i);
	}

	/*
	 * lib
	 */
	static Event<H<MidiMsg>> inFirst(H<MidiMsg> v, long def) {
		if(v.size() == 0) return(Event.make(def, v));
		return(Event.make(v.get(0).time, v));
	}
	
	public Analysis addOff() {
		int n = size();
		H<H<MidiMsg>> r = new H<H<MidiMsg>>(2*n);
		for(int i=0; i<n; i++) {
			Event<H<MidiMsg>> e = get(i);
			r.add(e);
			r.add(Event.make(e.time, new H<MidiMsg>()));
		}
		return(new Analysis(r));
	}
	
	public Analysis addFirst() {
		int n = size();
		H<H<MidiMsg>> r = new H<H<MidiMsg>>(n+1);
		if(n == 0) {
			r.add(Event.make(0, new H<MidiMsg>()));
		} else {
			Event<H<MidiMsg>> h0 = get(0);
			//long time = h0.time;
			H<MidiMsg> v = h0.value;
			int m = v.size();
			H<MidiMsg> left = new H<MidiMsg>();
			int j = 0;
			while(j<m && !v.get(j).value.isBegin()) left.add(v.get(j++));
			r.add(Event.make(0, left));
			H<MidiMsg> right = new H<MidiMsg>();
			while(j<m) right.add(v.get(j++));
			r.add(inFirst(right, 0));
			for(int i=1; i<n; i++) r.add(get(i));
		}
		return(new Analysis(r));
	}
	
	/*
	 * Sing : the simplest analysis
	 */
	public static Analysis sing(H<MidiMsg> h) {
		// ... t:X ... => ... t:[t:X] ...
		int n = h.size();
		Analysis r = new Analysis(n);
		H<MidiMsg> cur = new H<MidiMsg>();
		r.add(0, cur);
		for( Event<MidiMsg> e : h.values ) {
			r.add(e.time, new H<MidiMsg>(Vecteur.sing(e)));
		}
		return(r);
	}
	
	/*
	 * Merge outmost events with same timestamp
	 */
	public Analysis sync() {
		// ... t:H1 t:H2 ... => ... r:{H1 cup H2} 
		int n = offOn.size();
		Analysis r = new Analysis(n);
		if(n == 0) return(r);
		H<MidiMsg> cur = new H<MidiMsg>();
		long time = offOn.get(0).time;
		for( Event<H<MidiMsg>> e : offOn.values ) {
			if(time != e.time) {
				r.add(time, cur);
				time = e.time;
				cur = e.value;
			} else {
				cur.addAll(e.value);
				//cur = cur.merge(e.value);
			}
		}
		r.add(time, cur);
		return(r);
	}
	
	/*
	 * On must be placed one slot on two
	 */
	public Analysis OnSeparation() {
		// ... ti:Hi ... => t0:h0 t1:H1 h2:h2 ...
		int n = offOn.size();
		Analysis r = new Analysis(n);
		H<MidiMsg> cur = new H<MidiMsg>();
		long lastime = 0;
		for( Event<H<MidiMsg>> e : offOn.values ) {
			if(Vecteur.any(e.value.values, x->x.value.isBegin())) {
				lastime = e.time;
				r.add(inFirst(cur, lastime-1));
				r.add(e);
				cur = new H<MidiMsg>();
			} else {
				cur.addAll(e.value);
			}
		}
		r.add(inFirst(cur, lastime+1));
		return(r);
	}
	
	/*
	 * Sync with some delay
	 */
	public Analysis compressDelay(int delay) {
		//  ... t1:H1 t'1:H'1 t2:H2 ... =(t2-t1<delay)=> ... t1:[H1 cup H2 cup H3] ...
		int n = offOn.size();
		if(n < 3) return(this);
		Analysis r = new Analysis(n);
		r.add(get(0));
		Event<H<MidiMsg>> lastOn = get(1);
		Event<H<MidiMsg>> lastOff = get(2);
		long lastTime = lastOn.time;
		for(int i=3; i<n; i+=2) {
			Event<H<MidiMsg>> e = get(i);
			if(e.time - lastTime <= delay) {
				System.out.println("compress " + (e.time-lastTime));
				lastOn.value.addAll(lastOff.value);
				lastOn.value.addAll(e.value);
			} else {
				r.add(lastOn);
				r.add(lastOff);
				lastOn = e;
				lastOff = get(i+1);
			}
			lastTime = e.time;
		}
		r.add(lastOn);
		r.add(lastOff);
		return(r);
	}
	
	/*
	 * 
	 */
	public static Analysis countOn(H<MidiMsg> h, int count) {
		int n = h.size();
		H<H<MidiMsg>> r = new H<H<MidiMsg>>(n/3);
		H<MidiMsg> cur = new H<MidiMsg>();
		r.add(Event.make(0, cur));
		int seen = 0;
		for(int i=0; i<n; i++) {
			Event<MidiMsg> e = h.get(i);
			if(e.value.isBegin()) {
				if(seen == count) {
					cur = new H<MidiMsg>();
					r.add(Event.make(e.time, cur));
					seen = 0;
				}
				seen++;
			}
			cur.add(e);
		}
		return(new Analysis(r));
	}
	
	/*
	 * Strict slice
	 */
	public static Analysis strictSlice(H<MidiMsg> h, int slice) {
		int n = h.size();
		H<H<MidiMsg>> r = new H<H<MidiMsg>>(n/8);
		H<MidiMsg> cur = new H<MidiMsg>();
		r.add(Event.make(0, cur));
		long next = slice;
		for(int i=0; i<n; i++) {
			Event<MidiMsg> e = h.get(i);
			if(e.time >= next) {
				cur = new H<MidiMsg>();
				r.add(Event.make(next, cur));
				next += slice;
			}
			if(e.time < next) cur.add(e);
		}
		return(new Analysis(r));
	}
	
	/*
	 * Slice where we adjust the start of the sublist
	 */
	public static Analysis slice(H<MidiMsg> h, int slice) {
		int epsilon = (int) (slice * 0.1);
		int n = h.size();
		H<H<MidiMsg>> r = new H<H<MidiMsg>>(n/8);
		H<MidiMsg> cur = new H<MidiMsg>();
		r.add(Event.make(0, cur));
		long next = slice;
		for(int i=0; i<n; i++) {
			Event<MidiMsg> e = h.get(i);
			if(e.time >= next) {
				cur = new H<MidiMsg>();
				if((e.time - next) < epsilon) next = e.time;
				r.add(Event.make(next, cur));
				next += slice;
			}
			if(e.time < next) cur.add(e);
		}
		return(new Analysis(r));
	}
	
	/*
	 * 
	 */
	public Analysis syncOff() {
		int n = offOn.size();
		Analysis r = new Analysis(n);
		for( int i=0; i<n; i++ ) {
			Event<H<MidiMsg>> e = get(i);
			long t = e.time;
			H<MidiMsg> v = e.value;
			int m = v.size();
			H<MidiMsg> vv = new H<MidiMsg>(m);
			for(int j=0; j<m; j++) vv.add(Event.make(t, v.get(j).value));
			r.add(Event.make(t, vv));
		}
		return(r);
	}
	

	/*
	 * The transformation of Jean
	 * ... [A:t X:t Y:t] [] [a:tt Z:tt] -> [A:t X:t Y:t] [a:tt-1] [Z:tt] 
	 */
	public Analysis unmeet() {
		int n = offOn.size();
		if(n < 2) return(this);
		H<H<MidiMsg>> oo = new H<H<MidiMsg>>(n);
		oo.add(offOn.get(0));
		Event<H<MidiMsg>> lastBegin = offOn.get(1);
		oo.add(lastBegin);
		for(int i=2; i<n-1; i+=2) {
			Event<H<MidiMsg>> off = offOn.get(i);
			Event<H<MidiMsg>> on = offOn.get(i+1);
			Event<MidiMsg> r = hasUnmeet(lastBegin.value, off.value, on.value);
			if(r != null) {
				//System.out.println("Gotcha!");
				oo.add(Event.make(off.time, new H<MidiMsg>(Vecteur.sing(r))));
				oo.add(Event.make(on.time, remove(on.value, r)));
			} else {
				oo.add(off);
				oo.add(on);
			}
			lastBegin = on;
		}
		oo.add(offOn.get(n-1));
		return(new Analysis(oo));
	}
	
	H<MidiMsg> remove(H<MidiMsg> v, Event<MidiMsg> e) {
		Vector<Event<MidiMsg>> r = new Vector<Event<MidiMsg>>(v.values);
		r.remove(e);
		return(new H<MidiMsg>(r));
	}
	
	static Event<MidiMsg> hasUnmeet(H<MidiMsg> lastBegin, H<MidiMsg> off, H<MidiMsg> on) {
		// [A:t X:t Y:t] [] [a:tt Z:tt] => a
		if(off.size() != 0) return(null);
		for(Event<MidiMsg> x : lastBegin.values) {
			if(x.value.isBegin()) {
				for(int i=0; i<on.size(); i++) {
					Event<MidiMsg> y = on.get(i);
					if(y.value.isEnd() && x.value.correspond(y.value)) {
						return(y);
					}
				}
			}
		}
		return(null);
	}
	
	/*
	 * Test for sime data implementation
	 * Sync
	 * ... A:t X:t Y:tt -> ... [A:t X:t]:t [Y:tt]:tt
	 */
	public static C<H<MidiMsg>> CsyncCH(C<MidiMsg> c) {
		Peek<MidiMsg> me = new Peek<MidiMsg>(c);
		return(new C<H<MidiMsg>>() {
			public Event<H<MidiMsg>> get() throws EndOfStream  {
				H<MidiMsg> r = new H<MidiMsg>();
				Event<MidiMsg> event = me.peek();
				long time = event.time;
				try {
					while(time == event.time) {
						r.add(event);
						me.get();
						event = me.peek();
					}
				} catch (EndOfStream e) {
				}
				return(Event.make(time, r));
			}
		});
	}

	public static H<H<MidiMsg>> HsyncHH(H<MidiMsg> c) {
		int n = c.size();
		H<H<MidiMsg>> rr = new H<H<MidiMsg>>(n);
		if(n == 0) return(rr);
		H<MidiMsg> r = new H<MidiMsg>();
		long time = c.get(0).time;
		for(int i=0; i<n; i++) {
			Event<MidiMsg> event = c.get(i);
			if(time == event.time) {
				r.add(event);
			} else {
				rr.add(Event.make(time, r));
				r = new H<MidiMsg>();
				time = event.time;
			}
		}
		rr.add(Event.make(time, r));
		return(rr);
	}

	public static H<H<MidiMsg>> CsyncHH(C<MidiMsg> c) {
		return(HsyncHH(c.toH()));
	}
}
