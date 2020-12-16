package btindices.indexgeneration;

import btindexmodels.BTIndex;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.rdfhdt.hdt.enums.TripleComponentOrder;
import org.rdfhdt.hdt.enums.TripleComponentRole;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.triples.IteratorTripleID;
import org.rdfhdt.hdt.triples.TripleID;

/**
 * This class provides methods in order to create a single BT Index for efficient RDF data exploration.
 *
 * @author Maximilian Wenzel
 */
public class SPOBTIndexGenerator extends BTIndexGenerator {

    public SPOBTIndexGenerator(HDT hdt) {
        super(hdt, TripleComponentOrder.SPO);
        roleX = TripleComponentRole.SUBJECT;
        roleY = TripleComponentRole.PREDICATE;
        roleZ = TripleComponentRole.OBJECT;
        order = TripleComponentOrder.SPO;
    }

    /**
     * Creates an BT index by iterating over all triples of the HDT file. If the subject component of a triple is
     * present in hsCenterType and the object component of the triple is present in the hsOutsiderType, the
     * corresponding triple is added to the BT index.
     */
    public BTIndex createBTIndexWithHashSet(UnifiedSet<Long> hsCenterCategory, UnifiedSet<Long> hsOutsiderCategory) {

        // subjects have to be retrieved in lexicographical order

        processTripleForHashSets(hsCenterCategory, hsOutsiderCategory, TripleComponentRole.SUBJECT);
        return createBTIndex();
    }

    private void processTripleForHashSets(UnifiedSet<Long> hsCenterCategory, UnifiedSet<Long> hsOutsiderCategory, TripleComponentRole role) {
        long numResources = dic.getNsubjects();

        for (long id = 1; id <= numResources; id++) {
            if (!hsCenterCategory.contains(id)) {
                continue;
            }

            IteratorTripleID itIDSbjResults = hdt.getTriples().search(new TripleID(id, 0, 0));

            while (itIDSbjResults.hasNext()) {
                TripleID currentTriple = itIDSbjResults.next();

                currX = currentTriple.getSubject();
                currY = currentTriple.getPredicate();
                currZ = currentTriple.getObject();

                // if sbj is not in center hash set -> continue
                // if obj has a pure object id (i.e. it does not occur as a subject in the data set) -> continue
                // if obj is not in outsider hash set -> continue
                if (currZ > dic.getNshared() || !hsOutsiderCategory.contains(currZ)) {
                    // triple is not relevant
                    continue;
                }

                addTripleToBTIndex();
            }
        }

    }


}
