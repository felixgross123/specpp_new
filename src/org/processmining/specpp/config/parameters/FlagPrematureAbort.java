package org.processmining.specpp.config.parameters;

/**
 * Parameter indicating whether to prematurely abort the search once the Rho ETC-Precision Threshold
 * is reached when using the ETC-based composer.
 */
public class FlagPrematureAbort implements Parameters {

    /**
     * Flag.
     */
    private final boolean prematureAbort;

    /**
     * Creates a new FlagPrematureAbort-parameter.
     * @param prematureAbort Value.
     * @return FlagPrematureAbort-parameter.
     */
    public static FlagPrematureAbort prematureAbort(boolean prematureAbort) {
        return new FlagPrematureAbort(prematureAbort);
    }

    /**
     * Returns the FlagPrematureAbort-parameter with its default value true.
     * @return FlagETCPrecisionCutOff-parameter.
     */
    public static FlagPrematureAbort getDefault() {
        return prematureAbort(true);
    }


    /**
     * Creates a new FlagPrematureAbort-parameter.
     * @param prematureAbort Value.
     */
    public FlagPrematureAbort(boolean prematureAbort) {
        this.prematureAbort = prematureAbort;
    }

    /**
     * Returns the value of the FlagPrematureAbort-parameter.
     * @return Value.
     */
    public boolean getPrematureAbort() {
        return prematureAbort;
    }

}
