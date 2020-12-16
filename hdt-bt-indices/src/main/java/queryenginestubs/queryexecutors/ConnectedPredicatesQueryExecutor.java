package queryenginestubs.queryexecutors;

import btindexmodels.categoryexplorationmodels.CenterHashSets;
import btindices.statisticalquerygeneration.QueryModel;
import queryenginestubs.QueryEngineStub;
import queryenginestubs.interfaces.ReachableCategoriesCalc;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

import java.net.URI;

public class ConnectedPredicatesQueryExecutor extends QueryExecutor {
    protected ReachableCategoriesCalc cpCalc;
    protected URI centerURI;
    protected UnifiedSet<Long> hsSubject;
    protected UnifiedSet<Long> hsObject;

    public ConnectedPredicatesQueryExecutor(QueryEngineStub qeStub, QueryModel qm) {
        super(qeStub, qm);
        cpCalc = (ReachableCategoriesCalc) qeStub;

        CenterHashSets chs = new CenterHashSets();
        chs = cpCalc.getHashSetFromQueryModel(qm);
        hsSubject = chs.hsSubject;
        hsObject = chs.hsObject;
        centerURI = qm.types.get(0);
    }

    @Override
    public Boolean call() throws Exception {
        cpCalc.calcReachableCategoriesPreds(hsSubject, hsObject, centerURI);
        return true;
    }
}
