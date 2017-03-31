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

		while(!end)
		{

			// Receive Packet
			DatagramPacket receivePacket = new DatagramPacket(receivedPacket, receivedPacket.length);
			serverSocket.receive(receivePacket);
			byte[] receiveData = receivePacket.getData();

			// Convert Packet data to packet object
			RDTPacket packet = new RDTPacket(receiveData, receivePacket.getLength());

			System.out.println("Received Segment " + packet.getSeqNo() + "; Packet Size: " + receivePacket.getLength());
			
			end = packet.getLast();
			String text = new String(packet.getData());
			writer.write(text);

			// Create and ACK Packet with the sequence number
			RDTAck ackPacket = new RDTAck(packet.getSeqNo());
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

