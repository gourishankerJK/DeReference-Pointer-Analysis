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
        // called from a single context. In this context,
        // k1 points to first object, k3 points to
        // third object, and first object’s f points to second object
        PubTest k2 = k1.f;
        k2.f = k3;
        PubTest t = new PubTest(); // fourth object
        k3.f = t;
        t.f = null;
    }

    static PubTest test2() {
        PubTest k1 = new PubTest(); // first object
        PubTest k2 = foo2(k1);
        PubTest k3 = new PubTest(); // second object
        PubTest k4 = foo2(k3);
        PubTest k5 = null;
        // k2 and k4 will point to both objects because the call depth was more than two
        if (k2 == k4) {
            k5 = k2;
        }
        return k5;
    }

    static PubTest foo2(PubTest k5) {
        // two contexts (both of length 1) reach here.
        // In the first context, k5 points to first object.
        // In the second context k5 points to second object.
        PubTest k7 = bar2(k5);
        // under both both contexts, k7 will point to both objects.
        return k7;
    }

    static PubTest bar2(PubTest k5) {
        // two contexts (both of length 2) reach here.
        // In the first context, k5 points to first object.
        // In the second context k5 points to second object.
        PubTest k7 = bar3(k5);
        // under both both contexts, k7 will point to both objects.
        return k7;
    }

    static PubTest bar3(PubTest k6) {
        // due to length bound of two, only one context reaches here.
        // Under this context, k6 can point to first object or second object.
        return k6;
    }

    static PubTest test3() {
        PubTest k8 = new PubTest(); // first object
        PubTest k9 = baz2(k8);
        PubTest k10 = new PubTest(); // second object
        PubTest k11 = baz2(k10);
        PubTest k12 = null;
        if (k9 == k11) {
            k12 = k9;
        }
        return k12;
    }

    static PubTest baz2(PubTest k5) {
        // two contexts (both of length 1) reach here.
        // In the first context, k5 points to first object.
        // In the second context k5 points to second object.
        PubTest k7 = baz3(k5);
        // k7 will point to first object in first context,
        // and will point to second object in second context
        return k7;
    }

    static PubTest baz3(PubTest k6) {
        // two contexts (both of length 2) reach here.
        // In the first context, k6 points to first object.
        // In the second context k6 points to second object.
        return k6;
    }

    static void infRec() {
        PubTest v2 = new PubTest();
        v2.f = new PubTest();

        return;
    }

    static void rec1() {
        PubTest v1 = new PubTest();
        rec2();
        rec3();

    }

    private static PubTest rec2() {

        PubTest v2 = new PubTest();
        rec3();
        return v2;

    }
    static void rec3() {
        PubTest v1 = new PubTest();
        rec2();
        rec1();

    }
     static void rec4() {
        PubTest v1 = new PubTest();
        rec1();;
         infRec();

    }

    static PubTest doubleInfRec1(PubTest p) {
        PubTest x = new PubTest();
        PubTest y = doubleInfRec2(x);
        return y;
    }

    static PubTest doubleInfRec2(PubTest p) {

        PubTest y = doubleInfRec1(p);
        return y;
    }

    static void condTest() {
        PubTest n1 = fn();
        PubTest n2 = fn();
        if (n1 == n2) {
            n1 = new PubTest();
        } else {
            n2 = new PubTest();
        }
    }
    static PubTest fn() {
        return null;
    }
}