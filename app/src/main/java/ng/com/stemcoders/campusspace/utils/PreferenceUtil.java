package ng.com.stemcoders.campusspace.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import ng.com.stemcoders.campusspace.R;
import ng.com.stemcoders.campusspace.net.models.ModeratorModel;

public class PreferenceUtil
{
    public static boolean isModeratorLogged(Context context)
    {
        SharedPreferences sharedPreferences = context.getSharedPreferences(context.getString(R.string.prefs_name), Context.MODE_PRIVATE);
        return sharedPreferences.getBoolean(context.getString(R.string.pref_is_moderator_logged_key), false);
    }

    public static String moderatorUsername(Context context)
    {
        SharedPreferences sharedPreferences = context.getSharedPreferences(context.getString(R.string.prefs_name), Context.MODE_PRIVATE);
        return sharedPreferences.getString(context.getString(R.string.prefs_moderator_username_key), null);
    }

    public static String moderatorPassword(Context context)
    {
        SharedPreferences sharedPreferences = context.getSharedPreferences(context.getString(R.string.prefs_name), Context.MODE_PRIVATE);
        return sharedPreferences.getString(context.getString(R.string.prefs_moderator_password_key), null);
    }

    public static String moderatorName(Context context)
    {
        SharedPreferences sharedPreferences = context.getSharedPreferences(context.getString(R.string.prefs_name), Context.MODE_PRIVATE);
        return sharedPreferences.getString(context.getString(R.string.prefs_moderator_name_key), null);
    }

    public static String moderatorEmail(Context context)
    {
        SharedPreferences sharedPreferences = context.getSharedPreferences(context.getString(R.string.prefs_name), Context.MODE_PRIVATE);
        return sharedPreferences.getString(context.getString(R.string.prefs_moderator_email_key), null);
    }

    public static String moderatorGender(Context context)
    {
        SharedPreferences sharedPreferences = context.getSharedPreferences(context.getString(R.string.prefs_name), Context.MODE_PRIVATE);
        return sharedPreferences.getString(context.getString(R.string.prefs_moderator_gender_key), null);
    }

    public static String moderatorPhone(Context context)
    {
        SharedPreferences sharedPreferences = context.getSharedPreferences(context.getString(R.string.prefs_name), Context.MODE_PRIVATE);
        return sharedPreferences.getString(context.getString(R.string.prefs_moderator_phone_key),  null);
    }

    public static void setupModeratorData(Context context, ModeratorModel moderatorModel, String password)
    {
        SharedPreferences sharedPreferences = context.getSharedPreferences(context.getString(R.string.prefs_name), Context.MODE_PRIVATE);
        sharedPreferences.edit()
                .putBoolean(context.getString(R.string.pref_is_moderator_logged_key), true)
                .putString(context.getString(R.string.prefs_moderator_username_key), moderatorModel.getUsername())
                .putString(context.getString(R.string.prefs_moderator_password_key), password)
                .putString(context.getString(R.string.prefs_moderator_name_key), moderatorModel.getFull_name())
                .putString(context.getString(R.string.prefs_moderator_email_key), moderatorModel.getEmail())
                .putString(context.getString(R.string.prefs_moderator_gender_key), moderatorModel.getGender())
                .putString(context.getString(R.string.prefs_moderator_phone_key), moderatorModel.getPhone())
                .apply();
    }

    public static void clearModeratorData(Context context)
    {
        SharedPreferences sharedPreferences = context.getSharedPreferences(context.getString(R.string.prefs_name), Context.MODE_PRIVATE);
        sharedPreferences.edit()
                .putBoolean(context.getString(R.string.pref_is_moderator_logged_key), false)
                .putString(context.getString(R.string.prefs_moderator_username_key), "")
                .putString(context.getString(R.string.prefs_moderator_password_key), "password")
                .putString(context.getString(R.string.prefs_moderator_name_key), "")
                .putString(context.getString(R.string.prefs_moderator_email_key), "")
                .putString(context.getString(R.string.prefs_moderator_gender_key), "")
                .putString(context.getString(R.string.prefs_moderator_phone_key), "")
                .apply();
    }

    public static String displayName(Context context)
    {
        SharedPreferences sharedPreferences = context.getSharedPreferences(context.getString(R.string.prefs_name), Context.MODE_PRIVATE);
        return sharedPreferences.getString(context.getString(R.string.prefs_display_name_key),  "anonymous");
    }

    public static int facultyId(Context context)
    {
        SharedPreferences sharedPreferences = context.getSharedPreferences(context.getString(R.string.prefs_name), Context.MODE_PRIVATE);
        return sharedPreferences.getInt(context.getString(R.string.prefs_faculty_id_key),  -1);
    }

    public static int departmentId(Context context)
    {
        SharedPreferences sharedPreferences = context.getSharedPreferences(context.getString(R.string.prefs_name), Context.MODE_PRIVATE);
        return sharedPreferences.getInt(context.getString(R.string.prefs_department_id_key),  -1);
    }

    public static int levelId(Context context)
    {
        SharedPreferences sharedPreferences = context.getSharedPreferences(context.getString(R.string.prefs_name), Context.MODE_PRIVATE);
        return sharedPreferences.getInt(context.getString(R.string.prefs_level_id_key),  -1);
    }

    public static int courseId(Context context)
    {
        SharedPreferences sharedPreferences = context.getSharedPreferences(context.getString(R.string.prefs_name), Context.MODE_PRIVATE);
        return sharedPreferences.getInt(context.getString(R.string.prefs_course_id_key),  -1);
    }

    public static void setUserData(Context context, String displayName, int facultyId, int departmentId, int levelId, int courseId)
    {
        SharedPreferences sharedPreferences = context.getSharedPreferences(context.getString(R.string.prefs_name), Context.MODE_PRIVATE);
        sharedPreferences.edit()
                .putString(context.getString(R.string.prefs_display_name_key), displayName)
                .putInt(context.getString(R.string.prefs_faculty_id_key), facultyId)
                .putInt(context.getString(R.string.prefs_department_id_key), departmentId)
                .putInt(context.getString(R.string.prefs_level_id_key), levelId)
                .putInt(context.getString(R.string.prefs_course_id_key), courseId)
                .apply();
    }
}




























