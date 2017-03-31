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

		File file = new File(filename);
		FileInputStream inputStream = new FileInputStream(file);
		byte[] data = new byte[(int) file.length()];
		inputStream.read(data);
		inputStream.close();

		int FILE_SIZE = data.length;

		System.out.println("FILE SIZE: " + FILE_SIZE + " bytes");

		DatagramSocket clientSocket = new DatagramSocket();
		InetAddress IPAddress = InetAddress.getByName("localhost");

		int packetCount = 0;

		boolean end = false;

		while (!end) {

			int startByte = packetCount * MSS;
			int endByte = (packetCount + 1) * MSS;

			if (endByte > FILE_SIZE) {
				end = true;
			}

			byte[] partData = Arrays.copyOfRange(data, packetCount * MSS, endByte > FILE_SIZE ? FILE_SIZE : endByte);
			RDTPacket dataPacket = new RDTPacket(packetCount + 1, partData, end);

			byte[] sendData = dataPacket.generatePacket();

			byte[] receiveData = new byte[MSS];
			DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, receiverPort);
			clientSocket.send(sendPacket);
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			clientSocket.receive(receivePacket);
			String modifiedSentence = new String(receivePacket.getData());
			System.out.println("FROM SERVER (Packet " + (++packetCount) + "):" + modifiedSentence.length());
			System.out.println("");
			System.out.println("");

			
		}

		clientSocket.close();
	}
}