package org.processmining.estminer.specpp.componenting.system;

import org.processmining.estminer.specpp.componenting.traits.UsesGlobalComponentSystem;
import org.processmining.estminer.specpp.componenting.traits.UsesLocalComponentSystem;
import org.processmining.estminer.specpp.traits.Initializable;

import java.util.List;

public interface FullComponentSystemUser extends UsesLocalComponentSystem, UsesGlobalComponentSystem, Initializable {

    void registerSubComponent(FullComponentSystemUser subComponent);

    void unregisterSubComponent(FullComponentSystemUser subComponent);

    List<FullComponentSystemUser> collectTransitiveSubcomponents();

    default void connectLocalComponentSystem(LocalComponentRepository lcr) {
        collectTransitiveSubcomponents().forEach(csu -> lcr.consumeEntirely(csu.localComponentSystem()));
        lcr.fulfil(lcr);
        // TODO decide whether this is a good idea collectTransitiveSubcomponents().forEachOrdered(csu -> csu.localComponentSystem().consumeEntirely(lcr));
    }

    @Override
    default ComponentCollection getComponentCollection() {
        return UsesGlobalComponentSystem.super.getComponentCollection();
    }
}
