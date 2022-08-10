package org.processmining.estminer.specpp.datastructures.tree.events;

import org.processmining.estminer.specpp.datastructures.tree.base.TreeNode;
import org.processmining.estminer.specpp.supervision.observations.TreeEvent;

public abstract class TreeNodeEvent<N extends TreeNode> implements TreeEvent {

    protected final N source;

    protected TreeNodeEvent(N source) {
        this.source = source;
    }

    public N getSource() {
        return source;
    }

}