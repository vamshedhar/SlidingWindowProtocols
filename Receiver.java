import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.nio.charset.Charset;

class Receiver
{

	public static double LOST_ACK = 0.05;

	public static void main(String args[]) throws Exception
	{

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

		DatagramSocket serverSocket = new DatagramSocket(receiverPort);

		byte[] inputData = new byte[1024];
		DatagramPacket inputDataPacket = new DatagramPacket(inputData, inputData.length);
		serverSocket.receive(inputDataPacket);

		String inputDataText = new String(inputDataPacket.getData());

		String[] inputDataArray = inputDataText.split(",");

		String protocol = inputDataArray[0].trim();

		int bitsOfSqeunceNo = Integer.parseInt(inputDataArray[1].trim());
		int WINDOW_SIZE = Integer.parseInt(inputDataArray[2].trim());
		int MSS = Integer.parseInt(inputDataArray[3].trim());

		System.out.println("Receiving Protocol: " + protocol);


		int lastSeqNo = (int) (Math.pow(2.0, (double) bitsOfSqeunceNo));

		
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
	}
}

