import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import javax.swing.JOptionPane;

public class Client {
	private static final int MAX_PACKET_SIZE = 1024;
	private static final String ECHO_ACK_PREFIX = "[ACK]";

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

			String dstIP = JOptionPane.showInputDialog("IP Destino?");
			if (dstIP == null || dstIP.trim().isEmpty()) {
				return;
			}

			String dstPortInput = JOptionPane.showInputDialog("Porta Destino?");
			if (dstPortInput == null) {
				return;
			}
			int dstPort = Integer.parseInt(dstPortInput.trim());

			InetAddress destinationAddress = InetAddress.getByName(dstIP.trim());
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
					int parsed = Integer.parseInt(typeInput.trim());
					type = (byte) parsed;
				} catch (NumberFormatException ex) {
					JOptionPane.showMessageDialog(null, "Tipo invalido.");
					continue;
				}

				String msg;
				if (type == Payload.TYPE_ECHO) {
					msg = "ECHO";
				} else {
					msg = JOptionPane.showInputDialog("Mensagem?");
					if (msg == null) {
						break;
					}
				}

				try {
					Payload payload = new Payload(type, nickname, msg);
					byte[] bytes = payload.toBytes();

					DatagramPacket packet = new DatagramPacket(
							bytes,
							bytes.length,
							destinationAddress,
							dstPort
					);
					socket.send(packet);
					System.out.println("Enviado [" + Payload.typeLabel(type) + "] para "
							+ destinationAddress.getHostAddress() + ":" + dstPort);
				} catch (IllegalArgumentException ex) {
					JOptionPane.showMessageDialog(null, "Erro no payload: " + ex.getMessage());
				}

				int resp = JOptionPane.showConfirmDialog(
						null,
						"Nova mensagem?",
						"Continuar",
						JOptionPane.YES_NO_OPTION
				);
				if (resp == JOptionPane.NO_OPTION) {
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

	private static void receiveLoop(DatagramSocket socket, String myNickname) {
		while (!socket.isClosed()) {
			try {
				byte[] buffer = new byte[MAX_PACKET_SIZE];
				DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
				socket.receive(packet);

				Payload payload = Payload.fromBytes(packet.getData(), packet.getLength());

				String from = packet.getAddress().getHostAddress() + ":" + packet.getPort();
				String header = "Recebido de " + from + " [" + Payload.typeLabel(payload.getType()) + "]";
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
