import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileReader;
import java.net.Socket;

public class MainCl {
    public static void main(String[] args) throws Exception {
        // Проверяем, что переданы аргументы командной строки
        if (args.length < 2) {
            System.out.println("Usage: java Client <filename> <server-address>");
            return;
        }

        String filename = args[0];
        String serverAddress = args[1];

        // Читаем содержимое файла
        StringBuilder fileContent = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                fileContent.append(line).append('\n');
            }
        }

        // Устанавливаем соединение с сервером
        try (Socket clientSocket = new Socket(serverAddress, 12345)) {
            // Отправляем содержимое файла на сервер
            DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
            outToServer.writeBytes(fileContent.toString());

            System.out.println("File sent to server successfully.");
        }
    }
}
