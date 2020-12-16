package queryenginestubs.queryexecutors;

import btindices.statisticalquerygeneration.QueryModel;
import queryenginestubs.QueryEngineStub;

import java.util.concurrent.Callable;

/**
 * This class is required in order to execute queries in a separate thread for the
 * efficiency comparison (-> timeout events are made possible).
 */
public abstract class QueryExecutor implements Callable<Boolean> {

    protected QueryEngineStub qeStub;
    protected QueryModel qm;

    public QueryExecutor(QueryEngineStub qeStub, QueryModel qm) {
        this.qeStub = qeStub;
        this.qm = qm;
    }

    /**
     * This method is called from a specific ExecutorService Object if a query is executed.
     */
    public abstract Boolean call() throws Exception;
}
