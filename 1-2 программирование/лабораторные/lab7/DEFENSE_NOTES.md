# Заметки для защиты lab6

Проект: `/Users/makssavelev/lab6`

Тема: разделение старой lab5 на клиент-серверную архитектуру по TCP.

## Главная идея

Раньше команды выполнялись внутри одного консольного приложения.

Теперь:

- клиент читает команды пользователя;
- клиент превращает команду в объект запроса;
- запрос сериализуется и отправляется серверу по TCP;
- сервер выполняет команду над коллекцией;
- сервер возвращает объект ответа;
- клиент печатает результат.

Формат сообщения:

```text
[4 байта длины][байты сериализованного объекта]
```

## Модули проекта

Проект физически разделён на три модуля:

- `common` - общие классы, которые нужны и клиенту, и серверу;
- `client` - клиентское приложение;
- `server` - серверное приложение, хранение коллекции и парсер файла.

Зависимости:

```text
client -> common
server -> common
```

`common` нужен, чтобы не дублировать классы запросов, ответов, сериализации и данных.

Старый пакет `cmd` из lab5 удалён. Он запускал старую монолитную консольную программу, а в lab6 точками входа должны быть `server.ServerMain` и `client.ClientMain`.

## Основные новые пакеты

`common/src/common/request`

Классы запросов команд. Например:

- `HelpRequest`
- `ShowRequest`
- `AddRequest`
- `UpdateRequest`
- `ExecuteScriptRequest`

Нужны, чтобы клиент отправлял на сервер не строку, а понятный объект команды.

`common/src/common/response/CommandResponse.java`

Общий ответ сервера:

- `success` - успешно ли выполнена команда;
- `message` - текстовое сообщение;
- `result` - результат команды, например коллекция или `CollectionInfo`.

`common/src/common/io`

Сериализация и сетевой формат:

- `ObjectSerializer` превращает объект в байты и обратно;
- `FramedMessageCodec` добавляет длину сообщения перед байтами объекта.

`common/src/common/dto/CollectionInfo.java`

Информация о коллекции. Лежит в `common`, потому что сервер создаёт этот объект, а клиент получает его в ответе на команду `info`.

Внутри него хранятся строки с названиями классов, а не `Class<?>`. Так клиенту не нужно иметь серверный класс `storage.VehicleCollection` в своём модуле.

## Сервер

Файлы:

- `server/src/server/ServerMain.java`
- `server/src/server/Server.java`
- `server/src/server/ServerCommandProcessor.java`

`ServerMain`

Что делает:

- читает порт;
- загружает коллекцию из `FILE_NAME`;
- запускает `Server`;
- читает серверные команды `save` и `exit`.

Важное изменение:

`save` и `exit` теперь доступны именно на сервере, а не на клиенте.

`Server`

Что делает:

- использует `ServerSocketChannel`;
- использует `Selector`;
- принимает клиентов через `OP_ACCEPT`;
- читает запросы через `OP_READ`;
- отправляет ответы через `OP_WRITE`;
- хранит состояние каждого клиента в `ClientState`.

Почему нужен `ClientState`:

- у каждого клиента свой `readBuffer`;
- у каждого клиента свой `writeBuffer`;
- в неблокирующем режиме `write()` может отправить не все байты сразу.

Важное изменение:

Ошибка одного клиента больше не останавливает сервер. Если один клиент прислал мусор или оборвал соединение, сервер закрывает только его канал и продолжает работать.

`ServerCommandProcessor`

Что делает:

- получает `CommandRequest`;
- через `instanceof` понимает тип команды;
- вызывает нужный метод коллекции;
- возвращает `CommandResponse`.

Важное изменение:

Для `add`, `add_if_max`, `remove_greater` сервер создаёт новый `Vehicle` у себя, чтобы `id` генерировался на серверной стороне, а не приходил от клиента.

## Клиент

Файлы:

- `client/src/client/ClientMain.java`
- `client/src/client/Client.java`
- `client/src/client/ClientCommandParser.java`
- `client/src/client/ClientVehicleReader.java`
- `client/src/client/ClientFuelTypeParser.java`

`ClientMain`

Что делает:

- читает команды из консоли;
- обрабатывает локальные команды `exit` и `execute_script`;
- остальные команды отправляет серверу.

`Client`

Что делает:

- создаёт обычный `Socket`;
- подключается к серверу;
- отправляет запрос;
- ждёт ответ.

Важное изменение:

Добавлен timeout на ожидание ответа:

```java
socket.setSoTimeout(READ_TIMEOUT_MS);
```

Зачем:

`Selector` есть на сервере, а клиент использует обычный блокирующий `Socket`. Если сервер не ответит, клиент без timeout может зависнуть навсегда.

`ClientCommandParser`

Что делает:

- превращает строку команды в объект запроса;
- например `show` превращает в `ShowRequest`;
- `filter_by_fuel_type 3` превращает в `FilterByFuelTypeRequest`.

`ClientVehicleReader`

Что делает:

- вводит поля `Vehicle` на клиенте;
- используется для `add`, `update`, `add_if_max`, `remove_greater`;
- умеет работать с подсказками в консоли и без подсказок при выполнении скрипта.

`ClientFuelTypeParser`

Что делает:

- парсит `FuelType`;
- поддерживает ввод по названию и по номеру:

```text
1 - GASOLINE
2 - ELECTRICITY
3 - DIESEL
4 - MANPOWER
5 - NUCLEAR
```

## execute_script

Реализован на клиенте.

Почему на клиенте:

Файл скрипта находится у пользователя на клиентской машине. Сервер не обязан иметь доступ к этому пути.

Что происходит:

- клиент открывает файл;
- читает команды построчно;
- каждую команду обрабатывает как обычную пользовательскую команду;
- запросы отправляются серверу;
- если в скрипте есть `add`, следующие строки используются как поля `Vehicle`.

Добавлена защита от рекурсии:

```text
script1 -> script1
script1 -> script2 -> script1
```

## VehicleCollection

Файл: `server/src/storage/VehicleCollection.java`

Что улучшено:

- `show()` больше не возвращает внутренний список коллекции напрямую;
- теперь `show()` возвращает копию списка через Stream API;
- после `update()` коллекция снова сортируется по имени;
- `getById()` ищет элемент через `stream().filter().findFirst()`;
- `head()` берёт первый элемент через `stream().findFirst()`;
- `addIfMax()` ищет максимальный элемент через `stream().max()`;
- `removeById()` сначала ищет элемент через `getById()`, а потом удаляет его;
- `removeGreater()` через `stream().filter()` собирает элементы для удаления;
- `save()` переписан через Stream API;
- `countByNumberOfWheels()` переписан через Stream API;
- `filterByFuelType()` переписан через Stream API;
- `filterStartsWithName()` переписан через Stream API.

Почему `show()` возвращает копию:

Если вернуть настоящий `list`, внешний код сможет изменить коллекцию в обход методов `add`, `removeById`, `clear`.

Например:

```java
collection.show().clear();
```

Поэтому безопаснее:

```java
return new LinkedList<>(list);
```

## Валидация Vehicle и Coordinates

Файлы:

- `common/src/data/Vehicle.java`
- `common/src/data/Coordinates.java`

Что изменено:

`assert` заменён на обычные проверки `if`.

Почему:

В Java `assert` обычно выключен. Значит, проверка может вообще не выполниться.

Теперь при неправильном поле выбрасывается:

```java
IllegalArgumentException
```

Например:

```java
if (numberOfWheels <= 0) {
    throw new IllegalArgumentException("Поле numberOfWheels должно быть больше 0.");
}
```

## serialVersionUID

Добавлен в классы, которые сериализуются и передаются между клиентом и сервером:

- классы запросов из `common/src/common/request`;
- `CommandResponse`;
- `Vehicle`;
- `Coordinates`;
- `CollectionInfo`.

Пример:

```java
private static final long serialVersionUID = 1L;
```

Зачем:

Java-сериализация использует этот номер как версию класса. Если номер не указать, Java вычисляет его автоматически. После изменений в классе автоматически вычисленный номер может поменяться, и при чтении объекта может возникнуть `InvalidClassException`.

Короткий ответ для защиты:

`serialVersionUID` нужен, чтобы явно зафиксировать версию сериализуемого класса, потому что объекты запросов, ответов и данные коллекции передаются по сети через `ObjectOutputStream` и `ObjectInputStream`.

## Что можно сказать про volatile

В `Server` есть:

```java
private volatile boolean running = true;
```

`running` читает серверный поток в цикле:

```java
while (running)
```

А меняет другой поток - серверная консоль, когда вводим `exit`.

`volatile` нужен, чтобы серверный поток точно увидел изменение `running = false`.

## Как проверить

Компиляция:

```bash
mkdir -p out/common out/client out/server
/opt/homebrew/opt/openjdk@17/bin/javac -d out/common common/src/**/*.java
/opt/homebrew/opt/openjdk@17/bin/javac -cp out/common -d out/client client/src/**/*.java
/opt/homebrew/opt/openjdk@17/bin/javac -cp "out/common:lib/*" -d out/server server/src/**/*.java
```

Запуск сервера:

```bash
FILE_NAME=scripts/data.json /opt/homebrew/opt/openjdk@17/bin/java -Dlog4j.configurationFile=resources/log4j2.xml -cp "out/common:out/server:lib/*" server.ServerMain 5555
```

Запуск клиента:

```bash
/opt/homebrew/opt/openjdk@17/bin/java -cp out/common:out/client client.ClientMain localhost 5555
```

Проверка, что ошибка одного клиента не роняет сервер:

```bash
printf 'abcd' | nc localhost 5555
```

После этого обычный клиент всё равно должен работать.

Проверка `execute_script`:

```text
execute_script scripts/test_script.txt
```

Проверка сломанного JSON:

если файл существует, но внутри неправильный формат или некорректные значения полей,
сервер не падает, а создаёт пустую коллекцию.

## Log4J2

На сервер добавлен Log4J2.

Что логируется:

- запуск сервера;
- загрузка коллекции;
- новое подключение;
- получение запроса;
- обработка команды;
- отправка ответа;
- отключение клиента;
- ошибки клиента;
- серверные команды `save` и `exit`;
- сохранение коллекции.

Конфиг: `resources/log4j2.xml`.

Файл логов после запуска: `logs/server.log`.
