/*
 * Descricao: envia um arquivo via UDP usando o protocolo de upload da atividade 2.
 * Autores:
	- Mateus Santos Fernandes
	- Matheus Floriano Saito da Silva
 * Data de criacao: 27/04/2026
 * Data de atualizacao: 30/04/2026
 */
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.swing.JOptionPane;

/**
 * Cliente interativo para selecionar um arquivo e envia-lo ao servidor UDP.
 */
public class Client {
	private static final int CHUNK_SIZE = 1024;

	/**
	 * Ponto de entrada do cliente.
	 *
	 * @param args argumentos de linha de comando, nao utilizados.
	 */
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

			String destinationIpInput = JOptionPane.showInputDialog("IP Destino?");
			if (destinationIpInput == null || destinationIpInput.trim().isEmpty()) {
				return;
			}

			String dstPortInput = JOptionPane.showInputDialog("Porta Destino?");
			if (dstPortInput == null || dstPortInput.trim().isEmpty()) {
				return;
			}
			int dstPort = Integer.parseInt(dstPortInput.trim());

			InetAddress destinationAddress = InetAddress.getByName(destinationIpInput.trim());
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

	/**
	 * Envia um payload para o destino informado.
	 *
	 * @param socket socket UDP em uso.
	 * @param address endereco IP de destino.
	 * @param port porta de destino.
	 * @param payload pacote a ser enviado.
	 * @throws IOException quando o envio falhar.
	 */
	private static void sendPayload(DatagramSocket socket, InetAddress address, int port, Payload payload)
			throws IOException {
		byte[] bytes = payload.toBytes();
		DatagramPacket packet = new DatagramPacket(bytes, bytes.length, address, port);
		socket.send(packet);
	}

	/**
	 * Calcula o SHA-1 hexadecimal de um vetor de bytes.
	 *
	 * @param bytes dados de entrada.
	 * @return checksum em hexadecimal minusculo.
	 * @throws NoSuchAlgorithmException quando SHA-1 nao estiver disponivel.
	 */
	private static String sha1Hex(byte[] bytes) throws NoSuchAlgorithmException {
		MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
		byte[] digest = messageDigest.digest(bytes);
		StringBuilder hexBuilder = new StringBuilder();
		for (byte currentByte : digest) {
			hexBuilder.append(String.format("%02x", currentByte));
		}
		return hexBuilder.toString();
	}
}
