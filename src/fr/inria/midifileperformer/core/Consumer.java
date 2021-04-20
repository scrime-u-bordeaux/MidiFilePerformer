package fr.inria.midifileperformer.core;

import fr.inria.fun.Proc1;

public interface Consumer<T> {
    public void accept(T value);
    
    public static <T> Consumer<T> fun(Proc1<T> f) {
    	return(new Consumer<T>() {
    		public void accept(T value) {
    			f.operation(value);
    		}
    	});
    }
}
