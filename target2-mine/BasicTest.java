public class BasicTest {
    BasicTest f;

    static public BasicTest f() {
        return new BasicTest();
    }

    public BasicTest g() {
        return new BasicTest();
    }

    void MyTest() {
        BasicTest v1 = new BasicTest();
        BasicTest v2 = new BasicTest();
        v2 = v1;

        BasicTest v3 = f();
        v3.f = v1.f = v2.f = g();
    }

    static void fun1() {
        BasicTest v1 = new BasicTest();
        BasicTest v2 = new BasicTest();
        v2.f = v1;
    }

    static void fun2(int value) {
        BasicTest v1 = new BasicTest();
        BasicTest v2 = new BasicTest();
        v2.f = v2;
        if (value == 100) {
            v2.f = v1;
        }
    }

    static BasicTest fun3() {
        BasicTest v1 = new BasicTest();
        BasicTest v2 = new BasicTest();
        BasicTest v3 = new BasicTest();
        int value = iiscPavUtil.random();
        v2.f = v3;
        while (value < 100) {
            v2.f = new BasicTest();
            value += 1;
        }
        v3.f = v2.f;
        v1.f = v3;
        return v1;
    }

    static void fun4() {
        BasicTest v1 = new BasicTest();
        BasicTest v2 = new BasicTest();
        BasicTest v3 = new BasicTest();
        v1.f = v2;
        v1.f = v3;
        int value = 0;
        if (value == 100) {
            v1 = v2;
        } else {
            v1 = v3;
        }
        BasicTest v4 = new BasicTest();
        v1.f = v1;
    }

    static void fun5(int value) {
        BasicTest v1 = new BasicTest();
        BasicTest v2 = new BasicTest();
        BasicTest v3 = new BasicTest();
        v1 = v3;
        if (value <= 100) {
            v1.f = v2;
        } else {
            v1.f = v3;
        }
        BasicTest v4 = new BasicTest();
        v4 = v1.f;
    }

    static void fun3_public(int value) {
        BasicTest v1 = new BasicTest();
        BasicTest v2 = new BasicTest();
        v2.f = v2;
        if (value <= 100) {
            v2.f = v1;
        } else
            v2.f = null;
        // Situation 4 partially -- union for an object field
    }

    static void fun4_public(int value) {
        BasicTest v1 = new BasicTest();
        BasicTest v2 = new BasicTest();
        BasicTest v3 = new BasicTest();
        v2.f = v3;
        while (value < 100) {
            v2.f = new BasicTest();
            value += 1; // Situation 5a partially (object field only)
        }
        v3.f = v2.f;
    }

    static void fun5_public(int value) {
        BasicTest v1;
        BasicTest v2 = new BasicTest();
        BasicTest v3 = new BasicTest();
        BasicTest v4 = new BasicTest();
        v2.f = null;
        v3.f = v4;
        if (value == 100) {
            v1 = v2; // Situation 3 (strong update)
        } else if (value == 200) {
            v1 = v3; // Situation 3 (strong update)
        } else {
            v1 = null;
        } // Situation 4 partially -- union for a variable
        BasicTest v5 = v1.f; // Situation 6, Situation 8
        BasicTest v6 = new BasicTest();
        v1.f = v6; // Situation 1, Situation 5b, Situation 7
        v5 = v1.f; // Situation 6
    }

    static void fun6_public(int value, int n) {
        BasicTest v1, v2 = null;
        int old = value;
        do {
            v1 = new BasicTest();
            if (value % 2 == 0) {
                v2 = v1;
            }
            value++;
        } while (value - old < 2 || value < n);
        v2.f = new BasicTest();
        if (v1 == null)
            v2.f = new BasicTest(); // unreachable
        if (v1 != v2)
            v2.f = new BasicTest(); // reachable
    }

    static void fun7_public(int value, int n) {
        BasicTest v1, v2 = null;
        int old = value;
        if (null != null) {
            v1 = new BasicTest();
        }
        do {
            v1 = new BasicTest();
            if (value % 2 == 0) {
                v2 = v1;
            }
            value++;
        } while (value - old < 2 || value < n);
        v2.f = new BasicTest();
        if (v1 == null)
            v2.f = new BasicTest(); // unreachable
        if (v1 != v2)
            v2.f = new BasicTest(); // reachable
    }

    public static void main(String args[]) {

    }
}