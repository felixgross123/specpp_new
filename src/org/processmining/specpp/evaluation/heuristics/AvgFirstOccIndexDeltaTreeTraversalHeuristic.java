package org.processmining.specpp.evaluation.heuristics;

import org.apache.commons.collections4.BidiMap;
import org.processmining.specpp.componenting.data.DataRequirements;
import org.processmining.specpp.componenting.data.ParameterRequirements;
import org.processmining.specpp.componenting.delegators.DelegatingDataSource;
import org.processmining.specpp.componenting.system.ComponentSystemAwareBuilder;
import org.processmining.specpp.config.parameters.AlphaTreeTraversalHeuristic;
import org.processmining.specpp.datastructures.encoding.IntEncodings;
import org.processmining.specpp.datastructures.log.Activity;
import org.processmining.specpp.datastructures.log.Log;
import org.processmining.specpp.datastructures.log.Variant;
import org.processmining.specpp.datastructures.log.impls.Factory;
import org.processmining.specpp.datastructures.log.impls.IndexedVariant;
import org.processmining.specpp.datastructures.petri.Place;
import org.processmining.specpp.datastructures.petri.Transition;
import org.processmining.specpp.datastructures.tree.base.HeuristicStrategy;
import org.processmining.specpp.datastructures.tree.heuristic.SubtreeMonotonicity;
import org.processmining.specpp.datastructures.tree.heuristic.TreeNodeScore;
import org.processmining.specpp.datastructures.tree.nodegen.PlaceNode;
import org.processmining.specpp.traits.ZeroOneBounded;

import java.util.*;

/**
 * Tree-Traversal Heuristic based on the average first occurrence index activities
 */
public class AvgFirstOccIndexDeltaTreeTraversalHeuristic implements HeuristicStrategy<PlaceNode, TreeNodeScore>, ZeroOneBounded, SubtreeMonotonicity.Decreasing {

    /**
     * Mapping: Activity -> AverageFirstOccurrenceIndex
     */
    private final Map<Activity, Double> activityToMeanFirstOccurrenceIndex;

    /**
     * Mapping between activities and transitions (and vice versa)
     */
    private final BidiMap<Activity, Transition> actTransMapping;

    /**
     * Alpha
     */
    private final double alpha;

    /**
     * Maximal averageFirstOccurrenceIndex-Delta between two activities
     */
    private final double maxDelta;

    /**
     * Maximal size of candidate places
     */
    private final int maxSize;


    /**
     * Creates new AvgFirstOccIndexDeltaTreeTraversalHeuristic.
     * @param activityToMeanFirstOccurrenceIndex Mapping between activities to their averageFirstOccurrenceIndex.
     * @param actTransMapping Mapping between activities and transitions (and vice versa).
     * @param alpha Alpha.
     * @param maxDelta Maximal averageFirstOccurrenceIndex-Delta.
     * @param maxSize Maximal size of candidate places.
     */
    public AvgFirstOccIndexDeltaTreeTraversalHeuristic(Map<Activity, Double> activityToMeanFirstOccurrenceIndex, BidiMap<Activity, Transition> actTransMapping, double alpha, double maxDelta, int maxSize) {
        this.activityToMeanFirstOccurrenceIndex = activityToMeanFirstOccurrenceIndex;
        this.actTransMapping = actTransMapping;
        this.alpha = alpha;
        this.maxDelta = maxDelta;
        this.maxSize = maxSize;
    }

    /**
     * Local Builder Class
     */
    public static class Builder extends ComponentSystemAwareBuilder<AvgFirstOccIndexDeltaTreeTraversalHeuristic> {

        /**
         * Log
         */
        private final DelegatingDataSource<Log> rawLog = new DelegatingDataSource<>();

        /**
         * Mapping between activities and transitions (and vice versa)
         */
        private final DelegatingDataSource<BidiMap<Activity, Transition>> actTransMapping = new DelegatingDataSource<>();

        /**
         * Activity-Integer encoding
         */
        private final DelegatingDataSource<IntEncodings<Activity>> encAct = new DelegatingDataSource<>();

        /**
         * Alpha
         */
        private final DelegatingDataSource<AlphaTreeTraversalHeuristic> alpha = new DelegatingDataSource<>();


        /**
         * States requirements to builds a new AverageFirstOccurrenceIndexDeltaTreeTraversalHeuristic
         */
        public Builder() {
            globalComponentSystem().require(DataRequirements.RAW_LOG, rawLog).require(DataRequirements.ACT_TRANS_MAPPING, actTransMapping).require(DataRequirements.ENC_ACT, encAct)
                    .require(ParameterRequirements.ALPHA_TREETRAVERSALHEURISTIC, alpha);
        }

        /**
         * Executed if requirements listed in Builder() are fulfilled
         * @return AverageFirstOccurrenceIndexDeltaTreeTraversalHeuristic.
         */
        @Override
        protected AvgFirstOccIndexDeltaTreeTraversalHeuristic buildIfFullySatisfied() {
            Log log = rawLog.getData();
            Map<Activity, Double> activityToMeanFirstOccurrenceIndex = new HashMap<>();
            Map<Activity, Integer> activityToFreqSum = new HashMap<>();

            //calc averageFirstOccIndices
            for (IndexedVariant indexedVariant : log) {

                Set<Activity> seen = new HashSet<>();
                Variant variant = indexedVariant.getVariant();
                int variantFrequency = log.getVariantFrequency(indexedVariant.getIndex());

                int j = 0;
                for (Activity a : variant) {
                    if (!seen.contains(a)) {
                        if(!activityToMeanFirstOccurrenceIndex.containsKey(a)) {
                            activityToMeanFirstOccurrenceIndex.put(a, (double) j);
                            activityToFreqSum.put(a,variantFrequency);
                        } else {
                            int freqSumA = activityToFreqSum.get(a);

                            double newAvg = ((double)freqSumA / (double)(freqSumA + variantFrequency)) * activityToMeanFirstOccurrenceIndex.get(a) + ((double)variantFrequency / (double)(freqSumA + variantFrequency)) * j;
                            activityToMeanFirstOccurrenceIndex.put(a, newAvg);
                            activityToFreqSum.put(a,freqSumA + variantFrequency);
                        }
                    }
                    j++;
                    seen.add(a);
                }
            }

            //calc maxDelta and maxSize
            double maxDelta = activityToMeanFirstOccurrenceIndex.get(Factory.ARTIFICIAL_END);
            int maxSize = encAct.getData().getPresetEncoding().size() + encAct.getData().getPostsetEncoding().size();

            return new AvgFirstOccIndexDeltaTreeTraversalHeuristic(activityToMeanFirstOccurrenceIndex, actTransMapping.getData(), alpha.getData().getAlpha(), maxDelta, maxSize);
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

        if (p.isHalfEmpty()) return new TreeNodeScore(0);

        double delta = 0.0;

        for (Transition ti : node.getPlace().preset()) {
            Activity ai = actTransMapping.getKey(ti);
            for (Transition to : node.getPlace().postset()) {
                Activity ao = actTransMapping.getKey(to);
                delta += Math.abs(activityToMeanFirstOccurrenceIndex.get(ao) - activityToMeanFirstOccurrenceIndex.get(ai));

            }
        }

        delta = delta / (node.getPlace().preset().size() * node.getPlace().postset().size());
        
        double score =  alpha * (delta / maxDelta) + (1-alpha) * ((double) node.getPlace().size() / maxSize);
        return new TreeNodeScore(score);
    }

    /**
     * Returns a comparator of the AverageFirstOccurrenceIndexDelta
     * @return Comparator.
     */
    @Override
    public Comparator<TreeNodeScore> heuristicValuesComparator() {
        return Comparator.naturalOrder();
    }

}
