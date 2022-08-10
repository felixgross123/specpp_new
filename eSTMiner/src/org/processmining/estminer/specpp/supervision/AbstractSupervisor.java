package org.processmining.estminer.specpp.supervision;

import org.processmining.estminer.specpp.componenting.system.AbstractComponentSystemUser;
import org.processmining.estminer.specpp.supervision.piping.LayingPipe;

public abstract class AbstractSupervisor extends AbstractComponentSystemUser implements Supervisor {

    protected LayingPipe beginLaying() {
        return LayingPipe.inst();
    }

}