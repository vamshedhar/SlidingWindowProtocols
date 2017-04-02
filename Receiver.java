/**
 *	
 * @author Vamshedhar Reddy Chintala
 */
import java.io.*;
import java.net.*;
import java.util.*;

class Receiver
{

	// ACK loss probability
	public static double LOST_ACK = 0.05;

	public static void main(String args[]) throws Exception
	{

		// Validate arguments 
		if (args.length != 1) {
            System.out.println("Invalid Format!");
            System.out.println("Expected Format: java Receiver <portNum>");
            return;
        }

        int receiverPort;

        // check if bit length is a valid integer
		try{
			receiverPort = Integer.parseInt(args[0]);
		} catch(NumberFormatException e){
			System.out.println("Invalid Port Number");
			return;
		}

		// Open Socket
		DatagramSocket serverSocket = new DatagramSocket(receiverPort);

		System.out.println("Server Started: Waiting for packets!!");

		// Receive Protocol details from the Sender
		byte[] inputData = new byte[1024];
		DatagramPacket inputDataPacket = new DatagramPacket(inputData, inputData.length);
		serverSocket.receive(inputDataPacket);

		String inputDataText = new String(inputDataPacket.getData());

		String[] inputDataArray = inputDataText.split(",");

		String protocol = inputDataArray[0].trim();

		int bitsOfSqeunceNo = Integer.parseInt(inputDataArray[1].trim());
		int WINDOW_SIZE = Integer.parseInt(inputDataArray[2].trim());
		int MSS = Integer.parseInt(inputDataArray[3].trim());

		// Total Sequence No's allowed
		int lastSeqNo = (int) (Math.pow(2.0, (double) bitsOfSqeunceNo));

		System.out.println("***********************************");
		System.out.println("Receiving Protocol - " + protocol);
		System.out.println("***********************************");


		if (protocol.equals("GBN")) {
			byte[] receivedPacket = new byte[16 + 1 + 4 + MSS];
			BufferedWriter writer = new BufferedWriter(new FileWriter("output.txt"));

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

				if (packet.isValidPacket()) {
					if (waitingForPacket % lastSeqNo == packet.getSeqNo()) {
						System.out.println("Received Segment " + packet.getSeqNo() + ";");
						end = packet.getLast();
						String text = new String(packet.getData());
						writer.write(text);
						waitingForPacket++;
					} else{
						System.out.println("Discarded " + packet.getSeqNo() + "; Out of Order Segment Received; Expecting " + waitingForPacket % lastSeqNo);
					}
				} else{
					System.out.println("Discarded " + packet.getSeqNo() + "; Checksum Error;");
				}

				// Create and ACK Packet with the sequence number
				RDTAck ackPacket = new RDTAck((waitingForPacket - 1) % lastSeqNo);
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
			}

			writer.flush();
			writer.close();

			serverSocket.close();
		} else if(protocol.equals("SR")){
			HashMap<Integer, RDTPacket> receivedPackets = new HashMap<Integer, RDTPacket>();
		
			byte[] receivedPacket = new byte[16 + 1 + 4 + MSS];
			BufferedWriter writer = new BufferedWriter(new FileWriter("output.txt"));

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
		} else{
			serverSocket.close();
		}

		
		
	}
}

