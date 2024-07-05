import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class Main {
    public static void main(String[] args) throws Exception {
        // Проверяем, что переданы аргументы командной строки
        if (args.length < 3) {
            System.out.println("Usage: java Server <port> <max-threads> <save-path>");
            return;
        }

        int port = Integer.parseInt(args[0]);
        int maxThreads = Integer.parseInt(args[1]);
        String savePath = args[2].replace('/', '\\'); // Заменяем прямые слеши на обратные

        // Создаем серверный сокет для прослушивания входящих соединений
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server started on port " + port);

            // Создаем пул потоков
            ThreadPool threadPool = new ThreadPool(maxThreads);

            while (true) {
                // Принимаем входящее соединение от клиента
                Socket clientSocket = serverSocket.accept();

                // Отправляем соединение в пул потоков для обработки
                threadPool.execute(new ClientHandler(clientSocket, savePath));
            }
        }
    }
}

class ClientHandler implements Runnable {
    private Socket clientSocket;
    private String savePath;

    public ClientHandler(Socket clientSocket, String savePath) {
        this.clientSocket = clientSocket;
        this.savePath = savePath;
    }

    @Override
    public void run() {
        try {
            // Читаем содержимое файла от клиента
            DataInputStream inFromClient = new DataInputStream(clientSocket.getInputStream());
            byte[] fileContent = new byte[inFromClient.available()];
            inFromClient.readFully(fileContent);

            // Сохраняем содержимое файла на диск
            String filename = savePath + "\\file-" + System.currentTimeMillis() + ".txt"; // Используем обратные слеши для Windows
            Files.write(Paths.get(filename), fileContent, StandardOpenOption.CREATE_NEW);

            System.out.println("File received and saved: " + filename);
        } catch (Exception e) {

            e.printStackTrace();
        }
    }
}

class ThreadPool {
    private int maxThreads;
    private WorkerThread[] workerThreads;

    public ThreadPool(int maxThreads) {
        this.maxThreads = maxThreads;
        workerThreads = new WorkerThread[maxThreads];
        for (int i = 0; i < maxThreads; i++) {
            workerThreads[i] = new WorkerThread();
            workerThreads[i].start();
        }
    }

    public void execute(Runnable task) {
        synchronized (this) {
            while (getBusyThreadCount() == maxThreads) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        for (WorkerThread workerThread : workerThreads) {
            if (!workerThread.isBusy()) {
                workerThread.execute(task);
                break;
            }
        }
    }

    private int getBusyThreadCount() {
        int count = 0;
        for (WorkerThread workerThread : workerThreads) {
            if (workerThread.isBusy()) {
                count++;
            }
        }
        return count;
    }

    class WorkerThread extends Thread {
        private Runnable task;
        private boolean isBusy;

        public boolean isBusy() {
            return isBusy;
        }

        public synchronized void execute(Runnable task) {
            this.task = task;
            this.isBusy = true;
            notify();
        }

        @Override
        public synchronized void run() {
            while (true) {
                try {
                    if (isBusy) {
                        task.run();
                        isBusy = false;
                        synchronized (ThreadPool.this) {
                            ThreadPool.this.notify();
                        }
                    } else {
                        wait();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
