import java.io.IOException;

public class BasicTest {
    BasicTest f;

    static void fun1() throws IOException {
        BasicTest v1 = new BasicTest();
        BasicTest v2 = new BasicTest(), v3 = new BasicTest();
        v3.f = v2;
        v2.f = v1;
        int x = 2;
        int y = 3;
        if (x > y) {
            v2.f = v2;
        } else {
            v2.f = v1;
        }
    }
}
