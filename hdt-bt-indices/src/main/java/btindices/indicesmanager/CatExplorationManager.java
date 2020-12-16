package btindices.indicesmanager;

import btindexmodels.categoryexplorationmodels.CatConnection;
import btindexmodels.categoryexplorationmodels.ConnectedPredicates;
import btindexmodels.categoryexplorationmodels.ReachableCategories;
import btindexmodels.categoryexplorationmodels.SingleJoinModel;
import btindices.HDTUtil;
import btindices.RDFUtilities;
import btindices.statisticalquerygeneration.QueryModel;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.rdfhdt.hdt.dictionary.Dictionary;
import org.rdfhdt.hdt.enums.TripleComponentRole;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.triples.IteratorTripleID;
import org.rdfhdt.hdt.triples.IteratorTripleString;
import org.rdfhdt.hdt.triples.TripleID;
import org.rdfhdt.hdt.triples.TripleString;
import org.rdfhdt.hdt.util.StopWatch;

import java.net.URI;
import java.util.ArrayList;

/**
 * Represents a class which contains all necessary tools in order to explore an HDT file using the corresponding HDT BT
 * Indices.
 *
 * @author Maximilian Wenzel
 */
public class CatExplorationManager {
    public static final StopWatch sw = new StopWatch();

    public HDT hdt;
    public Dictionary dic;
    public CtCIndicesManager btIndexManager;

    public URI currentURI;
    public URI subjectCategory;
    public URI objectCategory;

    /**
     * All resources of the current center containing the resources as subject IDs.
     */
    public UnifiedSet<Long> hsSubject;

    /**
     * All resources of the current center containing the resources as object IDs.
     */
    public UnifiedSet<Long> hsObject;
    //public UnifiedSet<String> hsString;

    public ReachableCategories rc;
    public ConnectedPredicates[] connectedPreds;
    public IteratorTripleID currentConnectionItID;

    private TripleComponentRole subsetOf;

    public CatExplorationManager(String hdtPath, String ctcIndicesPath) {
        loadFiles(hdtPath, ctcIndicesPath);
    }

    public void loadFiles(String hdtPath, String btIndicesPath) {
        sw.reset();
        System.out.println("Loading HDT file ... ");
        hdt = HDTUtil.loadHDTFile(hdtPath);
        System.out.println("Time needed to load HDT file: " + sw.stopAndShow());
        sw.reset();
        dic = hdt.getDictionary();

        System.out.println("Loading BTIndices ... ");
        btIndexManager = new CtCIndicesManagerImpl(btIndicesPath, hdt, true);
        System.out.println("Time needed to load BTIndices: " + sw.stopAndShow());

        this.connectedPreds = new ConnectedPredicates[btIndexManager.typeURIs.length];
    }

    /**
     * Sets the starting point of the exploration to a set of resources which are of the corresponding type that has
     * been specified by the user.
     *
     * @param chosenIndex Relates to the "classURIs" array of the BTIndexManager object.
     */
    public void chooseInitialClass(int chosenIndex) {
        hsSubject = null;
        hsObject = null;

        // intial class
        currentURI = btIndexManager.typeURIs[chosenIndex];
        subjectCategory = currentURI;

        TripleID tID = HDTUtil.getTripleIDFromStrings(dic, null, RDFUtilities.rdfType, currentURI.toString());
        currentConnectionItID = HDTUtil.executeQuery(hdt, tID);

		/*
		// calculate reachable categories
		rc = btIndexManager.getReachableCategories(currentURI);
		*/

        // initialize "subsetOf" variable with subject, because the "rdfType"-query contains all
        // relevant resources at the subject component
        subsetOf = TripleComponentRole.SUBJECT;
    }

    /**
     * Chooses the incoming connection defined by the ReachableCategories object and updates the current center resource
     * set correspondingly. If the filter variable is set to true, the current center resources are only filtered, i.e.,
     * all resources which do not have the appropriate connection are kicked out.
     *
     * @param index Relates to the "incomingCategories" array list of the ReachableCategories object.
     */
    public void chooseIncomingReachableCategory(int index, boolean filter) {

        chooseIncomingConnection(rc.incomingCategories.get(index).getOutsider(), null, filter);
    }


    /**
     * Chooses the outgoing connection defined by the ReachableCategories object and updates the current center resource
     * set correspondingly. If the filter variable is set to true, the current center resources are only filtered, i.e.,
     * all resources which do not have the appropriate connection are kicked out.
     *
     * @param index Relates to the "outgoingCategories" array list of the ReachableCategories object.
     */
    public void chooseOutgoingReachableCategory(int index, boolean filter) {

        chooseOutgoingConnection(rc.outgoingCategories.get(index).getOutsider(), null, filter);
    }

    /**
     * Chooses the outgoing connection defined by the ConnectedPredicates object and updates the current center resource
     * set correspondingly. If the filter variable is set to true, the current center resources are only filtered, i.e.,
     * all resources which do not have the appropriate connection are kicked out.
     */
    public void chooseOutgoingPredicateConnection(int indexCat, int indexPred, boolean filter) {
        SingleJoinModel con = connectedPreds[indexCat].outgoingPreds.get(indexPred);
        URI outsiderCat = con.getOutsider();
        URI predicate = con.getPredicate();
        chooseOutgoingConnection(outsiderCat, predicate.toString(), filter);
    }

    /**
     * Chooses the incoming connection defined by the ConnectedPredicates object and updates the current center resource
     * set correspondingly. If the filter variable is set to true, the current center resources are only filtered, i.e.,
     * all resources which do not have the appropriate connection are kicked out.
     */
    public void chooseIncomingPredicateConnection(int indexCat, int indexPred, boolean filter) {
        SingleJoinModel con = connectedPreds[indexCat].incomingPreds.get(indexPred);
        URI outsiderCat = con.getOutsider();
        URI predicate = con.getPredicate();
        chooseIncomingConnection(outsiderCat, predicate.toString(), filter);
    }

    /**
     * Chooses the outgoing connection defined by type URI and updates the current center resource set correspondingly.
     * If the filter variable is set to true, the current center resources are only filtered, i.e., all resources which
     * do not have the appropriate connections are kicked out.
     */
    public void chooseOutgoingConnection(URI outgoingType, String predicate, boolean filter) {

        // hash set resources act as subjects
        subsetOf = TripleComponentRole.SUBJECT;

        subjectCategory = currentURI;
        objectCategory = outgoingType;

        if (!filter) {
            // take 'outgoing connection' from currentURI -> currentURI is now outsiderClass
            currentURI = objectCategory;
        }

        performJoin(predicate, filter);
    }

    public void chooseOutgoingConnection(URI outgoingType, String predicate) {
        chooseOutgoingConnection(outgoingType, predicate, false);
    }

    public void chooseIncomingConnection(URI incomingType, String predicate) {
        chooseIncomingConnection(incomingType, predicate, false);
    }

    /**
     * Chooses the incoming connection defined by type URI and updates the current center resource set correspondingly.
     */
    public void chooseIncomingConnection(URI incomingType, String predicate, boolean filter) {

        // hash set resources act as objects
        subsetOf = TripleComponentRole.OBJECT;

        subjectCategory = incomingType;
        objectCategory = currentURI;

        if (!filter) {
            // take 'incoming connection' -> currentURI is now centerClass
            currentURI = subjectCategory;
        }

        performJoin(predicate, filter);
    }

    /**
     * After the center and the chosen outsider category have been updated a join is performed in order to intialize the
     * new center with a subset of the resource set of the outsider category. The predicate "pred" can be set in order
     * to specify the predicate connection to the outsider category.
     */
    public void performJoin(String pred, boolean filter) {
        long predID = 0;
        if (pred != null) {
            predID = dic.stringToId(pred, TripleComponentRole.PREDICATE);
        }

        if (hsSubject == null) {
            // if it is the first query
            currentConnectionItID = btIndexManager.executeQuery(subjectCategory, objectCategory, 0, predID, 0);
        } else {

            UnifiedSet<Long> hs = null;

            if (subsetOf.equals(TripleComponentRole.OBJECT)) {
                hs = hsObject;
            } else {
                hs = hsSubject;
            }

            currentConnectionItID = btIndexManager.executeSubsetQuery(hs, subsetOf, subjectCategory, objectCategory, 0, predID,
                    0);
        }

        // flip subsetOf in order to get subset of new category only if it is not a filter query
        if (!filter) {
            if (subsetOf == TripleComponentRole.SUBJECT) {
                subsetOf = TripleComponentRole.OBJECT;
            } else {
                subsetOf = TripleComponentRole.SUBJECT;
            }
        }
        updateHashSets();
    }

    /**
     * Updates both ID hash sets regarding the resources of the new center.
     */
    private void updateHashSets() {
        if (subsetOf.equals(TripleComponentRole.SUBJECT)) {

            hsSubject = HDTUtil.generateHashSetForIterator(currentConnectionItID, TripleComponentRole.SUBJECT);
            hsObject = HDTUtil.getSharedIDsFromHashSet(dic, hsSubject);
        } else {
            hsObject = HDTUtil.generateHashSetForIterator(currentConnectionItID, TripleComponentRole.OBJECT);
            hsSubject = HDTUtil.getSharedIDsFromHashSet(dic, hsObject);
        }
    }

    /**
     * Updates the reachable categories for the current center resource set.
     */
    public void updateReachableCategories() {

        if (hsSubject != null) {
            rc = btIndexManager.getReachableCategories(hsSubject, hsObject, currentURI);

        } else {
            rc = btIndexManager.getReachableCategories(currentURI);
        }

    }

    /**
     * Updates the predicate connections in relation to the current center and outsider category.
     */
    public void updatePredicateConnections() {

        URI centerType = URI.create(currentURI.toString());
        if (hsSubject == null) {

            for (int i = 0; i < this.btIndexManager.typeURIs.length; i++) {
                URI outsiderType = URI.create(btIndexManager.typeURIs[i].toString());
                connectedPreds[i] = btIndexManager.getConnectedPredicates(centerType, outsiderType);
            }

        } else {

            for (int i = 0; i < this.btIndexManager.typeURIs.length; i++) {
                URI outsiderType = URI.create(btIndexManager.typeURIs[i].toString());
                connectedPreds[i] = btIndexManager.getConnectedPredicates(hsSubject, hsObject, centerType, outsiderType);
            }
        }

    }

    /**
     * Filters the initial center resource set by the given conditions. E.g. if resourceRole == Subject ->
     * (filterResource, filterPred, x) with x being in the current UnifiedSet object. else -> (x, filterPred,
     * filterResource)
     */
    public void facetedSearch(String filterPred, String filterResource, TripleComponentRole resourceRole) {
        String resource;
        TripleString tripleStr = new TripleString();
        tripleStr.setPredicate(filterPred);

        // the position which is not occupied in the resulting Triple ID
        TripleComponentRole wildcardPosition;

        if (resourceRole.equals(TripleComponentRole.OBJECT)) {

            resource = filterResource;
            tripleStr.setObject(resource);
            tripleStr.setSubject(null);
            wildcardPosition = TripleComponentRole.SUBJECT;

        } else {
            resource = filterResource;
            tripleStr.setSubject(resource);

            tripleStr.setObject(null);
            wildcardPosition = TripleComponentRole.OBJECT;
        }

        if (hsSubject == null) {
            updateHashSets();
        }


        if (wildcardPosition.equals(TripleComponentRole.OBJECT)) {
            // filter only hsObject
            hsObject = filterResults(dic, hsObject, tripleStr, wildcardPosition);

            // get subset of objects for hash set
            hsSubject = HDTUtil.getSharedIDsFromHashSet(dic, hsObject);

        } else {
            // filter only hsSubject
            hsSubject = filterResults(dic, hsSubject, tripleStr, wildcardPosition);

            // get subset of subjects for hash set
            hsObject = HDTUtil.getSharedIDsFromHashSet(dic, hsSubject);

        }

    }

    public void updateExplorationState(QueryModel qm) {
        ArrayList<URI> predicates = qm.predicates;
        ArrayList<CatConnection> catConnections = qm.catConnections;
        ArrayList<URI> types = qm.types;

        int pos = btIndexManager.getTypePositionForURI(types.get(0));
        chooseInitialClass(pos);

        if (predicates == null || predicates.size() == 0) {

            for (int i = 1; i < types.size(); i++) {

                if (catConnections.get(i - 1).equals(CatConnection.OUT)) {

                    chooseOutgoingConnection(types.get(i), null);
                } else {

                    chooseIncomingConnection(types.get(i), null);
                }
            }
        } else {

            for (int i = 1; i < types.size(); i++) {
                String predicate = predicates.get(i - 1) == null ? null : predicates.get(i - 1).toString();

                if (catConnections.get(i - 1).equals(CatConnection.OUT)) {

                    chooseOutgoingConnection(types.get(i), predicate);
                } else {

                    chooseIncomingConnection(types.get(i), predicate);
                }
            }
        }
    }

    /**
     * Filters a UnifiedSet object by the given conditions and returns the resulting subset. The TripleComponentRole
     * object indicates which triple component of the query results should be compared to the hashSet resources. E.g.
     * "tripleID = (0, 5, 42)" and "wildcardPos = Subject" indicates that the subject position is compared to the
     * hashSet resources. All resources at the "wildcardPos" triple component of the query results which are also
     * present in the hash set are adopted to the resulting UnfiedSet object.
     *
     * @return The subset of the given UnifiedSet object that were present in the query results at the "wildcardPos"
     * triple component.
     */
    private UnifiedSet<String> filterResults(UnifiedSet<String> hashSet, TripleString tripleString,
                                             TripleComponentRole wildcardPos) {

        UnifiedSet<String> newHashSet = new UnifiedSet<String>();
        IteratorTripleString queryResults;

        String resource;

        String s = tripleString.getSubject() != null ? tripleString.getSubject().toString() : null;
        String p = tripleString.getPredicate() != null ? tripleString.getPredicate().toString() : null;
        String o = tripleString.getObject() != null ? tripleString.getObject().toString() : null;

        queryResults = HDTUtil.executeQuery(hdt, s, p, o);

        while (queryResults.hasNext()) {

            if (wildcardPos.equals(TripleComponentRole.SUBJECT)) {
                resource = queryResults.next().getSubject().toString();

                if (hashSet.contains(resource)) {
                    newHashSet.add(resource);
                }

            } else {
                // object position acts as wildcard, i.e. it is replaced by the resources in the
                // hash set
                resource = queryResults.next().getObject().toString();

                if (hashSet.contains(resource)) {
                    newHashSet.add(resource);
                }
            }
        }

        return newHashSet;
    }

    /**
     * Prints all connected predicate connections in relation to the current center and all outsider categories to the
     * standard output.
     */
    public void printConnectedPredicates() {
        for (int i = 0; i < connectedPreds.length; i++) {
            if (connectedPreds[i].incomingPreds.size() > 0
                    || connectedPreds[i].outgoingPreds.size() > 0) {
                System.out.println("[" + i + "]");
                connectedPreds[i].printReachableCategories();
                System.out.println();
            }
        }
    }

    /**
     * Filters a UnifiedSet object by the given conditions and returns the resulting subset. The TripleComponentRole
     * object indicates which triple component of the query results should be compared to the hashSet resources. E.g.
     * "tripleID = (0, 5, 42)" and "wildcardPos = Subject" indicates that the subject position is compared to the
     * hashSet resources. All resources at the "wildcardPos" triple component of the query results which are also
     * present in the hash set are adopted to the resulting UnfiedSet object.
     *
     * @return The subset of the given UnifiedSet object that were present in the query results at the "wildcardPos"
     * triple component.
     */
    private UnifiedSet<Long> filterResults(Dictionary dic, UnifiedSet<Long> hs, TripleString tripleStr,
                                           TripleComponentRole wildcardPos) {

        IteratorTripleID queryResults;
        UnifiedSet<Long> newHashSet = new UnifiedSet<Long>();

        long resourceID = 0;


        // get all triples which conform the filter condition
        TripleID tID = HDTUtil.convertTripleStrToTripleID(dic, tripleStr);
        queryResults = hdt.getTriples().search(tID);


        while (queryResults.hasNext()) {

            TripleID currentTripleID = queryResults.next();

            if (wildcardPos.equals(TripleComponentRole.SUBJECT)) {
                resourceID = currentTripleID.getSubject();

            } else {
                // object position acts as wildcard, i.e. it is replaced by the resources in the
                // hash set
                resourceID = currentTripleID.getObject();

            }

            // add resource to new filtered sets
            if (hs.contains(resourceID)) {
                newHashSet.add(resourceID);
            }

        }

        return newHashSet;

    }

    /**
     * Filters all Subject-IDs of an IteratorTripleID object by the given conditions and returns the resulting subset as
     * a UnifiedSet object. The TripleComponentRole object indicates which triple component of the query results should
     * be compared to the Iterator resources. E.g. "tripleID = (0, 5, 42)" and "wildcardPos = Subject" indicates that
     * the subject component is compared to the subject Iterator resources. All resources at the "wildcardPos" triple
     * component of the query results which are also present in the hash set are adopted to the resulting UnfiedSet
     * object.
     *
     * @return The subset of subject-IDs of the passed IteratorTripleID that were present in the query results at the
     * "wildcardPos" triple component.
     */
    private UnifiedSet<String> filterResults(IteratorTripleString itString, TripleString tripleStr,
                                             TripleComponentRole wildcardPos) {

        IteratorTripleString queryResults;

        String resource;

        UnifiedSet<String> hsCenter = HDTUtil.generateHashSetForIterator(itString, TripleComponentRole.SUBJECT);
        queryResults = HDTUtil.executeQuery(this.hdt, tripleStr);

        // resulting hash set
        UnifiedSet<String> newHashSet = new UnifiedSet<String>(hsCenter.size());

        while (queryResults.hasNext()) {

            if (wildcardPos.equals(TripleComponentRole.SUBJECT)) {
                resource = queryResults.next().getSubject().toString();

                if (hsCenter.contains(resource)) {
                    newHashSet.add(resource);
                }

            } else {
                // object position acts as wildcard, i.e. it is replaced by the resources in the
                // hash set
                resource = queryResults.next().getObject().toString();

                if (hsCenter.contains(resource)) {
                    newHashSet.add(resource);
                }
            }
        }

        itString.goToStart();

        return newHashSet;
    }

    /**
     * Calculates how much resources of each "classURIs" type are present in the current center resource set in relation
     * to the entire number of resources of the corresponding type.
     */
    public void printDossier() {

        TripleID tID = new TripleID();
        tID.setSubject(0);

        // generate hash set from iterator if it is null
        if (hsSubject == null) {
            // get all subjects, because initial query contains relevant resources at subject position
            hsSubject = HDTUtil.generateHashSetForIterator(currentConnectionItID, TripleComponentRole.SUBJECT);

            // get subset of resources which also act as an object in the dataset
            hsObject = HDTUtil.getSharedIDsFromHashSet(dic, hsSubject);
        }

        URI[] classURIs = btIndexManager.typeURIs;

        long rdfTypeID = dic.stringToId(RDFUtilities.rdfType, TripleComponentRole.PREDICATE);
        tID.setPredicate(rdfTypeID);

        long typeID;

        for (int i = 0; i < classURIs.length; i++) {

            typeID = dic.stringToId(classURIs[i].toString(), TripleComponentRole.OBJECT);
            tID.setObject(typeID);
            IteratorTripleID typeItID = HDTUtil.executeQuery(hdt, tID);

            int resultsInCommon = 0;

            while (typeItID.hasNext()) {
                if (hsSubject.contains(typeItID.next().getSubject())) {
                    resultsInCommon++;
                }
            }

            typeItID.goToStart();

            System.out.println("Class: " + classURIs[i] + ", Triples: " + resultsInCommon + " of "
                    + typeItID.estimatedNumResults());
            double percentage = (double) resultsInCommon / (double) typeItID.estimatedNumResults();
            System.out.println("Percentage: " + percentage);
        }
    }


    public void changeExplorationState(UnifiedSet<Long> hsSubject, UnifiedSet<Long> hsObject, URI centerURI) {

        this.hsSubject = hsSubject;
        this.hsObject = hsObject;
        this.subjectCategory = centerURI;
        this.currentURI = centerURI;
    }

}
