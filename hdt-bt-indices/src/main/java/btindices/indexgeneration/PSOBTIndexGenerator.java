package btindices.indexgeneration;

import btindexmodels.BTIndex;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.rdfhdt.hdt.enums.TripleComponentOrder;
import org.rdfhdt.hdt.enums.TripleComponentRole;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.triples.IteratorTripleID;
import org.rdfhdt.hdt.triples.TripleID;

import java.util.Iterator;
import java.util.TreeSet;

/**
 * This class provides methods in order to create a single PS-O BT Index for efficient RDF data exploration.
 *
 * @author Maximilian Wenzel
 */
public class PSOBTIndexGenerator extends BTIndexGenerator {

    public PSOBTIndexGenerator(HDT hdt) {
        super(hdt, TripleComponentOrder.PSO);
        // PS-O BT index
        roleX = TripleComponentRole.PREDICATE;
        roleY = TripleComponentRole.SUBJECT;
        roleZ = TripleComponentRole.OBJECT;
        order = TripleComponentOrder.PSO;
    }

    /**
     * Adds all triples to the PS-O BT index if the subject or object (in relation to in- or outgoing connections)
     * component is present in hsSubject. If outgoing is true, PS-O index contains: SELECT * { ?s rdf:type type_i . ?s
     * ?p ?o . } If outgoing is false, PS-O index contains: SELECT * { ?o rdf:type type_i . ?s ?p ?o . }
     */
    public BTIndex createFSBTIndexWithHashSet(UnifiedSet<Long> hsSubject, boolean outgoing) {
        initVariables();

        long numResources = dic.getNpredicates();

        for (long id = 1; id <= numResources; id++) {

            IteratorTripleID itID = hdt.getTriples().search(new TripleID(0, id, 0));

            // sort objects in ascending order
            TreeSet<Long> sbjHs = new TreeSet<>();
            long nextSbj;
            long nextObj;
            TripleID tID;
            while (itID.hasNext()) {
                tID = itID.next();

                if (!outgoing) {
                    nextObj = tID.getObject();
                    if (nextObj <= dic.getNshared()) {
                        // save all objects which are of the respective rdf:type -> consider only shared resources because
                        // type annotated resources are all subjects
                        if (hsSubject.contains(nextObj)) {
                            sbjHs.add(nextObj);
                        }
                    }
                } else if (outgoing) {
                    nextSbj = tID.getSubject();
                    // save all objects independent of their type if subject component is contained in hash set
                    if (hsSubject.contains(nextSbj)) {
                        sbjHs.add(nextSbj);
                    }
                }
            }
            itID.goToStart();

            Iterator<Long> sbjIt = sbjHs.iterator();

            while (sbjIt.hasNext()) {
                IteratorTripleID objIt = hdt.getTriples().search(new TripleID(sbjIt.next(), id, 0));

                while (objIt.hasNext()) {

                    TripleID currentTriple = objIt.next();

                    currX = currentTriple.getPredicate();
                    currY = currentTriple.getSubject();
                    currZ = currentTriple.getObject();

                    addTripleToBTIndex();
                }
            }
        }

        return createBTIndex();
    }


    /**
     * Creates an BT index by iterating over all triples of the HDT file. If the subject component of a triple is
     * present in hsCenterType and the object component of the triple is present in the hsOutsiderType, the
     * corresponding triple is added to the BT index.
     */
    public BTIndex createCatBTIndexWithHashSet(UnifiedSet<Long> hsSubject, UnifiedSet<Long> hsObject) {

        long numPreds = dic.getNpredicates();

        for (long pred = 1; pred <= numPreds; pred++) {

            IteratorTripleID itID = hdt.getTriples().search(new TripleID(0, pred, 0));

            // sort objects in ascending order
            TreeSet<Long> sbjHs = new TreeSet<>();
            long nextSbj;
            TripleID tID;
            while (itID.hasNext()) {
                tID = itID.next();
                nextSbj = tID.getSubject();
                if (hsSubject.contains(nextSbj)) {
                    sbjHs.add(nextSbj);
                }
            }
            itID.goToStart();

            Iterator<Long> sbjIt = sbjHs.iterator();

            while (sbjIt.hasNext()) {
                IteratorTripleID objIt = hdt.getTriples().search(new TripleID(sbjIt.next(), pred, 0));

                while (objIt.hasNext()) {

                    TripleID currentTriple = objIt.next();
                    if (!hsObject.contains(currentTriple.getObject())) {
                        continue;
                    }

                    currX = currentTriple.getPredicate();
                    currY = currentTriple.getSubject();
                    currZ = currentTriple.getObject();

                    addTripleToBTIndex();
                }
            }
        }

        return createBTIndex();
    }

}
