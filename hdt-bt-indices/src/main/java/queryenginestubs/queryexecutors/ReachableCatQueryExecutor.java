package queryenginestubs.queryexecutors;

import btindexmodels.categoryexplorationmodels.CenterHashSets;
import queryenginestubs.QueryEngineStub;
import queryenginestubs.interfaces.ReachableCategoriesCalc;
import btindices.statisticalquerygeneration.QueryModel;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

import java.net.URI;

public class ReachableCatQueryExecutor extends QueryExecutor {

    protected ReachableCategoriesCalc rcCalc;
    protected URI centerURI;
    protected UnifiedSet<Long> hsSubject;
    protected UnifiedSet<Long> hsObject;

    public ReachableCatQueryExecutor(QueryEngineStub qeStub, QueryModel qm) {
        super(qeStub, qm);
        rcCalc = (ReachableCategoriesCalc) qeStub;

        CenterHashSets chs = new CenterHashSets();
        chs = rcCalc.getHashSetFromQueryModel(qm);
        hsSubject = chs.hsSubject;
        hsObject = chs.hsObject;
        centerURI = qm.types.get(0);
    }

    @Override
    public Boolean call() throws Exception {
        rcCalc.calcReachableCategories(hsSubject, hsObject, centerURI);
        return true;
    }
}
