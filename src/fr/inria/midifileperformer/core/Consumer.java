package fr.inria.midifileperformer.core;

public interface Consumer<T> {
    public void accept(T value);
}
