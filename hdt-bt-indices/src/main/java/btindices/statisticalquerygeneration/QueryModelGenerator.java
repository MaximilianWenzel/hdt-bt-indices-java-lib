package btindices.statisticalquerygeneration;

import btindexmodels.categoryexplorationmodels.CatConnection;
import btindexmodels.categoryexplorationmodels.ConnectedPredicates;
import btindexmodels.categoryexplorationmodels.SingleJoinModel;
import btindexmodels.LabeledResults;
import btindices.indicesmanager.CatExplorationManager;
import btindices.HDTUtil;
import btindices.RDFUtilities;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

/**
 * Extracts a set of query models from a given data set.
 */
public class QueryModelGenerator {

    CatExplorationManager em;
    String outputFile;
    boolean predicates;
    boolean onlyCount;
    private long lowerBound;
    private long upperBound;
    long maxJoinElement;


    URI[] types;
    private double quantile;

    private ArrayList<URI> typesForThres;
    private ArrayList<LabeledResults> typesSortedByResults;
    private URI[] initialTypes;
    public int numOfQueriesToExtract;

    private UnifiedSet<URI> typesToAvoid;
    private boolean duplicatePredsOrTypes;


    public QueryModelGenerator(CatExplorationManager em, boolean predicates, boolean onlyCount, int numOfQueriesToExtract) {

        this.em = em;
        this.types = em.btIndexManager.typeURIs;

        this.predicates = predicates;
        this.onlyCount = onlyCount;
        outputFile = "qmg_queries";

        this.lowerBound = 0;
        this.upperBound = Long.MAX_VALUE;
        this.typesForThres = new ArrayList<URI>();
        this.numOfQueriesToExtract = numOfQueriesToExtract;
        duplicatePredsOrTypes = true;
        typesSortedByResults = HDTUtil.getAllTypesSortedByResults(types, em.hdt);

        this.maxJoinElement = 0;
    }

    public ArrayList<QueryModel> generateJoinQueries(int maxJoins, double percentile, long lowerBound, long upperBound) {

        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        maxJoinElement = 0;
        duplicatePredsOrTypes = true;

        this.typesForThres = new ArrayList<URI>();


        ArrayList<LabeledResults> ls = new ArrayList<>();

        for (int i = 0; i < typesSortedByResults.size(); i++) {
            long results = typesSortedByResults.get(i).results;
            if (results <= this.upperBound && results > this.lowerBound) {
                ls.add(typesSortedByResults.get(i));
            }
        }
        Collections.sort(ls);

        // get top 10 types as initial categories
        initialTypes = new URI[Math.min(ls.size(), 10)];
        for (int i = 0; i < initialTypes.length; i++) {
            initialTypes[i] = URI.create(ls.get(ls.size() - (i + 1)).label);
        }

        for (int i = 0; i < ls.size(); i++) {
            typesForThres.add(URI.create(ls.get(i).label));
        }

        return generateJoinQueries(maxJoins, percentile, lowerBound, upperBound, initialTypes);
    }


    public ArrayList<QueryModel> generateJoinQueries(int maxJoins, double percentile, long lowerBound, long upperBound, URI[] initialTypes) {

        /*
        typesToAvoid = new UnifiedSet<>();
        typesToAvoid.add(URI.create(HDTUtil.rdfsClass));
        typesToAvoid.add(URI.create(HDTUtil.owlThing));
        typesToAvoid.add(URI.create(HDTUtil.rdfsResource));
        */
        numOfQueriesToExtract = Integer.MAX_VALUE;
        maxJoinElement = 0;
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        this.quantile = percentile;

        ArrayList<QueryModel> result = new ArrayList<QueryModel>();

        System.out.println("Generate Join Queries: #Joins = " + maxJoins + ", percentile = " + percentile + ", predicates = " + predicates);

        // get join queries for each type
        for (int i = 0; i < initialTypes.length; i++) {

            System.out.println("Current type: (" + (i+1) + "/" + initialTypes.length + ")");
            int remainingJoins = maxJoins;

            // reset ExplorationManager and choose initial type
            int typeIndex = em.btIndexManager.getTypePositionForURI(initialTypes[i]);
            em.chooseInitialClass(typeIndex);

            // generate initial query model for type
            ArrayList<URI> queryTypes = new ArrayList<URI>();
            queryTypes.add(initialTypes[i]);

            ArrayList<CatConnection> catConnections = new ArrayList<CatConnection>();

            if (!this.predicates) {
                QueryModel q = new QueryModel(queryTypes, null, catConnections, this.onlyCount);
                result.addAll(performJoins(remainingJoins, q));

            } else {
                ArrayList<URI> predicates = new ArrayList<URI>();
                QueryModel q = new QueryModel(queryTypes, predicates, catConnections, this.onlyCount);
                result.addAll(this.performJoinsForGivenPredicates(remainingJoins, q));
            }
        }

        // extract specified amount of queries (randomly)
        ArrayList<Integer> indexArray = new ArrayList<Integer>();
        for (int i = 0; i < numOfQueriesToExtract && i < result.size(); i++) {
            indexArray.add(i);
        }
        Collections.shuffle(indexArray, new Random(42));

        ArrayList<QueryModel> extracted = new ArrayList<QueryModel>();

        for (int i = 0; i < indexArray.size() && i < result.size(); i++) {
            extracted.add(result.get(indexArray.get(i)));
        }
        return extracted;
    }

    private ArrayList<QueryModel> performJoins(int remainingJoins, QueryModel q) {
        ArrayList<QueryModel> result = new ArrayList<QueryModel>();

        if (remainingJoins == 0) {
            result.add(q);
            return result;
        }

        UnifiedSet<Long> hsSubjectState = null;
        UnifiedSet<Long> hsObjectState = null;

        if (em.hsSubject != null) {

            hsSubjectState = em.hsSubject.clone();
            hsObjectState = em.hsObject.clone();
        }

        URI centerURIState = null;
        try {
            centerURIState = new URI(em.currentURI.toString());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        em.updateReachableCategories();

        ArrayList<SingleJoinModel> catIn = em.rc.incomingCategories;
        filterGreaterThanThreshold(catIn);

        ArrayList<SingleJoinModel> catOut = em.rc.outgoingCategories;
        filterGreaterThanThreshold(catOut);

        if (catIn.size() == 0 && catOut.size() == 0) {
            // no join is possible
            return new ArrayList<QueryModel>();
        }

        if (catIn.size() > 0) {

            // reset exploration manager to the current state
            em.changeExplorationState(hsSubjectState == null ? null : hsSubjectState.clone(),
                    hsObjectState == null ? null : hsObjectState.clone(), centerURIState);

            em.updateReachableCategories();
            catIn = em.rc.incomingCategories;

            if (!duplicatePredsOrTypes) {

                UnifiedSet<URI> usedTypes = new UnifiedSet<>();
                usedTypes.addAll(q.types);
                //typesToAvoid.forEach(uri -> usedTypes.add(uri)); // because neo4j can't handle it

                for (int k = 0; k < catIn.size(); k++) {
                    if (usedTypes.contains(catIn.get(k).getOutsider())) {
                        catIn.remove(k);
                        k--;
                    }
                }
            }

            if (catIn.size() > 0) {

                URI median = getPercentileFromCategories(catIn, quantile).getOutsider();

                QueryModel qCopy = q.getCopy();
                qCopy.types.add(median);
                qCopy.catConnections.add(CatConnection.IN);

                em.chooseIncomingConnection(median, null);

                ArrayList<QueryModel> queryModels = performJoins(remainingJoins - 1, qCopy);

                if (queryModels != null) {
                    result.addAll(queryModels);
                }
            }

        }

        if (catOut.size() > 0) {
            // reset exploration manager to the current state
            em.changeExplorationState(hsSubjectState == null ? null : hsSubjectState.clone(),
                    hsObjectState == null ? null : hsObjectState.clone(), centerURIState);

            em.updateReachableCategories();
            catOut = em.rc.outgoingCategories;

            if (!duplicatePredsOrTypes) {
                UnifiedSet<URI> usedTypes = new UnifiedSet<>();
                usedTypes.addAll(q.types);
                //typesToAvoid.forEach(uri -> usedTypes.add(uri)); // because neo4j can't handle it
                for (int k = 0; k < catOut.size(); k++) {
                    if (usedTypes.contains(catOut.get(k).getOutsider())) {
                        catOut.remove(k);
                        k--;
                    }
                }
            }

            if (catOut.size() > 0) {

                URI median = getPercentileFromCategories(catOut, quantile).getOutsider();

                QueryModel qCopy = q.getCopy();
                qCopy.types.add(median);
                qCopy.catConnections.add(CatConnection.OUT);

                em.chooseOutgoingConnection(median, null);

                ArrayList<QueryModel> queryModels = performJoins(remainingJoins - 1, qCopy);

                if (queryModels != null) {
                    result.addAll(queryModels);
                }
            }
        }

        return result;
    }

    private ArrayList<QueryModel> performJoinsForGivenPredicates(int remainingJoins, QueryModel q) {

        ArrayList<QueryModel> result = new ArrayList<QueryModel>();

        if (remainingJoins == 0) {
            result.add(q);
            return result;
        }

        UnifiedSet<Long> hsSubjectState = null;
        UnifiedSet<Long> hsObjectState = null;

        if (em.hsSubject != null) {

            hsSubjectState = em.hsSubject.clone();
            hsObjectState = em.hsObject.clone();
        }

        URI centerURIState = null;
        try {
            centerURIState = new URI(em.currentURI.toString());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        em.updatePredicateConnections();
        ConnectedPredicates[] connectedPreds = em.connectedPreds;

        if (!duplicatePredsOrTypes) {

            UnifiedSet<URI> usedTypes = new UnifiedSet<>();
            usedTypes.addAll(q.types);
            //typesToAvoid.forEach(uri -> usedTypes.add(uri)); // because neo4j can't handle it
            UnifiedSet<URI> usedPredicates = new UnifiedSet<>();
            usedPredicates.add(URI.create(RDFUtilities.rdfType)); // also not contained in neo4j
            usedPredicates.addAll(q.predicates);

            for (int k = 0; k < connectedPreds.length; k++) {
                ArrayList<SingleJoinModel> outgoingPreds = connectedPreds[k].outgoingPreds;
                ArrayList<SingleJoinModel> incomingPreds = connectedPreds[k].incomingPreds;
                for (int j = 0; j < outgoingPreds.size(); j++) {
                    if (usedTypes.contains(outgoingPreds.get(j).getOutsider())
                            || usedPredicates.contains(outgoingPreds.get(j).getPredicate())) {
                        outgoingPreds.remove(j);
                        j--;
                    }
                }
                for (int j = 0; j < incomingPreds.size(); j++) {
                    if (usedTypes.contains(incomingPreds.get(j).getOutsider())
                            || usedPredicates.contains(incomingPreds.get(j).getPredicate())) {
                        incomingPreds.remove(j);
                        j--;
                    }
                }
            }
        }

        SingleJoinModel predIn = getPercentileFromPredicates(true, connectedPreds, quantile);
        SingleJoinModel predOut = getPercentileFromPredicates(false, connectedPreds, quantile);

        if (predIn != null) {

            QueryModel qCopy = q.getCopy();
            qCopy.types.add(predIn.getOutsider());
            qCopy.predicates.add(predIn.getPredicate());
            qCopy.catConnections.add(CatConnection.IN);


            em.chooseIncomingConnection(predIn.getOutsider(), predIn.getPredicate().toString());

            ArrayList<QueryModel> queryModels = performJoinsForGivenPredicates(remainingJoins - 1, qCopy);

            if (queryModels != null) {
                result.addAll(queryModels);
            }

        }

        if (predOut != null) {
            // reset exploration manager to the current state - state is lost through
            // recursive method call
            em.changeExplorationState(hsSubjectState == null ? null : hsSubjectState.clone(),
                    hsObjectState == null ? null : hsObjectState.clone(), centerURIState);

            em.updatePredicateConnections();

            QueryModel qCopy = q.getCopy();
            qCopy.types.add(predOut.getOutsider());
            qCopy.predicates.add(predOut.getPredicate());
            qCopy.catConnections.add(CatConnection.OUT);

            em.chooseOutgoingConnection(predOut.getOutsider(), predOut.getPredicate().toString());

            ArrayList<QueryModel> queryModels = performJoinsForGivenPredicates(remainingJoins - 1, qCopy);

            if (queryModels != null) {
                result.addAll(queryModels);
            }
        }

        return result;
    }

    /**
     * Returns the type for which a rate of (1 - percentile) types of the array list have a higher amount of results.
     */
    private SingleJoinModel getPercentileFromCategories(ArrayList<SingleJoinModel> joinList, double percentile) {

        Collections.sort(joinList, new SingleJoinModel());
        SingleJoinModel result = joinList.get((int) ((joinList.size() - 1) * percentile));

        // set maxJoinElement in order to track the highest amount of join results
        maxJoinElement = Math.max(result.getResults(), maxJoinElement);
        return result;
    }

    /**
     * Calculates all reachable categories for the given types, saves the join results as SingleJoinModel objects, sorts them
     * in an ascending manner and returns the (1 - percentile) * arraySize element of the resulting ArrayList.
     */
    public SingleJoinModel getPercentileFromAllInitialTypes(URI[] types, double percentile, boolean incoming) {

        ArrayList<SingleJoinModel> results = getSortedInitialConnections(types, incoming);
        Collections.sort(results, new SingleJoinModel());

        SingleJoinModel result = results.get((int) ((results.size() - 1) * percentile));

        // set maxJoinElement in order to track the highest amount of join results
        maxJoinElement = Math.max(result.getResults(), maxJoinElement);

        return result;
    }

    public SingleJoinModel getPredicatePercentileFromAllInitialTypes(URI[] types, double percentile, boolean incoming) {

        ArrayList<SingleJoinModel> results = getSortedInitialPredicateConnections(types, incoming);

        SingleJoinModel result = results.get((int) ((results.size() - 1) * percentile));

        // set maxJoinElement in order to track the highest amount of join results
        maxJoinElement = Math.max(result.getResults(), maxJoinElement);
        return result;
    }

    public ArrayList<SingleJoinModel> getSortedInitialConnections(URI[] types, boolean incoming) {

        ArrayList<SingleJoinModel> results = new ArrayList<SingleJoinModel>();

        for (int i = 0; i < types.length; i++) {

            int typeIndex = em.btIndexManager.getTypePositionForURI(types[i]);
            em.chooseInitialClass(typeIndex);
            em.updateReachableCategories();

            SingleJoinModel tr = null;

            if (incoming) {

                results.addAll(em.rc.incomingCategories);
            } else {

                results.addAll(em.rc.outgoingCategories);
            }
        }

        filterGreaterThanThreshold(results);

        return results;
    }

    /**
     * Returns the type and predicate for which a rate of (1 - percentile) of the connected predicates have a higher
     * amount of results for the given connection type (i.e. "in" or "out").
     */
    private SingleJoinModel getPercentileFromPredicates(boolean incoming, ConnectedPredicates[] connectedPreds,
                                                             double percentile) {

        ArrayList<SingleJoinModel> relevantResults = new ArrayList<SingleJoinModel>();

        for (int i = 0; i < connectedPreds.length; i++) {

            if (incoming) {

                if (connectedPreds[i].incomingPreds.size() > 0) {
                    relevantResults.addAll(connectedPreds[i].incomingPreds);
                }
            } else {

                if (connectedPreds[i].outgoingPreds.size() > 0) {
                    relevantResults.addAll(connectedPreds[i].outgoingPreds);
                }
            }
        }

        filterGreaterThanThreshold(relevantResults);

        if (relevantResults.size() == 0) {

            return null;
        }

        // sort the relevant results in ascending order
        Collections.sort(relevantResults, new SingleJoinModel());
        SingleJoinModel result = relevantResults.get((int) ((relevantResults.size() - 1) * percentile));
        maxJoinElement = result.getResults();
        return result;
    }

    public ArrayList<SingleJoinModel> getSortedInitialPredicateConnections(URI[] types, boolean incoming) {
        ArrayList<SingleJoinModel> results = new ArrayList<SingleJoinModel>();

        for (int i = 0; i < types.length; i++) {

            int typeIndex = em.btIndexManager.getTypePositionForURI(types[i]);
            em.chooseInitialClass(typeIndex);
            em.updatePredicateConnections();

            for (int j = 0; j < em.btIndexManager.typeURIs.length; j++) {

                if (incoming) {

                    results.addAll(em.connectedPreds[j].incomingPreds);
                } else {

                    results.addAll(em.connectedPreds[j].outgoingPreds);
                }
            }

        }

        filterGreaterThanThreshold(results);

        Collections.sort(results, new SingleJoinModel());
        return results;
    }

    public ArrayList<SingleJoinModel> getInitialIncomingOutgoingCons(URI[] types, boolean incoming, boolean predicates) {
        ArrayList<SingleJoinModel> results = new ArrayList<SingleJoinModel>();

        for (int i = 0; i < types.length; i++) {
            int typeIndex = em.btIndexManager.getTypePositionForURI(types[i]);
            em.chooseInitialClass(typeIndex);
            if (predicates) {

                em.updatePredicateConnections();
                for (int j = 0; j < em.btIndexManager.typeURIs.length; j++) {

                    if (incoming) {

                        results.addAll(em.connectedPreds[j].incomingPreds);
                    } else {

                        results.addAll(em.connectedPreds[j].outgoingPreds);
                    }
                }
            } else {

                em.updateReachableCategories();
                if (incoming) {

                    results.addAll(em.rc.incomingCategories);
                } else {

                    results.addAll(em.rc.outgoingCategories);
                }

            }


        }
        Collections.sort(results, new SingleJoinModel());

        return results;
    }

    public long getMaxPercentileOfInitialIncomingOutgoingCons(URI types[], double quantile, boolean predicates) {
        ArrayList<SingleJoinModel> incoming = getInitialIncomingOutgoingCons(types, true, predicates);
        ArrayList<SingleJoinModel> outgoing = getInitialIncomingOutgoingCons(types, false, predicates);

        long inQuantile = 0;
        if (incoming.size() > 0) {

            inQuantile = incoming.get((int) ((incoming.size() - 1) * quantile)).getResults();
        }

        long outQuantile = 0;
        if (outgoing.size() > 0) {

            outQuantile = outgoing.get((int) ((outgoing.size() - 1) * quantile)).getResults();
        }

        return Math.max(inQuantile, outQuantile);
    }

    public ArrayList<SingleJoinModel> getMeansOfInitialIncomingOutgoingCons(URI[] types, double percentile, boolean predicates) {

            ArrayList<SingleJoinModel> results = new ArrayList<SingleJoinModel>();
            ArrayList<SingleJoinModel> incoming = getInitialIncomingOutgoingCons(types, true, predicates);
            ArrayList<SingleJoinModel> outgoing = getInitialIncomingOutgoingCons(types, false, predicates);


            SingleJoinModel percentileIn = new SingleJoinModel();
            if (incoming.size() > 0) {

                percentileIn = incoming.get((int) ((incoming.size() - 1) * percentile));
            }

            SingleJoinModel percentileOut = new SingleJoinModel();
            if (outgoing.size() > 0) {

                percentileOut = outgoing.get((int) ((outgoing.size() - 1) * percentile));
            }

            SingleJoinModel mean = new SingleJoinModel();
            mean.setCenter(percentileIn.getCenter());
            mean.setResults((long) ((percentileIn.getResults() + percentileOut.getResults()) / 2));

            results.add(mean);

            Collections.sort(results, new SingleJoinModel());
            return results;
    }


    public ArrayList<SingleJoinModel> getInitialTypesSortedByResults() {
        ArrayList<SingleJoinModel> results = new ArrayList<SingleJoinModel>();

        for (int i = 0; i < em.btIndexManager.typeURIs.length; i++) {
            em.chooseInitialClass(i);
            SingleJoinModel sjm = new SingleJoinModel();
            sjm.setResults(em.currentConnectionItID.estimatedNumResults());
            sjm.setCenter(em.btIndexManager.typeURIs[i]);
            results.add(sjm);
        }

        Collections.sort(results, new SingleJoinModel());
        return results;
    }

    public SingleJoinModel getPercentileOfTypes(double percentile) {

        ArrayList<SingleJoinModel> typeResults = getInitialTypesSortedByResults();
        return typeResults.get((int) ((typeResults.size() - 1) * percentile));
    }


    private void filterGreaterThanThreshold(ArrayList<SingleJoinModel> list) {

        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getResults() > upperBound
                    || list.get(i).getResults() <= lowerBound
            || !typesForThres.contains(list.get(i).getOutsider())) {
                list.remove(i);
                i--;

            }
        }
    }

}
