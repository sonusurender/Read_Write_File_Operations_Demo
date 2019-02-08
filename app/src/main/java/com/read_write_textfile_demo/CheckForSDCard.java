package com.read_write_textfile_demo;

import android.os.Environment;

public class CheckForSDCard {
    //Check for SD Card if it is mounted or not
    public boolean isSDCardPresent() {
        if (Environment.getExternalStorageState().equals(

                Environment.MEDIA_MOUNTED)) {
            return true;
        }
        return false;
    }
}
