package redactedrice.gbcframework;


import java.util.Map;
import java.util.Map.Entry;

public final class SegmentNamingUtils {
    private SegmentNamingUtils() {}

    static final String SUBSEGMENT_STARTLINE = ".";
    static final String PLACEHOLDER_MARKER = "#";

    public static String formSubsegmentName(String subsegment, String rootSegmentName) {
        return rootSegmentName + "." + subsegment;
    }

    public static boolean isOnlySubsegmentPartOfLabel(String line) {
        return line.startsWith(SUBSEGMENT_STARTLINE);
    }

    public static boolean containsPlaceholder(String line) {
        return line.contains(PLACEHOLDER_MARKER);
    }

    public static String createPlaceholder(String placeholderId) {
        return PLACEHOLDER_MARKER + placeholderId + PLACEHOLDER_MARKER;
    }

    public static String replacePlaceholders(String line, Map<String, String> placeholderToArgs) {
        // TODO: refactor to find placeholder in line and search map for that
        for (Entry<String, String> entry : placeholderToArgs.entrySet()) {
            if (!entry.getKey().startsWith(PLACEHOLDER_MARKER)
                    || !entry.getKey().endsWith(PLACEHOLDER_MARKER)) {
                throw new IllegalArgumentException("Non placeholder key passed: " + entry.getKey());
            }
            line = line.replace(entry.getKey(), entry.getValue());
        }
        return line;
    }
}
