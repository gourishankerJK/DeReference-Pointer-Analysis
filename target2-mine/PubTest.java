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
}