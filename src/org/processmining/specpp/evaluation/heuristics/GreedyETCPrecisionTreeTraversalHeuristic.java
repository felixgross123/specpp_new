package org.processmining.specpp.evaluation.heuristics;

import org.apache.commons.collections4.BidiMap;
import org.processmining.specpp.componenting.data.DataRequirements;
import org.processmining.specpp.componenting.data.ParameterRequirements;
import org.processmining.specpp.componenting.delegators.DelegatingDataSource;
import org.processmining.specpp.componenting.system.ComponentSystemAwareBuilder;
import org.processmining.specpp.componenting.system.link.AbstractBaseClass;
import org.processmining.specpp.config.parameters.AlphaTreeTraversalHeuristic;
import org.processmining.specpp.datastructures.encoding.IntEncodings;
import org.processmining.specpp.datastructures.log.Activity;
import org.processmining.specpp.datastructures.log.Log;
import org.processmining.specpp.datastructures.petri.Place;
import org.processmining.specpp.datastructures.petri.Transition;
import org.processmining.specpp.datastructures.tree.base.HeuristicStrategy;
import org.processmining.specpp.datastructures.tree.heuristic.SubtreeMonotonicity;
import org.processmining.specpp.datastructures.tree.heuristic.TreeNodeScore;
import org.processmining.specpp.datastructures.tree.nodegen.PlaceNode;
import org.processmining.specpp.traits.ZeroOneBounded;
import org.processmining.specpp.util.JavaTypingUtils;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * Tree-Traversal Heuristic greedily expanding towards places that possibly constrain precision the most
 */
public class GreedyETCPrecisionTreeTraversalHeuristic extends AbstractBaseClass implements HeuristicStrategy<PlaceNode, TreeNodeScore>, ZeroOneBounded, SubtreeMonotonicity.Decreasing {

    /**
     * Mapping between activities and transitions (and vice versa)
     */
    private final BidiMap<Activity, Transition> actTransMapping;

    /**
     * Activity Mapping activityToEscapingEdges (derived from ETC-based Composer)
     */
    private final DelegatingDataSource<Map<Activity, Integer>> delegatingDataSourceE = new DelegatingDataSource<>(HashMap::new);

    /**
     * Activity Mapping activityToAllowed (derived from ETC-based Composer)
     */
    private final DelegatingDataSource<Map<Activity, Integer>> delegatingDataSourceA = new DelegatingDataSource<>(HashMap::new);

    /**
     * Alpha
     */
    private final double alpha;

    /**
     * Maximal size of candidate places
     */
    private final int maxSize;

    /**
     * Creates a new GreedyETCPrecisionTreeTraversalHeuristic.
     * @param actTransMapping Mapping between activities and transitions (and vice versa).
     * @param maxSize Maximal size of candidate places.
     * @param alpha Alpha.
     */
    public GreedyETCPrecisionTreeTraversalHeuristic(BidiMap<Activity, Transition> actTransMapping, int maxSize, double alpha) {
        this.actTransMapping = actTransMapping;
        this.alpha = alpha;
        this.maxSize = maxSize;

        //hack: retrieve activity mappings against the "flow" of the PEC-Cycle
        globalComponentSystem()
                .provide(DataRequirements.dataSource("activitiesToEscapingEdges_GreedyETC", JavaTypingUtils.castClass(DelegatingDataSource.class)).fulfilWithStatic(delegatingDataSourceE))
                .provide(DataRequirements.dataSource("activitiesToAllowed_GreedyETC", JavaTypingUtils.castClass(DelegatingDataSource.class)).fulfilWithStatic(delegatingDataSourceA));
    }

    /**
     * Called initially
     */
    @Override
    protected void initSelf() {

    }

    /**
     * Local builder-class
     */
    public static class Builder extends ComponentSystemAwareBuilder<GreedyETCPrecisionTreeTraversalHeuristic> {

        /**
         * Log
         */
        final DelegatingDataSource<Log> rawLog = new DelegatingDataSource<>();

        /**
         * Mapping between activities and transitions (and vice versa)
         */
        private final DelegatingDataSource<BidiMap<Activity, Transition>> actTransMapping = new DelegatingDataSource<>();

        /**
         * Alpha
         */
        private final DelegatingDataSource<AlphaTreeTraversalHeuristic> alpha = new DelegatingDataSource<>();

        /**
         * Activity-integer encoding
         */
        private final DelegatingDataSource<IntEncodings<Activity>> encAct = new DelegatingDataSource<>();

        /**
         * States requirements to builds a new GreedyETCPrecisionTreeTraversalHeuristic
         */
        public Builder() {
            globalComponentSystem().require(DataRequirements.RAW_LOG, rawLog)
                .require(DataRequirements.ACT_TRANS_MAPPING, actTransMapping)
                .require(DataRequirements.ENC_ACT, encAct)
                .require(ParameterRequirements.ALPHA_TREETRAVERSALHEURISTIC, alpha);
        }

        /**
         * Executed if requirements listed in Builder() are fulfilled
         * @return GreedyETCPrecisionTreeTraversalHeuristic.
         */
        @Override
        protected GreedyETCPrecisionTreeTraversalHeuristic buildIfFullySatisfied() {
            int maxSize = encAct.getData().getPresetEncoding().size() + encAct.getData().getPostsetEncoding().size();
            return new GreedyETCPrecisionTreeTraversalHeuristic(actTransMapping.getData(), maxSize, alpha.getData().getAlpha());
        }
    }

    /**
     * Computes the heuristic-score of a candidate place.
     * @param node Candidate place.
     * @return Heuristic Score.
     */
    @Override
    public TreeNodeScore computeHeuristic(PlaceNode node) {
        Place p = node.getPlace();
        Map<Activity, Integer> activityToEscapingEdges = delegatingDataSourceE.getData();
        Map<Activity, Integer> activityToAllowed = delegatingDataSourceA.getData();

        if(p.isHalfEmpty()) {
            return new TreeNodeScore(Double.MAX_VALUE);
        } else {
            if(activityToEscapingEdges.isEmpty()) {
                return  new TreeNodeScore(Double.MAX_VALUE);
            } else {

                int sumEPostSet = 0;
                for(Transition t : node.getPlace().postset()) {
                    Activity a = actTransMapping.getKey(t);
                    sumEPostSet += activityToEscapingEdges.get(a);
                }

                int sumAPostSet = 0;
                for(Transition t : node.getPlace().postset()) {
                    Activity a = actTransMapping.getKey(t);
                    sumAPostSet += activityToAllowed.get(a);
                }

                double score = (alpha) * ((double) sumEPostSet / sumAPostSet) + (1-alpha) * (1 - (double) p.size() / maxSize);
                return new TreeNodeScore(score);

            }
        }
    }

    /**
     * Returns a comparator of the EventuallyFollowsTreeTraversalHeuristic
     * @return Comparator.
     */
    @Override
    public Comparator<TreeNodeScore> heuristicValuesComparator() {
        return Comparator.reverseOrder();
    }

}
