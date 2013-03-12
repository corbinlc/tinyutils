// Copyright (C) 2013 Corbin Champion
//
// This file is part of tiny utils.
//
// tiny utils is free software; you can redistribute it and/or modify it
// under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 3 of the License, or (at
// your option) any later version.
//
// tiny utils is distributed in the hope that it will be useful, but
// WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with tiny utils. If not, see <http://www.gnu.org/licenses/>.


package champion.tinyutils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.TextView;

public class tinyutilsMain extends Activity {

	private ProgressDialog mPd_ring;
	private boolean mAlreadyStarted;
	private boolean mErrOcc = false;
	private String mPrefix = "";

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main); 
		TextView textView = (TextView)findViewById(R.id.myTextView);
		textView.setText("Tiny Utils is unpacking and then launching.");

		mAlreadyStarted = false;

		ViewTreeObserver viewTreeObserver = textView.getViewTreeObserver();
		if (viewTreeObserver.isAlive()) { 
			viewTreeObserver.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
				@Override
				public void onGlobalLayout() {
					if (mAlreadyStarted == false) {
						mAlreadyStarted = true;
						mPd_ring = ProgressDialog.show(tinyutilsMain.this, "Unpacking Tiny Utils", "This won't take long.",true);
						mPd_ring.setCancelable(false);
						Thread t = new Thread() {
							public void run() {
								try {
									unpackAll();
								} catch (Exception e) {
									Log.e("LongToast", "", e);
								}
							}
						};
						t.start();
					}
				}
			});
		}
	}

	@Override
	public void onResume() {
		super.onResume(); 
	}

	private void unpackAll() {

		if (updateRequired("champion.tinyutils")) {

			exec("chmod 0777 /data/data/champion.tinyutils");
			exec("chmod 0777 /data/data/champion.tinyutils/lib"); 

			//delete old directories
			exec("rm -r /data/data/champion.tinyutils/bin");
			exec("rm -rf /data/data/champion.tinyutils/bin");
			
			File dir = getDir("internalLib", Context.MODE_PRIVATE);
			exec("rm -r " + dir.getAbsolutePath());
			exec("rm -rf " + dir.getAbsolutePath());

		}

		installPackage("champion.tinyutils");

		launchATE();
	}

	private void installPackage(String packageName) {
		String packageTopStr = "/data/data/"+packageName+"/lib/";

		mErrOcc = false;

		if (updateRequired(packageName)) {
			//first create directories needed
			processDirFile(packageTopStr+"lib__install_dir.so");
			//create all files needed, but linking them to actual files in the lib dir
			updatePrefix(packageTopStr+"lib__install_test.so");
			processLinkFile(packageTopStr+"lib__install_file.so");
			//create all links needed
			mPrefix = "";
			processLinkFile(packageTopStr+"lib__install_link.so");

			if (mErrOcc == false) {
				createVersionFile(packageName);
			}
		}

	}

	public boolean deleteDir(File dir) {
		if (dir.isDirectory()) {
			String[] children = dir.list();
			for (int i=0; i<children.length; i++) {
				boolean success = deleteDir(new File(dir, children[i]));
				if (!success) {
					return false;
				}
			}
		}
		// The directory is now empty so delete it
		return dir.delete();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{ 
		super.onConfigurationChanged(newConfig);
	}

	private void processDirFile(String dirFile) {
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(dirFile));
			String line;
			while ((line = br.readLine()) != null) {
				createDir(line);
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}

	private void processLinkFile(String linkFile) {
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(linkFile));
			String line;
			while ((line = br.readLine()) != null) {
				String[] temp = line.split(" ");
				File tmpFile = new File(temp[1]);
				if (tmpFile.exists()) {
					tmpFile.delete();
				}
				exec("ln -s " + mPrefix + line);
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void updatePrefix(String testFile) {
		boolean savedError = mErrOcc;
		mErrOcc = false;
		exec(testFile);
		if (mErrOcc) {
			File dir = getDir("internalLib", Context.MODE_PRIVATE);
			mPrefix = dir.getAbsolutePath() + "/";
			try {
				copyFolder("/data/data/champion.tinyutils/lib/", mPrefix);
			} catch (IOException e) {
				savedError = true;
			}
		} else {
			mPrefix = "/data/data/champion.tinyutils/lib/";
		}
		mErrOcc = savedError;
	}

	private void createDir(File dir) {
		if (!dir.getParentFile().exists()) { 
			createDir(dir.getParentFile()); 
		}
		if (dir.exists()) { 
			return; 
		} 
		Log.v("Unzip", "Creating dir " + dir.getName()); 
		if (!dir.mkdir()) { 
			throw new RuntimeException("Can not create dir " + dir); 
		}
		exec("chmod 0777 " + dir.getAbsolutePath());
	}

	private void createDir(String path) {
		File dir = new File(path);
		createDir(dir);
	}

	public void copyFolder(String srcStr, String destStr) throws IOException {
		File src = new File(srcStr);
		File dest = new File(destStr);
		copyFolder(src, dest);
	}

	public void copyFolder(File src, File dest) throws IOException{

		if(src.isDirectory()){

			//if directory not exists, create it
			if(!dest.exists()){
				dest.mkdir();
				System.out.println("Directory copied from " + src + "  to " + dest);
				exec("chmod 0777 " + dest.getAbsolutePath());
			}

			//list all the directory contents
			String files[] = src.list();

			for (String file : files) {
				//construct the src and dest file structure
				File srcFile = new File(src, file);
				File destFile = new File(dest, file);
				//recursive copy
				copyFolder(srcFile,destFile);
			}

		}else{
			//if file, then copy it
			//Use bytes stream to support all file types
			InputStream in = new FileInputStream(src);
			OutputStream out = new FileOutputStream(dest); 

			byte[] buffer = new byte[1024];

			int length;
			//copy the file content in bytes 
			while ((length = in.read(buffer)) > 0){
				out.write(buffer, 0, length);
			}

			in.close();
			out.close();
			System.out.println("File copied from " + src + " to " + dest);
			exec("chmod 0777 " + dest.getAbsolutePath());
		}
	}

	private void exec(String command) {
		Runtime runtime = Runtime.getRuntime(); 
		Process process;
		try {
			process = runtime.exec(command);
			try {
				String str;
				process.waitFor();
				BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));
				while ((str = stdError.readLine()) != null) {
					Log.e("Exec",str);
					mErrOcc = true;
				}
				process.getInputStream().close(); 
				process.getOutputStream().close(); 
				process.getErrorStream().close(); 
			} catch (InterruptedException e) {
				mErrOcc = true;
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		finish();
	}

	private void launchATE() {
		mPd_ring.dismiss();
		Intent termIntent = new Intent(tinyutilsMain.this, jackpal.androidterm.RemoteInterface.class);
		termIntent.addCategory(Intent.CATEGORY_DEFAULT);
		termIntent.setAction("jackpal.androidterm.OPEN_NEW_WINDOW");
		startActivity(termIntent);
		finish();
	}

	private boolean updateRequired(String packageName) {
		String version;

		try {
			PackageInfo pi = getPackageManager().getPackageInfo(packageName, 0);
			version = pi.versionName;     // this is the line Eclipse complains
		}
		catch (PackageManager.NameNotFoundException e) {
			return false;
		}

		File versionFile = new File("/data/data/champion.tinyutils/"+packageName+"."+version);

		if (versionFile.exists()==false) {
			return true;
		} else {
			return false;
		}
	}

	private void createVersionFile(String packageName) {
		String version;

		try {
			PackageInfo pi = getPackageManager().getPackageInfo(packageName, 0);
			version = pi.versionName;     // this is the line Eclipse complains
		}
		catch (PackageManager.NameNotFoundException e) {
			version = "?";
		}

		File versionFile = new File("/data/data/champion.tinyutils/"+packageName+"."+version);

		if (versionFile.exists()==false) {
			try {
				versionFile.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}