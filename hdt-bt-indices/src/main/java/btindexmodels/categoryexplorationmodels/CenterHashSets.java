package btindexmodels.categoryexplorationmodels;

import org.eclipse.collections.impl.set.mutable.UnifiedSet;

/**
 * Represents all resources of the current center of the exploration state by distinguishing between subject and
 * object IDs.
 */
public class CenterHashSets {

    public UnifiedSet<Long> hsSubject;
    public UnifiedSet<Long> hsObject;

    public CenterHashSets () {

    }
    public CenterHashSets(UnifiedSet<Long> hsSubject, UnifiedSet<Long> hsObject) {
        this.hsSubject = hsSubject;
        this.hsObject = hsObject;
    }
}
