import java.io.*;
import java.net.*;
import java.util.Arrays;

class Sender
{

	public static int MSS = 500;

	public static int receiverPort = 9876;

	public static int bitsOfSqeunceNo = 4;

	public static int WINDOW_SIZE = 8;

	public static int TIMEOUT = 10000000;

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

		while (!(endOfFile && waitingForAck == nextPacket)) {

			while(nextPacket - waitingForAck < WINDOW_SIZE && !endOfFile){
				int startByte = nextPacket * MSS;
				int endByte = (nextPacket + 1) * MSS;

				if (endByte > FILE_SIZE) {
					System.out.println("End of File");
					endOfFile = true;
				}

				byte[] partData = Arrays.copyOfRange(data, nextPacket * MSS, endByte > FILE_SIZE ? FILE_SIZE : endByte);

				RDTPacket dataPacket = new RDTPacket(nextPacket, partData, endOfFile);

				byte[] sendData = dataPacket.generatePacket();

				byte[] receiveData = new byte[MSS];

				DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, receiverPort);
				clientSocket.send(sendPacket);

				System.out.println("Sending " + nextPacket + "; Packet Size: " + sendData.length);

				nextPacket++;
				packetsSent++;
			}


			byte[] receiveData = new byte[4];
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			clientSocket.receive(receivePacket);

			RDTAck ackPacket = new RDTAck(receivePacket.getData());

			System.out.println("Received ACK: " + ackPacket.getSeqNo());
			waitingForAck = ackPacket.getSeqNo() + 1;
			System.out.println("");

			Thread.sleep(1000);
		}

		clientSocket.close();
	}
}