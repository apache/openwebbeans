package org.apache.webbeans.test.tck;

import java.lang.annotation.Annotation;
import java.util.List;

import javax.webbeans.manager.Manager;

import org.apache.webbeans.context.ContextFactory;
import org.apache.webbeans.deployment.DeploymentTypeManager;
import org.apache.webbeans.test.mock.MockHttpSession;
import org.apache.webbeans.test.mock.MockManager;
import org.jboss.webbeans.tck.api.Managers;

public class ManagersImpl implements Managers
{

    public Manager createManager()
    {
        ContextFactory.initApplicationContext(null);
        ContextFactory.initRequestContext(null);
        ContextFactory.initSessionContext(new MockHttpSession());
        ContextFactory.initConversationContext(null);
        return MockManager.getInstance();
    }

    public List<Class<? extends Annotation>> getEnabledDeploymentTypes()
    {
        return DeploymentTypeManager.getInstance().getEnabledDeploymentTypes();
    }

    public void setEnabledDeploymentTypes(List<Class<? extends Annotation>> enabledDeploymentTypes)
    {
        for (Class<? extends Annotation> deploymentType : enabledDeploymentTypes)
        {
            DeploymentTypeManager.getInstance().addNewDeploymentType(deploymentType, 1);
        }
    }

}
