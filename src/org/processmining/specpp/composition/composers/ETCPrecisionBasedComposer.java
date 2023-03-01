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
import org.processmining.specpp.config.parameters.RhoETCPrecisionThreshold;
import org.processmining.specpp.config.parameters.GammaETCPrecisionGainThreshold;
import org.processmining.specpp.config.parameters.FlagPrematureAbort;
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
 * ETC-Precision based Composer (for further details refer to paper)
 * @param <I> Type of Composition
 */
public class ETCPrecisionBasedComposer<I extends AdvancedComposition<Place>> extends AbstractComposer<Place, I, CollectionOfPlaces> implements ConstrainingComposer<Place, I, CollectionOfPlaces, CandidateConstraint<Place>> {

    private final DelegatingDataSource<Log> logSource = new DelegatingDataSource<>();
    private final DelegatingDataSource<BidiMap<Activity, Transition>> actTransMapping = new DelegatingDataSource<>();
    private final DelegatingEvaluator<Place, VariantMarkingHistories> markingHistoriesEvaluator = new DelegatingEvaluator<>();
    private final DelegatingDataSource<RhoETCPrecisionThreshold> rho = new DelegatingDataSource<>();
    private final DelegatingDataSource<GammaETCPrecisionGainThreshold> gamma = new DelegatingDataSource<>();
    private final DelegatingDataSource<FlagPrematureAbort> prematureAbort = new DelegatingDataSource<>();
    private final PrefixAutomaton prefixAutomaton = new PrefixAutomaton(new PAState());
    private final Map<Activity, Set<Place>> activityToIngoingPlaces = new HashMap<>();
    private final Map<Activity, Integer> activityToEscapingEdges = new HashMap<>();
    private final Map<Activity, Integer> activityToAllowed = new HashMap<>();
    private final EventSupervision<CandidateCompositionEvent<Place>> compositionEventSupervision = PipeWorks.eventSupervision();
    private final EventSupervision<CandidateConstraint<Place>> constraintEvents = PipeWorks.eventSupervision();
    private double currETCPrecision;
    private boolean newAddition = false;

    /**
     * Creates a new ETC-based Compioser
     * @param composition Collection of places ("Intermediate Model")
     */
    public ETCPrecisionBasedComposer(I composition) {
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
                               .require(EvaluationRequirements.PLACE_MARKING_HISTORY, markingHistoriesEvaluator)
                               .require(ParameterRequirements.RHO_ETCPRECISION_THRESHOLD, rho)
                               .require(ParameterRequirements.GAMMA_ETCPRECISIONGAIN_THRESHOLD, gamma)
                               .require(ParameterRequirements.FLAG_PREMATUREABORT, prematureAbort)
                               .provide(SupervisionRequirements.observable("composer.constraints.ETCCutOff", getPublishedConstraintClass(), getConstraintPublisher()));
        localComponentSystem().provide(SupervisionRequirements.observable("composer.events", JavaTypingUtils.castClass(CandidateCompositionEvent.class), compositionEventSupervision))
                              .provide(SupervisionRequirements.observable("composer.constraints.ETCCutOff", getPublishedConstraintClass(), getConstraintPublisher()))
                .provide(DataRequirements.dataSource("activitiesToAllowed", JavaTypingUtils.castClass(Map.class), StaticDataSource.of(activityToAllowed)))
                .provide(DataRequirements.dataSource("activitiesToEscapingEdges", JavaTypingUtils.castClass(Map.class), StaticDataSource.of(activityToEscapingEdges)));
        ;
    }


    /**
     * Deliberate, whether place should be added to the composition or not
     * @param candidate the candidate to decide acceptance for
     * @return true, if candidate should be added. Otherwise, false.
     */
    @Override
    protected boolean deliberateAcceptance(Place candidate) {

        //eventSupervisor.observe(new DebugEvent("read me"));

        //System.out.println("Evaluating Place " + candidate);
        if (!checkPrecisionGain(candidate)) {
            // no decrease in EE(a) for any activity a that was reevaluated

            //System.out.println("Place " + candidate + " not accepted (no increase in precision)");
            //System.out.println("----------------");
            return false;

        } else {

            // candidate place makes the result more precise -> check for potentially implicit places
            //System.out.println("Place " + candidate + " accepted");

            //collect potentially implcit places
            LinkedList<Place> potImpl = new LinkedList<>();

            BitEncodedSet<Transition> candidateOut = candidate.postset();
            Set<Activity> activitiesToRevealuate = new HashSet<>();
            for (Transition t: candidateOut) {
                activitiesToRevealuate.add(actTransMapping.getData().getKey(t));
            }
            for(Activity a : activitiesToRevealuate){
                potImpl.addAll(activityToIngoingPlaces.get(a));
            }

            //check implicitness and remove
            addToActivityPlacesMapping(candidate);

            for (Place pPotImpl : potImpl) {

                //System.out.println("ImplicitnessCheck: " + pPotImpl);
                if (checkImplicitness(pPotImpl)) {

                    revokeAcceptance(pPotImpl);
                    //System.out.println(pPotImpl + " implicit --> remove");
                }
            }

            removeFromActivityPlacesMapping(candidate);
            //System.out.println("----------------");

            newAddition = true;
            return true;
        }
    }

    /**
     * check (tau=1) / approximate (tau<1) whether a place makes the intermediate result sufficiently more precise
     * @param p place
     * @return true, if p is not implicit / sufficiently improves precision. Otherwise, false.
     */
    public boolean checkPrecisionGain(Place p) {
        BitEncodedSet<Transition> candidateOut = p.postset();
        Set<Activity> activitiesToRevealuate = new HashSet<>();
        for (Transition t: candidateOut) {
            activitiesToRevealuate.add(actTransMapping.getData().getKey(t));
        }

        boolean isMorePrecise = false;

        addToActivityPlacesMapping(p);

        Map<Activity, Integer> tmpActivityToEscapingEdges = new HashMap<>(activityToEscapingEdges);
        Map<Activity, Integer> tmpActivityToAllowed = new HashMap<>(activityToAllowed);

        for (Activity a : activitiesToRevealuate) {
            int[] evalRes = evaluatePrecision(a);
            int newEE = evalRes[0];
            int newAllowed = evalRes[1];

            if (newEE < activityToEscapingEdges.get(a)) {
                isMorePrecise = true;
            }

            tmpActivityToEscapingEdges.put(a, newEE);
            tmpActivityToAllowed.put(a, newAllowed);
        }

        removeFromActivityPlacesMapping(p);

        double newETCPrecision = calcETCPrecision(tmpActivityToEscapingEdges, tmpActivityToAllowed);

        if (gamma.getData().getGamma() == 0) {
            if(isMorePrecise) {
                activityToEscapingEdges.putAll(tmpActivityToEscapingEdges);
                activityToAllowed.putAll(tmpActivityToAllowed);
                currETCPrecision = newETCPrecision;
                return true;
            }
            return false;
        }

        if (newETCPrecision - currETCPrecision > gamma.getData().getGamma() ) {
            //note: if p brings gain in precision we assume that it will be accepted (hence we update the scores)
            activityToEscapingEdges.putAll(tmpActivityToEscapingEdges);
            activityToAllowed.putAll(tmpActivityToAllowed);
            currETCPrecision = newETCPrecision;
            return true;
        }
        return false;
    }

    /**
     * check (tau=1) / approximate (tau<1) whether a place is implicit / insufficiently constrains precision
     * @param p place
     * @return true, if p is implicit / insufficiently constrains precision. Otherwise, false.
     */
    public boolean checkImplicitness(Place p) {

        BitEncodedSet<Transition> pPotImplOut = p.postset();
        Set<Activity> activitiesToReevaluatePPotImpl = new HashSet<>();
        for (Transition t: pPotImplOut) {
            activitiesToReevaluatePPotImpl.add(actTransMapping.getData().getKey(t));
        }

        removeFromActivityPlacesMapping(p);

        Map<Activity, Integer> tmpActivityToEscapingEdges = new HashMap<>(activityToEscapingEdges);
        Map<Activity, Integer> tmpActivityToAllowed = new HashMap<>(activityToAllowed);

        boolean hasEqualValues = true;

        for (Activity a : activitiesToReevaluatePPotImpl) {
            int[] evalRes = evaluatePrecision(a);
            int newEE = evalRes[0];
            int newAllowed = evalRes[1];

            if((newEE != activityToEscapingEdges.get(a)) || newAllowed != activityToAllowed.get(a)) {
                hasEqualValues = false;
                if(gamma.getData().getGamma() == 0) {
                    break;
                }
            }

            tmpActivityToEscapingEdges.put(a, newEE);
            tmpActivityToAllowed.put(a, newAllowed);

        }

        addToActivityPlacesMapping(p);

        double newETCPrecision = calcETCPrecision(tmpActivityToEscapingEdges, tmpActivityToAllowed);
        if(gamma.getData().getGamma() == 0) {
            return hasEqualValues;
        }

        if(currETCPrecision - newETCPrecision > gamma.getData().getGamma()) {
            return false;
        } else {
            activityToEscapingEdges.putAll(tmpActivityToEscapingEdges);
            activityToAllowed.putAll(tmpActivityToAllowed);
            currETCPrecision = newETCPrecision;
            return true;
        }

    }

    /**
     * Hook, executed when candidate is revoked (is implicit) -> Update mappings
     * @param candidate the accepted candidate
     */
    @Override
    protected void acceptanceRevoked(Place candidate) {
        //update ActivityPlaceMapping
        removeFromActivityPlacesMapping(candidate);
        compositionEventSupervision.observe(new CandidateAcceptanceRevoked<>(candidate));
    }

    /**
     * Hook, executed when candidate is accepted (is more precise) -> Update mappings
     * @param candidate the accepted candidate
     */
    @Override
    protected void candidateAccepted(Place candidate) {
        addToActivityPlacesMapping(candidate);
        compositionEventSupervision.observe(new CandidateAccepted<>(candidate));
    }

    @Override
    protected void candidateRejected(Place candidate) {
        compositionEventSupervision.observe(new CandidateRejected<>(candidate));
    }

    public double calcPartETCPrecision(Place candidate) {
        int sumAllowed = 0;
        int sumEscaping = 0;
        for(Transition t : candidate.postset()) {
            Activity a = actTransMapping.getData().getKey(t);
            sumAllowed += activityToAllowed.get(a);
            sumEscaping += activityToEscapingEdges.get(a);
        }
        return 1.0 - ((double) sumEscaping /(double) sumAllowed);
    }

    @Override
    public void candidatesAreExhausted() {
        System.out.println("(Approximate) Precision: " + calcETCPrecision(activityToEscapingEdges, activityToAllowed));
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

    /**
     * Add a place test-wise
     * @param p place
     */
    private void addToActivityPlacesMapping(Place p){
        for(Transition t : p.postset()) {
            Activity a = actTransMapping.get().getKey(t);
            Set<Place> tIn = activityToIngoingPlaces.get(a);
            tIn.add(p);
        }
    }

    /**
     * Remove a place test-wise
     * @param p place
     */
    private void removeFromActivityPlacesMapping(Place p){
        for(Transition t : p.postset()) {
            Activity a = actTransMapping.get().getKey(t);
            Set<Place> tIn = activityToIngoingPlaces.get(a);
            tIn.remove(p);
        }
    }


    /**
     * Check whether search can be aborted prematurely
     * @return true, if search can be aborted. Otherwise, false.
     */
    @Override
    public boolean isFinished() {
        if(prematureAbort.getData().getPrematureAbort() && newAddition) {
            newAddition = false;
            return checkPrecisionThreshold(rho.get().getRho());
        } else {
            return false;
        }
    }

    /**
     * Check whether precision threshold rho has been met
     * @return true, if threshold is reached. Otherwise, false.
     */
    public boolean checkPrecisionThreshold(double p) {
        if(currETCPrecision >= p) {
            System.out.println("PREMATURE ABORT precision threshold " + rho.get().getRho() + " reached");
            return true;
        } else {
            return false;
        }

    }

    /**
     * Calculates the (approximate) ETC-precision based on the given Look-Up Tables
     * @param activityToEscapingEdges Mapping from activities to #EscapingEdges
     * @param activityToAllowed Mapping from activities to #Allowed
     * @return (approximate) ETC-precision
     */
    public double calcETCPrecision(Map<Activity, Integer> activityToEscapingEdges, Map<Activity, Integer> activityToAllowed) {
        int EE = 0;
        for (int i : activityToEscapingEdges.values()) {
            EE += i;
        }
        int allowed = 0;
        for (int i : activityToAllowed.values()) {
            allowed += i;
        }
        //for starting activity:
        allowed += logSource.getData().totalTraceCount();

        return (1 - ((double)EE/allowed));
    }


    /**
     * evaluate (tau=1) / approximate (tau<1) the precision of an activity
     * @param a activity to evaluate
     * @return Integer-array of size two. [0]-#EscapingEdges a, [1]-#Allowed a
     */
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


    @Override
    public Observable<CandidateConstraint<Place>> getConstraintPublisher() {
        return constraintEvents;
    }

    @Override
    public Class<CandidateConstraint<Place>> getPublishedConstraintClass() {
        return JavaTypingUtils.castClass(CandidateConstraint.class);
    }



}
