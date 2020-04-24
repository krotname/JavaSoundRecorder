import java.text.SimpleDateFormat;
import java.util.Date;

public class Main {

    public static final String ACCESS_TOKEN = "kvq1cDlP8TAAAAAAAAAAG0s7DGJfMauiU6ZYCvhS1ls3C4ex-1ZZjaacyA1nXDw_";
    public static final Integer TIMING = 60000;

    public static void main(String[] args) throws InterruptedException {


        SimpleDateFormat formatname = new SimpleDateFormat("yyyyMMdd'_'HHmmss");

        JavaSoundRecorder recorder = new JavaSoundRecorder();
        while(true){
            String filePath  = formatname.format(new Date()) + ".wav";
            //System.out.println(filePath);
            recorder.recordSound(TIMING, filePath);
            Thread.sleep(TIMING+100);

        }




    }
}
