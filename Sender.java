import java.io.*;
import java.net.*;
import java.util.Arrays;

class Sender
{

	public static int MSS = 500;

	public static int receiverPort = 9876;

	public static int bitsOfSqeunceNo = 4;

	public static int WINDOW_SIZE = 8;

	public static int TIMEOUT = 5000;

	public static String filename = "inputfile.txt";


	public static void main(String args[]) throws Exception
	{

		System.out.println("Sending file: " + filename);

		// Read data from file into bytes
		File file = new File(filename);
		FileInputStream inputStream = new FileInputStream(file);
		byte[] data = new byte[(int) file.length()];
		inputStream.read(data);
		inputStream.close();

		int FILE_SIZE = data.length;
		System.out.println("FILE SIZE: " + FILE_SIZE + " bytes");

		// Open send socket
		DatagramSocket clientSocket = new DatagramSocket();
		InetAddress IPAddress = InetAddress.getByName("localhost");

		int packetsSent = 0;

		boolean endOfFile = false;

		int nextPacket = 0;

		int waitingForAck = 0;

		long startTime = System.currentTimeMillis();

		while (!(endOfFile && waitingForAck == nextPacket)) {

			while(nextPacket - waitingForAck < WINDOW_SIZE && !endOfFile){
				startTime = System.currentTimeMillis();
				int startByte = nextPacket * MSS;
				int endByte = (nextPacket + 1) * MSS;

				if (endByte > FILE_SIZE) {
					System.out.println("End of File");
					endOfFile = true;
				}

				byte[] partData = Arrays.copyOfRange(data, nextPacket * MSS, endByte > FILE_SIZE ? FILE_SIZE : endByte);

				RDTPacket dataPacket = new RDTPacket(nextPacket, partData, endOfFile);

				byte[] sendData = dataPacket.generatePacket();

				DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, receiverPort);
				clientSocket.send(sendPacket);

				System.out.println("Sending " + nextPacket + "; Packet Size: " + sendData.length);

				nextPacket++;
				packetsSent++;
			}


			byte[] receiveData = new byte[4];
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

			try{
				clientSocket.setSoTimeout(TIMEOUT - ((new Date()).getTime() - startTime));

				clientSocket.receive(receivePacket);

				RDTAck ackPacket = new RDTAck(receivePacket.getData());

				System.out.println("Received ACK: " + ackPacket.getSeqNo());
				waitingForAck = Math.max(waitingForAck, ackPacket.getSeqNo() + 1);
				System.out.println("");

				Thread.sleep(1000);
			} catch(SocketTimeoutException e){

				String message = "Packet " + waitingForAck + ": Timer expired; Resending";

				for(int i = waitingForAck; i < nextPacket; i++){

					message += (" " + i);

					int startByte = i * MSS;
					int endByte = (i + 1) * MSS;

					byte[] partData = Arrays.copyOfRange(data, i * MSS, endByte > FILE_SIZE ? FILE_SIZE : endByte);

					RDTPacket dataPacket = new RDTPacket(i, partData, endByte > FILE_SIZE);

					byte[] sendData = dataPacket.generatePacket();

					DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, receiverPort);
					clientSocket.send(sendPacket);
				}

				startTime = System.currentTimeMillis();

				message += "; Timer started";

				System.out.println(message);
			}
			
		}

		clientSocket.close();
	}
}