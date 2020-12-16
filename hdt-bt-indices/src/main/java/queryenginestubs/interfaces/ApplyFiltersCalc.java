package queryenginestubs.interfaces;

import btindices.statisticalquerygeneration.QueryModel;

import java.util.ArrayList;

public interface ApplyFiltersCalc {

    public ArrayList<String> applyFacets(QueryModel qm);
}
