package fr.inria.midifileperformer.app;

import java.util.Vector;

import fr.inria.bps.base.Event;
import fr.inria.bps.base.Pair;
import fr.inria.fun.Fun0;
import fr.inria.lognet.sos.Picture;
import fr.inria.lognet.sos.Shape;
import fr.inria.lognet.sos.Sos;
import fr.inria.lognet.sos.SosColor;
import fr.inria.midifileperformer.impl.MidiMsg;

public class MainApp extends Configurator {
	Selector selector;

	public static void launch() {
		MainApp me = new MainApp();
		Sos.frame(Sos.border(10, me), 10, 10, "Midifile Performer");
		me.selector.change(0);
	}

	public static void main(String[] args) {
		launch();
	}

	public void init(Picture p, Shape father) {
		super.init(p, father);
		// Must be done after awt setup
		makeConfig();
		Sos.listen(fr.inria.lognet.sos.Event.quit, picture.root.shape, e -> System.exit(0));
	}

	public MainApp() {
		super(Sos.label("Welcome"));
		selector = new Selector(1000, 500, makeSelector());
		new Keyboard(this, this);
		shape = Sos.column(10, new Shape[] {
				Sos.pencolor(SosColor.red, console),
				Sos.row(4, new Shape[] {
						Sos.label("Input : "),
						Sos.pencolor(SosColor.blue, input),
						Sos.label("           Output : "),
						Sos.pencolor(SosColor.blue, output),
				}),
				Sos.row(4, new Shape[] {
						Sos.label("File to play : "),
						Sos.pencolor(SosColor.blue, file),
						Sos.eol(),
				}),
				Sos.row(4, new Shape[] {
						Sos.label("                Start time : "),
						Sos.pencolor(SosColor.blue, start),
						Sos.label("     time : "),
						Sos.pencolor(SosColor.red, step),
						Sos.label("      Stop time : "),
						Sos.pencolor(SosColor.blue, stop),
				}),
				Sos.contour(1, new KeyboardDisplay(this)),
				selector,
		});
	}


	long oldTime = -1;
	void showTime(long time) {
		if(oldTime != time) {
			super.showTime(time);
			if(selectFile != null) selectFile.display.display.time = ((int) time);
			selectFile.display.dirty();
			picture.dorepaint();
			oldTime = time;
		}
	}


	SelectFile selectFile = null;
	Shape options = null;

	Shape makeSelectFile() {
		if(selectFile == null) selectFile = new SelectFile(this);
		return(selectFile);
	}
	Shape makeOptions() {
		if(options == null) options = new Options(this);
		return(options);
	}

	Vector<Pair<Shape,Fun0<Shape>>> makeSelector() {
		Vector<Pair<Shape,Fun0<Shape>>> r = new Vector<Pair<Shape,Fun0<Shape>>>(5);
		r.add(Pair.cons(
				Sos.label(" File "), 
				() -> makeSelectFile() ));
		r.add(Pair.cons(
				Sos.label(" Options "), 
				() -> makeOptions() ));
		return(r);
	}

	Shape debug() {
		//System.out.println("ON "+ShortMessage.NOTE_ON);

		Vector<Event<MidiMsg>> l1 = src();
		Vector<Event<MidiMsg>> l2 = record();
		Shape s1 = Sos.slicer(1, 10, l1, 0);
		Shape s2 = Sos.slicer(1, 10, l2, 0);

		return(Sos.column(3, new Shape[] {
				Sos.row(2, 
						Sos.contour(2, s1),
						Sos.contour(2, s2)
						)
		})); 
	}

	static Shape noGrow(Shape s) {
		return(Sos.column(0, Sos.row(10, s, Sos.eol()), Sos.eoc()));
	}
}
