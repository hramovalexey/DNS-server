import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.util.Arrays;

/*
 Вспомогательный класс для разбора DNS запросов от клиента и сервера
 Многие поля добавлены "прозапас", но сейчас никак не используются.
 На данный момент используется поле message, name, lenght
 */
public class Message {
    public final byte[] message;
    public final int id;
    public final int qr; // query = 0, reply = 1
    public final int rd; // recursion = 1, no = 0
    /* rcode используется для тестовых целей (проверка ответов от вышестоящего DNS)
    Кроме того, данное поле можно использовать в дальнейшем при кэшировании ответов (пока кэширование не реализовано)
     0 - no error
     1 - format error
     2 - server failure
     3 - name error
     4 - nor impl
     5 - refused
    */
    public final int rcode;
    public final int ra; // поддерживается рекурсия - 1
    public final int qdcount; // количество запросов
    public String name;
    public final int lenght; // длина сообщения

    public Message(byte[] input) {
        message = Arrays.copyOf(input, input.length);
        lenght = message.length;
        ByteArrayInputStream stream = new ByteArrayInputStream(message);
        byte[] idAr = new byte[2];
        stream.read(idAr, 0, 2);
        id = (idAr[0] << 8 | idAr[1]);
           int b3 = stream.read();
        qr = (b3 & 0x80) >>> 7;
               rd = b3 & 0x1;
              int b4 = stream.read();
        ra = (b4 & 0x80) >> 7;
             rcode = b4 & 0xf;
               qdcount = ((stream.read() << 8) | stream.read()); // b5, b6
               stream.skip(6);
        name = "";
        parseName(stream);
          }


    private void parseName(ByteArrayInputStream stream) {
        int num = stream.read(); // количество символов
              while (num != 0) {
            byte[] bAr = new byte[num];
            stream.read(bAr, 0, bAr.length);
            String str = new String(bAr
                    , Charset.forName("ASCII")
            );
            num = stream.read();
            name = name.concat(str);
            if (num != 0) name = name.concat(".");
                   }
        name = name.trim().toLowerCase().replaceFirst("^www\\.", "");
    }

    // Используется при тестировании
    public String toString() {
        return String
                .format("Message: %s\nid:\t%s\nqr:\t%s\nrd:\t%s\nrcode:\t%s\nra:\t%s\nqdcount:\t%s\nname:\t%s\nlenght:\t%s\n",
                        Arrays.toString(message), id, qr, rd, rcode, ra, qdcount, name, lenght);
    }


    public static void main(String[] args) {

           }
}
