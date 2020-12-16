package queryenginestubs;

import btindexmodels.facetedsearchmodels.AvailableFacets;
import btindices.indicesmanager.FilterExplorationManagerIndices;
import queryenginestubs.interfaces.ApplyFiltersCalc;
import queryenginestubs.interfaces.AvailableFiltersCalc;
import btindices.statisticalquerygeneration.QueryModel;

import java.util.ArrayList;

public class PVIndicesStub extends QueryEngineStub implements AvailableFiltersCalc, ApplyFiltersCalc {
    public FilterExplorationManagerIndices fsManager;

    public PVIndicesStub(String hdtPath, String pvIndicesDir) {
        fsManager = new FilterExplorationManagerIndices(hdtPath, pvIndicesDir);
    }

    @Override
    public void updateFSManagerState(QueryModel qm) {
        fsManager.updateFSManagerState(qm);
    }

    @Override
    public AvailableFacets calcAvailableFacets() {
        fsManager.updateAvailableFacets();
        return fsManager.availableFacets;
    }

    @Override
    public ArrayList<String> applyFacets(QueryModel qm) {
        return fsManager.applyFacetsAndInitToRDFType(qm);
    }
}
