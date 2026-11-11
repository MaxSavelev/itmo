import com.fastcgi.FCGIInterface;

import java.io.PrintStream;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class Main {
    private static final String[] REQUIRED_PARAMETERS = {"x", "y", "r"};
    private static final int MAX_X_LENGTH = 1000;
    public static void main(String[] args) {
        PrintStream console = System.err;
        FCGIInterface fcgiInterface = new FCGIInterface();
        console.println("Программа запущена");
        console.println("Ожидаю запрос...");

        while (fcgiInterface.FCGIaccept() != -1) {
            console.println("Запрос принят");
            long startTime = System.nanoTime();
            try {
                if (!"GET".equals(System.getProperty("REQUEST_METHOD"))) {
                    sendResponse("405 Method Not Allowed",
                            createErrorJson("Для этого запроса разрешён только метод GET."),
                            "Allow: GET");
                    continue;
                }

                String queryString = System.getProperty("QUERY_STRING");
                Map<String, String> parameters = parseQueryString(queryString);
                validateRequiredParameters(parameters);
                BigDecimal x = parseX(parameters.get("x"));
                int y = parseY(parameters.get("y"));
                int r = parseR(parameters.get("r"));
                boolean hit = isPointInArea(x, y, r);
                long endTime = System.nanoTime();
                long difference = endTime - startTime;
                Instant data = Instant.now();
                String result = createResult(x,y,r,hit, data, difference);
                sendResponse("200 OK", result);
                console.println("Время выполнения: " + difference);
                console.println("Дата выполнения: " + data);
                console.println("Получены параметры: " + parameters);
                console.println("Результат: " + (hit ? "Попадание" : "Промах"));
                console.println("\n"+ result);
            } catch (IllegalArgumentException e) {
                console.println("Некорректные параметры запроса: " + e.getMessage());
                String error = createErrorJson(e.getMessage());
                sendResponse("400 Bad Request", error);
            } finally {
                console.println("Ожидаю запрос...");
            }
        }
    }

    private static Map<String, String> parseQueryString(String queryString) {
        if (queryString == null || queryString.isBlank()) {
            throw new IllegalArgumentException("Не передана строка параметров.");
        }

        Map<String, String> parameters = new HashMap<>();

        for (String pair : queryString.split("&", -1)) {
            String[] parts = pair.split("=", 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException("В паре параметра отсутствует знак '='.");
            }

            String name = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
            String value = URLDecoder.decode(parts[1], StandardCharsets.UTF_8);

            if (name.isBlank()) {
                throw new IllegalArgumentException("Имя параметра не должно быть пустым.");
            }
            if (parameters.containsKey(name)) {
                throw new IllegalArgumentException("Параметр '" + name + "' передан повторно.");
            }

            parameters.put(name, value);
        }

        return parameters;
    }

    private static void validateRequiredParameters(Map<String, String> parameters) {
        for (String name : REQUIRED_PARAMETERS) {
            String value = parameters.get(name);
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("Параметр '" + name + "' отсутствует или пуст.");
            }
        }
    }

    private static int parseY(String value) {
        int y;
        try {
            y = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Y должен быть целым числом.", e);
        }
        if (y > 5 || y < -3) {
            throw new IllegalArgumentException("Y должен быть от -3 до 5 включительно.");
        }
        return y;
    }

    private static int parseR(String value) {
        int r;
        try {
            r = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("R должен быть целым числом.", e);
        }
        if (r > 5 || r < 1) {
            throw new IllegalArgumentException("R должен быть от 1 до 5 включительно.");
        }
        return r;
    }

    private static BigDecimal parseX(String value) {
        if (value.length() > MAX_X_LENGTH) {
            throw new IllegalArgumentException("Запись X слишком длинная: допускается не более "
                    + MAX_X_LENGTH + " символов.");
        }
        if (!value.matches("[+-]?\\d+(\\.\\d+)?")) {
            throw new IllegalArgumentException("X должен быть записан обычным десятичным числом");
        }
        BigDecimal x;
        try {
            x = new BigDecimal(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("X должен быть числом.", e);
        }
        if (x.compareTo(new BigDecimal("5")) >= 0 || x.compareTo(new BigDecimal("-5")) <= 0) {
            throw new IllegalArgumentException("X должен быть от -5 до 5 не включительно.");
        }
        return x;
    }

    private static boolean isPointInArea(BigDecimal x, int y, int r) {
        BigDecimal yValue = BigDecimal.valueOf(y);
        BigDecimal rValue = BigDecimal.valueOf(r);
        BigDecimal two = BigDecimal.valueOf(2);

        boolean inCircle =
                x.compareTo(BigDecimal.ZERO) >= 0 &&
                        y >= 0 &&
                        x.multiply(x).add(yValue.multiply(yValue))
                                .multiply(BigDecimal.valueOf(4))
                                .compareTo(rValue.multiply(rValue)) <= 0;

        boolean inTriangle =
                x.compareTo(BigDecimal.ZERO) >= 0 &&
                        y <= 0 &&
                        x.subtract(yValue.multiply(two)).compareTo(rValue) <= 0;

        boolean inRectangle =
                x.compareTo(BigDecimal.ZERO) <= 0 &&
                        x.multiply(two).compareTo(rValue.negate()) >= 0 &&
                        y >= -r &&
                        y <= 0;

        return inCircle || inTriangle || inRectangle;
    }

    public static String createResult(BigDecimal x, int y, int r, boolean hit, Instant checkedAt, long executionTimeNs) {
        return "{\n"
                + "  \"x\": \"" + x.toPlainString() + "\",\n"
                + "  \"y\": " + y + ",\n"
                + "  \"r\": " + r + ",\n"
                + "  \"hit\": " + hit + ",\n"
                + "  \"checkedAt\": \"" + checkedAt.toString() + "\",\n"
                + "  \"executionTimeNs\": " + executionTimeNs + "\n"
                + "}";
    }

    private static String createErrorJson(String message) {
        StringBuilder escapedMessage = new StringBuilder();

        for (int i = 0; i < message.length(); i++) {
            char character = message.charAt(i);
            if (character == '"' || character == '\\') {
                escapedMessage.append('\\').append(character);
            } else if (character < 0x20) {
                escapedMessage.append(String.format("\\u%04x", (int) character));
            } else {
                escapedMessage.append(character);
            }
        }

        return "{\n  \"error\": \"" + escapedMessage + "\"\n}";
    }

    private static void sendResponse(String status, String body, String... additionalHeaders) {
        byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
        String headers = "Status: " + status + "\r\n"
                + "Content-Type: application/json; charset=UTF-8\r\n"
                + "Content-Length: " + bodyBytes.length + "\r\n";

        for (String header : additionalHeaders) {
            headers += header + "\r\n";
        }
        headers += "\r\n";

        System.out.print(headers);
        System.out.write(bodyBytes, 0, bodyBytes.length);
        System.out.flush();
    }
}
