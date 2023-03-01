package org.processmining.specpp.config.parameters;

/**
 * Parameter to guide the TreeTraversalHeuristics.
 * alpha = 0 --> BFS (prefer simpler places ony)
 * alpha = 1 --> prefer places with the best heuristic score only
 */
public class AlphaTreeTraversalHeuristic implements Parameters {


    /**
     * Value.
     */
    private final double alpha;

    /**
     * Creates a new AlphaTreeTraversalHeuristics-parameter.
     * @param a Value for alpha.
     * @return AlphaTreeTraversalHeuristics-parameter.
     */
    public static AlphaTreeTraversalHeuristic alpha(double a) {
        return new AlphaTreeTraversalHeuristic(a);
    }

    /**
     * Returns the AlphaTreeTraversalHeuristics-parameter with its default value 1.0
     * @return AlphaTreeTraversalHeuristics-parameter
     */
    public static AlphaTreeTraversalHeuristic getDefault() {
        return alpha(1.0);
    }


    /**
     * Creates a new AlphaTreeTraversalHeuristics-parameter.
     * @param a Value for alpha.
     */
    public AlphaTreeTraversalHeuristic(double a) {
        this.alpha = a;
    }

    /**
     * Returns the value of the AlphaTreeTraversalHeuristics-parameter.
     * @return Alpha.
     */
    public double getAlpha() {
        return alpha;
    }

    /**
     * Returns a string with the name of the parameter and its value.
     * @return String.
     */
    @Override
    public String toString() {
        return "TreeTraversalHeuristicParameter(alpha=" + alpha + ")";
    }
}
