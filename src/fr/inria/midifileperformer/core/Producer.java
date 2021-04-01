package fr.inria.midifileperformer.core;

import java.util.concurrent.LinkedBlockingQueue;

public interface Producer<T> {
    public void accept(LinkedBlockingQueue<T> queue);
}
