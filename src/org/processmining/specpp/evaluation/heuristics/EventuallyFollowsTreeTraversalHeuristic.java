package org.processmining.specpp.evaluation.heuristics;
import org.processmining.specpp.componenting.data.DataRequirements;
import org.processmining.specpp.componenting.data.ParameterRequirements;
import org.processmining.specpp.componenting.delegators.DelegatingDataSource;
import org.processmining.specpp.componenting.system.ComponentSystemAwareBuilder;
import org.processmining.specpp.config.parameters.AlphaTreeTraversalHeuristic;
import org.processmining.specpp.datastructures.encoding.IntEncoding;
import org.processmining.specpp.datastructures.encoding.IntEncodings;
import org.processmining.specpp.datastructures.log.Activity;
import org.processmining.specpp.datastructures.log.Log;
import org.processmining.specpp.datastructures.log.Variant;
import org.processmining.specpp.datastructures.log.impls.IndexedVariant;
import org.processmining.specpp.datastructures.tree.base.HeuristicStrategy;
import org.processmining.specpp.datastructures.tree.heuristic.SubtreeMonotonicity;
import org.processmining.specpp.datastructures.tree.heuristic.TreeNodeScore;
import org.processmining.specpp.datastructures.tree.nodegen.PlaceNode;
import org.processmining.specpp.datastructures.vectorization.IntVector;
import org.processmining.specpp.traits.ZeroOneBounded;

import java.util.Comparator;

/**
 * Tree-Traversal Heuristic based on the eventually-follows relation between pairs of activities
 */
public class EventuallyFollowsTreeTraversalHeuristic implements HeuristicStrategy<PlaceNode, TreeNodeScore>, ZeroOneBounded, SubtreeMonotonicity.Decreasing {

    /**
     * Eventually-follows counts (indices according to Activity-Integer encoding)
     */
    protected int[][] eventuallyFollows;

    /**
     * Alpha
     */
    private final double alpha;

    /**
     * Maximal eventually-follows count
     */
    private final double maxEF;

    /**
     * Maximal size of candidate places
     */
    private final int maxSize;

    /**
     * Creates new EventuallyFollowsTreeTraversalHeuristic.
     * @param eventuallyFollows Eventually-follows counts.
     * @param alpha Alpha.
     * @param maxEF Maximal eventually-follows count.
     * @param maxSize Maximal size of candidate places.
     */
    public EventuallyFollowsTreeTraversalHeuristic(int[][] eventuallyFollows, double alpha, double maxEF, int maxSize) {
        this.eventuallyFollows = eventuallyFollows;
        this.alpha = alpha;
        this.maxEF = maxEF;
        this.maxSize = maxSize;
    }

    /**
     * Local Builder-Class
     */
    public static class Builder extends ComponentSystemAwareBuilder<EventuallyFollowsTreeTraversalHeuristic> {

        /**
         * Log
         */
        private final DelegatingDataSource<Log> rawLog = new DelegatingDataSource<>();

        /**
         * Activity-Integer encoding
         */
        private final DelegatingDataSource<IntEncodings<Activity>> encAct = new DelegatingDataSource<>();

        /**
         * Alpha
         */
        private final DelegatingDataSource<AlphaTreeTraversalHeuristic> alpha = new DelegatingDataSource<>();

        /**
         * States requirements to builds a new EventuallyFollowsTreeTraversalHeuristic
         */
        public Builder() {
            globalComponentSystem().require(DataRequirements.RAW_LOG, rawLog)
                .require(DataRequirements.ENC_ACT, encAct)
                .require(ParameterRequirements.ALPHA_TREETRAVERSALHEURISTIC, alpha);
        }

        /**
         * Executed if requirements listed in Builder() are fulfilled
         * @return EventuallyFollowsTreeTraversalHeuristic.
         */
        @Override
        protected EventuallyFollowsTreeTraversalHeuristic buildIfFullySatisfied() {
            Log log = rawLog.getData();
            IntEncodings<Activity> activityIntEncodings = encAct.getData();
            IntEncoding<Activity> presetEncoding = activityIntEncodings.getPresetEncoding();
            IntEncoding<Activity> postsetEncoding = activityIntEncodings.getPostsetEncoding();
            IntVector frequencies = log.getVariantFrequencies();
            int preSize = presetEncoding.size();
            int postSize = postsetEncoding.size();

            int[][] ef = new int[preSize][postSize];
            for (int i = 0; i < ef.length; i++) {
                ef[i] = new int[postSize];
            }

            //calculate eventually-follows counts
            for (IndexedVariant indexedVariant : log) {
                Variant variant = indexedVariant.getVariant();
                double f = frequencies.get(indexedVariant.getIndex());
                for (int i = 0; i < variant.getLength(); i++) {
                    Activity a = variant.getAt(i);
                    if (presetEncoding.isInDomain(a)) {
                        Integer m = presetEncoding.encode(a);
                        for (int j = i + 1; j < variant.getLength(); j++) {
                            Activity b = variant.getAt(j);
                            if (postsetEncoding.isInDomain(b)) {
                                Integer n = postsetEncoding.encode(b);
                                ef[m][n] += f;
                            }
                        }
                    }
                }
            }


            //calc max eventually-follows count
            double maxEF = 0;

            for (int[] rows : ef) {
                for (int entry : rows) {
                    if (entry > maxEF) {
                        maxEF = entry;
                    }
                }
            }

            //calc maxSize
            int maxSize = encAct.getData().getPresetEncoding().size() + encAct.getData().getPostsetEncoding().size();

            return new EventuallyFollowsTreeTraversalHeuristic(ef, alpha.getData().getAlpha(), maxEF, maxSize);
        }
    }


    /**
     * Computes the heuristic-score of a candidate place.
     * @param node Candidate place.
     * @return Heuristic Score.
     */
    @Override
    public TreeNodeScore computeHeuristic(PlaceNode node) {
        double sum = node.getPlace().preset()
                .streamIndices()
                .flatMap(i -> node.getPlace().postset().streamIndices().map(j -> eventuallyFollows[i][j]))
                .sum();

        double score = alpha * (sum / (node.getPlace().preset().size() * node.getPlace().postset().size()) / maxEF) + (1-alpha) * (1 - ((double) node.getPlace().size() / maxSize));
        return new TreeNodeScore(score);
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
