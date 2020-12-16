package btindexmodels;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import btindexmodels.iterators.BTIndexPOSandPSOIterator;
import btindexmodels.iterators.IteratorTripleIDExact;
import org.rdfhdt.hdt.compact.bitmap.AdjacencyList;
import org.rdfhdt.hdt.compact.bitmap.Bitmap;
import org.rdfhdt.hdt.compact.bitmap.BitmapFactory;
import org.rdfhdt.hdt.compact.sequence.Sequence;
import org.rdfhdt.hdt.compact.sequence.SequenceFactory;
import org.rdfhdt.hdt.compact.sequence.SequenceLog64;
import org.rdfhdt.hdt.enums.TripleComponentOrder;
import org.rdfhdt.hdt.enums.TripleComponentRole;
import org.rdfhdt.hdt.exceptions.IllegalFormatException;
import org.rdfhdt.hdt.exceptions.NotFoundException;
import org.rdfhdt.hdt.listener.ProgressListener;
import org.rdfhdt.hdt.options.ControlInfo;
import org.rdfhdt.hdt.triples.IteratorTripleID;
import org.rdfhdt.hdt.triples.TripleID;
import org.rdfhdt.hdt.triples.impl.BitmapTriples;
import org.rdfhdt.hdt.util.listener.IntermediateListener;

import btindexmodels.iterators.BTIndexSPOIterator;

/**
 * This class represents an extension of the BitmapTriples class in order to
 * create a type_i-type_j index for two RDF types of a given HDT file. The only
 * difference to the original BitmapTriples class is that an additional id
 * sequence for the subjects is saved in the BT Index class to retrieve the
 * correct subject ID from the original HDT dictionary.
 * 
 * @author Maximilian Wenzel
 *
 */
public class BTIndex extends BitmapTriples {

	/**
	 * Role of the resource at subject position. In case of an PO-S index, it is a predicate.
	 */
	private TripleComponentRole subjectPosRole;
	private SequenceLog64 seqX;

	public BTIndex() {
		super();
	}

	public BTIndex(SequenceLog64 seqX, Sequence seqY, Sequence seqZ, Bitmap bitY, Bitmap bitZ,
			TripleComponentOrder order) {
		this.seqX = seqX;
		this.seqY = seqY;
		this.seqZ = seqZ;
		this.bitmapY = bitY;
		this.bitmapZ = bitZ;
		this.order = order;

		switch (order) {
			case SPO:
				subjectPosRole = TripleComponentRole.SUBJECT;
			case POS:
				subjectPosRole = TripleComponentRole.PREDICATE;
			case PSO:
				subjectPosRole = TripleComponentRole.PREDICATE;
		}

		adjY = new AdjacencyList(seqY, bitmapY);
		adjZ = new AdjacencyList(seqZ, bitmapZ);
	}

	/**
	 * Searches a given triple id pattern in the BitmapTriples. All triple search
	 * combinations are ready to be used, e.g. SPO, ?PO, ?P? etc.
	 */
	@Override
	public IteratorTripleID search(TripleID pattern) {

		TripleID searchPattern = new TripleID();
		searchPattern.assign(pattern);

		switch (order) {

			case POS:
			case PSO:
				// POS or PSO index
				// get the new index of the predicate
				if (searchPattern.getPredicate() != 0) {
					long predID = searchPattern.getPredicate();
					long newPredID = 0;

					try {
						newPredID = seqX.binSearchExact(predID, 0, seqX.getNumberOfElements() - 1);
					} catch (NotFoundException e) {

						// subject not present in the bt index -> return empty iterator
						return new BTIndexPOSandPSOIterator(seqX, new IteratorTripleIDExact());
					}

					searchPattern.setPredicate(newPredID + 1);
				}
				break;

			case SPO:
				// SPO index
				// get the new index of the subject
				if (searchPattern.getSubject() != 0) {
					long sbjID = searchPattern.getSubject();
					long newSbjID = 0;

					try {
						newSbjID = seqX.binSearchExact(sbjID, 0, seqX.getNumberOfElements() - 1);
					} catch (NotFoundException e) {

						// subject not present in the bt index -> return empty iterator
						return new BTIndexSPOIterator(seqX, new IteratorTripleIDExact());
					}

					searchPattern.setSubject(newSbjID + 1);
				}
				break;

		}

		IteratorTripleID itID = super.search(searchPattern);

		switch (order) {
			case SPO:
				return new BTIndexSPOIterator(seqX, itID);
			default:
				// PSO and POS iterator
				return new BTIndexPOSandPSOIterator(seqX, itID);
		}

		/*
		 * ArrayList<TripleID> convertedTriples = new ArrayList<TripleID>();
		 *
		 * while (itID.hasNext()) { TripleID current = itID.next();
		 * current.setSubject(seqX.get(current.getSubject() - 1));
		 * convertedTriples.add(new TripleID(current.getSubject(),
		 * current.getPredicate(), current.getObject())); }
		 *
		 * itID.goToStart();
		 *
		 * return new ListTripleIDIterator(convertedTriples);
		 */
	}

	/**
	 * Saves the BitmapTriples with the corresponding subject sequence to a
	 * specified file location.
	 */
	@Override
	public void save(OutputStream output, ControlInfo ci, ProgressListener listener) throws IOException {
		ci.clear();
		ci.setFormat(getType());
		ci.setInt("order", order.ordinal());
		ci.setType(ControlInfo.Type.TRIPLES);
		ci.save(output);

		IntermediateListener iListener = new IntermediateListener(listener);
		bitmapY.save(output, iListener);
		bitmapZ.save(output, iListener);
		seqX.save(output, iListener);
		seqY.save(output, iListener);
		seqZ.save(output, iListener);
	}

	/**
	 * Loads the BitmapTriples with the corresponding subject sequence from a
	 * specified file location.
	 */
	@Override
	public void load(InputStream input, ControlInfo ci, ProgressListener listener) throws IOException {

		if (ci.getType() != ControlInfo.Type.TRIPLES) {
			throw new IllegalFormatException("Trying to read a triples section, but was not triples.");
		}

		if (!ci.getFormat().equals(getType())) {
			throw new IllegalFormatException(
					"Trying to read BitmapTriples, but the data does not seem to be BitmapTriples");
		}

		order = TripleComponentOrder.values()[(int) ci.getInt("order")];

		IntermediateListener iListener = new IntermediateListener(listener);

		bitmapY = BitmapFactory.createBitmap(input);
		bitmapY.load(input, iListener);

		bitmapZ = BitmapFactory.createBitmap(input);
		bitmapZ.load(input, iListener);

		seqX = (SequenceLog64) SequenceFactory.createStream(input);
		seqX.load(input, iListener);

		seqY = SequenceFactory.createStream(input);
		seqY.load(input, iListener);

		seqZ = SequenceFactory.createStream(input);
		seqZ.load(input, iListener);

		adjY = new AdjacencyList(seqY, bitmapY);
		adjZ = new AdjacencyList(seqZ, bitmapZ);
	}
}
