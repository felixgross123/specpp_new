package org.processmining.estminer.specpp.datastructures.tree.base;

import org.processmining.estminer.specpp.datastructures.tree.base.impls.AbstractLocalNode;

public interface ChildGenerator<P extends NodeProperties, S extends NodeState, N extends AbstractLocalNode<P, S, N>> {

    N generateChild(N parent);

    boolean hasChildrenLeft(N parent);

    int potentialChildrenCount(N parent);

    Iterable<N> potentialChildren(N parent);
}