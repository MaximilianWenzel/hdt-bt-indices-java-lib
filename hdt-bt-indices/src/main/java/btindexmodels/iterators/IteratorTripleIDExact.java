package btindexmodels.iterators;

import org.rdfhdt.hdt.enums.ResultEstimationType;
import org.rdfhdt.hdt.enums.TripleComponentOrder;
import org.rdfhdt.hdt.triples.IteratorTripleID;
import org.rdfhdt.hdt.triples.TripleID;

import java.util.ArrayList;

/**
 * Triple ID iterator which provides exact results for the cardinality estimation, because it uses
 * and array list internally.
 */
public class IteratorTripleIDExact implements IteratorTripleID {

    ArrayList<TripleID> tIDList;
    int currentIndex;

    public IteratorTripleIDExact() {
        currentIndex = 0;
        this.tIDList = new ArrayList<>();
    }

    public IteratorTripleIDExact(ArrayList<TripleID> tIDList) {
        currentIndex = 0;
        this.tIDList = tIDList;
    }

    @Override
    public boolean hasPrevious() {
        return currentIndex > 0;
    }

    @Override
    public TripleID previous() {
        return tIDList.get(currentIndex);
    }

    @Override
    public void goToStart() {
        currentIndex = 0;
    }

    @Override
    public boolean canGoTo() {
        return true;
    }

    @Override
    public void goTo(long pos) {
        currentIndex = (int) pos;
    }

    @Override
    public long estimatedNumResults() {
        return tIDList.size();
    }

    @Override
    public ResultEstimationType numResultEstimation() {
        return ResultEstimationType.EXACT;
    }

    @Override
    public TripleComponentOrder getOrder() {
        return TripleComponentOrder.SPO;
    }

    @Override
    public boolean hasNext() {
        return currentIndex < tIDList.size();
    }

    @Override
    public TripleID next() {
        return tIDList.get(currentIndex++);
    }
}
