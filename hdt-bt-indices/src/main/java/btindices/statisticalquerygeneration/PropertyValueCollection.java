package btindices.statisticalquerygeneration;

import btindexmodels.categoryexplorationmodels.CatConnection;
import btindexmodels.facetedsearchmodels.SemanticAnnotation;
import btindexmodels.facetedsearchmodels.SemanticAnnotationResults;
import btindices.indicesmanager.PVIndicesManager;
import btindices.RDFUtilities;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.rdfhdt.hdt.dictionary.Dictionary;
import org.rdfhdt.hdt.enums.TripleComponentRole;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.triples.IteratorTripleID;
import org.rdfhdt.hdt.triples.TripleID;

import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeSet;

/**
 * Represents a class to collect all property-value pairs for a given RDF data set.
 */
public class PropertyValueCollection {

    HDT hdt;
    Dictionary dic;
    PVIndicesManager indicesManager;
    URI[] rdfClasses;

    public PropertyValueCollection(HDT hdt, PVIndicesManager indicesManager) {
        this.hdt = hdt;
        this.dic = hdt.getDictionary();
        this.indicesManager = indicesManager;
        rdfClasses = this.indicesManager.typeURIs;
    }


    /**
     * Get the top n property-value pairs for a given data set.
     * @param topNPropertyValues Specifies how many property-value pairs should be returned.
     * @return
     */
    public ArrayList<SemanticAnnotationResults> getPropertyValuePairs(int topNPropertyValues) {
        TreeSet<SemanticAnnotationResults> result = new TreeSet<>();
        Iterator<? extends CharSequence> predicates = dic.getPredicates().getSortedEntries();
        long numObjs = dic.getNobjects();
        TripleID tID = new TripleID(0, 0, 0);

        while (predicates.hasNext()) {
            String predStr = predicates.next().toString();

            if (predStr.equals(RDFUtilities.rdfType)) {
                continue;
            }

            long predID = dic.stringToId(predStr, TripleComponentRole.PREDICATE);
            tID.setPredicate(predID);

            IteratorTripleID itID;
            for (int i = 1; i <= numObjs; i++) {
                tID.setObject(i);
                itID = hdt.getTriples().search(tID);

                if (itID.estimatedNumResults() > 0) {
                    SemanticAnnotationResults r = new SemanticAnnotationResults();
                    r.facetID = tID.getPredicate();
                    r.facetValue = tID.getObject();
                    r.facetValueIsObject = true;
                    r.numAnnotatedResults = itID.estimatedNumResults();
                    result.add(r);
                }
            }
        }

        // extract top N property-value pairs
        return extractTopNPropertyValuePairs(result, topNPropertyValues);
    }

    private ArrayList<SemanticAnnotationResults> extractTopNPropertyValuePairs(TreeSet<SemanticAnnotationResults> propertyValuePairs, int n) {
        // extract top n results
        ArrayList<SemanticAnnotationResults> topNPropertyValuePairs = new ArrayList<>();
        Iterator<SemanticAnnotationResults> itSA = propertyValuePairs.descendingIterator();
        for (int count = 0; count < n && itSA.hasNext(); count++) {
            topNPropertyValuePairs.add(itSA.next());
        }
        return topNPropertyValuePairs;
    }

    /**
     * Uses the PV indices in order to obtain all property-value pairs which produce more results than a given threshold.
     */
    public ArrayList<SemanticAnnotationResults> getPropertyValuePairsWithPVIndices(long thresResults) {

        ArrayList<SemanticAnnotationResults> result = new ArrayList<>();

        TripleID tID = new TripleID(0, 0, 0);
        for (int i = 0; i < rdfClasses.length; i++) {

            System.out.println("Class: (" + (i + 1) + "/" + rdfClasses.length + ")");
            SemanticAnnotation annoClass = new SemanticAnnotation();
            annoClass.facet = RDFUtilities.rdfType;
            annoClass.facetValue = rdfClasses[i].toString();
            annoClass.facetValueIsObject = true;

            long countResults = 0;
            UnifiedSet<Long> predicates = new UnifiedSet<>();
            UnifiedSet<Long> objects = new UnifiedSet<>();

            tID.setSubject(0);
            tID.setPredicate(0);
            tID.setObject(0);
            IteratorTripleID itID = indicesManager.queryBTIndex(tID, rdfClasses[i].toString(), CatConnection.OUT);

            TripleID next = null;
            while (itID.hasNext()) {
                countResults++;

                tID = itID.next();
                predicates.add(tID.getPredicate());
                objects.add(tID.getObject());
            }

            if (countResults < thresResults) {
                // not relevant
                continue;
            }

            // for each predicate-object combination check if #results > threshold
            Iterator<Long> predIt = predicates.iterator();
            Iterator<Long> objIt = objects.iterator();

            while (predIt.hasNext()) {
                tID.setSubject(0);
                tID.setPredicate(predIt.next());

                while (objIt.hasNext()) {
                    tID.setObject(objIt.next());
                    itID = indicesManager.queryBTIndex(tID, rdfClasses[i].toString(), CatConnection.OUT);

                    // count results
                    long countPropertyValue = 0;
                    while (itID.hasNext()) {
                        countPropertyValue++;
                        itID.next();
                    }
                    if (countPropertyValue > thresResults) {

                        SemanticAnnotationResults r = new SemanticAnnotationResults();
                        r.facetID = tID.getPredicate();
                        r.facetValue = tID.getObject();
                        r.facetValueIsObject = true;
                        r.numAnnotatedResults = countPropertyValue;
                        result.add(r);
                    }
                }
                objIt = objects.iterator();
            } // each predicate
        } // each rdf class

        return result;
    }


}
