package fr.inria.midifileperformer.app;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Vector;

public class PlayerConfig {
	Vector<String> filenames;
	String filename;
	long start = 0;
	long stop = -1;
	boolean loop = true;
	boolean unmeet = true;
	boolean keepTempo = false;
	boolean keepPedal = false;

	Vector<InputDevice> inputs;   
	Vector<OutputDevice> outputs;


	public PlayerConfig() {
	}

	public void saveConfig(File config) throws Exception {
		boolean e = config.exists();
		if(!e) {
			config.createNewFile();
		}
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
		print.close();
	}

	public void restoreConfig(PlayerZone player, File config) throws Exception {
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
		int ninputs = Integer.parseInt(d.readLine());
		inputs = new Vector<InputDevice>(ninputs);
		for(int i=0; i<ninputs; i++) { 
			InputDevice dev = InputDevice.byName(player, d.readLine());
			if(dev != null) inputs.add(dev);
		}
		int noutputs = Integer.parseInt(d.readLine());
		outputs = new Vector<OutputDevice>(noutputs);
		for(int i=0; i<noutputs; i++) {
			OutputDevice dev = OutputDevice.byName(player, d.readLine());
			if(dev != null)  outputs.add(dev);
		}
		d.close();
	}
}
