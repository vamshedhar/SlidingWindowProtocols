import java.io.*;
import java.net.*;
import java.util.*;

class SenderSR
{

	public static int MSS = 500;

	public static int receiverPort = 9876;

	public static int bitsOfSqeunceNo = 4;

	public static int WINDOW_SIZE = 8;

	public static long TIMEOUT = 5000;

	public static String filename = "fulldata.txt";

	public static double LOST_PACKET = 0.1;

	public static void main(String args[]) throws Exception
	{

		int lastSeqNo = (int) (Math.pow(2.0, (double) bitsOfSqeunceNo));

		HashMap<Integer, Long> sentPacketTimers = new HashMap<Integer, Long>();

		HashMap<Integer, RDTAck> receivedAcks = new HashMap<Integer, RDTAck>();

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

		int lastPacketNo = FILE_SIZE / MSS;

		boolean endOfFile = false;

		int nextPacket = 0;

		int waitingForAck = 0;

		Queue<Integer> WINDOW = new ArrayDeque<>();

		for(int i = 0; i < WINDOW_SIZE; i++){
			WINDOW.add(i);
		}

		System.out.println(String.valueOf(WINDOW));

		while (waitingForAck <= lastPacketNo) {

			while(nextPacket - waitingForAck < WINDOW_SIZE && !endOfFile){

				sentPacketTimers.put(nextPacket, System.currentTimeMillis());

				int startByte = nextPacket * MSS;
				int endByte = (nextPacket + 1) * MSS;

				if (endByte > FILE_SIZE) {
					System.out.println("End of File");
					endOfFile = true;
				}

				byte[] partData = Arrays.copyOfRange(data, nextPacket * MSS, endByte > FILE_SIZE ? FILE_SIZE : endByte);

				RDTPacket dataPacket = new RDTPacket(nextPacket % lastSeqNo, partData, endOfFile);

				byte[] sendData = dataPacket.generatePacket();

				DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, receiverPort);

				if (Math.random() > LOST_PACKET) {
					clientSocket.send(sendPacket);
				} else{
					System.out.println("Lost Packet: " + nextPacket % lastSeqNo);
				}

				System.out.println("Sending " + nextPacket % lastSeqNo + "; Packet Size: " + sendData.length);

				nextPacket++;
			}


			byte[] receiveData = new byte[4];
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

			try{
				long TIMER = TIMEOUT - (System.currentTimeMillis() - sentPacketTimers.get(waitingForAck));

				// System.out.println(TIMER > 0 ? (int) TIMER : 10);

				if (TIMER < 0) {
					throw new SocketTimeoutException();
				}

				clientSocket.setSoTimeout((int) TIMER);

				clientSocket.receive(receivePacket);

				RDTAck ackPacket = new RDTAck(receivePacket.getData());

				System.out.println("Received ACK: " + ackPacket.getSeqNo());

				ArrayList<Integer> WINDOW_LIST = new ArrayList<Integer>(WINDOW);

				int actualSeqNo = waitingForAck + WINDOW_LIST.indexOf(ackPacket.getSeqNo());

				receivedAcks.put(actualSeqNo, ackPacket);

				Thread.sleep(100);

				while(receivedAcks.containsKey(waitingForAck)){
					WINDOW.add((waitingForAck + WINDOW_SIZE) % lastSeqNo);
					waitingForAck++;
					WINDOW.remove();
					System.out.println(String.valueOf(WINDOW));
				}

				System.out.println("");
			} catch(SocketTimeoutException e){

				System.out.println("Packet " + waitingForAck % lastSeqNo + ": Timer expired; Resending " + waitingForAck % lastSeqNo);

				int startByte = waitingForAck * MSS;
				int endByte = (waitingForAck + 1) * MSS;

				byte[] partData = Arrays.copyOfRange(data, startByte, endByte > FILE_SIZE ? FILE_SIZE : endByte);

				RDTPacket dataPacket = new RDTPacket(waitingForAck % lastSeqNo, partData, endByte > FILE_SIZE);

				byte[] sendData = dataPacket.generatePacket();

				DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, receiverPort);

				if (Math.random() > LOST_PACKET) {
					clientSocket.send(sendPacket);
				}else{
					System.out.println("Lost Packet: " + waitingForAck % lastSeqNo);
				}

				sentPacketTimers.put(waitingForAck, System.currentTimeMillis());

				System.out.println("Timer Re-started");
			}
			
		}

		clientSocket.close();
	}
}