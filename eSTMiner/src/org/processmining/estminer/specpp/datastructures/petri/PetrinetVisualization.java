package org.processmining.estminer.specpp.datastructures.petri;

import org.processmining.estminer.specpp.supervision.observations.Visualization;
import org.processmining.graphvisualizers.algorithms.GraphVisualizerAlgorithm;
import org.processmining.plugins.graphviz.visualisation.DotPanel;

import javax.swing.*;

public class PetrinetVisualization extends Visualization<DotPanel> {
    public PetrinetVisualization(String title, DotPanel dotPanel) {
        super(title, dotPanel);
    }

    public static PetrinetVisualization of(ProMPetrinetWrapper petrinetWrapper) {
        return new PetrinetVisualization(petrinetWrapper.getLabel(), visPetrinetWrapper(petrinetWrapper));
    }

    private static DotPanel visPetrinetWrapper(ProMPetrinetWrapper petrinetWrapper) {
        GraphVisualizerAlgorithm alg = new GraphVisualizerAlgorithm();
        return (DotPanel) alg.apply(null, petrinetWrapper.getNet());
    }
}
