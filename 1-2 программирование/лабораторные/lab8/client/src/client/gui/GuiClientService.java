package client.gui;

import client.Client;
import common.request.AuthenticatedRequest;
import common.request.CommandRequest;
import common.request.LoginRequest;
import common.request.RegisterRequest;
import common.response.CommandResponse;

import java.io.IOException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Locale;

/**
 * Отправляет запросы GUI-клиента на сервер.
 */
public final class GuiClientService {
    private final Client client;
    private final GuiSession session;
    private GuiMessages messages;

    public GuiClientService(String host, int port, GuiSession session) {
        this.client = new Client(host, port);
        this.session = session;
        messages = GuiMessages.forLocale(Locale.forLanguageTag("ru"));
    }

    public void setMessages(GuiMessages messages) {
        this.messages = java.util.Objects.requireNonNull(messages);
    }

    /**
     * Отправляет запрос входа и сохраняет пользователя при успехе.
     *
     * @param login логин
     * @param password пароль
     * @return ответ сервера
     * @throws GuiClientException если не удалось обменяться данными с сервером
     */
    public CommandResponse login(String login, String password) throws GuiClientException {
        LoginRequest request = new LoginRequest(login, password);
        CommandResponse response = sendRaw(request);
        if (response.isSuccess()) {
            session.setCredentials(login, password);
        }
        return response;
    }

    /**
     * Отправляет запрос регистрации и сохраняет пользователя при успехе.
     *
     * @param login логин
     * @param password пароль
     * @return ответ сервера
     * @throws GuiClientException если не удалось обменяться данными с сервером
     */
    public CommandResponse register(String login, String password) throws GuiClientException {
        RegisterRequest request = new RegisterRequest(login, password);
        CommandResponse response = sendRaw(request);
        if (response.isSuccess()) {
            session.setCredentials(login, password);
        }
        return response;
    }

    /**
     * Отправляет обычную команду вместе с логином и паролем текущего пользователя.
     *
     * @param request команда
     * @return ответ сервера
     * @throws GuiClientException если пользователь не вошёл или сервер недоступен
     */
    public CommandResponse send(CommandRequest request) throws GuiClientException {
        if (!session.isAuthorized()) {
            throw new GuiClientException(messages.get("error.loginRequired"));
        }
        return sendRaw(new AuthenticatedRequest(session.getLogin(), session.getPassword(), request));
    }

    private CommandResponse sendRaw(CommandRequest request) throws GuiClientException {
        try {
            return client.send(request);
        } catch (UnknownHostException e) {
            throw new GuiClientException(messages.get("error.hostUnknown"), e);
        } catch (ConnectException e) {
            throw new GuiClientException(messages.get("error.serverUnavailable"), e);
        } catch (SocketTimeoutException e) {
            throw new GuiClientException(messages.get("error.serverTimeout"), e);
        } catch (NoRouteToHostException e) {
            throw new GuiClientException(messages.get("error.noRoute"), e);
        } catch (IOException e) {
            throw new GuiClientException(messages.get("error.networkExchange"), e);
        } catch (ClassNotFoundException e) {
            throw new GuiClientException(messages.get("error.unknownResponse"), e);
        }
    }
}
