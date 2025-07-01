package redactedrice.gbcframework;


import java.io.IOException;
import java.util.List;

import redactedrice.gbcframework.addressing.AddressRange;

public abstract interface QueuedWriter {
    public abstract void append(byte... bytes) throws IOException;

    public abstract String getCurrentBlockName();

    public abstract void startNewBlock(int segmentStartAddress);

    public abstract void startNewBlock(int segmentStartAddress, String segmentName);

    public abstract void startNewBlock(int segmentStartAddress, List<AddressRange> reuseHints);

    public abstract void startNewBlock(int segmentStartAddress, String segmentName,
            List<AddressRange> reuseHints);

    // Lower priority then writes/blocks
    public abstract void queueBlankedBlock(AddressRange range);

    public default void queueBlankedBlocks(List<AddressRange> ranges) {
        for (AddressRange range : ranges) {
            queueBlankedBlock(range);
        }
    }
}
