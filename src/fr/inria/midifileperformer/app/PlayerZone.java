package fr.inria.midifileperformer.app;

import java.util.Vector;
import java.util.concurrent.LinkedBlockingQueue;

import fr.inria.bps.base.Line;
import fr.inria.fun.Proc0;
import fr.inria.lognet.sos.Picture;
import fr.inria.lognet.sos.Shape;
import fr.inria.lognet.sos.Sos;
import fr.inria.lognet.sos.SosColor;
import fr.inria.lognet.sos.event.SosMouse;
import fr.inria.midifileperformer.MidiMsg;
import fr.inria.midifileperformer.core.Event;

public class PlayerZone extends Shape {
    private LinkedBlockingQueue<MidiMsg> pool = null;
    public LinkedBlockingQueue<Proc0> todo = new LinkedBlockingQueue<Proc0>();
    Vector<Event<MidiMsg>> toSee = new Vector<Event<MidiMsg>>();

    public PlayerZone(int w, int h) {
	this.width = w;
	this.height = h;
	listen(SosMouse.enter, this, e -> picture.root.requestFocus());
	//Sos.listen(SosMouse.down, this, e -> down());
	//Sos.listen(SosMouse.up, this, e -> up());
	Sos.listen(fr.inria.lognet.sos.Event.keyboard, this, e -> down());
	Sos.listen(fr.inria.lognet.sos.Event.unkeyboard, this, e -> up());
    }

    public void init(Picture p, Shape father) {
	super.init(p, father);
	/*
	new Thread() {
	    public void run() {
		try {
		    Thread.sleep(1000);
		    todo.put(() -> step());
		} catch (Exception e) {
		}
	    }
	}.start();
	*/
    }

    void step() {
    }

    void down() {
	try {
	    int n = fr.inria.lognet.sos.Event.key;
	    //System.out.println("key " + n);
	    pool.put(MidiMsg.NoteOn(Math.min(n, 127), getVelocity()));
	} catch (Exception e) {
	    throw(new RuntimeException(e));
	}
    }

    void up() {
	try {
	    int n = fr.inria.lognet.sos.Event.key;
	    //System.out.println("unkey " + n);
	    pool.put(MidiMsg.NoteOff(Math.min(n, 127), getVelocity()));
	} catch (Exception e) {
	    throw(new RuntimeException(e));
	}
    }

    int getPitch() {
	return(64);
    }

    int getVelocity() {
	Line d = new Line(y, 128, y+height, 40);
	return(d.iget(SosMouse.y));
    }

    public void repaint(int cx, int cy, int w, int h) {
	Line lx = new Line(0, 10, 1, width-10);
	Line ly = new Line(0, 10, 1, height-10);
	picture.fillrectangle(SosColor.blue, x+lx.iget(Math.random()), y+ly.iget(Math.random()), 10, 10);
	/*
	if(lastEvent != null) {
	    long time = lastEvent.time;
	    MidiMsg msg = lastEvent.value;
	    if(msg.isBegin()) {
		Line line = new Line(0, height, 128, 0);
		int p = msg.getKey();
		System.out.println("key " + p);
		picture.fillrectangle(SosColor.blue, line.iget(p), 0, 10, 10);
	    }
	    //int dx = (int) (lastEvent.time % width);
	}
	*/
	//picture.root.FullRepaint();
    }

    /*
     * Use it as an Input Device
     */
    public void accept(LinkedBlockingQueue<MidiMsg> queue) {
	pool = queue;
    }

    public void close() {
    }

    /*
     * Use it as an Output Device
     */
    public void display(Event<MidiMsg> event) {
	
	//System.out.println("receive " + event);
	//lastEvent = event;
	dirty();
    }
}
