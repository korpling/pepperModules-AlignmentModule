package org.corpus_tools.peppermodules.alignment;

import org.corpus_tools.pepper.modules.PepperModuleProperties;
import org.corpus_tools.pepper.modules.PepperModuleProperty;

public class AlignerProperties extends PepperModuleProperties {
	/** The name of the text containing the source */
	public static final String PROP_TEXT_NAME_SOURCE = "source.text.name";
	/** The name of the text containing the target */
	public static final String PROP_TEXT_NAME_TARGET = "target.text.name";
	/** The qualified name of the alignment annotation name on the source */
	public static final String PROP_ANNO_NAME_SOURCE = "source.anno.qname";
	/** The qualified name of the alignment annotation name on the target */
	public static final String PROP_ANNO_NAME_TARGET = "target.anno.qname";
	/** If you want to additionally label the alignment edges, provide a name for the token or span annotation here. The annotation will be searched on both source and target tokens to be more flexible, values need to be identical if you use both. */
	public static final String PROP_ANNO_QNAME_ALIGN_LABEL = "label.anno.qname";
	
	public AlignerProperties() {
		addProperty(PepperModuleProperty.create()
				.withName(PROP_TEXT_NAME_SOURCE)
				.withType(String.class)
				.withDescription("The name of the text containing the source.")
				.isRequired(true)
				.build());
		addProperty(PepperModuleProperty.create()
				.withName(PROP_TEXT_NAME_TARGET)
				.withType(String.class)
				.withDescription("The name of the text containing the target.")
				.isRequired(true)
				.build());
		addProperty(PepperModuleProperty.create()
				.withName(PROP_ANNO_NAME_SOURCE)
				.withType(String.class)
				.withDescription("The qualified name of the alignment annotation name on the source.")
				.isRequired(true)
				.build());
		addProperty(PepperModuleProperty.create()
				.withName(PROP_ANNO_NAME_TARGET)
				.withType(String.class)
				.withDescription("The qualified name of the alignment annotation name on the target.")
				.isRequired(true)
				.build());
		addProperty(PepperModuleProperty.create()
				.withName(PROP_ANNO_QNAME_ALIGN_LABEL)
				.withType(String.class)
				.withDescription("If you want to additionally label the alignment edges, provide a name for the token or span annotation here. The annotation will be searched on both source and target tokens to be more flexible, values need to be identical if you use both.")
				.withDefaultValue(null)
				.isRequired(false)
				.build());
	}
	
	public String getSourceTextName() {
		return (String) getProperty(PROP_TEXT_NAME_SOURCE).getValue();
	}
	
	public String getTargetTextName() {
		return (String) getProperty(PROP_TEXT_NAME_TARGET).getValue();
	}
	
	public String getSourceAnnoQName() {
		return (String) getProperty(PROP_ANNO_NAME_SOURCE).getValue();
	}
	
	public String getTargetAnnoQName() {
		return (String) getProperty(PROP_ANNO_NAME_TARGET).getValue();
	}
	
	public String getAlignmentLabelAnnoQName() {
		Object value = getProperty(PROP_ANNO_QNAME_ALIGN_LABEL).getValue();
		return value == null? null : (String) value;
	}
}
