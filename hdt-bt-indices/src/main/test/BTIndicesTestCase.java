import btindices.indexgeneration.CtCIndicesGenerator;
import btindices.statisticalquerygeneration.CategoryQueryGenerator;
import btindices.statisticalquerygeneration.QueryModel;
import btindices.statisticalquerygeneration.QueryModelManager;
import org.rdfhdt.hdt.enums.RDFNotation;
import queryenginestubs.CtCIndicesStub;
import queryenginestubs.HDTJenaStub;
import queryenginestubs.interfaces.JoinQueryCalc;
import queryenginestubs.PlainHDTStub;
import org.junit.*;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * Holds the data which is required in the BT indices tests.
 */
public class BTIndicesTestCase {

    String ttlPath;
    String hdtPath;
    String ctcIndicesDir;
    String pvIndicesDir;
    String categoryQueriesDir;
    String filterQueriesDir;
    String hybridQueriesDir;
    JoinQueryCalc hdtJena;
    JoinQueryCalc plainHDT;
    JoinQueryCalc btIndices;
    ArrayList<QueryModel> qmList;
    String root;

    @Before
    public void setUp() {
        this.root = Paths.get(System.getProperty("user.dir"), "unit_test_data").toString();
        this.ttlPath = Paths.get(root, "swdf.ttl").toString();
        this.hdtPath = Paths.get(root, "swdf.hdt").toString();
        this.ctcIndicesDir = Paths.get(root,"ctcIndices").toString();
        this.pvIndicesDir = Paths.get(root,"pvIndices").toString();
        this.filterQueriesDir = Paths.get(root, "filterQueries").toString();
        this.categoryQueriesDir = Paths.get(root, "categoryQueries").toString();
        this.hybridQueriesDir = Paths.get(root, "hybridQueries").toString();

        File btiDirectory = new File(ctcIndicesDir);
        if (!btiDirectory.exists() || !(new File(hdtPath).exists())) {
            System.out.println("BT indices do not exits. Creating new index...");
            CtCIndicesGenerator btiGen = new CtCIndicesGenerator(ttlPath, RDFNotation.TURTLE, hdtPath, ctcIndicesDir);
            btiGen.generateCtCPSOIndices();
        }


        plainHDT = new PlainHDTStub(hdtPath);
        hdtJena = new HDTJenaStub(hdtPath);
        btIndices = new CtCIndicesStub(hdtPath, ctcIndicesDir);

        String filePath = Paths.get(categoryQueriesDir, "q2_2").toString();
        qmList = QueryModelManager.loadQueryModelsFromFile(filePath);
        if (qmList.size() == 0) {
            CategoryQueryGenerator cqe = new CategoryQueryGenerator(hdtPath, ctcIndicesDir, categoryQueriesDir);
            cqe.extractAllQueries(4);
        }
    }
}
