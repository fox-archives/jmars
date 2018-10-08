package edu.asu.jmars.layer.tes6;

import java.util.*;

class SourcedRuntimeException extends RuntimeException {
    public SourcedRuntimeException(Object source, Exception cause){
        super(cause);
        this.source = source;
    }
    
    public Object getSource(){ return source; }
    
    private Object source;
}
