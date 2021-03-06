package fr.inria.midifileperformer.app;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.util.Vector;

import fr.inria.bps.base.Vecteur;
import fr.inria.lognet.sos.Picture;
import fr.inria.lognet.sos.Shape;
import fr.inria.lognet.sos.Sos;
import fr.inria.lognet.sos.SosColor;
import fr.inria.lognet.sos.event.SosMouse;
import fr.inria.lognet.sos.shape.Label;
import fr.inria.lognet.sos.shape.Wrapper;
import fr.inria.midifileperformer.core.C;
import fr.inria.midifileperformer.core.Consumer;
import fr.inria.midifileperformer.core.Event;
import fr.inria.midifileperformer.core.Record;
import fr.inria.midifileperformer.impl.Config;
import fr.inria.midifileperformer.impl.InputDevice;
import fr.inria.midifileperformer.impl.MidiMsg;
import fr.inria.midifileperformer.impl.MidiRendering;
import fr.inria.midifileperformer.impl.OutputDevice;

public class MetaPlayer extends Wrapper {
	private Vector<String> filenamesJustFugace;
	Label console = Sos.label("Welcome");
	Label file = Sos.label("filetoplay");
	Label input = Sos.label("input");
	Label output = Sos.label("output");
	Label start = Sos.label("     ");
	Label step = Sos.label ("     ");
	Label stop = Sos.label ("     ");
	PlayerZone playerZone = new PlayerZone(600, 400, this);
	Thread player = null;
	Record<MidiMsg> record;

	Config config;

	public static void main(String[] args) {
		if(args.length == 0) args = inDir();
		MetaPlayer me = new MetaPlayer(toVector(args));
		Sos.frame(Sos.border(20, me), 10, 10, "Midi File Performer");
	}

	static String[] inDir() {
		String[] files = new File(".").list(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return(name.endsWith(".mid") || name.endsWith(".midi"));
			}
		});
		return(files);
	}

	static Vector<String> toVector(String[] a) {
		int n = a.length;
		Vector<String> r = new Vector<String>(n);
		for(int i=0; i<n; i++) r.add(a[i]);
		return(r);
	}

	public void init(Picture p, Shape father) {
		super.init(p, father);
		// Must be done after awt setup
		makeConfig(filenamesJustFugace);
		Sos.listen(fr.inria.lognet.sos.Event.quit, picture.root.shape, e -> System.exit(0));
	}

	public MetaPlayer(Vector<String> filenames) {
		this.filenamesJustFugace = filenames;
		Sos.listen(SosMouse.up, file, e -> popup(new ChangeFileName(this), "change input file"));
		Sos.listen(SosMouse.up, input, e -> changeInput());
		Sos.listen(SosMouse.up, output, e -> popup(new ChangeOutput(this), "change output"));
		Sos.listen(SosMouse.up, start, e -> popup(new ChangeStart(this), "change start"));
		Sos.listen(SosMouse.up, stop, e -> popup(new ChangeStop(this), "change stop"));
		//Sos.listen(SosMouse.up, step, e -> expert());
		shape = Sos.column(10, new Shape[] {
				Sos.row(8, new Shape[] {
						Sos.button("Quit", (s -> System.exit(0))),
						Sos.button("Options", (s -> popup(new ChangeOptions(this), "change options"))),
				}),
				Sos.pencolor(SosColor.red, console),
				Sos.row(4, new Shape[] {
						Sos.label("Input : "),
						//Sos.popup("Input", () -> selectInput()),
						Sos.pencolor(SosColor.blue, input),
						Sos.label("           Output : "),
						Sos.pencolor(SosColor.blue, output),
						Sos.label("           File to play : "),
						Sos.pencolor(SosColor.blue, file),
				}),
				Sos.row(4, new Shape[] {
						Sos.label("                Start time : "),
						Sos.pencolor(SosColor.blue, start),
						Sos.label("     time : "),
						Sos.pencolor(SosColor.red, step),
						Sos.label("      Stop time : "),
						Sos.pencolor(SosColor.blue, stop),
				}),
				Sos.contour(2, playerZone),
				Sos.row(4, new Shape[] {
						Sos.button("Restart", (s -> configChanged())),
						Sos.button("Save Config", (s -> saveConfig())),
						Sos.button("Restore Config", (s -> restoreConfig())),
						Sos.button("Player", (s -> popup(new MidiPlayer(this, src(), record()), "Player"))),
						Sos.button("Panic", (s -> panic())),
				}),
		});
	}
	
	Shape selectInput() {
		Vector<InputDevice> removable = new Vector<InputDevice>(config.inputs);
		int n1 = removable.size();
		Vector<InputDevice> addable = ChangeInput.others(removable, this);
		int n2 = addable.size();
		int n = n1 + n2 + 1;
		Shape[] menu = new Shape[n];
		for(int i=0; i<n1; i++)
			menu[i] = Sos.button("remove "+removable.get(i), e ->{});
		for(int i=n1; i<n1+n2; i++) {
			menu[i] = Sos.button("add "+addable.get(i-n1), e ->{});
		}
		menu[n-1] = Sos.button("Browse", e -> changeInput());
		return(Sos.column(0, menu));
	}
	
	void changeInput() {
		popup(new ChangeInput(this), "change input");
	}

	void panic() {
		Vector<MidiMsg> v = MidiMsg.allNotesOff;
		int n = v.size();
		for(int i=0; i<n; i++) {
			MidiMsg msg = v.get(i);
			for(Consumer<Event<MidiMsg>> cons : config.outputs)
				cons.accept(Event.make(System.currentTimeMillis(), msg));
		}
	}

	Vector<Event<MidiMsg>> src() {
		C<MidiMsg> cin = MidiRendering.readAndFilter(config, config.filename);
		Record<MidiMsg> rin = new Record<MidiMsg>(cin);
		rin.force();
		return(rin.recorded);
	}

	Vector<Event<MidiMsg>> record() {
		return(record.recorded);
	}

	void popup(Shape s, String header) {
		Sos.frame(Sos.border(20, s), 100, 20, header);
	}

	void expert() {
		Expert a = new Expert(this, config);
		Sos.frame(Sos.border(20, a), 100, 20, "Advanced options");
	}

	void showTime(long time) {
		//System.out.println("SHOWTIME");
		step.reset(""+time);
		// this repaint doesn't return when input == PlayerZone && ouput != Midi
		//picture.root.FullRepaint();
		//System.out.println("SHOWED");
	}

	/*
	 * config utilisation
	 */

	void makeConfig(Vector<String> filenames) {
		config = new Config(filenames);
		restoreConfig();
	}

	void saveConfig() {
		try {
			config.saveConfig();
		} catch (Exception e) {
			console.reset("Error when saving config " + e);
		}
	}

	void restoreConfig() {
		try {
			config.restoreConfig();
		} catch (FileNotFoundException e) {
			config.addInput(PlayerInputDevice.myName);
			config.addOutput(PlayerOutputDevice.myName);
		} catch (Exception e) {
			console.reset("Error when restoring config " + e);
		}
		configChanged();
	}

	void configChanged() {
		if(player != null) {
			player.interrupt();
			// Not safe afer restoring a config
			//for(InputDevice dev : config.inputs) dev.close();
			//for(OutputDevice dev : config.outputs) dev.close();
			player = null;
		}
		file.reset(config.filename);
		int n = config.inputs.size();
		input.reset(n == 0 ? "no input" :
			n == 1 ? ""+config.inputs.get(0) :
				n == 2 ? config.inputs.get(0) + "," + config.inputs.get(1) :
					n + " inputs selected" );
		n = config.outputs.size();
		output.reset(n == 0 ? "no output" :
			n == 1 ? ""+config.outputs.get(0) :
				n == 2 ? config.outputs.get(0) + "," + config.outputs.get(1) :
					n + " outputs selected" );
		start.reset(""+config.start);
		stop.reset(""+config.stop);
		C<MidiMsg> linear = MidiRendering.launch(config, t -> showTime(t));
		Record<MidiMsg> record = Record.make(linear);
		this.record = record;
		player = MidiRendering.launchPlayer(config.outputs, record);
	}

	public Vector<InputDevice> standardInputDevice() {
		return(Vecteur.sing(new PlayerInputDevice(playerZone)));
	}

	public Vector<OutputDevice> standardOutputDevice() {
		return(Vecteur.sing(new PlayerOutputDevice(playerZone)));
	}

}

