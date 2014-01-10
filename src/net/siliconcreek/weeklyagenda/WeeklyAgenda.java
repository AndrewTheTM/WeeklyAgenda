package net.siliconcreek.weeklyagenda;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import com.commonsware.cwac.anddown.AndDown;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.services.drive.Drive;
import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


public class WeeklyAgenda extends FragmentActivity implements
		ActionBar.TabListener {

	/**
	 * The {@link android.support.v4.view.PagerAdapter} that will provide
	 * fragments for each of the sections. We use a
	 * {@link android.support.v4.app.FragmentPagerAdapter} derivative, which
	 * will keep every loaded fragment in memory. If this becomes too memory
	 * intensive, it may be best to switch to a
	 * {@link android.support.v4.app.FragmentStatePagerAdapter}.
	 */
	SectionsPagerAdapter mSectionsPagerAdapter;

	/**
	 * The {@link ViewPager} that will host the section contents.
	 */
	ViewPager mViewPager;
	
	private static final int MAX_PAGES=3;
	static final int REQUEST_ACCOUNT_PICKER=1;
	static final int REQUEST_AUTHORIZATION=2;
	static final int CAPTURE_IMAGE=3;
	
	private static Uri fileUri;
	private static Drive service;
	private GoogleAccountCredential credential;
	private String mFileId;
	private static final String ACTION_DRIVE_OPEN = "com.google.android.apps.drive.DRIVE_OPEN";
	private String APP_NAME="net.siliconcreek.WeeklyAgenda";
	List<String> filenames;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_weekly_agenda);

		// Set up the action bar.
		final ActionBar actionBar = getActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		filenames=new ArrayList<String>();
		
		/*
		 * Turned off for now
		 *
		//Google Drive authentication	
		credential = GoogleAccountCredential.usingOAuth2(getApplicationContext(), Arrays.asList(DriveScopes.DRIVE));
		startActivityForResult(credential.newChooseAccountIntent(),REQUEST_ACCOUNT_PICKER);
		//So far, so good...
		 * 
		 */
		
		
		// Get the current week (assume start on Sunday)
		Date today =new Date();
		Date lastSunday=null;
		SimpleDateFormat dayToday=new SimpleDateFormat("EEEE",Locale.US);
		//The below is klunky, but it works.
		if(dayToday.format(today).equalsIgnoreCase("Sunday")){
			lastSunday=today;
		}else{
			for(int d=-1;d>-7;d--){
				Calendar c = Calendar.getInstance(Locale.US);
				c.setTime(today);
				c.add(Calendar.DATE, d);
				if(dayToday.format(c.getTime()).equalsIgnoreCase("Sunday")){
					lastSunday=c.getTime();
					break;
				}	
			}
		}
		SimpleDateFormat fileDateFormat=new SimpleDateFormat("yyyy-MM-dd");
		filenames.add("wk_"+fileDateFormat.format(lastSunday)+".md");
		
		Calendar c=Calendar.getInstance(Locale.US);
		c.setTime(lastSunday);
		c.add(Calendar.DATE, 7);
		filenames.add("wk_"+fileDateFormat.format(c.getTime())+".md");
		c.add(Calendar.DATE, 14);
		filenames.add("wk_"+fileDateFormat.format(c.getTime())+".md");
		
		//FIXME: delete the file if it is older than the one on the server and ensure I have the newest file
		String urlBase="HIDDEN";
		for(String fn:filenames){
			File file=new File(Environment.getExternalStorageDirectory() + "/" + fn);
			if(!file.exists())
				//file.delete();
			new DataDownloader().execute(urlBase+fn);
		}
		
		

		// Create the adapter that will return a fragment for each of the three
		// primary sections of the app.
		mSectionsPagerAdapter = new SectionsPagerAdapter(
				getSupportFragmentManager());

		// Set up the ViewPager with the sections adapter.
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mSectionsPagerAdapter);

		// When swiping between different sections, select the corresponding
		// tab. We can also use ActionBar.Tab#select() to do this if we have
		// a reference to the Tab.
		mViewPager
				.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
					@Override
					public void onPageSelected(int position) {
						actionBar.setSelectedNavigationItem(position);
					}
				});

		// For each of the sections in the app, add a tab to the action bar.
		for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
			// Create a tab with text corresponding to the page title defined by
			// the adapter. Also specify this Activity object, which implements
			// the TabListener interface, as the callback (listener) for when
			// this tab is selected.
			actionBar.addTab(actionBar.newTab()
					.setText(mSectionsPagerAdapter.getPageTitle(i))
					.setTabListener(this));
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.weekly_agenda, menu);
		return true;
	}

	@Override
	public void onTabSelected(ActionBar.Tab tab,
			FragmentTransaction fragmentTransaction) {
		// When the given tab is selected, switch to the corresponding page in
		// the ViewPager.
		mViewPager.setCurrentItem(tab.getPosition());
	}

	@Override
	public void onTabUnselected(ActionBar.Tab tab,
			FragmentTransaction fragmentTransaction) {
	}

	@Override
	public void onTabReselected(ActionBar.Tab tab,
			FragmentTransaction fragmentTransaction) {
	}

	/**
	 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
	 * one of the sections/tabs/pages.
	 */
	public class SectionsPagerAdapter extends FragmentPagerAdapter {

		public SectionsPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			// getItem is called to instantiate the fragment for the given page.
			// Return a DummySectionFragment (defined as a static inner class
			// below) with the page number as its lone argument.
			Fragment fragment = new DummySectionFragment();
			Bundle args = new Bundle();
			
			
			
				
			
			args.putString(DummySectionFragment.ARG_MAIN_TEXT,getFileContents(position) );
			
			fragment.setArguments(args);
			return fragment;
		}

		public String getFileContents(int position){
			String mainText="";
			File file=new File(Environment.getExternalStorageDirectory() + "/" + filenames.get(position));
			if(file.exists()){
				BufferedReader br = null;
				try {
					br = new BufferedReader(new FileReader(file));
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
				String line;
				try {
					while((line=br.readLine())!=null){
						mainText+=line+"\n";
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			return mainText;
			
		}
		
		@Override
		public int getCount() {
			return MAX_PAGES;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			
			Locale l = Locale.getDefault();
			
			switch (position) {
			case 0:
				return getString(R.string.title_section1).toUpperCase(l);
			case 1:
				return getString(R.string.title_section2).toUpperCase(l);
			case 2:
				return getString(R.string.title_section3).toUpperCase(l);	
			}
			return null;
		}		

	}

	/**
	 * A dummy fragment representing a section of the app, but that simply
	 * displays dummy text.
	 */
	public static class DummySectionFragment extends Fragment {
		/**
		 * The fragment argument representing the section number for this
		 * fragment.
		 */
		public static final String ARG_SECTION_NUMBER = "section_number";
		public static final String ARG_MAIN_TEXT = "main_text";

		public DummySectionFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_weekly_agenda_dummy, container, false);
			TextView mainText=(TextView) rootView.findViewById(R.id.textView1);
			
			/*
			 * Bypass doesn't work
			 */
			//Bypass bypass=new Bypass();
			//String markdownString=getArguments().getString(ARG_MAIN_TEXT);
			//CharSequence string=bypass.markdownToSpannable(markdownString);
			
			/*
			 * Workaround
			 */
			String string = getArguments().getString(ARG_MAIN_TEXT);
			
			AndDown s=new AndDown();
			String ss=s.markdownToHtml(string);
			
			
			//\\//\\
			//mainText.setText(string);
			//mainText.setMovementMethod(LinkMovementMethod.getInstance());
			
			mainText.setText(Html.fromHtml(ss));
			
			
			return rootView;
		}
	}
	

}
