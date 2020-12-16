package queryenginestubs.queryexecutors;

import queryenginestubs.interfaces.JoinQueryCalc;
import queryenginestubs.QueryEngineStub;
import btindices.statisticalquerygeneration.QueryModel;

/**
 * Executes the query as verbose query, i.e., it returns all results as an array list of strings.
 */
public class VerboseQueryExecutor extends QueryExecutor {

    JoinQueryCalc joinQueryStub;

    public VerboseQueryExecutor(QueryEngineStub qeStub, QueryModel qm) {
        super(qeStub, qm);
        qm.onlyCount = false;
        joinQueryStub = (JoinQueryCalc) qeStub;
    }

    @Override
    public Boolean call() throws Exception {
        joinQueryStub.executeQuery(qm);
        return true;
    }
}
