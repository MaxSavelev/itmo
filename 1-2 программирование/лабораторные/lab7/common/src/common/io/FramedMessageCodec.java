package common.io;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public final class FramedMessageCodec {
    
    private static final int MAX_MESSAGE_SIZE = 1024 * 1024;

    private FramedMessageCodec() {
    }

    public static ByteBuffer encodeToBuffer(Object object) throws IOException {
        byte[] payload = ObjectSerializer.serialize(object);
        checkMessageSize(payload.length);

        
        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES + payload.length);
        buffer.putInt(payload.length);
        buffer.put(payload);
        
        buffer.flip();

        return buffer;
    }

    public static Object decodePayload(byte[] payload) throws IOException, ClassNotFoundException {
        return ObjectSerializer.deserialize(payload);
    }

    public static void write(Object object, OutputStream outputStream) throws IOException {
        byte[] payload = ObjectSerializer.serialize(object);
        checkMessageSize(payload.length);

        DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
        
        dataOutputStream.writeInt(payload.length);
        dataOutputStream.write(payload);
        dataOutputStream.flush();
    }

    public static Object read(InputStream inputStream) throws IOException, ClassNotFoundException {
        DataInputStream dataInputStream = new DataInputStream(inputStream);
        
        int length = dataInputStream.readInt();
        checkMessageSize(length);

        byte[] payload = dataInputStream.readNBytes(length);
        if (payload.length != length) {
            throw new EOFException("Сообщение закончилось раньше, чем ожидалось.");
        }

        return ObjectSerializer.deserialize(payload);
    }

    public static byte[] tryReadPayload(ByteBuffer buffer) throws IOException {
        
        buffer.flip();

        if (buffer.remaining() < Integer.BYTES) {
            
            buffer.compact();
            return null;
        }

        buffer.mark();
        int length = buffer.getInt();
        checkMessageSize(length);

        if (buffer.remaining() < length) {
            
            buffer.reset();
            buffer.compact();
            return null;
        }

        byte[] payload = new byte[length];
        buffer.get(payload);
        
        buffer.compact();

        return payload;
    }

    private static void checkMessageSize(int length) throws IOException {
        if (length < 0 || length > MAX_MESSAGE_SIZE) {
            throw new IOException("Некорректный размер сообщения: " + length);
        }
    }
}
