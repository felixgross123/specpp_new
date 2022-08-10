package org.processmining.estminer.specpp.config;

import org.processmining.estminer.specpp.componenting.system.ComponentInitializer;
import org.processmining.estminer.specpp.componenting.system.ComponentSystemAdapter;

public class Configuration extends ComponentInitializer {
    public Configuration(ComponentSystemAdapter componentSystemAdapter) {
        super(componentSystemAdapter);
    }

    public <T> T createFrom(SimpleBuilder<T> builder) {
        return checkout(checkout(builder).build());
    }

    public <T, A> T createFrom(InitializingBuilder<T, A> builder, A argument) {
        return checkout(checkout(builder).build(argument));
    }

}