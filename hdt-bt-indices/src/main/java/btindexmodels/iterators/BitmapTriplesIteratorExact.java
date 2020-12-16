package btindexmodels.iterators;

import org.rdfhdt.hdt.enums.ResultEstimationType;
import org.rdfhdt.hdt.enums.TripleComponentOrder;
import org.rdfhdt.hdt.triples.IteratorTripleID;
import org.rdfhdt.hdt.triples.TripleID;

public class BitmapTriplesIteratorExact implements IteratorTripleID {

    long numResults;
    IteratorTripleID itID;

    public BitmapTriplesIteratorExact(IteratorTripleID _itID) {
        numResults = 0;
        this.itID = _itID;

        while(itID.hasNext()) {
            itID.next();
            numResults++;
        }
        itID.goToStart();
    }

    @Override
    public boolean hasPrevious() {
        return itID.hasPrevious();
    }

    @Override
    public TripleID previous() {
        return itID.previous();
    }

    @Override
    public void goToStart() {
        itID.goToStart();
    }

    @Override
    public boolean canGoTo() {
        return itID.canGoTo();
    }

    @Override
    public void goTo(long pos) {
        itID.goTo(pos);
    }

    @Override
    public long estimatedNumResults() {
        return numResults;
    }

    @Override
    public ResultEstimationType numResultEstimation() {
        return ResultEstimationType.EXACT;
    }

    @Override
    public TripleComponentOrder getOrder() {
        return itID.getOrder();
    }

    @Override
    public boolean hasNext() {
        return itID.hasNext();
    }

    @Override
    public TripleID next() {
        return itID.next();
    }
}
