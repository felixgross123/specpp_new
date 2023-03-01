package org.processmining.specpp.datastructures.tree.heuristic;

import org.processmining.specpp.base.Candidate;
import org.processmining.specpp.componenting.delegators.ContainerUtils;
import org.processmining.specpp.componenting.supervision.SupervisionRequirements;
import org.processmining.specpp.composition.events.CandidateCompositionEvent;
import org.processmining.specpp.datastructures.tree.base.HeuristicStrategy;
import org.processmining.specpp.datastructures.tree.base.LocalNode;
import org.processmining.specpp.datastructures.tree.base.NodeProperties;
import org.processmining.specpp.supervision.piping.Observer;
import org.processmining.specpp.util.JavaTypingUtils;

public class UpdatableHeuristicExpansionStrategy<C extends Candidate & NodeProperties, N extends LocalNode<C, ?, N>, H extends HeuristicValue<? super H>> extends HeuristicTreeExpansion<N, H> implements Observer<CandidateCompositionEvent<C>> {


    public UpdatableHeuristicExpansionStrategy(HeuristicStrategy<? super N, H> heuristicStrategy) {
        super(heuristicStrategy);
        localComponentSystem().require(SupervisionRequirements.observable("composer.events", JavaTypingUtils.castClass(CandidateCompositionEvent.class)), ContainerUtils.observeResults(this));
    }


    @Override
    protected void addNode(N node, H heuristic) {
        super.addNode(node, heuristic);
    }

    @Override
    protected void removeNode(N node) {
        super.removeNode(node);
    }

    @Override
    public void observe(CandidateCompositionEvent<C> observation) {

    }


}
