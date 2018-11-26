package org.corpus_tools.peppermodules.alignment;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.corpus_tools.pepper.common.DOCUMENT_STATUS;
import org.corpus_tools.pepper.common.PepperConfiguration;
import org.corpus_tools.pepper.impl.PepperManipulatorImpl;
import org.corpus_tools.pepper.impl.PepperMapperImpl;
import org.corpus_tools.pepper.modules.PepperManipulator;
import org.corpus_tools.pepper.modules.PepperMapper;
import org.corpus_tools.pepper.modules.PepperModule;
import org.corpus_tools.pepper.modules.PepperModuleProperties;
import org.corpus_tools.pepper.modules.exceptions.PepperModuleNotReadyException;
import org.corpus_tools.salt.SALT_TYPE;
import org.corpus_tools.salt.SaltFactory;
import org.corpus_tools.salt.common.SCorpus;
import org.corpus_tools.salt.common.SCorpusGraph;
import org.corpus_tools.salt.common.SDocument;
import org.corpus_tools.salt.common.SDocumentGraph;
import org.corpus_tools.salt.common.SSpan;
import org.corpus_tools.salt.common.STextualDS;
import org.corpus_tools.salt.common.SToken;
import org.corpus_tools.salt.core.GraphTraverseHandler;
import org.corpus_tools.salt.core.SAnnotation;
import org.corpus_tools.salt.core.SGraph.GRAPH_TRAVERSE_TYPE;
import org.corpus_tools.salt.core.SLayer;
import org.corpus_tools.salt.core.SNode;
import org.corpus_tools.salt.core.SRelation;
import org.corpus_tools.salt.graph.Identifier;
import org.corpus_tools.salt.graph.Label;
import org.eclipse.emf.common.util.URI;
import org.osgi.service.component.annotations.Component;

/**
 * This is a dummy implementation to show how a {@link PepperManipulator} works.
 * Therefore it just prints out some information about a corpus like the number
 * of nodes, edges and for instance annotation frequencies. <br/>
 * This class can be used as a template for an own implementation of a
 * {@link PepperManipulator} Take a look at the TODO's and adapt the code.
 * If this is the first time, you are implementing a Pepper module, we strongly
 * recommend, to take a look into the 'Developer's Guide for Pepper modules',
 * you will find on
 * <a href="http://corpus-tools.org/pepper/">http://corpus-tools.org/pepper</a>.
 * 
 * @author Martin Klotz
 */
@Component(name = "AlignmentManipulatorComponent", factory = "PepperManipulatorComponentFactory")
public class Aligner extends PepperManipulatorImpl {
	public static final String ALIGNMENT_NAME = "align";
	private static final String LAYER_NAME_NEW = "NEW";
	// =================================================== mandatory
	// ===================================================
	/**
	 * <strong>OVERRIDE THIS METHOD FOR CUSTOMIZATION</strong> <br/>
	 * A constructor for your module. Set the coordinates, with which your
	 * module shall be registered. The coordinates (modules name, version and
	 * supported formats) are a kind of a fingerprint, which should make your
	 * module unique.
	 */
	public Aligner() {
		super();
		setName("AlignmentManipulator");
		// TODO change suppliers e-mail address
		setSupplierContact(URI.createURI(PepperConfiguration.EMAIL));
		// TODO change suppliers homepage
		setSupplierHomepage(URI.createURI(PepperConfiguration.HOMEPAGE));
		// TODO add a description of what your module is supposed to do
		setDesc("The manipulator, traverses over the document-structure and prints out some information about it, like the frequencies of annotations, the number of nodes and edges and so on. ");
	}
	
	@Override
	public void start() {
		if (getSaltProject() == null || getSaltProject().getCorpusGraphs().isEmpty()) {
			// instead of handling the exception just pass on to super implementation, that takes care of the exception or provides an alternative plan  FIXME?
			super.start();
		}		
		mergeDocuments();
		super.start();
	}
	
	/**
	 * FIXME this method is public since testing this is impossible without a document controller being set in advance ... look into this
	 */
	public void mergeDocuments() {
		SCorpusGraph corpusGraph = getSaltProject().getCorpusGraphs().get(0);
		List<SDocument> documentNames = corpusGraph.getDocuments();
		Set<String> used = new HashSet<>();
		Set<Pair<SDocument, SDocument>> documentPairs = new HashSet<>(); 
		for (SDocument doc : documentNames) {
			String name = doc.getName();
			if (!used.contains(name)) {
				for (SDocument other_doc : documentNames) {
					String other_name = other_doc.getName();
					if (!other_name.equals(name) && !used.contains(other_name) && ((other_name.startsWith(name)) || name.startsWith(other_name))) {
						used.add(name);
						used.add(other_name);
						documentPairs.add(Pair.of(doc, other_doc));
					}
				}
			}
		}
		if (used.size() != documentNames.size()) {
			// throw exception
		}
		if (documentPairs.isEmpty()) {
			return;
		}
		List<SDocument> removeDocuments = new ArrayList<>();
		for (Pair<SDocument, SDocument> pair : documentPairs) {
			SDocument targetDocument = pair.getLeft();
			SDocument sourceDocument = pair.getRight();
			removeDocuments.add(sourceDocument);
			SDocumentGraph targetGraph = targetDocument.getDocumentGraph();
			SLayer layerNew = SaltFactory.createSLayer();
			layerNew.setName(LAYER_NAME_NEW);
			layerNew.setGraph(targetGraph);
			SDocumentGraph sourceGraph = sourceDocument.getDocumentGraph();
			List<SNode> nodes = new ArrayList<>();
			sourceGraph.getNodes().stream().forEach(nodes::add);
			List<SRelation<SNode, SNode>> relations = new ArrayList<>();
			sourceGraph.getRelations().stream().forEach(relations::add);
			List<Label> labels = new ArrayList<>();
			sourceGraph.getAnnotations().stream().forEach(labels::add);
			nodes.stream().forEach(targetGraph::addNode);
			nodes.stream().forEach(layerNew::addNode);
			relations.stream().forEach(targetGraph::addRelation);
			labels.stream().forEach(targetGraph::addLabel);			
		}
		removeDocuments.stream().forEach(corpusGraph::removeNode);
	}

	/**
	 * <strong>OVERRIDE THIS METHOD FOR CUSTOMIZATION</strong> <br/>
	 * This method creates a customized {@link PepperMapper} object and returns
	 * it. You can here do some additional initialisations. Thinks like setting
	 * the {@link Identifier} of the {@link SDocument} or {@link SCorpus} object
	 * and the {@link URI} resource is done by the framework (or more in detail
	 * in method {@link #start()}). The parameter <code>Identifier</code>, if a
	 * {@link PepperMapper} object should be created in case of the object to
	 * map is either an {@link SDocument} object or an {@link SCorpus} object of
	 * the mapper should be initialized differently. <br/>
	 * 
	 * @param Identifier
	 *            {@link Identifier} of the {@link SCorpus} or {@link SDocument}
	 *            to be processed.
	 * @return {@link PepperMapper} object to do the mapping task for object
	 *         connected to given {@link Identifier}
	 */
	public PepperMapper createPepperMapper(Identifier Identifier) {
		AlignmentMapper mapper = new AlignmentMapper();
		mapper.setDocument( getSaltProject().getCorpusGraphs().get(0).getDocument(Identifier) );
		return (mapper);
	}

	/**
	 * This class is a dummy implementation for a mapper, to show how it works.
	 * Pepper or more specific this dummy implementation of a Pepper module
	 * creates one mapper object per {@link SDocument} object and
	 * {@link SCorpus} object each. This ensures, that each of those objects is
	 * run independently from another and runs parallelized. <br/>
	 * The method {@link #mapSCorpus()} is supposed to handle all
	 * {@link SCorpus} object and the method {@link #mapSDocument()} is supposed
	 * to handle all {@link SDocument} objects. <br/>
	 * In our dummy implementation, we just print out some information about a
	 * corpus to system.out. This is not very useful, but might be a good
	 * starting point to explain how access the several objects in Salt model.
	 */
	public class AlignmentMapper extends PepperMapperImpl {
		
		/**
		 * prints out some information about document-structure
		 */
		@Override
		public DOCUMENT_STATUS mapSDocument() {
			SDocumentGraph graph = getDocument().getDocumentGraph();
			AlignerProperties properties = (AlignerProperties) Aligner.this.getProperties();
			int[][][] alignment = properties.getAlignmentData();
			String sentenceName = properties.getSentenceName();
			int sOffset = properties.getSmallestSentenceValue();
			List<SSpan> spans = graph.getSpans();
			SSpan[][] spanPairs = new SSpan[alignment.length][2];
			STextualDS[] dataSources = {graph.getTextualDSs().get(0), graph.getTextualDSs().get(1)};
			SLayer probe = graph.getLayerByName(LAYER_NAME_NEW).get(0);
			for (SSpan span : spans) {
				SAnnotation sentenceAnno = span.getAnnotation(sentenceName); 
				if (sentenceAnno != null) {
					int sentenceIx = Integer.parseInt( sentenceAnno.getValue_STEXT() ) - sOffset;
					int writeIx = 0;
					if (span.getLayers().contains(probe)) {
						writeIx++;
					}
					spanPairs[sentenceIx][writeIx] = span;
				}
			}
			for (int i=0; i < alignment.length; i++) {
				List<SToken> sourceTokens = graph.getSortedTokenByText( graph.getOverlappedTokens(spanPairs[i][0]) );
				List<SToken> targetTokens = graph.getSortedTokenByText( graph.getOverlappedTokens(spanPairs[i][1]) );
				for (int[] alignPair : alignment[i]) {
					graph.createRelation(sourceTokens.get(alignPair[0]), targetTokens.get(alignPair[1]), SALT_TYPE.SPOINTING_RELATION, null).setType(ALIGNMENT_NAME);
				}
			}
			graph.getNodes().stream().forEach((SNode n) -> n.removeLayer(probe));
			graph.removeLayer(probe);
			return (DOCUMENT_STATUS.COMPLETED);
		}
	}

	// =================================================== optional
	// ===================================================
	/**
	 * <strong>OVERRIDE THIS METHOD FOR CUSTOMIZATION</strong> <br/>
	 * This method is called by the pepper framework after initializing this
	 * object and directly before start processing. Initializing means setting
	 * properties {@link PepperModuleProperties}, setting temporary files,
	 * resources etc. . returns false or throws an exception in case of
	 * {@link PepperModule} instance is not ready for any reason.
	 * 
	 * @return false, {@link PepperModule} instance is not ready for any reason,
	 *         true, else.
	 */
	@Override
	public boolean isReadyToStart() throws PepperModuleNotReadyException {
		// TODO make some initializations if necessary
		return (super.isReadyToStart());
	}
}
