public class BasicTest {
    BasicTest f;

    static void fun1() {
        BasicTest v1 = new BasicTest();
        BasicTest v2 = new BasicTest();
        v2.f = v1;
    }

    static void fun2(int i) {
        Object t1 = null;
        Object t2 = new BasicTest();
        Object t3 = null;
        if (i > 10) {
            t3 = t1;
        } else {
            t3 = t2;
        }
        t3.toString();
    }

    public static void main(String args[]) {
    }
}
