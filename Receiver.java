import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.nio.charset.Charset;

class Receiver
{

	public static double LOST_ACK = 0.05;

	public static void main(String args[]) throws Exception
	{

		// byte[] toHash = "QWERTYUIOPASDFGHJKLZXCVBNM".getBytes();

		// System.out.println(toHash[0]);
		// System.out.println(toHash[1] & 0xff);

		// int value = ((toHash[0] & 0xff) << 8) + (toHash[1] & 0xff);

		// System.out.println(value);

		// String binary = "0" + Integer.toString(value, 2);
		// System.out.println(binary.getBytes().length);


		int bitsOfSqeunceNo = Sender.bitsOfSqeunceNo;
		int MSS = Sender.MSS;

		int lastSeqNo = (int) (Math.pow(2.0, (double) bitsOfSqeunceNo));

		DatagramSocket serverSocket = new DatagramSocket(9876);
		byte[] receivedPacket = new byte[16 + 1 + 4 + MSS];
		BufferedWriter writer = new BufferedWriter(new FileWriter("output.txt"));

		System.out.println("Server Started: Waiting for packets!!");

		boolean end = false;

		int waitingForAck = 0;

		while(!end)
		{

			// Receive Packet
			DatagramPacket receivePacket = new DatagramPacket(receivedPacket, receivedPacket.length);
			serverSocket.receive(receivePacket);
			byte[] receiveData = receivePacket.getData();

			// Convert Packet data to packet object
			RDTPacket packet = new RDTPacket(receiveData, receivePacket.getLength());

			if (packet.isValidPacket()) {
				if (waitingForAck % lastSeqNo == packet.getSeqNo()) {
					System.out.println("Received Segment " + packet.getSeqNo() + ";");
					end = packet.getLast();
					String text = new String(packet.getData());
					writer.write(text);
					waitingForAck++;
				} else{
					System.out.println("Discarded " + packet.getSeqNo() + "; Out of Order Segment Received; Expecting " + waitingForAck % lastSeqNo);
				}
			} else{
				System.out.println("Discarded " + packet.getSeqNo() + "; Checksum Error;");
			}

			// Create and ACK Packet with the sequence number
			RDTAck ackPacket = new RDTAck((waitingForAck - 1) % lastSeqNo);
			byte[] ackData = ackPacket.generatePacket();

			Thread.sleep(1000);

			DatagramPacket sendACK = new DatagramPacket(ackData, ackData.length, receivePacket.getAddress(), receivePacket.getPort());

			if (Math.random() > LOST_ACK) {
				serverSocket.send(sendACK);
			} else{
				System.out.println("Lost ACK");
			}

			System.out.println("ACK Sent: " + ackPacket.getSeqNo());
			System.out.println("");
		}

		writer.flush();
		writer.close();

		serverSocket.close();
	}
}

