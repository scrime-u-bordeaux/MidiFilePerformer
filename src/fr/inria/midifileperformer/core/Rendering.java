package fr.inria.midifileperformer.core;

import java.util.Vector;

import fr.inria.bps.base.Event;
import fr.inria.bps.base.Pair;
import fr.inria.bps.base.Vecteur;
import fr.inria.midifileperformer.impl.InputDevice;

public class Rendering {


	// Give some static functions of the flavor {Peek}Chronology<T1> Chronology<T2> -> Chronology<T3>


	/*
	 * Simple examples of rendering
	 */
	public static <T1,T2> C<T1> stepBy(C<T1> c1, C<T2> c2) {
		return(new C<T1>() {
			public Event<T1> get() throws EndOfStream {
				c2.get();
				return(c1.get());
			};
		});
	}


	/*
	 * Suppose a preprocessing to collect events for Begin then End
	 */
	public static <T1 extends Interval,T2 extends Interval,T3> C<Vector<T3>> 
	mergeBegin(C<Vector<T1>> c1, C<T2> c2, Merger<T1,T2,T3> merge) {
		return(new C<Vector<T3>>() {
			Vector<Vector<T1>> pending = new Vector<Vector<T1>>();
			boolean lastBegin = false;
			boolean init = false;
			public Event<Vector<T3>> get() throws EndOfStream {
				if(!init) {
					init = true;
					Event<Vector<T1>> e1 = c1.get();
					return(mergeLeft(e1.value));
				}
				Event<T2> ev2 = c2.get();
				if(ev2.value.isBegin()) return(mapMerge(collectForBegin(), ev2));
				if(ev2.value.isEnd()) return(mapMerge(collectForEnd(), ev2));
				return(mergeRight(ev2));
			}

			Vector<T1> collectForBegin() throws EndOfStream {
				Event<Vector<T1>> e1 = c1.get();
				if(lastBegin) {
					pending.add(e1.value);
					e1 = c1.get();
				}
				lastBegin = true;
				return(e1.value);
			}

			Vector<T1> collectForEnd() throws EndOfStream {
				if(pending.size() != 0) return(Vecteur.next(pending));
				lastBegin = false;
				Event<Vector<T1>> e1 = c1.get();
				return(e1.value);
			}

			Event<Vector<T3>> mergeLeft(Vector<T1> v) {
				return(Event.make(System.currentTimeMillis(), Vecteur.map(v, x -> merge.left(x))));
			}
			Event<Vector<T3>> mapMerge(Vector<T1> v, Event<T2> ev2) {
				return(Event.make(ev2.time, Vecteur.map(v, x -> merge.merge(x, ev2.value))));
			}
			Event<Vector<T3>> mergeRight(Event<T2> ev2) {
				return(Event.make(ev2.time, merge.right(ev2.value)).sing());
			}
		});
	}

	/*
	 * Same as previous but we respect the order of End
	 */
	public static double tempo = 1.0;
	public static <T1,T2 extends Interval & SameEvent<T2>,T3> C<T3> 
	mergeBeginEnd(C<T1> c1, C<T2> c2, Merger<T1,T2,T3> merge) {
		return(new C<T3>() {
			Vector<Pair<T2,T1>> pending = new Vector<Pair<T2,T1>>();
			boolean init = false;
			long lasttime = -1;
			public Event<T3> get() throws EndOfStream {
				//System.out.println("init mbe");
				if(!init) {
					init = true;
					//System.out.println("mergex "+c1);
					Event<T1> e1 = c1.get();
					//System.out.println("merge "+e1);
					return(Event.make(System.currentTimeMillis(), merge.left(e1.value)));
				}
				//System.out.println("get mbe");
				Event<T2> ev2 = c2.get();
				//System.out.println("got mbe");
				if(ev2.value.isBegin()) return(collectForBegin(ev2));
				if(ev2.value.isEnd()) return(collectForEnd(ev2));
				return(Event.make(ev2.time, merge.right(ev2.value)));
			}

			Event<T3> collectForBegin(Event<T2> ev2) throws EndOfStream {
				Event<T1> e1 = c1.get();
				long time = e1.time;
				if(lasttime > 0) {
					tempo = InputDevice.tempoOn()/((double) (time-lasttime));
					//System.out.println(InputDevice.tempoOn() + " vs " + (time-lasttime));
					//System.out.println("tempo "+tempo);
				}
				lasttime = time;
				try {
					Event<T1> de1 = c1.get();
					pending.add(Pair.cons(ev2.value, de1.value));
				} catch (EndOfStream e) {
				}
				return(Event.make(ev2.time, merge.merge(e1.value, ev2.value)));
			}

			Event<T3> collectForEnd(Event<T2> ev2) {
				return(Event.make(ev2.time, merge.merge(remassq(ev2), ev2.value)));
			}

			T1 remassq(Event<T2> ev2) {
				int n = pending.size();
				for(int i=0; i<n; i++) {
					Pair<T2,T1> p = pending.get(i);
					if(p.car.correspond(ev2.value)) {
						pending.removeElementAt(i);
						return(p.cdr);
					}
				}
				// Strange!!
				return(Vecteur.next(pending).cdr);
			}
		});
	}

}

