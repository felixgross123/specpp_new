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

/**
 * Composer used to prune the candidate tree based on precision when using the ETC-based composer (as terminal composer).
 * @param <I> Type of Composition
 * @param <R> Type of Result
 */
public class ETCPrecisionCutOff<I extends CompositionComponent<Place>, R extends Result> extends FilteringComposer<Place, I, R> implements ConstrainingComposer<Place, I, R, CandidateConstraint<Place>> {

    /**
     * Precision-threshold rho used to determine when to cut off subtrees
     */
    protected final DelegatingDataSource<RhoETCPrecisionThreshold> rho = new DelegatingDataSource<>();

    /**
     * Pipe to put in "constraining" events, received by candidate-proposition
     */
    protected final EventSupervision<CandidateConstraint<Place>> constraintEvents = PipeWorks.eventSupervision();

    /**
     * Mapping between activities and transitions (and vice versa)
     */
    private final DelegatingDataSource<BidiMap<Activity, Transition>> actTransMapping = new DelegatingDataSource<>();

    /**
     * Activity Mapping activityToAllowed (derived from ETC-based Composer)
     */
    private final DelegatingDataSource<Map<Activity, Integer>> delActivitiesToAllowed = new DelegatingDataSource<>();

    /**
     * Activity Mapping activityToEscapingEdges (derived from ETC-based Composer)
     */
    private final DelegatingDataSource<Map<Activity, Integer>> delActivitiesToEscapingEdges = new DelegatingDataSource<>();

    /**
     * Creates new (recursive) ETCPrecisionCutOff-composer.
     * @param childComposer Next composer in chain.
     */
    public ETCPrecisionCutOff(ComposerComponent<Place, I, R> childComposer) {
        super(childComposer);
        globalComponentSystem().require(ParameterRequirements.RHO_ETCPRECISION_THRESHOLD, rho)
                .require(DataRequirements.ACT_TRANS_MAPPING, actTransMapping)
                .provide(SupervisionRequirements.observable("composer.constraints.ETCCutOff", getPublishedConstraintClass(), getConstraintPublisher()));
        localComponentSystem().provide(SupervisionRequirements.observable("composer.constraints.ETCCutOff", getPublishedConstraintClass(), getConstraintPublisher()))
                .require(dataSource("activitiesToAllowed", JavaTypingUtils.castClass(Map.class)), delActivitiesToAllowed)
                .require(dataSource("activitiesToEscapingEdges", JavaTypingUtils.castClass(Map.class)), delActivitiesToEscapingEdges);
    }

    /**
     * Is executed at initialization.
     */
    @Override
    protected void initSelf() {

    }

    /**
     * Deliberates the acceptance of a candidate place. Executes forward(place) to forward "place" to the next
     * Composer in the chain. Executes gotFiltered(place) if "place" is discarded.
     * @param place Candidate place.
     */
    @Override
    public void accept(Place place) {

        Map<Activity, Integer> activitiesToAllowed = delActivitiesToAllowed.getData();
        Map<Activity, Integer> activitiesToEscapingEdges = delActivitiesToEscapingEdges.getData();

        if(activitiesToAllowed.size()>0 && activitiesToEscapingEdges.size()>0) {

            //search for outgoing activity with highest activity-wise precision
            double maxFraction = Double.MIN_VALUE;

            for(Transition t : place.postset()) {
                Activity a = actTransMapping.getData().getKey(t);
                double frac = 1.0 - ((double) activitiesToEscapingEdges.get(a) / activitiesToAllowed.get(a));
                if(frac > maxFraction) maxFraction = frac;
            }

            //check if maximum activity-wise precision is less than threshold rho
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

    /**
     * Is executed after a candidate place is discarded.
     * @param place Candidate place.
     */
    protected void gotFiltered(Place place) {
    }

    /**
     * Returns the publisher of the candidate constrain.
     * @return Publisher.
     */
    @Override
    public Observable<CandidateConstraint<Place>> getConstraintPublisher() {
        return constraintEvents;
    }

    /**
     * Returns the class of the publisher of the candidate constrain.
     * @return Class.
     */
    @Override
    public Class<CandidateConstraint<Place>> getPublishedConstraintClass() {
        return JavaTypingUtils.castClass(CandidateConstraint.class);
    }

}
