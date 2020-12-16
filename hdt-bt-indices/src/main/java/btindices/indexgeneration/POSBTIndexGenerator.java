package btindices.indexgeneration;

import btindexmodels.BTIndex;
import btindices.HDTUtil;
import org.apache.jena.rdf.model.RDFNode;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.rdfhdt.hdt.dictionary.Dictionary;
import org.rdfhdt.hdt.enums.TripleComponentOrder;
import org.rdfhdt.hdt.enums.TripleComponentRole;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.triples.IteratorTripleID;
import org.rdfhdt.hdt.triples.TripleID;

import java.util.Iterator;
import java.util.TreeSet;

/**
 * This class provides methods in order to create a single PO-S BT Index for efficient RDF data exploration.
 *
 * @author Maximilian Wenzel
 */
public class POSBTIndexGenerator extends BTIndexGenerator {

    public POSBTIndexGenerator(HDT hdt) {
        super(hdt, TripleComponentOrder.POS);
        // PO-S BT index
        roleX = TripleComponentRole.PREDICATE;
        roleY = TripleComponentRole.OBJECT;
        roleZ = TripleComponentRole.SUBJECT;
        order = TripleComponentOrder.POS;
    }

    /**
     * Converts an object to the correct HDT dictionary id if it is a literal.
     */
    private long convertLiteralToID(RDFNode objNode, Dictionary dic) {
        long objID = dic.stringToId(objNode.toString(), TripleComponentRole.OBJECT);

        if (objID == -1) {
            // object is a literal
            String literal = "\"" + objNode.toString() + "\"";
            objID = dic.stringToId(literal, TripleComponentRole.OBJECT);

            if (objID == -1) {
                // '\"' backslashes not in dictionary -> replace '\"' with '"'
                literal = literal.replace("\\\"", "\"");
                objID = dic.stringToId(literal, TripleComponentRole.OBJECT);

                if (objID == -1) {
                    // dictionary contains datatype
                    literal = literal + "^^" + objNode.asLiteral().getDatatypeURI();
                    objID = dic.stringToId(literal, TripleComponentRole.OBJECT);
                }
            }
        }
        return objID;
    }

    /**
     * Creates an BT index by iterating over all triples of the HDT file. If the predicate component of a triple is
     * present in hsPredicate and the object component of the triple is present in the hsSubject, the corresponding
     * triple is added to the BT index.
     */
    public BTIndex createBTIndexWithHashSet(UnifiedSet<Long> hsSubject, boolean outgoing) {

        initVariables();
        processTripleForHashSets(hsSubject, outgoing);

        return createBTIndex();
    }

    /**
     * Adds all triples to the PO-S BT index if the subject or object (in relation to in- or outgoing connections)
     * component is present in hsSubject.
     * If outgoing is true, PO-S index contains:
     * SELECT * {
     *     ?s rdf:type type_i .
     *     ?s ?p ?o .
     * }
     * If outgoing is false, PO-S index contains:
     * SELECT * {
     *     ?o rdf:type type_i .
     *     ?s ?p ?o .
     * }
     */
    private void processTripleForHashSets(UnifiedSet<Long> hsSubject, boolean outgoing) {
        long numPreds = dic.getNpredicates();

        for (long pred = 1; pred <= numPreds; pred++) {

            IteratorTripleID itID = HDTUtil.executeQuery(hdt, new TripleID(0, pred, 0));

            // sort objects in ascending order
            TreeSet<Long> objHs = new TreeSet<>();
            long nextObj;
            TripleID tID;
            while (itID.hasNext()) {
                tID = itID.next();
                nextObj = tID.getObject();
                if (!outgoing && nextObj <= dic.getNshared()) {
                    // save all objects which are of the respective rdf:type -> consider only shared resources because
                    // type annotated resources are all subjects
                    if (hsSubject.contains(nextObj))
                        objHs.add(nextObj);
                } else if (outgoing) {
                    // save all objects independent of their type if subject component is contained in hash set
                    if (hsSubject.contains(tID.getSubject())) {
                        objHs.add(nextObj);
                    }
                }
            }
            itID.goToStart();

            Iterator<Long> objIt = objHs.iterator();

            TripleID predObjSearch;

            while (objIt.hasNext()) {
                predObjSearch = new TripleID(0, pred, objIt.next());
                IteratorTripleID sbjIt = HDTUtil.executeQuery(hdt, predObjSearch);

                while (sbjIt.hasNext()) {

                    TripleID currentTriple = sbjIt.next();

                    // if sbj is not in subject hash set -> continue
                    if (outgoing && !hsSubject.contains(currentTriple.getSubject())) {
                        // triple is not relevant
                        continue;
                    }

                    currX = currentTriple.getPredicate();
                    currY = currentTriple.getObject();
                    currZ = currentTriple.getSubject();


                    addTripleToBTIndex();
                }
            }
        }

    }



}
