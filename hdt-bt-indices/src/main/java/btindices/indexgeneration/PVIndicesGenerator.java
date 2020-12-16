package btindices.indexgeneration;

import btindexmodels.BTIndex;
import btindices.HDTUtil;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.rdfhdt.hdt.enums.RDFNotation;
import org.rdfhdt.hdt.enums.TripleComponentOrder;
import org.rdfhdt.hdt.enums.TripleComponentRole;
import org.rdfhdt.hdt.triples.IteratorTripleID;
import org.rdfhdt.hdt.triples.TripleID;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.TreeSet;

public class PVIndicesGenerator extends BTIndicesGenerator {

    private String sparqlQuery;

    long nextObj;

    long objTypeID;
    int objTypePos;
    long nextSbj;

    long sbjTypeID;
    int sbjTypePos;
    BTIndexGenerator currentIndex;

    public PVIndicesGenerator(String hdtPath, String pvIndicesDir) {
        super(hdtPath, pvIndicesDir);
    }

    public PVIndicesGenerator(String rdfPath, RDFNotation rdfNotation, String hdtOutputPath, String pvIndicesDir) {
        super(rdfPath, rdfNotation, hdtOutputPath, pvIndicesDir);
    }

    public static void main(String[] args) {

        PVIndicesGenerator btIndicesGenerator;

        if (args.length == 2) {
            // <HDT-PATH> <FS-INDICES-DIRECTORY>
            // args[0] -> hdtPath
            // args[1] -> target FS Indices directory
            btIndicesGenerator = new PVIndicesGenerator(args[0], args[1]);
        } else if (args.length == 3) {
            // <RDF-PATH> <HDT-OUTPUT-PATH> <FS-INDICES-DIRECTORY>
            RDFNotation rdfNotation = RDFNotation.guess(args[0]);
            btIndicesGenerator = new PVIndicesGenerator(args[0], rdfNotation, args[1], args[2]);
        } else {
            System.err.println("Could not parse arguments. Available command line functions:");
            System.err.println("PVIndicesGenerator.jar <HDT-PATH> <BT-INDICES-DIRECTORY>");
            System.err.println("PVIndicesGenerator.jar <RDF-PATH> <HDT-OUTPUT-PATH> <BT-INDICES-DIRECTORY>");
            return;
        }

        btIndicesGenerator.generatePVIndices();
        try {
            FileWriter fw = new FileWriter("pvIndicesGenerationDuration.txt");
            fw.write("Time required to generate all FS indices: " + btIndicesGenerator.timeGeneratingBTIndices);
            if (btIndicesGenerator.timeGeneratingHDT != null) {
                fw.write("Time required to generate HDT file: " + btIndicesGenerator.timeGeneratingHDT);
                fw.write("Time required to generate HDT index: " + btIndicesGenerator.timeGeneratingHDTIndex);
            }
            fw.flush();
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void generatePVIndices() {
        globalSw.reset();
        fetchCategoriesFromHDTFile();
        new File(directoryPath).mkdirs();
        generateOutgoingPOSPVIndices();
        generateIncomingPSOPVIndices();
        saveClasses();
        this.timeGeneratingBTIndices = globalSw.stopAndShow();
    }

    public void generatePVIndicesSeparately() {
        globalSw.reset();
        fetchCategoriesFromHDTFile();
        new File(directoryPath).mkdirs();
        createAllIndicesSeparately();
        saveClasses();
        this.timeGeneratingBTIndices = globalSw.stopAndShow();
    }

    private void generateIncomingPSOPVIndices() {

        System.out.println("Generate incoming PS-O PV indices...");
        BTIndexGenerator[] btig = initializeBTIndices(TripleComponentOrder.PSO);


        long numPreds = dic.getNpredicates();


        TripleID predTriple;
        TripleID typeSearch = new TripleID(0, rdfTypeID, 0);
        TripleID predSearch = new TripleID(0,0,0);


        for (long pred = 1; pred <= numPreds; pred++) {

            predSearch.setPredicate(pred);
            IteratorTripleID itID = HDTUtil.executeQuery(hdt, predSearch);

            while (itID.hasNext()) {
                predTriple = itID.next();
                nextObj = predTriple.getObject();

                // get type from object
                if (nextObj > dic.getNshared()) {
                    // object has no type
                    continue;
                }
                typeSearch.setSubject(nextObj);
                IteratorTripleID objTypeSearch = HDTUtil.executeQuery(hdt, typeSearch);

                while (objTypeSearch.hasNext()) {
                    // for each subject type
                    objTypeID = objTypeSearch.next().getObject();
                    objTypePos = typeIDToTypePos.getOrDefault(objTypeID, -1);
                    if (objTypePos == -1) {
                        // type not relevant
                        continue;
                    }

                    // add object to corresponding index
                    nextSbj = predTriple.getSubject();

                    currentIndex = btig[objTypePos];
                    // PS-O order
                    currentIndex.currX = pred;
                    currentIndex.currY = nextSbj;
                    currentIndex.currZ = nextObj;
                    currentIndex.addTripleToBTIndex();
                }
            }
        }

        saveBTIndices(btig, "_in");

    }

    private void generateOutgoingPOSPVIndices() {

        System.out.println("Generate outgoing PO-S PV indices...");
        BTIndexGenerator[] btig = initializeBTIndices(TripleComponentOrder.POS);

        IteratorTripleID sbjTypes;

        TripleID rdfTypeSearch = new TripleID(0, rdfTypeID, 0);
        TripleID predSearch = new TripleID(0, 0, 0);

        long numPreds = dic.getNpredicates();
        for (long pred = 1; pred <= numPreds; pred++) {
            predSearch.setPredicate(pred);
            IteratorTripleID itID = HDTUtil.executeQuery(hdt, predSearch);

            // sort objects in ascending order
            TreeSet<Long> objHs = new TreeSet<>();
            long nextObj;
            TripleID tID;
            while (itID.hasNext()) {
                tID = itID.next();
                nextObj = tID.getObject();
                // save all objects independent of their type
                objHs.add(nextObj);
            }
            itID.goToStart();

            Iterator<Long> objIt = objHs.iterator();

            TripleID predObjSearch;

            // for each sorted object
            while (objIt.hasNext()) {
                predObjSearch = new TripleID(0, pred, objIt.next());
                IteratorTripleID sbjIt = HDTUtil.executeQuery(hdt, predObjSearch);

                // add subject to corresponding BT index
                while (sbjIt.hasNext()) {

                    TripleID currentTriple = sbjIt.next();
                    rdfTypeSearch.setSubject(currentTriple.getSubject());
                    sbjTypes = HDTUtil.executeQuery(hdt, rdfTypeSearch);

                    while (sbjTypes.hasNext()) {
                        // get type and add corresponding triple to BT index
                        sbjTypeID = sbjTypes.next().getObject();
                        sbjTypePos = typeIDToTypePos.getOrDefault(sbjTypeID, -1);
                        if (sbjTypePos == -1) {
                            // type is not relevant
                            continue;
                        }

                        currentIndex = btig[sbjTypePos];

                        // PO-S index
                        currentIndex.currX = currentTriple.getPredicate();
                        currentIndex.currY = currentTriple.getObject();
                        currentIndex.currZ = currentTriple.getSubject();

                        currentIndex.addTripleToBTIndex();

                    }
                }
            }
        }

        saveBTIndices(btig, "_out");
    }

    private void saveBTIndices(BTIndexGenerator[] btig, String fileSuffix) {
        for (int i = 0; i < btig.length; i++) {
            System.out.println("Create BT index (" + (i + 1) + "/" + btig.length + ")");
            BTIndex bti = btig[i].createBTIndex();
            saveBTIndex(bti, i + fileSuffix);
        }
    }

    private BTIndexGenerator[] initializeBTIndices(TripleComponentOrder order) {
        BTIndexGenerator[] btig = new BTIndexGenerator[types.size()];
        for (int i = 0; i < types.size(); i++) {
            btig[i] = new BTIndexGenerator(hdt, order);
        }
        return btig;
    }

    public void createAllIndicesSeparately() {
        fetchCategoriesFromHDTFile();
        POSBTIndexGenerator posIndexGenerator = new POSBTIndexGenerator(hdt);
        PSOBTIndexGenerator psoIndexGenerator = new PSOBTIndexGenerator(hdt);

        // /bitmapTripleIndex
        new File(directoryPath).mkdirs();

        // get index for every class-class combination and save it to corresponding file
        for (int i = 0; i < types.size(); i++) {

            UnifiedSet<Long> hsSubject = HDTUtil.generateHashSetForIterator(typeResourcesIt.get(i),
                    TripleComponentRole.SUBJECT);

            BTIndex btIndexOut;
            BTIndex btIndexIn;

            sw.reset();
            System.out.println("Type (" + i + "/" + types.size() + ")");
            System.out.println("Start generating bitmap triples: " + types.get(i));

            btIndexOut = posIndexGenerator.createBTIndexWithHashSet(hsSubject, true);
            btIndexIn = psoIndexGenerator.createFSBTIndexWithHashSet(hsSubject, false);

            System.out.println("Created bitmap triples in time: " + sw.stopAndShow());

            System.out.println("Save to file ...");

            saveBTIndex(btIndexOut, "" + i + "_out");
            saveBTIndex(btIndexIn, "" + i + "_in");
            System.out.println("Time required for saving: " + sw.stopAndShow());

        }

        saveClasses();
    }

}
