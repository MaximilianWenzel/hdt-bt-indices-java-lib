package queryenginestubs.interfaces;


import btindexmodels.facetedsearchmodels.AvailableFacets;
import btindices.statisticalquerygeneration.QueryModel;

/**
 * Represents the basis for the comparison of the available facets computation for a given center.
 */
public interface AvailableFiltersCalc {
    /**
     * Calculates the resources of the center in terms of IDs after the query model has been executed.
     */
    public void updateFSManagerState(QueryModel qm);

    /**
     * Calculates all available facets for the given center.
     */
    public AvailableFacets calcAvailableFacets();
}
