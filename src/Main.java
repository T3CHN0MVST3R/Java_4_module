import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // Приветствие
        System.out.println("===============================================\n" +
                "Добро пожаловать в приложение прогноза погоды!\n" +
                "===============================================\n" +
                "Это приложение получает метеорологические данные с Яндекс.Погода.\n");

        // Ввод API-ключа
        System.out.println("Пожалуйста, введите ваш публичный API-ключ для работы с Яндекс.Погода:");
        String apiKey = scanner.nextLine();

        // Ввод координат (широта и долгота)
        System.out.println("Введите широту:");
        double lat = scanner.nextDouble();
        System.out.println("Введите долготу:");
        double lon = scanner.nextDouble();

        // Ввод периода (limit)
        System.out.println("Введите количество дней для расчёта средней температуры (от 1 до 7):");
        int days = scanner.nextInt();

        // Получаем данные о погоде
        System.out.println("Получаем данные о погоде...");
        String jsonResponse = getWeatherData(lat, lon, days, apiKey);
        if (jsonResponse.isEmpty()) {
            System.out.println("Не удалось получить данные от API.");
            return; // Выходим, если данные не получены
        }

        StringBuilder formattedResult = formatWeatherData(jsonResponse);

        // Выбор способа сохранения результата
        System.out.println("Выберите, как вы хотите сохранить результат:\n1. Сохранить результат в текстовом формате\n2. Сохранить результат в виде JSON-файла\n3. Вывести результат в консоль");
        int saveOption = scanner.nextInt();
        scanner.nextLine();

        switch (saveOption) {
            case 1:
                System.out.println("Введите путь для сохранения файла или нажмите Enter для сохранения в текущую директорию:");
                String textPath = scanner.nextLine();
                saveToTextFile(formattedResult.toString(), textPath.isEmpty() ? "weather_result.txt" : textPath);
                break;
            case 2:
                System.out.println("Введите путь для сохранения файла или нажмите Enter для сохранения в текущую директорию:");
                String jsonPath = scanner.nextLine();
                saveToJsonFile(jsonResponse, jsonPath.isEmpty() ? "weather_result.json" : jsonPath);
                break;
            case 3:
                // Дополнительный выбор формата вывода в консоль
                System.out.println("Выберите формат вывода в консоль:\n1. Оригинальный JSON\n2. Текстовый формат");
                int displayOption = scanner.nextInt();
                scanner.nextLine();

                if (displayOption == 1) {
                    System.out.println(jsonResponse); // Вывод оригинального JSON
                } else if (displayOption == 2) {
                    System.out.println(formattedResult.toString()); // Вывод отформатированного текста
                } else {
                    System.out.println("Неверная опция.");
                }
                break;
            default:
                System.out.println("Неверная опция.");
        }

        System.out.println("Работа завершена.");
    }

    // Метод для получения данных о погоде
    public static String getWeatherData(double lat, double lon, int limit, String apiKey) {
        String jsonResponse = "";
        try {
            String urlString = "https://api.weather.yandex.ru/v2/forecast?lat=" + lat + "&lon=" + lon + "&limit=" + limit;
            URL url = new URL(urlString);

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("X-Yandex-API-Key", apiKey);

            // Проверяем успешность запроса
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                System.out.println("Ошибка: Сервер вернул код " + responseCode);
                return "";
            }

            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuilder content = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }

            in.close();
            connection.disconnect();

            jsonResponse = content.toString();

        } catch (IOException e) {
            System.out.println("Ошибка при подключении к API: " + e.getMessage());
        }
        return jsonResponse;
    }

    // Форматирование данных
    public static StringBuilder formatWeatherData(String jsonData) {
        StringBuilder result = new StringBuilder();
        try {
            JSONObject jsonResponse = new JSONObject(jsonData);
            JSONArray forecasts = jsonResponse.getJSONArray("forecasts");

            result.append("==========================\n");
            result.append("Основная информация в указанной точке\n");

            double totalTemp = 0;
            for (int i = 0; i < forecasts.length(); i++) {
                JSONObject forecast = forecasts.getJSONObject(i);
                JSONObject dayPart = forecast.getJSONObject("parts").getJSONObject("day");
                int temp = dayPart.getInt("temp_avg");
                totalTemp += temp;
                result.append("Температура за день ").append(i + 1).append(": ").append(temp).append("°C\n");
            }

            double averageTemp = totalTemp / forecasts.length();
            result.append("Средняя температура: ").append(averageTemp).append("°C\n");
            result.append("==========================\n");

            result.append("Дополнительная информация:\n");
            JSONObject fact = jsonResponse.getJSONObject("fact");
            int tempNow = fact.getInt("temp");
            String condition = fact.getString("condition");
            double windSpeed = fact.getDouble("wind_speed");
            int pressure = fact.getInt("pressure_mm");

            result.append("Текущая температура: ").append(tempNow).append("°C\n");
            result.append("Погодное условие: ").append(condition).append("\n");
            result.append("Скорость ветра: ").append(windSpeed).append(" м/с\n");
            result.append("Давление: ").append(pressure).append(" мм рт. ст.\n");
            result.append("==========================\n");

        } catch (Exception e) {
            System.out.println("Ошибка при форматировании данных: " + e.getMessage());
        }
        return result;
    }

    // Сохранение в текстовый файл
    public static void saveToTextFile(String data, String filePath) {
        try {
            File file = new File(filePath);
            if (!file.isAbsolute()) {
                file = Paths.get(System.getProperty("user.dir"), filePath).toFile();
            }
            try (PrintWriter out = new PrintWriter(new FileWriter(file))) {
                out.println(data);
                System.out.println("Результат сохранён в файл: " + file.getAbsolutePath());
            }
        } catch (IOException e) {
            System.out.println("Ошибка при сохранении в текстовый файл: " + e.getMessage());
        }
    }

    // Сохранение в JSON файл
    public static void saveToJsonFile(String jsonData, String filePath) {
        try {
            File file = new File(filePath);
            if (!file.isAbsolute()) {
                file = Paths.get(System.getProperty("user.dir"), filePath).toFile();
            }
            try (PrintWriter out = new PrintWriter(new FileWriter(file))) {
                out.println(jsonData);
                System.out.println("Результат сохранён в файл: " + file.getAbsolutePath());
            }
        } catch (IOException e) {
            System.out.println("Ошибка при сохранении в JSON-файл: " + e.getMessage());
        }
    }
}
