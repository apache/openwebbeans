package org.jboss.cdi.tck.tests.implementation.producer.method.definition;

import javax.enterprise.inject.Produces;
import java.util.ArrayList;
import java.util.List;

// org.jboss.cdi.tck.tests.implementation.producer.method.definition.ProducerMethodDefinitionTest.testTypeVariableReturnType()
// seems to mean (comment) this class is missing
public class SpiderListProducer extends GeneralListProducer<Spider> {
    @Produces
    public List<Spider> spiders() {
        return new ArrayList<Spider>();
    }
}
