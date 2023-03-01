package org.processmining.specpp.config.parameters;

/**
 * Parameter to guide the ETC-based composer.
 */
public class GammaETCPrecisionGainThreshold implements Parameters {

    /**
     * Value.
     */
    private final double gamma;

    /**
     * Creates a new GammaETCPrecisionGainThreshold-parameter.
     * @param g Value for gamma.
     * @return GammaETCPrecisionGainThreshold-parameter.
     */
    public static GammaETCPrecisionGainThreshold gamma(double g) {
        return new GammaETCPrecisionGainThreshold(g);
    }

    /**
     * Returns the GammaETCPrecisionGainThreshold-parameter with its default value 0.0
     * @return GammaETCPrecisionGainThreshold-parameter
     */
    public static GammaETCPrecisionGainThreshold getDefault() {
        return gamma(0.0);
    }

    /**
     * Creates a new GammaETCPrecisionGainThreshold-parameter.
     * @param g Value for gamma.
     */
    public GammaETCPrecisionGainThreshold(double g) {
        this.gamma = g;
    }

    /**
     * Returns the value of the GammaETCPrecisionGainThreshold-parameter.
     * @return Gamma.
     */
    public double getGamma() {
        return gamma;
    }

    /**
     * Returns a string with the  name of the parameter and its value.
     * @return String.
     */
    @Override
    public String toString() {
        return "ETCPrecisionGainThreshold(gamma=" + gamma + ")";
    }
}
