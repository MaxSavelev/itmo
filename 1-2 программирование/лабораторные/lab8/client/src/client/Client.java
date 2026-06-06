package client;

import common.io.FramedMessageCodec;
import common.request.CommandRequest;
import common.response.CommandResponse;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Отвечает за сетевой обмен клиента с сервером.
 * <p>
 * На каждый запрос создаёт TCP-соединение, отправляет объект команды
 * и читает объект ответа от сервера.
 * </p>
 */
public class Client {
    private static final int CONNECTION_TIMEOUT_MS = 3000;
    private static final int READ_TIMEOUT_MS = 5000;

    private final String host;
    private final int port;

    /**
     * Создаёт клиент для подключения к указанному серверу.
     *
     * @param host адрес сервера
     * @param port порт сервера
     */
    public Client(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * Отправляет один объект запроса на сервер и ждёт один объект ответа.
     *
     * @param request объект команды, который нужно выполнить на сервере
     * @return ответ сервера с результатом выполнения команды
     * @throws IOException если не удалось подключиться, отправить запрос или прочитать ответ
     * @throws ClassNotFoundException если в ответе пришёл объект класса, которого нет у клиента
     */
    public CommandResponse send(CommandRequest request) throws IOException, ClassNotFoundException {
        try (Socket socket = new Socket()) {
            
            
            
            socket.connect(new InetSocketAddress(host, port), CONNECTION_TIMEOUT_MS);
            
            
            socket.setSoTimeout(READ_TIMEOUT_MS);

            
            FramedMessageCodec.write(request, socket.getOutputStream());
            
            
            Object object = FramedMessageCodec.read(socket.getInputStream());

            if (object instanceof CommandResponse response) {
                return response;
            }

            throw new IOException("Сервер вернул объект неизвестного типа.");
        }
    }
}
