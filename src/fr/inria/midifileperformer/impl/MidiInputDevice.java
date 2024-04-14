package fr.inria.midifileperformer.impl;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Transmitter;

public class MidiInputDevice extends InputDevice {
	MidiDevice dev;

	public MidiInputDevice(String name, MidiDevice dev) {
		super(name);
		this.dev = dev;
	}

	public boolean same(InputDevice dev) {
		if(dev instanceof MidiInputDevice) return(this.dev == ((MidiInputDevice) dev).dev);
		return(false);
	}

	public static void reset() {
		MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
		for(int i=0; i<infos.length; i++) {
			try {
				MidiDevice md = MidiSystem.getMidiDevice(infos[i]);
				if(md.getMaxTransmitters() != 0) {
					String name = md.getDeviceInfo().getName();
					devices.put(name, new MidiInputDevice(name, md));
				}
			} catch(Exception e) {}
		}
	}
	
	public void open() {
		super.open();
		try {
			Transmitter transmitter = dev.getTransmitter();
			dev.open();
			Midi.transmit(transmitter, m -> send(m));
		} catch(Exception e) {
			return;
		}
	}

	public void close() {
		super.close();
		// When closing a MidiDevice we can't reopen it at all...
		//System.out.println("Closing " + this);
		//dev.close();
	}
}
