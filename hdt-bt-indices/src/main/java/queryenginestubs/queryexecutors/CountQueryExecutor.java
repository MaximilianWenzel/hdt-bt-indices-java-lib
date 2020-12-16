package queryenginestubs.queryexecutors;

import queryenginestubs.interfaces.JoinQueryCalc;
import queryenginestubs.QueryEngineStub;
import btindices.statisticalquerygeneration.QueryModel;

public class CountQueryExecutor extends QueryExecutor {
    JoinQueryCalc joinQueryStub;

    public CountQueryExecutor(QueryEngineStub qeStub, QueryModel qm) {
        super(qeStub, qm);
        qm.onlyCount = true;
        joinQueryStub = (JoinQueryCalc) qeStub;
    }

    @Override
    public Boolean call() throws Exception {
        joinQueryStub.executeQuery(qm);
        return true;
    }
}
