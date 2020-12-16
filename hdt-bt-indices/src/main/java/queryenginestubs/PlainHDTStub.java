package queryenginestubs;

import btindexmodels.categoryexplorationmodels.*;
import btindexmodels.facetedsearchmodels.AvailableFacets;
import btindices.HDTUtil;
import btindices.indicesmanager.FilterExplorationManagerPlainHDT;
import btindices.statisticalquerygeneration.QueryModel;
import btindices.RDFUtilities;
import queryenginestubs.interfaces.*;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.rdfhdt.hdt.dictionary.Dictionary;
import org.rdfhdt.hdt.enums.TripleComponentRole;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.triples.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class PlainHDTStub extends QueryEngineStub implements JoinQueryCalc, ReachableCategoriesCalc,
        ApplyFiltersCalc, AvailableFiltersCalc, HybridQueryCalc {

    private HDT hdt;
    private Dictionary dic;

    IteratorTripleID itID;
    UnifiedSet<Long> hsSubject;
    UnifiedSet<Long> hsObject;

    UnifiedSet<Long> hsForJoin;
    private long rdfTypePred;
    public FilterExplorationManagerPlainHDT fsManager;

    // get all types
    private URI[] rdfTypes;


    public PlainHDTStub(String hdtPath) {
        hdt = HDTUtil.loadHDTFile(hdtPath);
        dic = hdt.getDictionary();
        this.rdfTypePred = dic.stringToId(RDFUtilities.rdfType, TripleComponentRole.PREDICATE);
        fsManager = new FilterExplorationManagerPlainHDT(hdt);

        rdfTypes = HDTUtil.getRDFTypesAsArray(hdt);

    }


    @Override
    public ArrayList<String> executeQuery(QueryModel q) {

        ArrayList<String> results = new ArrayList<String>();

        hsSubject = executeQueryAndReturnHashSet(q.types, q.catConnections, q.predicates).hsSubject;

        if (q.onlyCount) {
            results.add("" + hsSubject.size());
        } else {
            Iterator it = hsSubject.iterator();
            while (it.hasNext()) {
                results.add(dic.idToString((long)it.next(), TripleComponentRole.SUBJECT).toString());
            }
        }

        return results;
    }

    private void executeQueryForInitialType(URI type) {
        TripleID tID = HDTUtil.getTripleIDFromStrings(dic, null, RDFUtilities.rdfType, type.toString());
        itID = HDTUtil.executeQuery(hdt, tID);
        hsSubject = HDTUtil.generateHashSetForIterator(itID, TripleComponentRole.SUBJECT);
        hsObject = HDTUtil.getSharedIDsFromHashSet(dic, hsSubject);
    }

    public CenterHashSets executeQueryAndReturnHashSet(QueryModel qm) {
        return executeQueryAndReturnHashSet(qm.types, qm.catConnections, qm.predicates);
    }

    public CenterHashSets executeQueryAndReturnHashSet(ArrayList<URI> types, ArrayList<CatConnection> catConnections, ArrayList<URI> predicates) {
        // initialize hash sets for joins
        executeQueryForInitialType(types.get(0));

        for (int i = 1; i < types.size(); i++) {

            // get hash set for next type
            TripleID tID = HDTUtil.getTripleIDFromStrings(dic, null, RDFUtilities.rdfType, types.get(i).toString());
            itID = HDTUtil.executeQuery(hdt, tID);
            hsForJoin = HDTUtil.generateHashSetForIterator(itID, TripleComponentRole.SUBJECT);

            long predID = 0;
            if (predicates != null) {

                predID = dic.stringToId(predicates.get(i - 1).toString(), TripleComponentRole.PREDICATE);
            }

            if (catConnections.get(i - 1).equals(CatConnection.IN)) {

                hsSubject = performJoin(hsForJoin, predID, hsObject, TripleComponentRole.SUBJECT);
                hsObject = HDTUtil.getSharedIDsFromHashSet(dic, hsSubject);
            } else {

                hsObject = performJoin(hsSubject, predID, hsForJoin, TripleComponentRole.OBJECT);
                hsSubject = HDTUtil.getSharedIDsFromHashSet(dic, hsObject);
            }
        }

        return new CenterHashSets(hsSubject, hsObject);
    }

    /**
     * Performs a join and returns the corresponding IDs of the ressources which have been defined in the "toStore"
     * TripleComponentRole object.
     * @param hsSubject The set which contains all subject IDs.
     * @param predicate The predicate which is used as connection for the join.
     * @param hsObject The set which contains all object IDs.
     * @param toStore Indicates which triple component should be stored in the hash set.
     * @return
     */
    private UnifiedSet<Long> performJoin(UnifiedSet<Long> hsSubject, long predicate, UnifiedSet<Long> hsObject, TripleComponentRole toStore) {
        UnifiedSet<Long> result = new UnifiedSet<Long>();

        Iterator itSubjects = hsSubject.iterator();
        Triples triples = hdt.getTriples();

        TripleID tID = new TripleID();
        while (itSubjects.hasNext()) {
            long sbjID = (long) itSubjects.next();

            // get all objects for the current subject
            tID.setAll(sbjID, predicate, 0);
            itID = triples.search(tID);
            while (itID.hasNext()) {
                long objID = itID.next().getObject();

                // if object is present in the hash set, add the specified triple component to the result
                if (hsObject.contains(objID)) {
                    if (toStore.equals(TripleComponentRole.OBJECT)) {
                        result.add(objID);
                    } else {
                        result.add(sbjID);
                    }
                }
            }
        }

        return result;
    }

    public ArrayList<TripleID> performJoin(UnifiedSet<Long> hsSubject, long predicate, UnifiedSet<Long> hsObject) {
        ArrayList<TripleID> result = new ArrayList<TripleID>();

        Iterator itSubjects = hsSubject.iterator();
        Triples triples = hdt.getTriples();

        TripleID tID = new TripleID();
        while (itSubjects.hasNext()) {
            long sbjID = (long) itSubjects.next();

            // get all objects for the current subject
            tID.setAll(sbjID, predicate, 0);
            itID = triples.search(tID);
            while (itID.hasNext()) {
                TripleID toAdd = itID.next();

                // if object is present in the hash set, add the specified triple component to the result
                if (hsObject.contains(toAdd.getObject())) {

                    result.add(toAdd);
                }
            }
        }

        return result;
    }

    @Override
    public ReachableCategories calcReachableCategories(UnifiedSet<Long> hsSubject, UnifiedSet<Long> hsObject, URI centerType) {

        ReachableCategories rc = new ReachableCategories();


        // for each rdf type
        for (int i = 0; i < rdfTypes.length; i++) {

            long typeID = dic.stringToId(rdfTypes[i].toString(), TripleComponentRole.OBJECT);
            URI outsiderType = null;
            try {
                String uriStr = dic.idToString(typeID, TripleComponentRole.OBJECT).toString();
                outsiderType = new URI(uriStr);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
            SingleJoinModel outCon = getReachableCategories(hsSubject, centerType, outsiderType, CatConnection.OUT);
            SingleJoinModel inCon = getReachableCategories(hsObject, centerType, outsiderType, CatConnection.IN);

            if (outCon != null) {
                rc.outgoingCategories.add(outCon);
            }
            if (inCon != null) {
                rc.incomingCategories.add(inCon);
            }

        }

        return rc;
    }

    public SingleJoinModel getReachableCategories(UnifiedSet<Long> hs, URI centerURI, URI outsiderURI, CatConnection con) {
        long typeID = dic.stringToId(outsiderURI.toString(), TripleComponentRole.OBJECT);

        // get all resources which are from the current type
        IteratorTripleID outsiderCatIt = HDTUtil.executeQuery(hdt, new TripleID(0, rdfTypePred, typeID));

        UnifiedSet<Long> idsOutsiderHs = HDTUtil.generateHashSetForIterator(outsiderCatIt, TripleComponentRole.SUBJECT);

        if (con.equals(CatConnection.OUT)) {
            // outsider category acts as object -> get only shared IDs
            idsOutsiderHs = HDTUtil.getSharedIDsFromHashSet(dic, idsOutsiderHs);
        }
        Iterator<Long> idsOutsiderIt = idsOutsiderHs.iterator();

        SingleJoinModel connection = new SingleJoinModel();
        connection.setCenter(centerURI);
        connection.setOutsider(outsiderURI);

        if (con.equals(CatConnection.IN)) {
            connection.setConnection(CatConnection.IN);
        } else {
            connection.setConnection(CatConnection.OUT);
        }

        long countResults = 0;

        Iterator<Long> centerIt = hs.iterator();
        IteratorTripleID results = null;

        while (centerIt.hasNext()) {

            long centerID = centerIt.next();

            while (idsOutsiderIt.hasNext()) {

                long outsiderID = idsOutsiderIt.next();

                if (con.equals(CatConnection.IN)) {

                    results = HDTUtil.executeQuery(hdt, new TripleID(outsiderID, 0, centerID));
                } else {
                    results = HDTUtil.executeQuery(hdt, new TripleID(centerID, 0, outsiderID));
                }
                while (results.hasNext()) {
                    results.next();
                    countResults++;
                }
            }
            idsOutsiderIt = idsOutsiderHs.iterator();
        }

        if (countResults > 0) {
            SingleJoinModel conToAdd = connection.clone();
            conToAdd.setResults(countResults);
            return conToAdd;
        }

        return null;
    }

    @Override
    public ConnectedPredicates[] calcReachableCategoriesPreds(UnifiedSet<Long> hsSubject, UnifiedSet<Long> hsObject, URI centerType) {

        return getConnectedPreds(hsSubject, centerType);
    }


    private ConnectedPredicates[] getConnectedPreds(UnifiedSet<Long> hs, URI centerURI) {

        // hash maps which store predicate id as key and the corresponding count as value for each rdf type
        HashMap<Long, HashMap<Long, Long>> predCountOutForEachType = new HashMap<>();
        HashMap<Long, HashMap<Long, Long>> predCountInForEachType = new HashMap<>();
        for (int i = 0; i < rdfTypes.length; i++) {
            long typeID = dic.stringToId(rdfTypes[i].toString(), TripleComponentRole.OBJECT);
            predCountOutForEachType.put(typeID, new HashMap<>());
            predCountInForEachType.put(typeID, new HashMap<>());
        }

        Iterator<Long> centerIt = hs.iterator();
        TripleID tID = new TripleID(0,0,0);
        TripleID rdfTypeSearch = new TripleID(0, rdfTypePred, 0);
        IteratorTripleID itID;
        IteratorTripleID typeIterator;
        long resourceToGetTypeFrom;
        long centerResourceID;

        for (long pred = 1; pred <= dic.getNpredicates(); pred++) {

            tID.setPredicate(pred);
            while (centerIt.hasNext()) {

                // outgoing predicate connections
                centerResourceID = centerIt.next();
                tID.setSubject(centerResourceID);
                tID.setObject(0);
                itID = HDTUtil.executeQuery(hdt, tID);

                // examine for each resulting resource its rdf type
                while (itID.hasNext()) {
                    resourceToGetTypeFrom = itID.next().getObject();
                    if (resourceToGetTypeFrom > dic.getNshared()) {
                        // resource is not a subject -> discard
                        continue;
                    }
                    rdfTypeSearch.setSubject(resourceToGetTypeFrom);
                    typeIterator = HDTUtil.executeQuery(hdt, rdfTypeSearch);
                    while (typeIterator.hasNext()) {
                        HashMap<Long, Long> predCount = predCountOutForEachType.get(typeIterator.next().getObject());
                        if (predCount == null) {
                            // type is not relevant
                            continue;
                        }
                        long val = predCount.getOrDefault(pred, 0L);
                        val++;
                        predCount.put(pred, val);
                    }
                }

                if (centerResourceID > dic.getNshared()) {
                    // resource is a pure subject -> cannot be considered as object
                    continue;
                }
                // incoming predicate connections
                tID.setSubject(0);
                tID.setObject(centerResourceID);
                itID = HDTUtil.executeQuery(hdt, tID);

                // examine for each resulting resource its rdf type
                while (itID.hasNext()) {
                    resourceToGetTypeFrom = itID.next().getSubject();
                    rdfTypeSearch.setSubject(resourceToGetTypeFrom);
                    typeIterator = HDTUtil.executeQuery(hdt, rdfTypeSearch);
                    while (typeIterator.hasNext()) {
                        HashMap<Long, Long> predCount = predCountInForEachType.get(typeIterator.next().getObject());
                        if (predCount == null) {
                            // type is not relevant
                            continue;
                        }
                        long val = predCount.getOrDefault(pred, 0L);
                        val++;
                        predCount.put(pred, val);
                    }
                }
            }
            centerIt = hs.iterator();
        }

        ConnectedPredicates[] connectedPreds = new ConnectedPredicates[rdfTypes.length];


        for (int i = 0; i < rdfTypes.length; i++) {
            connectedPreds[i] = new ConnectedPredicates();
            connectedPreds[i].incomingPreds = new ArrayList<>();
            connectedPreds[i].outgoingPreds = new ArrayList<>();

            URI rdfType = rdfTypes[i];
            long rdfTypeID = dic.stringToId(rdfTypes[i].toString(), TripleComponentRole.OBJECT);

            SingleJoinModel preparedCon = new SingleJoinModel();
            preparedCon.setCenter(centerURI);
            preparedCon.setOutsider(rdfType);

            // get outgoing connections
            HashMap<Long, Long> preds = predCountOutForEachType.get(rdfTypeID);
            int finalI = i;
            preds.forEach((pred, count) -> {
                SingleJoinModel con = preparedCon.clone();
                con.setConnection(CatConnection.OUT);
                con.setResults(count);
                con.setPredicate(URI.create(dic.idToString(pred, TripleComponentRole.PREDICATE).toString()));
                connectedPreds[finalI].outgoingPreds.add(con);
            });

            // get incoming connections
            preds = predCountInForEachType.get(rdfTypeID);
            preds.forEach((pred, count) -> {
                SingleJoinModel con = preparedCon.clone();
                con.setConnection(CatConnection.IN);
                con.setResults(count);
                con.setPredicate(URI.create(dic.idToString(pred, TripleComponentRole.PREDICATE).toString()));
                connectedPreds[finalI].incomingPreds.add(con);
            });
        }

        return connectedPreds;
    }


    @Override
    public CenterHashSets getHashSetFromQueryModel(QueryModel qm) {
        return executeQueryAndReturnHashSet(qm.types, qm.catConnections, qm.predicates);
    }


    @Override
    public void updateFSManagerState(QueryModel qm) {
        fsManager.updateFSManagerState(qm);
    }

    @Override
    public AvailableFacets calcAvailableFacets() {
        fsManager.updateAvailableFacets();
        return fsManager.availableFacets;
    }

    @Override
    public ArrayList<String> applyFacets(QueryModel qm) {
        return fsManager.applyFacetsAndInitToRDFType(qm);
    }

    @Override
    public ArrayList<String> executeHybridQuery(QueryModel qm) {
        UnifiedSet<Long> center = executeQueryAndReturnHashSet(qm).hsSubject;
        fsManager.currentType = qm.types.get(qm.types.size() - 1);
        fsManager.center = center;
        return fsManager.applyFacets(qm);
    }
}
