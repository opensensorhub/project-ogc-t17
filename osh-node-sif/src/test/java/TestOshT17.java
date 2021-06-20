import org.sensorhub.impl.SensorHub;


public class TestOshT17
{
    private TestOshT17()
    {
    }


    public static void main(String[] args) throws Exception
    {
        SensorHub.main(new String[] {"config/config_misb_vmti_process.json", "storage"});
    }

}
