package fr.inria.midifileperformer.core;

import java.util.Vector;

import fr.inria.bps.base.Vecteur;

public class Core {

    /*
     * Cannot put on interface file
     */
    public static <T extends Interval> boolean hasBegin(Vector<T> v) {
	return(Vecteur.any(v, x -> x.isBegin()));
    }

}
