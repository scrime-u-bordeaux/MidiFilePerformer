package fr.inria.midifileperformer.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Vector;
import java.util.concurrent.LinkedBlockingQueue;

import javax.sound.midi.MidiMessage;

import fr.inria.midifileperformer.core.Record;

public class Config {
	static String configName = "MidifilePerformer.cfg";
	
	public HashMap<String,String> values = new HashMap<String,String>(30);
	
	public long start = 0;
	public long stop = -1;
	public boolean loop = true;
	public boolean unmeet = true;
	public boolean keepTempo = false;
	public boolean keepPedal = false;
	public boolean velocityOnHeight = false;
	public boolean adjacentKeyFilter = false;
	public boolean qwerty = true;
	public boolean showChannel = true;
	public boolean separateChord = false;
	public boolean separateChannel = false;
	public boolean analysis_sync_off = false;
	public boolean doSync = true;
	public boolean analysis_slice = false;
	public int slice_size = 1617;
	public int slice_start = 0;
	public boolean slice_strict = true;
	public boolean analysis_count_on = false;
	public int nb_on = 8;
	public boolean analysis_channel = false;
	public int selected_channel = 2;
	public int max_delay = 0;
	public double tempo = 1.0;
	public double initTempo = 1.0;
	public boolean setTempoByOn = false;
	public boolean detachPart = false;
	public double maxIncreaseTempo = 0.05;

	public Vector<InputDevice> inputs=new Vector<InputDevice>();   
	public Vector<OutputDevice> outputs=new Vector<OutputDevice>();
	
	public Vector<File> directories = new Vector<File>();
	public Vector<File> files = new Vector<File>();
	public Vector<String> _filenames;
	public File filename;
	
	/*
	 * Not saved
	 */
	public Thread player = null;
	public LinkedBlockingQueue<MidiMessage> byPass = new LinkedBlockingQueue<MidiMessage>();
	public Record<MidiMsg> record;
	public int[] stateChannels;
	

	public Config() {
		this.filename = new File(MidiRendering.demoSong);
		stateChannels = new int[16];
		for(int i=0; i<16; i++) stateChannels[i] = 0;
	}
	
	/*
	 * Managing Input/Output
	 */
	public void addInput(String name, boolean fake) {
		if(fake) {
			addInput(new InputDevice(name));
		} else {
			addInput(InputDevice.devices.get(name));
		}
	}
	
	public void addInput(InputDevice dev) {
		if(dev != null) {
			dev.open();
			inputs.add(dev);
		}
	}
	
	public void removeInput(String name) {
		removeInput(InputDevice.devices.get(name));
	}
	
	public void removeInput(InputDevice dev) {
		if(dev != null) {
			inputs.remove(dev);
			dev.close();
		}
	}
	
	public void addOutput(String name, boolean fake) {
		if(fake) {
			addOutput(new OutputDevice(name));
		} else {
			addOutput(OutputDevice.devices.get(name));
		}
	}
	
	public void addOutput(OutputDevice dev) {
		if(dev != null) {
			dev.open();
			outputs.add(dev);
		}
	}
	
	public void removeOutput(String name) {
		removeOutput(OutputDevice.devices.get(name));
	}
	
	public void removeOutput(OutputDevice dev) {
		if(dev != null) {
			outputs.remove(dev);
			dev.close();
		}
	}
	
	/*
	 * Manage the tempo
	 */
	public void resetTempo(double ntempo) {
		if(ntempo > tempo*(1+maxIncreaseTempo)) {
			ntempo = tempo*(1+maxIncreaseTempo);
		} else if(tempo < tempo*(1-maxIncreaseTempo)) {
			ntempo = tempo*(1-maxIncreaseTempo);
		}
		tempo = ntempo;
	}
	
	/*
	 * Managing the configuration file
	 */
	static File getConfigFile() {
		return(new File(System.getProperty("user.home"), configName));
	}

	public void saveConfig() throws Exception {
		saveConfig(getConfigFile());
	}

	void saveConfig(File config) throws Exception {
		boolean e = config.exists();
		if(!e) config.createNewFile();
		PrintWriter print = new PrintWriter(config);
		print.println(filename);
		print.println(start);
		print.println(stop);
		print.println(loop);
		print.println(unmeet);
		print.println(keepTempo);
		print.println(keepPedal);
		print.println(inputs.size());
		for(InputDevice dev : inputs) print.println(dev.toString());
		print.println(outputs.size());
		for(OutputDevice dev : outputs) print.println(dev.toString());
		print.println(directories.size());
		for(File f : directories) print.println(f.toString());
		print.println(adjacentKeyFilter);
		print.println(qwerty);
		print.println(showChannel);
		print.println(separateChord);
		print.println(separateChannel);
		print.println(analysis_sync_off);
		print.println(doSync);
		print.println(analysis_slice);
		print.println(slice_size);
		print.println(slice_start);
		print.println(analysis_count_on);
		print.println(nb_on);
		print.println(analysis_channel);
		print.println(selected_channel);
		print.println(max_delay);
		print.println(slice_strict);
		print.println(initTempo);
		print.println(setTempoByOn);
		print.println(detachPart);
		print.println(maxIncreaseTempo);
		
		print.close();
	}
	
	public void restoreConfig(boolean fake) throws Exception {
		restoreConfig(getConfigFile(), fake);
	}

	void restoreConfig(File config, boolean fake) throws Exception {
		FileInputStream in = new FileInputStream(config);
		BufferedReader d = new BufferedReader(new InputStreamReader(in));
		//filename = d.readLine();
		d.readLine();
		start = Long.parseLong(d.readLine());
		stop = Long.parseLong(d.readLine());
		loop = Boolean.parseBoolean(d.readLine());
		unmeet = Boolean.parseBoolean(d.readLine());
		keepTempo = Boolean.parseBoolean(d.readLine());
		keepPedal = Boolean.parseBoolean(d.readLine());

		for(InputDevice dev : inputs) dev.close();
		int ninputs = Integer.parseInt(d.readLine());
		inputs = new Vector<InputDevice>(ninputs);
		for(int i=0; i<ninputs; i++) addInput(d.readLine(), fake);
		
		for(OutputDevice dev : outputs) dev.close();
		int noutputs = Integer.parseInt(d.readLine());
		outputs = new Vector<OutputDevice>(noutputs);
		for(int i=0; i<noutputs; i++) addOutput(d.readLine(), fake);
		int ndirectories = Integer.parseInt(d.readLine());
		directories = new Vector<File>(ndirectories);
		for(int i=0; i<ndirectories; i++) directories.add(new File(d.readLine()));
		
		adjacentKeyFilter = Boolean.parseBoolean(d.readLine());
		qwerty = Boolean.parseBoolean(d.readLine());
		showChannel = Boolean.parseBoolean(d.readLine());
		separateChord = Boolean.parseBoolean(d.readLine());
		separateChannel = Boolean.parseBoolean(d.readLine());

		analysis_sync_off = Boolean.parseBoolean(d.readLine());
		doSync = Boolean.parseBoolean(d.readLine());
		analysis_slice = Boolean.parseBoolean(d.readLine());
		slice_size = Integer.parseInt(d.readLine());
		slice_start = Integer.parseInt(d.readLine());
		analysis_count_on = Boolean.parseBoolean(d.readLine());
		nb_on = Integer.parseInt(d.readLine());
		analysis_channel = Boolean.parseBoolean(d.readLine());
		selected_channel = Integer.parseInt(d.readLine());
		max_delay = Integer.parseInt(d.readLine());
		slice_strict = Boolean.parseBoolean(d.readLine());
		initTempo = Double.parseDouble(d.readLine());
		setTempoByOn = Boolean.parseBoolean(d.readLine());
		detachPart = Boolean.parseBoolean(d.readLine());
		maxIncreaseTempo = Double.parseDouble(d.readLine());

		d.close();
	}

}
