package fr.inria.midifileperformer;

import java.util.Vector;

import fr.inria.fun.Fun0;
import fr.inria.fun.Fun1;
import fr.inria.fun.Fun2;
import fr.inria.fun.Fun3;
import fr.inria.midifileperformer.core.C;
import fr.inria.midifileperformer.core.Consumer;
import fr.inria.midifileperformer.core.EndOfStream;
import fr.inria.midifileperformer.core.Event;
import fr.inria.midifileperformer.core.Peek;
import fr.inria.midifileperformer.impl.StringInterval;

public class Check extends RuntimeException {
	private static final long serialVersionUID = 1L;
	public static boolean verbose = false;
	public static boolean exitOnFirstError = true;
	
	public static void main(String[] args) {
		//Mains.launch(new Check(), args, 0);
		try {
			checkAll();
		} catch (Check e) {
		}
	}

	public static void checkAll() {
		checkDistributes();
		checkSeqs();
		checkMaps();
		checkDelays();
		checkFilters();
		checkFolds();
		checkSyncs();
		checkUnfolds();
		checkloop();
		checkCompresss();
		checkUnmeets();
	}

	static C<StringInterval> reads(String s) {
		if(s=="") return(C.NULL());
		String[] lines = s.split("\t");
		int n = lines.length;
		Vector<Event<StringInterval>> r = new Vector<Event<StringInterval>>(n);
		for(int i=0; i<n; i++) {
			String line = lines[i];
			String[] words = line.split(" ");
			int m = words.length;
			if(m < 2) throw new RuntimeException("empty line");
			String linen = words[0];
			long time = Long.parseLong(linen);
			r.add(Event.make(time, new StringInterval(words[1])));
		}
		return(C.make(r));
	}

	static C<Vector<StringInterval>> readv(String s) {
		if(s=="") return(C.NULL());
		String[] lines = s.split("\t");
		int n = lines.length;
		Vector<Event<Vector<StringInterval>>> r = new Vector<Event<Vector<StringInterval>>>(n);
		for(int i=0; i<n; i++) {
			String line = lines[i];
			String[] words = line.split(" ");
			int m = words.length;
			if(m == 0) throw new RuntimeException("empty line");
			String linen = words[0];
			long time = Long.parseLong(linen);
			Vector<StringInterval> values = new Vector<StringInterval>(n);
			for(int j=1; j<m; j++) values.add(new StringInterval(words[j]));
			r.add(Event.make(time, values));
		}
		return(C.make(r));
	}

	static <T> void check(String test, C<T> c1, C<T> c2, Fun2<T,T,Boolean> cmp) {
		int i = 0;
		if(verbose) System.out.println("-- checking " + test);
		while(true) {
			Event<T> ev1;
			Event<T> ev2;
			try {
				ev1 = c1.get();
			} catch (EndOfStream e1) {
				try {
					ev2 = c2.get();
				} catch (EndOfStream e2) {
					return;
				}
				fail(test, "not enought values", i, ev2, null);
				return;
			}
			try {
				ev2 = c2.get();
			} catch (EndOfStream e2) {
				fail(test, "too more values", i, ev1, null);
				return;
			}
			if((ev1.time != ev2.time) || !cmp.operation(ev1.value, ev2.value)) {
				fail(test, "not same event", i, ev1, ev2);
			}
			i++;
		}
	}
	
	static <T> void fail(String test, String msg, int i, Event<T> ev1, Event<T> ev2) {
		System.out.println("test \"" + test + "\" failed at index " + i + " : " + msg);
		System.out.println("\tcomputed :\t" + ev1);
		System.out.println("\tmust be :\t" + ev2);
		if(exitOnFirstError) throw(new Check());
	}

	static Fun2<StringInterval,StringInterval,Boolean> cmps = new Fun2<StringInterval,StringInterval,Boolean>() {
		public Boolean operation(StringInterval s1,StringInterval s2) {
			return(s1.data.compareTo(s2.data) == 0);
		}
	};

	static Fun2<Vector<StringInterval>,Vector<StringInterval>,Boolean> cmpv = 
			new Fun2<Vector<StringInterval>,Vector<StringInterval>,Boolean>() {
		public Boolean operation(Vector<StringInterval> v1,Vector<StringInterval> v2) {
			int n1 = v1.size();
			int n2 = v2.size();
			if(n1 != n2) return(false);
			for(int i=0; i<n1; i++) if(!cmps.operation(v1.get(i), v2.get(i))) return(false);
			return(true);
		}
	};

	/*
	 * Basic of basic
	 */
	public static void checkBasics() {
		checkBasic_0();
		checkBasic_1();
		checkBasic_2();
		checkBasic_3();
	}

	public static void checkBasic_0() {
		C<StringInterval> src = reads("0 a\t1 b\t2 c");
		C<StringInterval> done = src;
		C<StringInterval> r = reads("0 a\t1 b\t2 c");
		check("Basic 0 reads", done, r, cmps);
	}
	public static void checkBasic_1() {
		C<StringInterval> src = reads("0 a\t0 b\t2 c");
		C<StringInterval> done = src;
		C<StringInterval> r = reads("0 a\t0 b\t2 c");
		check("Basic 1 reads same time", done, r, cmps);
	}
	public static void checkBasic_2() {
		C<Vector<StringInterval>> src = readv("0 a\t1 b\t2 c");
		C<Vector<StringInterval>> done = src;
		C<Vector<StringInterval>> r = readv("0 a\t1 b\t2 c");
		check("Basic 2 readv", done, r, cmpv);
	}
	public static void checkBasic_3() {
		C<Vector<StringInterval>> src = readv("0 a b\t2 c");
		C<Vector<StringInterval>> done = src;
		C<Vector<StringInterval>> r = readv("0 a b\t2 c");
		check("Basic 3 readv multiple variable", done, r, cmpv);
	}
	
	/*
	 * Distribute
	 */
	public static void checkDistributes() {
		checkDistribute_0();
	}
	
	public static void checkDistribute_0() {
		C<StringInterval> src = reads("0 a\t1 b\t2 c");
		Vector<Consumer<Event<StringInterval>>> cons = new Vector<Consumer<Event<StringInterval>>>(2);
		Vector<Event<StringInterval>> v = new Vector<Event<StringInterval>>(6);
		cons.add(Consumer.fun(e -> v.add(e)));
		cons.add(Consumer.fun(e -> v.add(Event.make(e.time+10,StringInterval.META))));
		src.distribute(cons);
		src.force();
		C<StringInterval> done = C.make(v);
		C<StringInterval> r = reads("0 a\t10 META\t1 b\t11 META\t2 c\t12 META");		
		check("Distribute 0 duo", done, r, cmps);
	}
	
	/*
	 * Seq
	 */
	public static void checkSeqs() {
		checkSeq_0();
		checkSeq_1();
		checkSeq_2();
		checkSeq_3();
	}
	
	public static void checkSeq_0() {
		C<StringInterval> src1 = reads("0 a\t1 b");
		C<StringInterval> src2 = reads("2 c\t3 d");
		C<StringInterval> done = src1.seq(src2);
		C<StringInterval> r = reads("0 a\t1 b\t2 c\t3 d");		
		check("Seq 0 simple", done, r, cmps);
	}
	
	public static void checkSeq_1() {
		C<StringInterval> src1 = C.NULL();
		C<StringInterval> src2 = reads("2 c\t3 d");
		C<StringInterval> done = src1.seq(src2);
		C<StringInterval> r = reads("2 c\t3 d");		
		check("Seq 1 NULL left", done, r, cmps);
	}
	
	public static void checkSeq_2() {
		C<StringInterval> src1 = reads("0 a\t1 b");
		C<StringInterval> src2 = C.NULL();
		C<StringInterval> done = src1.seq(src2);
		C<StringInterval> r = reads("0 a\t1 b");		
		check("Seq 2 NULL right", done, r, cmps);
	}
	
	public static void checkSeq_3() {
		C<StringInterval> src1 = reads("0 a\t1 b");
		C<StringInterval> src2 = C.NULL();
		C<StringInterval> src3 = reads("2 c\t3 d");
		C<StringInterval> done = src1.seq(src2).seq(src3);
		C<StringInterval> r = reads("0 a\t1 b\t2 c\t3 d");		
		check("Seq 2 NULL mid", done, r, cmps);
	}

	/*
	 * Map
	 */
	public static void checkMaps() {
		checkMap_0();
		checkMap_1();
		checkMap_2();
	}

	public static void checkMap_0() {
		C<StringInterval> src = reads("0 a\t1 b\t2 c");
		C<StringInterval> done = src.map(x -> x);
		C<StringInterval> r = reads("0 a\t1 b\t2 c");
		check("Map 0 id", done, r, cmps);
	}
	public static void checkMap_1() {
		C<StringInterval> src = reads("0 a\t1 b\t2 c");
		C<StringInterval> done = src.map(x -> Event.make(x.time+1, x.value));
		C<StringInterval> r = reads("1 a\t2 b\t3 c");
		check("Map 1 change time", done, r, cmps);
	}
	public static void checkMap_2() {
		C<StringInterval> src = reads("0 a+\t1 b\t2 c-");
		C<StringInterval> done = src.map(x -> Event.make(x.time, x.value.removeAnnotation()));
		C<StringInterval> r = reads("0 a\t1 b\t2 c");
		check("Map 2 change value", done, r, cmps);
	}

	/*
	 * Delay
	 */
	public static void checkDelays() {
		checkDelay_0();
	}
	public static void checkDelay_0() {
		C<StringInterval> src = reads("0 a\t1 b\t2 c");
		Peek<StringInterval> done = Peek.make(src);
		C<StringInterval> r = reads("0 a\t1 b\t2 c");
		check("Delay 0 id", done, r, cmps);
	}

	/*
	 * filter
	 */
	static void checkFilter(String from, String to, 
			Fun1<Event<StringInterval>,Boolean> f) {
		C<StringInterval> src = reads(from);
		C<StringInterval> done = src.filter(f);
		C<StringInterval> r = reads(to);
		check("filter " + from, done, r, cmps);
	}

	public static void checkFilters() {
		checkFilter("0 a\t1 b\t2 c", "1 b", e -> (e.time == 1));
		checkFilter("0 a\t1 b\t2 c", "0 a\t1 b", e -> (e.time <= 1));
		checkFilter("0 a\t1 b\t2 c", "", e -> false);
		checkFilter("0 a-\t1 b+\t2 c-", "1 b+", e -> e.value.isEnd());
	}

	/*
	 * fold
	 */
	static void checkFold(String from, String to, 
			Fun3<Long, Vector<StringInterval>, Event<StringInterval>, Boolean> f) {
		C<StringInterval> src = reads(from);
		C<Vector<StringInterval>> done = src.fold(f);
		C<Vector<StringInterval>> r = readv(to);
		check("fold " + from, done, r, cmpv);
	}

	public static void checkFolds() {
		checkFold("0 a\t1 b\t2 c", "2 a b c", (t,v,e) -> true);
		checkFold("0 a\t1 b\t2 c", "1 a b\t2 c", (t,v,e) -> e.time <= 1 || v.size() == 0);
		checkFold("0 a\t1 b\t2 c\t3 d\t4 e", "1 a b\t3 c d\t4 e", (t,v,e) -> v.size() < 2);
	}

	/*
	 * Sync
	 */
	static void checkSync(String from, String to) {
		C<StringInterval> src = reads(from);
		C<Vector<StringInterval>> done = src.sync();
		C<Vector<StringInterval>> r = readv(to);
		check("sync " + from, done, r, cmpv);
	}
	
	public static void checkSyncs() {
		checkSync("0 a\t1 b\t2 c", "0 a\t1 b\t2 c");
		checkSync("0 a\t0 b\t2 c", "0 a b\t2 c");
		checkSync("0 a\t1 b\t1 c", "0 a\t1 b c");
		checkSync("0 a\t2 b\t2 c\t3 d", "0 a \t2 b c\t3 d");
	}
	
	/*
	 * Unfold
	 */
	static void checkUnfold(String from, String to) {
		C<Vector<StringInterval>> src = readv(from);
		C<StringInterval> done = C.unfold(src);
		C<StringInterval> r = reads(to);
		check("unfold " + from, done, r, cmps);
	}

	public static void checkUnfolds() {
		checkUnfold("0 a\t1 b\t2 c", "0 a\t1 b\t2 c");
		checkUnfold("0 a b\t2 c", "0 a\t0 b\t2 c");
		checkUnfold("0 a\t1 b c", "0 a\t1 b\t1 c");
		checkUnfold("0 a b c", "0 a\t0 b\t0 c");
		checkUnfold("0 a b\t1 b c d\t2 a c", "0 a\t0 b\t1 b\t1 c\t1 d\t2 a\t2 c");
	}

	/*
	 * Loop
	 */
	public static void checkloop() {
		C<StringInterval> done = C.loop(new Fun0<C<StringInterval>>() {
			int i = 0;
			public C<StringInterval> operation() {
				if(++i > 3) return(null);
				return(reads("0 a\t1 b"));
			}
		});
		C<StringInterval> r = reads("0 a\t1 b\t0 a\t1 b\t0 a\t1 b");
		check("loop", done, r, cmps);
	}

	/*
	 * compressBetweenOn
	 */
	static void checkCompress(String from, String to) {
		C<Vector<StringInterval>> src = readv(from);
		C<Vector<StringInterval>> done = C.compressBetweenOn(src);
		C<Vector<StringInterval>> r = readv(to);
		check("compress " + from, done, r, cmpv);
	}

	public static void checkCompresss() {
		checkCompress("", "0 ");
		checkCompress("0 ", "0 ");
		checkCompress("0 a", "0 a");
		checkCompress("0 a b", "0 a b");
		checkCompress("0 a \t1 b", "1 a b");
		checkCompress("0 a \t1 b-", "0 a \t1 b- \t2");
		checkCompress("0 a \t1 b \t2 c-", "1 a b\t2 c- \t3");
		checkCompress("0 a-", "-1 \t0 a-\t1");
		checkCompress("0 a- b", "-1 \t0 a- b\t1");
		checkCompress("0 a- \t1 b", "-1 \t0 a- \t1 b");
		checkCompress("0 a- \t2 b-", "-1 \t0 a- \t1 \t2 b- \t3");
		checkCompress("0 a- \t1 b \t2 c-", "-1 \t0 a- \t1 b \t2 c- \t3");
		checkCompress("0 a- \t1 b \t2 c \t3 d-", "-1 \t0 a- \t2 b c \t3 d- \t4");
		checkCompress("0 a \t2 b c\t3 d", "3 a b c d");
		checkCompress("0 a-\t2 b c\t3 d", "-1 \t0 a-\t3 b c d");
		checkCompress("0 a-\t10 b- c\t20 d", "-1 \t0 a- \t9 \t10 b- c \t20 d");
		checkCompress("0 a-\t10 b c-\t20 d", "-1 \t0 a- \t9 \t10 b c- \t20 d");
		checkCompress("0 a-\t10 b c\t20 d-", "-1 \t0 a- \t10 b c \t20 d- \t21");
		checkCompress("0 a-\t10 b c\t20 d\t30 e-", "-1\t0 a-\t20 b c d\t30 e- \t31");
		checkCompress("0 a\t10 b c\t20 d-\t30 e", "10 a b c \t20 d- \t30 e");
		checkCompress("0 a\t10 b c\t20 d\t30 e-", "20 a b c d \t30 e- \t31");
		checkCompress("0 a\t10 b c-\t20 d-\t30 e", "0 a\t10 b c-\t19\t20 d-\t30 e");
	}
	
	/*
	 * unmeet
	 */
	static void checkUnmeet(String from, String to) {
		C<Vector<StringInterval>> src = readv(from);
		C<Vector<StringInterval>> done = C.unmeet(src);
		C<Vector<StringInterval>> r = readv(to);
		check("unmeet " + from, done, r, cmpv);
	}
	
	public static void checkUnmeets() {
		checkUnmeet("", "");
		checkUnmeet("0 x", "0 x");
		checkUnmeet("0 x \t1 a- \t3 \t4 a+ b-", "0 x \t1 a- \t3 a+ \t4 b-");
	}
}
