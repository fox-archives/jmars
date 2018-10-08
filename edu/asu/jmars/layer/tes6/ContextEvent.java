package edu.asu.jmars.layer.tes6;

import java.util.EventObject;
import java.util.Vector;
import java.util.Iterator;


public class ContextEvent extends EventObject {
    /**
	 * stop eclipse from complaining
	 */
	private static final long serialVersionUID = 1L;

	protected ContextEvent(Object source, int type){
        super(source);
        this.type = type;
    }

    public static ContextEvent getContextAddedEventInstance(
        Object source,
        TesContext ctx,
        TesContext.CtxImage ctxImage
    )
    {
        ContextEvent e = new ContextEvent(source, CONTEXT_ADDED);

        e.ctx = ctx;
        e.ctxImage = ctxImage;
        
        return e;
    }

    public static ContextEvent getContextRemovedEventInstance(
        Object source,
        TesContext ctx,
        TesContext.CtxImage ctxImage
    )
    {
        ContextEvent e = new ContextEvent(source, CONTEXT_DELETED);

        e.ctx = ctx;
        e.ctxImage = ctxImage;
        
        return e;
    }

    public static ContextEvent getContextUpdatedEventInstance(
        Object source,
        TesContext ctx,
        TesContext.CtxImage ctxImage,
        TesContext.CtxImage oldCtxImage,
        int whatChanged
    )
    {
        ContextEvent e = new ContextEvent(source, CONTEXT_UPDATED);

        e.ctx = ctx;
        e.ctxImage = ctxImage;
        e.oldCtxImage = oldCtxImage;
        e.whatChanged = whatChanged;
        
        return e;
    }

    public static ContextEvent getContextActivatedEventInstance(
        Object source,
        TesContext ctx,
        TesContext.CtxImage ctxImage,
        TesContext oldCtx,
        TesContext.CtxImage oldCtxImage
    )
    {
        ContextEvent e = new ContextEvent(source, CONTEXT_ACTIVATED);

        e.ctx = ctx;
        e.ctxImage = ctxImage;
        e.oldCtx = oldCtx;
        e.oldCtxImage = oldCtxImage;
        
        return e;
    }

    public String toString(){
        StringBuffer sbuf = new StringBuffer();
        Vector pairs = new Vector();

        pairs.add("type="+eventTypeStr[type]);
        pairs.add("ctx="+((ctxImage==null)?"null":ctxImage.getTitle()));
        
        if (type == CONTEXT_UPDATED){
            pairs.add("whatChanged="+whatChangedString());
        }
        if (type == CONTEXT_DELETED || type == CONTEXT_ACTIVATED){
            pairs.add("oldCtx="+((oldCtxImage==null)?"null":oldCtxImage.getTitle()));
        }
        
        sbuf.append(this.getClass().getName());
        sbuf.append("[");
        for(Iterator i = pairs.iterator(); i.hasNext(); ){
            sbuf.append((String)i.next());
            if (i.hasNext()){ sbuf.append(","); }
        }
        sbuf.append("]");

        return sbuf.toString();
    }

    public String whatChangedString(){
        String s = "";
        if ((whatChanged & TITLE_CHANGED)   != 0){ s += "titleChanged,"; }
        if ((whatChanged & FIELDS_CHANGED)  != 0){ s += "fieldsChanged,"; }
        if ((whatChanged & SELECTS_CHANGED) != 0){ s += "selectsChanged,"; }
        if ((whatChanged & ORDERBY_CHANGED) != 0){ s += "orderBysChanged,"; }
        if ((whatChanged & COLORBY_CHANGED) != 0){ s += "colorByChanged,"; }
        if ((whatChanged & DRAWREAL_CHANGED) != 0){ s += "drawRealChanged,"; }
        if ((whatChanged & DRAWNULL_CHANGED) != 0){ s += "drawNullChanged,"; }
        return s;
    }

    /**
     * Returns the event type which is one of {@link #CONTEXT_ADDED}, {@link #CONTEXT_ADDED}, {@link #CONTEXT_UPDATED}, {@link #CONTEXT_DELETED}.
     * @return The event type associated with this event.
     */
    public int getType(){ return type; }
    
    /**
     * Returns the context associated with this event.
     * <ul>
     * <li> <b>{@link #CONTEXT_ACTIVATED}:</b> The newly activated context.
     * <li> <b>{@link #CONTEXT_ADDED}:</b> The newly added context.
     * <li> <b>{@link #CONTEXT_UPDATED}:</b> The updated context.
     * <li> <b>{@link #CONTEXT_DELETED}:</b> The deleted context. 
     * </ul>
     * @return Returns the context associated with this event.
     */
    public TesContext getContext(){ return ctx; }
    
    /**
     * Returns the context-image of the context associated with this event.
     * <ul>
     * <li> <b>{@link #CONTEXT_ACTIVATED}:</b> The newly activated context-image.
     * <li> <b>{@link #CONTEXT_ADDED}:</b> The newly added context-image.
     * <li> <b>{@link #CONTEXT_UPDATED}:</b> The post-updated context-image.
     * <li> <b>{@link #CONTEXT_DELETED}:</b> The deleted context-image. 
     * @return The context-image associated with this event.
     */
    public TesContext.CtxImage getCtxImage(){ return ctxImage; }
    
    /**
     * Returns the old-context associated with this event.
     * <ul>
     * <li> <b>{@link #CONTEXT_ACTIVATED}:</b> The deactivated context.
     * </ul>
     * @return Returns the old-context associated with this event.
     */
    public TesContext getOldContext(){ return oldCtx; }
    
    /**
     * Returns the context-image of the old-context associated with this event.
     * <ul>
     * <li> <b>{@link #CONTEXT_ACTIVATED}:</b> The deactivated context-image.
     * <li> <b>{@link #CONTEXT_UPDATED}:</b> The pre-updated context-image.
     * @return The old-context-image associated with this event.
     */
    public TesContext.CtxImage getOldCtxImage(){ return oldCtxImage; }
    
    /**
     * Describes the change details in case of updates.
     * @return Flags describing what changed through a {@link #CONTEXT_UPDATED} event.
     */
    public int getWhatChanged(){ return whatChanged; }


    private int type;

    private TesContext ctx;
    private TesContext.CtxImage ctxImage;

    private TesContext oldCtx;
    private TesContext.CtxImage oldCtxImage;
    
    private int whatChanged;


    // event types
    public static final int CONTEXT_ADDED = 1;
    public static final int CONTEXT_UPDATED = 2;
    public static final int CONTEXT_DELETED = 3;
    public static final int CONTEXT_ACTIVATED = 4;

    private static final String[] eventTypeStr = {
        "INVALID",
        "CONTEXT_ADDED",
        "CONTEXT_UPDATED",
        "CONTEXT_DELETED",
        "CONTEXT_ACTIVATED"
    };

    // TODO: These should come directly from ctxImage objects
    public static final int NOTHING_CHANGED  = 0;
    public static final int TITLE_CHANGED    = 1<<0;
    public static final int FIELDS_CHANGED   = 1<<1;
    public static final int SELECTS_CHANGED  = 1<<2;
    public static final int ORDERBY_CHANGED  = 1<<3;
    public static final int COLORBY_CHANGED  = 1<<4;
    public static final int DRAWREAL_CHANGED = 1<<5;
    public static final int DRAWNULL_CHANGED = 1<<6;

    public static int makeWhatChanged(
        boolean titleChanged,
        boolean fieldsChanged,
        boolean selectsChanged,
        boolean orderBysChanged,
        boolean colorByChanged,
		boolean drawRealChanged,
		boolean drawNullChanged
        )
    {
        int whatChanged = NOTHING_CHANGED;

        if (titleChanged){    whatChanged |= TITLE_CHANGED;   }
        if (fieldsChanged){   whatChanged |= FIELDS_CHANGED;  }
        if (selectsChanged){  whatChanged |= SELECTS_CHANGED; }
        if (orderBysChanged){ whatChanged |= ORDERBY_CHANGED; }
        if (colorByChanged){  whatChanged |= COLORBY_CHANGED; }
        if (drawRealChanged){  whatChanged |= DRAWREAL_CHANGED; }
        if (drawNullChanged){  whatChanged |= DRAWNULL_CHANGED; }

        return whatChanged;
    }
}
