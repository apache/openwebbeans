package org.apache.webbeans.jpa;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;

public class EntityManagersManager
{

    /**
     * key = unitName + "." + "name"
     * value = ThreadLocal&lt;EntityManager&gt;
     */
    private Map<String, ThreadLocal<EntityManager>> entityManagers = new HashMap<String, ThreadLocal<EntityManager>>();
    
    
    /**
     * @param unitName
     * @param name
     * @return the EntityManager or <code>null</code> if not yet set
     */
    public EntityManager get(String unitName, String name)
    {
        EntityManager em = null;
        String key = getKey(unitName, name);
        
        ThreadLocal<EntityManager> tl = entityManagers.get(key);
        if (tl != null)
        {
            return tl.get();
        }
        
        return em;
    }

    /**
     * Set the given EntityManager for the current Thread.
     * This will replace any previously stored EntityManager for this
     * unitName and name.
     *  
     * @param unitName
     * @param name
     * @param em
     */
    public void set(String unitName, String name, EntityManager em)
    {
        String key = getKey(unitName, name);

        ThreadLocal<EntityManager> tl = entityManagers.get(key);
        if (tl == null)
        {
            tl = new ThreadLocal<EntityManager>();
            entityManagers.put(key, tl);
        }

        tl.set(em);
    }

    
    /**
     * construct the key for the {@link #entityManagers} Map
     * @return String key for the map
     */
    private String getKey(String unitName, String name)
    {
        if (unitName == null)
        {
            unitName = "";
        }
        
        if (name == null)
        {
            name = "";
        }
        
        return unitName + "." + name;
    }
    
}
