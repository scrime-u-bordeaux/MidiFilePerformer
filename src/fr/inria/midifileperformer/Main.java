package fr.inria.midifileperformer;

import java.util.Vector;

import fr.inria.bps.base.Mains;
import fr.inria.midifileperformer.core.C;
import fr.inria.midifileperformer.core.Event;
import fr.inria.midifileperformer.core.Rendering;

public class Main {
    public static void main(String[] args) {
	//mergeOn("/Users/bserpett/Desktop/Papers/Expressivity/Midi/bach_846.mid", 1, 0);
	//testjh("/Users/bserpett/Desktop/Papers/Expressivity/Midi/test.sev");
	Mains.launch(new Main(), args, 0);
    }
     
    /*
     * Example
     */
    
    static Vector<String> dataString = new Vector<String>();
    static {
	dataString.add("da");
	dataString.add("re");
	dataString.add("sofl");
    }
    public static void stepString(int ms) {
	C<String> c1 = C.make(dataString, 50);
	C<Void> c2 = C.clock(ms, null);
	C<String> r = Lib.trace(Rendering.stepBy(c1, c2));
	r.force();
    }
    
    public static void stepMidi(String filename, int ms, int out) {
	C<MidiMsg> c1 = Midi.readMidi(filename);
	C<Void> c2 = C.clock(ms, null);
	Midi.synthesize(Rendering.stepBy(c1, c2), out);
    }
     
    /*
     * beginEnd
     */   
    public static void beginEnd(String filename, int in, int out) {
	
	
	C<MidiMsg> cin = Midi.readMidi(filename);
	C<Vector<MidiMsg>> c1 = Lib.stdAnalysis(cin, true);
	
  	C<MidiMsg> c2 = Midi.keyboard(in);
  	
 	C<Vector<MidiMsg>> r = Rendering.mergeBegin(c1, c2, MidiMsg.merge);
 	  	
  	Midi.synthesize(C.unfold(r), out);
  	System.exit(0);
      }
     
    public static void schedule_sev(String filename, int in) {
	C<Vector<StringInterval>> c1 = StringInterval.read(filename);
 	C<MidiMsg> c2 = Midi.keyboard(in);
 	Lib.trace(Rendering.mergeBegin(c1, c2, StringInterval.mergeMidi)).force();
 	//System.exit(0);
    }
    
    public static void midiInOut(int in, int out) {
	Midi.synthesize(Lib.trace(Midi.keyboard(in)), out);
    }
    
    public static void convertSimple(String filename) {
	C<MidiMsg> c = Midi.readMidi(filename);
	Event<MidiMsg> e = c.get();
	while(e != null) {
	    MidiMsg m = e.value;
	    if(m.isBegin()) {
		System.out.println(e.time + " " + MidiMsg.note(m.getKey()) + "-");
	    } else if(m.isEnd()) {
		System.out.println(e.time + " " + MidiMsg.note(m.getKey()) + "+");
	    }
	    e = c.get();
	}
  }
 
 }
