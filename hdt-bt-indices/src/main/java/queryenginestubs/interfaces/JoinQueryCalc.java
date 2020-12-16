package queryenginestubs.interfaces;

import btindices.statisticalquerygeneration.QueryModel;

import java.util.ArrayList;

public interface JoinQueryCalc {

    public ArrayList<String> executeQuery(QueryModel qm);
}
