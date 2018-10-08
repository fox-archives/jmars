package edu.asu.jmars.layer.tes6;


import java.util.*;
import javax.swing.event.EventListenerList;


class RunningStateChangeEvent extends EventObject {
    public RunningStateChangeEvent(Object source, int scope, int event){
        super(source);
        this.scope = scope;
        this.event = event;
    }
    
    public int getScope(){
        return scope;
    }

    public int getEvent(){
        return event;
    }

    int scope;
    int event;

    public static final int SCOPE_DB_IO = 0;
    public static final int SCOPE_MAIN = 1;
    public static final int SCOPE_PANNER = 2;

    public static final int EVENT_START = 0;
    public static final int EVENT_END = 1;
}
