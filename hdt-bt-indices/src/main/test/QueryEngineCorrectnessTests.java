import btindexmodels.categoryexplorationmodels.CenterHashSets;
import btindexmodels.categoryexplorationmodels.ConnectedPredicates;
import btindexmodels.categoryexplorationmodels.ReachableCategories;
import btindexmodels.categoryexplorationmodels.SingleJoinModel;
import btindexmodels.facetedsearchmodels.AvailableFacet;
import btindices.statisticalquerygeneration.QueryModel;
import btindices.statisticalquerygeneration.QueryModelManager;
import queryenginestubs.*;
import org.junit.Before;
import org.junit.Test;
import org.rdfhdt.hdt.util.StopWatch;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class QueryEngineCorrectnessTests extends BTIndicesTestCase {

    @Before
    public void setUp() {
        super.setUp();
    }

    @Test
    public void testFSQueries() {
        PVIndicesStub PVIndicesStub = new PVIndicesStub(hdtPath, pvIndicesDir);
        PlainHDTStub plainHDTStub = new PlainHDTStub(hdtPath);

        String qmPath = Paths.get(filterQueriesDir, "q2_2").toString();
        ArrayList<QueryModel> qms = QueryModelManager.loadQueryModelsFromFile(qmPath);

        StopWatch sw = new StopWatch();
        double totalTimePV = 0;
        double totalTimeHDT = 0;

        for (int i = 0; i < qms.size(); i++) {
            QueryModel qm = qms.get(i);
            sw.reset();
            ArrayList<String> resFSIndices = PVIndicesStub.applyFacets(qm);
            totalTimePV += (double) sw.stopAndGet() / 1000000;
            Collections.sort(resFSIndices);

            sw.reset();
            ArrayList<String> resPlainHDT = plainHDTStub.applyFacets(qm);
            totalTimeHDT += (double) sw.stopAndGet() / 1000000;
            Collections.sort(resPlainHDT);


            assertTrue(resFSIndices.size() == resPlainHDT.size());

            for (int j = 0; j < resFSIndices.size(); j++) {
                assertTrue(resFSIndices.get(j).equals(resPlainHDT.get(j)));
            }
        }
        System.out.println("PV indices: " + totalTimePV);
        System.out.println("Plain HDT: " + totalTimeHDT);
    }

    @Test
    public void testVerboseQueries() {

        for (int i = 0; i < qmList.size(); i++) {
            //System.out.println(QueryModelFormatter.getSparqlQuery(qmList.get(i)));
            qmList.get(i).onlyCount = false;
            ArrayList<String> resultJena = hdtJena.executeQuery(qmList.get(i));
            ArrayList<String> resultBTIndices = btIndices.executeQuery(qmList.get(i));
            ArrayList<String> resultPlainHDT = plainHDT.executeQuery(qmList.get(i));

            Collections.sort(resultJena);
            Collections.sort(resultBTIndices);
            Collections.sort(resultPlainHDT);

            for (int j = 0; j < resultJena.size(); j++) {
                assertEquals(resultJena.get(j), resultBTIndices.get(j));
                assertEquals(resultPlainHDT.get(j), resultJena.get(j));
            }

        }
    }

    @Test
    public void testReachableCategories() {
        CtCIndicesStub btIndices = new CtCIndicesStub(hdtPath, ctcIndicesDir);
        PlainHDTStub plainHDT = new PlainHDTStub(hdtPath);

        URI centerURI = null;
        QueryModel qm = new QueryModel();
        qm.catConnections = null;
        try {
            centerURI = new URI("http://data.semanticweb.org/ns/swc/ontology#OrganisedEvent");
            qm.types.add(centerURI);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        CenterHashSets chs = plainHDT.executeQueryAndReturnHashSet(qm);

        ReachableCategories rcBTIndices = btIndices.calcReachableCategories(chs.hsSubject, chs.hsObject, centerURI);
        ReachableCategories rcPlainHDT = plainHDT.calcReachableCategories(chs.hsSubject, chs.hsObject, centerURI);

        System.out.println("PlainHDT:");
        rcPlainHDT.printReachableCategories();
        System.out.println();
        System.out.println("BTIndices:");
        rcBTIndices.printReachableCategories();


        for (int i = 0; i < rcPlainHDT.incomingCategories.size(); i++) {
            SingleJoinModel sjmPlainHDT = rcPlainHDT.incomingCategories.get(i);
            SingleJoinModel sjmBTIndices = rcBTIndices.incomingCategories.get(i);
            assertEquals(sjmPlainHDT.getOutsider(), sjmBTIndices.getOutsider());
            assertEquals(sjmPlainHDT.getResults(), sjmBTIndices.getResults());
        }

        for (int i = 0; i < rcPlainHDT.outgoingCategories.size(); i++) {
            SingleJoinModel sjmPlainHDT = rcPlainHDT.outgoingCategories.get(i);
            SingleJoinModel sjmBTIndices = rcBTIndices.outgoingCategories.get(i);
            assertEquals(sjmPlainHDT.getOutsider(), sjmBTIndices.getOutsider());
            assertEquals(sjmPlainHDT.getResults(), sjmBTIndices.getResults());
        }
    }

    @Test
    public void testConnectedPredicates() {
        CtCIndicesStub btIndices = new CtCIndicesStub(hdtPath, ctcIndicesDir);
        PlainHDTStub plainHDT = new PlainHDTStub(hdtPath);

        URI centerURI = null;
        QueryModel qm = new QueryModel();
        qm.catConnections = null;
        try {
            centerURI = new URI("http://data.semanticweb.org/ns/swc/ontology#OrganisedEvent");
            qm.types.add(centerURI);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        CenterHashSets chs = plainHDT.executeQueryAndReturnHashSet(qm);
        ConnectedPredicates[] cpBTIndices = btIndices.calcReachableCategoriesPreds(chs.hsSubject, chs.hsObject, centerURI);
        ConnectedPredicates[] cpPlainHDT = plainHDT.calcReachableCategoriesPreds(chs.hsSubject, chs.hsObject, centerURI);

        System.out.println("PlainHDT:");
        for (int i = 0; i < cpBTIndices.length; i++) {
            cpPlainHDT[i].printReachableCategories();
        }
        System.out.println();
        System.out.println("BTIndices:");
        for (int i = 0; i < cpBTIndices.length; i++) {
            cpBTIndices[i].printReachableCategories();
        }

        for (int k = 0; k < cpBTIndices.length; k++) {
            for (int i = 0; i < cpBTIndices[k].incomingPreds.size(); i++) {
                Collections.sort(cpPlainHDT[k].incomingPreds, new SingleJoinModel());
                Collections.sort(cpBTIndices[k].incomingPreds, new SingleJoinModel());
                SingleJoinModel sjmPlainHDT = cpPlainHDT[k].incomingPreds.get(i);
                SingleJoinModel sjmBTIndices = cpBTIndices[k].incomingPreds.get(i);
                assertEquals(sjmPlainHDT.getOutsider(), sjmBTIndices.getOutsider());
                assertEquals(sjmPlainHDT.getPredicate(), sjmBTIndices.getPredicate());
                assertEquals(sjmPlainHDT.getResults(), sjmBTIndices.getResults());
            }
        }

        for (int k = 0; k < cpBTIndices.length; k++) {
            for (int i = 0; i < cpBTIndices[k].outgoingPreds.size(); i++) {
                Collections.sort(cpPlainHDT[k].outgoingPreds, new SingleJoinModel());
                Collections.sort(cpBTIndices[k].outgoingPreds, new SingleJoinModel());
                SingleJoinModel sjmPlainHDT = cpPlainHDT[k].outgoingPreds.get(i);
                SingleJoinModel sjmBTIndices = cpBTIndices[k].outgoingPreds.get(i);
                assertEquals(sjmPlainHDT.getOutsider(), sjmBTIndices.getOutsider());
                assertEquals(sjmPlainHDT.getPredicate(), sjmBTIndices.getPredicate());
                assertEquals(sjmPlainHDT.getResults(), sjmBTIndices.getResults());
            }
        }

    }

    @Test
    public void testCalcAvailableFacets() {
        PVIndicesStub PVIndicesStub = new PVIndicesStub(hdtPath, pvIndicesDir);
        PlainHDTStub plainHDTStub = new PlainHDTStub(hdtPath);

        String qmPath = Paths.get(filterQueriesDir, "q1_2").toString();
        ArrayList<QueryModel> qms = QueryModelManager.loadQueryModelsFromFile(qmPath);

        ArrayList<AvailableFacet> afFSIndices;
        ArrayList<AvailableFacet> afPlainHDT;

        for (int i = 0; i < qms.size() && i < 10; i++) {
            PVIndicesStub.updateFSManagerState(qms.get(i));
            PVIndicesStub.calcAvailableFacets();

            plainHDTStub.updateFSManagerState(qms.get(i));
            plainHDTStub.calcAvailableFacets();

            afFSIndices = PVIndicesStub.fsManager.availableFacets.outgoingConnections;
            afPlainHDT = plainHDTStub.fsManager.availableFacets.outgoingConnections;
            compareAvailableFacets(afFSIndices, afPlainHDT);

            afFSIndices = PVIndicesStub.fsManager.availableFacets.incomingConnections;
            afPlainHDT = plainHDTStub.fsManager.availableFacets.incomingConnections;
            compareAvailableFacets(afFSIndices, afPlainHDT);
        }
    }

    private void compareAvailableFacets(ArrayList<AvailableFacet> afs1, ArrayList<AvailableFacet> afs2) {
        Collections.sort(afs1);
        Collections.sort(afs2);
        assertTrue(afs1.size() == afs2.size());

        for (int j = 0; j < afs1.size(); j++) {
            assertTrue(afs1.get(j).equals(afs2.get(j)));
        }
    }

    @Test
    public void testHybridQueries() {
        CtCPVHybridStub hybridStub = new CtCPVHybridStub(hdtPath, ctcIndicesDir, pvIndicesDir);
        PlainHDTStub plainHDTStub = new PlainHDTStub(hdtPath);

        ArrayList<QueryModel> qms = QueryModelManager.loadQueryModelsFromFile(hybridQueriesDir + "/q2_2");

        ArrayList<String> resultsHybrid;
        ArrayList<String> resultsPlainHDT;

        for (int i = 0; i < 100 && i < qms.size(); i++) {
            qms.get(i).setCenter(-1);
            resultsHybrid = hybridStub.executeHybridQuery(qms.get(i));
            resultsPlainHDT = plainHDTStub.executeHybridQuery(qms.get(i));

            //System.out.println(QueryModelFormatter.getSparqlQuery(qms.get(i)));

            Collections.sort(resultsHybrid);
            Collections.sort(resultsPlainHDT);

            //System.out.println(resultsHybrid);
            //System.out.println(resultsPlainHDT);
            //System.out.println(resultsRDFox);

            assertTrue(resultsHybrid.equals(resultsPlainHDT));
        }
    }
}
