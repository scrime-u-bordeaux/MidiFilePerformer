package fr.inria.midifileperformer.impl;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Receiver;

import fr.inria.bps.base.Event;

public class MidiOutputDevice extends OutputDevice {
	MidiDevice dev;
	Receiver receiver;

	public MidiOutputDevice(String name, MidiDevice dev) {
		super(name);
		this.dev = dev;
	}

	public static void reset() {
		MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
		for(int i=0; i<infos.length; i++) {
			try {
				MidiDevice md = MidiSystem.getMidiDevice(infos[i]);
				if(md.getMaxReceivers() != 0) {
					String name = md.getDeviceInfo().getName();
					devices.put(name, new MidiOutputDevice(name, md));
				}
			} catch(Exception e) {}
		}
	}

	public boolean same(OutputDevice dev) {
		if(dev instanceof MidiOutputDevice) return(this.dev == ((MidiOutputDevice) dev).dev);
		return(false);
	}

	public void open() {
		super.open();
		try {
			receiver = dev.getReceiver();
			dev.open();
		} catch(Exception e) {
			return;
		}
	}

	public void accept(Event<MidiMsg> event) {
		super.accept(event);
		receiver.send(event.value.msg, -1);
	}

	public void close() {
		super.close();
		dev.close();
	}
}
