package org.corpus_tools.peppermodules.alignment;

import org.corpus_tools.pepper.modules.PepperModuleProperties;
import org.corpus_tools.pepper.modules.PepperModuleProperty;

public class AlignerProperties extends PepperModuleProperties {
	/** Provides a path to a file containing the alignment by file name. */
	public static final String PROP_ALIGNMENT_FILE = "alignment.file";
	/** name of sentence span annotation */
	public static final String PROP_SENTENCE_NAME = "sentence.name";
	/** smallest sentence index used in data (usually 0 or 1) **/
	public static final String PROP_SMALLEST_SENTENCE_VALUE = "sentence.smallest.value";
	/** suffix identifying an counter file */
	public static final String PROP_SUFFIX = "alignment.suffix";
	
	public AlignerProperties() {
		addProperty(PepperModuleProperty.create()
				.withName(PROP_ALIGNMENT_FILE)
				.withType(String.class)
				.withDescription("Provides a path to a file containing the alignment by file name.")
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
		addProperty(PepperModuleProperty.create()
				.withName(PROP_SUFFIX)
				.withType(String.class)
				.withDescription("TODO")
				.isRequired(true)
				.build()
				);
	}
	
	/** This method provides the path where the alignment is to be read from. There is one {@value Aligner#ALIGNMENT_FILE_ENDING}-file per document to be aligned with another, non-mentioned document.
	 * @return The directory containing the alignment files.
	 *  */
	public String getAlignmentFile() {
		return (String) getProperty(PROP_ALIGNMENT_FILE).getValue();
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
	public Integer getSmallestSentenceValue() {
		return (Integer) getProperty(PROP_SMALLEST_SENTENCE_VALUE).getValue();
	}
	
	public String getSuffix() {
		return (String) getProperty(PROP_SUFFIX).getValue();
	}
}
