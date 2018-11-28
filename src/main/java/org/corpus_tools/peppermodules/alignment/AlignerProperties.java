package org.corpus_tools.peppermodules.alignment;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

import org.corpus_tools.pepper.modules.PepperModuleProperties;
import org.corpus_tools.pepper.modules.PepperModuleProperty;
import org.corpus_tools.pepper.modules.exceptions.PepperModuleException;

public class AlignerProperties extends PepperModuleProperties {
	/** provides a path to the alignment configuration */
	public static final String PROP_ALIGNMENT_FILES_DIR = "alignment.dir";
	/** name of sentence span annotation */
	public static final String PROP_SENTENCE_NAME = "sentence.name";
	/** smallest sentence index used in data (usually 0 or 1) **/
	public static final String PROP_SMALLEST_SENTENCE_VALUE = "sentence.smallest.value";
	
	public AlignerProperties() {
		addProperty(PepperModuleProperty.create()
				.withName(PROP_ALIGNMENT_FILES_DIR)
				.withType(String.class)
				.withDescription("provides a path to the alignment configuration")
				.isRequired(true)
				.build());
		addProperty(PepperModuleProperty.create()
				.withName(PROP_SENTENCE_NAME)
				.withType(String.class)
				.withDescription("name of sentence span annotation")
				.withDefaultValue("sentence")
				.isRequired(false)
				.build());
		addProperty(PepperModuleProperty.create()
				.withName(PROP_SMALLEST_SENTENCE_VALUE)
				.withType(Integer.class)
				.withDescription("smallest sentence index used in data (usually 0 or 1)")
				.withDefaultValue(0)
				.isRequired(false)
				.build());
	}
	
	/** This method provides the path where the alignment is to be read from. There is one {@value Aligner#ALIGNMENT_FILE_ENDING}-file per document to be aligned with another, non-mentioned document.
	 * @return The directory containing the alignment files.
	 *  */
	public String getAlignmentDir() {
		return (String) getProperty(PROP_ALIGNMENT_FILES_DIR).getValue();
	}
	
	/**
	 * Provides the annotation name for sentence spans.
	 * @return sentence annotation name
	 */
	public String getSentenceName() {
		return (String) getProperty(PROP_SENTENCE_NAME).getValue();
	}
	
	/**
	 * Returns the smallest sentence index used. Usually this is zero or one.
	 * @return the smallest sentence value as Integer
	 */
	public int getSmallestSentenceValue() {
		return (int) getProperty(PROP_SMALLEST_SENTENCE_VALUE).getValue();
	}
}
