package net.programmierecke.radiodroid;

import android.app.Activity;

public class RadioDroidStationDetail extends Activity {
	// Intent itsIntent;
	// IPlayerService itsPlayerService = null;
	//
	// @Override
	// protected void onCreate(Bundle savedInstanceState) {
	// super.onCreate(savedInstanceState);
	// setContentView(R.layout.station_detail);
	// // setTitle(R.string.startseiteanzeigen_titel);
	//
	// // connect to service
	// Intent anIntentPlayerService = new Intent(RadioDroidStationDetail.this,
	// PlayerService.class);
	// startService(anIntentPlayerService);
	// bindService(anIntentPlayerService, itsServiceConnection,
	// Context.BIND_AUTO_CREATE);
	//
	// // find controls
	// TextView aTextViewName = (TextView) findViewById(R.id.itsTextViewName);
	// TextView aTextViewCountry = (TextView)
	// findViewById(R.id.itsTextViewCountry);
	// TextView aTextViewTags = (TextView) findViewById(R.id.itsTextViewTags);
	// TextView aTextViewLanguage = (TextView)
	// findViewById(R.id.itsTextViewLanguage);
	// Button aButtonHomepage = (Button) findViewById(R.id.itsButtonHomepage);
	// Button aButtonPlay = (Button) findViewById(R.id.itsButtonPlay);
	//
	// // get base calling intent
	// itsIntent = getIntent();
	//
	// // set data of form items
	// aTextViewName.setText(itsIntent.getStringExtra("name"));
	// aTextViewCountry.setText(itsIntent.getStringExtra("country"));
	// aTextViewTags.setText(itsIntent.getStringExtra("tags"));
	// aTextViewLanguage.setText(itsIntent.getStringExtra("language"));
	// aButtonHomepage.setText(itsIntent.getStringExtra("homepage"));
	// aButtonHomepage.setOnClickListener(new View.OnClickListener() {
	// public void onClick(View v) {
	// Intent anIntent = new Intent(Intent.ACTION_VIEW, Uri
	// .parse(itsIntent.getStringExtra("homepage")));
	// startActivity(anIntent);
	// }
	// });
	// aButtonPlay.setOnClickListener(new View.OnClickListener() {
	//
	// public void onClick(View v) {
	// PlayUrl(itsIntent.getStringExtra("url"),
	// itsIntent.getStringExtra("name"),itsIntent.getStringExtra("id"));
	// }
	// });
	// }
	//
	// private ServiceConnection itsServiceConnection = new ServiceConnection()
	// {
	// public void onServiceConnected(ComponentName className, IBinder binder) {
	// // (1)
	// itsPlayerService = IPlayerService.Stub.asInterface(binder);
	// }
	//
	// public void onServiceDisconnected(ComponentName name) {
	// itsPlayerService = null;
	// }
	// };
	//
	// void PlayUrl(String theUrl, String theName,String theID) {
	// if (itsPlayerService != null) {
	// try {
	// itsPlayerService.Play(theUrl,theName,theID);
	// } catch (RemoteException e) {
	// Log.e("remove", "" + e);
	// }
	// } else {
	// Toast.makeText(getApplicationContext(),
	// "player service not running", Toast.LENGTH_SHORT);
	// }
	// }
}
