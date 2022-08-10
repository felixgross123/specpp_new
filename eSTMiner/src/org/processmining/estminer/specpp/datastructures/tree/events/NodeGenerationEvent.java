package org.processmining.estminer.specpp.datastructures.tree.events;

import org.processmining.estminer.specpp.datastructures.tree.base.TreeNode;

public class NodeGenerationEvent<N extends TreeNode> extends TreeNodeEvent<N> {
    public NodeGenerationEvent(N source) {
        super(source);
    }

    @Override
    public String toString() {
        return "GenerationEvent(" + source + ")";
    }

}