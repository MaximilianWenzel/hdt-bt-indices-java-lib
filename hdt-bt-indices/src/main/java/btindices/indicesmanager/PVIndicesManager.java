package btindices.indicesmanager;

import btindexmodels.BTIndex;
import btindexmodels.categoryexplorationmodels.CatConnection;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTVocabulary;
import org.rdfhdt.hdt.options.ControlInfo;
import org.rdfhdt.hdt.options.ControlInformation;
import org.rdfhdt.hdt.triples.IteratorTripleID;
import org.rdfhdt.hdt.triples.TripleID;
import org.rdfhdt.hdt.util.io.CountInputStream;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;

public class PVIndicesManager extends BTIndicesManager {

    public HashMap<String, Integer> classToPosition;

    /**
     * PO-S indices
     */
    BTIndex[] btIndicesOut;

    /**
     * PS-O indices
     */
    BTIndex[] btIndicesIn;

    public PVIndicesManager(HDT hdt, String directoryPath) {
        // initialize necessary variables
        ci = new ControlInformation();
        ci.clear();
        ci.setType(ControlInfo.Type.TRIPLES);
        ci.setFormat(HDTVocabulary.TRIPLES_TYPE_BITMAP);

        this.directoryPath = directoryPath;

        this.hdt = hdt;
        this.dic = hdt.getDictionary();

        // read all index files from the specified directory into main memory
        typeURIs = readClassURIs(directoryPath + "/rdfClassURIs");
        readBTIndices();

        // create "class to position" converter
        classToPosition = new HashMap<>();
        for (int i = 0; i < typeURIs.length; i++) {
            classToPosition.put(typeURIs[i].toString(), i);
        }
    }

    /**
     * Reads all files of the specified BTIndices directory into the data structures of this class.
     */
    private void readBTIndices() {

        String filePath;
        File f;
        btIndicesOut = new BTIndex[typeURIs.length];
        btIndicesIn = new BTIndex[typeURIs.length];

        for (int i = 0; i < typeURIs.length; i++) {
            // generate file path
            // name convention: uriClassPosition_<IN/OUT>
            btIndicesOut[i] = readSingleBTIndex("" + i + "_out");
            btIndicesIn[i] = readSingleBTIndex("" + i + "_in");
        }
    }

    private BTIndex readSingleBTIndex(String fileName) {
        File f = new File(this.directoryPath, fileName);

        try (InputStream input = new CountInputStream(new BufferedInputStream(new FileInputStream(f)))) {

            BTIndex btIndex = new BTIndex();
            ci.clear();
            ci.load(input);
            btIndex.load(input, ci, null);
            return btIndex;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public IteratorTripleID queryBTIndex(TripleID tID, String rdfClass, CatConnection con) {

        int rdfClassPos = classToPosition.get(rdfClass);
        return queryBTIndex(tID, rdfClassPos, con);
    }

    public IteratorTripleID queryBTIndex(TripleID tID, int rdfClassPos, CatConnection con) {

        if (con.equals(CatConnection.OUT)) {
            return btIndicesOut[rdfClassPos].search(tID);
        } else {
            return btIndicesIn[rdfClassPos].search(tID);
        }
    }

}
