package org.corpus_tools.peppermodules.alignment;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.corpus_tools.pepper.common.DOCUMENT_STATUS;
import org.corpus_tools.pepper.common.PepperConfiguration;
import org.corpus_tools.pepper.impl.PepperManipulatorImpl;
import org.corpus_tools.pepper.impl.PepperMapperImpl;
import org.corpus_tools.pepper.modules.ModuleController;
import org.corpus_tools.pepper.modules.PepperImporter;
import org.corpus_tools.pepper.modules.PepperManipulator;
import org.corpus_tools.pepper.modules.PepperMapper;
import org.corpus_tools.pepper.modules.PepperMapperController;
import org.corpus_tools.pepper.modules.exceptions.PepperModuleDataException;
import org.corpus_tools.pepper.modules.exceptions.PepperModuleException;
import org.corpus_tools.salt.SALT_TYPE;
import org.corpus_tools.salt.SaltFactory;
import org.corpus_tools.salt.common.SCorpusGraph;
import org.corpus_tools.salt.common.SDocument;
import org.corpus_tools.salt.common.SDocumentGraph;
import org.corpus_tools.salt.common.SSpan;
import org.corpus_tools.salt.common.SToken;
import org.corpus_tools.salt.core.SAnnotation;
import org.corpus_tools.salt.core.SLayer;
import org.corpus_tools.salt.core.SNode;
import org.corpus_tools.salt.core.SRelation;
import org.corpus_tools.salt.graph.Identifier;
import org.corpus_tools.salt.graph.Label;
import org.eclipse.emf.common.util.URI;
import org.osgi.service.component.annotations.Component;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;

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
	public static final String ALIGNMENT_FILE_ENDING = "json";
	private static final String LAYER_NAME_NEW = "NEW";
	
	/** maps from document name to alignment array */
	private Map<String, int[][][]> alignmentMap = null;
	private Map<SDocument, SDocument> source2targetDocument;
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
		setSupplierContact(URI.createURI(PepperConfiguration.EMAIL));
		setSupplierHomepage(URI.createURI(PepperConfiguration.HOMEPAGE));
		// TODO add a description of what your module is supposed to do
		setDesc("");
		setProperties(new AlignerProperties());
	}
	
	@Override
	public void start() {
		if (getSaltProject() == null || getSaltProject().getCorpusGraphs().isEmpty()) {
			// instead of handling the exception just pass on to super implementation, that takes care of the exception or provides an alternative plan  FIXME?
			super.start();
		}		
		prepareMergeOfDocuments();
		super.start();
	}
	
	/**
	 * FIXME this method is public since testing this is impossible without a document controller being set in advance ... look into this
	 */
	public void prepareMergeOfDocuments() {
		String path = "<not set>";
		try {
			path = ((AlignerProperties) getProperties()).getAlignmentFile();
			int i = 0;
			GsonBuilder builder = new GsonBuilder();
			Gson gson = builder.create();
			JsonReader reader = new JsonReader( new FileReader(path) );
			TypeToken<?> tt = new TypeToken<Map<String, int[][][]>>(){};
			alignmentMap = gson.fromJson(reader, tt.getType());
		} catch (IOException e) {
			throw new PepperModuleException("An error occured reading the alignment files: " + path);
		}
		SCorpusGraph corpusGraph = getSaltProject().getCorpusGraphs().get(0);
		source2targetDocument = new HashMap<>();
		String suffix = ((AlignerProperties) getProperties()).getSuffix();
		for (String documentName : alignmentMap.keySet()) {
			SDocument original = null;
			SDocument translation = null;
			for (Iterator<SDocument> docs = corpusGraph.getDocuments().iterator(); (original==null || translation==null) && docs.hasNext(); ) {
				SDocument currentDoc = docs.next();
				String currentName = currentDoc.getName();
				if (currentName.equals(documentName + ((AlignerProperties) getProperties()).getSuffix())) {
					original = currentDoc;
				}
				else if (currentName.equals(documentName + suffix)) {
					translation = currentDoc;
				}
			}
			if (original != null && translation != null) {
				source2targetDocument.put(original, translation);	
			}
		}
		if (source2targetDocument.isEmpty()) {
			return;
		}
		corpusGraph.getDocuments().stream().filter((SDocument d) -> !source2targetDocument.containsKey(d)).forEach(corpusGraph::removeNode);
		for (Entry<SDocument, SDocument> e : source2targetDocument.entrySet()) {
			System.out.println(e.getKey() + "(" + e.getKey().getDocumentGraph() + ")" + " <-> " + e.getValue()+ "(" + e.getValue().getDocumentGraph() + ")");
		}
	}
	
	private int[][][] getAlignmentByDocumentName(String name) {
		return alignmentMap.get(name);
	}
	
	public PepperMapper createPepperMapper(Identifier Identifier) {
		AlignmentMapper mapper = new AlignmentMapper();
		SDocument document = getSaltProject().getCorpusGraphs().get(0).getDocument(Identifier);
		if (document != null) { 
			mapper.setDocument(document);
		}
		return (mapper);
	}

	public class AlignmentMapper extends PepperMapperImpl {
		
		/**
		 * prints out some information about document-structure
		 */
		@Override
		public DOCUMENT_STATUS mapSDocument() {
			if (getDocument() == null || !source2targetDocument.containsKey( getDocument() )) {
				logger.warn("Document should not be mapped!");
				return DOCUMENT_STATUS.DELETED;
			}
			SDocumentGraph graph = getDocument().getDocumentGraph();
			while ( source2targetDocument.get(getDocument()).getDocumentGraph() == null ) {
				try {
					this.controller.wait(250);
				} catch (InterruptedException e) {}
			}
			mergeDocuments();
			AlignerProperties properties = (AlignerProperties) Aligner.this.getProperties();
			int[][][] alignment = Aligner.this.getAlignmentByDocumentName( getDocument().getName() );
			String sentenceName = properties.getSentenceName();
			int sOffset = properties.getSmallestSentenceValue();
			List<SSpan> spans = graph.getSpans();
			SSpan[][] spanPairs = new SSpan[alignment.length][2];
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
		
		private void mergeDocuments() {			
			SDocument sourceDocument = getDocument();
			SDocument targetDocument = source2targetDocument.get(sourceDocument);
			if (targetDocument == null) {
				String addInfo = "";
				if (source2targetDocument.values().contains( getDocument() )) {
					addInfo = "mapping is inverse!";
				}
				throw new PepperModuleDataException(this, "Could not map " + getDocument().getName() + ", because there is no target document available. " + addInfo);
			}
			SDocumentGraph targetGraph = targetDocument.getDocumentGraph();
			if (targetGraph == null) {
				throw new PepperModuleDataException(this, "Target graph is null for " + targetDocument.getName() + ". Source graph: " + sourceDocument.getDocumentGraph());
			}
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
	}
}
