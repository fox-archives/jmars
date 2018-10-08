package edu.asu.jmars.layer.tes6;

import java.util.EventListener;

interface ContextEventListener extends EventListener {
    public void contextAdded(ContextEvent evt);
    public void contextDeleted(ContextEvent evt);
    public void contextUpdated(ContextEvent evt);
    public void contextActivated(ContextEvent evt);
}

class ContextEventAdapter implements ContextEventListener {
    public void contextAdded(ContextEvent evt){}
    public void contextDeleted(ContextEvent evt){}
    public void contextUpdated(ContextEvent evt){}
    public void contextActivated(ContextEvent evt){}
}
