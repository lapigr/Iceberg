package xyz.lapig.iceberg;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.Spanned;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import xyz.lapig.iceberg.handlers.LastFMContainer;

public class MainActivity extends AppCompatActivity {

    private CoordinatorLayout homeLayout;
    private int activeTab;
    private LastFMContainer recent;
    private LastFMContainer albums;
    private LastFMContainer artists;
    private String user="lapigr";
    private TextView homeView;

	private ExecutorService updateExecuter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        homeLayout = (CoordinatorLayout) findViewById(R.id.home_view);
        homeView = (TextView)findViewById(R.id.textView);
        activeTab=-1;
        recent=new LastFMContainer(getString(R.string.recent),user,getString(R.string.api_key));
        albums=new LastFMContainer(getString(R.string.albums),user,getString(R.string.api_key));
        artists=new LastFMContainer(getString(R.string.artists),user,getString(R.string.api_key));

        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        user = sharedPref.getString(getString(R.string.user), "lapigr");

        updateExecuter = Executors.newCachedThreadPool();

        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                snackAttack("Clearing stored data");
                recent.clear();
                //recent.updateBackground();
                albums.updateBackground();
                artists.updateBackground();
                homeView.setText(Html.fromHtml("<b>"+"Title"+"</b>" +  "<br />" + 
					"<small>" + "description" + "</small>" + "<br />" + 
					"<small>" + "DateAdded" + "</small>"+"<br /><font color='#ff0000'>COLORED</font>", Html.FROM_HTML_OPTION_USE_CSS_COLORS));
            }}
        );

        final TabLayout tab_holder= (TabLayout) findViewById(R.id.tab_bar);
        tab_holder.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
			  try{
                switch(tab.getPosition()){
                    case 0:
                        homeView.setText("Updating..");
                        viewUpdateAsync(recent);
                        widgetUpdateAsync(recent);
                        activeTab=0;
                        break;
                    case 1:
                        homeView.setText(albums.toFormattedString());
                        activeTab=1;
                        break;
                    case 2:
                        homeView.setText(artists.toFormattedString());
                        activeTab=2;
                        break;
                    case -1:
                        homeView.setText(recent.toFormattedString());
                        break;
                    default:
                        snackAttack("default");
                        break;
                }
			  }
			  catch(Exception e){
				  e.printStackTrace();
			  }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }
            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                try{
                switch(tab.getPosition()){
                    case 0:
                        Future<Spanned> fRecent = updateExecuter.submit(recent);
                        Spanned responseRecent=fRecent.get();
                        homeView.setText(responseRecent);

                        widgetUpdateAsync(recent);
                        activeTab=0;
                        break;
                    case 1:
                        Future<Spanned> fAlbums = updateExecuter.submit(albums);
                        Spanned responseAlbums=fAlbums.get();
                        homeView.setText(responseAlbums);
                        activeTab=1;
                        break;
                    case 2:
                        Future<Spanned> fArtists = updateExecuter.submit(artists);
                        Spanned responseArtists=fArtists.get();
                        homeView.setText(responseArtists);
                        activeTab=2;
                        break;
                    default:
                        snackAttack("Invalid tab");
                        break;
                }
                }
                catch(Exception e){
                    e.printStackTrace();
                }
            }
        });
        try{
            Future<Spanned> fRecent = updateExecuter.submit(recent);
            Spanned responseRecent=fRecent.get();
            homeView.setText(responseRecent);
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    public boolean widgetUpdateAsync(LastFMContainer target){
        Intent intent = new Intent(IcebergWidget.ACTION_TEXT_CHANGED);
        intent.putExtra("updatedWidgetText", Html.toHtml(target.toFormattedString(), Html.FROM_HTML_OPTION_USE_CSS_COLORS)); //haha epic
        new Thread(new BackgroundTasks(0,getApplicationContext(), intent)).start();
        return true;
    }
	public boolean viewUpdateAsync(LastFMContainer target)
	{
		Handler handler = new Handler();
        final Future<Spanned> fRecent = updateExecuter.submit(target);
        
        handler.post(new Runnable(){
            public void run(){
                try{
                Spanned responseRecent = fRecent.get();
                homeView.setText(responseRecent);
                }
                catch(Exception e)
                {
                    e.printStackTrace();
                }
            }
        });
        return true;
    }
    public void snackAttack(String msg){
        Snackbar.make(homeLayout, msg, Snackbar.LENGTH_SHORT).setAction("Action", null).show();
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
    @Override
    public void onPause() {
        super.onPause();  // Always call the superclass method first
        updateExecuter.shutdown();
    }
    @Override
    public void onResume() {
        super.onResume();
        updateExecuter = Executors.newCachedThreadPool();
		try{
		switch(activeTab){
            case 0:
				Spanned s=recent.toFormattedString();
                homeView.setText(s);
				widgetUpdateAsync(recent);
                break;
            case 1:

                homeView.setText(albums.toFormattedString());
                break;
            case 2:
                homeView.setText(artists.toFormattedString());
                break;
            default:
			    viewUpdateAsync(recent);
                activeTab=0;
				break;
        }
		}
		catch(Exception e){
	        e.printStackTrace();
			snackAttack("Interruption error");
		}
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
            intent.putExtra("user", user);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(getString(R.string.user), user);
        editor.commit();
        updateExecuter.shutdown();
        updateExecuter=null;
    }
}
