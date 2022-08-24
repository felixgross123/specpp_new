package org.processmining.estminer.specpp.supervision.instrumentators;

import org.processmining.estminer.specpp.base.Candidate;
import org.processmining.estminer.specpp.base.PostProcessor;
import org.processmining.estminer.specpp.base.Result;
import org.processmining.estminer.specpp.base.impls.SPECpp;
import org.processmining.estminer.specpp.componenting.supervision.SupervisionRequirements;
import org.processmining.estminer.specpp.componenting.system.GlobalComponentRepository;
import org.processmining.estminer.specpp.componenting.system.link.ComposerComponent;
import org.processmining.estminer.specpp.componenting.system.link.CompositionComponent;
import org.processmining.estminer.specpp.componenting.system.link.ProposerComponent;
import org.processmining.estminer.specpp.supervision.Supervisor;
import org.processmining.estminer.specpp.supervision.observations.performance.PerformanceEvent;
import org.processmining.estminer.specpp.supervision.observations.performance.TaskDescription;
import org.processmining.estminer.specpp.supervision.piping.TimeStopper;

import java.util.List;

public class InstrumentedSPECpp<C extends Candidate, I extends CompositionComponent<C>, R extends Result, F extends Result> extends SPECpp<C, I, R, F> {

    public static final TaskDescription PEC_CYCLE = new TaskDescription("PEC Cycle");
    public static final TaskDescription TOTAL_CYCLING = new TaskDescription("Total PEC Cycling");

    private final TimeStopper timeStopper = new TimeStopper();


    public InstrumentedSPECpp(GlobalComponentRepository cr, List<Supervisor> supervisors, ProposerComponent<C> proposer, ComposerComponent<C, I, R> composer, PostProcessor<R, F> postProcessor) {
        super(cr, supervisors, proposer, composer, postProcessor);
        componentSystemAdapter().provide(SupervisionRequirements.observable("pec.performance", PerformanceEvent.class, timeStopper));
    }

    @Override
    protected void executeAllPECCycles() {
        timeStopper.start(TOTAL_CYCLING);
        executeAllPECCycles();
        timeStopper.stop(TOTAL_CYCLING);
    }

    @Override
    public boolean executePECCycle() {
        timeStopper.start(PEC_CYCLE);
        boolean stop = executePECCycle();
        timeStopper.stop(PEC_CYCLE);
        return stop;
    }


}