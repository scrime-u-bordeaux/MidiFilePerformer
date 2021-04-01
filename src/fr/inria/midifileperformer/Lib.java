package fr.inria.midifileperformer;

import java.net.URL;
import java.util.Vector;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;

import fr.inria.bps.base.Vecteur;
import fr.inria.fun.Proc1;
import fr.inria.midifileperformer.core.C;
import fr.inria.midifileperformer.core.Event;
import fr.inria.midifileperformer.core.Interval;
import fr.inria.midifileperformer.core.Merger;
import fr.inria.midifileperformer.core.SameEvent;

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
				if(v == null) return(null);
				return(Vecteur.map(v, x -> merge.merge(x, v2)));
			}
			public Vector<T3> right(T2 v2) {
				return(Vecteur.sing(merge.right(v2)));
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

	public static <T extends Interval & SameEvent<T>> C<Vector<T>> stdAnalysis(C<T> c, boolean unmeet) {
		C<Vector<T>> c1 = C.compressBetweenOn(c.sync());
		if(unmeet)
			c1 = C.unmeet(c1);
		return(c1);
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
