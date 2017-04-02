/**
 *	
 * @author Vamshedhar Reddy Chintala
 */
import java.util.Arrays;
import java.nio.ByteBuffer;


public class RDTAck{
	
	private int seqNo;

	public RDTAck(int seqNo) {
		this.seqNo = seqNo;
	}

	public RDTAck(byte[] packet) {
		ByteBuffer wrapped = ByteBuffer.wrap(packet);
		this.seqNo = wrapped.getInt();
	}

	public int getSeqNo() {
		return seqNo;
	}

	public void setSeqNo(int seqNo) {
		this.seqNo = seqNo;
	}

	public byte[] generatePacket(){

		byte[] packet = ByteBuffer.allocate(4).putInt(this.seqNo).array();

		return packet;
	}

	@Override
	public String toString() {
		return "UDPAcknowledgement [seq=" + seqNo + "]";
	}

}
