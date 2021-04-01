package fr.inria.midifileperformer.core;

public interface Merger<T1,T2,T3> {
    public T3 merge(T1 x1, T2 x2);
    public T3 left(T1 x1);
    public T3 right(T2 x2);
}
