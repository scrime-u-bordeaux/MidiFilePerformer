package fr.inria.midifileperformer.core;

public interface SameEvent<T> {
    public boolean correspond(T event);
}
