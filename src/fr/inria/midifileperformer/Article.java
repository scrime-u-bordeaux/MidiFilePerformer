package fr.inria.midifileperformer;

import java.math.BigInteger;
import java.util.Vector;
import java.util.function.Function;

import fr.inria.bps.base.Mains;
import fr.inria.bps.base.Vecteur;

public class Article {

	public static void main(String[] args) {
		Article me = new Article();
		Mains.launch(me, args, 0);
	}
	
	/*
	 * Entries
	 */
	public void see() {
		System.out.println("nb etirement "+etire(scherzo(), 0));
		//System.out.println("nb etirement article "+aetire(scherzo()));
	}
	
	public void seeArpegeEtire(int size) {
		for(int i=0; i<size; i++) {
			Vector<Vector<String>> p = arpege(i);
			System.out.println(p + " -> " + etire(p, 0));
			System.out.println(p + " a-> " + aetire(p));
		}
	}
	
	
	public void seeArpege(int size) {
		seeTeX(size, 2*size-1, i -> arpege(i));
	}
	
	public void seeChord(int size) {
		seeTeX(size, size+1, i -> accord(i));
	}
	
	void seeTeX(int size, int max, Function<Integer,Vector<Vector<String>>> f) {
		String rs = "";
		for(int i=0; i<max; i++) rs+="r";
		System.out.println("\\begin{tabular}{r | " + rs + " | r}");
		System.out.println(header(max));
		System.out.println("\\hline");
		for(int i=0; i<size; i++) {
			Vector<Vector<String>> a =  f.apply(i); 
			Vector<Integer> v = pe(a, 0);
			System.out.println(i+" "+texLine(v, max));
		}
		System.out.println("\\hline");
		System.out.println("\\end{tabular}");
	}
	
	public void pescherzo() {
		Vector<BigInteger> r = Bpe(scherzo(), 0);
		System.out.println(Bsum(r));
	}
	
	public void chk() {
		Vector<BigInteger> r = Bpe(accord(5), 0);
		System.out.println(r);
		System.out.println(Bsum(r));
	}
	
	String header(int max) {
		String r = "";
		for(int i=0; i<max; i++) r+=i+" & ";
		return("size & "+r+" \\Sigma \\\\");
	}
	
	String texLine(Vector<Integer> v, int max) {
		String r = "";
		for(int i=0; i<v.size(); i++)
			r += v.get(i) + " & ";
		for(int i=v.size(); i<max; i++)
			r += "0 & ";
		r += sum(v) + " \\\\";
		return(r);
	}
	
	/*
	 * Lib
	 */
	int sum(Vector<Integer> v) {
		int r = 0;
		for( int x : v ) r+=x;
		return(r);
	}

	BigInteger Bsum(Vector<BigInteger> v) {
		BigInteger r = BigInteger.ZERO;
		for( BigInteger x : v ) r = r.add(x);
		return(r);
	}
	
	int etire(Vector<Vector<String>> p, int i) {
		if(i == p.size()) return(1);
		return(etire(p, i+1) * pow(2*(p.size()-i), p.get(i).size()));
	}
	
	int aetire(Vector<Vector<String>> p) {
		int n = p.size();
		int r = 1;
		for(int i=1; i<=n; i++) {
			r *= pow(2*(n-i+1), p.get(i-1).size());
		}
		return(r);
	}
	
	int pow(int x, int n) {
		if(n == 0) return(1);
		return(x*pow(x, n-1));
	}

	Vector<BigInteger> Bpe(Vector<Vector<String>> p, int i) {
		if(i == p.size()) return(Vecteur.convert(BigInteger.ONE));
		Vector<BigInteger> r = Bpe(p, i+1);
		return(Bpel(r, p.get(i).size()));
	}
	
	Vector<BigInteger> Bpel(Vector<BigInteger> done, int n) {
		if(n == 0) return(done);
		done = BpelPush(done);
		for(int i=1; i<n; i++) done = BpelInsert(done);
		//System.out.println("PEL "+done);
		return(done);
	}
	
	Vector<BigInteger> BpelInsert(Vector<BigInteger> done) {
		int n = done.size();
		Vector<BigInteger> r = new Vector<BigInteger>(n+1);
		r.add(BigInteger.ZERO);
		BigInteger bi = BigInteger.ONE;
		for(int i=1; i<n; i++) {
			BigInteger x = done.get(i).multiply(bi);
			BigInteger y = done.get(i-1).multiply(bi.subtract(BigInteger.ONE));
			//r.add((i-1)*done.get(i-1)+i*done.get(i));
			r.add(x.add(y));
			bi = bi.add(BigInteger.ONE);
		}
		BigInteger mm = done.get(n-1).multiply(bi.subtract(BigInteger.ONE));
		//r.add((n-1)*done.get(n-1));
		r.add(mm);
		//System.out.println("PELin "+r);
		return(r);
	}
	
	Vector<BigInteger> BpelPush(Vector<BigInteger> done) {
		int n = done.size();
		Vector<BigInteger> r = new Vector<BigInteger>(n+2);
		r.add(BigInteger.ZERO);
		r.add(done.get(0));
		BigInteger pbi = BigInteger.ONE;
		BigInteger bi = pbi.add(BigInteger.ONE);
		for(int i=2; i<n+1; i++) {
			r.add(pbi.multiply(done.get(i-2).add(bi.multiply(done.get(i-1)))));
			//r.add((i-1)*done.get(i-2)+i*done.get(i-1));
			pbi = bi;
			bi = pbi.add(BigInteger.ONE);
		}
		//r.add(n*done.get(n-1));
		r.add(pbi.multiply(done.get(n-1)));
		//System.out.println("PELpush "+r);
		return(r);
	}

	Vector<Integer> pe(Vector<Vector<String>> p, int i) {
		if(i == p.size()) return(Vecteur.convert(1));
		Vector<Integer> r = pe(p, i+1);
		return(pel(r, p.get(i).size()));
	}
	
	Vector<Integer> pel(Vector<Integer> done, int n) {
		if(n == 0) return(done);
		done = pelPush(done);
		for(int i=1; i<n; i++) done = pelInsert(done);
		//System.out.println("PEL "+done);
		return(done);
	}
	
	Vector<Integer> pelInsert(Vector<Integer> done) {
		int n = done.size();
		Vector<Integer> r = new Vector<Integer>(n+1);
		r.add(0);
		for(int i=1; i<n; i++)
			r.add((i-1)*done.get(i-1)+i*done.get(i));
		r.add((n-1)*done.get(n-1));
		//System.out.println("PELin "+r);
		return(r);
	}
	
	Vector<Integer> pelPush(Vector<Integer> done) {
		int n = done.size();
		Vector<Integer> r = new Vector<Integer>(n+2);
		r.add(0);
		r.add(done.get(0));
		for(int i=2; i<n+1; i++)
			r.add((i-1)*done.get(i-2)+i*done.get(i-1));
		r.add(n*done.get(n-1));
		//System.out.println("PELpush "+r);
		return(r);
	}
	
	Vector<Vector<String>> accord(int n) {
		Vector<String> a = new Vector<String>(n);
		for(int i=0; i<n; i++) a.add(""+i);
		return(Vecteur.sing(a));
	}

	Vector<Vector<String>> arpege(int n) {
		Vector<Vector<String>> r = new Vector<Vector<String>>(n);
		for(int i=0; i<n; i++) r.add(Vecteur.sing(""+i));
		return(r);
	}
	
	Vector<Vector<String>> scherzo() {
		return(Vecteur.convert(
				Vecteur.convert("a4"),
				Vecteur.convert("b4"),
				Vecteur.convert("f4", "a4", "c5"),
				Vecteur.convert("d5"),
				Vecteur.convert("e4", "g4", "e5"),
				Vecteur.convert("g3", "b4"),
				Vecteur.convert("a3", "c5"),
				Vecteur.convert("f3", "a4", "f5"),
				Vecteur.convert("b3", "a4", "d5"),
				Vecteur.convert("e3", "g4", "e5")
				));
	}
	
	public Vector<Vector<String>> ascherzo() {
		return(Vecteur.convert(
				Vecteur.convert("a"),
				Vecteur.convert("b"),
				Vecteur.convert("c", "d", "e"),
				Vecteur.convert("f"),
				Vecteur.convert("g", "h", "i"),
				Vecteur.convert("j", "k"),
				Vecteur.convert("l", "m"),
				Vecteur.convert("n", "o", "p"),
				Vecteur.convert("q", "r", "s"),
				Vecteur.convert("t", "u", "v")
				));
	}


}
