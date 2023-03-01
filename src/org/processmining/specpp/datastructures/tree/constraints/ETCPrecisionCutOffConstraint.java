package org.processmining.specpp.datastructures.tree.constraints;

import org.processmining.specpp.base.impls.CandidateConstraint;
import org.processmining.specpp.datastructures.petri.Place;

/**
 * Class representing the Constraint for cutting off subtrees when using the ETC-based Composer
 * (if FlagETCPrecisionCutOff = true).
 */
public class ETCPrecisionCutOffConstraint extends CandidateConstraint<Place> {

    /**
     * Creates a new ETCPrecisionCutOffConstraint.
     * @param affectedPlace Candidate place.
     */
    public ETCPrecisionCutOffConstraint(Place affectedPlace) {
        super(affectedPlace);
    }

    /**
     * Returns a string describing the constraint.
     * @return String.
     */
    @Override
    public String toString() {
        return "ETCPrecisionCutOffConstrainedPlace(" + getAffectedCandidate() + ")";
    }
}
