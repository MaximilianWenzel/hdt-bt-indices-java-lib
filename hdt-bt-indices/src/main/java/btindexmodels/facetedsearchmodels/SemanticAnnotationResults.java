package btindexmodels.facetedsearchmodels;

import org.rdfhdt.hdt.dictionary.Dictionary;
import org.rdfhdt.hdt.enums.TripleComponentRole;

public class SemanticAnnotationResults implements Comparable<SemanticAnnotationResults> {
    public Long numAnnotatedResults;
    public Long facetID;
    public Long facetValue;
    public boolean facetValueIsObject;

    @Override
    public int compareTo(SemanticAnnotationResults o) {
        int compare = Long.compare(numAnnotatedResults, o.numAnnotatedResults);
        if (compare == 0) {
            compare = facetID.compareTo(o.facetID);
            if (compare == 0) {
                compare = facetID.compareTo(o.facetValue);
                if (compare == 0) {
                    return -1;
                }
            }
        }
        return compare;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof SemanticAnnotationResults) {
            SemanticAnnotationResults s = (SemanticAnnotationResults) o;
            return numAnnotatedResults == s.numAnnotatedResults
                    && facetID == s.facetID
                    && facetValue == s.facetValue
                    && facetValueIsObject == s.facetValueIsObject;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + numAnnotatedResults.intValue();
        result = 31 * result + facetID.intValue();
        result = 31 * result + facetValue.intValue();
        result = 31 * result + (facetValueIsObject ? 1 : 0);

        return result;
    }

    public SemanticAnnotation getSemanticAnnotation(Dictionary dic) {
        SemanticAnnotation s = new SemanticAnnotation();
        s.facet = dic.idToString(facetID, TripleComponentRole.PREDICATE).toString();
        s.facetValue = dic.idToString(facetValue, TripleComponentRole.OBJECT).toString();
        s.facetValueIsObject = this.facetValueIsObject;
        return s;
    }
}
