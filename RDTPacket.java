import java.util.Arrays;
import java.nio.ByteBuffer;


public class RDTPacket {

	private int seqNo;
	
	private byte[] data;
	
	private boolean last;

	public RDTPacket(int seqNo, byte[] data, boolean last) {
		this.seqNo = seqNo;
		this.data = data;
		this.last = last;
	}

	public RDTPacket(byte[] packetData) {
		this.last = Integer.valueOf(packetData[0]) == 1 ? true : false;

		byte[] sequenceNo = Arrays.copyOfRange(packetData, 1, 5);
		ByteBuffer wrapped = ByteBuffer.wrap(sequenceNo);

		this.seqNo = wrapped.getInt();

		this.data = Arrays.copyOfRange(packetData, 5, packetData.length);
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

	public byte[] generatePacket(){

		byte[] sequenceNo = ByteBuffer.allocate(4).putInt(this.seqNo).array();
		byte last = (byte) (this.last ? 1 : 0);

		System.out.println("Seq Length: " + sequenceNo.length);

		byte[] packet = new byte[1 + sequenceNo.length + this.data.length];

		packet[0] = last;

		System.arraycopy(sequenceNo, 0, packet, 1, sequenceNo.length);
		System.arraycopy(this.data, 0, packet, sequenceNo.length + 1, this.data.length);

		return packet;
	}

	@Override
	public String toString() {
		return "UDPPacket [seq=" + seqNo + ", data=" + Arrays.toString(data)
				+ ", last=" + last + "]";
	}
	
}