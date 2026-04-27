import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.swing.JOptionPane;

public class Server {
	private static final int MAX_PACKET_SIZE = 1400;
	private static final String UPLOAD_DIR = "uploads";

	private static InetAddress activeClientAddress;
	private static int activeClientPort;
	private static String activeFileName;
	private static long activeFileSize;
	private static int expectedSequence;
	private static ByteArrayOutputStream activeContent;

	public static void main(String[] args) {
		DatagramSocket socket = null;
		try {
			String portInput = JOptionPane.showInputDialog("Porta local do servidor?");
			if (portInput == null || portInput.trim().isEmpty()) {
				return;
			}
			int port = Integer.parseInt(portInput.trim());

			Files.createDirectories(Path.of(UPLOAD_DIR));

			socket = new DatagramSocket(port);
			System.out.println("Servidor UDP de upload ouvindo na porta " + port);
			System.out.println("Pasta de destino: " + Path.of(UPLOAD_DIR).toAbsolutePath());

			while (true) {
				byte[] buffer = new byte[MAX_PACKET_SIZE];
				DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
				socket.receive(packet);

				try {
					Payload payload = Payload.fromBytes(packet.getData(), packet.getLength());
					handlePacket(payload, packet.getAddress(), packet.getPort());
				} catch (IllegalArgumentException ex) {
					System.out.println("Pacote invalido de "
							+ packet.getAddress().getHostAddress() + ":" + packet.getPort()
							+ " -> " + ex.getMessage());
				}
			}
		} catch (NumberFormatException ex) {
			JOptionPane.showMessageDialog(null, "Porta invalida.");
		} catch (IOException ex) {
			System.out.println("IO: " + ex.getMessage());
		} finally {
			if (socket != null && !socket.isClosed()) {
				socket.close();
			}
		}
	}

	private static void handlePacket(Payload payload, InetAddress fromAddress, int fromPort) {
		if (payload.getType() == Payload.TYPE_START) {
			activeClientAddress = fromAddress;
			activeClientPort = fromPort;
			activeFileName = sanitizeFileName(payload.getFileName());
			activeFileSize = payload.getFileSize();
			expectedSequence = 0;
			activeContent = new ByteArrayOutputStream();

			System.out.println("START recebido de " + fromAddress.getHostAddress() + ":" + fromPort
					+ " arquivo=" + activeFileName + " tamanho=" + activeFileSize + " bytes");
			return;
		}

		if (!isActiveUploadFromSender(fromAddress, fromPort)) {
			System.out.println("Pacote ignorado (sem START ativo do remetente): "
					+ fromAddress.getHostAddress() + ":" + fromPort);
			return;
		}

		if (payload.getType() == Payload.TYPE_DATA) {
			if (payload.getSequenceNumber() != expectedSequence) {
				System.out.println("Sequencia inesperada: recebido=" + payload.getSequenceNumber()
						+ " esperado=" + expectedSequence + " (pacote ignorado)");
				return;
			}

			byte[] data = payload.getData();
			activeContent.write(data, 0, data.length);
			expectedSequence++;
			return;
		}

		if (payload.getType() == Payload.TYPE_END) {
			finalizeUpload(payload.getChecksum());
		}
	}

	private static boolean isActiveUploadFromSender(InetAddress fromAddress, int fromPort) {
		return activeClientAddress != null
				&& activeContent != null
				&& activeClientPort == fromPort
				&& activeClientAddress.equals(fromAddress);
	}

	private static void finalizeUpload(String receivedChecksum) {
		try {
			byte[] bytes = activeContent.toByteArray();
			String calculated = sha1Hex(bytes);

			if (bytes.length != activeFileSize) {
				System.out.println("Falha de upload: tamanho divergente. esperado=" + activeFileSize
						+ " recebido=" + bytes.length);
				resetState();
				return;
			}

			if (!calculated.equalsIgnoreCase(receivedChecksum)) {
				System.out.println("Falha de upload: checksum invalido. esperado=" + receivedChecksum
						+ " calculado=" + calculated);
				resetState();
				return;
			}

			Path outputFile = Path.of(UPLOAD_DIR, activeFileName);
			Files.write(outputFile, bytes);

			System.out.println("Upload concluido com sucesso: " + outputFile.toAbsolutePath());
			System.out.println("SHA-1 validado: " + calculated);
		} catch (IOException ex) {
			System.out.println("Erro ao salvar arquivo: " + ex.getMessage());
		} catch (NoSuchAlgorithmException ex) {
			System.out.println("SHA-1 indisponivel: " + ex.getMessage());
		} finally {
			resetState();
		}
	}

	private static String sanitizeFileName(String fileName) {
		return Path.of(fileName).getFileName().toString();
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

	private static void resetState() {
		activeClientAddress = null;
		activeClientPort = 0;
		activeFileName = null;
		activeFileSize = 0;
		expectedSequence = 0;
		activeContent = null;
	}
}
