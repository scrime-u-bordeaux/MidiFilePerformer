package fr.inria.midifileperformer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Vector;

import fr.inria.midifileperformer.core.C;
import fr.inria.midifileperformer.core.Event;

public class SimpleMidiFile {

    /*
     * Parsing
     */
    public static C<MidiMsg> read(String filename) {
	Vector<Event<MidiMsg>> v = parse(filename);
	return(C.make(v));
    }

    public static Vector<Event<MidiMsg>> parse(String filename) {
	Vector<Event<MidiMsg>> r = new Vector<Event<MidiMsg>>();
	File file = new File(filename);
	try {
	    FileInputStream in = new FileInputStream(file);
	    BufferedReader d = new BufferedReader(new InputStreamReader(in));
	    String line = d.readLine();
	    while(line != null) {
		r.add(parseLine(line));
		line = d.readLine();
	    }
	    d.close();
	    in.close();
	} catch (Exception e) {
	    throw(new RuntimeException(e));
	}
	return(r);
    }
    
    static Event<MidiMsg> parseLine(String line) {
	String[] words = line.split(" ");
	int m = words.length;
	if(m < 2) throw new RuntimeException("empty line");
	String linen = words[0];
	long time = Long.parseLong(linen);
	return(Event.make(time, toMidi(words[1])));
    }
   
    static MidiMsg toMidi(String s) {
	if(!isBegin(s) && !isEnd(s))
	    throw(new RuntimeException("not a valid note " + s));
	boolean begin = isBegin(s);
	int note = noteIndex(prefix(s));
	if(begin) return(MidiMsg.NoteOn(note, 100));
	return(MidiMsg.NoteOff(note, 0));
    }
    
    public static final String[] NOTE_ENAMES = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
    public static final String[] NOTE_FNAMES = {"Do", "Do#", "Re", "Mib", "Mi", "Fa", "Fa#", "Sol", "Sol#", "La", "Sib", "Si"};
    String note(int k) {
	int octave = (k / 12)-1;
	int note = k % 12;
	String noteName = NOTE_FNAMES[note];
	return(noteName + octave);
    }
   static int noteIndex(String s) {
       int index = nameIndex(prefix(s));
       int octave = octave(s.substring(s.length()-1, s.length()));
       return((octave+1)*12 + index);
   }
   static int nameIndex(String s) {
       for(int i=0; i<12; i++) {
	   if(s.equalsIgnoreCase(NOTE_ENAMES[i]) || s.equalsIgnoreCase(NOTE_FNAMES[i]))
	       return(i);
       }
       throw(new RuntimeException("invalid note name : " + s));
   }
   static int octave(String s) {
       return(Integer.parseInt(s));
   }

    static boolean isBegin(String data) {
	return(data.endsWith("-"));
    }

    static boolean isEnd(String data) {
	return(data.endsWith("+"));
    }
    
    static String prefix(String data) {
	return(data.substring(0, data.length()-1));
    }
    
    static Vector<Event<MidiMsg>> convert(String[] v) {
	Vector<Event<MidiMsg>> r = new Vector<Event<MidiMsg>>();
	for(String line : v)
	    r.add(parseLine(line));
	return(r);
    }
    
    public static C<MidiMsg> demo() {
	    return(C.make(convert(new String[] {
		    "0 La3-", "0 Do4-", "0 Fa4-", "0 Re3-", "1000 La3+", "1000 Do4+", "1000 Fa4+",
		    "1000 La3-", "1000 Do4-", "1000 Fa4-", "2000 La3+", "2000 Do4+",
		    "2000 Fa4+", "2000 Fa4-", "2000 Do4-", "2000 La3-", "3000 Fa4+", "3000 Do4+",
		    "3000 La3+", "3000 La3-", "3000 Do4-", "3000 Fa4-", "3000 Re3+", "3250 Fa2-",
		    "3500 La3+", "3500 Do4+", "3500 Fa4+", "3500 Fa2+", "3500 Fa#2-", "3750 La3-",
		    "4000 La3+", "4000 Si3-", "4000 Re4-", "4000 Fa4-", "4000 Fa#2+", "4000 Sol2-",
		    "5000 Si3+", "5000 Re4+", "5000 Fa4+", "5000 Si3-", "5000 Re4-", "5000 Fa4-",
		    "6000 Si3+", "6000 Re4+", "6000 Fa4+", "6000 Si3-", "6000 Re4-", "6000 Fa4-",
		    "6000 Sol2+", "6250 Re3-", "6500 Si3+", "6500 Re4+", "6500 Fa4+", "6500 Si3-",
		    "7000 Si3+", "7000 Si3-", "7000 Re4-", "7000 Fa4-", "7500 Si3+", "7500 Re4+",
		    "7500 Fa4+", "7500 Si3-", "8000 Si3+", "8000 Sol3-", "8000 Si3-", "8000 Mi4-",
		    "8000 Re3+", "8000 Do3-", "9000 Sol3+", "9000 Si3+", "9000 Mi4+", "9000 Sol3-",
		    "9000 Si3-", "9000 Mi4-", "10000 Sol3+", "10000 Si3+", "10000 Mi4+", "10000 Sol3-",
		    "10000 Si3-", "10000 Mi4-", "11000 Sol3+", "11000 Si3+", "11000 Mi4+", "11000 Mi4-",
		    "11000 Si3-", "11000 Sol3-", "11000 Do3+", "11250 Mi2-", "11500 Mi4+", "11500 Si3+",
		    "11500 Sol3+", "11500 Sol3-", "12000 Sol3+", "12000 Fa3-", "12000 La3-", "12000 Do4-",
		    "12000 Mi2+", "12000 Fa2-", "13000 Fa3+", "13000 La3+", "13000 Do4+", "13000 Fa3-",
		    "13000 La3-", "13000 Do4-", "14000 Fa3+", "14000 La3+", "14000 Do4+", "14000 Fa3-",
		    "14000 La3-", "14000 Do4-", "15000 Fa3+", "15000 La3+", "15000 Do4+", "15000 Do4-",
		    "15000 La3-", "15000 Fa3-", "15000 Fa2+", "15500 Fa#2-", "16000 Do4+", "16000 La3+",
		    "16000 Fa3+", "16000 Re3-", "16000 Sol3-", "16000 Si3-", "16000 Fa#2+", "16000 Sol2-",
		    "17000 Re3+", "17000 Sol3+", "17000 Si3+", "17000 Re3-", "17000 Sol3-", "17000 Si3-",
		    "18000 Re3+", "18000 Sol3+", "18000 Si3+", "20000 Sol2+", "21000 Mi3-", "22000 La3-",
		    "22000 Re4-", "22000 Fa4-", "22000 Mi3+", "22000 Fa3-", "23000 La3+", "23000 Re4+",
		    "23000 Fa4+", "23000 Fa4-", "23000 Re4-", "23000 La3-", "24000 Fa4+", "24000 Re4+",
		    "24000 La3+", "24000 Si3-", "24000 Re4-", "24000 Fa4-", "25000 Si3+", "25000 Re4+",
		    "25000 Fa4+", "25000 Si3-", "25000 Re4-", "25000 Fa4-", "25000 Fa3+", "25250 Sol3-",
		    "26000 Si3+", "26000 Re4+", "26000 Fa4+", "26000 Si3-", "26000 Re4-", "26000 Fa4-",
		    "27000 Si3+", "27000 Re4+", "27000 Fa4+", "27000 Si3-", "27000 Re4-", "27000 Fa4-",
		    "27000 Sol3+", "27000 Re3-", "28000 Si3+", "28000 Re4+", "28000 Fa4+", "28000 Mi4-",
		    "28000 Si3-", "28000 Sol3-", "28000 Re3+", "28000 Mi3-", "29000 Mi4+", "29000 Si3+",
		    "29000 Sol3+", "29000 Sol3-", "29000 Si3-", "29000 Mi4-", "30000 Sol3+", "30000 Si3+",
		    "30000 Mi4+", "30000 Sol3-", "30000 Si3-", "30000 Mi4-", "31000 Sol3+", "31000 Si3+",
		    "31000 Mi4+", "31000 Sol3-", "31000 Si3-", "31000 Mi4-", "31000 Mi3+", "31250 Do#3-",
		    "31500 Sol3+", "31500 Si3+", "31500 Mi4+", "31500 Sol3-", "32000 Sol3+", "32000 Do4-",
		    "32000 La3-", "32000 Fa#3-", "32000 Do#3+", "32000 Re3-", "33000 Do4+", "33000 La3+",
		    "33000 Fa#3+", "33000 Fa#3-", "33000 La3-", "33000 Do4-", "34000 Fa#3+", "34000 La3+",
		    "34000 Do4+", "34000 Fa#3-", "34000 La3-", "34000 Do4-", "34750 Fa#3+", "34750 La3+",
		    "34750 Do4+", "34750 Fa#3-", "35000 Fa#3+", "35000 Sib3-", "35000 Re3+", "35250 Mib3-",
		    "35500 Sib3+", "35500 Fa#3-", "36000 Fa#3+", "36000 Sol3-", "36000 La3-", "36000 Si3-",
		    "36000 Re4-", "36000 Mib3+", "36000 Sol2-", "40000 Sol3+", "40000 La3+", "40000 Re4+",
		    "40000 Si3+", "40000 Sol2+",
	    })));
    }
}
