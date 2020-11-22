package ng.com.stemcoders.campusspace.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import timber.log.Timber;

public class FileUtil
{
    public static final String RESOURCES_DIRECTORY = "Campus Space Resources";

    public static final int PERMISSION_REQUEST_CODE = 100;

    public static File getDir (Context context, String path)
    {
        File dir = new File(context.getExternalMediaDirs()[0], path);
        if (!dir.exists())
            dir.mkdirs();

        return dir;
    }

    public static File getResourcesDir(Context context)
    { return getDir(context, RESOURCES_DIRECTORY); }

    public static boolean fileExists(String path)
    { return new File(path).exists(); }

    public static boolean resourceFileExists(Context context, String fileName)
    {return fileExists(getResourcesDir(context).getAbsolutePath() + File.separator + fileName); }

    public static File newFile(String path) throws IOException
    {
        File file = new File(path);
        if (!file.exists())
            file.createNewFile();

        return file;
    }

    public static File newResourceFile(Context context, String fileName) throws IOException
    { return newFile(getResourcesDir(context).getAbsolutePath() + File.separator + fileName); }

    public static File newResourceFileMeta(Context context, String fileName) throws IOException
    {
        int extIndex = fileName.indexOf(".");
        String newName = "." + fileName.substring(0, extIndex) + ".meta";
        return newFile(getResourcesDir(context).getAbsolutePath() + File.separator + newName);
    }

    public static Uri getFileUri(Context context, File file)
    {
        return FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".provider",
                file);
    }

    public static Intent buildViewFileIntent(Context context, File file)
    {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setData(getFileUri(context, file));
        return intent;
    }

    public static List<File> getResourcesMetaFiles(Context context)
    {
        File[] metaFiles = getResourcesDir(context).listFiles((file, name) ->
        {
            if (name.endsWith(".meta"))
                return true;
            else
                return false;
        });

        List<File> list = new ArrayList<>();
        for (File file : metaFiles)
            list.add(file);

        Collections.sort(list, (f1, f2) ->
                Long.compare(f2.lastModified(), f1.lastModified()));

        return list;
    }
}


















