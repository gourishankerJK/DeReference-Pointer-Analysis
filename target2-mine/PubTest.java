public class PubTest {
    PubTest f;

    // treat each test* method below as an entry point, and
    // hence analyze it under ‘@’ (i.e,, epsilon) context
    static void test1() {
        // example 1 starts here
        PubTest k1 = new PubTest(); // first object
        PubTest k2 = new PubTest(); // second object
        k1.f = k2;
        PubTest k3 = new PubTest(); // third object
        foo1(k1, k3);
        // first object’s f points to second object, second object’s
        // f will point to third object here,
        // and third object’s f points to fourth object,
        // and fourth object’s f points to null
    }

    static void foo1(PubTest k1, PubTest k3) {
        PubTest k2 = k1.f;
        k2.f = k3;
        PubTest t = new PubTest(); // fourth object
        k3.f = t;
        t.f = null;
    }

    static PubTest test17() {
        PubTest v1 = new PubTest();
        PubTest v2 = new PubTest();
        PubTest v3 = new PubTest();
        v3.f = test17_fun1(v1, v2);
        return v1;
    }

    static PubTest test17_fun1(PubTest v1, PubTest v2) {
        PubTest v4 = new PubTest();
        v4.f = null;
        v4.f = test17_fun2(v1, v2);
        return null;
    }

    static PubTest test17_fun2(PubTest v1, PubTest v2) {

        v1.f = null;
        v1.f = test17();
        return null;
    }

    static void test22() {
        PubTest v1 = new PubTest();
        PubTest v2 = new PubTest();
        v1.f = v1;
        v1.f = test22_fun1(v1, v2);
        PubTest v3 = v1.f;
        v3.f = v2.f = v1.f;
    }

    static PubTest test22_fun1(PubTest v3, PubTest v4) {
        // v3.f=null;
        v4.f = v4;
        v3.f = v4.f;
        return test22_fun2(v3.f, v4.f);

    }

    static PubTest test22_fun2(PubTest v5, PubTest v6) {
        v5.f = v6;

        return test22_fun3(v5, v6);
    }

    static PubTest test22_fun3(PubTest v7, PubTest v8) {
        PubTest v9 = new PubTest();
        v9.f = v9;
        v9.f = null;
        return v9.f;
    }

    static void test24() {
        // multiple returns
        test24_in(new PubTest());
    }

    static void test24_in(PubTest v1) {
        if (null != null) {
            return;
        }

        v1.f = null;
        return;
    }

    static PubTest test18() {
        PubTest v1 = new PubTest();
        test18_fun1(v1);
        PubTest v4 = new PubTest();
        v1.f = v1;
        v4 = v1.f;
        v4.f = v4;
        return null;
    }

    static PubTest test18_fun1(PubTest v1) {
        PubTest v2 = new PubTest();
        v1.f = v2;
        test18_fun2(v1, v2);
        return v1;
    }

    static PubTest test18_fun2(PubTest v1, PubTest v2) {
        PubTest v3 = new PubTest();
        test18_fun3(v1, v2);
        v1.f = v3;
        return v1;
    }

    static PubTest test18_fun3(PubTest v1, PubTest v2) {
        PubTest v3 = new PubTest();
        v3 = null;
        v1.f = v3;
        return v1;
    }

    static void test21() {
        int a = 0;
        PubTest v1 = new PubTest();

        if (v1 == null) {
            PubTest v2 = new PubTest();
            v2.f = null;
        }

        if (a == a)
            v1.f = null;

        if (null == null)
            v1.f = v1;
        else
            v1.f = null;

    }
}