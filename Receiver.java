import java.io.*;
import java.net.*;
import java.util.Arrays;

class Receiver
{
	public static void main(String args[]) throws Exception
	{
		DatagramSocket serverSocket = new DatagramSocket(9876);
		byte[] receivedPacket = new byte[1024];
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
			RDTPacket packet = new RDTPacket(receiveData);
			end = packet.getLast();

			String sentence = new String(packet.getData());
			System.out.println("Received Packet: " + packet.getSeqNo());
			System.out.println("Last Packet: " + end);
			writer.write(sentence);
			InetAddress IPAddress = receivePacket.getAddress();
			int port = receivePacket.getPort();


			RDTAck ackPacket = new RDTAck(packet.getSeqNo());
			byte[] ackData = ackPacket.generatePacket();

			Thread.sleep(1000);
			
			DatagramPacket sendPacket = new DatagramPacket(ackData, ackData.length, IPAddress, port);
			serverSocket.send(sendPacket);
			System.out.println("Sent ACK: " + ackData.getSeqNo());
			System.out.println("");
		}

		writer.flush();
		writer.close();

		serverSocket.close();
	}
}

