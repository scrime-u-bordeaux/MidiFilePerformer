package fr.inria.midifileperformer.app;

import java.util.Vector;

import fr.inria.bps.base.Event;
import fr.inria.lognet.sos.Shape;
import fr.inria.lognet.sos.Sos;
import fr.inria.lognet.sos.shape.ScrollBar;
import fr.inria.lognet.sos.shape.Wrapper;
import fr.inria.midifileperformer.impl.MidiMsg;

public class MidiScrollDisplay extends Wrapper {
	MidiDisplay display;
	ScrollBar sh;
	ScrollBar sv;
	ScrollBar zh;
	ScrollBar zv;

	public MidiScrollDisplay(Configurator master, Vector<Event<MidiMsg>> events, boolean horizontal) {
		display = new MidiDisplay(master, events, horizontal, () -> reset());
		sh = Sos.hscroll(0, 100, 10, 0, x -> scrollX(x));
		zh = Sos.hscroll(0, 100, 10, 1, x -> zoomX(x));
		sv = Sos.vscroll(0, 128, 1, 64, x -> scrollY(x));
		zv = Sos.vscroll(32, 256, 10, 128, x -> zoomY(x));
		shape = Sos.column(2, new Shape[] {
				Sos.row(2, Sos.column(2, sv, zv), display),
				Sos.row(2, sh, zh),
		});
	}

	public void scrollX(int x) {
		display.time = x;
		dirty();
	}

	public void scrollY(int x) {
		display.pitch = x;
		dirty();
	}

	public void zoomX(int x) {
		display.timeWindow = x;
		dirty();
	}

	public void zoomY(int x) {
		display.wpitch = x;
		dirty();
	}
	
	public void reset() {
		int max = maxTime();
		sh.reset(0, max, display.timeWindow/2, display.time);
		zh.reset(5000, max, max/20, display.timeWindow);
		sv.reset(0, 128, 1, display.pitch);
		zv.reset(32, 256, 10, display.wpitch);
	}
	
	public int maxTime() {
		Vector<Event<MidiMsg>> events = display.events;
		int n = events.size(); 
		if(n == 0) return(1);
		return((int) events.get(n-1).time);
	}
}
