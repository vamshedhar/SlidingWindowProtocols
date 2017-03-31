import java.util.Arrays;
import java.nio.ByteBuffer;


public class RDTPacket {

	public int seq;
	
	public byte[] data;
	
	public boolean last;

	public RDTPacket(int seq, byte[] data, boolean last) {
		this.seq = seq;
		this.data = data;
		this.last = last;
	}

	public RDTPacket(byte[] packetData) {
		this.last = Integer.valueOf(packetData[0]) == 1 ? true : false;

		byte[] sequenceNo = Arrays.copyOfRange(packetData, 1, 5);
		ByteBuffer wrapped = ByteBuffer.wrap(sequenceNo);

		this.seq = wrapped.getInt();

		this.data = Arrays.copyOfRange(packetData, 5, packetData.length);
	}

	public int getSeq() {
		return seq;
	}

	public void setSeq(int seq) {
		this.seq = seq;
	}

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}

	public boolean isLast() {
		return last;
	}

	public void setLast(boolean last) {
		this.last = last;
	}

	public byte[] generatePacket(){

		byte[] sequenceNo = ByteBuffer.allocate(4).putInt(this.seq).array();
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
		return "UDPPacket [seq=" + seq + ", data=" + Arrays.toString(data)
				+ ", last=" + last + "]";
	}
	
}