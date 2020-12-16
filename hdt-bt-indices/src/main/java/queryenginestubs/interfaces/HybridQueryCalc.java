package queryenginestubs.interfaces;

import btindices.statisticalquerygeneration.QueryModel;

import java.util.ArrayList;

public interface HybridQueryCalc {

    public ArrayList<String> executeHybridQuery(QueryModel qm);
}
