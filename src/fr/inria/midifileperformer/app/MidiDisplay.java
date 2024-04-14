package fr.inria.midifileperformer.app;

import java.util.Vector;

import fr.inria.bps.base.Event;
import fr.inria.bps.base.Line;
import fr.inria.bps.base.Vecteur;
import fr.inria.fun.Proc0;
import fr.inria.fun.Proc2;
import fr.inria.lognet.sos.Shape;
import fr.inria.lognet.sos.Sos;
import fr.inria.lognet.sos.SosColor;
import fr.inria.lognet.sos.event.SosMouse;
import fr.inria.midi.MidiLib;
import fr.inria.midifileperformer.impl.MidiMsg;

public class MidiDisplay extends Shape {
	Configurator master;
	Vector<Event<MidiMsg>> events;
	Proc0 reset;
	int timeSlice = 5000;
	int timeWindow = 5000;
	int time = 0;
	int pitch = 64;
	int wpitch = 128;
	Vector<Vector<Integer>> opens;
	Vector<Integer> openss;

	public MidiDisplay(Configurator master, Vector<Event<MidiMsg>> events, 
			boolean horizontal, Proc0 reset) {
		this.master = master;
		this.events = events;
		this.horizontal = horizontal;
		this.reset = reset;
		makeOpens();
		//System.out.println("events " + events);
		//System.out.println("opens " + opens);
		//System.out.println("openss " + openss);
		Sos.listen(SosMouse.down, this, e -> down());
		Sos.listen(SosMouse.drag, this, e -> drag());
		Sos.listen(SosMouse.wheel, this, e->wheel());
		listen(SosMouse.enter, this, e -> picture.root.requestFocus());
	}
	
	void reset() {
		reset.operation();
		dirty();
	}
	
	int mx = 0;
	int my = 0;
	int mtime = 0;
	int mpitch = 0;
	void down() {
		mx = SosMouse.x;
		my = SosMouse.y;
		mtime = time;
		mpitch = pitch;
	}
	void drag() {
		if(horizontal) {
			time = new Line(mx, mtime, mx+width, mtime-timeWindow).iget(SosMouse.x);
			pitch = new Line(my, mpitch, my+height, mpitch+wpitch).iget(SosMouse.y);
		} else {
			time = new Line(my, mtime, my+height, mtime-timeWindow).iget(SosMouse.y);
			pitch = new Line(mx, mpitch, mx+width, mpitch+wpitch).iget(SosMouse.x);
		}
		reset();
	}
	
	void wheel() {
		if(SosMouse.modifier == 0) {
			int incr = new Line(0, 0, 1, 100).iget(SosMouse.how);
			timeWindow = Math.max(10, timeWindow+incr);
		} else {
			int incr = new Line(0, 0, 1, 4).iget(SosMouse.how);
			wpitch = Math.max(10, wpitch+incr);
		}
		reset();
	}
	
	public void reset(Vector<Event<MidiMsg>> events) {
		this.events = events;
		makeOpens();
		time = 0;
		reset();
	}

	int findOpen(Vector<Integer> open, MidiMsg msg) {
		//System.out.println("find open "+msg+" in "+Vecteur.map(open, i->events.get(i)));
		for(int i=0; i<open.size(); i++) {
			int index = open.get(i);
			Event<MidiMsg> event = events.get(index);
			if(MidiLib.correspond(msg.msg, event.value.msg))
				return(i);
		}
		//System.out.println("NOT FOUND " + msg + " in " + open);
		return(-1);
	}

	int followOpen(Vector<Integer> open, int i, long time,
			Proc2<Event<MidiMsg>,Event<MidiMsg>> f) {
		for( ; i<events.size(); i++) {
			Event<MidiMsg> ev = events.get(i);
			if(ev.time >= time) return(i);
			MidiMsg msg = ev.value;
			if(MidiLib.isBegin(msg.msg)) {
				open.add(i);
			} else if(MidiLib.isEnd(msg.msg)) {
				int io = findOpen(open, msg);
				if(io != -1) {
					f.operation(events.get(open.get(io)), ev);
					open.remove(io);
				} else {
					//System.out.println(" follow fail " + msg + " in " + open);
					//System.out.println(" index " + i + " time " + ev.time + " last " + time);
					f.operation(null, ev);
				}
			}
		}
		return(i);
	}

	void makeOpens() {
		opens = new Vector<Vector<Integer>>();
		openss = new Vector<Integer>();
		Vector<Integer> open = new Vector<Integer>();
		openss.add(0);
		opens.add(new Vector<Integer>(open));
		int i=0;
		long time = timeSlice;
		while(i<events.size()) {
			i = followOpen(open, i, time, (x,y)->{});
			openss.add(i);
			opens.add(new Vector<Integer>(open));
			time += timeSlice;
		}
		opens.add(open);
	}

	Vector<Integer> getOpen(long time) {
		int index = (int) (time / timeSlice);
		Vector<Integer> open = new Vector<Integer>(Vecteur.get(opens, index));
		int i = openss.get(index);
		followOpen(open, i, time, (oev,cev)-> {});
		return(open);
	}
	
	boolean horizontal = false;
	boolean reverse = false;
	boolean centered = true;
	
	public void repaint(int cx, int cy, int w, int h) {
		long w2 = timeWindow/2;
//		long btime = time - w2;
//		long etime = time + w2;
		long btime = centered ? time - w2 : time;
		long etime = centered ? time + w2 : time + timeWindow;
		int index = (int) (btime / timeSlice);
		Vector<Integer> open = new Vector<Integer>(Vecteur.get(opens, index));
		int i = Vecteur.get(openss, index);
		//		System.out.println("btime " + btime + " etime " + etime +
		//				" index " + index + " i0 " + i + " open " + open);
		i = followOpen(open, i, btime, (x,y)->{});
		//		System.out.println(" -> " + " i1 " + i + " open " + open);


		int wp2 = wpitch/2;
		Line spitch = new Line(pitch+wp2, 0, pitch-wp2, horizontal ? height : width );
		//Line intensity = new Line(128, 0, 0, horizontal ? height : width);
		int wi = Math.max(1, (horizontal ? height : width)/wpitch);
		//wi = Math.max(1, spitch.iget(index))
		int lp = reverse ? (horizontal ? width : height) : 0;
		int rp = reverse ? 0 : (horizontal ? width : height);
		Line dd = new Line(btime, lp, etime, rp);

		// A vertical bar to see the time
		int mid = dd.iget(time);
		if(horizontal) {
			drawline(SosColor.white, mid, 0, mid, height);
		} else {
			drawline(SosColor.white, 0, mid, width, mid);
		}
		// 
		followOpen(open, i, etime, (oev,cev) -> {
			//System.out.println("\t oev " + oev + " cev " + cev);
			if(oev != null) {
				int pval = spitch.iget(MidiLib.getKey(oev.value.msg));
				SosColor c = getColorEvent(oev);
				displayBox(c, dd.iget(oev.time), dd.iget(cev.time), pval, wi);
			} else {
				int pval = spitch.iget(MidiLib.getKey(cev.value.msg));
				displayPoint(SosColor.white, dd.iget(cev.time), pval, 3*wi);
				//System.out.println(" POINT " + cev + " " + dd.iget(cev.time) + " " + pval);
			}
		});
		// We have to close events in open
		for( int ind : open ) {
			Event<MidiMsg> oev = events.get(ind);
			int pval = spitch.iget(MidiLib.getKey(oev.value.msg));
			SosColor c = getColorEvent(oev);
			displayBox(c, dd.iget(oev.time), dd.iget(etime), pval, wi);
		}
	}
	

	static SosColor colors[] = SosColor.degrade(SosColor.blue, SosColor.red, 128);
	boolean showVelocity = false;
	static SosColor ccolors[] = {
			SosColor.red,
			SosColor.blue,
			SosColor.green,
			SosColor.black,
			SosColor.white,
			new SosColor(128,128,0),
			new SosColor(128,0,128),
			new SosColor(0,128,128),
			new SosColor(128,64,64),
			new SosColor(64,128,64),
			new SosColor(64,64,128),
			new SosColor(128,128,64),
			new SosColor(128,64,128),
			new SosColor(64,128,128),
	};
	SosColor getColorEvent(Event<MidiMsg> ev) {
		if(master.config.showChannel) {
			return(ccolors[MidiLib.getChannel(ev.value.msg) % ccolors.length]);
		} else {
			return(colors[MidiLib.getVelocity(ev.value.msg) % colors.length]);
		}
	}

	void displayBox(SosColor c, int t0, int t1, int pval, int wi) {
		int left = (t0<t1) ? t0 : t1;
		int width = (t0<t1) ? t1-t0 : t0-t1;
		if(horizontal) {
			fillrectangle(c, left, pval, width, wi);
			drawcircle(SosColor.white, left, pval+wi/2, 1+wi/2);
		} else {
			fillrectangle(c, pval, left, wi, width);
		}
	}

	void displayPoint(SosColor c, int t, int pval, int wi) {
		if(horizontal) {
			fillrectangle(c, t, pval, wi, wi);
		} else {
			fillrectangle(c, pval, t, wi, wi);
		}
	}

	public int wscale(boolean grow) {
		return(80);
	}

	public int hscale(boolean grow) {
		return(90);
	}

}
