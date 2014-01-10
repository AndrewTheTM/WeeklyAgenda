package net.siliconcreek.weeklyagenda;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

public class DataDownloader extends AsyncTask<String,Void,String> {
	public static final String APP_NAME="net.siliconcreek.WeeklyAgenda";

	@Override
	protected String doInBackground(String... params) {
		String url=params[0];
		String baseFileName=url.substring(url.lastIndexOf("/"), url.length());
		try{
			URL u=new URL(url);
			InputStream is=u.openStream();
			DataInputStream dis =new DataInputStream(is);
			byte[] buffer = new byte[1024];
			int length;
			FileOutputStream fos=new FileOutputStream(new File(Environment.getExternalStorageDirectory() + "/" + baseFileName));
			while((length=dis.read(buffer))>0){
				fos.write(buffer, 0, length);
			}
		}catch(MalformedURLException mue){
			Log.e(APP_NAME,mue.getMessage());
		} catch (IOException e) {
			Log.e(APP_NAME,e.getMessage());
		}
		return null;
	}
	
	@Override
	protected void onPostExecute(String result){
		
	}

}
