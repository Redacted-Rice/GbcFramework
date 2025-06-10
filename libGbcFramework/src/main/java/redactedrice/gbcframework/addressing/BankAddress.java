package redactedrice.gbcframework.addressing;


import redactedrice.gbcframework.RomConstants;
import redactedrice.gbcframework.utils.RomUtils;

public class BankAddress 
{
	public static final byte UNASSIGNED_BANK = -1;
	public static final short UNASSIGNED_ADDRESS = -1;

	public static final BankAddress UNASSIGNED = new BankAddress(UNASSIGNED_BANK, UNASSIGNED_ADDRESS);
	public static final BankAddress ZERO = new BankAddress((byte) 0, (short) 0);

	public enum BankAddressLimitType
	{
		IN_VALID_RANGES,
		WITHIN_BANK,
		WITHIN_BANK_OR_START_OF_NEXT
	}

	public enum BankAddressToUseType
	{
		BANK_AND_BANK_IN_ADDRESS,
		BANK_ONLY,
		ADDRESS_IN_BANK_ONLY
	}
	
	private byte bank;
	private short addressInBank;
	
	public BankAddress()
	{
		bank = UNASSIGNED.bank;
		addressInBank = UNASSIGNED.addressInBank;
	}
	
	public BankAddress(byte bank, short addressInBank)
	{
		setBank(bank);
		setAddressInBank(addressInBank);
	}

	public BankAddress(BankAddress toCopy) 
	{
		bank = toCopy.bank;
		addressInBank = toCopy.addressInBank;
	}
	
	public BankAddress(int globalAddress) 
	{		
		if (globalAddress < 0 || globalAddress >= RomConstants.NUMBER_OF_BANKS * RomConstants.BANK_SIZE)
		{
			throw new IllegalArgumentException("BankAddress - invalid globalAddress given (" + globalAddress + 
					") - the globalAddress must be between 0 and " + RomConstants.NUMBER_OF_BANKS * RomConstants.BANK_SIZE); 
		}
		setBank(RomUtils.determineBank(globalAddress));
		setAddressInBank(RomUtils.convertToBankOffset(globalAddress));
	}
	
	public BankAddress newAtStartOfBank()
	{
		return new BankAddress(bank, (short) 0);
	}

	public BankAddress newAtStartOfNextBank() 
	{
		if (isBankInRange((byte) (bank + 1)))
		{
			return new BankAddress((byte) (bank + 1), (short) 0);
		}
		return null;
	}
	
	public BankAddress newOffsetted(int offset)
	{
		BankAddress copy = new BankAddress(this);
		if (!copy.offset(offset))
		{
			return null;
		}
		return copy;
	}

	public BankAddress newOffsetted(int offset, BankAddressLimitType limit)
	{
		BankAddress copy = new BankAddress(this);
		if (!copy.offset(offset, limit))
		{
			return null;
		}
		return copy;
	}
	
	public BankAddress newSum(BankAddress toAdd)
	{
		BankAddress copy = new BankAddress(this);
		if (!copy.add(toAdd))
		{
			return null;
		}
		return copy;
	}
	
	public BankAddress newSum(BankAddress toAdd, BankAddressToUseType whatToAdd)
	{
		BankAddress copy = new BankAddress(this);
		if (!copy.add(toAdd, whatToAdd))
		{
			return null;
		}
		return copy;
	}

	public BankAddress newSum(BankAddress toAdd, BankAddressToUseType whatToAdd, BankAddressLimitType limit)
	{
		BankAddress copy = new BankAddress(this);
		if (!copy.add(toAdd, whatToAdd, limit))
		{
			return null;
		}
		return copy;
	}
	
	public BankAddress newAbsoluteDifferenceBetween(BankAddress toGetRelativeOf)
	{
		return newAbsoluteDifferenceBetween(toGetRelativeOf, BankAddressToUseType.BANK_AND_BANK_IN_ADDRESS);
	}
	
	public BankAddress newAbsoluteDifferenceBetween(BankAddress toGetRelativeOf, BankAddressToUseType whatToDiff)
	{
		switch (whatToDiff)
		{
		case BANK_ONLY:
			return new BankAddress((byte) Math.abs(bank - toGetRelativeOf.bank), (short) 0);
		case ADDRESS_IN_BANK_ONLY:
			return new BankAddress((byte) 0, (short) Math.abs(addressInBank - toGetRelativeOf.addressInBank));
		default: //Default to all
		case BANK_AND_BANK_IN_ADDRESS:
			return new BankAddress(Math.abs(getDifference(toGetRelativeOf, BankAddressToUseType.BANK_AND_BANK_IN_ADDRESS)));
		}
	}
	
	public boolean offset(int offset)
	{
		return offset(offset, BankAddressLimitType.IN_VALID_RANGES);
	}
	
	public boolean offset(int offset, BankAddressLimitType limit)
	{
		byte newBank = bank;
		short newAddress = (short) (addressInBank + offset);
		switch (limit)
		{
		case WITHIN_BANK:
		case WITHIN_BANK_OR_START_OF_NEXT:
			if (!isAddressInBankInRange(bank, newAddress, limit))
			{
				return false;
			}
			break;
		default: // Default to IN_VALID_RANGES
		case IN_VALID_RANGES:
			while (!isAddressInBankInRange(bank, newAddress, BankAddressLimitType.WITHIN_BANK))
			{
				newBank++;
				newAddress -= RomConstants.BANK_SIZE;
			}
			
			if (!isBankInRange(newBank))
			{
				return false;
			}
			break;
		}

		// Set the variables
		bank = newBank;
		addressInBank = newAddress;
		return true;
	}

	public boolean add(BankAddress toAdd)
	{
		return add(toAdd, BankAddressToUseType.BANK_AND_BANK_IN_ADDRESS, BankAddressLimitType.IN_VALID_RANGES);
	}
	
	public boolean add(BankAddress toAdd, BankAddressToUseType whatToAdd)
	{
		return add(toAdd, whatToAdd, BankAddressLimitType.IN_VALID_RANGES);
	}
	
	public boolean add(BankAddress toAdd, BankAddressToUseType whatToAdd, BankAddressLimitType limit)
	{
		int bankSum = bank;
		int addressSum = addressInBank;
		switch(whatToAdd)
		{
		case BANK_ONLY:
			bankSum += toAdd.bank;
			break;
		case ADDRESS_IN_BANK_ONLY:
			addressSum += toAdd.addressInBank;
			break;
		case BANK_AND_BANK_IN_ADDRESS:
		default: // Default to everything
			bankSum += toAdd.bank;
			addressSum += toAdd.addressInBank;
			break;
		}
		
		// Check the address
		boolean valid = isAddressInBankInRange(bankSum, addressSum, limit);
		
		// Now check bank if address is valid
		if (valid)
		{
			switch (limit)
			{
			default: // Default to IN_VALID_RANGES
			case IN_VALID_RANGES:
				valid = isBankInRange(bankSum);
				break;
			case WITHIN_BANK_OR_START_OF_NEXT:
				valid = bankSum == bank ||
					(bankSum - 1 == bank && addressSum == 0);
				break;
			case WITHIN_BANK:
				valid = bankSum == bank;
				break;
			}
		}
		
		if (valid)
		{
			bank = (byte) bankSum;
			addressInBank = (short) addressSum;
			return true;
		}
		return false;
	}
	
	public void setToCopyOf(BankAddress toCopy) 
	{
		bank = toCopy.bank;
		addressInBank = toCopy.addressInBank;
	}

	public void setBank(byte bank) 
	{
		if (!isBankInRange(bank))
		{
			throw new IllegalArgumentException("BankAddress - invalid bank given (" + bank + 
					") - the bank must be between 0 and " + RomConstants.NUMBER_OF_BANKS + 
					" or the reserved UNASSIGNED_BANK value (" + UNASSIGNED_BANK + ")"); 
		}
		this.bank = bank;
	}
	
	public void setAddressInBank(short addressInBank) 
	{
		if (!isAddressInBankInRange(bank, addressInBank, BankAddressLimitType.WITHIN_BANK))
		{
			throw new IllegalArgumentException("BankAddress - invalid addressInBank given (" + addressInBank + 
					") - the bank must be between 0 and " + RomConstants.BANK_SIZE +
					" or the reserved UNASSIGNED_ADDRESS value (" + UNASSIGNED_ADDRESS + ")"); 
		}
		this.addressInBank = addressInBank;
	}
	
	private static boolean isBankInRange(int bankToCheck)
	{
		return (bankToCheck >= 0 && bankToCheck < RomConstants.NUMBER_OF_BANKS) ||
				bankToCheck == UNASSIGNED_BANK;
	}
	
	private static boolean isAddressInBankInRange(int bankToCheck, int addrToCheck, BankAddressLimitType type)
	{
		switch (type)
		{
		default:
		case IN_VALID_RANGES:
			while (!isAddressInBankInRange(bankToCheck, addrToCheck, BankAddressLimitType.WITHIN_BANK))
			{
				bankToCheck++;
				addrToCheck -= RomConstants.BANK_SIZE;
			}
			return isBankInRange(bankToCheck);
		case WITHIN_BANK:
		case WITHIN_BANK_OR_START_OF_NEXT:
			return (addrToCheck == UNASSIGNED_ADDRESS ||
					addrToCheck >= 0 && 
					(addrToCheck < RomConstants.BANK_SIZE || 
							(type ==  BankAddressLimitType.WITHIN_BANK_OR_START_OF_NEXT && addrToCheck == RomConstants.BANK_SIZE)));
		}
	}
	
	public boolean isFullAddress() 
	{
		return bank != UNASSIGNED_BANK && addressInBank != UNASSIGNED_ADDRESS;
	}

	public boolean isBankUnassigned() 
	{
		return bank == UNASSIGNED_BANK;
	}

	public boolean isAddressInBankUnassigned() 
	{
		return addressInBank == UNASSIGNED_ADDRESS;
	}

	public boolean isSameBank(BankAddress toCheck) 
	{
		return bank == toCheck.bank;
	}

	public boolean offsetFits(int offset) 
	{
		return isAddressInBankInRange(bank, this.addressInBank + offset, BankAddressLimitType.IN_VALID_RANGES);
	}
	
	public boolean offsetFits(int offset, BankAddressLimitType type) 
	{
		return isAddressInBankInRange(bank, this.addressInBank + offset, type);
	}

	public byte getBank()
	{
		return bank;
	}

	public short getAddressInBank()
	{
		return addressInBank;
	}
	
	public int getDifference(BankAddress other)
	{
		return getDifference(other, BankAddressToUseType.BANK_AND_BANK_IN_ADDRESS);
	}
	
	public int getDifference(BankAddress other, BankAddressToUseType whatToDiff)
	{
		switch (whatToDiff)
		{
		default: // Default to all
		case BANK_AND_BANK_IN_ADDRESS:
			return (other.bank - bank) * RomConstants.BANK_SIZE +
					other.addressInBank - addressInBank;
		case BANK_ONLY:
			return (other.bank - bank) * RomConstants.BANK_SIZE;
		case ADDRESS_IN_BANK_ONLY:
			return other.addressInBank - addressInBank;
		}
	}

	public AddressRange getDifferenceAsRange(BankAddress other)
	{
		return getDifferenceAsRange(other, BankAddressToUseType.BANK_AND_BANK_IN_ADDRESS);
	}
	
	public AddressRange getDifferenceAsRange(BankAddress other, BankAddressToUseType whatToDiff)
	{
		int globalAddress = 0;
		int otherGlobalAddress = 0;
		
		switch (whatToDiff)
		{
		default: // Default to both
		case BANK_AND_BANK_IN_ADDRESS:
			globalAddress = RomUtils.convertToGlobalAddress(bank, addressInBank);
			otherGlobalAddress = RomUtils.convertToGlobalAddress(other.bank, other.addressInBank);
			break;
		case BANK_ONLY:
			globalAddress = RomUtils.convertToGlobalAddress(bank, (short) 0);
			otherGlobalAddress = RomUtils.convertToGlobalAddress(other.bank, (short) 0);
			break;
		case ADDRESS_IN_BANK_ONLY:		
			globalAddress = addressInBank;
			otherGlobalAddress = other.addressInBank;
			break;
		}
		
		if (globalAddress > otherGlobalAddress)
		{
			return new AddressRange(otherGlobalAddress, globalAddress);
		}
		else if (globalAddress < otherGlobalAddress)
		{
			return new AddressRange(globalAddress, otherGlobalAddress);
		}

		return new AddressRange(globalAddress, globalAddress + 1);
	}
	
    @Override
    public boolean equals(Object o) 
    {
    	return equals(o, BankAddressToUseType.BANK_AND_BANK_IN_ADDRESS);
    }
    
    public boolean equals(Object o, BankAddressToUseType whatToCheck)
    {
        // If the object is compared with itself then return true 
        if (o == this) {
            return true;
        }
 
        // Check if it is an instance of BankAddress
        if (!(o instanceof BankAddress)) 
        {
            return false;
        }
        
        // Compare the data and return accordingly
        BankAddress ba = (BankAddress) o;
        
        switch (whatToCheck)
        {
        default:
        case BANK_AND_BANK_IN_ADDRESS:
            return Byte.compare(bank, ba.bank) == 0 && Short.compare(addressInBank, ba.addressInBank) == 0;
        case BANK_ONLY:
            return Byte.compare(bank, ba.bank) == 0;
    	case ADDRESS_IN_BANK_ONLY:
    		return Short.compare(addressInBank, ba.addressInBank) == 0;
        }
    }
    
    @Override
    // Not used but to added since equals was overridden
    public int hashCode() 
    {
    	return bank << 16 + addressInBank;
    }

	@Override
	public String toString()
	{
		return String.format("0x%x:%4x(%d)", bank, addressInBank + 0x4000, RomUtils.convertToGlobalAddress(bank, addressInBank));
	}
}
