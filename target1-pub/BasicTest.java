import java.io.IOException;

public class BasicTest {
    BasicTest f;

    static void fun1() throws IOException {
        BasicTest v1 = new BasicTest();
        BasicTest v2 = new BasicTest();
        if(v1 == null){
            v1 = v2;
        }
        else {
            v1 = null;
        }
        v2.f = v1;
    }

    private static int something() {
        return 23;
    }

    static void fun2(int i) {
        int x = something();
        x++;
        i = x;
        Object t1 = null;
        Object t2 = new Object();
        Object t3 = new Object();
        if (i > 10) {
            t3 = t1;
        } else {
            t3 = t2;
        }
        t3.toString();
    }

    static void fun3(int i) {
        BasicTest t1 = null;
        BasicTest t2 = new BasicTest();
        t2.f.f = t1;
    }
}
