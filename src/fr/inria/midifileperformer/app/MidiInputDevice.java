package fr.inria.midifileperformer.app;

import java.util.concurrent.LinkedBlockingQueue;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.Transmitter;

import fr.inria.midifileperformer.Midi;
import fr.inria.midifileperformer.MidiMsg;

public class MidiInputDevice extends InputDevice {
	MidiDevice dev;

	public MidiInputDevice(MidiDevice dev) {
		this.dev = dev;
	}

	public boolean same(InputDevice dev) {
		if(dev instanceof MidiInputDevice) return(this.dev == ((MidiInputDevice) dev).dev);
		return(false);
	}

	public void accept(LinkedBlockingQueue<MidiMsg> queue) {
		try {
			Transmitter transmitter = dev.getTransmitter();
			dev.open();
			Midi.transmit(transmitter, queue);
		} catch(Exception e) {
			return;
		}
	}

	public void close() {
		// When closing a MidiDevice we can't reopen it at all...
		//System.out.println("Closing " + this);
		//dev.close();
	}

	public String toString() {
		return(dev.getDeviceInfo().getName());
	}
}
