package org.apache.webbeans.jpa.spi;


import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

/**
 * SPI for getting EntityManager and EntityManagerFactory depending on the  
 * System we are running on. This could be plain JPA Persistence or might
 * be retrieved from the EJB container. 
 */
public interface JPAService
{

    /**
     * get the EntityManagerFactory with the given name.
     * @param unitName JPA persistence unit name
     * @return EntityManagerFactory or <code>null</code> if not found
     */
    public abstract EntityManagerFactory getPersistenceUnit(String unitName);

    /**
     * Get a transactional EntityManager for the current thread using a 
     * ThreadLocal.
     * TODO: from the SPEC: the EntityManger must have dependent scope, but this 
     * does not make sense for e.g. &#x0040;ApplicationScoped.
     * @param unitName the name of the persistence unit. Can be empty or <code>null</code>
     * @param name the name of the EntityManager. Can be empty or <code>null</code>
     * @return a transactional EntityManager
     */
    public abstract EntityManager getPersistenceContext(String unitName, String name);

}