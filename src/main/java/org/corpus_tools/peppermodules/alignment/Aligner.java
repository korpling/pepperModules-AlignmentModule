package org.corpus_tools.peppermodules.alignment;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.corpus_tools.pepper.common.DOCUMENT_STATUS;
import org.corpus_tools.pepper.common.PepperConfiguration;
import org.corpus_tools.pepper.impl.PepperManipulatorImpl;
import org.corpus_tools.pepper.impl.PepperMapperImpl;
import org.corpus_tools.pepper.modules.PepperManipulator;
import org.corpus_tools.pepper.modules.PepperMapper;
import org.corpus_tools.pepper.modules.exceptions.PepperModuleDataException;
import org.corpus_tools.salt.SALT_TYPE;
import org.corpus_tools.salt.SaltFactory;
import org.corpus_tools.salt.common.SDocumentGraph;
import org.corpus_tools.salt.common.SPointingRelation;
import org.corpus_tools.salt.common.SSpan;
import org.corpus_tools.salt.common.SSpanningRelation;
import org.corpus_tools.salt.common.STextualDS;
import org.corpus_tools.salt.common.STimelineRelation;
import org.corpus_tools.salt.common.SToken;
import org.corpus_tools.salt.core.SAnnotation;
import org.corpus_tools.salt.core.SLayer;
import org.corpus_tools.salt.core.SRelation;
import org.corpus_tools.salt.graph.Identifier;
import org.corpus_tools.salt.util.DataSourceSequence;
import org.corpus_tools.salt.util.SaltUtil;
import org.eclipse.emf.common.util.URI;
import org.osgi.service.component.annotations.Component;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * This is a dummy implementation to show how a {@link PepperManipulator} works.
 * Therefore it just prints out some information about a corpus like the number
 * of nodes, edges and for instance annotation frequencies. <br/>
 * This class can be used as a template for an own implementation of a
 * {@link PepperManipulator} Take a look at the TODO's and adapt the code. If
 * this is the first time, you are implementing a Pepper module, we strongly
 * recommend, to take a look into the 'Developer's Guide for Pepper modules',
 * you will find on
 * <a href="http://corpus-tools.org/pepper/">http://corpus-tools.org/pepper</a>.
 * 
 * @author Martin Klotz
 */
@Component(name = "AlignmentManipulatorComponent", factory = "PepperManipulatorComponentFactory")
public class Aligner extends PepperManipulatorImpl {

	// =================================================== mandatory
	// ===================================================
	/**
	 * <strong>OVERRIDE THIS METHOD FOR CUSTOMIZATION</strong> <br/>
	 * A constructor for your module. Set the coordinates, with which your module
	 * shall be registered. The coordinates (modules name, version and supported
	 * formats) are a kind of a fingerprint, which should make your module unique.
	 */
	public Aligner() {
		super();
		setName("AlignmentManipulator");
		setSupplierContact(URI.createURI(PepperConfiguration.EMAIL));
		setSupplierHomepage(URI.createURI(PepperConfiguration.HOMEPAGE));
		// TODO add a description of what your module is supposed to do
		setDesc("This module aligns tokens with pointing relations that carry the same annotation value for two specified annotation names.");
		setProperties(new AlignerProperties());
	}

	public PepperMapper createPepperMapper(Identifier Identifier) {
		AlignmentMapper mapper = new AlignmentMapper();
		return (mapper);
	}

	public class AlignmentMapper extends PepperMapperImpl {
		private static final String ERR_MSG_NO_SUCH_TEXT = "No such text: ";
		private static final String ERR_MSG_LABEL_ANNO_ERR = "Cannot even partially assign edge labels. No such annotation: {}. Check the provided annotation name or do not set property '"
				+ AlignerProperties.PROP_ANNO_QNAME_ALIGN_LABEL + "'.";

		
		@Override
		public DOCUMENT_STATUS mapSDocument() {
			String sourceTextName = getAlignerProperties().getSourceTextName();
			String targetTextName = getAlignerProperties().getTargetTextName();
			String sourceAnnoQName = getAlignerProperties().getSourceAnnoQName();
			String targetAnnoQName = getAlignerProperties().getTargetAnnoQName();
			String alignmentName = getAlignerProperties().getAlignmentName();

			String automaticTimeAlignValue = getAlignerProperties().getAutomaticTimeAlignmentValue();
			
			
			Map<String, SToken> sources = getId2TokenMap(sourceTextName, sourceAnnoQName, automaticTimeAlignValue);
			Map<String, SToken> targets = getId2TokenMap(targetTextName, targetAnnoQName, automaticTimeAlignValue);
			align(sources, targets, alignmentName);
		
			if(getAlignerProperties().getRemoveTimeline()) {
				SDocumentGraph graph = getDocument().getDocumentGraph();
				
				graph.removeNode(graph.getTimeline());
				graph.getTimelineRelations().stream().forEach(graph::removeRelation);
			}
			
			return (DOCUMENT_STATUS.COMPLETED);
		}

		private AlignerProperties getAlignerProperties() {
			return ((AlignerProperties) getProperties());
		}

		private List<SToken> getTokensByDS(STextualDS ds) {
			DataSourceSequence<Number> dss = new DataSourceSequence<Number>(ds, ds.getStart(), ds.getEnd());
			return getDocument().getDocumentGraph().getTokensBySequence(dss);
		}

		private STextualDS getDSbyName(String dsName) {
			try {
				return getDocument().getDocumentGraph().getTextualDSs().stream()
						.filter((STextualDS tds) -> dsName.equals(tds.getName())).findFirst().get();
			} catch (NoSuchElementException e) {
				throw new PepperModuleDataException(this, ERR_MSG_NO_SUCH_TEXT + dsName, e);
			}
		}

		private Map<String, SToken> getId2TokenMap(final String textName, final String annoQName,
				final String automaticTimeAlignValue) {
			SDocumentGraph graph = getDocument().getDocumentGraph();
			SLayer tokenLayer = SaltFactory.createSLayer();
			tokenLayer.setName(textName);
			tokenLayer.setGraph(graph);
			STextualDS ds = getDSbyName(textName);
			Multimap<SToken, SSpan> token2SpanMap = HashMultimap.create();
			for (SSpan span : graph.getSpans()) {
				List<SToken> overlappedTokens = graph.getOverlappedTokens(span);
				if (span.getAnnotation(annoQName) != null) {
					for(SToken tok : overlappedTokens) {
						token2SpanMap.put(tok, span);
					}
				}
			}
			List<SToken> tokens = getTokensByDS(ds);
			tokens.stream().forEach(tokenLayer::addNode);
			Map<String, SToken> id2TokenMap = new HashMap<>();
			for (SToken tok : tokens) {
				if (token2SpanMap.containsKey(tok)) {
					for(SSpan span : token2SpanMap.get(tok)) {
						SAnnotation anno = span.getAnnotation(annoQName);
						if (anno != null) {
							String value = anno.getValue_STEXT();
	
							if (automaticTimeAlignValue != null && !automaticTimeAlignValue.isEmpty()
									&& automaticTimeAlignValue.equals(value)) {
								// find all aligned timeline events and add an ID derived from their index
								for(SRelation<?,?> rel : tok.getOutRelations()) {
										if(rel instanceof STimelineRelation) {
											STimelineRelation timeRel = (STimelineRelation) rel;
											for(int i=timeRel.getStart(); i < timeRel.getEnd(); i++) {
												id2TokenMap.put(timeRel.getTarget().getId() + "_" + i , tok);
											}
										}
								}
							} else if (value != null) {
								for (String id : value.split(",")) {
									id2TokenMap.put(id.trim(), tok);
								}
							}
						}
					}
				}
			}
			return id2TokenMap;
		}

		private void align(Map<String, SToken> sources, Map<String, SToken> targets, String alignmentName) {
			String labelAnnoQName = getAlignerProperties().getAlignmentLabelAnnoQName();
			SDocumentGraph graph = getDocument().getDocumentGraph();
			Set<Pair<String, String>> existingRelations = new HashSet<>();
			SLayer aLayer = SaltFactory.createSLayer();
			aLayer.setName(alignmentName);
			aLayer.setGraph(graph);
			for (Entry<String, SToken> sourceEntry : sources.entrySet()) {
				SToken sourceToken = sourceEntry.getValue();
				SToken targetToken = targets.get(sourceEntry.getKey());
				if (targetToken == null) {
					String sourceSpan = graph.getText(sourceToken);
					logger.error("Alignment target \"" + sourceEntry.getKey() + "\" not found for token \"" + sourceSpan
							+ "\" (" + sourceToken.getId() + ").");
				} else  {
					SPointingRelation alignRel = (SPointingRelation) graph.createRelation(sourceToken, targetToken,
							SALT_TYPE.SPOINTING_RELATION, null);
					alignRel.setType(alignmentName);
					existingRelations.add(Pair.of(sourceToken.getId(), targetToken.getId()));
					aLayer.addRelation(alignRel);
				}
			}
			for (Entry<String, SToken> targetEntry : targets.entrySet()) {
				SToken targetToken = targetEntry.getValue();
				SToken sourceToken = sources.get(targetEntry.getKey());
				if (sourceToken == null) {
					String targetSpan = graph.getText(targetToken);
					logger.error("Alignment source \"" + targetEntry.getKey() + "\" not found for token \"" + targetSpan
							+ "\" (" + targetToken.getId() + ").");
				} else {
					Pair<String, String> nodePair = Pair.of(sourceToken.getId(), targetToken.getId());
					if (!existingRelations.contains(nodePair)) {
						SPointingRelation alignRel = (SPointingRelation) graph.createRelation(sourceToken, targetToken,
								SALT_TYPE.SPOINTING_RELATION, null);
						alignRel.setType(alignmentName);
						aLayer.addRelation(alignRel);
					}
				}
			}
			if (labelAnnoQName != null) { // annotate alignment edges
				boolean isOnSpan = graph.getSpans().stream().anyMatch((SSpan s) -> s.containsLabel(labelAnnoQName));
				String defaultValue = getAlignerProperties().getAlignmentLabelDefaultValue();
				if (!isOnSpan && defaultValue == null && graph.getTokens().stream().noneMatch((SToken t) -> t.containsLabel(labelAnnoQName))) {
					throw new PepperModuleDataException(this, String.format(ERR_MSG_LABEL_ANNO_ERR, labelAnnoQName));
				} else {
					for (SRelation rel : aLayer.getRelations()) {
						SAnnotation edgeLabel = null;
						if (isOnSpan) {
							Optional<SRelation> optionalValue = ((SToken) rel.getSource()).getInRelations().stream()
									.filter((SRelation r) -> r instanceof SSpanningRelation
											&& r.getSource().containsLabel(labelAnnoQName))
									.findFirst();
							if (!optionalValue.isPresent()) {
								optionalValue = ((SToken) rel.getTarget()).getInRelations().stream()
										.filter((SRelation r) -> r instanceof SSpanningRelation
												&& r.getSource().containsLabel(labelAnnoQName))
										.findFirst();
							}
							if (optionalValue.isPresent()) {
								edgeLabel = ((SSpan) optionalValue.get().getSource()).getAnnotation(labelAnnoQName);
							}
						} else {
							edgeLabel = ((SToken) rel.getSource()).getAnnotation(labelAnnoQName);
							if (edgeLabel == null) {
								edgeLabel = ((SToken) rel.getTarget()).getAnnotation(labelAnnoQName);
							}
						}

						if (edgeLabel == null && defaultValue != null) {
							Pair<String, String> qname = SaltUtil.splitQName(labelAnnoQName);
							rel.createAnnotation(qname.getKey(), qname.getValue(), defaultValue);
						} else if (edgeLabel != null) {
							rel.createAnnotation(null, edgeLabel.getName(), edgeLabel.getValue());
						}
					}
				}
			}
		}
	}
}
