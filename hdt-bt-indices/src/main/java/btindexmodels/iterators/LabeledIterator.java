package btindexmodels.iterators;

import java.net.URI;

import org.rdfhdt.hdt.triples.IteratorTripleString;

/**
 * Represents a data structure in order to store an iterator with a
 * corresponding URI.
 * 
 * @author Maximilian Wenzel
 *
 */
public class LabeledIterator {

	protected IteratorTripleString itString;
	protected URI uri;

	public LabeledIterator() {

	}

	public LabeledIterator(IteratorTripleString itString, URI uri) {

		this.itString = itString;
		this.uri = uri;
	}

	public IteratorTripleString getItString() {
		return itString;
	}

	public void setItString(IteratorTripleString itString) {
		this.itString = itString;
	}

	public URI getUri() {
		return uri;
	}

	public void setUri(URI uri) {
		this.uri = uri;
	}
}
