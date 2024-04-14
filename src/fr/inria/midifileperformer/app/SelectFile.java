package fr.inria.midifileperformer.app;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.io.FilenameFilter;
import java.util.Vector;

import fr.inria.bps.base.Event;
import fr.inria.bps.base.Vecteur;
import fr.inria.fun.Fun0;
import fr.inria.lognet.sos.Shape;
import fr.inria.lognet.sos.Sos;
import fr.inria.lognet.sos.SosColor;
import fr.inria.lognet.sos.event.TransfertEvent;
import fr.inria.lognet.sos.shape.AutoScroll;
import fr.inria.lognet.sos.shape.PenColor;
import fr.inria.lognet.sos.shape.Wrapper;
import fr.inria.midifileperformer.impl.Midi;
import fr.inria.midifileperformer.impl.MidiMsg;

public class SelectFile extends Wrapper {
	Configurator master;
	MidiScrollDisplay display;
	File currentdir;
	Wrapper aread;
	Wrapper areaf;
	Player player = null;

	public SelectFile(Configurator master) {
		this.master = master;
		Vector<Event<MidiMsg>> events = master.src();
		Vector<File> dirs = master.config.directories;
		if(dirs.size() > 0) currentdir = dirs.get(0);
		Shape sdirs = dirs(dirs, false);
		Shape sfiles = files(); 
		display = new MidiScrollDisplay(master, events, true);
		Sos.listen(TransfertEvent.event, this, e -> drop((TransfertEvent) e));
		aread = new AutoScroll(300, 100, sdirs, false, false);
		areaf = new AutoScroll(300, 100, sfiles, false, false);
		shape = Sos.column(3, new Shape[] {
				Sos.row(8,
						Sos.column(0, 
								Sos.namedshape("Directories", SosColor.red, 100, aread),
								Sos.row(3, 
										Sos.button("Remove current directory", e -> rmCurrentDir()),
										Sos.button("Get from Clipboard", e -> getFromClipBoard()) )),
						Sos.namedshape("Files", SosColor.red, 100, areaf) ),
				Sos.row(8, 
						Sos.button("Set Start time", e -> setStartTime()),
						Sos.button("Set Stop time", e -> setStopTime()) ),
				display,
				Sos.row(4, new Shape[] {
						Sos.button("Restart", (s -> restart())),
						Sos.button("Play/Stop", (s -> playStop(() -> master.src()))),
						Sos.button("Play/Stop Recorded", (s -> playStop(() -> master.record()))),
						//Sos.button("tmp", (s -> master.expert())),
				}),
		});
	}
	
	void restart() {
		quit();
		master.configChanged();
	}
	
	void quit() {
		if(player != null) {
			player.quit = true;
			player = null;
		}
	}
	
	boolean playing = false;
	void playStop(Fun0<Vector<Event<MidiMsg>>> f) {
		if(playing) {
			playing = false;
			stop();
		} else {
			playing = true;
			play(f);
		}
	}

	void stop() {
		if(player != null) {
			player.stop = true;
		}
	}

	void play(Fun0<Vector<Event<MidiMsg>>> f) {
		if(player == null || player.finish) {
			playEvents(f.operation());
		} else {
			player.stop = false;
			try {
				player.sync.put(true);
			} catch (Exception e) {
				master.msg("cannot restart player");
				quit();
				play(f);
			}
		}
	}

	void playRecorded() {
		playEvents(master.record());
	}

	void playEvents(Vector<Event<MidiMsg>> toPlay) {
		master.config.start = 0;
		master.config.stop = -1;
		display.display.reset(toPlay);
		player = Player.launch(master.config.outputs, toPlay, x -> master.showTime(x));
	}

	void setStartTime() {
		master.changeStartTime(display.display.time);
	}

	void setStopTime() {
		master.changeStopTime(display.display.time);
	}

	/*
	 * Directories selection
	 */
	void setCurrentDir(File dir) {
		currentdir = dir;
		areaf.reshape(files());
	}

	void rmCurrentDir() {
		Vector<File> dirs = master.config.directories;
		dirs.remove(currentdir);
		aread.reshape(dirs(dirs, false));
		setCurrentDir(dirs.size() > 0 ? dirs.get(0) : null);
		areaf.reshape(files());
	}

	void openDir(File dir) {
		if(!master.config.directories.contains(dir)) {
			master.config.directories.add(dir);
			aread.reshape(dirs(master.config.directories, false));
			setCurrentDir(dir);
		}
	}

	Shape dirs(Vector<File> dirs, boolean init) {
		Shape down = init ? new Shape(30, 100) : Sos.eoc();
		return(Sos.column(0, Sos.column(3, Vecteur.map(dirs, d -> dir(d))), down));
	}

	Shape dir(File d) {
		Shape s = Sos.label(d.getName());
		PenColor cs = new PenColor(SosColor.black, s) {
			public void repaint(int cx, int cy, int cw, int ch) {
				color = (d.equals(currentdir)) ? SosColor.blue : SosColor.black;
				super.repaint(cx, cy, cw, ch);
			}
		};
		return(Sos.row(0, Sos.button(cs, e -> setCurrentDir(d)), Sos.eol()));
	}

	/*
	 * File selection
	 */

	Shape files() {
		File[] v = new File[0];
		if(currentdir != null) {
			v = inDir(currentdir);
		}
		return(Sos.column(0, Sos.column(3, Vecteur.map(v, d -> file(d))), Sos.eoc()));
	}

	Shape file(File d) {
		Shape s = Sos.label(d.getName());
		PenColor cs = new PenColor(SosColor.black, s) {
			public void repaint(int cx, int cy, int cw, int ch) {
				color = (d.equals(master.config.filename)) ? SosColor.blue : SosColor.black;
				super.repaint(cx, cy, cw, ch);
			}
		};
		return(Sos.row(0, Sos.button(cs, e -> openFile(d)), Sos.eol()));
	}

	void openFile(File f) {
		master.config.filename = f;
		master.config.start = 0;
		master.config.stop = -1;
		//master.config.
		//master.showTime(0);
		display.display.reset(Midi.getEvents(f));
		master.configChanged();
		dirty();
	}

	static File[] inDir(File dir) {
		File[] files = dir.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return(name.endsWith(".mid") || name.endsWith(".midi"));
			}
		});
		return(files);
	}

	void drop(TransfertEvent e) {
		for( File file : e.getFiles()) {
			if(file.isDirectory()) {
				master.msg("receiving a directory");
				openDir(file);
			} else {
				master.msg("receiving a single file");
				openFile(file);
			}
		}
	}

	void getFromClipBoard() {
		Toolkit tk = Toolkit.getDefaultToolkit();
		Clipboard cp = tk.getSystemClipboard();
		Transferable s = cp.getContents(this);
		DataFlavor stf = DataFlavor.stringFlavor;
		try {
			Object o = s.getTransferData(stf);
			if(o instanceof String) {
				String str = (String) o;
				File f = new File(str);
				if(f.isDirectory()) {
					openDir(f);
				} else {
					master.msg(str + " is not a directory");
				}
				System.out.println("allow insert of " + o);
			} else {
				master.msg("Clipboard doesn't contain a string");
			}
		} catch(Exception err) {
			master.msg(""+err);
		}
	}
}
