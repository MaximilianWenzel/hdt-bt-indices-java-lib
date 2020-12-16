package btindices.indexgeneration;

import btindexmodels.BTIndex;
import org.rdfhdt.hdt.compact.bitmap.Bitmap375;
import org.rdfhdt.hdt.compact.sequence.SequenceLog64;
import org.rdfhdt.hdt.dictionary.Dictionary;
import org.rdfhdt.hdt.enums.TripleComponentOrder;
import org.rdfhdt.hdt.enums.TripleComponentRole;
import org.rdfhdt.hdt.hdt.HDT;

/**
 * This class provides methods in order to create a single BT Index for efficient RDF data exploration.
 *
 * @author Maximilian Wenzel
 */
public class BTIndexGenerator {

    SequenceLog64 seqX;
    SequenceLog64 seqY;
    SequenceLog64 seqZ;

    Bitmap375 bitmapY;
    Bitmap375 bitmapZ;

    long prevX;
    long prevY;
    long prevZ;

    long currX;
    long currY;
    long currZ;

    // indicates the role of X, Y and Z in the BT index
    TripleComponentRole roleX;
    TripleComponentRole roleY;
    TripleComponentRole roleZ;

    TripleComponentOrder order;

    /**
     * The actual class representation of an HDT file.
     */
    HDT hdt;
    /**
     * The dictionary of the HDT file which has been loaded.
     */
    Dictionary dic;
    long numTriples;

    public BTIndexGenerator(HDT hdt, TripleComponentOrder order) {
        this.hdt = hdt;
        dic = hdt.getDictionary();
        this.order = order;
        initVariables();
    }

    void initVariables() {
        seqX = new SequenceLog64(63);
        seqY = new SequenceLog64(63);
        seqZ = new SequenceLog64(63);

        bitmapY = new Bitmap375();
        bitmapZ = new Bitmap375();

        prevX = 0;
        prevY = 0;
        prevZ = 0;

        currX = 0;
        currY = 0;
        currZ = 0;

        numTriples = 0;
    }


    /**
     * Adds the values of "currSbj", "currPred" and "currObj" to the BT index which is currently generated.
     */
    void addTripleToBTIndex() {
        if (numTriples == 0) {
            // first triple
            seqX.append(currX);
            seqY.append(currY);
            seqZ.append(currZ);
        } else if (currX != prevX) {

            if (prevX > currX) {
                notSortedException();
            }
            seqX.append(currX);
            bitmapY.append(true);
            seqY.append(currY);
            bitmapZ.append(true);
            seqZ.append(currZ);

        } else if (currY != prevY) {

            if (prevY > currY) {
                notSortedException();
            }
            bitmapY.append(false);
            seqY.append(currY);
            bitmapZ.append(true);
            seqZ.append(currZ);

        } else if (currZ != prevZ) { // eliminate duplicates

            if (prevZ > currZ) {
                notSortedException();
            }
            bitmapZ.append(false);
            seqZ.append(currZ);
        }


        prevX = currX;
        prevY = currY;
        prevZ = currZ;

        numTriples++;
    }

    public BTIndex createBTIndex() {
        if (numTriples > 0) {

            bitmapY.append(true);
            bitmapZ.append(true);
        }

        seqX.aggressiveTrimToSize();
        seqY.aggressiveTrimToSize();
        seqZ.aggressiveTrimToSize();

        BTIndex bti = new BTIndex(seqX, seqY, seqZ, bitmapY, bitmapZ, order);

        return bti;
    }

    private void notSortedException() {
        try {
            throw new Exception("Resources not sorted in ascending order for BT index generation!");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Last: " + prevX + " " + prevY + " " + prevZ);
            System.out.println("Current: " + currX + " " + currY + " " + currZ);
            System.exit(1);
        }
    }

}
