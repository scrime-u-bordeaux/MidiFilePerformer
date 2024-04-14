package fr.inria.midifileperformer.impl;

import java.util.Hashtable;
import java.util.Vector;
import java.util.concurrent.LinkedBlockingQueue;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;

import fr.inria.midi.CCTracker;
import fr.inria.midi.MidiLib;

public class InputDevice {
	public String name;
	public boolean opened = false;
	public static LinkedBlockingQueue<MidiMsg> hub = new LinkedBlockingQueue<MidiMsg>();

	public InputDevice(String name) {
		this.name = name;
		devices.put(name, this);
	}

	public static Hashtable<String,InputDevice> devices = new Hashtable<String,InputDevice>(); 
	public static InputDevice getDevice(String name) {
		return(devices.get(name));
	}

	public static Vector<InputDevice> all() {
		return(new Vector<InputDevice>(devices.values()));
	}

	public boolean same(InputDevice dev) {
		return(dev.name.equals(name));
	}

	public boolean equals(Object o) {
		if(o instanceof InputDevice) return(same((InputDevice) o));
		return(false);
	}

	public void open() {
		//System.out.println("open "+name);
		opened = true;
	}

	public void close() {
		//System.out.println("close "+name);
		opened = false;
	}

	public boolean never = false;
	static long lastInputOn = -1;
	static long lastLastInputOn = -1;
	public static long tempoOn() {
		if(lastLastInputOn != -1) return(lastInputOn - lastLastInputOn);
		return(-1);
	}
	public void send(MidiMessage message) {
		if(opened) {
			try {
				//System.out.println("send " + MidiLib.info(message));
				if(never && MidiLib.isCC(message)) {
					ShortMessage sm = (ShortMessage) message;
					int cc = sm.getData1();
					int v = sm.getData2();
					//System.out.println(MidiLib.info(msg)+" "+cc+" "+v);
					MidiMessage r = tryNoteOnOff(cc, v);
					if(r != null) put(hub, new MidiMsg(r));
				} else {
					if(MidiLib.isBegin(message)) {
						lastLastInputOn = lastInputOn;
						lastInputOn = System.currentTimeMillis();
					}
					put(hub, new MidiMsg(message));
				}
			} catch (Exception e) {
				System.out.println("cannot put the message " + message);
				close();
			}
		}
	}

	static void put(LinkedBlockingQueue<MidiMsg> queue, MidiMsg m) {
		try {
			//System.out.println("Add in queue "+m);
			queue.put(m);
		} catch (Exception e) {
			System.out.println("cannot put the message " + m);
		}
	}

	CCTracker left = new CCTracker(60, 50, 10, 50, 10, 2.0);
	CCTracker right = new CCTracker(70, 50, 10, 50, 10, 2.0);
	MidiMessage tryNoteOnOff(int code, int v) {
		//System.out.println("try " + code + " "+v);
		if(code == 101) {
			return(left.max(v));
		} else if(code == 102) {
			return(right.max(v));
		}
		return(null);
	}

	public String toString() {
		return(name);
	}
}
