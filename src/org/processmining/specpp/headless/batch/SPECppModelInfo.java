package org.processmining.specpp.headless.batch;

import org.processmining.specpp.base.impls.SPECpp;
import org.processmining.specpp.composition.BasePlaceComposition;
import org.processmining.specpp.datastructures.petri.CollectionOfPlaces;
import org.processmining.specpp.datastructures.petri.Place;
import org.processmining.specpp.datastructures.petri.ProMPetrinetWrapper;

public class SPECppModelInfo extends BatchedExecutionResult {

    public static final String[] COLUMN_NAMES = new String[]{"run identifier", "initial place count", "post processed place count", "post processed arc count", "post processed avg. out-degree", "post processed avg. in-degree"};
    private final int initialPlaceCount;
    private final int postProcessedPlaceCount;
    private final int postProcessedArcsCount;
    private final double postProcessedAvgOutDegree;
    private final double postProcessedAvgInDegree;

    public SPECppModelInfo(String runIdentifier, SPECpp<Place, BasePlaceComposition, CollectionOfPlaces, ProMPetrinetWrapper> specpp) {
        super(runIdentifier, "SPECppModelInfo");
        initialPlaceCount = specpp.getInitialResult() != null ? specpp.getInitialResult().size() : -1;
        postProcessedPlaceCount = specpp.getPostProcessedResult() != null ? specpp.getPostProcessedResult()
                                                                                  .getPlaces()
                                                                                  .size() : -1;
        postProcessedArcsCount = specpp.getPostProcessedResult() != null ? specpp.getPostProcessedResult().getEdges().size(): -1;

        double avgOutDegree = 0;
        double avgInDegree = 0;

        if(specpp.getPostProcessedResult() != null) {
            for (org.processmining.models.graphbased.directed.petrinet.elements.Transition t : specpp.getPostProcessedResult().getTransitions()) {
                avgOutDegree += specpp.getPostProcessedResult().getOutEdges(t).size();
            }
            avgOutDegree /= specpp.getPostProcessedResult().getTransitions().size();

            for (org.processmining.models.graphbased.directed.petrinet.elements.Transition t : specpp.getPostProcessedResult().getTransitions()) {
                avgInDegree += specpp.getPostProcessedResult().getInEdges(t).size();
            }
            avgInDegree /= specpp.getPostProcessedResult().getTransitions().size();
        }
        postProcessedAvgOutDegree = specpp.getPostProcessedResult() != null ? avgOutDegree : -1;
        postProcessedAvgInDegree = specpp.getPostProcessedResult() != null ? avgInDegree : -1;
    }

    public SPECppModelInfo(String runIdentifier, int initialPlaceCount, int postProcessedPlaceCount, int postProcessedArcsCount, double postProcessedAvgOutDegree, double postProcessedAvgInDegree) {
        super(runIdentifier, "SPECppModelInfo");
        this.initialPlaceCount = initialPlaceCount;
        this.postProcessedPlaceCount = postProcessedPlaceCount;
        this.postProcessedArcsCount = postProcessedArcsCount;
        this.postProcessedAvgOutDegree = postProcessedAvgOutDegree;
        this.postProcessedAvgInDegree = postProcessedAvgInDegree;
    }


    @Override
    public String[] getColumnNames() {
        return COLUMN_NAMES;
    }

    @Override
    public String[] toRow() {
        return new String[]{runIdentifier, Integer.toString(initialPlaceCount), Integer.toString(postProcessedPlaceCount), Integer.toString(postProcessedArcsCount), Double.toString(postProcessedAvgOutDegree), Double.toString(postProcessedAvgInDegree)};
    }
}
