package fr.inria.midifileperformer.app;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.Receiver;

import fr.inria.midifileperformer.MidiMsg;
import fr.inria.midifileperformer.core.Event;

public class MidiOutputDevice extends OutputDevice {
	MidiDevice dev;
	Receiver receiver;
	long delay = -1;

	public MidiOutputDevice(MidiDevice dev) {
		this.dev = dev;
	}

	public boolean same(OutputDevice dev) {
		if(dev instanceof MidiOutputDevice) return(this.dev == ((MidiOutputDevice) dev).dev);
		return(false);
	}

	public void open() {
		try {
			receiver = dev.getReceiver();
			dev.open();
			delay = -1;
		} catch(Exception e) {
			return;
		}
	}

	public void accept(Event<MidiMsg> event) {
		if(delay == -1) {
			delay = System.currentTimeMillis() - event.time;
		}
		long toSleep = event.time + delay - System.currentTimeMillis();
		//System.out.println(event.time + " + "  + delay + " - " + System.currentTimeMillis() + " = " + toSleep);
		if(toSleep > 10) {
			try {
				Thread.sleep(toSleep);
			} catch (InterruptedException e) {
				// Thread is dying
				throw(new RuntimeException(e));
			}
		}
		//System.out.println("MIDI " + event);
		receiver.send(event.value.msg, -1);
	}

	public void close() {
		dev.close();
	}

	public String toString() {
		return(dev.getDeviceInfo().getName());
	}
}
