package org.processmining.specpp.config.parameters;

/**
 * Parameter indicating whether to cut off subtrees when using the ETC-based composer.
 */
public class FlagETCPrecisionCutOff implements Parameters {


    /**
     * Flag.
     */
    private final boolean cutOff;

    /**
     * Creates a new FlagETCPrecisionCutOff-parameter.
     * @param cutOff Value.
     * @return FlagETCPrecisionCutOff-parameter.
     */
    public static FlagETCPrecisionCutOff cutOff(boolean cutOff) {
        return new FlagETCPrecisionCutOff(cutOff);
    }

    /**
     * Returns the FlagETCPrecisionCutOff-parameter with its default value false.
     * @return FlagETCPrecisionCutOff-parameter.
     */
    public static FlagETCPrecisionCutOff getDefault() {
        return cutOff(false);
    }

    /**
     * Creates a new FlagETCPrecisionCutOff-parameter.
     * @param cutOff Value.
     */
    public FlagETCPrecisionCutOff(boolean cutOff) {
        this.cutOff = cutOff;
    }

    /**
     * Returns the value of the FlagETCPrecisionCutOff-parameter.
     * @return Value.
     */
    public boolean getCutOff() {
        return cutOff;
    }

}
