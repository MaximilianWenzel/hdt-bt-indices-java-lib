package btindices.indexgeneration;

import btindexmodels.BTIndex;
import btindices.indicesmanager.CtCIndicesManager;
import btindices.indicesmanager.CtCIndicesManagerImpl;
import btindices.HDTUtil;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.rdfhdt.hdt.enums.RDFNotation;
import org.rdfhdt.hdt.enums.TripleComponentOrder;
import org.rdfhdt.hdt.enums.TripleComponentRole;
import org.rdfhdt.hdt.triples.IteratorTripleID;
import org.rdfhdt.hdt.triples.TripleID;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

/**
 * This class is used to generate all Class-to-Class BT Indices for a given HDT file in order to provide faster query execution for
 * the exploration of RDF data.
 *
 * @author Maximilian Wenzel
 */
public class CtCIndicesGenerator extends BTIndicesGenerator {

    /**
     * Contains all predicate URIs for each of the classes.
     */
    protected ArrayList<ArrayList<ArrayList<String>>> predicates;

    long nextSbj;
    long nextObj;
    long sbjTypeID;
    long objTypeID;
    int sbjTypePos;
    int objTypePos;
    TripleID predTriple;
    TripleID typeSearch;
    TripleID predSearch;

    BTIndexGenerator[][] btig;

    public CtCIndicesGenerator(String hdtPath, String ctcIndicesDir) {
        super(hdtPath, ctcIndicesDir);
    }

    public CtCIndicesGenerator(String rdfPath, RDFNotation rdfNotation, String hdtOutputPath, String btIndicesDir) {
        super(rdfPath, rdfNotation, hdtOutputPath, btIndicesDir);
    }

    public static void main(String[] args) {
        CtCIndicesGenerator btIndicesGenerator;

        if (args.length == 2) {
            // <HDT-PATH> <BT-INDICES-DIRECTORY>
            // args[0] -> hdtPath
            // args[1] -> target BT Indices directory
            btIndicesGenerator = new CtCIndicesGenerator(args[0], args[1]);
        } else if (args.length == 3) {
            // <RDF-PATH> <HDT-OUTPUT-PATH> <BT-INDICES-DIRECTORY>
            RDFNotation rdfNotation = RDFNotation.guess(args[0]);
            btIndicesGenerator = new CtCIndicesGenerator(args[0], rdfNotation, args[1], args[2]);
        } else {
            System.err.println("Could not parse arguments. Available command line functions:");
            System.err.println("CtCIndicesGenerator.jar <HDT-PATH> <BT-INDICES-DIRECTORY>");
            System.err.println("CtCIndicesGenerator.jar <RDF-PATH> <HDT-OUTPUT-PATH> <BT-INDICES-DIRECTORY>");
            return;
        }

        System.out.println("Start generating indices...");
        btIndicesGenerator.generateCtCPSOIndices();

        btIndicesGenerator.writeStatisticsToFile();
    }


    /**
     * Generates the CtC indices in the specified directory.
     */
    public void generateCtCPSOIndices() {
        globalSw.reset();

        fetchCategoriesFromHDTFile();
        new File(directoryPath).mkdirs();

        btig = new BTIndexGenerator[types.size()][types.size()];


        BTIndexGenerator currentIndex;

        long numPreds = dic.getNpredicates();

        typeSearch = new TripleID(0, rdfTypeID, 0);
        predSearch = new TripleID(0,0,0);

        for (int i = 0; i < types.size(); i++) {
            for (int j = 0; j < types.size(); j++) {
                btig[i][j] = new BTIndexGenerator(hdt, TripleComponentOrder.PSO);
            }
        }

        for (long pred = 1; pred <= numPreds; pred++) {

            predSearch.setPredicate(pred);
            IteratorTripleID itID = HDTUtil.executeQuery(hdt, predSearch);

            while (itID.hasNext()) {
                predTriple = itID.next();
                nextSbj = predTriple.getSubject();
                typeSearch.setSubject(nextSbj);
                IteratorTripleID sbjTypeSearch = HDTUtil.executeQuery(hdt, typeSearch);

                while (sbjTypeSearch.hasNext()) {
                    // for each subject type
                    sbjTypeID = sbjTypeSearch.next().getObject();
                    sbjTypePos = typeIDToTypePos.getOrDefault(sbjTypeID, -1);
                    if (sbjTypePos == -1) {
                        // not a relevant type
                        continue;
                    }

                    // for each object type
                    nextObj = predTriple.getObject();
                    if (nextObj > dic.getNshared()) {
                        // object has no rdf type
                        break;
                    }
                    typeSearch.setSubject(nextObj);
                    IteratorTripleID objTypeSearch = HDTUtil.executeQuery(hdt, typeSearch);

                    while (objTypeSearch.hasNext()) {
                        objTypeID = objTypeSearch.next().getObject();
                        objTypePos = typeIDToTypePos.getOrDefault(objTypeID, -1);
                        if (objTypePos == -1) {
                            continue;
                        }

                        currentIndex = btig[sbjTypePos][objTypePos];
                        // PS-O order
                        currentIndex.currX = pred;
                        currentIndex.currY = nextSbj;
                        currentIndex.currZ = nextObj;
                        currentIndex.addTripleToBTIndex();
                    }
                }
            }
        }

        for (int i = 0; i < btig.length; i++) {
            System.out.println("Create CtC indices for type (" + (i + 1) + "/" + btig.length + ")");
            for (int j = 0; j < btig[i].length; j++) {
                BTIndex bti = btig[i][j].createBTIndex();
                saveBTIndex(bti, i + "_" + j);
            }
        }
        saveCategoriesAndPredicates();
        this.timeGeneratingBTIndices = globalSw.stopAndShow();
    }



    /**
     * Saves the categories and predicates to the specified path.
     */
    protected void saveCategoriesAndPredicates() {
        // save classes BEFORE predicate retrieval, because they are needed for
        // efficient querying
        saveClasses();

        // fetch predicates at the end because bt indices can be used for efficient
        // retrieval
        sw.reset();
        System.out.println("Fetching predicate URIs from HDT file ...");
        btIndicesManager = new CtCIndicesManagerImpl(directoryPath, hdt, false);

        try {
            predicates = getDistinctPredicatesForClasses(btIndicesManager);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        System.out.println("Fetched predicate URIs in " + sw.stopAndShow());

        savePredicates();

    }

    /**
     * Save Format: All distinct predicates for each of the classes are saved to a file called "rdfPredicateURIs". The
     * predicates assigned to a single RDF class are separated by a semicolon. The predicate lists for different classes
     * are separated by "\r\n". The predicates index corresponds to the class URI index, e.g. all predicates of
     * "predicates[0][1]" are mapped to the BTIndex "class0-class1".
     */
    public void savePredicates() {
        // save all predicate URIs to the index directory
        try (CSVPrinter csvPrinter = new CSVPrinter(new FileWriter(directoryPath + "/" + "rdfPredicateURIs"),
                CSVFormat.DEFAULT.withHeader("classFrom", "classTo", "predicate"))) {

            // classesFrom
            for (int i = 0; i < predicates.size(); i++) {

                // classesTo
                for (int k = 0; k < predicates.get(i).size(); k++) {

                    // specific predicates
                    for (int j = 0; j < predicates.get(i).get(k).size(); j++) {

                        csvPrinter.printRecord(types.get(i), types.get(k), predicates.get(i).get(k).get(j));
                    }

                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Calculates all distinct predicates which exist within a BT Index.
     */
    private ArrayList<ArrayList<ArrayList<String>>> getDistinctPredicatesForClasses(CtCIndicesManager btIndexManager)
            throws URISyntaxException {
        ArrayList<ArrayList<ArrayList<String>>> result = new ArrayList<ArrayList<ArrayList<String>>>();

        for (int i = 0; i < types.size(); i++) {

            ArrayList<ArrayList<String>> reachableCategories = new ArrayList<ArrayList<String>>();

            int centerIndex = btIndexManager.getTypePositionForURI(new URI(types.get(i)));

            for (int k = 0; k < types.size(); k++) {

                int outsiderIndex = btIndexManager.getTypePositionForURI(new URI(types.get(k)));

                if (centerIndex == -1 || outsiderIndex == -1) {
                    System.out.println(i);
                    System.out.println(k);
                }
                BTIndex btIndex = btIndexManager.btIndices[centerIndex][outsiderIndex];

                // save predicates to hash set in order to get distinct predicates
                UnifiedSet<Long> predicateHs = new UnifiedSet<Long>();

                // get all triples of the index
                IteratorTripleID itID = btIndex.search(new TripleID(0, 0, 0));

                while (itID.hasNext()) {

                    predicateHs.add(itID.next().getPredicate());
                }

                ArrayList<String> predicates = new ArrayList<String>();

                Iterator<Long> predIterator = predicateHs.iterator();
                long val;

                while (predIterator.hasNext()) {
                    val = predIterator.next();
                    predicates.add(btIndexManager.dic.idToString(val, TripleComponentRole.PREDICATE).toString());
                }

                reachableCategories.add(predicates);
            }

            result.add(reachableCategories);
        }

        return result;
    }



}
