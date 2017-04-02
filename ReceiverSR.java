/**
 *	
 * @author Vamshedhar Reddy Chintala
 */
import java.io.*;
import java.net.*;
import java.util.*;

class ReceiverSR
{

	public static double LOST_ACK = 0.05;

	public static void main(String args[]) throws Exception
	{
		int bitsOfSqeunceNo = SenderSR.bitsOfSqeunceNo;
		int MSS = SenderSR.MSS;
		int WINDOW_SIZE = SenderSR.WINDOW_SIZE;

		int lastSeqNo = (int) (Math.pow(2.0, (double) bitsOfSqeunceNo));

		HashMap<Integer, RDTPacket> receivedPackets = new HashMap<Integer, RDTPacket>();
		DatagramSocket serverSocket = new DatagramSocket(9876);
		byte[] receivedPacket = new byte[16 + 1 + 4 + MSS];
		BufferedWriter writer = new BufferedWriter(new FileWriter("output.txt"));

		System.out.println("Server Started: Waiting for packets!!");

		boolean end = false;

		int waitingForPacket = 0;

		Queue<Integer> WINDOW = new ArrayDeque<>();

		for(int i = 0; i < WINDOW_SIZE; i++){
			WINDOW.add(i);
		}

		while(!end)
		{

			// Receive Packet
			DatagramPacket receivePacket = new DatagramPacket(receivedPacket, receivedPacket.length);
			serverSocket.receive(receivePacket);
			byte[] receiveData = receivePacket.getData();

			// Convert Packet data to packet object
			RDTPacket packet = new RDTPacket(receiveData, receivePacket.getLength());

			if (packet.isValidPacket()) {
	
				System.out.println("Received Segment " + packet.getSeqNo() + ";");

				ArrayList<Integer> WINDOW_LIST = new ArrayList<Integer>(WINDOW);

				int actualSeqNo = waitingForPacket + WINDOW_LIST.indexOf(packet.getSeqNo());

				if(!receivedPackets.containsKey(actualSeqNo)){
					receivedPackets.put(actualSeqNo, packet);

					if (waitingForPacket != actualSeqNo) {
						System.out.println("Out of Order Segment Received; Stored to Buffer; Expecting " + waitingForPacket % lastSeqNo);
					}
				} else{
					System.out.println("Discarded Segment " + packet.getSeqNo() + ": Duplicate Packet;");
				}
				
				while(receivedPackets.containsKey(waitingForPacket)){
					RDTPacket bufferedPacket = receivedPackets.get(waitingForPacket);
					end = bufferedPacket.getLast();
					String text = new String(bufferedPacket.getData());
					writer.write(text);
					System.out.println("Delivered Packet: " + waitingForPacket % lastSeqNo);
					WINDOW.add((waitingForPacket + WINDOW_SIZE) % lastSeqNo);
					waitingForPacket++;
					WINDOW.remove();
				}

				// Create and ACK Packet with the sequence number
				RDTAck ackPacket = new RDTAck(packet.getSeqNo());
				byte[] ackData = ackPacket.generatePacket();

				Thread.sleep(100);

				DatagramPacket sendACK = new DatagramPacket(ackData, ackData.length, receivePacket.getAddress(), receivePacket.getPort());

				if (Math.random() > LOST_ACK) {
					serverSocket.send(sendACK);
				} else{
					System.out.println("Lost ACK");
				}

				System.out.println("ACK Sent: " + ackPacket.getSeqNo());
				System.out.println("");
			} else{
				System.out.println("Discarded " + packet.getSeqNo() + "; Checksum Error;");
			}

		}

		writer.flush();
		writer.close();

		serverSocket.close();
	}
}

