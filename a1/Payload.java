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

	public byte getType() {
		return type;
	}

	public String getNickname() {
		return nickname;
	}

	public String getMessage() {
		return message;
	}

	public byte[] toBytes() {
		byte[] nickBytes = nickname.getBytes(StandardCharsets.UTF_8);
		byte[] msgBytes = message.getBytes(StandardCharsets.UTF_8);

		int size = 1 + 1 + nickBytes.length + 1 + msgBytes.length;
		byte[] bytes = new byte[size];

		int idx = 0;
		bytes[idx++] = type;
		bytes[idx++] = (byte) nickBytes.length;

		System.arraycopy(nickBytes, 0, bytes, idx, nickBytes.length);
		idx += nickBytes.length;

		bytes[idx++] = (byte) msgBytes.length;
		System.arraycopy(msgBytes, 0, bytes, idx, msgBytes.length);

		return bytes;
	}

	public static Payload fromBytes(byte[] bytes, int length) {
		if (bytes == null) {
			throw new IllegalArgumentException("Payload nao pode ser null.");
		}

		if (length < 4) {
			throw new IllegalArgumentException("Payload invalido: tamanho insuficiente.");
		}

		int idx = 0;

		byte type = bytes[idx++];
		validateType(type);

		int nickLen = Byte.toUnsignedInt(bytes[idx++]);
		if (nickLen < 1 || nickLen > 64) {
			throw new IllegalArgumentException("Payload invalido: tamanho de apelido fora do intervalo.");
		}

		if (idx + nickLen > length) {
			throw new IllegalArgumentException("Payload invalido: apelido incompleto.");
		}

		String nickname = new String(Arrays.copyOfRange(bytes, idx, idx + nickLen), StandardCharsets.UTF_8);
		idx += nickLen;

		if (idx >= length) {
			throw new IllegalArgumentException("Payload invalido: tamanho de mensagem ausente.");
		}

		int msgLen = Byte.toUnsignedInt(bytes[idx++]);
		if (idx + msgLen > length) {
			throw new IllegalArgumentException("Payload invalido: mensagem incompleta.");
		}

		String message = new String(Arrays.copyOfRange(bytes, idx, idx + msgLen), StandardCharsets.UTF_8);

		return new Payload(type, nickname, message);
	}

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

	private static void validateType(byte type) {
		if (type != TYPE_NORMAL && type != TYPE_EMOJI && type != TYPE_URL && type != TYPE_ECHO) {
			throw new IllegalArgumentException("Tipo de mensagem invalido: " + type);
		}
	}
}
