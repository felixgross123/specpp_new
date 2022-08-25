package org.processmining.estminer.specpp.supervision.instrumentators;

import org.processmining.estminer.specpp.base.Candidate;
import org.processmining.estminer.specpp.base.Result;
import org.processmining.estminer.specpp.componenting.supervision.SupervisionRequirements;
import org.processmining.estminer.specpp.componenting.system.link.ComposerComponent;
import org.processmining.estminer.specpp.componenting.system.link.CompositionComponent;
import org.processmining.estminer.specpp.supervision.observations.performance.PerformanceEvent;
import org.processmining.estminer.specpp.supervision.observations.performance.TaskDescription;

public class InstrumentedComposer<C extends Candidate, I extends CompositionComponent<C>, R extends Result> extends AbstractInstrumentingDelegator<ComposerComponent<C, I, R>> implements ComposerComponent<C, I, R> {

    public static final TaskDescription CANDIDATE_COMPOSITION = new TaskDescription("Candidate Composition");
    public static final TaskDescription RESULT_GENERATION = new TaskDescription("Result Generation");

    public InstrumentedComposer(ComposerComponent<C, I, R> delegate) {
        super(delegate);
        globalComponentSystem().provide(SupervisionRequirements.observable("composer.performance", PerformanceEvent.class, timeStopper));
    }

    public boolean isFinished() {
        return delegate.isFinished();
    }

    public I getIntermediateResult() {
        return delegate.getIntermediateResult();
    }

    public R generateResult() {
        timeStopper.start(RESULT_GENERATION);
        R r = delegate.generateResult();
        timeStopper.stop(RESULT_GENERATION);
        return r;
    }

    public void accept(C c) {
        timeStopper.start(CANDIDATE_COMPOSITION);
        delegate.accept(c);
        timeStopper.stop(CANDIDATE_COMPOSITION);
    }

}
