package org.processmining.specpp.headless.batch;

import org.processmining.models.graphbased.directed.DirectedGraph;
import org.processmining.models.graphbased.directed.DirectedGraphEdge;
import org.processmining.models.graphbased.directed.DirectedGraphNode;
import org.processmining.models.graphbased.directed.petrinet.PetrinetEdge;
import org.processmining.models.graphbased.directed.petrinet.PetrinetNode;
import org.processmining.specpp.base.impls.SPECpp;
import org.processmining.specpp.composition.BasePlaceComposition;
import org.processmining.specpp.datastructures.graph.Edge;
import org.processmining.specpp.datastructures.graph.Graph;
import org.processmining.specpp.datastructures.graph.Vertex;
import org.processmining.specpp.datastructures.petri.CollectionOfPlaces;
import org.processmining.specpp.datastructures.petri.Place;
import org.processmining.specpp.datastructures.petri.ProMPetrinetWrapper;
import org.processmining.specpp.postprocessing.AddDanglingTransitionPostProcessing;

import java.util.*;

public class SPECppModelInfo extends BatchedExecutionResult {

    public static final String[] COLUMN_NAMES = new String[]{"run identifier", "initial place count", "post processed place count", "post processed arc count", "dangling Transitions", "subcomponents"};
    private final int initialPlaceCount;
    private final int postProcessedPlaceCount;
    private final int postProcessedArcsCount;
    private final int danglingTransitions;
    private final int subcomponents;

    public SPECppModelInfo(String runIdentifier, SPECpp<Place, BasePlaceComposition, CollectionOfPlaces, ProMPetrinetWrapper> specpp) {
        super(runIdentifier, "SPECppModelInfo");
        initialPlaceCount = specpp.getInitialResult() != null ? specpp.getInitialResult().size() : -1;
        postProcessedPlaceCount = specpp.getPostProcessedResult() != null ? specpp.getPostProcessedResult()
                                                                                  .getPlaces()
                                                                                  .size() : -1;
        postProcessedArcsCount = specpp.getPostProcessedResult() != null ? specpp.getPostProcessedResult().getEdges().size(): -1;

        danglingTransitions = AddDanglingTransitionPostProcessing.danglingTransitions;


        subcomponents = specpp.getPostProcessedResult() != null ? (getSubcomponents(specpp.getPostProcessedResult()) > 0 ? getSubcomponents(specpp.getPostProcessedResult())-1 : 0) : -1;

    }

    public SPECppModelInfo(String runIdentifier, int initialPlaceCount, int postProcessedPlaceCount, int postProcessedArcsCount, int danglingTransitions, int subcomponents) {
        super(runIdentifier, "SPECppModelInfo");
        this.initialPlaceCount = initialPlaceCount;
        this.postProcessedPlaceCount = postProcessedPlaceCount;
        this.postProcessedArcsCount = postProcessedArcsCount;
        this.danglingTransitions = danglingTransitions;
        this.subcomponents = subcomponents;
    }


    @Override
    public String[] getColumnNames() {
        return COLUMN_NAMES;
    }

    @Override
    public String[] toRow() {
        return new String[]{runIdentifier, Integer.toString(initialPlaceCount), Integer.toString(postProcessedPlaceCount), Integer.toString(postProcessedArcsCount), Integer.toString(danglingTransitions), Integer.toString(subcomponents)};
    }

    public int getSubcomponents(ProMPetrinetWrapper model) {
        DirectedGraph<? extends DirectedGraphNode,? extends DirectedGraphEdge> g = model.getGraph();
        int components = 0;
        Map<DirectedGraphNode, Boolean> visited = new HashMap<>();

        for (DirectedGraphNode n : g.getNodes()) {
            visited.put(n, false);
        }
        for (DirectedGraphNode n : g.getNodes()) {
            if(!visited.get(n)) {
                DFS(g, n, visited);
                components++;
            }
        }
        return components;
    }

    public void DFS(DirectedGraph<?,?> g, DirectedGraphNode n, Map<DirectedGraphNode, Boolean> visited) {
        visited.put(n, true);

        for (DirectedGraphEdge<?, ?> edge : g.getOutEdges(n)) {
            if (!visited.get(edge.getTarget())) {
                DFS(g, edge.getTarget(), visited);
            }
        }
        for (DirectedGraphEdge<?, ?> edge : g.getInEdges(n)) {
            if (!visited.get(edge.getSource())) {
                DFS(g, edge.getSource(), visited);
            }
        }
    }

}
