package fr.inria.midifileperformer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Vector;

import fr.inria.midifileperformer.core.C;
import fr.inria.midifileperformer.impl.MidiMsg;

public class JeanHaury {
	/* conversion vers Midi où les lignes sont numérotées de 2 en 2
     t:[] i p
     		2*t:On(i,p,1)
     		2*t+1:Off(i,p)
     t:[< i p
     		t:On(i,p,1)
     t:[> i p
     		t:Off(i,p)
     t:]> i p
     		t+1:Off(i,p)
     t[<> i p
     		t:On(i,p,1)
     		t+epsilon:Off(i,p)
	 */

	public static C<Vector<MidiMsg>> convert(C<Vector<JHStatus>> c) {
		throw(new RuntimeException("Not yet!"));
	}

	public static C<Vector<JHStatus>> read(String filename) {
		Vector<Vector<JHStatus>> v = parse(filename);
		return(C.make(v, 10));
	}

	public static Vector<Vector<JHStatus>> parse(String filename) {
		Vector<Vector<JHStatus>> r = new Vector<Vector<JHStatus>>();
		File file = new File(filename);
		try {
			FileInputStream in = new FileInputStream(file);
			BufferedReader d = new BufferedReader(new InputStreamReader(in));
			String line = d.readLine();
			while(line != null) {
				Vector<JHStatus> events = parseLine(line.split(" "));
				if(events.size() > 0) r.add(events);
				line = d.readLine();
			}
			in.close();
		} catch (Exception e) {
			throw(new RuntimeException(e));
		}
		return(r);
	}

	public static Vector<JHStatus> parseLine(String[] words) {
		Vector<JHStatus> r = new Vector<JHStatus>();
		int n = words.length;
		if(n == 0) return(r);
		Integer.parseInt(words[0]);
		for(int i=1; i<n; i++) {
			String key = words[i];
			if(key.startsWith("[]")) {
				r.add(new NoteDePas(Integer.parseInt(words[i+1]), Integer.parseInt(words[i+2])));
				i+=2;
			} else if(key.startsWith("[<>")) {
				r.add(new NoteEchappee(Integer.parseInt(words[i+1]), Integer.parseInt(words[i+2])));
				i+=2;
			} else if(key.startsWith("[<")) {
				r.add(new NoteTenue(Integer.parseInt(words[i+1]), Integer.parseInt(words[i+2])));
				i+=2;
			} else if(key.startsWith("[>")) {
				r.add(new NoteArreteeDebut(Integer.parseInt(words[i+1]), Integer.parseInt(words[i+2])));
				i+=2;
			} else if(key.startsWith("[<")) {
				r.add(new NoteArreteeFin(Integer.parseInt(words[i+1]), Integer.parseInt(words[i+2])));
				i+=2;
			} else if(key.startsWith("Ofs")) {
				r.add(new Ofs(Integer.parseInt(words[i+1]), Integer.parseInt(words[i+2])));
				i+=2;
			}
		}
		return(r);
	}

}

class JHStatus {
}
class JHChannel extends JHStatus {
	int channel;

	JHChannel(int channel) {
		this.channel = channel;
	}
}

class JHNote extends JHChannel {
	int pitch;

	JHNote(int channel, int pitch) {
		super(channel);
		this.pitch = pitch;
	}
}
class NoteDePas extends JHNote {
	NoteDePas(int channel, int pitch) {
		super(channel, pitch);
	}
	public String toString() {
		return("[] " + channel + " " + pitch);
	}
}
class NoteEchappee extends JHNote {
	NoteEchappee(int channel, int pitch) {
		super(channel, pitch);
	}
	public String toString() {
		return("[<> " + channel + " " + pitch);
	}
}
class NoteTenue extends JHNote {
	NoteTenue(int channel, int pitch) {
		super(channel, pitch);
	}
	public String toString() {
		return("[< " + channel + " " + pitch);
	}
}
class NoteArreteeDebut extends JHNote {
	NoteArreteeDebut(int channel, int pitch) {
		super(channel, pitch);
	}
	public String toString() {
		return("[> " + channel + " " + pitch);
	}
}
class NoteArreteeFin extends JHNote {
	NoteArreteeFin(int channel, int pitch) {
		super(channel, pitch);
	}
	public String toString() {
		return("]> " + channel + " " + pitch);
	}
}
class JHVelocity extends JHChannel {
	int velocity;

	JHVelocity(int channel, int velocity) {
		super(channel);
		this.velocity = velocity;
	}
}

class Ofs extends JHVelocity {
	Ofs(int channel, int velocity) {
		super(channel, velocity);
	}
	public String toString() {
		return("0fs " + channel + " " + velocity);
	}
}