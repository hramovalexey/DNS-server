import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;

// тестовый клиент отправляет 50 раз запрос для адреса example.com
public class TestClient {
    public static void main(String[] args) throws IOException {
        // пример запроса адреса example.com
        byte[] message = {
                0xA, 0xA, 0x01, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x07, 0x65,
                0x78, 0x61, 0x6d, 0x70, 0x6c, 0x65, 0x03, 0x63, 0x6f, 0x6d, 0x00, 0x00, 0x01, 0x00,
                0x01
        };
        InetAddress address = InetAddress.getByName("localhost");
        // Отправка
        DatagramPacket packet = new DatagramPacket(message, message.length, address, 53);
        DatagramSocket socket = new DatagramSocket(54);

        for (int i = 0; i < 50; i++) {
            socket.send(packet);
        }

        // Получение первого ответа
        byte[] bufRes = new byte[512];
        DatagramPacket packetRes = new DatagramPacket(bufRes, bufRes.length);
        socket.receive(packetRes);
        System.out.println(Arrays.toString(packetRes.getData()));
    }
}
