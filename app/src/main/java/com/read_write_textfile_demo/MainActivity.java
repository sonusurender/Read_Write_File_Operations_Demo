package com.read_write_textfile_demo;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static EditText enterMessage;
    private static Button saveMessage, readMessage, deleteMessage;
    private static TextView savedMessageFileNameandPath, savedMessageTime, saveMessageData;
    private static RelativeLayout readMessageLayout;

    private static File fileDirectory = null;//Main Directory File

    private static final String DirectoryName = "Androhub Saved Files";//Main Directory Name
    private static final String FileName = "androhub.txt";//Text File Name
    private static final int READ_BLOCK_SIZE = 512;//Block Size for text File

    //Preferences to Store TimStamp when data is Saved
    private static SharedPreferences timeSharedPreferences;
    private static SharedPreferences.Editor editor;

    //Preferences Name and KeyValue for TimeStamp
    private static final String SharedPreferencesName = "TimePreferences";
    private static final String TimeKeyValue = "TimeKeyValue";

    private static final int PERMISSION_REQUEST_CODE = 122;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Check Permission for Marshmallow Devices
        checkPermission();

        initViews();
        setListeners();
    }

    //Init All views
    private void initViews() {

        //Init shared preferences
        timeSharedPreferences = getSharedPreferences(SharedPreferencesName, Context.MODE_PRIVATE);
        editor = timeSharedPreferences.edit();

        enterMessage = (EditText) findViewById(R.id.enterMessage);

        saveMessage = (Button) findViewById(R.id.saveMessage);
        readMessage = (Button) findViewById(R.id.readMessage);
        deleteMessage = (Button) findViewById(R.id.deleteMessage);

        savedMessageFileNameandPath = (TextView) findViewById(R.id.savedTextFileNameandPath);
        savedMessageTime = (TextView) findViewById(R.id.savedTextFileTime);
        saveMessageData = (TextView) findViewById(R.id.savedFileMessageBody);

        readMessageLayout = (RelativeLayout) findViewById(R.id.readMessageLayout);


        //Check if sd card is present or not
        if (new CheckForSDCard().isSDCardPresent()) {
            fileDirectory = new File(
                    Environment.getExternalStorageDirectory() + "/"
                            + DirectoryName);
        } else
            Toast.makeText(MainActivity.this, "SD Card is not present.", Toast.LENGTH_SHORT).show();//Show toast if SD Card is not mounted
    }


    //set listeners to buttons
    private void setListeners() {
        saveMessage.setOnClickListener(this);
        readMessage.setOnClickListener(this);
        deleteMessage.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.saveMessage:
                saveMessage();
                break;
            case R.id.readMessage:
                showSavedFileData(readMessageFromSDCard());
                break;
            case R.id.deleteMessage:
                deleteMessageFromSDCard();
                break;
        }
    }


    //Call Save Message Method
    private void saveMessage() {
        String messageBody = enterMessage.getText().toString().trim();//get Input message

        //Check if it is null or not
        if (messageBody.length() == 0 && messageBody.equals(""))
            Toast.makeText(MainActivity.this, "Please Enter some message.", Toast.LENGTH_SHORT).show();
        else {

            try {
                //Create Main Directory if not present
                if (!fileDirectory.exists())
                    fileDirectory.mkdir();

                //Make File Name under main Directory
                File savedFile = new File(fileDirectory.getAbsolutePath() + "/" + FileName);

                //If Saved File exists then check if it's size is greater than 0
                if (savedFile.exists()) {
                    if (savedFile.length() > 0)
                        appendOrOverrideSavedFile(savedFile, messageBody);//if file present then show alert for appending and overriding
                    else {
                        //else save data into file
                        saveMessageIntoSDCard(savedFile, messageBody);
                        Toast.makeText(getBaseContext(), "File saved successfully!",
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    //If Saved file doesn't exists then create new file and save file
                    saveMessageIntoSDCard(savedFile, messageBody);
                    Toast.makeText(getBaseContext(), "File saved successfully!",
                            Toast.LENGTH_SHORT).show();
                }

            } catch (Exception e) {
                e.printStackTrace();
                Log.e("Message", e.getLocalizedMessage());

            }
        }
    }


    //Method to show alert if file already present
    private void appendOrOverrideSavedFile(final File savedFile, final String message) {
        final String savedMessage = readMessageFromSDCard();//Read saved message

        //Check if saved message not null
        if (savedMessage != null && savedMessage.length() > 0 && !savedMessage.equals("")) {

            //If not null then show alert
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("Choose..");
            builder.setMessage("What do you want to do with message?\n\nAppend data to current saved file or override saved data.");
            builder.setPositiveButton("OVERRIDE", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    //If Override is selected then textfile data is overrided
                    saveMessageIntoSDCard(savedFile, message);
                    Toast.makeText(getBaseContext(), "File Overrided Successfully!",
                            Toast.LENGTH_SHORT).show();
                }
            });
            builder.setNegativeButton("APPEND", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    //If Append is selected then textfile data is appended with new data
                    saveMessageIntoSDCard(savedFile, savedMessage + "\n\n" + message);
                    Toast.makeText(getBaseContext(), "File Appended Successfully!",
                            Toast.LENGTH_SHORT).show();
                }
            });
            builder.setNeutralButton("CANCEL", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {

                }
            });
            AlertDialog ad = builder.create();
            ad.setCancelable(true);
            ad.setCanceledOnTouchOutside(true);
            ad.show();
        } else
            //If there is nothing to read from saved fie then simply add data to file
            saveMessageIntoSDCard(savedFile, message);

    }


    //Method for writing data to text file
    private void saveMessageIntoSDCard(File savedFile, String messageBody) {
        try {

            //File writer is used for writing data
            FileWriter fWriter = new FileWriter(savedFile);
            fWriter.write(messageBody);//write data
            fWriter.flush();//flush writer
            fWriter.close();//close writer


            //Get Current Date and put it in Shared Preferences
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");
            String currentDateandTime = sdf.format(new Date());

            editor.putString(TimeKeyValue, currentDateandTime);
            editor.commit();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    //Method that will return saved text file data after reading
    private String readMessageFromSDCard() {

        //First check if main directory is present or not
        if (!fileDirectory.exists())
            Toast.makeText(getBaseContext(), "There is no main directory present. ",
                    Toast.LENGTH_SHORT).show();
        else {

            //Then check if text file is present or not
            File savedFile = new File(fileDirectory.getAbsolutePath() + "/" + FileName);
            if (!savedFile.exists())
                Toast.makeText(getBaseContext(), "There is no such file to read data. ",
                        Toast.LENGTH_SHORT).show();
            else {
                //Finally read data using FileReader
                try {
                    FileReader rdr = new FileReader(fileDirectory.getAbsolutePath() + "/" + FileName);

                    char[] inputBuffer = new char[READ_BLOCK_SIZE];//get Block size as buffer
                    String savedData = "";
                    int charRead = rdr.read(inputBuffer);
                    //Read all data one by one by using loop and add it to string created above
                    for (int k = 0; k < charRead; k++) {
                        savedData += inputBuffer[k];
                    }
                    return savedData;//return saved data

                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e("Message", e.getLocalizedMessage());
                    return null;
                }
            }
        }
        return null;
    }

    //Show Saved data
    private void showSavedFileData(String savedMessage) {

        //Check if saved data is not null
        if (savedMessage != null && savedMessage.length() > 0 && !savedMessage.equals("")) {

            //Check if TimeStamp is present or not
            if (timeSharedPreferences.contains(TimeKeyValue))
                savedMessageTime.setText(timeSharedPreferences.getString(TimeKeyValue, ""));//Dispaly saved timestamp

            savedMessageFileNameandPath.setText(FileName + " - inside '" + fileDirectory.getAbsolutePath() + "'");//Display file path
            saveMessageData.setText(savedMessage);//display saved data
            readMessageLayout.setVisibility(View.VISIBLE);//Make visible the read message layout
        }
    }

    //Delete text file method
    private void deleteMessageFromSDCard() {

        //Check if main directory is present or not
        if (!fileDirectory.exists())
            Toast.makeText(getBaseContext(), "There is no main directory present.",
                    Toast.LENGTH_SHORT).show();
        else {

            //Now Check if text file is present or not
            File savedFile = new File(fileDirectory.getAbsolutePath() + "/" + FileName);
            if (!savedFile.exists())
                Toast.makeText(getBaseContext(), "There is no saved file present.",
                        Toast.LENGTH_SHORT).show();
            else {
                savedFile.delete();//If text file is present then delete file
                readMessageLayout.setVisibility(View.GONE);//hide read file layout

                //Remove Saved TimeStamp
                editor.remove(TimeKeyValue);
                editor.commit();

                Toast.makeText(getBaseContext(), "File deleted Successfully.",
                        Toast.LENGTH_SHORT).show();
            }

        }
    }


    /**
     * Permission Code for MARSHMALLOW DEVICES
     **/
    private boolean checkPermission() {
        ArrayList<String> permissions = new ArrayList<>();
        for (String permission : getAllPermissions()) {
            int result = checkPermission(this, permission);
            if (result != PackageManager.PERMISSION_GRANTED) {
                permissions.add(permission);
            }
        }
        if (permissions.size() != 0)
            requestPermission(permissions.toArray(new String[permissions.size()]));
        return true;
    }

    public static int checkPermission(final Context context, String permission) {
        return ContextCompat.checkSelfPermission(context, permission);
    }

    private void requestPermission(String[] permissions) {
        ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
    }

    private String[] getAllPermissions() {
        return new String[]{WRITE_EXTERNAL_STORAGE};
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0) {
                    int counter = 0;
                    for (int result : grantResults) {
                        if (result != PackageManager.PERMISSION_GRANTED) {
                            showMessageOKCancel("Both permissions are required for uploading/selecting image. Please try again.",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                checkPermission();
                                            }
                                        }
                                    }, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            finish();
                                        }
                                    });
                            return;
                        } else {
                            counter++;
                            if (counter == permissions.length)
                                Toast.makeText(this, "Great!! Permission is granted now you can proceed.", Toast.LENGTH_SHORT).show();
                        }
                    }


                }


                break;
        }
    }


    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener, DialogInterface.OnClickListener cancelListener) {
        new AlertDialog.Builder(MainActivity.this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", cancelListener)
                .setCancelable(false)
                .create()
                .show();
    }
}
