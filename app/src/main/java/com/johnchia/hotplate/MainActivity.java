package com.johnchia.hotplate;

/* So this is one ugly hack of a project, but it could still be extended a *bit* futher.  Options:
    1. configurable timeouts
    2. make it hotter
    3. ensure all tasks are cancelled before shutdown
    4. selectable burntask types (e.g. simple sqrt(rand), matrix mult, prime factoring, etc)
    5. built-in CPU temperature monitoring

    Potential candidates for making hotter
    https://github.com/ssvb/cpuburn-arm -- ASM busyloops & JPEG decoding
  */

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.Random;
import java.util.Stack;

import static android.text.TextUtils.join;

public class MainActivity extends AppCompatActivity {
    private ArrayList<String> resultList = new ArrayList();
    ArrayAdapter resultAdapter;
    private Stack tasks = new Stack();
    private HotThreadPoolExecutor executor = new HotThreadPoolExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        resultAdapter = new ArrayAdapter<String>(this,R.layout.content_main,resultList);
        ListView resultView = (ListView) findViewById(R.id.main_list);
        resultView.setAdapter(resultAdapter);

       FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {  onClickMain(view);            }
        });
    }

    // start a new burner
    public void onClickMain(View view) {
        // Add a new burn task and start executing it
        BurnTask task = new BurnTask();
        task.executeOnExecutor(executor,tasks.size());
        // clear the result list if no running tasks
        if (tasks.size() == 0) resultList.clear();
        tasks.push(task);
        TextView stv = (TextView) findViewById(R.id.statusTextView);
        stv.setText(tasks.size() + " running");
    }

    protected class BurnTask extends AsyncTask <Integer, Double, Void> {
        private int taskId;
        @Override
        protected Void doInBackground(Integer... params) {
            // Endless sqrt() loop
            // Every 100k cycles, or 1s, whichever is longest: update performance and cancel condition
            taskId = params[0];
            resultList.add(taskId,"");
            Random r = new Random();
            int count = 0;
            long startTime = SystemClock.elapsedRealtime();
            long interval = 0;
            double x = Math.sqrt(r.nextDouble());
            while (true) {
                x = Math.sqrt(r.nextDouble());
                count++;
                if (count%100000 > 0) continue;
                interval = SystemClock.elapsedRealtime() - startTime;
                if(interval > 1000) {
                    publishProgress(((double)count) / interval);
                    startTime = SystemClock.elapsedRealtime();
                    count = 0;
                }
                if(isCancelled()) {
                    Log.v("X","Cancelled");
                    break;
                }
            }
            return null;
        }
        @Override
        protected void onCancelled(Void v){
            super.onCancelled(v);
        }
        @Override
        protected void onProgressUpdate(Double... p) {
            // Give our benchmark to the resultList, then notify the adapter
            // but for the first result, calculate an average of all the tasks instead
            super.onProgressUpdate(p);
            resultList.set(taskId,""+p[0]);
            if (taskId==0) {
                double rAvg = 0;
                int rSize = resultList.size();
                for (int i = 0; i < resultList.size(); i++) {
                    String r = resultList.get(i);
                    if (r.isEmpty()) continue;
                    rAvg += Double.parseDouble(r);
                }
                resultList.set(taskId,""+rAvg);
            }
            resultAdapter.notifyDataSetChanged();
        }
    }
    public boolean onClickCancelAll() {
        TextView stv = (TextView) findViewById(R.id.statusTextView);
        BurnTask task;
        while (!tasks.isEmpty()) {
            task = (BurnTask) tasks.pop();
            task.cancel(true);
        }
        stv.setText(tasks.size() + " running");
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.cancel_all) {
             return onClickCancelAll();
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }
}
