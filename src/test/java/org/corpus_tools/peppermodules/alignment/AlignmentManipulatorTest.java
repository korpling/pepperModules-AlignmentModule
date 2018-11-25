package org.corpus_tools.peppermodules.alignment;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.corpus_tools.pepper.testFramework.PepperManipulatorTest;
import org.corpus_tools.salt.SALT_TYPE;
import org.corpus_tools.salt.SaltFactory;
import org.corpus_tools.salt.common.SCorpus;
import org.corpus_tools.salt.common.SCorpusGraph;
import org.corpus_tools.salt.common.SDocumentGraph;
import org.corpus_tools.salt.common.SToken;
import org.junit.Before;
import org.junit.Test;

/**
 * This is a dummy implementation of a JUnit test for testing the
 * {@link AlignmentManipulator} class. Feel free to adapt and enhance this test
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
public class AlignmentManipulatorTest extends PepperManipulatorTest {
	/**
	 * This method is called by the JUnit environment each time before a test
	 * case starts. So each time a method annotated with @Test is called. This
	 * enables, that each method could run in its own environment being not
	 * influenced by before or after running test cases.
	 */
	@Before
	public void setUp() {
		setFixture(new AlignmentManipulator());
	}
	
	@Test
	public void testDocumentMerging() {
		SCorpusGraph sourceGraph = new AlignedDemoSourceGraph().getCorpusGraph();
		SCorpusGraph expectedOutput = new AlignedDemoTargetGraph().getCorpusGraph();
		AlignmentManipulator manipulator = new AlignmentManipulator();
		manipulator.setCorpusGraph(sourceGraph);
		manipulator.start();
		SCorpusGraph generatedOutput = manipulator.getCorpusGraph();
		assertEquals(expectedOutput.getDocuments().size(), generatedOutput.getDocuments().size());		
	}
	
	private static class AlignedDemoSourceGraph {
		protected SCorpusGraph graph = null;
		protected static final String SOURCE_TEXT_EN = "Documents made to be aligned by you, Pepper!";
		protected static final String SOURCE_TEXT_DE = "Dokumente, gemacht von dir aligniert zu werden, Pepper!";
		private AlignedDemoSourceGraph() {
			SCorpus corpus = SaltFactory.createSCorpus();
			graph = SaltFactory.createSCorpusGraph();
			{
				SDocumentGraph docGraph = graph.createDocument(corpus, "en").createDocumentGraph();
				for (SToken tok : docGraph.createTextualDS(SOURCE_TEXT_EN).tokenize()) {
					tok.createAnnotation(null, "pos", "any");
				}
			}
			{
				SDocumentGraph docGraph = graph.createDocument(corpus, "de").createDocumentGraph();
				for (SToken tok : docGraph.createTextualDS(SOURCE_TEXT_DE).tokenize()) {
					tok.createAnnotation(null, "pos", "any");
				}
			}
		}
		
		protected SCorpusGraph getCorpusGraph() {
			return graph;
		}
	}
	
	private static class AlignedDemoTargetGraph extends AlignedDemoSourceGraph{
		private static final int[][] ALIGNMENTS = {
				{0, 0},
				{1, 2},
				{2, 6},
				{3, 7},
				{4, 5},
				{5, 3},
				{6, 4},
				{8, 9}
		};
		private AlignedDemoTargetGraph() {
			SCorpus corpus = SaltFactory.createSCorpus();
			graph = SaltFactory.createSCorpusGraph();
			{
				Map<Integer, Integer> alignmentByTargetIndex = new HashMap<>();
				{
					for (int i=0; i < ALIGNMENTS.length; i++) {
						alignmentByTargetIndex.put(ALIGNMENTS[i][1], ALIGNMENTS[i][0]);
					}
				}
				SDocumentGraph docGraph = graph.createDocument(corpus, "en").createDocumentGraph();
				for (SToken tok : docGraph.createTextualDS(SOURCE_TEXT_EN).tokenize()) {
					tok.createAnnotation(null, "pos", "any");
				}
				List<SToken> enTokens = docGraph.getSortedTokenByText();
				int tIx = 0;
				for (SToken tok : docGraph.createTextualDS(SOURCE_TEXT_DE).tokenize()) {
					tok.createAnnotation(null, "pos", "any");
					if (alignmentByTargetIndex.containsKey(tIx)) {
						int sIx = alignmentByTargetIndex.get(tIx);
						docGraph.createRelation(enTokens.get(sIx), tok, SALT_TYPE.SPOINTING_RELATION, null).setName(AlignmentManipulator.ALIGNMENT_NAME);
					}
					tIx++;
				}
			}
		}
	}
}
