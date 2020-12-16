package queryenginestubs.queryexecutors;

import btindexmodels.facetedsearchmodels.AvailableFacets;
import btindices.statisticalquerygeneration.QueryModel;
import queryenginestubs.interfaces.AvailableFiltersCalc;
import queryenginestubs.QueryEngineStub;

public class AvailableFacetsQueryExecutor extends QueryExecutor {

    AvailableFiltersCalc afCalc;

    public AvailableFacetsQueryExecutor(QueryEngineStub qeStub, QueryModel qm) {
        super(qeStub, qm);
        qm.onlyCount = false;
        afCalc = (AvailableFiltersCalc) qeStub;
        afCalc.updateFSManagerState(qm);
    }

    @Override
    public Boolean call() throws Exception {
        AvailableFacets af = afCalc.calcAvailableFacets();
        return true;
    }
}
