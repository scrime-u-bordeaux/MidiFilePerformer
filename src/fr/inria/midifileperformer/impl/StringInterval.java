package fr.inria.midifileperformer.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Vector;

import fr.inria.midifileperformer.core.C;
import fr.inria.midifileperformer.core.Interval;
import fr.inria.midifileperformer.core.Merger;
import fr.inria.midifileperformer.core.SameEvent;

public class StringInterval implements Interval, SameEvent<StringInterval> {
	public String data;

	public static StringInterval META = new StringInterval("META");

	public StringInterval(String data) {
		this.data = data;
	}

	public boolean isBegin() {
		return(data.endsWith("-"));
	}

	public boolean isEnd() {
		return(data.endsWith("+"));
	}

	public String prefix() {
		if(isBegin() || isEnd()) return(data.substring(0, data.length()-1));
		return(data);
	}

	public StringInterval removeAnnotation() {
		return(new StringInterval(prefix()));
	}

	public boolean correspond(StringInterval s) {
		return(prefix().equals(s.prefix()));
	}

	public static Merger<StringInterval,MidiMsg,StringInterval> mergeMidi = 
			new Merger<StringInterval,MidiMsg,StringInterval>() {
		public StringInterval merge(StringInterval x1, MidiMsg x2) {
			return(x1);
		}
		public StringInterval left(StringInterval x1) {
			return(x1);
		}
		public StringInterval right(MidiMsg x2) {
			return(META);
		}
	};

	/*
	 * Parsing
	 */
	public static C<Vector<StringInterval>> read(String filename) {
		Vector<Vector<StringInterval>> v = parse(filename);
		return(C.make(v, 10));
	}

	public static Vector<Vector<StringInterval>> parse(String filename) {
		Vector<Vector<StringInterval>> r = new Vector<Vector<StringInterval>>();
		File file = new File(filename);
		try {
			FileInputStream in = new FileInputStream(file);
			BufferedReader d = new BufferedReader(new InputStreamReader(in));
			String line = d.readLine();
			while(line != null) {
				if(line.length() == 0) {
					r.add(new Vector<StringInterval>());
				} else {
					Vector<StringInterval> events = parseLine(line.split(" "));
					r.add(events);
				}
				line = d.readLine();
			}
			in.close();
		} catch (Exception e) {
			throw(new RuntimeException(e));
		}
		return(r);
	}

	public static Vector<StringInterval> parseLine(String[] words) {
		Vector<StringInterval> r = new Vector<StringInterval>();
		int n = words.length;
		if(n == 0) return(r);
		for(int i=0; i<n; i++) r.add(new StringInterval(words[i]));
		return(r);
	}

	public String toString() {
		return(data);
	}

}
