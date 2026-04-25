package cmd;

import java.util.List;
import java.util.function.Consumer;

/**
 * Описание одной команды.
 * <p>
 * Здесь хранится имя команды, ее описание,
 * список аргументов и действие, которое нужно выполнить.
 * </p>
 *
 * @param name название команды, которое вводит пользователь
 * @param description короткое объяснение, что делает команда
 * @param argNames названия аргументов команды для справки
 * @param exec код, который выполняется при запуске команды
 * @param successMessage сообщение после успешного выполнения команды
 *
 * @author makssavelev
 * @version 1.0
 */
public record Command(String name, String description, List<String> argNames, Consumer<List<String>> exec, String successMessage) {
}
