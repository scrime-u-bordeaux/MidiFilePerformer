package fr.inria.midifileperformer.impl;

import java.util.Hashtable;
import java.util.Vector;

import fr.inria.bps.base.Event;
import fr.inria.midifileperformer.core.Consumer;

public class OutputDevice implements Consumer<Event<MidiMsg>> {
	public String name;
	
	public OutputDevice(String name) {
		this.name = name;
		devices.put(name, this);
	}
	
	public static Hashtable<String,OutputDevice> devices = new Hashtable<String,OutputDevice>(); 
	public static OutputDevice getDevice(String name) {
		return(devices.get(name));
	}
	
	public static Vector<OutputDevice> all() {
		return(new Vector<OutputDevice>(devices.values()));
	}

	public boolean same(OutputDevice dev) {
		return(dev.name.equals(name));
	}

	public boolean equals(Object o) {
		if(o instanceof OutputDevice) return(same((OutputDevice) o));
		return(false);
	}
	
	public void open() {
		//System.out.println("open "+name);
	}

	public void accept(Event<MidiMsg> value) {
		//System.out.println("accept "+name+" "+value);
	}
	
	public void close() {
		//System.out.println("close "+name);
	}
	
	public String toString() {
		return(name);
	}
}
