package queryenginestubs;

import btindices.indicesmanager.CatExplorationManager;
import btindices.indicesmanager.FilterExplorationManager;
import btindices.indicesmanager.FilterExplorationManagerIndices;
import btindices.statisticalquerygeneration.QueryModel;
import queryenginestubs.interfaces.HybridQueryCalc;

import java.util.ArrayList;

public class CtCPVHybridStub extends QueryEngineStub implements HybridQueryCalc {
    CatExplorationManager catManager;
    FilterExplorationManager filterManager;

    public CtCPVHybridStub(String hdtPath, String ctcIndicesDir, String pvIndicesDir) {
        catManager = new CatExplorationManager(hdtPath, ctcIndicesDir);
        filterManager = new FilterExplorationManagerIndices(catManager.hdt, pvIndicesDir);
    }

    @Override
    public ArrayList<String> executeHybridQuery(QueryModel qm) {
        catManager.updateExplorationState(qm);
        filterManager.currentType = catManager.currentURI;
        filterManager.center = catManager.hsSubject;
        return filterManager.applyFacets(qm);
    }
}
