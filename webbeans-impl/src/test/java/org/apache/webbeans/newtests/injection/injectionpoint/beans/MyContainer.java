package org.apache.webbeans.newtests.injection.injectionpoint.beans;

import javax.inject.Inject;

public class MyContainer {

    @Inject
    @PropertyHolder
    private String nestedProperty;

    public String getNestedProperty() {
        return nestedProperty;
    }
}
