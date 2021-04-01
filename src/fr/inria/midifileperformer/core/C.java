package fr.inria.midifileperformer.core;

import java.util.Vector;

import fr.inria.bps.base.Pair;
import fr.inria.fun.Fun0;
import fr.inria.fun.Fun1;
import fr.inria.fun.Fun3;
import fr.inria.midifileperformer.Lib;

public abstract class C<T> extends S<Event<T>> {

	/*
	 * Generic Chronology construction based on functions
	 */
	public static <T> C<T> make(Fun0<Event<T>> get) {
		return(new C<T>() {
			public Event<T> get() {
				return(get.operation());
			};
		});
	}

	/*
	 * The empty chronology 
	 */
	public static <T> C<T> NULL() {
		return(make(() -> null));
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
	 * Convert a vector of event to a PeekChronology
	 */
	public static <T> C<T> make(Vector<Event<T>> events) {
		return(new C<T>() {
			int i=0;
			public Event<T> get() {
				if(i >= events.size()) return(null);
				return(events.get(i++));
			}
		});
	}

	public static <T> C<T> make(Vector<T> values, int delay) {
		return(new C<T>() {
			int i=0;
			public Event<T> get() {
				if(i >= values.size()) return(null);
				return(Event.make(i*delay, values.get(i++)));
			}
		});
	}


	/*
	 * Collect
	 */

	public static <T1,T extends Producer<T1>> C<T1> collector(Vector<T> inputs) {
		return(collect(inputs).live());
	}

	/*
	 * Distribute
	 */
	public <T1 extends Consumer<Event<T>>> void distribute(Vector<T1> outputs) {
		Event<T> event = get();
		while(event != null) {
			for(Consumer<Event<T>> cons : outputs)
				cons.accept(event);
			event = get();
		}
	}


	/*
	 * seq
	 */
	public C<T> seq(C<T> c) {
		C<T> me = this; 
		return(new C<T>() {
			boolean left = true;
			public Event<T> get() {
				if(left) {
					Event<T> v = me.get();
					if(v != null) return(v);
					left = false;
				}
				Event<T> v2 = c.get();
				if(v2 == null) return(null);
				return(v2);
			}
		});
	}

	/*
	 * map
	 */
	public <T2> C<T2> map(Fun1<Event<T>,Event<T2>> f) {
		C<T> me = this; 
		return(new C<T2>() {
			public Event<T2> get() {
				Event<T> v = me.get();
				if(v == null) return(null);
				return(f.operation(v));
			}
		});
	}

	/*
	 * filter
	 */
	public C<T> filter(Fun1<Event<T>,Boolean> filter) {
		C<T> me = this; 
		return(new C<T>() {
			public Event<T> get() {
				Event<T> r = me.get();
				while((r != null) && !(filter.operation(r))) r = me.get();
				return(r);
			};
		});
	}

	/*
	 * folding
	 */
	public C<Vector<T>> fold(Fun3<Long, Vector<T>,
							 Event<T>,Boolean> insert) {
		Peek<T> me = new Peek<T>(this);
		return(new C<Vector<T>>() {
			public Event<Vector<T>> get() {
				Vector<T> r = new Vector<T>();
				Event<T> event = me.peek();
				if(event == null) return(null);
				long time = event.time;
				while(event != null && insert.operation(time, r, event)) {
					me.get();
					time = event.time;
					r.add(event.value);
					event = me.peek();
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
			public Event<Pair<T,T>> get() {
				Event<T> car = me.get();
				if(car == null) return(null);
				Event<T> cdr = me.get();
				if(cdr == null) return(Event.make(car.time, Pair.cons(car.value, null)));
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
			public Event<T> get() {
				while(i >= slice.size()) {
					Event<Vector<T>> e = c.get();
					if(e == null) return(null);
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
			public Event<T> get() {
				Event<T> e = current.get();
				while(e == null) {
					current = gen.operation();
					if(current == null) return(null);
					e = current.get();
				}
				return(e);
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
			public Event<Vector<T>> get() {
				if(!init) {
					init = true;
					// force a first event to be without Begin
					Event<Vector<T>> r = collectToBegin();
					return(r);
				}
				toggle = !toggle;
				if(toggle) {
					Event<Vector<T>> event = pc.get();
					if(event == null) return(null);
					lastOn = event.time;
					return(event);
				} else {
					return(collectToBegin());
				}
			}
			Event<Vector<T>> collectToBegin() {
				Vector<T> r = new Vector<T>();
				long time = -1;
				Event<Vector<T>> e = pc.peek();
				while(e!=null && (!Core.hasBegin(e.value))) {
					pc.get();
					time = e.time;
					r.addAll(e.value);
					e = pc.peek();
				}
				if(time == -1) {
					if(e == null) {
						time = lastOn + 1;
					} else {
						time = e.time - 1;
					}
				}
				return(Event.make(time, r));
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
			public Event<Vector<T>> get() {
				Event<Vector<T>> event = c.get();
				if(event == null) return(null);
				toggle = !toggle;
				if(toggle) {
					lastBegin = event.value;
					return(event);
				}
				Event<Vector<T>> nextBegin = c.peek();
				if(nextBegin == null) return(event);
				if(lastBegin != null)
					filterX(event.value, nextBegin.value);
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

}
