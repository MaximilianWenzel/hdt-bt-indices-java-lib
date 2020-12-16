package btindexmodels.facetedsearchmodels;

import java.io.Serializable;

public class SemanticAnnotation implements Serializable {
    private static final long serialVersionUID = 1L;

    public String facet;
    public String facetValue;
    public boolean facetValueIsObject;

    public SemanticAnnotation clone() {
        SemanticAnnotation clone = new SemanticAnnotation();
        clone.facet = "" + facet;
        clone.facetValue = "" + facetValue;
        clone.facetValueIsObject = facetValueIsObject;
        return clone;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof SemanticAnnotation) {
            SemanticAnnotation s = (SemanticAnnotation) o;
            if (facet.equals(s.facet)
                && facetValue.equals(s.facetValue)
                && facetValueIsObject == s.facetValueIsObject) {
                return true;
            }
        }
        return false;
    }
}
