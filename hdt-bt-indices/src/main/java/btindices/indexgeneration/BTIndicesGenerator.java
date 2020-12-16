package btindices.indexgeneration;

import btindexmodels.BTIndex;
import btindices.indicesmanager.CtCIndicesManager;
import btindices.RDFUtilities;
import btindices.HDTUtil;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.rdfhdt.hdt.dictionary.Dictionary;
import org.rdfhdt.hdt.enums.RDFNotation;
import org.rdfhdt.hdt.enums.TripleComponentRole;
import org.rdfhdt.hdt.exceptions.ParserException;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTManager;
import org.rdfhdt.hdt.hdt.HDTVocabulary;
import org.rdfhdt.hdt.listener.ProgressOut;
import org.rdfhdt.hdt.options.ControlInfo;
import org.rdfhdt.hdt.options.ControlInformation;
import org.rdfhdt.hdt.options.HDTSpecification;
import org.rdfhdt.hdt.triples.IteratorTripleID;
import org.rdfhdt.hdt.triples.TripleID;
import org.rdfhdt.hdt.triples.Triples;
import org.rdfhdt.hdt.util.StopWatch;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * This class is used to generate all BT Indices for a given HDT file in order to provide faster query execution for
 * exploration of the RDF data.
 *
 * @author Maximilian Wenzel
 */
public abstract class BTIndicesGenerator {

    public final static StopWatch globalSw = new StopWatch();
    public final static StopWatch sw = new StopWatch();

    /**
     * Time which was required in order to generate the indexed HDT file from an RDF dump file.
     */
    public String timeGeneratingHDT;

    /**
     * Time which was required in order to generate the indexed HDT file from an RDF dump file.
     */
    public String timeGeneratingHDTIndex;

    public String timeGeneratingBTIndices;

    /**
     * This variable is required in order to save and load BitmapTriples.
     */
    protected ControlInformation ci;
    /**
     * The actual class representation of an HDT file.
     */
    protected HDT hdt;
    protected Dictionary dic;
    protected long rdfTypeID;


    /**
     * Represents the path to the directory which will be created for the index, e.g. "/index" for a new directory
     * called "index" in the root folder.
     */
    public String directoryPath;
    /**
     * Contains all RDF types of the specified HDT file.
     */
    public ArrayList<String> types;
    protected HashMap<Long, Integer> typeIDToTypePos;

    /**
     * Contains all predicate URIs for each of the classes.
     */
    protected ArrayList<ArrayList<ArrayList<String>>> predicates;
    /**
     * Represents an array list which contains all resources for each rdf types of the specified HDT file as an
     * iterator.
     */
    protected ArrayList<IteratorTripleID> typeResourcesIt;
    /**
     * Is used in order to fetch efficiently all distinct predicates from each BTindex.
     */
    protected CtCIndicesManager btIndicesManager;
    /**
     * Progress listener which acts a parameter of certain methods of the HDT library in order to notify the user about
     * hte progress.
     */
    protected ProgressOut pOut;

    /**
     * @param btIndicesDir Represents the path to the directory which will be created for the index, e.g. "/index" for
     *                      a new directory called "index" in the root directory.
     */
    public BTIndicesGenerator(String hdtFilePath, String btIndicesDir) {

        initializeVariables(btIndicesDir, hdtFilePath);
    }

    /**
     * If a HDT file for a given RDF dataset has to be generated first, use this constructor.
     */
    public BTIndicesGenerator(String rdfPath, RDFNotation rdfNotation, String hdtOutputPath, String btIndicesDir) {
        String hdtBaseURI = "file://" + rdfPath;
        hdtBaseURI = hdtBaseURI.replace("\\", "/");
        // generate hdt file
        try {
            ProgressOut po = new ProgressOut();

            globalSw.reset();
            HDT hdt = HDTManager.generateHDT(rdfPath, hdtBaseURI, rdfNotation, new HDTSpecification(), po);
            timeGeneratingHDT = globalSw.stopAndShow();

            globalSw.reset();
            HDT hdtIndexed = HDTManager.indexedHDT(hdt, po);
            timeGeneratingHDTIndex = globalSw.stopAndShow();

            hdt.saveToHDT(hdtOutputPath, po);
            hdtIndexed.saveToHDT(hdtOutputPath + ".index.v1-1", po);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParserException e) {
            e.printStackTrace();
        }

        initializeVariables(btIndicesDir, hdtOutputPath);
    }


    protected void initializeVariables(String directoryPath, String hdtFilePath) {
        // initialize necessary variables
        ci = new ControlInformation();
        ci.clear();
        ci.setType(ControlInfo.Type.TRIPLES);
        ci.setFormat(HDTVocabulary.TRIPLES_TYPE_BITMAP);

        this.directoryPath = directoryPath;
        this.pOut = new ProgressOut();

        System.out.println("Loading HDT file...");
        this.hdt = HDTUtil.loadHDTFile(hdtFilePath);
        this.dic = hdt.getDictionary();
        this.rdfTypeID = dic.stringToId(RDFUtilities.rdfType, TripleComponentRole.PREDICATE);
    }

    public void fetchCategoriesFromHDTFile() {
        sw.reset();
        System.out.println("Fetching class URIs from HDT file ...");
        types = getDistinctClasses(hdt);
        System.out.println("Fetched class URIs in " + sw.stopAndShow());

        typeIDToTypePos = new HashMap<>();
        for (int i = 0; i < types.size(); i++) {
            long typeID = dic.stringToId(types.get(i), TripleComponentRole.OBJECT);
            typeIDToTypePos.put(typeID, i);
        }

        this.typeResourcesIt = getIteratorForEachClass(hdt, types);
    }

    public void saveBTIndex(BTIndex btIndex, String name) {
        try (OutputStream out = new FileOutputStream(directoryPath + "/" + name)) {
            // save bitmap triples to file
            btIndex.save(out, ci, pOut);

        } catch (Exception e) {
            e.printStackTrace();
        }
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
     * Save format: All distinct classes are saved to a file called "rdfClassURIs" and are separated by a semicolon.
     */
    void saveClasses() {
        // save all class URIs to the index directory
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(directoryPath + "/" + "rdfClassURIs"))) {

            for (String classURI : types) {
                writer.write(classURI + ";");
                writer.flush();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Fetches all triples for each rdf class of a specified HDT file. Returns the resources of each rdf class as an
     * iterator.
     */
    private ArrayList<IteratorTripleID> getIteratorForEachClass(HDT hdt, ArrayList<String> classes) {

        String classURI;
        Triples triples = hdt.getTriples();
        Dictionary dic = hdt.getDictionary();
        long rdfTypeID = dic.stringToId(RDFUtilities.rdfType, TripleComponentRole.PREDICATE);

        // get resources for each class
        /*
         * for each class c in C: SELECT ?x WHERE { ?x rdf:type c . } end
         */

        ArrayList<IteratorTripleID> itIDClassResources = new ArrayList<IteratorTripleID>();

        for (int i = 0; i < classes.size(); i++) {
            classURI = classes.get(i);

            long classID = dic.stringToId(classURI, TripleComponentRole.OBJECT);

            // 0 acts as wildcard for a tripleID pattern
            TripleID tID = new TripleID(0, rdfTypeID, classID);
            IteratorTripleID itID = triples.search(tID);
            itIDClassResources.add(itID);
        }

        return itIDClassResources;

    }


    /**
     * Fetches all RDF classes of a specified HDT file and returns them as an array list.
     *
     * @param hdt The HDT file to be queried.
     * @return An array list of all RDF classes of the corresponding HDT file.
     */
    private ArrayList<String> getDistinctClasses(HDT hdt) {


        long rdfTypeID = hdt.getDictionary().stringToId(RDFUtilities.rdfType, TripleComponentRole.PREDICATE);
        TripleID tID = new TripleID(0, rdfTypeID, 0);

        IteratorTripleID itID = hdt.getTriples().search(tID);

        // try to extract all categories with rdf:type predicate
        UnifiedSet<Long> hs = new UnifiedSet<>();
        while (itID.hasNext()) {
            hs.add(itID.next().getObject());
        }

        // remove undesired categories
        HDTUtil.removeUndesiredCategories(hdt.getDictionary(), hs);

        // if more than 100 categories exist for the data set, reduce number of categories to reasonable size
        if (hs.size() > 100) {
            int minimumSize = 15;
            System.out.println("Number of categories too large: " + hs.size());
            System.out.println("Try to reduce it number of categories...");
            // add all classes to hash set if the number of resources in the category is less than the threshold
            Triples triples = hdt.getTriples();
            double lowerBound = triples.size() * 0.8;
            tID.setAll(0, 0,0);
            long numberOfCategories = 0;
            int i = 16;
            while (numberOfCategories < minimumSize && lowerBound > 1) {
                System.out.println("Lower bound for number of resources in categories: " + lowerBound);
                hs = new UnifiedSet<Long>();
                IteratorTripleID itClass;
                while (itID.hasNext()) {
                    long obj = itID.next().getObject();
                    tID.setObject(obj);
                    itClass = triples.search(tID);
                    if (itClass.estimatedNumResults() > lowerBound) {
                        hs.add(obj);
                    }
                }
                itID.goToStart();
                lowerBound *= 0.1;
                numberOfCategories = hs.size();
                System.out.println("Number of categories: " + numberOfCategories);
            }
        }

        // iterate over hash set to get all distinct classes
        Iterator<Long> distinctClasses = hs.iterator();
        ArrayList<String> classesList = new ArrayList<String>();

        while (distinctClasses.hasNext()) {
            long classID = distinctClasses.next();
            // get string from id
            String classStr = hdt.getDictionary().idToString(classID, TripleComponentRole.OBJECT).toString();
            classesList.add(classStr);
        }
        return classesList;
    }

    public void writeStatisticsToFile() {
        try {
            FileWriter fw = new FileWriter("ctcIndicesGenerationDuration.txt");
            fw.write("Time required to generate all Category indices: " + this.timeGeneratingBTIndices + "\r\n");
            if (this.timeGeneratingHDT != null) {
                fw.write("Time required to generate HDT file: " + this.timeGeneratingHDT + "\r\n");
                fw.write("Time required to generate HDT index: " + this.timeGeneratingHDTIndex + "\r\n");
                fw.write("Number of triples: " + this.hdt.getTriples().getNumberOfElements() + "\r\n");
                fw.write("Number of RDF classes: " + this.types.size());
            }
            fw.flush();
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
