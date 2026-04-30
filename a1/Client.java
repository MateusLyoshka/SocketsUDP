/*
 * Descricao: cliente UDP ponto a ponto da atividade 1, com envio e recepcao de mensagens.
 * Autores:
	- Mateus Santos Fernandes
	- Matheus Floriano Saito da Silva
 * Data de criacao: 26/04/2026
 * Data de atualizacao: 30/04/2026
 */
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import javax.swing.JOptionPane;

/**
 * Cliente UDP para envio e recebimento de mensagens entre dois pares.
 */
public class Client {
	private static final int MAX_PACKET_SIZE = 1024;
	private static final String ECHO_ACK_PREFIX = "[ACK]";

	/**
	 * Ponto de entrada da aplicacao cliente.
	 *
	 * @param args argumentos de linha de comando, nao utilizados.
	 */
	public static void main(String[] args) {
		DatagramSocket socket = null;
		try {
			String nicknameInput = JOptionPane.showInputDialog("Seu apelido (1-64 bytes):");
			if (nicknameInput == null || nicknameInput.trim().isEmpty()) {
				return;
			}
			final String nickname = nicknameInput.trim();

			String localPortInput = JOptionPane.showInputDialog("Porta local para escutar?");
			if (localPortInput == null) {
				return;
			}
			int localPort = Integer.parseInt(localPortInput.trim());

			String destinationIpInput = JOptionPane.showInputDialog("IP Destino?");
			if (destinationIpInput == null || destinationIpInput.trim().isEmpty()) {
				return;
			}

			String destinationPortInput = JOptionPane.showInputDialog("Porta Destino?");
			if (destinationPortInput == null) {
				return;
			}
			int destinationPort = Integer.parseInt(destinationPortInput.trim());

			InetAddress destinationAddress = InetAddress.getByName(destinationIpInput.trim());
			socket = new DatagramSocket(localPort);

			DatagramSocket finalSocket = socket;
			Thread receiver = new Thread(() -> receiveLoop(finalSocket, nickname));
			receiver.setDaemon(true);
			receiver.start();

			while (true) {
				String typeInput = JOptionPane.showInputDialog(
						"Tipo (1=normal, 2=emoji, 3=url, 4=echo):");
				if (typeInput == null) {
					break;
				}

				byte type;
				try {
					int parsedType = Integer.parseInt(typeInput.trim());
					type = (byte) parsedType;
				} catch (NumberFormatException ex) {
					JOptionPane.showMessageDialog(null, "Tipo invalido.");
					continue;
				}

				String message;
				if (type == Payload.TYPE_ECHO) {
					message = "ECHO";
				} else {
					message = JOptionPane.showInputDialog("Mensagem?");
					if (message == null) {
						break;
					}
				}

				try {
					Payload payload = new Payload(type, nickname, message);
					byte[] bytes = payload.toBytes();

					DatagramPacket packet = new DatagramPacket(
							bytes,
							bytes.length,
							destinationAddress,
							destinationPort
					);
					socket.send(packet);
					System.out.println("Enviado [" + Payload.typeLabel(type) + "] para "
							+ destinationAddress.getHostAddress() + ":" + destinationPort);
				} catch (IllegalArgumentException ex) {
					JOptionPane.showMessageDialog(null, "Erro no payload: " + ex.getMessage());
				}

				int response = JOptionPane.showConfirmDialog(
						null,
						"Nova mensagem?",
						"Continuar",
						JOptionPane.YES_NO_OPTION
				);
				if (response == JOptionPane.NO_OPTION) {
					break;
				}
			}
		} catch (NumberFormatException ex) {
			JOptionPane.showMessageDialog(null, "Porta invalida.");
		} catch (SocketException ex) {
			System.out.println("Socket: " + ex.getMessage());
		} catch (IOException ex) {
			System.out.println("IO: " + ex.getMessage());
		} finally {
			if (socket != null && !socket.isClosed()) {
				socket.close();
			}
		}
	}

	/**
	 * Mantem a recepcao de mensagens enquanto o socket permanecer aberto.
	 *
	 * @param socket socket UDP usado para recepcao.
	 * @param myNickname apelido local usado nos retornos de echo.
	 */
	private static void receiveLoop(DatagramSocket socket, String myNickname) {
		while (!socket.isClosed()) {
			try {
				byte[] buffer = new byte[MAX_PACKET_SIZE];
				DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
				socket.receive(packet);

				Payload payload = Payload.fromBytes(packet.getData(), packet.getLength());

				String senderAddress = packet.getAddress().getHostAddress() + ":" + packet.getPort();
				String header = "Recebido de " + senderAddress + " [" + Payload.typeLabel(payload.getType()) + "]";
				String body = payload.getNickname() + ": " + payload.getMessage();

				System.out.println(header + " -> " + body);

				if (payload.getType() == Payload.TYPE_ECHO
						&& !payload.getMessage().startsWith(ECHO_ACK_PREFIX)) {
					Payload echoReply = new Payload(
							Payload.TYPE_ECHO,
							myNickname,
							ECHO_ACK_PREFIX + payload.getMessage()
					);
					byte[] replyBytes = echoReply.toBytes();
					DatagramPacket reply = new DatagramPacket(
							replyBytes,
							replyBytes.length,
							packet.getAddress(),
							packet.getPort()
					);
					socket.send(reply);
				}
			} catch (IllegalArgumentException ex) {
				System.out.println("Payload invalido recebido: " + ex.getMessage());
			} catch (IOException ex) {
				if (!socket.isClosed()) {
					System.out.println("Erro ao receber/enviar: " + ex.getMessage());
				}
			}
		}
	}
}
