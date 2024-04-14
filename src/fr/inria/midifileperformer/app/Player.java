package fr.inria.midifileperformer.app;

import java.util.Vector;
import java.util.concurrent.LinkedBlockingQueue;

import fr.inria.bps.base.Event;
import fr.inria.fun.Proc1;
import fr.inria.midifileperformer.impl.MidiMsg;
import fr.inria.midifileperformer.impl.OutputDevice;

public class Player {
	Vector<OutputDevice> outputs;
	Vector<Event<MidiMsg>> toPlay;
	Proc1<Long> step;
	public boolean stop = false;
	public boolean quit = false;
	public boolean finish = false;
	public LinkedBlockingQueue<Boolean> sync = new LinkedBlockingQueue<Boolean>();

	
	public Player(Vector<OutputDevice> outputs, Vector<Event<MidiMsg>> toPlay, Proc1<Long> step) {
		this.outputs = outputs;
		this.toPlay = toPlay;
		this.step = step;
	}
	
	public static Player launch(Vector<OutputDevice> outputs, Vector<Event<MidiMsg>> toPlay, Proc1<Long> step) {
		Player r = new Player(outputs, toPlay, step);
		new Thread() {
			public void run() {
				r.run();
			}
		}.start();
		return(r);
	}
	
	public void run() {
		if(toPlay.size() == 0) return;
		long delay = System.currentTimeMillis() - MidiMsg.firstOn(toPlay);
		try {
			int n = toPlay.size();
			for(int i=0; i<n; i++) {
				Event<MidiMsg> event = toPlay.get(i);
				long time = event.time;
				if(stop) {
					sync.take();
					delay = System.currentTimeMillis()-time;
				}
				if(quit) return;
				long toSleep = time + delay - System.currentTimeMillis();
				//step.operation(MidiMsg.nextOn(toPlay, i));
				//System.out.println("to sleep "+toSleep);
				if(toSleep > 10) Thread.sleep(toSleep);
				for(OutputDevice dev : outputs) dev.accept(event);
				if(event.value.isBegin())
					step.operation(event.time);
				
			}
		} catch (Exception e) {
		}
		finish=true;
	}

}
