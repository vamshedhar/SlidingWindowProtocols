/**
 *	
 * @author Vamshedhar Reddy Chintala
 */
import java.util.Arrays;
import java.nio.ByteBuffer;


public class RDTPacket {

	private int seqNo;
	
	private byte[] data;
	
	private boolean last;

	private String checksum;

	private double CHECKSUM_ERROR = 0.1;

	public RDTPacket(int seqNo, byte[] data, boolean last) {
		this.seqNo = seqNo;
		this.data = data;
		this.last = last;
	}

	public RDTPacket(byte[] packetData, int length) {
		this.checksum = new String(Arrays.copyOfRange(packetData, 0, 16));

		this.last = Integer.valueOf(packetData[16]) == 1 ? true : false;

		byte[] sequenceNo = Arrays.copyOfRange(packetData, 17, 21);
		ByteBuffer wrapped = ByteBuffer.wrap(sequenceNo);

		this.seqNo = wrapped.getInt();

		this.data = Arrays.copyOfRange(packetData, 21, length);
	}

	public int getSeqNo() {
		return seqNo;
	}

	public void setSeqNo(int seqNo) {
		this.seqNo = seqNo;
	}

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}

	public boolean getLast() {
		return last;
	}

	public void setLast(boolean last) {
		this.last = last;
	}

	public String onesComplement(String value){
		String complementValue = "";

		for(int i = 0; i < value.length(); i++){
			if (value.charAt(i) == '0') {
				complementValue += "1";
			} else{
				complementValue += "0";
			}
		}

		return complementValue;
	}

	public String onesComplementSum(String a, String b){
		int data1 = Integer.parseInt(a, 2);
		int data2 = Integer.parseInt(b, 2);

		int sum = data1 + data2;

		String result = Integer.toString(sum, 2);

		while(result.length() > 16 && result.charAt(0) == '1'){
			data1 = Integer.parseInt(result.substring(1), 2);
			data2 = Integer.parseInt(result.substring(0, 1), 2);

			sum = data1 + data2;

			result = Integer.toString(sum, 2);
		}

		return result;
	}

	public String generateChecksum(byte[] packetData){

		String[] binary16Data = new String[packetData.length / 2];

		for(int i = 0; i < packetData.length; i += 2){
			if (i + 1 >= packetData.length) {
				break;
			}

			int value = ((packetData[i] & 0xff) << 8) + (packetData[i + 1] & 0xff);

			binary16Data[i / 2] = Integer.toString(value, 2);
		}

		String onesComplementSumValue = binary16Data[0];

		for(int j = 1; j < binary16Data.length; j++){
			onesComplementSumValue = onesComplementSum(onesComplementSumValue, binary16Data[j]);
		}

		if (onesComplementSumValue.length() < 16) {
			int length = onesComplementSumValue.length();
			for(int i = 0; i < 16 - length; i++){
				onesComplementSumValue = "0" + onesComplementSumValue;
			}
		}

		String checksum = onesComplement(onesComplementSumValue);

		return checksum;
	}

	public byte[] generatePacketToHash(){
		byte[] sequenceNo = ByteBuffer.allocate(4).putInt(this.seqNo).array();
		byte last = (byte) (this.last ? 1 : 0);

		byte[] packetToHash = new byte[1 + sequenceNo.length + this.data.length];

		packetToHash[0] = last;

		System.arraycopy(sequenceNo, 0, packetToHash, 1, sequenceNo.length);
		System.arraycopy(this.data, 0, packetToHash, sequenceNo.length + 1, this.data.length);

		return packetToHash;
	}

	public byte[] generatePacket(){

		byte[] packetToHash = generatePacketToHash();

		checksum = generateChecksum(packetToHash);

		if (Math.random() <= CHECKSUM_ERROR) {
			addErrorToChecksum();
		}

		byte[] checksumBytes = checksum.getBytes();

		byte[] packet = new byte[16 + packetToHash.length];

		System.arraycopy(checksumBytes, 0, packet, 0, checksumBytes.length);
		System.arraycopy(packetToHash, 0, packet, checksumBytes.length, packetToHash.length);

		return packet;
	}

	public void addErrorToChecksum(){
		if (this.checksum.charAt(0) == '0') {
			this.checksum = "1" + this.checksum.substring(1);
		} else{
			this.checksum = "0" + this.checksum.substring(1);
		}
	}

	public boolean isValidPacket(){
		byte[] packetToHash = generatePacketToHash();

		String calculatedChecksum = generateChecksum(packetToHash);

		return this.checksum.equals(calculatedChecksum);
	}

	@Override
	public String toString() {
		return "UDPPacket [seq=" + seqNo + ", data=" + Arrays.toString(data)
				+ ", last=" + last + "]";
	}
	
}