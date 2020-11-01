import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Основной класс. Сервер запускается, как экземпляр данного класса
public class DnsServer {
    private ArrayList<String> forbidden; // список запрещенных адресов
    private String upper_server; // вышестоящий сервер
    private InetAddress address;
    private byte[] error_message;
    private int timeout; // тайм-аут ожидания от вышестоящего сервера
    private byte[] timeout_message;

    public DnsServer() throws IOException {
        readCfg();
        address = InetAddress.getByName(upper_server);
        byte[] bufClient = new byte[512];
        DatagramSocket socketClient = new DatagramSocket(53);
        ExecutorService service = Executors.newFixedThreadPool(10);
        System.out.println("Сервер запущен");
           while (true) {
              DatagramPacket packetFromClient = new DatagramPacket(bufClient, bufClient.length);
            socketClient.receive(packetFromClient);
            service.execute(new RunMessageHandler(packetFromClient));
        }
    }

    // считывание конфигурационного файла
    private void readCfg() throws IOException {
        File file = new File("dnscfg.properties");
        if (!file.isFile()) System.out.println("Нет файла конфигурации");
        Properties cfg = new Properties();
        cfg.load(new FileReader(file));
        upper_server = cfg.getProperty("upper_server").trim();
        forbidden = new ArrayList<>();
        cfg.stringPropertyNames().stream()
           .filter(x -> cfg.getProperty(x).equals(""))
           .map(x -> x.toLowerCase().trim().replaceFirst("^www\\.", ""))
           .forEach(x -> forbidden.add(x));
        error_message = cfg.getProperty("error_message").getBytes("ASCII");
        timeout_message = cfg.getProperty("timeout_message").getBytes("ASCII");
        timeout = Integer.parseInt(cfg.getProperty("timeout"));
    }

    /*
     проверка имени на наличие в списке запрещенных. Если запрещено, то true
    (проводится также проверка на запрос какого-либо нижестоящего домена из запрещенных)
    Производится поиск подстроки в каждом адресе
    При большом списке запрещенных адресов, считаю, лучше хранить список в древовидной структуре типа Tries
    и производить поиск по дереву, что избавит от необходимости обходить список целиком.
     */

    private boolean checkName(String name) {
        for (String forb : forbidden) {
            if (name.matches(String.format("(^|.*\\.)%s$", forb))) return true;
        }
        return false;
    }

    // Вспомогательный Runnable класс по обработке сообщения от клиента
    private class RunMessageHandler implements Runnable {
        private final DatagramPacket packetFromClient;
        private final int clientPort;
        private final InetAddress clientAddress;

        public RunMessageHandler(DatagramPacket packetFromClient) {
            this.packetFromClient = packetFromClient;
            this.clientPort = packetFromClient.getPort();
            this.clientAddress = packetFromClient.getAddress();
        }

        public void run() {
                     Message messageFromClient = new Message(packetFromClient.getData());
                     try {
                DatagramSocket socketClient = new DatagramSocket();

                // Проверка на запрещенные адреса
                if (checkName(messageFromClient.name)) {
                                   socketClient.send(new DatagramPacket(error_message, error_message.length,
                                                         clientAddress, clientPort));
                    return;
                }
                // Сообщение для вышестоящего сервера
                DatagramPacket packetToServer = new DatagramPacket(messageFromClient.message,
                                                                   messageFromClient.lenght,
                                                                   address, 53);
                DatagramSocket socketToServer = new DatagramSocket();
                socketToServer.setSoTimeout(timeout);
                             socketToServer.send(packetToServer);
                byte[] buf = new byte[512];
                DatagramPacket packetFromServer = new DatagramPacket(buf, buf.length);
                try {
                    socketToServer.receive(packetFromServer);
                }
                // Действие по истечении таймаута
                catch (SocketTimeoutException e) {
                                 socketClient.send(new DatagramPacket(timeout_message, timeout_message.length,
                                                         clientAddress, clientPort));
                    return;
                }
                // Ответ клиенту
                socketClient
                        .send(new DatagramPacket(packetFromServer.getData(),
                                                 packetFromServer.getLength(),
                                                 clientAddress, clientPort));
            }
            catch (IOException e) {
                System.out.println(e.toString());
            }
        }
    }

    public static void main(String[] args) throws IOException {
        DnsServer serv = new DnsServer();
    }
}
