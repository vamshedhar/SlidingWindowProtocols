/**
 *	
 * @author Vamshedhar Reddy Chintala
 */

import java.io.*;
import java.net.*;
import java.util.*;

class Sender
{

	// File thats transfered as packets
	public static String transferFilename = "fulldata.txt";

	// Packet Loss Probability
	public static double LOST_PACKET = 0.1;

	public static void main(String args[]) throws Exception
	{

		// Validate arguments 
		if (args.length != 3) {
            System.out.println("Invalid Format!");
            System.out.println("Expected Format: java Sender <filename or filepath> <portNum> <packetCount>");
            return;
        }

        String parametersFileName = args[0];

        int receiverPort;

        // check if bit length is a valid integer
		try{
			receiverPort = Integer.parseInt(args[1]);
		} catch(NumberFormatException e){
			System.out.println("Invalid Port Number");
			return;
		}

		int packetCount;

		// check if Packet Count is a valid integer
		try{
			packetCount = Integer.parseInt(args[2]);
		} catch(NumberFormatException e){
			System.out.println("Invalid Packet Count");
			return;
		}


		// Read protocol details from the input file

		BufferedReader reader = new BufferedReader(new FileReader(parametersFileName));
		List<String> lines = new ArrayList<String>();

		String line;

		while((line = reader.readLine()) != null) {
		    lines.add(line.trim());
		}

		reader.close();

		String protocol = lines.get(0);

		int bitsOfSqeunceNo = Integer.parseInt(lines.get(1).split(" ")[0].trim());

		int WINDOW_SIZE = Integer.parseInt(lines.get(1).split(" ")[1].trim());

		int TIMEOUT = Integer.parseInt(lines.get(2)) / 1000;

		int MSS = Integer.parseInt(lines.get(3));


		// Create a Socket
		DatagramSocket clientSocket = new DatagramSocket();
		InetAddress IPAddress = InetAddress.getByName("localhost");


		// Send protocol details from input file to Receiver
		String dataToReceiver = protocol + "," + bitsOfSqeunceNo + "," + WINDOW_SIZE + "," + MSS;
		byte[] inputData = dataToReceiver.getBytes();

		DatagramPacket inputDataPacket = new DatagramPacket(inputData, inputData.length, IPAddress, receiverPort);
		clientSocket.send(inputDataPacket);


		// Read data from file to transfer into bytes
		System.out.println("Sending file: " + transferFilename);
		File file = new File(transferFilename);
		FileInputStream inputStream = new FileInputStream(file);
		byte[] data = new byte[(int) file.length()];
		inputStream.read(data);
		inputStream.close();

		int FILE_SIZE = data.length;
		System.out.println("FILE SIZE: " + FILE_SIZE + " bytes");

		// Total Sequence No's allowed
		int lastSeqNo = (int) (Math.pow(2.0, (double) bitsOfSqeunceNo));

		// Switch according to the protocol

		System.out.println("***********************************");
		System.out.println("Using Protocol - " + protocol);
		System.out.println("***********************************");

		System.out.println("");

		if (protocol.equals("GBN")) {

			// Get Last packet no. from the size of the file
			int lastPacketNo = FILE_SIZE / MSS;

			// To check for the last packet
			boolean endOfFile = false;

			// Actual Seq No. of next packet to be sent
			int nextPacket = 0;

			// Actual Seq No. of expecting ACK
			int waitingForAck = 0;

			// Start Timer for latest packet
			long startTime = System.currentTimeMillis();

			// Window of packets sent and of which ACK is expected
			Queue<Integer> WINDOW = new ArrayDeque<>();

			// Intializing with first sent packets Seq No's
			for(int i = 0; i < WINDOW_SIZE; i++){
				WINDOW.add(i);
			}

			// End loop when last packets ACK is received
			while (waitingForAck <= lastPacketNo) {

				// Check if window space is free and send packets in free space
				while(nextPacket - waitingForAck < WINDOW_SIZE && !endOfFile){

					// Start timer for the sent packet
					startTime = System.currentTimeMillis();
					int startByte = nextPacket * MSS;
					int endByte = (nextPacket + 1) * MSS;

					if (endByte > FILE_SIZE) {
						System.out.println("End of File");
						endOfFile = true;
					}

					// Split transfering file into packets of size equal to MSS Bytes
					byte[] partData = Arrays.copyOfRange(data, nextPacket * MSS, endByte > FILE_SIZE ? FILE_SIZE : endByte);

					// Calcuate pack seq no. from the actual seq no.
					int sqeNo = nextPacket % lastSeqNo;

					// Create transfer packet from the data
					RDTPacket dataPacket = new RDTPacket(sqeNo, partData, endOfFile);

					// Generate bytes array from the packet data
					byte[] sendData = dataPacket.generatePacket();

					DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, receiverPort);

					// Induce Lost packet Error. Says Sending packet but actually the packet is lost
					if (Math.random() > LOST_PACKET) {
						clientSocket.send(sendPacket);
					} else{
						System.out.print("Lost Packet: " + sqeNo + "; ");
					}

					// Message for sending packet
					System.out.println("Sending " + sqeNo + ";");
					nextPacket++;
				}

				// Create packet to receive data
				byte[] receiveData = new byte[4];
				DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

				try{

					// Calculate time left from the actual start time
					long TIMER = TIMEOUT - (System.currentTimeMillis() - startTime);

					// Raise timeout if timer is less than zero
					if (TIMER < 0) {
						throw new SocketTimeoutException();
					}

					// Set timeout to socket for receiving ack
					clientSocket.setSoTimeout((int) TIMER);

					// Receive ack
					clientSocket.receive(receivePacket);

					// Decode received ack to get Seq No
					RDTAck ackPacket = new RDTAck(receivePacket.getData());

					// Adding a delay in response to show a clear flow of things
					Thread.sleep(100);
					System.out.println("");
					System.out.println("Received ACK: " + ackPacket.getSeqNo());

					// Check if ACK received is part of the window
					if (WINDOW.contains(ackPacket.getSeqNo())){

						// If ACK received is of packet after what we are expecting. Then account for LOST ACK and slide window by required size
						while(ackPacket.getSeqNo() != WINDOW.poll()){
							startTime = System.currentTimeMillis();
							WINDOW.add((waitingForAck + WINDOW_SIZE) % lastSeqNo);
							waitingForAck++;
						}
						WINDOW.add((waitingForAck + WINDOW_SIZE) % lastSeqNo);
						waitingForAck++;
					}
				} catch(SocketTimeoutException e){

					// Expected packet not delivered. Re-Send that and all packets after that
					String message = "Packet " + waitingForAck % lastSeqNo + ": Timer expired; Resending";

					for(int i = waitingForAck; i < nextPacket; i++){

						int sqeNo = i % lastSeqNo;

						message += (" " + sqeNo);

						int startByte = i * MSS;
						int endByte = (i + 1) * MSS;

						byte[] partData = Arrays.copyOfRange(data, startByte, endByte > FILE_SIZE ? FILE_SIZE : endByte);

						RDTPacket dataPacket = new RDTPacket(sqeNo, partData, endByte > FILE_SIZE);

						byte[] sendData = dataPacket.generatePacket();

						DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, receiverPort);

						// Induce lost packet Error
						if (Math.random() > LOST_PACKET) {
							clientSocket.send(sendPacket);
						}else{
							System.out.println("Lost Packet: " + sqeNo);
						}
					}

					// Restart Timer
					startTime = System.currentTimeMillis();

					message += "; Timer started";

					System.out.println(message);
					System.out.println("");
				}
				
			}

			// Close Socket
			clientSocket.close();
		} else if(protocol.equals("SR")){

			// Hashmap to store timers fro sending packets
			HashMap<Integer, Long> sentPacketTimers = new HashMap<Integer, Long>();

			// Hashmap to store received ACK's
			HashMap<Integer, RDTAck> receivedAcks = new HashMap<Integer, RDTAck>();

			// Find last Packet No.
			int lastPacketNo = FILE_SIZE / MSS;

			// To check last packet
			boolean endOfFile = false;

			// Actual Seq No. of the sending packet
			int nextPacket = 0;

			// Actual Seq No. of expecting ACK
			int waitingForAck = 0;

			// Create a Window of availale slots to send packets
			Queue<Integer> WINDOW = new ArrayDeque<>();

			// Initialize the window of packets sent
			for(int i = 0; i < WINDOW_SIZE; i++){
				WINDOW.add(i);
			}

			// Send packets till last packet's ack is received 
			while (waitingForAck <= lastPacketNo) {

				// Check is window is available to send packets
				while(nextPacket - waitingForAck < WINDOW_SIZE && !endOfFile){

					// Store sent time of the packets
					sentPacketTimers.put(nextPacket, System.currentTimeMillis());

					int startByte = nextPacket * MSS;
					int endByte = (nextPacket + 1) * MSS;

					// Check for the last packet
					if (endByte > FILE_SIZE) {
						System.out.println("End of File");
						endOfFile = true;
					}

					// Split transfering file into packets of size equal to MSS Bytes
					byte[] partData = Arrays.copyOfRange(data, nextPacket * MSS, endByte > FILE_SIZE ? FILE_SIZE : endByte);

					// Create Packet from the data
					RDTPacket dataPacket = new RDTPacket(nextPacket % lastSeqNo, partData, endOfFile);

					// Covert packet to Bytes array
					byte[] sendData = dataPacket.generatePacket();

					// Create Datagram Packet
					DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, receiverPort);

					// Induce Lost Packet Error. It says packet sent but it wont send the packet
					if (Math.random() > LOST_PACKET) {
						clientSocket.send(sendPacket);
					} else{
						System.out.println("Lost Packet: " + nextPacket % lastSeqNo);
					}

					// Sending packet message
					System.out.println("Sending " + nextPacket % lastSeqNo + ";");

					nextPacket++;
				}

				// Packet to receive ack
				byte[] receiveData = new byte[4];
				DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

				try{

					// Calculate remaining time for the expecting packet
					long TIMER = TIMEOUT - (System.currentTimeMillis() - sentPacketTimers.get(waitingForAck));

					// If timer expires raise Timeout Exception
					if (TIMER < 0) {
						throw new SocketTimeoutException();
					}

					// Set Timeout to receiveing socket
					clientSocket.setSoTimeout((int) TIMER);

					// Receive ACK Packet
					clientSocket.receive(receivePacket);

					// Decode ACK Packet
					RDTAck ackPacket = new RDTAck(receivePacket.getData());

					// Mesage to show received ACK
					System.out.println("Received ACK: " + ackPacket.getSeqNo());

					ArrayList<Integer> WINDOW_LIST = new ArrayList<Integer>(WINDOW);

					// Get actual Seq No. of received ACK
					int actualSeqNo = waitingForAck + WINDOW_LIST.indexOf(ackPacket.getSeqNo());

					// Store received acks to know out of order acks
					receivedAcks.put(actualSeqNo, ackPacket);

					// Adding a delay in response to show a clear flow of things
					Thread.sleep(100);

					// Check for received ACK's in sequence and clear the window for sending more packets
					while(receivedAcks.containsKey(waitingForAck)){
						WINDOW.add((waitingForAck + WINDOW_SIZE) % lastSeqNo);
						waitingForAck++;
						WINDOW.remove();
					}

					System.out.println("");
				} catch(SocketTimeoutException e){

					// Packet ACK not received. Time out occured. Resendig the packet

					System.out.println("Packet " + waitingForAck % lastSeqNo + ": Timer expired; Resending " + waitingForAck % lastSeqNo);

					int startByte = waitingForAck * MSS;
					int endByte = (waitingForAck + 1) * MSS;

					byte[] partData = Arrays.copyOfRange(data, startByte, endByte > FILE_SIZE ? FILE_SIZE : endByte);

					RDTPacket dataPacket = new RDTPacket(waitingForAck % lastSeqNo, partData, endByte > FILE_SIZE);

					byte[] sendData = dataPacket.generatePacket();

					DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, receiverPort);

					// Induce Lost packet error
					if (Math.random() > LOST_PACKET) {
						clientSocket.send(sendPacket);
					}else{
						System.out.println("Lost Packet: " + waitingForAck % lastSeqNo);
					}

					// Reset timer for resent packet
					sentPacketTimers.put(waitingForAck, System.currentTimeMillis());

					System.out.println("Timer Re-started");
				}
				
			}
			// close socket
			clientSocket.close();
		} else{
			// close socket
			clientSocket.close();
		}
	}
}