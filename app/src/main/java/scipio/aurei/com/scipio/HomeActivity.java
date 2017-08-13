package scipio.aurei.com.scipio;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.PointF;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import services.CouchBaseService;

public class HomeActivity extends AppCompatActivity {

    CouchBaseService couchBaseService;
    boolean mBound = false;
    ImageView shield;
    FrameLayout frameLayout;
    RelativeLayout relativeLayout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
//        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });


        relativeLayout = (RelativeLayout) findViewById(R.id.relativeLayout);


        frameLayout = (FrameLayout)findViewById(R.id.frameLayout);
        frameLayout.setOnTouchListener(new OnSwipeTouchListener(getApplicationContext()){
            @Override
            public void onSwipeLeft() {
                super.onSwipeLeft();
                Toast.makeText(getApplicationContext(), "LEFT", Toast.LENGTH_SHORT).show();
            }
        });

        shield = (ImageView)findViewById(R.id.homescreenShield);
        shield.setOnTouchListener(new OnSwipeTouchListener(getApplicationContext()){

            PointF DownPT = new PointF(); // Record Mouse Position When Pressed Down
            PointF StartPT = new PointF(); // Record Start Position of 'img'
            PointF InitialPT = new PointF(shield.getX(), shield.getY());
            static final int MIN_DISTANCE = 30;
            private float downX, downY, upX, upY;
            int[] startLocation = new int[2];
            int[] endLocation = new int[2];

            @Override
            public void onSwipeLeft() {
                super.onSwipeLeft();
                Toast.makeText(getApplicationContext(), "LEFT", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onSwipeRight() {
                super.onSwipeRight();
                Toast.makeText(getApplicationContext(), "RIGHT", Toast.LENGTH_SHORT).show();
            }

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int eid = event.getAction();
                switch (eid) {
                    case MotionEvent.ACTION_MOVE:
                        PointF mv = new PointF(event.getX() - DownPT.x, event.getY() - DownPT.y);
                        shield.setX((int) (StartPT.x + mv.x));
                        shield.setY((int) (StartPT.y + mv.y));
                        StartPT = new PointF(shield.getX(), shield.getY());
                        break;
                    case MotionEvent.ACTION_DOWN:
                        relativeLayout.removeAllViews();
                        DownPT.x = event.getX();
                        DownPT.y = event.getY();
                        StartPT = new PointF(shield.getX(), shield.getY());
                        downX = event.getX();
                        downY = event.getY();
                        shield.getLocationOnScreen(startLocation);
                        break;
                    case MotionEvent.ACTION_UP:

                        upX = event.getX();
                        upY = event.getY();
                        shield.getLocationOnScreen(endLocation);

//                        float deltaX = (downX - upX)*100;
//                        float deltaY = (downY - upY)*100;

                        float deltaX = (startLocation[0] - endLocation[0])*100;
                        float deltaY = (startLocation[1] - endLocation[1])*100;

                        // swipe horizontal?
                        if(Math.abs(deltaX)>=Math.abs(deltaY) /*&& Math.abs(deltaX) > MIN_DISTANCE*/){
                            // left or right
                            if(deltaX < 0) { this.onLeftToRightSwipe();
                                Log.v(String.valueOf(deltaX), String.valueOf(deltaY));
                            }
                            if(deltaX > 0) { this.onRightToLeftSwipe();
                                Log.v(String.valueOf(deltaX), String.valueOf(deltaY));
                            }
                        } else {
                            Log.v("Swipe only " + String.valueOf(Math.abs(deltaX)) + "need " + MIN_DISTANCE," ");
                        }

                        // swipe vertical?
                        if(Math.abs(deltaY)>=Math.abs(deltaX) /*&&Math.abs(deltaY)> MIN_DISTANCE*/){
                            // top or down

                            if(deltaY < 0) { this.onTopToBottomSwipe();
                                Log.v(String.valueOf(deltaX), String.valueOf(deltaY));
                            }
                            if(deltaY > 0) { this.onBottomToTopSwipe();
                                Log.v(String.valueOf(deltaX), String.valueOf(deltaY));
                            }
                        } else { Log.v("Swipe only " + String.valueOf(Math.abs(deltaY)) + "need " + MIN_DISTANCE," "); }

                        shield.setX(0);
                        shield.setY(0);

                        break;
                }



                return true;
            }
        });


    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent bindServiceIntent = new Intent(this, CouchBaseService.class);
        bindService(bindServiceIntent, mConnection, Context.BIND_AUTO_CREATE);
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            CouchBaseService.LocalBinder binder = (CouchBaseService.LocalBinder) service;
            couchBaseService = binder.getService();
            mBound = true;
            Toast.makeText(getApplicationContext(), String.valueOf(mBound), Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

    public class OnSwipeTouchListener implements View.OnTouchListener {

        LayoutInflater locationInflator = LayoutInflater.from(getApplicationContext());

        private final GestureDetector gestureDetector;
//        static final int MIN_DISTANCE = 100;
//        private float downX, downY, upX, upY;

        public OnSwipeTouchListener(Context context) {
            gestureDetector = new GestureDetector(context, new GestureListener());
        }

        public void onSwipeLeft() {
        }

        public void onSwipeRight() {
        }

        public final void onRightToLeftSwipe() {
            Log.v("Right", "Left");

            View locationView = locationInflator.inflate(R.layout.location_home, null);
            relativeLayout.addView(locationView);

        }

        public void onLeftToRightSwipe(){
            Log.v("Left", "Right");

            View locationView = locationInflator.inflate(R.layout.friends_home, null);
            relativeLayout.addView(locationView);

        }

        public void onTopToBottomSwipe(){
            Log.v("Top", "Bottom");

            View locationView = locationInflator.inflate(R.layout.history_home, null);
            relativeLayout.addView(locationView);

        }

        public void onBottomToTopSwipe(){
           Log.v("Bottom", "Top");

            View locationView = locationInflator.inflate(R.layout.newsession_home, null);
            relativeLayout.addView(locationView);

        }

        public boolean onTouch(View v, MotionEvent event) {

            return gestureDetector.onTouchEvent(event);
        }

        private final class GestureListener extends GestureDetector.SimpleOnGestureListener {

            private static final int SWIPE_DISTANCE_THRESHOLD = 100;
            private static final int SWIPE_VELOCITY_THRESHOLD = 100;

            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                float distanceX = e2.getX() - e1.getX();
                float distanceY = e2.getY() - e1.getY();
                if (Math.abs(distanceX) > Math.abs(distanceY) && Math.abs(distanceX) > SWIPE_DISTANCE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    if (distanceX > 0)
                        onSwipeRight();
                    else
                        onSwipeLeft();
                    return true;
                }
                return false;
            }
        }
    }

}
