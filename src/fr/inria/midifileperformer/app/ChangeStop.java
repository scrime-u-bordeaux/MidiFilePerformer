package fr.inria.midifileperformer.app;

import fr.inria.lognet.sos.Picture;
import fr.inria.lognet.sos.Shape;
import fr.inria.midifileperformer.core.Event;

public class ChangeStop extends ChangeStart {
    public ChangeStop(MetaPlayer master) {
	super(master);
    }
    
    public void init(Picture p, Shape father) {
	super.init(p, father);
	midis.reselect(find1(vmidi, config.stop));
    }
    
    void changeConfig() {
	Event<?> o = (Event<?>) midis.selected();
	config.stop = o.time;
    }
}