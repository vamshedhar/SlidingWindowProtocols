import java.io.*;
import java.net.*;
import java.util.Arrays;

class Receiver
{
	public static void main(String args[]) throws Exception
	{
		DatagramSocket serverSocket = new DatagramSocket(9876);
		byte[] receivedPacket = new byte[500 + 4 + 1];
		BufferedWriter writer = new BufferedWriter(new FileWriter("output.txt"));

		System.out.println("Server Started: Waiting for packets!!");

		boolean end = false;

		int waitingForAck == -1;

		while(!end)
		{

			// Receive Packet
			DatagramPacket receivePacket = new DatagramPacket(receivedPacket, receivedPacket.length);
			serverSocket.receive(receivePacket);
			byte[] receiveData = receivePacket.getData();

			// Convert Packet data to packet object
			RDTPacket packet = new RDTPacket(receiveData, receivePacket.getLength());

			if (waitingForAck == -1) {
				waitingForAck = packet.getSeqNo();
			}

			if (waitingForAck == packet.getSeqNo()) {
				System.out.println("Received Segment " + packet.getSeqNo() + ";");
				end = packet.getLast();
				String text = new String(packet.getData());
				writer.write(text);
				waitingForAck++;
			} else{
				System.out.println("Discarded " + packet.getSeqNo() + "; Out of Order Segment Received; Expecting " + waitingForAck);
			}

			// Create and ACK Packet with the sequence number
			RDTAck ackPacket = new RDTAck(waitingForAck - 1);
			byte[] ackData = ackPacket.generatePacket();

			Thread.sleep(1000);

			DatagramPacket sendACK = new DatagramPacket(ackData, ackData.length, receivePacket.getAddress(), receivePacket.getPort());
			serverSocket.send(sendACK);

			System.out.println("ACK Sent: " + ackPacket.getSeqNo());
			System.out.println("");
		}

		writer.flush();
		writer.close();

		serverSocket.close();
	}
}

