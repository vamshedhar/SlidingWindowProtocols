import java.io.*;
import java.net.*;
import java.util.Arrays;

class Sender
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

		long startTime = System.currentTimeMillis();

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
					System.out.println("Lost Packet: " + sqeNo);
				}

				System.out.println("Sending " + sqeNo + "; Packet Size: " + sendData.length);

				nextPacket++;
			}


			byte[] receiveData = new byte[4];
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

			try{
				long TIMER = TIMEOUT - (System.currentTimeMillis() - startTime);

				// System.out.println(TIMER > 0 ? (int) TIMER : 10);

				if (TIMER < 0) {
					throw new SocketTimeoutException();
				}

				clientSocket.setSoTimeout((int) TIMER);

				clientSocket.receive(receivePacket);

				RDTAck ackPacket = new RDTAck(receivePacket.getData());

				System.out.println("Received ACK: " + ackPacket.getSeqNo());

				Thread.sleep(1000);

				int actualSeqNo = (waitingForAck / lastSeqNo) * lastSeqNo + ackPacket.getSeqNo();

				if (waitingForAck <= actualSeqNo) {
					startTime = System.currentTimeMillis();
				}

				waitingForAck = Math.max(waitingForAck, actualSeqNo + 1);
				System.out.println("");
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
			}
			
		}

		clientSocket.close();
	}
}