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
 * Tree-Traversal Heuristic based on the directly-follows relation between pairs of activities
 */
public class DirectlyFollowsTreeTraversalHeuristic implements HeuristicStrategy<PlaceNode, TreeNodeScore>, ZeroOneBounded, SubtreeMonotonicity.Decreasing {

    /**
     * Directly-follows counts (indices according to Activity-Integer encoding)
     */
    private final int[][] dfCounts;

    /**
     * Alpha
     */
    private final double alpha;

    /**
     * Maximal directly-follows count
     */
    private final int maxDF;

    /**
     * Maximal size of candidate places
     */
    private final int maxSize;

    /**
     * Creates new DirectlyFollowsTreeTraversalHeuristic.
     * @param dfCounts Directly-follows counts.
     * @param alpha Alpha.
     * @param maxDF Maximal directly-follows count.
     * @param maxSize Maximal size of candidate places.
     */
    public DirectlyFollowsTreeTraversalHeuristic(int[][] dfCounts, double alpha, int maxDF, int maxSize) {
        this.dfCounts = dfCounts;
        this.alpha = alpha;
        this.maxDF = maxDF;
        this.maxSize = maxSize;
    }

    /**
     * Local Builder-Class
     */
    public static class Builder extends ComponentSystemAwareBuilder<DirectlyFollowsTreeTraversalHeuristic> {


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
         * States requirements to builds a new DirectlyFollowsTreeTraversalHeuristic
         */
        public Builder() {
            globalComponentSystem().require(DataRequirements.RAW_LOG, rawLog).require(DataRequirements.ENC_ACT, encAct).require(ParameterRequirements.ALPHA_TREETRAVERSALHEURISTIC, alpha);
        }

        /**
         * Executed if requirements listed in Builder() are fulfilled
         * @return DirectlyFollowsTreeTraversalHeuristic.
         */
        @Override
        protected DirectlyFollowsTreeTraversalHeuristic buildIfFullySatisfied() {

            Log log = rawLog.getData();
            IntEncodings<Activity> activityIntEncodings = encAct.getData();
            IntEncoding<Activity> presetEncoding = activityIntEncodings.getPresetEncoding();
            IntEncoding<Activity> postsetEncoding = activityIntEncodings.getPostsetEncoding();
            int preSize = presetEncoding.size();
            int postSize = postsetEncoding.size();

            int[][] counts = new int[preSize][postSize];
            for (int i = 0; i < counts.length; i++) {
                counts[i] = new int[postSize];
            }

            IntVector frequencies = log.getVariantFrequencies();

            //calculate dfCounts
            for (IndexedVariant indexedVariant : log) {
                Variant variant = indexedVariant.getVariant();
                int f = frequencies.get(indexedVariant.getIndex());
                Activity last = null;
                for (Activity activity : variant) {
                    if (last != null) {
                        if (presetEncoding.isInDomain(last) && postsetEncoding.isInDomain(activity)) {
                            Integer i = presetEncoding.encode(last);
                            Integer j = postsetEncoding.encode(activity);
                            counts[i][j] += f;
                        }
                    }
                    last = activity;
                }
            }

            //search for max DF value
            int maxDF = 0;

            for (int[] rows : counts) {
                for (int entry : rows) {
                    if (entry > maxDF) {
                        maxDF = entry;
                    }
                }
            }

            //calc maxSize
            int maxSize = encAct.getData().getPresetEncoding().size() + encAct.getData().getPostsetEncoding().size();

            return new DirectlyFollowsTreeTraversalHeuristic(counts, alpha.getData().getAlpha(), maxDF, maxSize);
        }
    }

    /**
     * Computes the heuristic-score of a candidate place.
     * @param node Candidate place.
     * @return Heuristic Score.
     */
    @Override
    public TreeNodeScore computeHeuristic(PlaceNode node) {
        int sum = node.getPlace().preset()
                .streamIndices()
                .flatMap(i -> node.getPlace().postset().streamIndices().map(j -> dfCounts[i][j]))
                .sum();

        double score = alpha * (((double) sum / (node.getPlace().preset().size() * node.getPlace().postset().size())) / maxDF) + (1-alpha) * (1-((double) node.getPlace().size() / maxSize));

        return new TreeNodeScore(score);
    }

    /**
     * Returns a comparator of the DirectlyFollowsTreeTraversalHeuristic
     * @return Comparator.
     */
    @Override
    public Comparator<TreeNodeScore> heuristicValuesComparator() {
        return Comparator.reverseOrder();
    }

}
