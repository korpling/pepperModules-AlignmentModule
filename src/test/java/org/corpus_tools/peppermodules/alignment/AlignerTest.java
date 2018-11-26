package org.corpus_tools.peppermodules.alignment;

import static org.junit.Assert.assertEquals;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.corpus_tools.pepper.modules.PepperMapper;
import org.corpus_tools.pepper.modules.exceptions.PepperModuleTestException;
import org.corpus_tools.pepper.testFramework.PepperManipulatorTest;
import org.corpus_tools.salt.SALT_TYPE;
import org.corpus_tools.salt.SaltFactory;
import org.corpus_tools.salt.common.SCorpus;
import org.corpus_tools.salt.common.SCorpusGraph;
import org.corpus_tools.salt.common.SDocumentGraph;
import org.corpus_tools.salt.common.SToken;
import org.corpus_tools.salt.common.SaltProject;
import org.corpus_tools.salt.core.SRelation;
import org.corpus_tools.salt.util.Difference;
import org.junit.Before;
import org.junit.Test;

/**
 * This is a dummy implementation of a JUnit test for testing the
 * {@link Aligner} class. Feel free to adapt and enhance this test
 * class for real tests to check the work of your manipulator. If you are not
 * confirm with JUnit, please have a look at <a
 * href="http://www.vogella.com/tutorials/JUnit/article.html">
 * http://www.vogella.com/tutorials/JUnit/article.html</a>. <br/>
 * Please note, that the test class is derived from
 * {@link PepperManipulatorTest}. The usage of this class should simplfy your
 * work and allows you to test only your single manipulator in the Pepper
 * environment.
 * 
 * @author Martin Klotz
 */
public class AlignerTest extends PepperManipulatorTest {
	/**
	 * This method is called by the JUnit environment each time before a test
	 * case starts. So each time a method annotated with @Test is called. This
	 * enables, that each method could run in its own environment being not
	 * influenced by before or after running test cases.
	 */
	@Before
	public void setUp() {
		setFixture(new Aligner());
	}
	
	@Test
	public void testAlignment() {
		SCorpusGraph sourceGraph = new AlignedDemoSourceGraph().getCorpusGraph();
		SCorpusGraph expectedOutput = new AlignedDemoTargetGraph().getCorpusGraph();
		Aligner manipulator = (Aligner) getFixture();
		AlignerProperties properties = new AlignerProperties();
		properties.setPropertyValue(AlignerProperties.PROP_ALIGNMENT_MAP_PATH, "src/test/resources/test.align");
		properties.setPropertyValue(AlignerProperties.PROP_SMALLEST_SENTENCE_VALUE, 1);
		manipulator.setProperties(properties);
		SaltProject project = SaltFactory.createSaltProject();
		project.addCorpusGraph(sourceGraph);
		manipulator.setSaltProject(project);
		manipulator.setCorpusGraph(sourceGraph);
		manipulator.mergeDocuments();
		SCorpusGraph generatedOutput = manipulator.getCorpusGraph();
		assertEquals(expectedOutput.getDocuments().size(), generatedOutput.getDocuments().size());
		PepperMapper mapper = manipulator.createPepperMapper( generatedOutput.getDocuments().get(0).getIdentifier() );
		mapper.mapSDocument();
		Set<Difference> diffSet = expectedOutput.getDocuments().get(0).getDocumentGraph().findDiffs(generatedOutput.getDocuments().get(0).getDocumentGraph());
		assertEquals(expectedOutput.getDocuments().get(0).getDocumentGraph().getRelations().size(), mapper.getDocument().getDocumentGraph().getRelations().size());
		assertEquals(diffSet.toString(), 0, diffSet.size());
	}
	
	private static class AlignedDemoSourceGraph {
		protected SCorpusGraph graph = null;
		protected static final String SOURCE_TEXT_EN = "Documents made to be aligned by you, Pepper!";
		protected static final String SOURCE_TEXT_DE = "Dokumente, gemacht von dir aligniert zu werden, Pepper!";
		private AlignedDemoSourceGraph() {
			SCorpus corpus = SaltFactory.createSCorpus();			
			graph = SaltFactory.createSCorpusGraph();
			corpus.setGraph(graph);
			{
				SDocumentGraph docGraph = graph.createDocument(corpus, "en").createDocumentGraph();
				for (SToken tok : docGraph.createTextualDS(SOURCE_TEXT_EN).tokenize()) {
					tok.createAnnotation(null, "pos", "any");
				}
				docGraph.createSpan( docGraph.getTokens() ).createAnnotation(null, "sentence", "1");
			}
			{
				SDocumentGraph docGraph = graph.createDocument(corpus, "en_de").createDocumentGraph();
				for (SToken tok : docGraph.createTextualDS(SOURCE_TEXT_DE).tokenize()) {
					tok.createAnnotation(null, "pos", "any");
				}
				docGraph.createSpan( docGraph.getTokens() ).createAnnotation(null, "sentence", "1");
			}
		}
		
		protected SCorpusGraph getCorpusGraph() {
			return graph;
		}
	}
	
	private static class AlignedDemoTargetGraph extends AlignedDemoSourceGraph{
		private static final int[][][] ALIGNMENTS = {{
				{0, 0},
				{1, 2},
				{2, 6},
				{3, 7},
				{4, 5},
				{5, 3},
				{6, 4},
				{8, 9}
		}};
		private AlignedDemoTargetGraph() {
			SCorpus corpus = SaltFactory.createSCorpus();
			graph = SaltFactory.createSCorpusGraph();
			corpus.setGraph(graph);
			{
				Map<Integer, Integer> alignmentByTargetIndex = new HashMap<>();
				{
					for (int i=0; i < ALIGNMENTS[0].length; i++) {
						alignmentByTargetIndex.put(ALIGNMENTS[0][i][1], ALIGNMENTS[0][i][0]);
					}
				}
				SDocumentGraph docGraph = graph.createDocument(corpus, "en").createDocumentGraph();
				for (SToken tok : docGraph.createTextualDS(SOURCE_TEXT_EN).tokenize()) {
					tok.createAnnotation(null, "pos", "any");
				}
				List<SToken> enTokens = docGraph.getSortedTokenByText();
				docGraph.createSpan(enTokens).createAnnotation(null, "sentence", "1");
				int tIx = 0;
				List<SToken> deTokens = new ArrayList<>();
				for (SToken tok : docGraph.createTextualDS(SOURCE_TEXT_DE).tokenize()) {
					tok.createAnnotation(null, "pos", "any");
					if (alignmentByTargetIndex.containsKey(tIx)) {
						int sIx = alignmentByTargetIndex.get(tIx);
						SRelation<?, ?> rel = docGraph.createRelation(enTokens.get(sIx), tok, SALT_TYPE.SPOINTING_RELATION, null);
						rel.setType(Aligner.ALIGNMENT_NAME);
					}
					deTokens.add(tok);
					tIx++;
				}
				docGraph.createSpan(deTokens).createAnnotation(null, "sentence", "1");
			}
		}
		
		private void serializeAlignment(String path) {
			try {
				ObjectOutputStream out = new ObjectOutputStream( new FileOutputStream(path) );
				out.writeObject(ALIGNMENTS);
				out.close();
			} catch (IOException e) {
				throw new PepperModuleTestException();
			}			
		}
	}
}
