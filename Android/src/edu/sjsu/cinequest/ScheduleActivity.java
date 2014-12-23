package edu.sjsu.cinequest;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import edu.sjsu.cinequest.CinequestActivity.FilmletListAdapter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentUris;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

/**
 * The Schedule Tab of the app 
 * 
 * @author Sravankumar Reddy Javaji
 * @author Dmitri Dimov
 */

class EventData {
	public int eid;
	public String name;
	public String stime;
	public String etime;
	public Long SDateInMillis;
	public EventData(int _eid,String _name, String _stime, String _etime, Long _SDateInMillis) {
		eid = _eid;
		name = _name;
		stime = _stime;
		etime = _etime;
		SDateInMillis = _SDateInMillis;
	}

	public String getName() {
		return name;
	}
	public String getStime() {
		return stime;
	}
	public String getEtime() {
		return etime;
	}
	public int getEId() {
		return eid;
	}
	public Long getSDateInMillis() {
		return SDateInMillis;
	}
	//ordering items Alphabetically
	public static class CompName implements Comparator<EventData> {
		@Override
		public int compare(EventData arg0, EventData arg1) {
			return arg0.getName().compareToIgnoreCase(arg1.getName());       
		}
	}
	//for ordering items chronologically
	public static class CompDate implements Comparator<EventData> {
		@Override
		public int compare(EventData arg0, EventData arg1) {
			if (arg0.getSDateInMillis() > arg1.getSDateInMillis())
			{
				return 1;
			}
			else if (arg0.getSDateInMillis() < arg1.getSDateInMillis())
			{
				return -1;
			}
			else  
				return arg0.getName().compareToIgnoreCase(arg1.getName());			       
		}
	}
}
/**
 * Any film related events are going to be displayed with this class.
 * Allows consumer to view any film related information with ScheduleActivity class.
 */
public class ScheduleActivity extends CinequestActivity {
	ListView listView;	
	SimpleDateFormat sdf;
	private DateUtils du;
	String fStartTime="";
	String fEndTime="";
	boolean is24HourFormat=false;

	private static String calendarName="Cinequest Calendar";
	private static String m_selectedCalendarId = "Cinequest Calendar";
	private static final String DATE_TIME_FORMAT = "MMM dd, yyyy'T'HH:mm";
	private List<EventData> events = new ArrayList<EventData>();
	private TextView nothingToday;	//Shows an empty list when the list is empty

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.schedulelayout);
		sdf = new SimpleDateFormat(DATE_TIME_FORMAT);
		du = new DateUtils();
		nothingToday = (TextView) this.findViewById(R.id.msg_for_empty_schedyle);
		listView = (ListView) findViewById(R.id.schedule_listview);
	}

	@Override
	protected void onResume() {
		super.onResume();  // Always call the superclass method first
		is24HourFormat=android.text.format.DateFormat.is24HourFormat(this);
		events.clear();
		populateSchedule();
	}

	public void populateSchedule()
	{

		String[] proj = new String[]{"_id", "calendar_displayName"};
		String calSelection = 
				"(calendar_displayName= ?) ";
		String[] calSelectionArgs = new String[] {
				calendarName                                        
		}; 

		Uri event=null;
		if (Build.VERSION.SDK_INT >= 8) {
			event = Uri.parse("content://com.android.calendar/calendars");
		} else {
			//Calendar code for API level < 8, needs lot of testing. 
			//May be some of the paramters (that we are populating above), have different naming conventions in different API Levels
			event = Uri.parse("content://calendar/calendars");
		}		

		Cursor l_managedCursor = null;
		try{
			l_managedCursor = getContentResolver().query(event, proj, calSelection, calSelectionArgs, null );
			if (l_managedCursor.moveToFirst()) {                        

				int l_idCol = l_managedCursor.getColumnIndex(proj[0]);
				do {                
					m_selectedCalendarId = l_managedCursor.getString(l_idCol);                
				} while (l_managedCursor.moveToNext());
			}
		}
		catch (Exception e){
			Log.i("ScheduleActivity:populateSchedule","Error while retrieving Cinequest Calendar ID from device Calendar");
		}

		l_managedCursor.close();
		l_managedCursor=null;

		Uri l_eventUri;

		if (Build.VERSION.SDK_INT >= 8) {
			l_eventUri = Uri.parse("content://com.android.calendar/events");
		} else {
			//Calendar code for API level < 8, needs lot of testing. 
			//May be some of the paramters (that we are populating above), have different naming conventions in different API Levels
			l_eventUri = Uri.parse("content://calendar/events");
		}

		String[] l_projection = new String[]{"_id","title", "dtstart", "dtend"};
		try{
			l_managedCursor = this.getContentResolver().query(l_eventUri, l_projection, "calendar_id=" + m_selectedCalendarId, null, "dtstart DESC, dtend DESC");
		}
		catch (Exception e){
			Log.i("ScheduleActivity:populateSchedule","Error while retrieving events from Cinequest Calendar");
		}
		if (l_managedCursor.moveToFirst()) {
			String l_title;
			String l_begin;
			String l_end;
			int e_id;
			long StartDateInMillis;
			int l_colid = l_managedCursor.getColumnIndex(l_projection[0]);
			int l_colTitle = l_managedCursor.getColumnIndex(l_projection[1]);
			int l_colBegin = l_managedCursor.getColumnIndex(l_projection[2]);
			int l_colEnd = l_managedCursor.getColumnIndex(l_projection[3]);
			do {
				e_id = l_managedCursor.getInt(l_colid);
				l_title = l_managedCursor.getString(l_colTitle);
				l_begin = getDateTimeStr(l_managedCursor.getString(l_colBegin));
				l_end = getDateTimeStr(l_managedCursor.getString(l_colEnd));
				StartDateInMillis = Long.parseLong(l_managedCursor.getString(l_colBegin));
				EventData edata= new EventData(e_id, l_title, l_begin, l_end, StartDateInMillis);
				events.add(edata);
			} 
			while (l_managedCursor.moveToNext());
		}

		//Uncomment below method, when you need to sort the listitems in schedule tab, by title.
		//Collections.sort(events, new EventData.CompName());

		Collections.sort(events, new EventData.CompDate());
		//The line below creates a new schedule will all sorted information of everything user added
		//up to this point. Note:SeparatedListIndexedAdapter does not sort information because
		//events are already sorted!
		SeparatedListIndexedAdapter separatedSchedule = new SeparatedListIndexedAdapter(this);
		/*{
			public View getView(final int position, View convertView, ViewGroup parent)
			{
				View v = super.getView(position, convertView, parent);
				if(position > 0)
				{
					
				}*/
				//final EventData q = (EventData) 
				
				/*Button button1 = (Button) v.findViewById(R.id.remove);
				button1.setOnClickListener( new OnClickListener() {
					@Override
					public void onClick(View v) {
						Uri eventUri;                    
						if (Build.VERSION.SDK_INT >= 8) {
							eventUri = Uri.parse("content://com.android.calendar/events");
						} else {
							//Calendar code for API level < 8, needs lot of testing. 
							//May be some of the parameters (that we are populating above), have different naming conventions in different API Levels
							eventUri = Uri.parse("content://calendar/events");
						}                        
						Uri deleteUri = ContentUris.withAppendedId(eventUri, q.getEId());
						try{
							int rows = getContentResolver().delete(deleteUri, null, null);
							if (rows==1){
								events.remove(position);
								listView.invalidateViews();
							}
						}
						catch (Exception e){
							Log.i("ScheduleActivity:populateSchedule","Error while removing Events from Calendar");
						}
					}
				});*/
			/*	return v;
			}
		};*/
		ArrayAdapter<EventData> adapter = null;
		List<EventData> byDay = null;
		int lastDay = 0;
		for(int i = 0; i < events.size(); i++)
		{
			//Have adapter store only events related to given day!
			String day = events.get(i).getStime().split(",")[0].split(" ")[1];
			if(lastDay == 0)
			{
				lastDay = Integer.parseInt(day);
				byDay = new ArrayList<EventData>(); //This generates a new ArrayList for every day
			}
			if(lastDay == Integer.parseInt(day))
			{
				byDay.add(events.get(i));	//Collects all events for that day
			}
			//The code below assigns all events for given day to adapter if next day is different or we
			//reached the end of all events for today
			if((i + 1 < events.size() &&
					lastDay != Integer.parseInt(events.get(i+1).getStime().split(",")[0].split(" ")[1])) &&
					lastDay == Integer.parseInt(day) || i == events.size() - 1)
			{
				adapter = new ArrayAdapter<EventData>(this.getApplicationContext(), R.layout.eventlistview, byDay) {
					@Override
					public View getView(final int position, View v, ViewGroup parent) {

						LayoutInflater inflater = LayoutInflater.from(getContext());
						final EventData q = getItem(position);                                
						if (v == null) v = inflater.inflate(R.layout.eventlistview, null);                                
						TextView textView = (TextView) v.findViewById(R.id.eventName);
						textView.setText(q.getName());

						String sStr[]=q.getStime().split("T");
						String eStr[]=q.getEtime().split("T");
						
						TextView textView1 = (TextView) v.findViewById(R.id.startTime);
						//if start date and end date of event won't match, then we will be displaying both the dates.
						//Else we will display only date once, followed by time.
						if (sStr[0].equalsIgnoreCase(eStr[0]))
						{
							if(!is24HourFormat)
							{
								fStartTime=du.formatTime(sStr[1]);
								fEndTime=du.formatTime(eStr[1]);
							}
							else
							{
								fStartTime=sStr[1];
								fEndTime=eStr[1];
							}
							textView1.setText(sStr[0]+"  Time: "+fStartTime+" - "+fEndTime);
						}
						else
						{
							textView1.setText(sStr[0]+" "+sStr[1]+" - "+eStr[0]+" "+eStr[1]);
						}
						textView1.setTypeface(null, Typeface.ITALIC);
						return v;                                
					}                            
				};
				//add this adapter to a SeparatedListIndexedAdapter
				separatedSchedule.addSection(localizeHumanFormat(events.get(i).getStime()), events.get(i).getStime().split(",")[0], adapter);
				lastDay = 0;
			}
		}
		
		separatedSchedule.setAsAdapterFor(listView);
		registerForContextMenu(listView);
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position,
					long id) {
					System.out.println("HERE!");
			}
		});
		if(separatedSchedule.getCount() == 0){
			listView.setVisibility(View.GONE);
			nothingToday.setVisibility(View.VISIBLE);
		}else{
			listView.setVisibility(View.VISIBLE);
			nothingToday.setVisibility(View.GONE);
		}
		//listView.setItemsCanFocus(false);
		l_managedCursor.close();
		l_managedCursor=null;
	}

	@SuppressLint("SimpleDateFormat")
	private String localizeHumanFormat(String inputDate)
	{
		SimpleDateFormat fmt = new SimpleDateFormat(DATE_TIME_FORMAT);
		Date date = null;
		try {
			date = fmt.parse(inputDate);
		} catch (ParseException e1) {
			e1.printStackTrace();
		}
		String dateInHumForm = (String)android.text.format.DateFormat.format("EEEE, MMMM dd", date);
		String[] temp = dateInHumForm.split(" ");
		int dayOfMonth = Integer.parseInt(temp[2]);
		return temp[0] + " " + temp[1] + " " + dayOfMonth;
	}

	@SuppressWarnings("deprecation")
	public static String getDateTimeStr(int p_delay_min) {
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat(DATE_TIME_FORMAT);
		if (p_delay_min == 0) {
			return sdf.format(cal.getTime());
		} else {
			Date l_time = cal.getTime();
			l_time.setMinutes(l_time.getMinutes() + p_delay_min);
			return sdf.format(l_time);
		}
	}

	public String getDateTimeStr(String p_time_in_millis) {	
		Date l_time = new Date(Long.parseLong(p_time_in_millis));
		return sdf.format(l_time);
	}			
}

