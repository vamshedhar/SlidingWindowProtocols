import java.io.*;
import java.net.*;
import java.util.*;

class ReceiverSR
{

	public static double LOST_ACK = 0.00;


	public static void main(String args[]) throws Exception
	{

		HashMap<Integer, RDTPacket> receivedPackets = new HashMap<Integer, RDTPacket>();
		DatagramSocket serverSocket = new DatagramSocket(9876);
		byte[] receivedPacket = new byte[500 + 4 + 1];
		BufferedWriter writer = new BufferedWriter(new FileWriter("output.txt"));

		System.out.println("Server Started: Waiting for packets!!");

		boolean end = false;

		int waitingForPacket = 0;

		while(!end)
		{

			// Receive Packet
			DatagramPacket receivePacket = new DatagramPacket(receivedPacket, receivedPacket.length);
			serverSocket.receive(receivePacket);
			byte[] receiveData = receivePacket.getData();

			// Convert Packet data to packet object
			RDTPacket packet = new RDTPacket(receiveData, receivePacket.getLength());

			System.out.println("Received Segment " + packet.getSeqNo() + ";");

			if (waitingForPacket != packet.getSeqNo()) {
				System.out.println("Out of Order Segment Received; Stored to Buffer; Expecting " + waitingForPacket);
			}

			if(!receivedPackets.containsKey(packet.getSeqNo())){
				receivedPackets.put(packet.getSeqNo(), packet);
			} else{
				System.out.println("Discarded Segment " + packet.getSeqNo() + ": Duplicate Packet;");
			}
			
			while(receivedPackets.containsKey(waitingForPacket)){
				RDTPacket bufferedPacket = receivedPackets.get(waitingForPacket);
				end = packet.getLast();
				String text = new String(packet.getData());
				writer.write(text);
				System.out.println("Delivered Packet: " + waitingForPacket);
				waitingForPacket++;
			}

			// Create and ACK Packet with the sequence number
			RDTAck ackPacket = new RDTAck(packet.getSeqNo());
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

