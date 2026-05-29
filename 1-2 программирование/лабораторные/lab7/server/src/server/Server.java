package server;

import common.io.FramedMessageCodec;
import common.request.CommandRequest;
import common.response.CommandResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RejectedExecutionException;

/**
 * TCP-сервер на Java NIO.
 * <p>
 * Сервер принимает подключения, читает запросы клиентов,
 * передаёт команды в {@link ServerCommandProcessor} и отправляет ответы.
 * Для работы с несколькими клиентами используется {@link Selector}.
 * </p>
 */
public class Server {
    private static final Logger logger = LogManager.getLogger(Server.class);

    private final int port;
    
    private final ServerCommandProcessor processor;
    private final ForkJoinPool readPool = new ForkJoinPool();
    private final ExecutorService writePool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    private final Queue<Runnable> selectorTasks = new ConcurrentLinkedQueue<>();
    
    
    private volatile boolean running = true;
    
    private Selector selector;

    /**
     * Создаёт сервер.
     *
     * @param port порт, который сервер будет слушать
     * @param processor обработчик команд коллекции
     */
    public Server(int port, ServerCommandProcessor processor) {
        this.port = port;
        this.processor = processor;
    }

    /**
     * Запускает основной цикл сервера.
     * <p>
     * Внутри создаются {@link Selector} и {@link ServerSocketChannel}.
     * Сервер ждёт события каналов: подключение, чтение запроса или запись ответа.
     * </p>
     *
     * @throws IOException если не удалось открыть канал, порт или выполнить сетевую операцию
     */
    public void run() throws IOException {
        try (Selector currentSelector = Selector.open();
             ServerSocketChannel serverChannel = ServerSocketChannel.open()) {
            selector = currentSelector;
            
            serverChannel.configureBlocking(false);
            
            serverChannel.bind(new InetSocketAddress(port));
            
            
            serverChannel.register(currentSelector, SelectionKey.OP_ACCEPT);

            int actualPort = ((InetSocketAddress) serverChannel.getLocalAddress()).getPort();
            System.out.println("Сервер запущен на порту " + actualPort);
            System.out.println("Ожидание подключений...");
            logger.info("Сервер запущен на порту {}", actualPort);
            logger.info("Ожидание подключений");

            while (running) {
                
                currentSelector.select();
                runSelectorTasks();
                
                
                
                
                
                Iterator<SelectionKey> iterator = currentSelector.selectedKeys().iterator();

                while (iterator.hasNext()) {
                    
                    
                    SelectionKey key = iterator.next();
                    iterator.remove();
                    
                    if (!key.isValid()) {
                        continue;
                    }

                    try {
                        
                        if (key.isAcceptable()) {
                            acceptClient(key, currentSelector);
                        } else if (key.isReadable()) {
                            startReadTask(key);
                        } else if (key.isWritable()) {
                            startWriteTask(key);
                        }
                    } catch (IOException e) {
                        
                        handleKeyError(key, e);
                    }
                }
            }

            closeClients(currentSelector);
        } finally {
            readPool.shutdownNow();
            writePool.shutdownNow();
            selector = null; 
        }
    }

    /**
     * Останавливает серверный цикл.
     * <p>
     * Метод может вызываться из другого потока, поэтому поле {@code running}
     * объявлено как {@code volatile}, а selector пробуждается через {@code wakeup()}.
     * </p>
     */
    public void stop() {
        running = false;
        logger.info("Получена команда остановки сервера");
        Selector currentSelector = selector;
        if (currentSelector != null) {
            
            currentSelector.wakeup();
        }
    }

    /**
     * Принимает новое подключение клиента.
     * <p>
     * После accept создаётся {@link SocketChannel} конкретного клиента,
     * и этот канал регистрируется в selector на чтение.
     * </p>
     *
     * @param key ключ серверного канала
     * @param selector selector, в который нужно добавить клиентский канал
     * @throws IOException если не удалось принять или настроить клиента
     */
    private void acceptClient(SelectionKey key, Selector selector) throws IOException {
        
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        
        SocketChannel clientChannel = serverChannel.accept();

        if (clientChannel == null) {
            return;
        }

        clientChannel.configureBlocking(false);
        
        clientChannel.register(selector, SelectionKey.OP_READ, new ClientState());

        String remoteAddress = String.valueOf(clientChannel.getRemoteAddress());
        System.out.println("Новое подключение: " + remoteAddress);
        logger.info("Новое подключение: {}", remoteAddress);
    }

    /**
     * Передаёт чтение запроса в {@link ForkJoinPool}.
     *
     * @param key ключ клиентского канала
     */
    private void startReadTask(SelectionKey key) {
        key.interestOps(key.interestOps() & ~SelectionKey.OP_READ);

        try {
            readPool.execute(() -> readRequest(key));
        } catch (RejectedExecutionException e) {
            closeClientInSelector(key, "Сервер завершает работу");
        }
    }

    /**
     * Читает запрос клиента в {@link ForkJoinPool}.
     *
     * @param key ключ клиентского канала
     */
    private void readRequest(SelectionKey key) {
        try {
            SocketChannel clientChannel = (SocketChannel) key.channel();

            ClientState state = (ClientState) key.attachment();

        
        
            int bytesRead = clientChannel.read(state.readBuffer);
        
            if (bytesRead == -1) {
                closeClientInSelector(key, "Клиент отключился");
                return;
            }

        
            byte[] payload = FramedMessageCodec.tryReadPayload(state.readBuffer);
        
            if (payload == null) {
                enableRead(key);
                return;
            }
        
        
            startCommandThread(key, payload);
        } catch (IOException e) {
            closeClientInSelector(key, "Ошибка клиента: " + e.getMessage());
        }
    }

    /**
     * Запускает обработку полученной команды в отдельном потоке.
     *
     * @param key ключ клиентского канала
     * @param payload байты сериализованного запроса
     */
    private void startCommandThread(SelectionKey key, byte[] payload) {
        Thread commandThread = new Thread(() -> processPayload(key, payload), "command-handler");
        commandThread.start();
    }

    /**
     * Выполняет команду и готовит ответ клиенту.
     *
     * @param key ключ клиентского канала
     * @param payload байты сериализованного запроса
     */
    private void processPayload(SelectionKey key, byte[] payload) {
        CommandResponse response = handlePayload(payload);

        try {
            ByteBuffer responseBuffer = FramedMessageCodec.encodeToBuffer(response);
            runInSelector(() -> {
                if (!key.isValid()) {
                    return;
                }
                ClientState state = (ClientState) key.attachment();
                state.writeBuffer = responseBuffer;
                key.interestOps(SelectionKey.OP_WRITE);
            });
        } catch (IOException e) {
            closeClientInSelector(key, "Не удалось подготовить ответ клиенту");
        }
    }

    /**
     * Передаёт отправку ответа в Fixed thread pool.
     *
     * @param key ключ клиентского канала
     */
    private void startWriteTask(SelectionKey key) {
        key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);

        try {
            writePool.execute(() -> writeResponse(key));
        } catch (RejectedExecutionException e) {
            closeClientInSelector(key, "Сервер завершает работу");
        }
    }

    /**
     * Превращает байты запроса в объект команды и выполняет её.
     *
     * @param payload байты сериализованного объекта запроса
     * @return ответ, который нужно отправить клиенту
     */
    private CommandResponse handlePayload(byte[] payload) {
        try {
            
            Object object = FramedMessageCodec.decodePayload(payload);
            
            
            if (object instanceof CommandRequest request) {
                System.out.println("Получен запрос: " + request.getClass().getSimpleName());
                logger.info("Получен запрос: {}", request.getClass().getSimpleName());
                
                
                return processor.process(request);
            }
            String objectType = object == null ? "null" : object.getClass().getName();
            logger.warn("Сервер получил объект неизвестного типа: {}", objectType);
            return new CommandResponse(false, "Сервер получил неизвестный объект.", null);
        
            
        } catch (IOException | ClassNotFoundException e) {
            logger.warn("Не удалось прочитать запрос", e);
            return new CommandResponse(false, "Не удалось прочитать запрос. Возможно, данные повреждены.", null);
        } catch (RuntimeException e) {
            logger.warn("Не удалось обработать запрос", e);
            return new CommandResponse(false, "Не удалось обработать запрос на сервере.", null);
        }
    }

    /**
     * Отправляет подготовленный ответ клиенту.
     * <p>
     * В неблокирующем режиме {@code write()} может отправить не все байты сразу,
     * поэтому метод проверяет {@code hasRemaining()}.
     * </p>
     *
     * @param key ключ клиентского канала
     */
    private void writeResponse(SelectionKey key) {
        try {
            if (!key.isValid()) {
                return;
            }

            SocketChannel clientChannel = (SocketChannel) key.channel();
            ClientState state = (ClientState) key.attachment();

            if (state.writeBuffer == null) {
                enableRead(key);
                return;
            }

            clientChannel.write(state.writeBuffer);
            if (state.writeBuffer.hasRemaining()) {
                enableWrite(key);
                return;
            }

            runInSelector(() -> {
                if (!key.isValid()) {
                    return;
                }
                state.writeBuffer = null;
                key.interestOps(SelectionKey.OP_READ);
                System.out.println("Ответ отправлен клиенту.");
                logger.info("Ответ отправлен клиенту");
            });
        } catch (IOException e) {
            closeClientInSelector(key, "Ошибка отправки ответа: " + e.getMessage());
        } catch (RuntimeException e) {
            closeClientInSelector(key, "Ошибка отправки ответа");
        }
    }

    private void enableRead(SelectionKey key) {
        runInSelector(() -> {
            if (key.isValid()) {
                key.interestOps(SelectionKey.OP_READ);
            }
        });
    }

    private void enableWrite(SelectionKey key) {
        runInSelector(() -> {
            if (key.isValid()) {
                key.interestOps(SelectionKey.OP_WRITE);
            }
        });
    }

    private void closeClientInSelector(SelectionKey key, String reason) {
        runInSelector(() -> {
            if (key.isValid()) {
                closeClient(key, reason);
            }
        });
    }

    private void runInSelector(Runnable task) {
        selectorTasks.add(task);
        Selector currentSelector = selector;
        if (currentSelector != null) {
            currentSelector.wakeup();
        }
    }

    private void runSelectorTasks() {
        Runnable task;
        while ((task = selectorTasks.poll()) != null) {
            task.run();
        }
    }

    /**
     * Обрабатывает ошибку отдельного канала.
     * <p>
     * Ошибка одного клиента не должна останавливать весь сервер.
     * </p>
     *
     * @param key ключ канала, на котором произошла ошибка
     * @param e ошибка ввода-вывода
     */
    private void handleKeyError(SelectionKey key, IOException e) {
        if (key.channel() instanceof ServerSocketChannel) {
            System.out.println("Не удалось принять подключение: " + e.getMessage());
            logger.warn("Не удалось принять подключение", e);
            return;
        }

        closeClient(key, "Ошибка клиента: " + e.getMessage());
    }

    /**
     * Закрывает соединение с конкретным клиентом и убирает его из selector.
     *
     * @param key ключ клиентского канала
     * @param reason причина закрытия для вывода и лога
     */
    private void closeClient(SelectionKey key, String reason) {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        String remoteAddress = getRemoteAddress(clientChannel);
        System.out.println(reason + ": " + remoteAddress);
        logger.info("{}: {}", reason, remoteAddress);
        
        key.cancel();
        try {
            
            clientChannel.close();
        } catch (IOException e) {
            System.out.println("Не удалось закрыть клиентское соединение: " + e.getMessage());
            logger.warn("Не удалось закрыть клиентское соединение", e);
        }
    }

    /**
     * Возвращает адрес клиента для сообщений в консоль и лог.
     *
     * @param clientChannel канал клиента
     * @return строка с адресом клиента или сообщение, что адрес недоступен
     */
    private String getRemoteAddress(SocketChannel clientChannel) {
        try {
            
            
            return String.valueOf(clientChannel.getRemoteAddress());
        } catch (IOException e) {
            return "адрес недоступен";
        }
    }

    /**
     * Закрывает все клиентские каналы при завершении сервера.
     *
     * @param selector selector, в котором зарегистрированы каналы
     */
    private void closeClients(Selector selector) {
        for (SelectionKey key : selector.keys()) {
            if (key.channel() instanceof SocketChannel) {
                closeClient(key, "Закрытие клиентского соединения");
            }
        }
    }

    /**
     * Состояние обмена с одним клиентом.
     * <p>
     * У каждого клиента свои буферы чтения и записи,
     * потому что запросы и ответы разных клиентов нельзя смешивать.
     * </p>
     */
    private static class ClientState {
        
        private final ByteBuffer readBuffer = ByteBuffer.allocate(1024 * 1024 + Integer.BYTES);
        
        private ByteBuffer writeBuffer;
    }
}
