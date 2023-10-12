public class BasicTest
{
    BasicTest f;

    static void fun1()
    {
        BasicTest v1 = new BasicTest();   
        BasicTest v2 = new BasicTest();   
        v2.f = v1;                      
    }

    static void fun2(int value)
    {
        BasicTest v1 = new BasicTest();   
        BasicTest v2 = new BasicTest();   
        v2.f = v2;                      
        if(value == 100)                    
        {
            v2.f = v1;                  
        }
    }

    static BasicTest fun3()
    {
        BasicTest v1 = new BasicTest();   
        BasicTest v2 = new BasicTest();   
        BasicTest v3 = new BasicTest();   
        int value = iiscPavUtil.random();
        v2.f = v3;   
        while(value < 100)                  
        {
            v2.f = new BasicTest();                   
            value += 1;                     
        }
        v3.f = v2.f;                    
        v1.f = v3;    
        return v1;                  
    }

    static void fun4()
    {
        BasicTest v1 = new BasicTest();   
        BasicTest v2 = new BasicTest();   
        BasicTest v3 = new BasicTest();   
        v1.f = v2;                      
        v1.f = v3;                      
        int value = 0;                      
        if(value == 100)                    
        {
            v1 = v2;                    
        }
        else
        {
            v1 = v3;                    
        }
        BasicTest v4 = new BasicTest();   
        v1.f = v1;                      
    }

    static void fun5(int value)
    {
        BasicTest v1 = new BasicTest();
        BasicTest v2 = new BasicTest();
        BasicTest v3 = new BasicTest();
        v1 = v3;
        if(value<=100)
        {
            v1.f = v2;
        }
        else
        {
            v1.f = v3;
        }
        BasicTest v4 = new BasicTest();
        v4 = v1.f;
    }

    static void fun6(int i)
    {
        Object t1 = null;
        Object t2 = new Object();
        Object t3 = null;
        if (i>10) {
        t3 = t1;
        } else {
        t3 = t2;
        }
        t3.toString();
    }

    public static void main(String args[])
    {
        
    }
}
