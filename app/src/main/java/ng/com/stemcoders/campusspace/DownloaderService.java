package ng.com.stemcoders.campusspace;

import android.app.IntentService;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.os.SystemClock;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import org.greenrobot.eventbus.EventBus;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

import ng.com.stemcoders.campusspace.net.RetroServiceGenerator;
import ng.com.stemcoders.campusspace.net.models.ResourceModel;
import ng.com.stemcoders.campusspace.net.services.ResourcesService;
import ng.com.stemcoders.campusspace.utils.FileUtil;
import ng.com.stemcoders.campusspace.utils.NotificationUtil;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import timber.log.Timber;

public class DownloaderService extends IntentService
{
    public int DOWNLOAD_FOREGROUND_ID = 122;
    public static final String EXTRA_RESOURCE = "RESOURCE";

    private NotificationCompat.Builder notificationBuilder;
    private NotificationManagerCompat notificationManagerCompat;

    public DownloaderService()
    {
        super("DownloaderService");
        DOWNLOAD_FOREGROUND_ID = (int)(System.currentTimeMillis()/1000);
    }

    @Override
    protected void onHandleIntent(Intent intent)
    {
        try
        {
            ResourceModel resourceModel = (ResourceModel)intent.getExtras().getSerializable(EXTRA_RESOURCE);

            if (resourceModel == null)
            {
                Exception e = new IllegalArgumentException("No ResourceModel passed");
                Timber.e(e);
                EventBus.getDefault().post(new DownloadFailedEvent(e));
                return;
            }

            int resourceId = resourceModel.getId();

            notificationManagerCompat = NotificationManagerCompat.from(getApplicationContext());
            notificationBuilder = NotificationUtil.buildResourceDownloadingNotification(getApplicationContext(),
                    resourceModel.getFile());
            notificationBuilder.setProgress(0, 0, true);
            startForeground(DOWNLOAD_FOREGROUND_ID, notificationBuilder.build());

            ResourcesService resourcesService = RetroServiceGenerator.generateService(ResourcesService.class);
            Call<ResponseBody> call = resourcesService.downloadResource(resourceId);

            Response<ResponseBody> response = call.execute();
            if (response.isSuccessful())
            {
                File resourceFile = saveResourceFile(response.body(), resourceModel);
                notificationManagerCompat.cancel(DOWNLOAD_FOREGROUND_ID);
                SystemClock.sleep(500);
                notificationManagerCompat.notify((int)(System.currentTimeMillis()/1000),
                        NotificationUtil.buildResourceDownloadedNotification(getApplicationContext(),
                                resourceFile).build());
                EventBus.getDefault().post(new DownloadCompletedEvent(resourceFile));
            }
            else
            {
                Timber.e("Download failed; Resource does not exist");
                EventBus.getDefault().post(new DownloadFailedEvent(null));
            }
        } catch (Exception e)
        {
            Timber.e(e, "Download failed");
            EventBus.getDefault().post(new DownloadFailedEvent(e));
        }
        finally
        {
            stopForeground(true);
        }
    }

    private File saveResourceFile(ResponseBody responseBody, ResourceModel resourceModel) throws IOException
    {
        InputStream inputStream = null;
        FileOutputStream fileOutputStream = null;
        BufferedOutputStream bufferedOutputStream = null;

        try
        {
            String fileName = resourceModel.getFile();
            File resourceFile = FileUtil.newResourceFile(getApplicationContext(), fileName);
            File metaFile = FileUtil.newResourceFileMeta(getApplicationContext(), fileName);

            byte[] buffer = new byte[8192];

            long fileSize = responseBody.contentLength();
            long fileSizeDownloaded = 0;

            inputStream = responseBody.byteStream();
            fileOutputStream = new FileOutputStream(resourceFile);
            bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
            long lastUpdateTime = SystemClock.uptimeMillis();

            while (true)
            {
                int read = inputStream.read(buffer);

                if (read < 0)
                {
                    notificationBuilder.setProgress((int)fileSize, (int)fileSize, false);
                    notificationManagerCompat.notify(DOWNLOAD_FOREGROUND_ID, notificationBuilder.build());
                    break;
                }

                fileSizeDownloaded += read;
                bufferedOutputStream.write(buffer, 0, read);

                long now = SystemClock.uptimeMillis();
                if ((now - lastUpdateTime) >= 1000)
                {
                    notificationBuilder.setProgress((int)fileSize, (int)fileSizeDownloaded, false);
                    notificationManagerCompat.notify(DOWNLOAD_FOREGROUND_ID, notificationBuilder.build());
                    lastUpdateTime = now;
                }
            }

            bufferedOutputStream.flush();

            PrintWriter printWriter = new PrintWriter(new FileWriter(metaFile));
            printWriter.println(resourceModel.getTitle());
            printWriter.println(resourceModel.getCourse_code() + "  [" + resourceModel.getCategory() + "]");
            printWriter.println(formatSize(fileSize));
            printWriter.println(resourceFile.getName());
            printWriter.flush();
            printWriter.close();

            MediaScannerConnection.scanFile(getApplicationContext(), new String[]{resourceFile.getAbsolutePath()},
                    null, null);
            return resourceFile;
        } catch (IOException e)
        {
            throw e;
        } finally
        {
            try
            {
                if (inputStream != null)
                    inputStream.close();
                if (fileOutputStream != null)
                    fileOutputStream.getFD().sync();
                if (bufferedOutputStream != null)
                    bufferedOutputStream.close();
            } catch (IOException e){}
        }
    }

    public static String formatSize(long bytes)
    {
        String size = "";
        String suffix = "";
        double reducedSize = 0;

        if (bytes >= 1048576)
        {
            reducedSize = bytes / 1048576.0;
            suffix = "MB";
        }
        else if (bytes >= 1024)
        {
            reducedSize = bytes / 1024.0;
            suffix = "KB";
        }
        else
        {
            reducedSize = bytes;
            suffix = "B";
        }

        size = String.valueOf(reducedSize);

        if (size.length() >= 4)
            size = size.substring(0, 4);

        if (size.endsWith("."))
            size = size.substring(0, size.length()-1);

        size += " " + suffix;

        return size;
    }

    public static class DownloadFailedEvent
    {
        public final Exception exception;

        public DownloadFailedEvent(Exception exception)
        {
            this.exception = exception;
        }
    }

    public static class DownloadCompletedEvent
    {
        public final File resourceFile;

        public DownloadCompletedEvent(File resourceFile)
        {
            this.resourceFile = resourceFile;
        }
    }
}




























