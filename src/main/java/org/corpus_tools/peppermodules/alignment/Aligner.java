package org.corpus_tools.peppermodules.alignment;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
import org.corpus_tools.pepper.modules.exceptions.PepperModuleException;
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
import org.corpus_tools.salt.core.SAnnotation;
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
	public static final String ALIGNMENT_FILE_ENDING = "align";
	private static final String LAYER_NAME_NEW = "NEW";
	
	private Map<String, int[][][]> alignmentMap = null;
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
		setDesc("");
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
		this.alignmentMap = new HashMap<>();
		try {
			String path = ((AlignerProperties) getProperties()).getAlignmentDir();
			int i = 0;
			for (Path filePath : Files.newDirectoryStream( Paths.get(path) )) {
				String fileString = filePath.toString();
				ObjectInputStream in = new ObjectInputStream( new FileInputStream( fileString ));
				String fileName = filePath.getFileName().toString();
				alignmentMap.put(fileName.substring(0, fileName.lastIndexOf('.')), (int[][][]) in.readObject());
			}
		} catch (IOException | ClassNotFoundException e) {
			throw new PepperModuleException("An error occured reading the alignment files.");
		}
		SCorpusGraph corpusGraph = getSaltProject().getCorpusGraphs().get(0);
		Set<Pair<SDocument, SDocument>> documentPairs = new HashSet<>();
		for (String documentName : alignmentMap.keySet()) {
			SDocument original = null;
			SDocument translation = null;
			for (Iterator<SDocument> docs = corpusGraph.getDocuments().iterator(); (original==null || translation==null) && docs.hasNext(); ) {
				SDocument currentDoc = docs.next();
				String currentName = currentDoc.getName();
				if (currentName.equals(documentName)) {
					original = currentDoc;
				}
				else if (!currentName.equals(documentName) && currentName.startsWith(documentName)) {
					translation = currentDoc;
				}
			}
			if (original == null || translation == null) {
				throw new PepperModuleException("Documents could not be matched by names.");
			}
			documentPairs.add( Pair.of(original, translation) );
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
	
	private int[][][] getAlignmentByDocumentName(String name) {
		return alignmentMap.get(name);
	}
	
	public PepperMapper createPepperMapper(Identifier Identifier) {
		AlignmentMapper mapper = new AlignmentMapper();
		mapper.setDocument( getSaltProject().getCorpusGraphs().get(0).getDocument(Identifier) );
		return (mapper);
	}

	public class AlignmentMapper extends PepperMapperImpl {
		
		/**
		 * prints out some information about document-structure
		 */
		@Override
		public DOCUMENT_STATUS mapSDocument() {
			SDocumentGraph graph = getDocument().getDocumentGraph();
			AlignerProperties properties = (AlignerProperties) Aligner.this.getProperties();
			int[][][] alignment = Aligner.this.getAlignmentByDocumentName( getDocument().getName() );
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
}
