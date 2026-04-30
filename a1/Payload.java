/*
 * Descricao: representa e serializa mensagens UDP da atividade 1 com tipo, apelido e texto.
 * Autores:
	- Mateus Santos Fernandes
	- Matheus Floriano Saito da Silva
 * Data de criacao: 26/04/2026
 * Data de atualizacao: 30/04/2026
 */
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class Payload {
	public static final byte TYPE_NORMAL = 1;
	public static final byte TYPE_EMOJI = 2;
	public static final byte TYPE_URL = 3;
	public static final byte TYPE_ECHO = 4;

	private final byte type;
	private final String nickname;
	private final String message;

	/**
	 * Cria um payload de mensagem com tipo, apelido e texto.
	 *
	 * @param type tipo da mensagem.
	 * @param nickname apelido do remetente.
	 * @param message conteudo textual da mensagem.
	 * @throws IllegalArgumentException quando algum parametro for invalido.
	 */
	public Payload(byte type, String nickname, String message) {
		validateType(type);

		if (nickname == null || nickname.isEmpty()) {
			throw new IllegalArgumentException("Apelido nao pode ser vazio.");
		}

		byte[] nickBytes = nickname.getBytes(StandardCharsets.UTF_8);
		if (nickBytes.length < 1 || nickBytes.length > 64) {
			throw new IllegalArgumentException("Apelido deve ter de 1 a 64 bytes.");
		}

		if (message == null) {
			throw new IllegalArgumentException("Mensagem nao pode ser null.");
		}

		byte[] msgBytes = message.getBytes(StandardCharsets.UTF_8);
		if (msgBytes.length > 255) {
			throw new IllegalArgumentException("Mensagem deve ter no maximo 255 bytes.");
		}

		this.type = type;
		this.nickname = nickname;
		this.message = message;
	}

	/**
	 * Retorna o tipo da mensagem.
	 *
	 * @return tipo da mensagem.
	 */
	public byte getType() {
		return type;
	}

	/**
	 * Retorna o apelido do remetente.
	 *
	 * @return apelido do remetente.
	 */
	public String getNickname() {
		return nickname;
	}

	/**
	 * Retorna o conteudo da mensagem.
	 *
	 * @return texto da mensagem.
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * Serializa o payload para o formato binario definido pela atividade.
	 *
	 * @return vetor de bytes pronto para envio.
	 */
	public byte[] toBytes() {
		byte[] nicknameBytes = nickname.getBytes(StandardCharsets.UTF_8);
		byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);

		int size = 1 + 1 + nicknameBytes.length + 1 + messageBytes.length;
		byte[] bytes = new byte[size];

		int index = 0;
		bytes[index++] = type;
		bytes[index++] = (byte) nicknameBytes.length;

		System.arraycopy(nicknameBytes, 0, bytes, index, nicknameBytes.length);
		index += nicknameBytes.length;

		bytes[index++] = (byte) messageBytes.length;
		System.arraycopy(messageBytes, 0, bytes, index, messageBytes.length);

		return bytes;
	}

	/**
	 * Reconstrui um payload a partir dos bytes recebidos.
	 *
	 * @param bytes buffer bruto recebido.
	 * @param length quantidade de bytes validos no buffer.
	 * @return payload reconstruido.
	 * @throws IllegalArgumentException quando o pacote for invalido.
	 */
	public static Payload fromBytes(byte[] bytes, int length) {
		if (bytes == null) {
			throw new IllegalArgumentException("Payload nao pode ser null.");
		}

		if (length < 4) {
			throw new IllegalArgumentException("Payload invalido: tamanho insuficiente.");
		}

		int index = 0;

		byte type = bytes[index++];
		validateType(type);

		int nicknameLength = Byte.toUnsignedInt(bytes[index++]);
		if (nicknameLength < 1 || nicknameLength > 64) {
			throw new IllegalArgumentException("Payload invalido: tamanho de apelido fora do intervalo.");
		}

		if (index + nicknameLength > length) {
			throw new IllegalArgumentException("Payload invalido: apelido incompleto.");
		}

		String nickname = new String(Arrays.copyOfRange(bytes, index, index + nicknameLength), StandardCharsets.UTF_8);
		index += nicknameLength;

		if (index >= length) {
			throw new IllegalArgumentException("Payload invalido: tamanho de mensagem ausente.");
		}

		int messageLength = Byte.toUnsignedInt(bytes[index++]);
		if (index + messageLength > length) {
			throw new IllegalArgumentException("Payload invalido: mensagem incompleta.");
		}

		String message = new String(Arrays.copyOfRange(bytes, index, index + messageLength), StandardCharsets.UTF_8);

		return new Payload(type, nickname, message);
	}

	/**
	 * Converte o tipo numerico em uma descricao legivel.
	 *
	 * @param type tipo da mensagem.
	 * @return rotulo textual do tipo.
	 */
	public static String typeLabel(byte type) {
		switch (type) {
			case TYPE_NORMAL:
				return "NORMAL";
			case TYPE_EMOJI:
				return "EMOJI";
			case TYPE_URL:
				return "URL";
			case TYPE_ECHO:
				return "ECHO";
			default:
				return "DESCONHECIDO";
		}
	}

	/**
	 * Valida se o tipo pertence ao conjunto suportado pela atividade.
	 *
	 * @param type tipo da mensagem.
	 * @throws IllegalArgumentException quando o tipo for invalido.
	 */
	private static void validateType(byte type) {
		if (type != TYPE_NORMAL && type != TYPE_EMOJI && type != TYPE_URL && type != TYPE_ECHO) {
			throw new IllegalArgumentException("Tipo de mensagem invalido: " + type);
		}
	}
}
