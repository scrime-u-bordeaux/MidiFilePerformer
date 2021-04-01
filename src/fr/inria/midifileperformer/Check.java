package fr.inria.midifileperformer;

import java.util.Vector;

import fr.inria.fun.Fun0;
import fr.inria.fun.Fun2;
import fr.inria.midifileperformer.core.C;
import fr.inria.midifileperformer.core.Event;
import fr.inria.midifileperformer.core.Peek;

public class Check {
    public static void main(String[] args) {
	//Mains.launch(new Check(), args, 0);
	checkAll();
    }

    public static void checkAll() {
	checkBasics();
	checkMaps();
	checkDelays();
	checkFilters();
	checkFolds();
	checkSyncs();
	checkUnfolds();
    }

    static C<StringInterval> reads(String s) {
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
	Event<T> ev1 = c1.get();
	Event<T> ev2 = c2.get();
	while(ev1 != null &&  ev2 != null) {
	    if((ev1.time != ev2.time) || !cmp.operation(ev1.value, ev2.value)) {
		System.out.println("test \"" + test + "\" failed at index " + i);
		System.out.println("\tnot same event " + ev1 + " vs " + ev2);
	    }
	    i++;
	    ev1 = c1.get();
	    ev2 = c2.get();
	}
	if(ev2 != null) {
	    System.out.println("test \"" + test + "\" failed at index " + i);
	    System.out.println("not enought values : need at least " + ev2);
	}
	if(ev1 != null) {
	    System.out.println("test \"" + test + "\" failed at index " + i);
	    System.out.println("to omore values : at least " + ev1);
	}
    }

    static <T> void check(int test, C<T> c1, C<T> c2, Fun2<T,T,Boolean> cmp) {
	check(""+test, c1, c2, cmp);
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
    public static void checkFilters() {
	checkFilter_0();
	checkFilter_1();
	checkFilter_2();
	checkFilter_3();
    }

    public static void checkFilter_0() {
	C<StringInterval> src = reads("0 a\t1 b\t2 c");
	C<StringInterval> done = src.filter(e -> (e.time == 1));
	C<StringInterval> r = reads("1 b");
	check("Filter 0 time", done, r, cmps);
    }
    public static void checkFilter_1() {
	C<StringInterval> src = reads("0 a\t1 b\t2 c");
	C<StringInterval> done = src.filter(e -> (e.time <= 1));
	C<StringInterval> r = reads("0 a\t1 b");
	check("Filter 1 time range", done, r, cmps);
    }
    public static void checkFilter_2() {
	C<StringInterval> src = reads("0 a\t1 b\t2 c");
	C<StringInterval> done = src.filter(e -> false);
	C<StringInterval> r = C.NULL();
	check("Filter 2 all", done, r, cmps);
    }
    public static void checkFilter_3() {
	C<StringInterval> src = reads("0 a-\t1 b+\t2 c-");
	C<StringInterval> done = src.filter(e -> e.value.isEnd());
	C<StringInterval> r = reads("1 b+");
	check("Filter 3 value", done, r, cmps);
    }


    /*
     * fold
     */
    public static void checkFolds() {
	checkFold_0();
	checkFold_1();
	checkFold_2();
    }

    public static void checkFold_0() {
	C<StringInterval> src = reads("0 a\t1 b\t2 c");
	C<Vector<StringInterval>> done = src.fold((t,v,e) -> true);
	C<Vector<StringInterval>> r = readv("2 a b c");
	check("Fold 0 all", done, r, cmpv);
    }
    public static void checkFold_1() {
	C<StringInterval> src = reads("0 a\t1 b\t2 c");
	C<Vector<StringInterval>> done = src.fold((t,v,e) -> e.time <= 1 || v.size() == 0);
	C<Vector<StringInterval>> r = readv("1 a b\t2 c");
	check("Fold 1 firsts", done, r, cmpv);
    }
    public static void checkFold_2() {
	C<StringInterval> src = reads("0 a\t1 b\t2 c\t3 d\t4 e");
	C<Vector<StringInterval>> done = src.fold((t,v,e) -> v.size() < 2);
	C<Vector<StringInterval>> r = readv("1 a b\t3 c d\t4 e");
	check("Fold 2 slice", done, r, cmpv);
    }

    /*
     * Sync
     */
    public static void checkSyncs() {
	checkSync_0();
	checkSync_1();
	checkSync_2();
	checkSync_3();
    }

    public static void checkSync_0() {
	C<StringInterval> src = reads("0 a\t1 b\t2 c");
	C<Vector<StringInterval>> done = src.sync();
	C<Vector<StringInterval>> r = readv("0 a\t1 b\t2 c");
	check("Sync sing", done, r, cmpv);
    }
    public static void checkSync_1() {
	C<StringInterval> src = reads("0 a\t0 b\t2 c");
	C<Vector<StringInterval>> done = src.sync();
	C<Vector<StringInterval>> r = readv("0 a b\t2 c");
	check("Sync first", done, r, cmpv);
    }
    public static void checkSync_2() {
	C<StringInterval> src = reads("0 a\t1 b\t1 c");
	C<Vector<StringInterval>> done = src.sync();
	C<Vector<StringInterval>> r = readv("0 a\t1 b c");
	check("Sync last", done, r, cmpv);
    }
    public static void checkSync_3() {
	C<StringInterval> src = reads("0 a\t2 b\t2 c\t3 d");
	C<Vector<StringInterval>> done = src.sync();
	C<Vector<StringInterval>> r = readv("0 a \t2 b c\t3 d");
	check("Sync middle", done, r, cmpv);
    }

    public static void checkUnfolds() {
	checkUnfold_0();
	checkUnfold_1();
	checkUnfold_2();
	checkUnfold_3();
	checkUnfold_4();
    }

    public static void checkUnfold_0() {
	C<Vector<StringInterval>> src = readv("0 a\t1 b\t2 c");
	C<StringInterval> done = C.unfold(src);
	C<StringInterval> r = reads("0 a\t1 b\t2 c");
	check("unfold sing", done, r, cmps);
    }
    public static void checkUnfold_1() {
 	C<Vector<StringInterval>> src = readv("0 a b\t2 c");
 	C<StringInterval> done = C.unfold(src);
 	C<StringInterval> r = reads("0 a\t0 b\t2 c");
 	check("unfold first", done, r, cmps);
     }
    public static void checkUnfold_2() {
 	C<Vector<StringInterval>> src = readv("0 a\t1 b c");
 	C<StringInterval> done = C.unfold(src);
 	C<StringInterval> r = reads("0 a\t1 b\t1 c");
 	check("unfold last", done, r, cmps);
     }
    public static void checkUnfold_3() {
 	C<Vector<StringInterval>> src = readv("0 a b c");
 	C<StringInterval> done = C.unfold(src);
 	C<StringInterval> r = reads("0 a\t0 b\t0 c");
 	check("unfold all", done, r, cmps);
     }
    public static void checkUnfold_4() {
 	C<Vector<StringInterval>> src = readv("0 a b\t1 b c d\t2 a c");
 	C<StringInterval> done = C.unfold(src);
 	C<StringInterval> r = reads("0 a\t0 b\t1 b\t1 c\t1 d\t2 a\t2 c");
 	check("unfold gen", done, r, cmps);
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
	check(10, done, r, cmps);
    }

    /*
     * compressBetweenOn
     */
    public static void check30() {
	C<Vector<StringInterval>> src = readv("0 a \t2 b c\t3 d");
	C<Vector<StringInterval>> done = C.compressBetweenOn(src);
	C<Vector<StringInterval>> r = readv("3 a b c d");
	check(20, done, r, cmpv);
    }
    public static void check31() {
	C<Vector<StringInterval>> src = readv("0 a-\t2 b c\t3 d");
	C<Vector<StringInterval>> done = C.compressBetweenOn(src);
	C<Vector<StringInterval>> r = readv("-1\t0 a-\t3 b c d");
	check(31, done, r, cmpv);
    }
    public static void check32() {
	C<Vector<StringInterval>> src = readv("0 a-\t10 b- c\t20 d");
	C<Vector<StringInterval>> done = C.compressBetweenOn(src);
	C<Vector<StringInterval>> r = readv("-1\t0 a-\t9 \t10 b- c\t20 d");
	check(32, done, r, cmpv);
    }
    public static void check33() {
	C<Vector<StringInterval>> src = readv("0 a-\t10 b c-\t20 d");
	C<Vector<StringInterval>> done = C.compressBetweenOn(src);
	C<Vector<StringInterval>> r = readv("-1\t0 a-\t9 \t10 b c-\t20 d");
	check(33, done, r, cmpv);
    }
    public static void check34() {
	C<Vector<StringInterval>> src = readv("0 a-\t10 b c\t20 d-");
	C<Vector<StringInterval>> done = C.compressBetweenOn(src);
	C<Vector<StringInterval>> r = readv("-1\t0 a-\t10 b c\t20 d-");
	check(34, done, r, cmpv);
    }
    public static void check35() {
	C<Vector<StringInterval>> src = readv("0 a-\t10 b c\t20 d\t30 e-");
	C<Vector<StringInterval>> done = C.compressBetweenOn(src);
	C<Vector<StringInterval>> r = readv("-1\t0 a-\t20 b c d\t30 e-");
	check(35, done, r, cmpv);
    }
    public static void check36() {
	C<Vector<StringInterval>> src = readv("0 a-\t10 b c\t20 d-\t30 e");
	C<Vector<StringInterval>> done = C.compressBetweenOn(src);
	C<Vector<StringInterval>> r = readv("-1\t0 a-\t10 b c\t20 d-\t30 e");
	check(36, done, r, cmpv);
    }
    public static void check37() {
	C<Vector<StringInterval>> src = readv("0 a\t10 b- c\t20 d\t30 e");
	C<Vector<StringInterval>> done = C.compressBetweenOn(src);
	C<Vector<StringInterval>> r = readv("0 a\t10 b- c\t30 d e");
	check(37, done, r, cmpv);
    }
    public static void check38() {
	C<Vector<StringInterval>> src = readv("0 a\t10 b c\t20 d-\t30 e");
	C<Vector<StringInterval>> done = C.compressBetweenOn(src);
	C<Vector<StringInterval>> r = readv("10 a b c\t20 d-\t30 e");
	check(38, done, r, cmpv);
    }
    public static void check39() {
	C<Vector<StringInterval>> src = readv("0 a\t10 b c\t20 d\t30 e-");
	C<Vector<StringInterval>> done = C.compressBetweenOn(src);
	C<Vector<StringInterval>> r = readv("20 a b c d\t30 e-");
	check(39, done, r, cmpv);
    }
    public static void check40() {
	C<Vector<StringInterval>> src = readv("0 a\t10 b c-\t20 d-\t30 e");
	C<Vector<StringInterval>> done = C.compressBetweenOn(src);
	C<Vector<StringInterval>> r = readv("0 a\t10 b c-\t19\t20 d-\t30 e");
	check(40, done, r, cmpv);
    }



}
