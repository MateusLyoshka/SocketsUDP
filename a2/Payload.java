import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

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

	public static Payload start(String fileName, long fileSize) {
		if (fileName == null || fileName.isBlank()) {
			throw new IllegalArgumentException("Nome do arquivo invalido.");
		}
		byte[] nameBytes = fileName.getBytes(StandardCharsets.UTF_8);
		if (nameBytes.length > 255) {
			throw new IllegalArgumentException("Nome do arquivo muito grande.");
		}
		if (fileSize < 0) {
			throw new IllegalArgumentException("Tamanho do arquivo invalido.");
		}
		return new Payload(TYPE_START, fileName, fileSize, 0, null, null);
	}

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

	public static Payload end(String checksum) {
		if (checksum == null || checksum.isBlank()) {
			throw new IllegalArgumentException("Checksum invalido.");
		}
		if (checksum.length() != 40) {
			throw new IllegalArgumentException("Checksum SHA-1 deve ter 40 caracteres hex.");
		}
		return new Payload(TYPE_END, null, 0, 0, null, checksum.toLowerCase());
	}

	public byte getType() {
		return type;
	}

	public String getFileName() {
		return fileName;
	}

	public long getFileSize() {
		return fileSize;
	}

	public int getSequenceNumber() {
		return sequenceNumber;
	}

	public byte[] getData() {
		return data == null ? null : Arrays.copyOf(data, data.length);
	}

	public String getChecksum() {
		return checksum;
	}

	public byte[] toBytes() {
		switch (type) {
			case TYPE_START:
				byte[] nameBytes = fileName.getBytes(StandardCharsets.UTF_8);
				ByteBuffer startBuffer = ByteBuffer.allocate(1 + 1 + nameBytes.length + 8);
				startBuffer.put(type);
				startBuffer.put((byte) nameBytes.length);
				startBuffer.put(nameBytes);
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

	public static Payload fromBytes(byte[] bytes, int length) {
		if (bytes == null || length < 1) {
			throw new IllegalArgumentException("Pacote invalido.");
		}

		ByteBuffer buffer = ByteBuffer.wrap(bytes, 0, length);
		byte type = buffer.get();

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
}
