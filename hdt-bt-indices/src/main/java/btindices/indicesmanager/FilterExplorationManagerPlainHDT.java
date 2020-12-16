package btindices.indicesmanager;

import btindices.HDTUtil;
import btindexmodels.facetedsearchmodels.AvailableFacet;
import btindexmodels.facetedsearchmodels.SemanticAnnotation;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.rdfhdt.hdt.enums.TripleComponentRole;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.triples.IteratorTripleID;
import org.rdfhdt.hdt.triples.TripleID;

import java.util.ArrayList;
import java.util.Iterator;

public class FilterExplorationManagerPlainHDT extends FilterExplorationManager {


    public FilterExplorationManagerPlainHDT(String hdtPath) {
        super(hdtPath);
        typeURIs = HDTUtil.getRDFTypesAsArray(hdt);
        initThresholds();
    }

    public FilterExplorationManagerPlainHDT(HDT hdt) {
        super(hdt);
        typeURIs = HDTUtil.getRDFTypesAsArray(hdt);
        initThresholds();
    }

    private void initThresholds() {
        // thresholds (determined by experiments):
        thresAvailableFilters = 100000;
        thresApplyFilter = 5000;
    }

    @Override
    public void updateAvailableFacets() {
        if (appliedFacets.size() == 0) {
            calcInitialAvailableFacets();
        } else {
            calcAvailableFacets();
        }
    }

    @Override
    public void applyFacet(boolean outgoing, long facetID, long facetValueID) {

        center  = getSubsetOfHashSet(center, facetID, facetValueID, outgoing);

        // add applied facet to "history"
        SemanticAnnotation sa = new SemanticAnnotation();
        sa.facet = dic.idToString(facetID, TripleComponentRole.PREDICATE).toString();
        if (outgoing) {
            sa.facetValue = dic.idToString(facetValueID, TripleComponentRole.OBJECT).toString();
            sa.facetValueIsObject = true;
        } else {
            sa.facetValue = dic.idToString(facetValueID, TripleComponentRole.SUBJECT).toString();
            sa.facetValueIsObject = false;
        }
        appliedFacets.add(sa);
    }

    @Override
    public UnifiedSet<Long> getSubsetOfHashSet(UnifiedSet<Long> center, long facetID, long facetValue, boolean outgoing) {
        UnifiedSet<Long> hs = getRequiredCenterResources(outgoing);
        Long id;
        UnifiedSet<Long> updated = new UnifiedSet<>();

        if (hs.size() > thresApplyFilter) {

            if (outgoing) {
                // get annotated resources
                TripleID tID = new TripleID(0, facetID, facetValue);
                IteratorTripleID allAnnotatedResourcesIt = hdt.getTriples().search(tID);

                while (allAnnotatedResourcesIt.hasNext()) {
                    id = allAnnotatedResourcesIt.next().getSubject();
                    if (hs.contains(id)) {
                        updated.add(new Long(id));
                    }
                }
                return updated;

            } else {
                // get annotated resources
                TripleID tID = new TripleID(facetValue, facetID, 0);
                IteratorTripleID allAnnotatedResourcesIt = hdt.getTriples().search(tID);

                while (allAnnotatedResourcesIt.hasNext()) {
                    id = allAnnotatedResourcesIt.next().getObject();
                    if (id <= dic.getNshared() && hs.contains(id)) {
                        updated.add(new Long(id));
                    }
                }
                return updated;
            }

        } else {
            Iterator<Long> centerIt = center.iterator();
            IteratorTripleID itID;
            if (outgoing) {
                // get annotated resources
                TripleID tID = new TripleID(0, facetID, facetValue);

                while (centerIt.hasNext()) {
                    id = centerIt.next();
                    tID.setSubject(id);
                    itID = hdt.getTriples().search(tID);
                    if (itID.hasNext()) {
                        updated.add(new Long(id));
                    }
                }
                return updated;

            } else {
                // get annotated resources
                TripleID tID = new TripleID(facetValue, facetID, 0);

                while (centerIt.hasNext()) {
                    id = centerIt.next();
                    if (id <= dic.getNshared()) { // consider only objects which also occur as subjects
                        tID.setObject(id);
                        itID = hdt.getTriples().search(tID);
                        if (itID.hasNext()) {
                            updated.add(new Long(id));
                        }
                    }
                }
                return updated;

            }
        }
    }

    private void calcInitialAvailableFacets() {
        calcInitialAvailableFacets(true);
        calcInitialAvailableFacets(false);
    }

    private void calcInitialAvailableFacets(boolean outgoing){

        UnifiedSet<Long> hs = getRequiredCenterResources(outgoing);

        String predStr;
        long predID;
        TripleID tID = new TripleID(0, 0, 0);
        IteratorTripleID itID;

        predicates = dic.getPredicates().getSortedEntries();
        // count results and create facet values hash sets for each predicate
        while (predicates.hasNext()) {
            predStr = predicates.next().toString();
            predID = dic.stringToId(predStr, TripleComponentRole.PREDICATE);

            tID.setSubject(0);
            tID.setPredicate(predID);
            tID.setObject(0);
            itID = hdt.getTriples().search(tID);

            UnifiedSet<Long> facetValues = new UnifiedSet<>();

            if (outgoing) {
                while(itID.hasNext()) {
                    tID = itID.next();
                    if (hs.contains(tID.getSubject()))
                        facetValues.add(tID.getObject());
                }
            } else {
                while(itID.hasNext()) {
                    tID = itID.next();
                    if (hs.contains(tID.getObject()))
                        facetValues.add(tID.getSubject());
                }
            }


            if (facetValues.size() > 0) {

                AvailableFacet af = new AvailableFacet();
                af.predicate = "" + predStr;
                if (outgoing) {
                    af.objects = facetValues;
                    availableFacets.outgoingConnections.add(af);
                } else {
                    af.subjects = facetValues;
                    availableFacets.incomingConnections.add(af);
                }
            }
        }
        // go to start of iterator
        predicates = dic.getPredicates().getSortedEntries();
    }

    private void calcAvailableFacets() {
        calcAvailableFacets(true);
        calcAvailableFacets(false);

    }

    private void calcAvailableFacets(boolean outgoing) {
        UnifiedSet<Long> hs = getRequiredCenterResources(outgoing);

        // at least one filter has been applied to the data set
        TripleID tID = new TripleID(0, 0, 0);
        IteratorTripleID itID;

        ArrayList<AvailableFacet> updatedAvailableFacets = new ArrayList<>();
        Iterator<Long> centerIt;
        // get all available facets
        for (long predID = 1; predID <= dic.getNpredicates(); predID++) {
            // new facet values -> two different approaches
            UnifiedSet<Long> facetValues = new UnifiedSet<>();
            centerIt = hs.iterator();
            if (hs.size() > thresAvailableFilters) {
                // iterate over all triples for all semantic annotations with the appropriate predicate
                tID.setSubject(0);
                tID.setPredicate(predID);
                tID.setObject(0);
                itID = hdt.getTriples().search(tID);

                if (outgoing) {
                    while(itID.hasNext()) {
                        tID = itID.next();
                        if (hs.contains(tID.getSubject()))
                            facetValues.add(tID.getObject());
                    }
                } else {
                    while(itID.hasNext()) {
                        tID = itID.next();
                        if (tID.getObject() <= dic.getNshared() && hs.contains(tID.getObject()))
                            facetValues.add(tID.getSubject());
                    }
                }
            } else {
                // iterate over all resources and save their semantic annotation in a hash set
                tID.setSubject(0);
                tID.setPredicate(predID);
                tID.setObject(0);
                if (outgoing) {
                    tID.setObject(0);
                    while (centerIt.hasNext()) {
                        tID.setSubject(centerIt.next());
                        itID = hdt.getTriples().search(tID);
                        while (itID.hasNext()) {
                            facetValues.add(itID.next().getObject());
                        }
                    }
                } else {
                    tID.setSubject(0);
                    long nextObj;
                    while (centerIt.hasNext()) {
                        nextObj = centerIt.next();
                        if (nextObj > dic.getNshared()) // only shared resources relevant
                            continue;
                        tID.setObject(nextObj);
                        itID = hdt.getTriples().search(tID);
                        while (itID.hasNext()) {
                            facetValues.add(itID.next().getSubject());
                        }
                    }
                }
            }

            if (facetValues.size() > 0) {
                AvailableFacet af = new AvailableFacet();
                af.predicate = "" + dic.idToString(predID, TripleComponentRole.PREDICATE);
                if (outgoing) {
                    af.objects = facetValues;
                } else {
                    af.subjects = facetValues;
                }
                updatedAvailableFacets.add(af);
            }
        }

        if (outgoing) {
            availableFacets.outgoingConnections = updatedAvailableFacets;
        } else {
            availableFacets.incomingConnections = updatedAvailableFacets;
        }
    }
}
