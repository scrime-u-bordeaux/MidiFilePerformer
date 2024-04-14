package fr.inria.midifileperformer;

import java.net.URL;
import java.util.Vector;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;

import fr.inria.bps.base.Event;
import fr.inria.bps.base.Vecteur;
import fr.inria.fun.Proc1;
import fr.inria.midi.MidiLib;
import fr.inria.midifileperformer.core.C;
import fr.inria.midifileperformer.core.EndOfStream;
import fr.inria.midifileperformer.core.Interval;
import fr.inria.midifileperformer.core.Merger;
import fr.inria.midifileperformer.core.Peek;
import fr.inria.midifileperformer.core.SameEvent;
import fr.inria.midifileperformer.impl.Midi;
import fr.inria.midifileperformer.impl.MidiMsg;

public class Lib {

	/*
	 * The Jean's metapiano send periodically dummy message
	 */
	public static C<MidiMsg> metapiano(int in) {
		return(Midi.keyboard(in).filter(ev->dummy(ev.value.msg)));
	}

	static boolean dummy(MidiMessage message) {
		if(!(message instanceof ShortMessage)) return(false);
		ShortMessage msg = (ShortMessage) message;
		return(msg.getCommand() == 0x80 && msg.getData1() == 0);
	}

	/*
	 * Merge
	 */
	public static <T1,T2,T3> Merger<Vector<T1>,T2,Vector<T3>> toVector(Merger<T1,T2,T3> merge) {
		return(new Merger<Vector<T1>,T2,Vector<T3>>() {
			public Vector<T3> left(Vector<T1> v) {
				return(Vecteur.map(v, x -> merge.left(x)));
			}
			public Vector<T3> merge(Vector<T1> v, T2 v2) {
				return(Vecteur.map(v, x -> merge.merge(x, v2)));
			}
			public Vector<T3> right(T2 v2) {
				return(Vecteur.sing(merge.right(v2)));
			}
		});
	}

	public static <T> Merger<Event<T>,T,Event<T>> mergeEvent(Merger<T,T,T> m) {
		return(new Merger<Event<T>,T,Event<T>>() {
			public Event<T> merge(Event<T> x1, T x2) {
				return(Event.make(x1.time, m.merge(x1.value, x2)));
			}
			public Event<T> left(Event<T> x1) {
				return(x1);
			}
			public Event<T> right(T x2) {
				return(Event.start(x2));
			}
		});
	}


	/*
	 * Chrono -> Chrono
	 */
	public static <T> C<T> trace(C<T> c, Proc1<Event<T>> trace) {
		return(c.map(e -> {trace.operation(e); return(e);}));
	}

	public static <T> C<T> trace(C<T> c, String head) {
		return(Lib.trace(c, e -> System.out.println(head + " " + e)));
	}

	public static <T> C<T> trace(C<T> c) {
		return(Lib.trace(c, ""));
	}

	/*
	 * Analysis
	 */
	public static <T> Vector<Event<T>> reStart(long time, Vector<T> v) {
		return(Vecteur.map(v, x -> Event.make(time, x)));
	}

	public static <T> Vector<Event<T>> dist(Event<Vector<T>> e) {
		return(reStart(e.time, e.value));
	}

	public static <T extends Interval & SameEvent<T>> C<Vector<Event<T>>> stdAnalysis(C<T> c, boolean unmeet) {
		C<Vector<T>> c1 = C.compressBetweenOn(c.sync());
		if(unmeet)
			c1 = C.unmeet(c1);
		return(c1.map(e -> Event.make(e.time, reStart(e.time, e.value))));
	}

	public static <T extends Interval & SameEvent<T>> C<Vector<Event<T>>> unstdAnalysis(C<T> c, boolean unmeet) {
		C<Vector<Event<T>>> c1 = C.uncompressBetweenOn(c.sync());
		if(unmeet) c1 = C.ununmeet(c1);
		return(c1);
	}

	/*
	 * compressOn
	 */ 
	public static <T extends Interval> C<Vector<Event<T>>> countOnAnalysis(C<T> c, int count) {
		return(splitFirst(insertEmpty(compressOn(c, count))));
	}

	public static <T extends Interval> C<Vector<Event<T>>> compressOn(C<T> master, int count) {
		Peek<T> pc = Peek.make(master);
		return(new C<Vector<Event<T>>>() {
			public Event<Vector<Event<T>>> get() throws EndOfStream {
				int seen = 0;
				Vector<Event<T>> r = new Vector<Event<T>>();
				Event<T> ev = pc.peek();
				long time = ev.time;
				try {
					while(seen <= count) {
						if(ev.value.isBegin()) seen++;
						if(seen > count) return(Event.make(time, r));
						r.add(ev);
						ev = pc.nextPeek();
					}
				} catch (EndOfStream e) {
				}
				return(Event.make(time, r));
			}
		});
	}

	public static <T> C<Vector<Event<T>>> insertEmpty(C<Vector<Event<T>>> c) {
		return(new C<Vector<Event<T>>>() {
			boolean toggle = false;
			long last = 0;
			public Event<Vector<Event<T>>> get() throws EndOfStream {
				toggle = !toggle;
				if(toggle) {
					Event<Vector<Event<T>>> r = c.get();
					last = r.time;
					return(r);
				} else {
					return(Event.make(last, new Vector<Event<T>>()));
				}
			}});
	}

	public static <T extends Interval> C<Vector<Event<T>>> splitFirst(C<Vector<Event<T>>> c) {
		return(new C<Vector<Event<T>>>() {
			boolean init = false;
			Event<Vector<Event<T>>> realFirst = null;
			public Event<Vector<Event<T>>> get() throws EndOfStream {
				if(!init) {
					init = true;
					Event<Vector<Event<T>>> first = c.get(); 
					realFirst = splitAfterBegin(first);
					return(splitToBegin(first));
				} else if(realFirst != null) {
					Event<Vector<Event<T>>> r = realFirst;
					realFirst = null;
					return(r);
				} else {
					return(c.get());
				}
			}});
	}

	static <T extends Interval> Event<Vector<Event<T>>> splitToBegin(Event<Vector<Event<T>>> ev) {
		return(ev.convert(x -> Vecteur.prefix(x, e -> e.value.isBegin())));
	}

	static <T extends Interval> Event<Vector<Event<T>>> splitAfterBegin(Event<Vector<Event<T>>> ev) {
		return(ev.convert(x -> Vecteur.suffix(x, e -> e.value.isBegin())));
	}

	public static <T> C<Vector<Event<T>>> beatAnalysis(C<T> c, int beat) {
		return(C.compressBeat(c, beat));
	}

	/*
	 * chanAnalysis
	 */
	public static <T extends Interval> C<Vector<Event<MidiMsg>>> chanAnalysis(C<MidiMsg> c, int chan) {
		C<Vector<Event<MidiMsg>>> c1 = doChan(c.sync(), chan);
		return(c1);
	}

	public static <T extends Interval> C<Vector<Event<MidiMsg>>> doChan(C<Vector<MidiMsg>> c, int chan) {
		Peek<Vector<MidiMsg>> pc = Peek.make(c);
		return(new C<Vector<Event<MidiMsg>>>() {
			boolean toggle = false;
			Vector<Event<Vector<MidiMsg>>> shunk;
			public Event<Vector<Event<MidiMsg>>> get() throws EndOfStream {
				toggle = !toggle;
				if(toggle) {
					shunk = collectToBegin();
					return(onFirst(extractForOn(shunk)));
				} else {
					return(onFirst(extractForOff(shunk)));
				}
			}
			Event<Vector<Event<MidiMsg>>> onFirst(Vector<Event<MidiMsg>> v) {
				return(Event.make(v.get(0).time, v));
			}
			Vector<Event<MidiMsg>> extractForOff(Vector<Event<Vector<MidiMsg>>> v) {
				int n = v.size();
				Vector<Event<MidiMsg>> r = new Vector<Event<MidiMsg>>(n);
				for(int i=0; i<n; i++) {
					Event<Vector<MidiMsg>> e = v.get(i);
					long time = e.time;
					Vector<MidiMsg> msgs = e.value;
					for( MidiMsg m : msgs )
						if((MidiLib.getChannel(m.msg) == chan) || !m.isBegin())
							r.add(Event.make(time, m));
				}
				return(r);
			}
			Vector<Event<MidiMsg>> extractForOn(Vector<Event<Vector<MidiMsg>>> v) {
				int n = v.size();
				Vector<Event<MidiMsg>> r = new Vector<Event<MidiMsg>>(n);
				for(int i=0; i<n; i++) {
					Event<Vector<MidiMsg>> e = v.get(i);
					long time = e.time;
					Vector<MidiMsg> msgs = e.value;
					for( MidiMsg m : msgs )
						if((MidiLib.getChannel(m.msg) != chan) || m.isBegin())
							r.add(Event.make(time, m));
				}
				return(r);
			}
			Vector<Event<Vector<MidiMsg>>> collectToBegin() throws EndOfStream {
				return(pc.count(e -> Vecteur.any(e.value, m->hbc(m)), 2));
			}
			boolean hbc(MidiMsg m) {
				return(m.isBegin() && MidiLib.getChannel(m.msg) == chan);
			}
	});
}



// redirection of exception
public static URL url(String name) {
	try {
		return(new URL(name));
	} catch (Exception e) {
		throw(new RuntimeException(e));
	}
}

public static void sleep(long ms) {
	try {
		Thread.sleep(ms);
	} catch (Exception e) {
		throw(new RuntimeException(e));
	}
}
}
