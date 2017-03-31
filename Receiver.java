import java.io.*;
import java.net.*;
import java.util.Arrays;

class Receiver
{
	public static void main(String args[]) throws Exception
	{
		DatagramSocket serverSocket = new DatagramSocket(9876);
		byte[] receivedPacket = new byte[1024];
		byte[] sendData = new byte[1024];

		System.out.println("Server Started: Waiting for packets!!");

		while(true)
		{
			DatagramPacket receivePacket = new DatagramPacket(receivedPacket, receivedPacket.length);
			serverSocket.receive(receivePacket);

			byte[] receiveData = receivePacket.getData();

			RDTPacket packet = new RDTPacket(receiveData);

			String sentence = new String(packet.data);
			System.out.println("Received Packet: " + packet.seq);
			System.out.println("Last Packet: " + packet.last);
			System.out.println("");
			System.out.println(sentence);
			InetAddress IPAddress = receivePacket.getAddress();
			int port = receivePacket.getPort();
			String capitalizedSentence = sentence.toUpperCase();
			sendData = capitalizedSentence.getBytes();
			DatagramPacket sendPacket =
			new DatagramPacket(sendData, sendData.length, IPAddress, port);
			serverSocket.send(sendPacket);
		}
	}
}

