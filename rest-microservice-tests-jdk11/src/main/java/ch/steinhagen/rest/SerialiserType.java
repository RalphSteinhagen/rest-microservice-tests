package ch.steinhagen.rest;

/**
 * @author rstein
 */
public enum SerialiserType {
	BINARY("application/octet-stream", true),
    GSON("gson", false),
    JACKSON("jackson", false),
    FASTJSON("fastjson", false),
    NATIVE("native", false),
    UNKNOWN("unknown", false);

	private final String typeName;
    private final boolean isBinaryFormat;

    SerialiserType(final String typeName, final boolean isBinaryFormat) {
        this.typeName = typeName;
        this.isBinaryFormat = isBinaryFormat;
    }

    public boolean isBinary() {
    	return isBinaryFormat;
    }
    
    public boolean isText() {
    	return !isBinaryFormat;
    }

    public String getName() {
        return typeName;
    }

    public static SerialiserType parse(final String name) {
        if (name == null || name.isEmpty()) {
            return UNKNOWN;
        }
        final String nameLower = name.toLowerCase();
        for (SerialiserType type: SerialiserType.values()) {
            if (type.getName().contains(nameLower)) {
                return type;
            }
        }
        return UNKNOWN;
    }
}
