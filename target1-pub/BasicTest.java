public class BasicTest {
    BasicTest f;

    static void fun1() {
        BasicTest v1 = new BasicTest();
        BasicTest v2 = new BasicTest();
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
