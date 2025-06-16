package redactedrice.gbcframework.addressing;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import redactedrice.gbcframework.utils.RomUtils;

public class AddressRange 
{
	int start;
	int stopExclusive;
	
	public AddressRange(int start, int stopExclusive)
	{
		if (start > stopExclusive)
		{
			throw new IllegalArgumentException("AddressRange: The exclusive stop passed (" + stopExclusive + ") was before the start (" + start + ")");
		}
		this.start = start;
		this.stopExclusive = stopExclusive;
	}
	
	public AddressRange(BankAddress start, int size)
	{
		if (size < 0)
		{
			throw new IllegalArgumentException("AddressRange: The size passed (" + size + ") is not a positive number");
		}
		this.start = RomUtils.convertToGlobalAddress(start);
		this.stopExclusive = this.start + size;
	}
	
	public AddressRange(AddressRange toCopy)
	{
		this(toCopy.start, toCopy.stopExclusive);
	}
	
	public boolean contains(int doesContain)
	{
		return start <= doesContain && doesContain < stopExclusive;
	}
	
	public boolean contains(AddressRange doesContain)
	{
		return start <= doesContain.start && stopExclusive >= doesContain.stopExclusive;
	}
	
	public boolean overlaps(AddressRange toCheck)
	{
		// If it contains the start address or the last address, then it must overlap
		// Also overlaps it is contained entirely in the toCheck range
		return contains(toCheck.start) || contains(toCheck.stopExclusive - 1) || // -1 on end since its not inclusive
				toCheck.contains(this);
	}
	
	public AddressRange removeOverlap(AddressRange toRemove)
	{
		if (overlaps(toRemove))
		{
			// If this whole space is overlapped, set this to empty with invalid values
			if (toRemove.contains(this))
			{
				start = -1;
				stopExclusive = -1;
			}
			// if the space to remove its entirely contained, remove it from this
			// range by splitting it. We return the latter of the two new ranges
			if (contains(toRemove))
			{
				// if the have the same start, just modify this one as the other would be empty
				if (toRemove.start == start)
				{
					start = toRemove.stopExclusive;
				}
				// Similarly if we have the same stop, modify this one too
				else if (toRemove.stopExclusive == stopExclusive)
				{
					stopExclusive = toRemove.start;
				}
				// Otherwise, we will end up with splits on each side
				else
				{
					AddressRange newRange = new AddressRange(toRemove.stopExclusive, stopExclusive);
					stopExclusive = toRemove.start;
					return newRange;
				}
			}
			// else shorten this up based on the removed portion
			else if (contains(toRemove.start))
			{
				// If it contains the start, then the end must go past the end of this
				// one since we already checked for fully containing the time. Just
				// set the stop the the other's start
				stopExclusive = toRemove.start; 
			}
			// else it contains the stop
			else 
			{
				// If it contains the stop, then the start must be before the start of this
				// one since we already checked for fully containing the time. Just
				// set the start the the other's stop
				// We do -1 since the stop of what to remove is exclusive
				start = toRemove.stopExclusive;
			}
		}
		
		// No additional range added
		return null;
	}
	
	public AddressRange shiftNew(int shift)
	{
		return new AddressRange(start + shift, stopExclusive + shift);
	}
	
	public void shiftInPlace(int shift)
	{
		start += shift;
		stopExclusive += shift;
	}
	
	// Shrinks the start only
	public void shrink(int shrink)
	{
		start += shrink;
		if (start > stopExclusive)
		{
			start = stopExclusive;
		}
	}
	
	public int size()
	{
		return stopExclusive - start;
	}
	
	public boolean isEmpty()
	{
		return size() <= 0;
	}
	
	public int getStart()
	{
		return start;
	}
	
	public int getStopExclusive()
	{
		return stopExclusive;
	}
	
	public static void sortAndCombine(List<AddressRange> ranges)
	{
		// If there are none or only one, nothing to do
		if (ranges.size() > 1)
		{
			// Sort it by address range start
			List<AddressRange> sorted = new LinkedList<>(ranges);
			Collections.sort(sorted, (ar1, ar2) ->
					ar1.start - ar2.start);
			
			// Now go through and combine them		
			ranges.clear();
			int start = sorted.get(0).getStart();
			int stop = sorted.get(0).getStopExclusive();
			for (AddressRange ar : sorted)
			{
				if (ar.start <= stop)
				{
					if (ar.stopExclusive > stop)
					{
						stop = ar.stopExclusive;
					}
				}
				else
				{
					ranges.add(new AddressRange(start, stop));
					start = ar.start;
					stop = ar.stopExclusive;
				}
			}
			
			// Add the last one that was being worked on
			ranges.add(new AddressRange(start, stop));
		}
	}
}
