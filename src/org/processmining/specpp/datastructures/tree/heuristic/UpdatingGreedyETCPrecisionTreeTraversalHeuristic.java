package org.processmining.specpp.datastructures.tree.heuristic;

import org.apache.commons.collections4.BidiMap;
import org.processmining.specpp.componenting.data.DataRequirements;
import org.processmining.specpp.componenting.data.ParameterRequirements;
import org.processmining.specpp.componenting.delegators.DelegatingDataSource;
import org.processmining.specpp.composition.events.CandidateCompositionEvent;
import org.processmining.specpp.config.parameters.GammaETCPrecisionGainThreshold;
import org.processmining.specpp.datastructures.log.Activity;
import org.processmining.specpp.datastructures.petri.Place;
import org.processmining.specpp.datastructures.petri.Transition;
import org.processmining.specpp.datastructures.tree.base.HeuristicStrategy;
import org.processmining.specpp.datastructures.tree.heuristic.TreeNodeScore;
import org.processmining.specpp.datastructures.tree.heuristic.UpdatableHeuristicExpansionStrategy;
import org.processmining.specpp.datastructures.tree.nodegen.PlaceNode;
import org.processmining.specpp.util.JavaTypingUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Keeps the heuristic-scores of the places in the priority-queue updated when using the GreedyETCPrecisionTreeTraversalHeuristic
 */
public class UpdatingGreedyETCPrecisionTreeTraversalHeuristic extends UpdatableHeuristicExpansionStrategy<Place, PlaceNode, TreeNodeScore> {

    /**
     * Mapping: Activity to its prerequisite places (in priority queue)
     */
    private final Map<Activity, Set<PlaceNode>> activityToIngoingPlaceNodes = new HashMap<>();

    /**
     * Mapping between activities and transitions (and vice versa)
     */
    private final DelegatingDataSource<BidiMap<Activity, Transition>> actTransMapping = new DelegatingDataSource<>();

    /**
     * Activity Mapping activityToEscapingEdges (derived from ETC-based Composer)
     */
    private final DelegatingDataSource<Map<Activity, Integer>> delegatingDataSourceE = new DelegatingDataSource<>(HashMap::new);

    /**
     * Activity Mapping activityToAllowed (derived from ETC-based Composer)
     */
    private final DelegatingDataSource<Map<Activity, Integer>> delegatingDataSourceA = new DelegatingDataSource<>(HashMap::new);

    /**
     * Gamma
     */
    private final DelegatingDataSource<GammaETCPrecisionGainThreshold> gamma = new DelegatingDataSource<>();

    /**
     * ActivitiesToEscapingEdges-Map of the previous iteration (to compare for changes)
     */
    private Map<Activity, Integer> activitiesToEscapingEdgesPrevious = new HashMap<>();

    /**
     * ActivitiesToAllowed-Map of the previous iteration (to compare for changes)
     */
    private Map<Activity, Integer> activitiesToAllowedPrevious= new HashMap<>();

    /**
     * Creates a new UpdatingGreedyETCPrecisionTreeTraversalHeuristic
     * @param heuristicStrategy heuristic Strategy (GreedyETCPrecisionTreeTravesalHeuristic)
     */
    public UpdatingGreedyETCPrecisionTreeTraversalHeuristic(HeuristicStrategy heuristicStrategy) {
        super(heuristicStrategy);


        globalComponentSystem().require(DataRequirements.ACT_TRANS_MAPPING, actTransMapping)
                                .require(ParameterRequirements.GAMMA_ETCPRECISIONGAIN_THRESHOLD, gamma);
        globalComponentSystem().provide(DataRequirements.dataSource("activitiesToEscapingEdges_UpdatingGreedyETC", JavaTypingUtils.castClass(DelegatingDataSource.class)).fulfilWithStatic(delegatingDataSourceE))
                                .provide(DataRequirements.dataSource("activitiesToAllowed_UpdatingGreedyETC", JavaTypingUtils.castClass(DelegatingDataSource.class)).fulfilWithStatic(delegatingDataSourceA));


    }

    /**
     * Called when a node is added to the priority queue
     * @param node Added place.
     * @param score Heuristic score (computed by GreedyETCPrecisionTreeTravesalHeuristic)
     */
    @Override
    protected void addNode(PlaceNode node, TreeNodeScore score) {
        super.addNode(node, score);
        addToActivityPlacesMapping(node);

    }

    /**
     * Called when a node is removed from the priority queue
     * @param node Removed place.
     */
    @Override
    protected void clearHeuristic(PlaceNode node) {
        super.clearHeuristic(node);
        removeFromActivityPlacesMapping(node);
    }

    /**
     * Is called each time a composition-event happens (is put into the pipe)
     * @param observation Observation.
     */
    @Override
    public void observe(CandidateCompositionEvent<Place> observation) {
        Place pO = observation.getCandidate();

        switch (observation.getAction()) {
            case Accept:
                updatePlaces(pO);
                break;

            case RevokeAcceptance:
                if(gamma.getData().getGamma() > 0) {
                    updatePlaces(pO);
                }
                break;

            default:
                break;
        }
    }

    /**
     * Updates the heuristic score (according tho the GreedyETCPrecisionTreeTraversalHeuristic) of a candidate place in the priority queue.
     * @param pO Observed Place.
     */
    private void updatePlaces(Place pO) {
        Set<Activity> actvitiesChanged = new HashSet<>();

        Map<Activity, Integer> activitiesToEscapingEdges = delegatingDataSourceE.getData();
        Map<Activity, Integer> activitiesToAllowed = delegatingDataSourceA.getData();

        for(Transition t : pO.postset()) {
            Activity a = actTransMapping.get().getKey(t);
            if(activitiesToEscapingEdgesPrevious.isEmpty() && activitiesToAllowedPrevious.isEmpty()) {
                //only at the first accepted place: add all postset activities
                actvitiesChanged.add(a);
            } else {
                //check if EE Score has changed
                if (!activitiesToEscapingEdgesPrevious.get(a).equals(activitiesToEscapingEdges.get(a)) || !activitiesToAllowedPrevious.get(a).equals(activitiesToAllowed.get(a))) {
                    actvitiesChanged.add(a);
                }
            }
        }

        //Collect Places that need to be updated
        Set<PlaceNode> placesToUpdate = new HashSet<>();

        for(Activity a : actvitiesChanged) {
            placesToUpdate.addAll(activityToIngoingPlaceNodes.get(a));
        }

        for (PlaceNode p : placesToUpdate) {
            updateNode(p, getHeuristicStrategy().computeHeuristic(p));
        }

        //update previous map
        activitiesToEscapingEdgesPrevious = new HashMap<>(delegatingDataSourceE.getData());
        activitiesToAllowedPrevious = new HashMap<>(delegatingDataSourceA.getData());
    }


    /**
     * Adds a place to the activityToIngoingPlaces.
     * @param p Place.
     */
    private void addToActivityPlacesMapping(PlaceNode p){
        for (Transition t : p.getPlace().postset()) {
            Activity a = actTransMapping.get().getKey(t);

            if (!activityToIngoingPlaceNodes.containsKey(a)) {
                Set<PlaceNode> s = new HashSet<>();
                s.add(p);
                activityToIngoingPlaceNodes.put(a, s);
            } else {
                Set<PlaceNode> tIn = activityToIngoingPlaceNodes.get(a);
                tIn.add(p);
            }
        }
    }

    /**
     * Removes a place to the activityToIngoingPlaces.
     * @param p Place.
     */
    private void removeFromActivityPlacesMapping(PlaceNode p){
        for(Transition t : p.getPlace().postset()) {
            Activity a = actTransMapping.get().getKey(t);
            Set<PlaceNode> tIn = activityToIngoingPlaceNodes.get(a);
            tIn.remove(p);
        }
    }

}
