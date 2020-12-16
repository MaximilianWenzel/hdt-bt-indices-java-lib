package queryenginestubs.queryexecutors;

import btindices.statisticalquerygeneration.QueryModel;
import queryenginestubs.interfaces.ApplyFiltersCalc;
import queryenginestubs.QueryEngineStub;

public class ApplyFacetsQueryExecutor extends QueryExecutor {

    ApplyFiltersCalc facetCalculator;

    public ApplyFacetsQueryExecutor(QueryEngineStub qeStub, QueryModel qm) {
        super(qeStub, qm);
        qm.onlyCount = false;
        facetCalculator = (ApplyFiltersCalc) qeStub;
    }

    @Override
    public Boolean call() throws Exception {
        facetCalculator.applyFacets(qm);
        return true;
    }
}
