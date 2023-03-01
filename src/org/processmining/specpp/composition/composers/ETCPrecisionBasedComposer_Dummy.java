package org.processmining.specpp.composition.composers;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.MapIterator;
import org.processmining.specpp.base.AdvancedComposition;
import org.processmining.specpp.base.ConstrainingComposer;
import org.processmining.specpp.base.impls.AbstractComposer;
import org.processmining.specpp.base.impls.CandidateConstraint;
import org.processmining.specpp.componenting.data.DataRequirements;
import org.processmining.specpp.componenting.data.DataSource;
import org.processmining.specpp.componenting.data.ParameterRequirements;
import org.processmining.specpp.componenting.data.StaticDataSource;
import org.processmining.specpp.componenting.delegators.ConsumingContainer;
import org.processmining.specpp.componenting.delegators.DelegatingDataSource;
import org.processmining.specpp.componenting.delegators.DelegatingEvaluator;
import org.processmining.specpp.componenting.evaluation.EvaluationRequirements;
import org.processmining.specpp.componenting.supervision.SupervisionRequirements;
import org.processmining.specpp.composition.events.CandidateAcceptanceRevoked;
import org.processmining.specpp.composition.events.CandidateAccepted;
import org.processmining.specpp.composition.events.CandidateCompositionEvent;
import org.processmining.specpp.composition.events.CandidateRejected;
import org.processmining.specpp.config.parameters.FlagPrematureAbort;
import org.processmining.specpp.config.parameters.GammaETCPrecisionGainThreshold;
import org.processmining.specpp.config.parameters.RhoETCPrecisionThreshold;
import org.processmining.specpp.datastructures.encoding.BitEncodedSet;
import org.processmining.specpp.datastructures.log.Activity;
import org.processmining.specpp.datastructures.log.Log;
import org.processmining.specpp.datastructures.log.impls.Factory;
import org.processmining.specpp.datastructures.log.impls.IndexedVariant;
import org.processmining.specpp.datastructures.petri.CollectionOfPlaces;
import org.processmining.specpp.datastructures.petri.Place;
import org.processmining.specpp.datastructures.petri.Transition;
import org.processmining.specpp.datastructures.transitionSystems.PAState;
import org.processmining.specpp.datastructures.transitionSystems.PrefixAutomaton;
import org.processmining.specpp.datastructures.vectorization.VariantMarkingHistories;
import org.processmining.specpp.supervision.EventSupervision;
import org.processmining.specpp.supervision.piping.Observable;
import org.processmining.specpp.supervision.piping.PipeWorks;
import org.processmining.specpp.util.JavaTypingUtils;

import java.nio.IntBuffer;
import java.util.*;

/**
 * Dummy ETC-based Composer.
 * Accepts all places it receives, but updates the acitivity mappings activityToIngoingPlaces, activityToEscapingEdges and activityToAllowed.
 * Useful for experiments with the Uniwired-variant using the GreedyETCPrecisionTreeTraversalHeuristic, and crucially, no ETC-based Composer.
 * @param <I> Type of Composition
 */
public class ETCPrecisionBasedComposer_Dummy<I extends AdvancedComposition<Place>> extends AbstractComposer<Place, I, CollectionOfPlaces> {

    private final DelegatingDataSource<Log> logSource = new DelegatingDataSource<>();
    private final DelegatingDataSource<BidiMap<Activity, Transition>> actTransMapping = new DelegatingDataSource<>();
    private final DelegatingEvaluator<Place, VariantMarkingHistories> markingHistoriesEvaluator = new DelegatingEvaluator<>();
    private final PrefixAutomaton prefixAutomaton = new PrefixAutomaton(new PAState());
    private final Map<Activity, Set<Place>> activityToIngoingPlaces = new HashMap<>();
    private final Map<Activity, Integer> activityToEscapingEdges = new HashMap<>();
    private final Map<Activity, Integer> activityToAllowed = new HashMap<>();
    private final EventSupervision<CandidateCompositionEvent<Place>> compositionEventSupervision = PipeWorks.eventSupervision();


    public ETCPrecisionBasedComposer_Dummy(I composition) {
        super(composition, c -> new CollectionOfPlaces(c.toList()));

        ConsumingContainer<DataSource<DelegatingDataSource<Map<Activity, Integer>>>> consActivitiesToEscapingEdgesUpdatingGreedyETC = new ConsumingContainer<>(del -> del.getData().setDelegate(StaticDataSource.of(activityToEscapingEdges)));
        globalComponentSystem().require(DataRequirements.dataSource("activitiesToEscapingEdges_UpdatingGreedyETC", JavaTypingUtils.castClass(DelegatingDataSource.class)), consActivitiesToEscapingEdgesUpdatingGreedyETC);

        ConsumingContainer<DataSource<DelegatingDataSource<Map<Activity, Integer>>>> consActivitiesAllowedUpdatingGreedyETC = new ConsumingContainer<>(del -> del.getData().setDelegate(StaticDataSource.of(activityToAllowed)));
        globalComponentSystem().require(DataRequirements.dataSource("activitiesToAllowed_UpdatingGreedyETC", JavaTypingUtils.castClass(DelegatingDataSource.class)), consActivitiesAllowedUpdatingGreedyETC);

        ConsumingContainer<DataSource<DelegatingDataSource<Map<Activity, Integer>>>> consActivitiesToEscapingEdgesGreedyETC = new ConsumingContainer<>(del -> del.getData().setDelegate(StaticDataSource.of(activityToEscapingEdges)));
        globalComponentSystem().require(DataRequirements.dataSource("activitiesToEscapingEdges_GreedyETC", JavaTypingUtils.castClass(DelegatingDataSource.class)), consActivitiesToEscapingEdgesGreedyETC);

        ConsumingContainer<DataSource<DelegatingDataSource<Map<Activity, Integer>>>> consActivitiesToAllowedGreedyETC = new ConsumingContainer<>(del -> del.getData().setDelegate(StaticDataSource.of(activityToAllowed)));
        globalComponentSystem().require(DataRequirements.dataSource("activitiesToAllowed_GreedyETC", JavaTypingUtils.castClass(DelegatingDataSource.class)), consActivitiesToAllowedGreedyETC);


        globalComponentSystem().require(DataRequirements.RAW_LOG, logSource)
                               .require(DataRequirements.ACT_TRANS_MAPPING, actTransMapping)
                               .require(EvaluationRequirements.PLACE_MARKING_HISTORY, markingHistoriesEvaluator);
        ;
    }


    /**
     * Deliberate, whether place should be added to the composition or not
     * @param candidate the candidate to decide acceptance for
     * @return true, if candidate should be added. Otherwise, false.
     */
    @Override
    protected boolean deliberateAcceptance(Place candidate) {

        BitEncodedSet<Transition> candidateOut = candidate.postset();
        Set<Activity> activitiesToRevealuate = new HashSet<>();
        for (Transition t: candidateOut) {
            activitiesToRevealuate.add(actTransMapping.getData().getKey(t));
        }

        addToActivityPlacesMapping(candidate);

        for (Activity a : activitiesToRevealuate) {
            int[] evalRes = evaluatePrecision(a);
            int newEE = evalRes[0];
            int newAllowed = evalRes[1];

            activityToEscapingEdges.put(a, newEE);
            activityToAllowed.put(a, newAllowed);
        }

        removeFromActivityPlacesMapping(candidate);
        return true;
    }


    @Override
    protected void acceptanceRevoked(Place candidate) {
    }

    @Override
    protected void candidateAccepted(Place candidate) {
        addToActivityPlacesMapping(candidate);
        compositionEventSupervision.observe(new CandidateAccepted<>(candidate));
    }

    @Override
    protected void candidateRejected(Place candidate) {
    }

    @Override
    public void candidatesAreExhausted() {

    }


    /**
     * Initialize the ETC-based Composer: Build-Prefix Automaton and Look-Up Tables
     */
    @Override
    protected void initSelf() {
        // Build Prefix-Automaton
        Log log = logSource.getData();
        for(IndexedVariant indexedVariant : log) {
            prefixAutomaton.addVariant(indexedVariant.getVariant());
        }

        // Init ActivityPlaceMapping
        MapIterator<Activity, Transition> mapIterator = actTransMapping.get().mapIterator();
        while(mapIterator.hasNext()) {
            Activity a = mapIterator.next();
            activityToIngoingPlaces.put(a, new HashSet<>());
        }

        // Init EscapingEdges
        Set<Activity> activities = actTransMapping.getData().keySet();
        for(Activity a : activities) {
            if(!a.equals(Factory.ARTIFICIAL_START)) {
                int[] evalRes = evaluatePrecision(a);
                int EE = evalRes[0];
                int allowed = evalRes[1];
                activityToEscapingEdges.put(a, EE);
                activityToAllowed.put(a, allowed);
            }
        }
    }

    private void addToActivityPlacesMapping(Place p){
        for(Transition t : p.postset()) {
            Activity a = actTransMapping.get().getKey(t);
            Set<Place> tIn = activityToIngoingPlaces.get(a);
            tIn.add(p);
        }
    }

    private void removeFromActivityPlacesMapping(Place p){
        for(Transition t : p.postset()) {
            Activity a = actTransMapping.get().getKey(t);
            Set<Place> tIn = activityToIngoingPlaces.get(a);
            tIn.remove(p);
        }
    }

    @Override
    public boolean isFinished() {
       return false;
    }


    public int[] evaluatePrecision(Activity a) {

        int escapingEdges = 0;
        int allowed = 0;
        Log log = logSource.getData();


        Set<Place> prerequisites = activityToIngoingPlaces.get(a);

        // collect markingHistories
        LinkedList<VariantMarkingHistories> markingHistories = new LinkedList<>();
        for(Place p : prerequisites) {
            markingHistories.add(markingHistoriesEvaluator.eval(p));
        }

        //iterate log: variant by variant, activity by activity

        for(IndexedVariant variant : log) {

            int vIndex = variant.getIndex();
            int length = variant.getVariant().getLength();

            PAState logState = prefixAutomaton.getInitial();


            for (int j = 1; j < length * 2; j += 2) {
                int aIndex = ((j + 1) / 2) - 1;

                // update log state
                logState = logState.getTrans(log.getVariant(vIndex).getAt(aIndex)).getPointer();

                boolean isAllowedO = true;

                for(VariantMarkingHistories h : markingHistories) {
                    //check if column = 1 f.a. p in prerequites --> activity a is allowed
                    IntBuffer buffer = h.getAt(vIndex);
                    int p = buffer.position();

                    if(buffer.get(p + j) == 0) {
                        isAllowedO = false;
                        break;
                    }
                }

                if(isAllowedO) {
                    allowed += log.getVariantFrequency(vIndex);

                    //check if a is reflected
                    if (!logState.checkForOutgoingAct(a)) {
                        //a is not reflected, hence escaping
                        escapingEdges += log.getVariantFrequency(vIndex);
                    }
                }
            }
        }
        //System.out.println("EE(" + a +") = " + escapingEdges);
        return new int[]{escapingEdges, allowed};
    }



}
