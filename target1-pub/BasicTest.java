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
        int z = System.in.read();
        for (int i = 0; i < 10; i++) {
            if (i % 2 == 0)
                v2.f = v3;
            else
                v2.f = v1;
        }
        // switch (z) {
        // case 4:

        // break;
        // case 6:

        // break;
        // case 5:

        // break;

        // default:
        // break;
        // }
    }
}
