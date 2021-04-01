package fr.inria.midifileperformer.app;

import java.util.Vector;

import fr.inria.lognet.edit.LineEditor;
import fr.inria.lognet.sos.Picture;
import fr.inria.lognet.sos.Shape;
import fr.inria.lognet.sos.Sos;
import fr.inria.lognet.sos.SosColor;
import fr.inria.lognet.sos.shape.Slicer;

public class ChangeFileName extends ChangeConfig {
	Vector<String> copy;
	Slicer filenames;
	LineEditor fileEditor;
	DataTransfert foo = new DataTransfert();

	public ChangeFileName(MetaPlayer master) {
		super(master);
		copy = new Vector<String>(config.filenames);
		filenames = Sos.slicer(1, 20, copy, 0);
		fileEditor = new LineEditor(40);
		shape = Sos.column(10, new Shape[] {
				header,
				Sos.namedshape("Select your file", SosColor.red, 230, filenames),
				Sos.row(4, new Shape[] {
						Sos.label("Editor  :  "),
						Sos.contour(1, Sos.border(3, fileEditor)),
				}),
				Sos.row(4, new Shape[] {
						Sos.button("Add to the list", e -> add()),
						Sos.button("Past for clipboard", e -> get()),
				}),
		});
	}

	public void init(Picture p, Shape father) {
		super.init(p, father);
		filenames.reselect(config.filename);     
	}

	void add() {
		String name = fileEditor.currentLine();
		copy.add(0, name);
		filenames.reset(copy);
		filenames.reselect(0);
	}

	void get() {
		// check there is something cool in Editor
		String name = foo.get();
		fileEditor.edit.erase();
		fileEditor.edit.word_insert(name);
		System.out.println("final res " + name);
		dirty();
	}

	void changeConfig() {
		String name = (String) filenames.selected();
		config.filenames = copy;
		config.filename = name;
	}
}

