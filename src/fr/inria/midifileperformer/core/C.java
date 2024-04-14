package fr.inria.midifileperformer.core;

import java.util.Vector;

import fr.inria.bps.base.Event;
import fr.inria.bps.base.Pair;
import fr.inria.bps.base.Vecteur;
import fr.inria.fun.Fun0;
import fr.inria.fun.Fun1;
import fr.inria.fun.Fun3;
import fr.inria.midifileperformer.Lib;

public abstract class C<T> extends S<Event<T>> {

	/*
	 * The empty chronology 
	 */
	public static <T> C<T> NULL() {
		return(new C<T>() {
			public Event<T> get() throws EndOfStream {
				throw(new EndOfStream("The empty stream"));
			};
		});
	}
	
	/*
	 * Conversion to H
	 */
	public H<T> toH() {
		H<T> r = new H<T>();
		try {
			while(true) {
				r.add(get());
			}
		} catch(EndOfStream e) {
		}
		return(r);
	}

	/*
	 * Metronome
	 */
	public static <T> C<T> clock(int ms, T value) {
		return(new C<T>() {
			public Event<T> get() {
				Lib.sleep(ms);
				return(Event.make(System.currentTimeMillis(), value));
			}
		});
	}

	/*
	 * Convert a vector of event to a Chronology
	 */
	public static <T> C<T> make(Vector<Event<T>> events) {
		return(new C<T>() {
			int i=0;
			public Event<T> get() throws EndOfStream {
				if(i >= events.size()) throw(new EndOfStream("End of data"));
				return(events.get(i++));
			}
		});
	}

	public static <T> C<T> make(Vector<T> values, int delay) {
		return(new C<T>() {
			int i=0;
			public Event<T> get() throws EndOfStream {
				if(i >= values.size()) throw(new EndOfStream("End of data"));
				return(Event.make(i*delay, values.get(i++)));
			}
		});
	}


	/*
	 * Distribute
	 */
	public <T1 extends Consumer<Event<T>>> void distribute(Vector<T1> outputs) {
		try {
			while(true) {
				Event<T> event = get();
				for(Consumer<Event<T>> cons : outputs) {
					cons.accept(event);
				}
			}
		} catch (EndOfStream e) {
		}
	}


	/*
	 * seq
	 */
	public C<T> seq(C<T> c) {
		C<T> me = this; 
		return(new C<T>() {
			boolean left = true;
			public Event<T> get() throws EndOfStream  {
				if(left) {
					try {
						return(me.get());
					} catch (EndOfStream e) {
						left = false;
						return(c.get());
					}
				}
				return(c.get());
			}
		});
	}

	/*
	 * map
	 */
	public <T2> C<T2> map(Fun1<Event<T>,Event<T2>> f) {
		C<T> me = this; 
		return(new C<T2>() {
			public Event<T2> get() throws EndOfStream {
				return(f.operation(me.get()));
			}
		});
	}

	/*
	 * filter
	 */
	public C<T> filter(Fun1<Event<T>,Boolean> filter) {
		C<T> me = this; 
		return(new C<T>() {
			public Event<T> get() throws EndOfStream {
				Event<T> r = me.get();
				while(!(filter.operation(r))) r = me.get();
				return(r);
			};
		});
	}

	/*
	 * count
	 */
	public Vector<Event<T>> count(Fun1<Event<T>,Boolean> f, int n) throws EndOfStream {
		Peek<T> me = new Peek<T>(this);
		Vector<Event<T>> r = new Vector<Event<T>>();
		Event<T> ev = me.peek();
		int count = 0;
		try {
			while(true) {
				if(f.operation(ev)) {
					count++;
					if(count >= n) return(r);
				}
				r.add(ev);
				ev = me.nextPeek();
			}
		} catch(EndOfStream e) {
		}
		return(r);
	}

	/*
	 * collectTo
	 */
	public Vector<Event<T>> collectTo(Fun1<Event<T>,Boolean> f) throws EndOfStream {
		Peek<T> me = new Peek<T>(this);
		Vector<Event<T>> r = new Vector<Event<T>>();
		Event<T> ev = me.peek();
		try {
			while(!(f.operation(ev))) {
				r.add(ev);
				ev = me.nextPeek();
			}
		} catch(EndOfStream e) {
		}
		return(r);
	}

	/*
	 * getAndCollectTo
	 */
	public Vector<Event<T>> getAndCollectTo(Fun1<Event<T>,Boolean> f) throws EndOfStream {
		Peek<T> me = new Peek<T>(this);
		Vector<Event<T>> r = new Vector<Event<T>>();
		Event<T> ev = me.peek();
		try {
			r.add(ev);
			ev=me.nextPeek();
			while(!(f.operation(ev))) {
				r.add(ev);
				ev = me.nextPeek();
			}
		} catch(EndOfStream e) {
		}
		return(r);
	}

	/*
	 * folding
	 */
	public C<Vector<T>> fold(Fun3<Long, Vector<T>, Event<T>,Boolean> insert) {
		Peek<T> me = new Peek<T>(this);
		return(new C<Vector<T>>() {
			public Event<Vector<T>> get() throws EndOfStream  {
				Vector<T> r = new Vector<T>();
				Event<T> event = me.peek();
				long time = event.time;
				try {
					while(insert.operation(time, r, event)) {
						time = event.time;
						r.add(event.value);
						me.get();
						event = me.peek();
					}
				} catch (EndOfStream e) {
				}
				return(Event.make(time, r));
			}
		});
	}

	/*
	 * Merge all events with the same time
	 */
	public C<Vector<T>> sync() {
		return(fold((t,v,e) -> v.size() == 0 || t == e.time));
	}

	/*
	 * Merge two successive events
	 */
	public C<Pair<T,T>> pair() {
		C<T> me = this;
		return(new C<Pair<T,T>>() {
			public Event<Pair<T,T>> get() throws EndOfStream {
				Event<T> car = me.get();
				Event<T> cdr = me.get();
				return(Event.make(car.time, Pair.cons(car.value, cdr.value)));
			}
		});
	}

	/*
	 * Destructuration of blocks
	 */
	public static <T> C<T> unfold(C<Vector<T>> c) {
		return(new C<T>() {
			Vector<T> slice = new Vector<T>();
			long time;
			int i = 0;
			public Event<T> get() throws EndOfStream {
				while(i >= slice.size()) {
					Event<Vector<T>> e = c.get();
					time = e.time;
					slice = e.value;
					i = 0;
				}
				return(Event.make(time, slice.get(i++)));
			};
		});
	}

	/*
	 * Loop
	 */
	public static <T> C<T> loop(Fun0<C<T>> gen) {
		return(new C<T>() {
			C<T> current = gen.operation();
			public Event<T> get() throws EndOfStream {
				while(current != null) {
					try {
						return(current.get());
					} catch (EndOfStream e) {
						current = gen.operation();
					}
				}
				throw(new EndOfStream("end of loop"));
			};
		});
	}

	/*
	 * compressBetweenOn
	 */ 
	public static <T extends Interval> C<Vector<T>> compressBetweenOn(C<Vector<T>> master) {
		Peek<Vector<T>> pc = Peek.make(master);
		return(new C<Vector<T>>() {
			boolean init = false;
			boolean toggle = false;
			long lastOn = -1;
			public Event<Vector<T>> get() throws EndOfStream {
				if(!init) {
					init = true;
					// force a first event to be without Begin
					Event<Vector<T>> r = collectToBegin();
					return(r);
				}
				toggle = !toggle;
				if(toggle) {
					Event<Vector<T>> event = pc.get();
					lastOn = event.time;
					return(event);
				} else {
					return(collectToBegin());
				}
			}
			Event<Vector<T>> collectToBegin() {
				Vector<T> r = new Vector<T>();
				try {
					Event<Vector<T>> ev = pc.peek();
					long time = -1;
					try {
						while(!Interval.hasBegin(ev.value)) {
							time = ev.time;
							pc.get();
							r.addAll(ev.value);
							ev = pc.peek();
						}
					} catch (EndOfStream e) {
					}
					if(time == -1) time = ev.time-1;
					return(Event.make(time, r));
				} catch (EndOfStream e) {
					return(Event.make(lastOn+1, r));
				}
			}
		});
	}

	/*
	 * uncompressBetweenOn
	 */ 
	public static <T extends Interval> C<Vector<Event<T>>> uncompressBetweenOn(C<Vector<T>> master) {
		Peek<Vector<T>> pc = Peek.make(master);
		return(new C<Vector<Event<T>>>() {
			boolean init = false;
			boolean toggle = false;
			long lastOn = -1;
			public Event<Vector<Event<T>>> get() throws EndOfStream {
				if(!init) {
					init = true;
					// force a first event to be without Begin
					Event<Vector<Event<T>>> r = collectToBegin();
					return(r);
				}
				toggle = !toggle;
				if(toggle) {
					Event<Vector<T>> event = pc.get();
					lastOn = event.time;
					return(event.convert(v -> Lib.reStart(lastOn, v)));
				} else {
					return(collectToBegin());
				}
			}
			Event<Vector<Event<T>>> collectToBegin() {
				Vector<Event<T>> r = new Vector<Event<T>>();
				try {
					Event<Vector<T>> ev = pc.peek();
					long firsttime = ev.time;
					try {
						while(!Interval.hasBegin(ev.value)) {
							long curtime = ev.time;
							pc.get();
							r.addAll(Vecteur.map(ev.value, v -> Event.make(curtime, v)));
							ev = pc.peek();
						}
					} catch (EndOfStream e) {
					}
					return(Event.make(firsttime, r));
				} catch (EndOfStream e) {
					return(Event.make(lastOn+1, r));
				}
			}
		});
	}



	/*
	 * The transformation of Jean
	 */
	public static <T extends Interval & SameEvent<T>> C<Vector<T>> unmeet(C<Vector<T>> master) {
		Peek<Vector<T>> c = Peek.make(master);
		return(new C<Vector<T>>() {
			boolean toggle = true;
			Vector<T> lastBegin = null;
			public Event<Vector<T>> get() throws EndOfStream {
				Event<Vector<T>> event = c.get();
				toggle = !toggle;
				if(toggle) {
					lastBegin = event.value;
					return(event);
				}
				try {
					Event<Vector<T>> nextBegin = c.peek();
					if(nextBegin == null) return(event);
					if(lastBegin != null)
						filterX(event.value, nextBegin.value);
				} catch (EndOfStream e) {
				}
				return(event);
			}
			void filterX(Vector<T> E, Vector<T> nextB) {
				for(T x : lastBegin) {
					if(x.isBegin()) {
						for(int i=0; i<nextB.size(); ) {
							T y = nextB.get(i);
							if(y.isEnd() && x.correspond(y)) {
								nextB.remove(i);
								E.add(y);
							} else {
								i++;
							}
						}
					}
				}
			}
		});
	}

	/*
	 * The transformation of Jean
	 */
	public static <T extends Interval & SameEvent<T>> C<Vector<Event<T>>> ununmeet(C<Vector<Event<T>>> master) {
		Peek<Vector<Event<T>>> c = Peek.make(master);
		return(new C<Vector<Event<T>>>() {
			boolean toggle = true;
			Vector<Event<T>> lastBegin = null;
			public Event<Vector<Event<T>>> get() throws EndOfStream {
				Event<Vector<Event<T>>> event = c.get();
				toggle = !toggle;
				if(toggle) {
					lastBegin = event.value;
					return(event);
				}
				try {
					Event<Vector<Event<T>>> nextBegin = c.peek();
					if(nextBegin == null) return(event);
					if((lastBegin != null) && event.value.size() == 0)
						filterX(event.value, nextBegin.value);
				} catch (EndOfStream e) {
				}
				return(event);
			}
			void filterX(Vector<Event<T>> E, Vector<Event<T>> nextB) {
				// lastBegin=(..x..) E nextB=(..-x..) => (..x..) E+-x (....)
				for(Event<T> x : lastBegin) {
					if(x.value.isBegin()) {
						for(int i=0; i<nextB.size(); ) {
							Event<T> y = nextB.get(i);
							if(y.value.isEnd() && y.time == 0 && x.value.correspond(y.value)) {
								nextB.remove(i);
								E.add(y);
							} else {
								i++;
							}
						}
					}
				}
			}
		});
	}

	/*
	 * compressBeat
	 */ 
	public static <T> C<Vector<Event<T>>> compressBeat(C<T> master, int beat) {
		Peek<T> pc = Peek.make(master);
		return(new C<Vector<Event<T>>>() {
			long bbeat = 0;
			long ebeat = beat;
			boolean init = false;
			boolean toggle = false;
			public Event<Vector<Event<T>>> get() throws EndOfStream {
				if(!init) {
					init = true;
					// force a first event to be without Begin
					return(Event.start(new Vector<Event<T>>(0)));
				}
				toggle = !toggle;
				if(toggle) {
					return(collectToBeat(bbeat, ebeat));
				} else {
					bbeat = ebeat;
					ebeat = bbeat + beat; 
					return(Event.make(bbeat, new Vector<Event<T>>(0)));
				}
			}
			Event<Vector<Event<T>>> collectToBeat(long bbeat, long ebeat) throws EndOfStream {
				Vector<Event<T>> r = new Vector<Event<T>>();
				Event<T> ev = pc.peek();
				try {
					while(ev.time < ebeat) {
						pc.get();
						System.out.println("Beat "+bbeat+" "+ev+" "+ebeat);

						r.add(ev);
						ev = pc.peek();
					}
				} catch (EndOfStream e) {
				}
				return(Event.make(bbeat, r));
			}
		});
	}

}
