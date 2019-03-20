package org.corpus_tools.peppermodules.alignment;

import org.corpus_tools.pepper.testFramework.PepperManipulatorTest;
import org.junit.Before;

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
}
