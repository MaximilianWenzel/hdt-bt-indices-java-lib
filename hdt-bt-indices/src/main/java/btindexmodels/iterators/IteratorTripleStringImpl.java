package btindexmodels.iterators;

import java.util.ArrayList;
import java.util.Iterator;

import org.rdfhdt.hdt.enums.ResultEstimationType;
import org.rdfhdt.hdt.triples.IteratorTripleString;
import org.rdfhdt.hdt.triples.TripleString;

public class IteratorTripleStringImpl implements IteratorTripleString {

	ArrayList<TripleString> tripleStrings;
	Iterator<TripleString> iterator;
	
	public IteratorTripleStringImpl(ArrayList<TripleString> tripleStrings) {
		this.tripleStrings = tripleStrings;
		this.iterator = tripleStrings.iterator();
	}
	
	@Override
	public boolean hasNext() {
		return iterator.hasNext();
	}

	@Override
	public TripleString next() {
		return iterator.next();
	}

	@Override
	public void goToStart() {
		this.iterator = tripleStrings.iterator();
	}

	@Override
	public long estimatedNumResults() {
		return (long) tripleStrings.size();
	}

	@Override
	public ResultEstimationType numResultEstimation() {
		return ResultEstimationType.EXACT;
	}

}
