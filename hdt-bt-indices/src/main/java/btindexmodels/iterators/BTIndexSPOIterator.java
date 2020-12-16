package btindexmodels.iterators;

import org.rdfhdt.hdt.compact.sequence.SequenceLog64;
import org.rdfhdt.hdt.enums.ResultEstimationType;
import org.rdfhdt.hdt.enums.TripleComponentOrder;
import org.rdfhdt.hdt.triples.IteratorTripleID;
import org.rdfhdt.hdt.triples.TripleID;

public class BTIndexSPOIterator implements IteratorTripleID {

	
	private SequenceLog64 seqX;
	private IteratorTripleID itID;
	
	public BTIndexSPOIterator(IteratorTripleID itID) {
		this.itID = itID;
		this.seqX = null;
	}
	
	public BTIndexSPOIterator(SequenceLog64 seqX, IteratorTripleID itID) {
		this.seqX = seqX;
		this.itID = itID;
	}
	
	@Override
	public boolean hasNext() {
		return itID.hasNext();
	}

	@Override
	public TripleID next() {
		if (seqX == null) {
			return itID.next();
		}
		
		TripleID nextTriple = itID.next();
		nextTriple.setSubject(seqX.get(nextTriple.getSubject() - 1));
		return nextTriple;
	}

	@Override
	public boolean hasPrevious() {
		
		itID.hasPrevious();
		return false;
	}

	@Override
	public TripleID previous() {
		if (seqX == null) {
			return itID.previous();
		}
		
		TripleID previousTriple = itID.previous();
		previousTriple.setSubject(seqX.get(previousTriple.getSubject() - 1));
		return previousTriple;
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
		return itID.estimatedNumResults();
	}

	@Override
	public ResultEstimationType numResultEstimation() {
		return itID.numResultEstimation();
	}

	@Override
	public TripleComponentOrder getOrder() {
		return itID.getOrder();
	}

}
