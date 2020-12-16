import btindexmodels.categoryexplorationmodels.CatConnection;
import btindices.indexgeneration.CtCIndicesGenerator;
import btindices.indexgeneration.PVIndicesGenerator;
import btindices.RDFUtilities;
import btindices.statisticalquerygeneration.*;
import org.rdfhdt.hdt.enums.RDFNotation;
import queryenginestubs.PVIndicesStub;
import queryenginestubs.PlainHDTStub;
import org.junit.*;
import org.rdfhdt.hdt.triples.IteratorTripleID;
import org.rdfhdt.hdt.triples.TripleID;
import org.rdfhdt.hdt.util.StopWatch;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BTIndicesGenerationTests extends BTIndicesTestCase {

    @Test
    public void testFSIndicesGeneration() {
        PVIndicesGenerator btiFS = new PVIndicesGenerator(hdtPath, pvIndicesDir);
        StopWatch sw = new StopWatch();
        sw.reset();
        System.out.println("Together: " + sw.stopAndShow());

        sw.reset();
        btiFS.generatePVIndicesSeparately();
        System.out.println("Separately: " + sw.stopAndShow());
    }

    @Test
    public void testCatBTIndicesGeneration() {
        CtCIndicesGenerator btiFS = new CtCIndicesGenerator(ttlPath, RDFNotation.TURTLE, hdtPath, ctcIndicesDir);
        btiFS.generateCtCPSOIndices();
    }

    @Test
    public void testFilterQueryGeneration() {
        String queryOutputDir = filterQueriesDir;
        FilterQueryGenerator fqg = new FilterQueryGenerator(hdtPath, pvIndicesDir, queryOutputDir, 3);
        fqg.extract3LevelsOfDifficulty();
        String[] args = {queryOutputDir, "3", "3"};
        QueryModelPrinter.main(args);
    }

    @Test
    public void testCategoryQueryGeneration() {
        CategoryQueryGenerator cqg = new CategoryQueryGenerator(hdtPath, ctcIndicesDir, categoryQueriesDir);
        cqg.extractAllQueries(4);
        QueryModelManager.queryModelToString(categoryQueriesDir, 4, 3);
    }

    @Test
    public void testHybridQueryGeneration() {
        HybridQueryGenerator hqg = new HybridQueryGenerator(hdtPath, ctcIndicesDir, pvIndicesDir, categoryQueriesDir,
                filterQueriesDir, hybridQueriesDir);
        hqg.startExtraction();
        QueryModelManager.queryModelToString(hybridQueriesDir, 4, 3);
    }

    @Test
    public void testFSIndicesPOSSortedOrder() {
        PVIndicesStub PVIndicesStub = new PVIndicesStub(hdtPath, pvIndicesDir);
        PlainHDTStub plainHDTStub = new PlainHDTStub(hdtPath);
        TripleID tID = new TripleID(0,0,0);
        String rdfType = PVIndicesStub.fsManager.typeURIs[0].toString();
        IteratorTripleID itID = PVIndicesStub.fsManager.indicesManager.queryBTIndex(tID, rdfType, CatConnection.OUT);

        TripleID curr = new TripleID(0,0,0);
        TripleID last = new TripleID(0,0,0);

        // POS index
        while (itID.hasNext()) {
            curr.assign(itID.next());
            //System.out.println(curr.getPredicate() + " " + curr.getObject() + " " + curr.getSubject());

            assertTrue(curr.getPredicate() >= last.getPredicate());

            if (curr.getPredicate() == last.getPredicate()
                && curr.getObject() != last.getObject()) {
                assertTrue(curr.getObject() > last.getObject());

            } else if (curr.getObject() == last.getObject()){
                assertTrue(curr.getSubject() > last.getSubject());
            }
            last.assign(curr);
        }
    }


    @Test
    public void testCategoryExtraction() {
        CtCIndicesGenerator btiGenerator = new CtCIndicesGenerator(hdtPath, "btIndices");
        btiGenerator.fetchCategoriesFromHDTFile();

        for (int i = 0; i < btiGenerator.types.size(); i++) {
            assertFalse(RDFUtilities.undesiredCategories.contains(btiGenerator.types.get(i)));
        }
    }



}
