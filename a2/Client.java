import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.swing.JOptionPane;

public class Client {
	private static final int CHUNK_SIZE = 1024;

	public static void main(String[] args) {
		DatagramSocket socket = null;
		try {
			String filePathInput = JOptionPane.showInputDialog("Caminho do arquivo para upload?");
			if (filePathInput == null || filePathInput.trim().isEmpty()) {
				return;
			}

			Path filePath = Path.of(filePathInput.trim());
			if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
				JOptionPane.showMessageDialog(null, "Arquivo invalido.");
				return;
			}

			String dstIP = JOptionPane.showInputDialog("IP Destino?");
			if (dstIP == null || dstIP.trim().isEmpty()) {
				return;
			}

			String dstPortInput = JOptionPane.showInputDialog("Porta Destino?");
			if (dstPortInput == null || dstPortInput.trim().isEmpty()) {
				return;
			}
			int dstPort = Integer.parseInt(dstPortInput.trim());

			InetAddress destinationAddress = InetAddress.getByName(dstIP.trim());
			socket = new DatagramSocket();

			String fileName = filePath.getFileName().toString();
			long fileSize = Files.size(filePath);
			byte[] fileBytes = Files.readAllBytes(filePath);
			String checksum = sha1Hex(fileBytes);

			sendPayload(socket, destinationAddress, dstPort, Payload.start(fileName, fileSize));

			int seq = 0;
			for (int offset = 0; offset < fileBytes.length; offset += CHUNK_SIZE) {
				int length = Math.min(CHUNK_SIZE, fileBytes.length - offset);
				byte[] chunk = new byte[length];
				System.arraycopy(fileBytes, offset, chunk, 0, length);
				sendPayload(socket, destinationAddress, dstPort, Payload.data(seq, chunk, length));
				seq++;
			}

			sendPayload(socket, destinationAddress, dstPort, Payload.end(checksum));

			JOptionPane.showMessageDialog(
					null,
					"Upload enviado.\nArquivo: " + fileName
							+ "\nTamanho: " + fileSize + " bytes"
							+ "\nSHA-1: " + checksum
			);
		} catch (NumberFormatException ex) {
			JOptionPane.showMessageDialog(null, "Porta destino invalida.");
		} catch (IOException ex) {
			System.out.println("IO: " + ex.getMessage());
		} catch (NoSuchAlgorithmException ex) {
			System.out.println("SHA-1 indisponivel: " + ex.getMessage());
		} finally {
			if (socket != null && !socket.isClosed()) {
				socket.close();
			}
		}
	}

	private static void sendPayload(DatagramSocket socket, InetAddress address, int port, Payload payload)
			throws IOException {
		byte[] bytes = payload.toBytes();
		DatagramPacket packet = new DatagramPacket(bytes, bytes.length, address, port);
		socket.send(packet);
	}

	private static String sha1Hex(byte[] bytes) throws NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance("SHA-1");
		byte[] digest = md.digest(bytes);
		StringBuilder sb = new StringBuilder();
		for (byte b : digest) {
			sb.append(String.format("%02x", b));
		}
		return sb.toString();
	}
}
