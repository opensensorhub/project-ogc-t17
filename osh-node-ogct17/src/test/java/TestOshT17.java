import org.sensorhub.impl.SensorHub;


public class TestOshT17
{
    private TestOshT17()
    {
    }


    public static void main(String[] args) throws Exception
    {
        SensorHub.main(new String[] {"src/test/resources/config.json", "storage"});
    }

}
