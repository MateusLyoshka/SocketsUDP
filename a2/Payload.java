/*
 * Descricao: representa e serializa os pacotes do protocolo de upload UDP da atividade 2.
 * Autores:
	- Mateus Santos Fernandes
	- Matheus Floriano Saito da Silva
 * Data de criacao: 27/04/2026
 * Data de atualizacao: 30/04/2026
 */
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Modelo de payload usado pelo cliente e pelo servidor.
 */
public class Payload {
	public static final byte TYPE_START = 1;
	public static final byte TYPE_DATA = 2;
	public static final byte TYPE_END = 3;

	private final byte type;
	private final String fileName;
	private final long fileSize;
	private final int sequenceNumber;
	private final byte[] data;
	private final String checksum;

	private Payload(byte type, String fileName, long fileSize, int sequenceNumber, byte[] data, String checksum) {
		this.type = type;
		this.fileName = fileName;
		this.fileSize = fileSize;
		this.sequenceNumber = sequenceNumber;
		this.data = data;
		this.checksum = checksum;
	}

	/**
	 * Cria um payload START com nome e tamanho do arquivo.
	 *
	 * @param fileName nome do arquivo a ser enviado.
	 * @param fileSize tamanho total do arquivo em bytes.
	 * @return payload configurado como START.
	 * @throws IllegalArgumentException quando os parametros sao invalidos.
	 */
	public static Payload start(String fileName, long fileSize) {
		if (fileName == null || fileName.isBlank()) {
			throw new IllegalArgumentException("Nome do arquivo invalido.");
		}
		byte[] fileNameBytes = fileName.getBytes(StandardCharsets.UTF_8);
		if (fileNameBytes.length > 255) {
			throw new IllegalArgumentException("Nome do arquivo muito grande.");
		}
		if (fileSize < 0) {
			throw new IllegalArgumentException("Tamanho do arquivo invalido.");
		}
		return new Payload(TYPE_START, fileName, fileSize, 0, null, null);
	}

	/**
	 * Cria um payload DATA com numero de sequencia e bloco de dados.
	 *
	 * @param sequenceNumber numero sequencial do bloco.
	 * @param data bytes do bloco de dados.
	 * @param length quantidade de bytes validos no bloco.
	 * @return payload configurado como DATA.
	 * @throws IllegalArgumentException quando os parametros sao invalidos.
	 */
	public static Payload data(int sequenceNumber, byte[] data, int length) {
		if (sequenceNumber < 0) {
			throw new IllegalArgumentException("Sequencia invalida.");
		}
		if (data == null) {
			throw new IllegalArgumentException("Dados nao podem ser nulos.");
		}
		if (length < 0 || length > data.length || length > 1024) {
			throw new IllegalArgumentException("Tamanho de bloco invalido.");
		}
		return new Payload(TYPE_DATA, null, 0, sequenceNumber, Arrays.copyOf(data, length), null);
	}

	/**
	 * Cria um payload END com o checksum SHA-1 do arquivo.
	 *
	 * @param checksum checksum SHA-1 em hexadecimal.
	 * @return payload configurado como END.
	 * @throws IllegalArgumentException quando o checksum for invalido.
	 */
	public static Payload end(String checksum) {
		if (checksum == null || checksum.isBlank()) {
			throw new IllegalArgumentException("Checksum invalido.");
		}
		if (checksum.length() != 40) {
			throw new IllegalArgumentException("Checksum SHA-1 deve ter 40 caracteres hex.");
		}
		return new Payload(TYPE_END, null, 0, 0, null, checksum.toLowerCase());
	}

	/**
	 * Retorna o tipo do payload.
	 *
	 * @return tipo do pacote.
	 */
	public byte getType() {
		return type;
	}

	/**
	 * Retorna o nome do arquivo, quando aplicavel.
	 *
	 * @return nome do arquivo ou null.
	 */
	public String getFileName() {
		return fileName;
	}

	/**
	 * Retorna o tamanho do arquivo, quando aplicavel.
	 *
	 * @return tamanho do arquivo em bytes.
	 */
	public long getFileSize() {
		return fileSize;
	}

	/**
	 * Retorna o numero de sequencia do bloco, quando aplicavel.
	 *
	 * @return numero de sequencia.
	 */
	public int getSequenceNumber() {
		return sequenceNumber;
	}

	/**
	 * Retorna uma copia dos dados do bloco.
	 *
	 * @return bytes do bloco ou null.
	 */
	public byte[] getData() {
		return data == null ? null : Arrays.copyOf(data, data.length);
	}

	/**
	 * Retorna o checksum, quando aplicavel.
	 *
	 * @return checksum em hexadecimal ou null.
	 */
	public String getChecksum() {
		return checksum;
	}

	/**
	 * Serializa o payload para o formato binario do protocolo.
	 *
	 * @return vetor de bytes pronto para envio.
	 */
	public byte[] toBytes() {
		switch (type) {
			case TYPE_START:
				byte[] fileNameBytes = fileName.getBytes(StandardCharsets.UTF_8);
				ByteBuffer startBuffer = ByteBuffer.allocate(1 + 1 + fileNameBytes.length + 8);
				startBuffer.put(type);
				startBuffer.put((byte) fileNameBytes.length);
				startBuffer.put(fileNameBytes);
				startBuffer.putLong(fileSize);
				return startBuffer.array();
			case TYPE_DATA:
				ByteBuffer dataBuffer = ByteBuffer.allocate(1 + 4 + 2 + data.length);
				dataBuffer.put(type);
				dataBuffer.putInt(sequenceNumber);
				dataBuffer.putShort((short) data.length);
				dataBuffer.put(data);
				return dataBuffer.array();
			case TYPE_END:
				byte[] checksumBytes = checksum.getBytes(StandardCharsets.UTF_8);
				ByteBuffer endBuffer = ByteBuffer.allocate(1 + 1 + checksumBytes.length);
				endBuffer.put(type);
				endBuffer.put((byte) checksumBytes.length);
				endBuffer.put(checksumBytes);
				return endBuffer.array();
			default:
				throw new IllegalStateException("Tipo desconhecido: " + type);
		}
	}

	/**
	 * Desserializa um payload a partir de um pacote recebido.
	 *
	 * @param bytes buffer bruto recebido da rede.
	 * @param length quantidade de bytes validos no buffer.
	 * @return payload reconstruido.
	 * @throws IllegalArgumentException quando o pacote for invalido.
	 */
	public static Payload fromBytes(byte[] bytes, int length) {
		if (bytes == null || length < 1) {
			throw new IllegalArgumentException("Pacote invalido.");
		}

		ByteBuffer buffer = ByteBuffer.wrap(bytes, 0, length);
		byte type = buffer.get();
		validateType(type);

		if (type == TYPE_START) {
			if (buffer.remaining() < 1) {
				throw new IllegalArgumentException("Pacote START incompleto.");
			}
			int nameLength = Byte.toUnsignedInt(buffer.get());
			if (nameLength < 1 || nameLength > buffer.remaining() - 8) {
				throw new IllegalArgumentException("Nome do arquivo invalido.");
			}
			byte[] nameBytes = new byte[nameLength];
			buffer.get(nameBytes);
			String fileName = new String(nameBytes, StandardCharsets.UTF_8);
			if (buffer.remaining() < 8) {
				throw new IllegalArgumentException("Tamanho do arquivo ausente.");
			}
			long fileSize = buffer.getLong();
			return start(fileName, fileSize);
		}

		if (type == TYPE_DATA) {
			if (buffer.remaining() < 6) {
				throw new IllegalArgumentException("Pacote DATA incompleto.");
			}
			int sequenceNumber = buffer.getInt();
			int dataLength = Short.toUnsignedInt(buffer.getShort());
			if (dataLength > buffer.remaining() || dataLength > 1024) {
				throw new IllegalArgumentException("Bloco DATA invalido.");
			}
			byte[] data = new byte[dataLength];
			buffer.get(data);
			return data(sequenceNumber, data, dataLength);
		}

		if (type == TYPE_END) {
			if (buffer.remaining() < 1) {
				throw new IllegalArgumentException("Pacote END incompleto.");
			}
			int checksumLength = Byte.toUnsignedInt(buffer.get());
			if (checksumLength != 40 || checksumLength > buffer.remaining()) {
				throw new IllegalArgumentException("Checksum SHA-1 invalido.");
			}
			byte[] checksumBytes = new byte[checksumLength];
			buffer.get(checksumBytes);
			return end(new String(checksumBytes, StandardCharsets.UTF_8));
		}

		throw new IllegalArgumentException("Tipo de pacote invalido: " + type);
	}

	/**
	 * Valida se o tipo pertence ao conjunto suportado pelo protocolo.
	 *
	 * @param type tipo do pacote.
	 * @throws IllegalArgumentException quando o tipo nao existir.
	 */
	private static void validateType(byte type) {
		if (type != TYPE_START && type != TYPE_DATA && type != TYPE_END) {
			throw new IllegalArgumentException("Tipo de pacote invalido: " + type);
		}
	}
}
