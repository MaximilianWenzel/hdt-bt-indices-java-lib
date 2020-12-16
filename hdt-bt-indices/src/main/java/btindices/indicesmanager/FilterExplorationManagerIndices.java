package btindices.indicesmanager;

import btindexmodels.categoryexplorationmodels.CatConnection;
import btindexmodels.facetedsearchmodels.AvailableFacet;
import btindexmodels.facetedsearchmodels.SemanticAnnotation;
import btindices.RDFUtilities;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.rdfhdt.hdt.enums.TripleComponentRole;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.triples.IteratorTripleID;
import org.rdfhdt.hdt.triples.TripleID;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class FilterExplorationManagerIndices extends FilterExplorationManager {


    public FilterExplorationManagerIndices(String hdtPath, String pvIndicesDir) {
        super(hdtPath);
        sw.reset();
        System.out.println("Loading BT indices...");
        indicesManager = new PVIndicesManager(hdt, pvIndicesDir);
        typeURIs = indicesManager.typeURIs;
        System.out.println("BT indices loaded. Time required: " + sw.stopAndShow());
        initThresholds();
    }

    public FilterExplorationManagerIndices(HDT hdt, String pvIndicesDir) {
        super(hdt);
        sw.reset();
        System.out.println("Loading BT indices...");
        indicesManager = new PVIndicesManager(hdt, pvIndicesDir);
        typeURIs = indicesManager.typeURIs;
        System.out.println("BT indices loaded. Time required: " + sw.stopAndShow());
        initThresholds();
    }

    private void initThresholds() {
        // thresholds (determined by experiments):
        thresAvailableFilters = 5000;
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

        center = getSubsetOfHashSet(center, facetID, facetValueID, outgoing);

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
        Iterator<Long> hsIt = hs.iterator();
        UnifiedSet<Long> updated = new UnifiedSet<>();
        Long id;

        if (hs.size() > thresApplyFilter) {
            if (outgoing) {
                // get annotated resources
                TripleID tID = new TripleID(0, facetID, facetValue);
                IteratorTripleID allAnnotatedResourcesIt = indicesManager.queryBTIndex(tID, currentType.toString(), CatConnection.OUT);

                while (allAnnotatedResourcesIt.hasNext()) {
                    id = allAnnotatedResourcesIt.next().getSubject();
                    if (hs.contains(id)) {
                        updated.add(id);
                    }
                }

            } else {
                // get annotated resources
                TripleID tID = new TripleID(facetValue, facetID, 0);
                IteratorTripleID allAnnotatedResourcesIt = indicesManager.queryBTIndex(tID, currentType.toString(), CatConnection.IN);

                while (allAnnotatedResourcesIt.hasNext()) {
                    id = allAnnotatedResourcesIt.next().getObject();
                    if (id <= dic.getNshared() && hs.contains(id)) {
                        updated.add(id);
                    }
                }
            }

            return updated;
        } else {

            TripleID tID;
            int rdfClassPosition = indicesManager.classToPosition.get(currentType.toString());
            IteratorTripleID itID;
            if (outgoing) {
                tID = new TripleID(0, facetID, facetValue);
                while (hsIt.hasNext()) {
                    id = hsIt.next();
                    tID.setSubject(id);
                    itID = indicesManager.queryBTIndex(tID, rdfClassPosition, CatConnection.OUT);
                    if (itID.hasNext()) {
                        updated.add(id);
                    }
                }
            } else {
                tID = new TripleID(facetValue, facetID, 0);
                while (hsIt.hasNext()) {
                    id = hsIt.next();
                    if (id <= dic.getNshared()) { // consider only objects which also occur as subjects
                        tID.setObject(id);
                        itID = indicesManager.queryBTIndex(tID, rdfClassPosition, CatConnection.IN);
                        if (itID.hasNext()) {
                            updated.add(id);
                        }
                    }
                }
            }
            return updated;
        }
    }

    private void calcInitialAvailableFacets() {

        calcInitialOutgoingConnections();
        calcInitialIncomingConnections();

        SemanticAnnotation sa = new SemanticAnnotation();
        sa.facet = RDFUtilities.rdfType;
        sa.facetValue = currentType.toString();
        appliedFacets.add(sa);

        // change triple component role of center resources to subjects
        // because all type annotated resources are automatically subjects
        centerRole = TripleComponentRole.SUBJECT;
    }

    private void calcInitialIncomingConnections() {
        TripleID tID = new TripleID(0, 0, 0);
        IteratorTripleID itID = indicesManager.queryBTIndex(tID, currentType.toString(), CatConnection.IN);
        HashMap<Long, UnifiedSet<Long>> predicateToSubjects = new HashMap<>();
        UnifiedSet<Long> hs;

        while (itID.hasNext()) {
            tID = itID.next();
            hs = predicateToSubjects.get(tID.getPredicate());
            if (hs == null) {
                UnifiedSet<Long> subjects = new UnifiedSet<>();
                subjects.add(tID.getSubject());
                predicateToSubjects.put(tID.getPredicate(), subjects);
            } else {
                hs.add(tID.getSubject());
            }
        }
        itID.goToStart();

        // set results to the available facets
        availableFacets.incomingConnections = new ArrayList<>();
        predicateToSubjects.forEach((predID, subjects) -> {
            AvailableFacet af = new AvailableFacet();
            af.predicate = dic.idToString(predID, TripleComponentRole.PREDICATE).toString();
            af.subjects = subjects;
            availableFacets.incomingConnections.add(af);
        });
    }

    private void calcInitialOutgoingConnections() {
        TripleID tID = new TripleID(0, 0, 0);
        IteratorTripleID itID = indicesManager.queryBTIndex(tID, currentType.toString(), CatConnection.OUT);
        HashMap<Long, UnifiedSet<Long>> predicateToObjects = new HashMap<>();
        UnifiedSet<Long> hs;

        while (itID.hasNext()) {
            tID = itID.next();
            hs = predicateToObjects.get(tID.getPredicate());
            if (hs == null) {
                UnifiedSet<Long> objects = new UnifiedSet<>();
                objects.add(tID.getObject());
                predicateToObjects.put(tID.getPredicate(), objects);
            } else {
                hs.add(tID.getObject());
            }
        }
        itID.goToStart();

        // set results to the available facets
        availableFacets.outgoingConnections = new ArrayList<>();
        predicateToObjects.forEach((predID, objects) -> {
            AvailableFacet af = new AvailableFacet();
            af.predicate = dic.idToString(predID, TripleComponentRole.PREDICATE).toString();
            af.objects = objects;
            availableFacets.outgoingConnections.add(af);
        });

    }

    private void calcAvailableFacets() {

        this.availableFacets.incomingConnections = calcAvailableIncomingFacets();
        this.availableFacets.outgoingConnections = calcAvailableOutgoingFacets();

    }

    private ArrayList<AvailableFacet> calcAvailableOutgoingFacets() {
        TripleID tID = new TripleID(0, 0, 0);
        IteratorTripleID itID;

        ArrayList<AvailableFacet> updatedAvailableFacets = new ArrayList<>();
        Iterator<Long> centerIt;

        UnifiedSet<Long> hsSubjects = getRequiredCenterResources(true);

        // get all available facets
        for (long predID = 1; predID < dic.getNpredicates(); predID++) {

            UnifiedSet<Long> objects = new UnifiedSet<>();

            if (hsSubjects.size() < thresAvailableFilters) {

                centerIt = hsSubjects.iterator();
                long sbj;
                while (centerIt.hasNext()) {
                    sbj = centerIt.next();
                    tID.setSubject(sbj);
                    tID.setPredicate(predID);
                    tID.setObject(0);
                    itID = hdt.getTriples().search(tID);
                    while (itID.hasNext()) {
                        tID = itID.next();
                        objects.add(tID.getObject());
                    }
                }
            } else {

                tID.setSubject(0);
                tID.setPredicate(predID);
                tID.setObject(0);
                itID = indicesManager.queryBTIndex(tID, currentType.toString(), CatConnection.OUT);

                while (itID.hasNext()) {
                    tID = itID.next();
                    if (hsSubjects.contains(tID.getSubject())) {
                        objects.add(tID.getObject());
                    }
                }
            }

            if (objects.size() > 0) {
                AvailableFacet af = new AvailableFacet();
                af.predicate = "" + dic.idToString(predID, TripleComponentRole.PREDICATE);
                af.objects = objects;
                updatedAvailableFacets.add(af);
            }
        }
        return updatedAvailableFacets;
    }

    private ArrayList<AvailableFacet> calcAvailableIncomingFacets() {
        TripleID tID = new TripleID(0, 0, 0);
        IteratorTripleID itID;

        ArrayList<AvailableFacet> updatedAvailableFacets = new ArrayList<>();
        Iterator<Long> centerIt;
        UnifiedSet<Long> hsObjects = getRequiredCenterResources(false);

        // get all available facets
        for (long predID = 1; predID <= dic.getNpredicates(); predID++) {
            UnifiedSet<Long> subjects = new UnifiedSet<>();

            if (hsObjects.size() < thresAvailableFilters) {

                centerIt = hsObjects.iterator();
                long obj;
                while (centerIt.hasNext()) {
                    obj = centerIt.next();
                    tID.setSubject(0);
                    tID.setPredicate(predID);
                    tID.setObject(obj);
                    itID = hdt.getTriples().search(tID);
                    while (itID.hasNext()) {
                        tID = itID.next();
                        subjects.add(tID.getSubject());
                    }
                }
            } else {

                tID.setSubject(0);
                tID.setPredicate(predID);
                tID.setObject(0);
                itID = indicesManager.queryBTIndex(tID, currentType.toString(), CatConnection.IN);

                while (itID.hasNext()) {
                    tID = itID.next();
                    if (hsObjects.contains(tID.getObject())) {
                        subjects.add(tID.getSubject());
                    }
                }
            }


            if (subjects.size() > 0) {
                AvailableFacet af = new AvailableFacet();
                af.predicate = "" + dic.idToString(predID, TripleComponentRole.PREDICATE);
                af.subjects = subjects;
                updatedAvailableFacets.add(af);
            }
        }
        return updatedAvailableFacets;
    }
}
