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

	public static double LOST_PACKET = 0.1;

	public static void main(String args[]) throws Exception
	{

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

		// Main Part

		// Open send socket
		DatagramSocket clientSocket = new DatagramSocket();
		InetAddress IPAddress = InetAddress.getByName("localhost");

		String dataToReceiver = protocol + "," + bitsOfSqeunceNo + "," + WINDOW_SIZE + "," + MSS;

		byte[] inputData = dataToReceiver.getBytes();

		// Send protocol details to receiver
		DatagramPacket inputDataPacket = new DatagramPacket(inputData, inputData.length, IPAddress, receiverPort);

		clientSocket.send(inputDataPacket);

		// Thread.sleep(100000);

		int lastSeqNo = (int) (Math.pow(2.0, (double) bitsOfSqeunceNo));

		System.out.println("Sending file: " + transferFilename);

		// Read data from file into bytes
		File file = new File(transferFilename);
		FileInputStream inputStream = new FileInputStream(file);
		byte[] data = new byte[(int) file.length()];
		inputStream.read(data);
		inputStream.close();

		int FILE_SIZE = data.length;
		System.out.println("FILE SIZE: " + FILE_SIZE + " bytes");

		int lastPacketNo = FILE_SIZE / MSS;

		boolean endOfFile = false;

		int nextPacket = 0;

		int waitingForAck = 0;

		long startTime = System.currentTimeMillis();

		Queue<Integer> WINDOW = new ArrayDeque<>();

		for(int i = 0; i < WINDOW_SIZE; i++){
			WINDOW.add(i);
		}

		// System.out.println(lastPacketNo);

		while (waitingForAck <= lastPacketNo) {

			while(nextPacket - waitingForAck < WINDOW_SIZE && !endOfFile){
				startTime = System.currentTimeMillis();
				int startByte = nextPacket * MSS;
				int endByte = (nextPacket + 1) * MSS;

				if (endByte > FILE_SIZE) {
					System.out.println("End of File");
					endOfFile = true;
				}

				byte[] partData = Arrays.copyOfRange(data, nextPacket * MSS, endByte > FILE_SIZE ? FILE_SIZE : endByte);

				int sqeNo = nextPacket % lastSeqNo;

				RDTPacket dataPacket = new RDTPacket(sqeNo, partData, endOfFile);

				byte[] sendData = dataPacket.generatePacket();

				DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, receiverPort);

				if (Math.random() > LOST_PACKET) {
					clientSocket.send(sendPacket);
				} else{
					System.out.print("Lost Packet: " + sqeNo + "; ");
				}

				System.out.println("Sending " + sqeNo + "; Packet Size: " + sendData.length);
				nextPacket++;
			}


			byte[] receiveData = new byte[4];
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

			try{
				long TIMER = TIMEOUT - (System.currentTimeMillis() - startTime);

				if (TIMER < 0) {
					throw new SocketTimeoutException();
				}

				clientSocket.setSoTimeout((int) TIMER);

				clientSocket.receive(receivePacket);

				RDTAck ackPacket = new RDTAck(receivePacket.getData());
				Thread.sleep(100);
				System.out.println("");
				System.out.println("Received ACK: " + ackPacket.getSeqNo());

				if (WINDOW.contains(ackPacket.getSeqNo())){
					while(ackPacket.getSeqNo() != WINDOW.poll()){
						startTime = System.currentTimeMillis();
						WINDOW.add((waitingForAck + WINDOW_SIZE) % lastSeqNo);
						waitingForAck++;
					}
					WINDOW.add((waitingForAck + WINDOW_SIZE) % lastSeqNo);
					waitingForAck++;
				}
			} catch(SocketTimeoutException e){

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

					if (Math.random() > LOST_PACKET) {
						clientSocket.send(sendPacket);
					}else{
						System.out.println("Lost Packet: " + sqeNo);
					}
				}

				startTime = System.currentTimeMillis();

				message += "; Timer started";

				System.out.println(message);
				System.out.println("");
			}
			
		}

		clientSocket.close();
	}
}