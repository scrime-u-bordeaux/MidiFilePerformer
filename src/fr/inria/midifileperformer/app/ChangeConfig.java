package fr.inria.midifileperformer.app;

import fr.inria.lognet.sos.Picture;
import fr.inria.lognet.sos.Shape;
import fr.inria.lognet.sos.Sos;
import fr.inria.lognet.sos.shape.Wrapper;

public abstract class ChangeConfig extends Wrapper {
	MetaPlayer master;
	PlayerConfig config;
	Shape header;

	abstract void changeConfig();

	public ChangeConfig(MetaPlayer master) {
		this.master = master;
		this.config = master.config;
		header = Sos.row(8, new Shape[] {
				Sos.button("Accept", (s -> accept())),
				Sos.button("Abort", (s -> abort())),
		});
	}

	public void init(Picture p, Shape father) {
		super.init(p, father);
		Sos.listen(fr.inria.lognet.sos.Event.quit, picture.root.shape, e -> abort());
	}

	void accept() {
		changeConfig();
		master.configChanged();
		picture.root.quit();
	}

	void abort() {
		picture.root.quit();
	}

}
