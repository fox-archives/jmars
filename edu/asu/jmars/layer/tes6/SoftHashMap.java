package edu.asu.jmars.layer.tes6;

import java.util.*;
import java.lang.ref.*;


class SoftHashMap extends AbstractMap {
    public SoftHashMap(){
        init();
    }

    public SoftHashMap(Map t){
        init();
        putAll(t);
    }

    public void clear(){
        map.clear();
    }

    public boolean containsKey(Object key){
        processRefQ();
        return map.containsKey(key);
    }

    public boolean containsValue(Object value){
        Collection vals = map.values();
        return vals.contains(value);
    }

    // TODO: Not up to par.
    public Set entrySet(){
        processRefQ();
        
        Set eSet = map.entrySet();
        SoftVal v;
        Set outputEntrySet = new HashSet();
        Entry e;

        for(Iterator i = eSet.iterator(); i.hasNext(); ){
            e = (Entry)i.next();
            Object val = ((SoftVal)e.getValue()).get();

            if (val != null){
                outputEntrySet.add(new MapEntry(e.getKey(), val));
            }
        }
        
        return outputEntrySet;
    }

    // Perpetual false, unless we are comparing against the same object.
    public boolean equals(Object o){
        if (o == this){ return true; }
        return false;
    }

    public Object get(Object key){
        processRefQ();

        Object v = map.get(key);
        if (v != null){
            v = ((SoftVal)v).get();
        }

        return v;
    }

    private void init(){
        map = new HashMap();
        refq = new ReferenceQueue();
    }

    public boolean isEmpty(){
        processRefQ();

        return (map.size() == 0);
    }

    public Set keySet(){
        processRefQ();

        return map.keySet();
    }

    private void processRefQ(){
        SoftVal v;

        while((v = (SoftVal)refq.poll()) != null){
            map.remove(v.key);
        }
    }

    public Object put(Object key, Object value){
        processRefQ();

        Object oldValue = map.put(key, new SoftVal(key, value, refq));
        if (oldValue != null){
            oldValue = ((SoftVal)oldValue).get();
        }
        return oldValue;
    }

    public void putAll(Map t){
        processRefQ();

        Set eSet = t.entrySet();

        for(Iterator i = eSet.iterator(); i.hasNext(); ){
            Map.Entry e = (Map.Entry)i.next();
            map.put(e.getKey(), new SoftVal(e.getKey(), e.getValue(), refq));
        }
    }

    public Object remove(Object key){
        processRefQ();

        Object oldValue = map.remove(key);
        if (oldValue != null){
            oldValue = ((SoftVal)oldValue).get();
        }
        return oldValue;
    }

    public int size(){
        processRefQ();

        return map.size();
    }

    public String toString(){
        processRefQ();

        StringBuffer buff = new StringBuffer();

        buff.append("SoftHashMap[");
        
        Set kSet = map.keySet();
        for(Iterator i = kSet.iterator(); i.hasNext(); ){
            buff.append(i.next().toString());
            if (i.hasNext()){ buff.append(","); }
        }
        buff.append("]");

        return buff.toString();
    }

    public Collection values(){
        processRefQ();

        Collection c = map.values();
        Vector vals = new Vector(c.size());
        Object v;

        for(Iterator i = c.iterator(); i.hasNext(); ){
            v = ((SoftVal)i.next()).get();
            if (v != null){ vals.add(v); }
        }

        return vals;
    }


    private HashMap map;
    private ReferenceQueue refq;

    private final class SoftVal extends SoftReference {
        public SoftVal(Object key, Object val, ReferenceQueue q){
            super(val,q);
            this.key = key;
        }

        protected void finalize() throws Throwable{
            if (LOG_FINALIZATIONS){
                System.err.println("key "+ key.toString()+" finalized.");
            }
        }

        public Object key;
    }

    private final class MapEntry implements Entry {
        public MapEntry(Object key, Object value){
            this.key = key;
            this.value = value;
        }

        public Object getKey(){ return key; }
        public Object getValue(){ return value; }
        public Object setValue(Object value){
            throw new UnsupportedOperationException("Method setValue() not supported on "+getClass().getName());
        }

        private Object key;
        private Object value;
    }

    private static boolean LOG_FINALIZATIONS = false;
}
