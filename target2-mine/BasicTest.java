public class BasicTest {
    BasicTest f, g;

    static public void f() {
        BasicTest x = REC_TEST_2(new BasicTest());
        x.f = null;
        // v1.f = new BasicTest();

    }

    static public void fa(BasicTest v1) {
        // rec_test(new BasicTest());
        // v1.f = new BasicTest();
        fa(v1);
    }

    static public BasicTest rec_test(BasicTest v1) {
        BasicTest n = new BasicTest();
        rec_test_1(v1);
        if (n.f == v1) {
            return n.f;
        } else {
            v1.f = null;
            return n;
        }
    }

    static public BasicTest REC_TEST_2(BasicTest v1) {
        BasicTest n = new BasicTest();
        rec_test_1(v1);
        return n;
    }

    static public BasicTest rec_test_1(BasicTest v1) {
        BasicTest x = new BasicTest();
        rec_test_1(x);
        return x;
    }

    static public BasicTest g(BasicTest v1) {
        v1.f = new BasicTest();
        v1 = new BasicTest();
        return new BasicTest();
    }

    static public BasicTest g(BasicTest v1, int j, BasicTest v2, int i) {
        return new BasicTest();
    }

    static BasicTest SizeOneNULLTest(BasicTest v) {
        BasicTest v1 = new BasicTest();
        BasicTest v2 = new BasicTest();
        BasicTest v3 = new BasicTest();
        v1.f = v2;
        v2.f = v3;
        v1.f = v2.f;
        v1 = v3.f;
        g(v1);
        return v1;
    }

    static void BothNUll() {
        BasicTest v1 = new BasicTest();
        BasicTest v2 = new BasicTest();
        if (v1 == v2) {
            v1 = null;
        } else
            v2 = null;

        v1 = null;
        if (v1 == v2) {
            v2 = new BasicTest();
        } else {
            v1 = new BasicTest();
        }

    }

    static void fun1() {
        BasicTest v1 = new BasicTest();
        BasicTest v2 = new BasicTest();
        v2.f = v1;
        g(v1);
    }

    public static void conditional_check() {
        int i = 0;
        BasicTest v1 = new BasicTest();
        v1.f = null;
        Object t1 = null;
        Object t3 = null;
        if (v1.f == t1) {
            v1.f = null;
        }
    }

    public static void assign_check(BasicTest t) {
        BasicTest v1 = new BasicTest();
        BasicTest v2 = new BasicTest();
        BasicTest v3 = new BasicTest();
        v1.f = v3;
        v1.f = v2;
        v2.f = t;
        v2.f = v1.f;
    }

    BasicTest xAssignWeakNew() {
        BasicTest v1 = new BasicTest();
        BasicTest v2 = new BasicTest();
        BasicTest v3 = this;
        if (v1 == v2) {
            System.out.println(v1);
        }
        v1 = new BasicTest();
        v1 = v3;
        return v1;

    }

    static void nullify(BasicTest x) {
        x.f = null;
        x.g = null;
        nullify(x);
        return;
    }

    static boolean conditional(BasicTest x, BasicTest y) {
        return x.f == y.f;
    }

    static int multipleReturn(BasicTest x) {
        if (conditional(x, null)) {
            x.f = null;
            return 0;
        } else {
            return 3;
        }
    }

    static void fun3(int value) {
        BasicTest v1 = new BasicTest();
        BasicTest v2 = new BasicTest();
        if (conditional(v1, v2)) {
            v2.f = null;
        } else {
            v2.f = v1;
        }
        nullify(v1);
        nullify(v2);
        multipleReturn(v2);
        // Situation 4 partially -- union for an object field
    }

    static void doubleRec2(BasicTest x) {
        doubleRec1(x);
    }

    static void doubleRec1(BasicTest x) {
        doubleRec2(x);
    }

    static void fun4(int value) {
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

    static void fun5(int value) {
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

    static void fun6(int value, int n) {
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

    static void fun7(int value, int n) {
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