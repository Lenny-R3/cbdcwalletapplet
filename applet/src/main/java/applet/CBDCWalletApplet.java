package applet;

import javacard.framework.*;
import javacard.security.RandomData;

public class CBDCWalletApplet extends Applet implements MultiSelectable {
	// codes of INS byte in the command APDU header
	final static byte CREDIT = (byte) 0x30;
	final static byte DEBIT = (byte) 0x40;
	final static byte GET_BALANCE = (byte) 0x50;

	public static void install(byte[] bArray, short bOffset, byte bLength) {
		new CBDCWalletApplet(bArray, bOffset, bLength);
	}

	// maximum balance
	final static short MAX_BALANCE = 0x7FFF;
	// maximum transaction amount
	final static byte MAX_TRANSACTION_AMOUNT = 127;

	// signal invalid transaction amount
	// amount > MAX_TRANSACTION_AMOUNT or amount < 0
	final static short SW_INVALID_TRANSACTION_AMOUNT = 0x6A83;

	// signal that the balance exceed the maximum
	final static short SW_EXCEED_MAXIMUM_BALANCE = 0x6A84;
	// signal the the balance becomes negative
	final static short SW_NEGATIVE_BALANCE = 0x6A85;

	/* instance variables declaration */
	public static short balance = 5;

	public CBDCWalletApplet(byte[] buffer, short offset, byte length) {
		register();
	}

	public void process(APDU apdu) {
		byte[] apduBuffer = apdu.getBuffer();
//		byte cla = apduBuffer[ISO7816.OFFSET_CLA];
		byte ins = apduBuffer[ISO7816.OFFSET_INS];
//		short lc = (short) apduBuffer[ISO7816.OFFSET_LC];
//		short p1 = (short) apduBuffer[ISO7816.OFFSET_P1];
//		short p2 = (short) apduBuffer[ISO7816.OFFSET_P2];

		switch (ins) {
			case GET_BALANCE:
				getBalance(apdu);
				return;
			case DEBIT:
				debit(apdu);
				return;
			case CREDIT:
				credit(apdu);
				return;
			default:
				ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
		}

	}

	public boolean select(boolean b) {
		return true;
	}

	public void deselect(boolean b) {

	}

	private void getBalance(APDU apdu) {

		byte[] buffer = apdu.getBuffer();

		// inform system that the applet has finished
		// processing the command and the system should
		// now prepare to construct a response APDU
		// which contains data field
		short le = apdu.setOutgoing();

		if (le < 2)
			ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);

		//informs the CAD the actual number of bytes
		//returned
		apdu.setOutgoingLength((byte) 2);

		// move the balance data into the APDU buffer
		// starting at the offset 0
		buffer[0] = (byte) (balance >> 8);
		buffer[1] = (byte) (balance & 0xFF);

		// send the 2-byte balance at the offset
		// 0 in the apdu buffer
		apdu.sendBytes((short) 0, (short) 2);

	} // end of getBalance method

	private void credit(APDU apdu) {

		byte[] buffer = apdu.getBuffer();

		// Lc byte denotes the number of bytes in the
		// data field of the command APDU
		byte numBytes = buffer[ISO7816.OFFSET_LC];

		// indicate that this APDU has incoming data
		// and receive data starting from the offset
		// ISO7816.OFFSET_CDATA following the 5 header
		// bytes.
		byte byteRead =
				(byte) (apdu.setIncomingAndReceive());

		// it is an error if the number of data bytes
		// read does not match the number in Lc byte
		if ((numBytes != 1) || (byteRead != 1))
			ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);

		// get the credit amount
		byte creditAmount = buffer[ISO7816.OFFSET_CDATA];

		// check the credit amount
		if ((creditAmount > MAX_TRANSACTION_AMOUNT)
				|| (creditAmount < 0))
			ISOException.throwIt(SW_INVALID_TRANSACTION_AMOUNT);

		// check the new balance
		if ((short) (balance + creditAmount) > MAX_BALANCE)
			ISOException.throwIt(SW_EXCEED_MAXIMUM_BALANCE);

		// credit the amount
		balance = (short) (balance + creditAmount);
	}


	private void debit(APDU apdu) {

		byte[] buffer = apdu.getBuffer();

		byte numBytes =
				(byte)(buffer[ISO7816.OFFSET_LC]);

		byte byteRead =
				(byte)(apdu.setIncomingAndReceive());

		if ( ( numBytes != 1 ) || (byteRead != 1) )
			ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);

		// get debit amount
		byte debitAmount = buffer[ISO7816.OFFSET_CDATA];

		// check debit amount
		if ( ( debitAmount > MAX_TRANSACTION_AMOUNT)
				||  ( debitAmount < 0 ) )
			ISOException.throwIt(SW_INVALID_TRANSACTION_AMOUNT);

		// check the new balance
		if ( (short)( balance - debitAmount ) < (short)0 )
			ISOException.throwIt(SW_NEGATIVE_BALANCE);

		balance = (short) (balance - debitAmount);

	} // end of debit method

}