package com.jakev.googlesms;

import java.util.ArrayList;

import com.jakev.googlesms.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

	

public class MainActivity extends Activity implements OnClickListener {
	
	private ArrayList<String> smsList = new ArrayList<String>();
    static int deleting_complete = 0;
	
	static final int PROGRESS_DIALOG = 0;
    ProgressThread progressThread;
    ProgressDialog progressDialog;
    AlertDialog completedDialog;    
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        Button removeButton;
        
        removeButton = (Button) findViewById(R.id.buttonRemove);
        removeButton.setOnClickListener(this);

    }
    
    @Override
    public void onBackPressed() {
    	finish();
    }

	@Override
	public void onClick(View v) {
		doWork();
	}
	
	//METHODS
	private void doWork() {
		
		findSMS();
		displayWarning();
	}
	
	public void findSMS(){
		
		Uri uri = Uri.parse("content://sms/inbox");
		Cursor cursor = getContentResolver().query(uri, null, null ,null,null);
		
		while (cursor.moveToNext()) {
			String smsMessage = cursor.getString(cursor.getColumnIndex("body"));
			String pid = cursor.getString(0);
			String duri = "content://sms/"+pid;
			
			
			if (isGoogleAuthMessage(smsMessage)) {
				smsList.add(duri);				
			}
		}
		cursor.close();
	}
	
	private boolean isGoogleAuthMessage(String msg) {
		
		if (msg.matches("^Your Google verification code is [0-9]{6}$")) {
			return true;
		}
		
		return false;
	}	
	
	private void displayWarning() {
		if (smsList.size() == 0) {
			
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage("I couldn't find any verification SMS messages..")
							
			       .setCancelable(false)
			       .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			           public void onClick(DialogInterface dialog, int id) {
			        	   dialog.cancel();
			           }
			       });
			
			AlertDialog alert = builder.create();	
				
			alert.show();			
			smsList.clear();
		}
		
		else {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage("Are you sure you want to delete " +
							Integer.toString(smsList.size()) +
							" message(s)?")							
			       .setCancelable(false)
			       .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
			           public void onClick(DialogInterface dialog, int id) {
			                deleteMessages();
			           }
			       })
			       .setNegativeButton("No", new DialogInterface.OnClickListener() {
			           public void onClick(DialogInterface dialog, int id) {
			               smsList.clear(); 
			        	   dialog.cancel();
			           }
			       });
			
			AlertDialog alert = builder.create();	
				
			alert.show();
		}
	}

	
	private void deleteMessages() {
		
		//This dialog starts a new threat that shows progress. 
		showDialog(PROGRESS_DIALOG);
	}
	
	private void showCompleted() {
		
   		AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
		builder.setCancelable(false)
				.setMessage("SMS messages successfully deleted!")
				.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		        	   dialog.cancel();
		           }
		       });
		
		completedDialog = builder.create();
    	completedDialog.show();
		
        smsList.clear();
	}
	
	//Dialog Stuff
	protected Dialog onCreateDialog(int id) {
        switch(id) {
	        case PROGRESS_DIALOG:
	            progressDialog = new ProgressDialog(MainActivity.this);
	            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
	            progressDialog.setMessage("Deleting all Google verification messages...");
	            progressDialog.setCancelable(false);
	            return progressDialog;
	        default:
	            return null;
        }
    };
    
    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        switch(id) {
        	case PROGRESS_DIALOG:
	            progressDialog.setProgress(0);
	            progressThread = new ProgressThread(handler);
	            progressThread.start();
        }
    }
    
 // Define the Handler that receives messages from the thread and update the progress
    final Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            int total = msg.arg1;            
            progressDialog.setProgress(total);
            
            if (total >= 100){
                dismissDialog(PROGRESS_DIALOG);
                showCompleted(); 		//Show the "completed" dialog
            }
        }
    };

    /** Nested class that performs progress calculations (counting) */
    private class ProgressThread extends Thread {
        Handler mHandler;
        float total;
        float count;
        int current;
        
        ProgressThread(Handler h) {
            mHandler = h;
        }
       
        public void run() {  
            total = (float) smsList.size();
            count = (float) 1;
            
        	for (String uri : smsList) {
        		
        		current = (int)((count / total) * 100.0);
                
                getContentResolver().delete(Uri.parse(uri),null,null);
                Message msg = mHandler.obtainMessage();
                msg.arg1 =current;
                mHandler.sendMessage(msg);
        		count++;
    		}
        }
    }
}