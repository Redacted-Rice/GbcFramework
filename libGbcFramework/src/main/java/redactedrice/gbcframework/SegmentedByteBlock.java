package redactedrice.gbcframework;


import java.io.IOException;
import java.util.Set;

import redactedrice.gbcframework.addressing.AddressRange;
import redactedrice.gbcframework.addressing.AssignedAddresses;
import redactedrice.gbcframework.addressing.BankAddress;

public interface SegmentedByteBlock {
    public String getId();

    public default int getWorstCaseSize() {
        return getWorstCaseSize(null);
    }

    public int getWorstCaseSize(AssignedAddresses assignedAddresses);

    public Set<String> getSegmentIds();

    public BankAddress getSegmentsRelativeAddresses(BankAddress blockAddress,
            AssignedAddresses assignedAddresses, AssignedAddresses relAddresses);

    public void addByteSourceHint(AddressRange hint);

    public BankAddress write(QueuedWriter writer, AssignedAddresses assignedAddresses)
            throws IOException;

    public void checkAndFillSegmentGaps(BankAddress expectedFromPrevSegAddress,
            BankAddress nextSegAddress, QueuedWriter writer, String nextSegName) throws IOException;
}
