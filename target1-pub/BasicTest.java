public class BasicTest
{
    BasicTest f;

    static void fun1()
    {
        BasicTest v1 = new BasicTest();   
        BasicTest v2 = new BasicTest();   
        v2.f = v1;                      
    }
}
