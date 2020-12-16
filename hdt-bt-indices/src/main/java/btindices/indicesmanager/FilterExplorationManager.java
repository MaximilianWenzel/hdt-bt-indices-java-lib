package btindices.indicesmanager;

import btindexmodels.facetedsearchmodels.AvailableFacet;
import btindexmodels.facetedsearchmodels.AvailableFacets;
import btindexmodels.facetedsearchmodels.SemanticAnnotation;
import btindices.HDTUtil;
import btindices.statisticalquerygeneration.QueryModel;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.rdfhdt.hdt.dictionary.Dictionary;
import org.rdfhdt.hdt.enums.TripleComponentRole;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.triples.IteratorTripleID;
import org.rdfhdt.hdt.triples.TripleID;
import org.rdfhdt.hdt.util.StopWatch;

import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * A class which provides methods in order to explore a given HDT file by the principles of Faceted Navigation.
 */
public abstract class FilterExplorationManager {

    protected static final StopWatch sw = new StopWatch();
    public HDT hdt;
    public Dictionary dic;
    public URI currentType;
    public PVIndicesManager indicesManager;
    public URI[] typeURIs;
    public UnifiedSet<Long> center;
    /**
     * Indicates if center contains subject or object resources.
     */
    public TripleComponentRole centerRole;
    public AvailableFacets availableFacets;
    public ArrayList<SemanticAnnotation> appliedFacets;
    /**
     * If all available facets are calculated for the current set of resources and the number of resources "n" is less
     * than the specified threshold, for each resource a HDT search is performed in O(log(#hdtFileTriples)) and each
     * result is added in O(1) to the updated hash set: n * O(log(#hdtFileTriples)) + O(#result). If n >= threshold,
     * only one HDT search is performed on the POS index in order to fetch all resources which are annotated with the
     * corresponding facet. Subsequently, it is iterated over all fetched resources and it is examined whether the
     * resource is in the current center set: log(size(#triplesTypeIndex)) + O(#annotatedResourcesFromIndex))
     */
    public long thresAvailableFilters;

    /**
     * If the size of the current set of resources M while filtering the set is less than the threshold, for each
     * resource r from M it is examined if the triple (r, filterPred, filterValue) is contained in the bitmap triples.
     * Else the triple pattern (0, filterPred, filterValue) is resolved and all resources are stored to a hash set.
     * Subsequently, it is iterated over all resources from M and examined if they are present in the newly calculated
     * hash set.
     */
    public long thresApplyFilter;

    /**
     * All predicates of the HDT dictionary.
     */
    protected Iterator<? extends CharSequence> predicates;


    public FilterExplorationManager(String hdtPath) {

        System.out.println("Loading HDT file...");
        sw.reset();
        hdt = HDTUtil.loadHDTFile(hdtPath);
        System.out.println("HDT file loaded. Time required: " + sw.stopAndShow());
        thresAvailableFilters = 100000;
        thresApplyFilter = 100000;
        init();
    }

    public FilterExplorationManager(HDT hdt) {
        this.hdt = hdt;
        init();
    }

    private void init() {
        dic = hdt.getDictionary();
        predicates = dic.getPredicates().getSortedEntries();
        center = new UnifiedSet<>();
        centerRole = TripleComponentRole.SUBJECT;
        availableFacets = new AvailableFacets();
        appliedFacets = new ArrayList<>();
    }

    public void resetExplorationState() {
        center = new UnifiedSet<>();
        availableFacets = new AvailableFacets();
        appliedFacets = new ArrayList<>();
    }

    /**
     * Updates the current center to all resources which are of the given RDF class.
     */
    public void updateToInitialRDFClass(String rdfClass) {
        currentType = URI.create(rdfClass);

        currentType = URI.create(rdfClass);
        long rdfClassID = dic.stringToId(rdfClass, TripleComponentRole.OBJECT);

        TripleID tID = new TripleID(0, 0, rdfClassID);
        IteratorTripleID itID = hdt.getTriples().search(tID);

        center = HDTUtil.generateHashSetForIterator(itID, TripleComponentRole.SUBJECT);

        // all resources are subjects because they are annotated by rdf:type
        centerRole = TripleComponentRole.SUBJECT;
    }

    /**
     * Updates all available filters for the current set of resources in the center hash set.
     */
    public abstract void updateAvailableFacets();

    /**
     * Applies a filter to the current set of resources.
     *
     * @param outgoing     Indicates if an outgoing filter, i.e., f = (?s, pred, obj), or an incoming filter, i.e., f =
     *                     (sbj, pred, ?o), is applied to the current center.
     * @param facetID      Predicate ID which corresponds to the ID of the respective HDT dictionary.
     * @param facetValueID Object ID which corresponds to the ID of the HDT dictionary.
     */
    public abstract void applyFacet(boolean outgoing, long facetID, long facetValueID);

    /**
     * Returns a subset of a hash set which is annotated by the corresponding facet and facetValue, i.e., a given
     * property-value pair.
     *
     * @param outgoing If this value is true, the facet value acts as object, otherwise as subject.
     * @return
     */
    public abstract UnifiedSet<Long> getSubsetOfHashSet(UnifiedSet<Long> center, long facetID, long facetValue, boolean outgoing);

    /**
     * Apply the given semantic annotation, i.e., filter,  to the current center.
     *
     * @param sa
     */
    public void applyFacet(SemanticAnnotation sa) {
        long facetID = dic.stringToId(sa.facet, TripleComponentRole.PREDICATE);
        long facetValue = dic.stringToId(sa.facetValue, TripleComponentRole.OBJECT);
        applyFacet(sa.facetValueIsObject, facetID, facetValue);
    }

    public void displayAvailableFacets() {


        if (availableFacets == null) {
            System.out.println("No facets available.");
            return;
        }

        System.out.println("Incoming predicates:");
        displayAvailableFacetList(availableFacets.incomingConnections);
        System.out.println("Outgoing predicates:");
        displayAvailableFacetList(availableFacets.outgoingConnections);
    }

    private void displayAvailableFacetList(ArrayList<AvailableFacet> availableFacets) {
        String format = "%-10.10s %-10.10s %-60.60s %-15.15s%n";
        String facet;
        String facetStr;
        long numResults;
        String[] arr;

        for (int i = 0; i < availableFacets.size(); i++) {
            System.out.printf(format, "Position", "Facet-ID", "Facet-String", "#Results");
            System.out.println("-----------------------------------------------------------------------------------------------");
            facet = availableFacets.get(i).predicate;
            arr = facet.split("#");
            facetStr = arr[arr.length - 1];
            if (facetStr == null) {
                arr = facet.split("/");
                facetStr = arr[arr.length - 1];
            }
            long facetID = dic.stringToId(facet, TripleComponentRole.PREDICATE);
            numResults = availableFacets.get(i).objects.size();
            System.out.printf(format, "[" + i + "]", facetID, facetStr, numResults);
        }
    }

    public void displayFacetValues(boolean outgoing, int position) {

        ArrayList<AvailableFacet> availableFacets;

        if (outgoing) {
            availableFacets = this.availableFacets.outgoingConnections;
        } else {
            availableFacets = this.availableFacets.incomingConnections;
        }

        Iterator<Long> itFacetValues = availableFacets.get(position).objects.iterator();
        String facetValue;
        long facetID;
        System.out.println("Available facet values (only 100 results):");
        for (int i = 0; itFacetValues.hasNext() && i < 100; i++) {
            facetID = itFacetValues.next();
            facetValue = dic.idToString(facetID, TripleComponentRole.OBJECT).toString();
            System.out.println("ID: " + facetID + "  -> " + facetValue);
        }
    }

    public void displayInitialRDFClasses() {
        for (int i = 0; i < typeURIs.length; i++) {
            System.out.println("[" + i + "] " + typeURIs[i]);
        }
    }


    /**
     * Returns the resources of the current center which are required for a filter operation.
     *
     * @param centerAsSubject Indicates whether the resources of the center act a subjects in the current filter
     *                        operation.
     * @return
     */
    protected UnifiedSet<Long> getRequiredCenterResources(boolean centerAsSubject) {
        UnifiedSet<Long> hs;

        if (centerAsSubject) {
            // centerResources -- predicate --> facetValues
            if (centerRole.equals(TripleComponentRole.OBJECT)) {
                // get shared resources because only subjects are considered
                hs = HDTUtil.getSharedIDsFromHashSet(dic, center);
            } else {
                hs = center;
            }
        } else {
            // facetValues -- predicate --> centerResources
            if (centerRole.equals(TripleComponentRole.SUBJECT)) {
                // get shared resources because only objects are considered
                hs = HDTUtil.getSharedIDsFromHashSet(dic, center);
            } else {
                hs = center;
            }
        }

        return hs;
    }

    /**
     * Initializes the center with the corresponding type and applies all filters of the query model.
     */
    public void updateFSManagerState(QueryModel qm) {
        resetExplorationState();
        String rdfType = qm.types.get(qm.getCenter()).toString();
        updateToInitialRDFClass(rdfType);

        ArrayList<SemanticAnnotation> filters = qm.filters.get(qm.getCenter());

        for (int i = 0; i < filters.size(); i++) {
            updateAvailableFacets();
            SemanticAnnotation sa = filters.get(i);
            applyFacet(sa);
        }
    }

    public ArrayList<String> applyFacetsAndInitToRDFType(QueryModel qm) {
        resetExplorationState();
        String rdfType = qm.types.get(qm.getCenter()).toString();
        updateToInitialRDFClass(rdfType);

        return applyFacets(qm);

    }

    public ArrayList<String> applyFacets(QueryModel qm) {
        ArrayList<SemanticAnnotation> filters = qm.filters.get(qm.getCenter());
        for (int i = 0; i < filters.size(); i++) {
            SemanticAnnotation sa = filters.get(i);
            applyFacet(sa);
        }

        ArrayList<String> result = new ArrayList<>();
        if (qm.onlyCount) {
            result.add("" + center.size());
        } else {
            Iterator<Long> centerIt = center.iterator();
            while (centerIt.hasNext()) {
                String str = dic.idToString(centerIt.next(), TripleComponentRole.SUBJECT).toString();
                result.add(str);
            }
        }
        return result;
    }
}

