package queryenginestubs.queryexecutors;

import queryenginestubs.interfaces.HybridQueryCalc;
import queryenginestubs.QueryEngineStub;
import btindices.statisticalquerygeneration.QueryModel;

public class HybridQueryExecutor extends QueryExecutor {

    HybridQueryCalc hybridQueryStub;

    public HybridQueryExecutor(QueryEngineStub qeStub, QueryModel qm) {
        super(qeStub, qm);
        qm.onlyCount = false;
        hybridQueryStub = (HybridQueryCalc) qeStub;
    }

    @Override
    public Boolean call() throws Exception {
        hybridQueryStub.executeHybridQuery(qm);
        return true;
    }
}
