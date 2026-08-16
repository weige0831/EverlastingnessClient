import net.everlastingness.client.common.config.ClientConfig;
import java.io.StringReader;

public class CfgParse {
    public static void main(String[] a) throws Exception {
        System.out.println(ClientConfig.parse(new StringReader("{\"a\":true,\"b\":false}")));
        System.out.println(ClientConfig.parse(new StringReader("{\n  \"x\": false,\n  \"y\": true\n}")));
        System.out.println(ClientConfig.parse(new StringReader("{}")));
    }
}
