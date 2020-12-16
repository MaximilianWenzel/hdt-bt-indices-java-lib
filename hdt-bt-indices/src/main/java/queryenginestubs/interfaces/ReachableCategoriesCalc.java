package queryenginestubs.interfaces;

import btindexmodels.categoryexplorationmodels.CenterHashSets;
import btindexmodels.categoryexplorationmodels.ConnectedPredicates;
import btindexmodels.categoryexplorationmodels.ReachableCategories;
import btindices.statisticalquerygeneration.QueryModel;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

import java.net.URI;

/**
 * Represents the basis for the comparison of the reachable categories computation for a given center.
 */
public interface ReachableCategoriesCalc {

    /**
     * Calculates all reachable categories for the given center.
     */
    public ReachableCategories calcReachableCategories(UnifiedSet<Long> hsSubject, UnifiedSet<Long> hsObject, URI centerType);

    /**
     * Calculates the resources of the center in terms of IDs after the query model has been executed.
     */
    public CenterHashSets getHashSetFromQueryModel(QueryModel qm);

    /**
     * Calculates all connected predicates for the given center.
     */
    public ConnectedPredicates[] calcReachableCategoriesPreds(UnifiedSet<Long> hsSubject, UnifiedSet<Long> hsObject, URI centerType);
}
