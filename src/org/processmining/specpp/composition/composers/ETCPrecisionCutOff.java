package org.processmining.specpp.composition.composers;

import org.apache.commons.collections4.BidiMap;
import org.processmining.specpp.base.ConstrainingComposer;
import org.processmining.specpp.base.Result;
import org.processmining.specpp.base.impls.CandidateConstraint;
import org.processmining.specpp.base.impls.FilteringComposer;
import org.processmining.specpp.componenting.data.DataRequirements;
import org.processmining.specpp.componenting.data.ParameterRequirements;
import org.processmining.specpp.componenting.delegators.DelegatingDataSource;
import org.processmining.specpp.componenting.supervision.SupervisionRequirements;
import org.processmining.specpp.componenting.system.link.ComposerComponent;
import org.processmining.specpp.componenting.system.link.CompositionComponent;
import org.processmining.specpp.config.parameters.RhoETCPrecisionThreshold;
import org.processmining.specpp.datastructures.log.Activity;
import org.processmining.specpp.datastructures.petri.Place;
import org.processmining.specpp.datastructures.petri.Transition;
import org.processmining.specpp.datastructures.tree.constraints.ETCPrecisionCutOffConstraint;
import org.processmining.specpp.supervision.EventSupervision;
import org.processmining.specpp.supervision.piping.Observable;
import org.processmining.specpp.supervision.piping.PipeWorks;
import org.processmining.specpp.util.JavaTypingUtils;

import java.util.Map;

import static org.processmining.specpp.componenting.data.DataRequirements.dataSource;

public class ETCPrecisionCutOff<I extends CompositionComponent<Place>, R extends Result> extends FilteringComposer<Place, I, R> implements ConstrainingComposer<Place, I, R, CandidateConstraint<Place>> {

    protected final DelegatingDataSource<RhoETCPrecisionThreshold> rho = new DelegatingDataSource<>();
    protected final EventSupervision<CandidateConstraint<Place>> constraintEvents = PipeWorks.eventSupervision();
    private final DelegatingDataSource<BidiMap<Activity, Transition>> actTransMapping = new DelegatingDataSource<>();
    private final DelegatingDataSource<Map<Activity, Integer>> delActivitiesToAllowed = new DelegatingDataSource<>();
    private final DelegatingDataSource<Map<Activity, Integer>> delActivitiesToEscapingEdges = new DelegatingDataSource<>();

    public ETCPrecisionCutOff(ComposerComponent<Place, I, R> childComposer) {
        super(childComposer);
        globalComponentSystem().require(ParameterRequirements.RHO_ETCPRECISION_THRESHOLD, rho)
                                .require(DataRequirements.ACT_TRANS_MAPPING, actTransMapping)
                        .provide(SupervisionRequirements.observable("composer.constraints.ETCCutOff", getPublishedConstraintClass(), getConstraintPublisher()));
        localComponentSystem().provide(SupervisionRequirements.observable("composer.constraints.ETCCutOff", getPublishedConstraintClass(), getConstraintPublisher()))
                .require(dataSource("activitiesToAllowed", JavaTypingUtils.castClass(Map.class)), delActivitiesToAllowed)
                .require(dataSource("activitiesToEscapingEdges", JavaTypingUtils.castClass(Map.class)), delActivitiesToEscapingEdges);
    }

    @Override
    protected void initSelf() {

    }

    @Override
    public void accept(Place place) {

        Map<Activity, Integer> activitiesToAllowed = delActivitiesToAllowed.getData();
        Map<Activity, Integer> activitiesToEscapingEdges = delActivitiesToEscapingEdges.getData();

        if(activitiesToAllowed.size()>0 && activitiesToEscapingEdges.size()>0) {

            double maxFraction = Double.MIN_VALUE;

            for(Transition t : place.postset()) {
                Activity a = actTransMapping.getData().getKey(t);
                double frac = 1.0 - ((double) activitiesToEscapingEdges.get(a) / activitiesToAllowed.get(a));
                if(frac > maxFraction) maxFraction = frac;
            }

            if(maxFraction >= rho.getData().getRho()) {
                constraintEvents.observe(new ETCPrecisionCutOffConstraint(place));
                gotFiltered(place);
            }else {
                forward(place);
            }
        }else {
            forward(place);
        }
    }

    protected void gotFiltered(Place place) {
    }

    @Override
    public Observable<CandidateConstraint<Place>> getConstraintPublisher() {
        return constraintEvents;
    }

    @Override
    public Class<CandidateConstraint<Place>> getPublishedConstraintClass() {
        return JavaTypingUtils.castClass(CandidateConstraint.class);
    }

}
