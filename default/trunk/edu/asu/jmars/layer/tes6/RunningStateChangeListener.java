package edu.asu.jmars.layer.tes6;


import java.util.*;
import javax.swing.event.EventListenerList;


interface RunningStateChangeListener extends EventListener {
    public void runningStateChanged(RunningStateChangeEvent evt);
}
