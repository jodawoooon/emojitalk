package gujc.emotionalTalk.chat;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.affectiva.android.affdex.sdk.Frame;
import com.affectiva.android.affdex.sdk.detector.CameraDetector;
import com.affectiva.android.affdex.sdk.detector.Detector;
import com.affectiva.android.affdex.sdk.detector.Face;

import java.util.HashMap;
import java.util.List;

import gujc.emotionalTalk.R;
import gujc.emotionalTalk.fragment.ChatFragment;
import gujc.emotionalTalk.fragment.UserListInRoomFragment;

public class ChatActivity extends AppCompatActivity  implements Detector.ImageListener, CameraDetector.CameraEventListener {
    private DrawerLayout drawerLayout;
    private ChatFragment chatFragment;
    private UserListInRoomFragment userListInRoomFragment = null;

    final String LOG_TAG = "Emotion Detection";
    private final String TAG = "MyTag";
    public static String result = "Detecting";
    public final int PERMISSIONS_REQUEST_CODE = 100;
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA", "android.permission.WRITE_EXTERNAL_STORAGE"};
    Button startSDKButton;
    TextView smileTextView;
    TextView angerTextView;
    TextView valenceTextView;
    TextView contemptTextView;
    TextView surpriseTextView;
    TextView focusTextView;
    TextView sadnessTextView;
    TextView resultTextView;
    ImageView resultImageView;

    SurfaceView cameraPreview;

    boolean isSDKStarted;

    RelativeLayout mainLayout;

    CameraDetector detector;

    int previewWidth = 0;
    int previewHeight = 0;

    private HashMap _$_findViewCache;

    Button anlayButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);


        this.getWindow().setFlags(1024, 1024);
        this.getWindow().setFlags(128, 128);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);

        anlayButton = (Button) findViewById(R.id.realTimeAnalysButton);

        String toUid = getIntent().getStringExtra("toUid");
        final String roomID = getIntent().getStringExtra("roomID");
        String roomTitle = getIntent().getStringExtra("roomTitle");
        if (roomTitle!=null) {
            actionBar.setTitle(roomTitle);
        }


        // left drawer
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        findViewById(R.id.rightMenuBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (drawerLayout.isDrawerOpen(Gravity.RIGHT)) {
                    drawerLayout.closeDrawer(Gravity.RIGHT);
                } else {
                    if (userListInRoomFragment==null) {
                        userListInRoomFragment = UserListInRoomFragment.getInstance(roomID, chatFragment.getUserList());
                        getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.drawerFragment, userListInRoomFragment)
                                .commit();
                    }
                    drawerLayout.openDrawer(Gravity.RIGHT);
                }
            }
        });


        anlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), AnalyActivity.class);
                intent.putExtra("roomID", roomID);
                startActivity(intent);    //to start the activity specified by the Intent"
            }
        });


        // chatting area
        chatFragment = ChatFragment.getInstance(toUid, roomID);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.mainFragment, chatFragment )
                .commit();

        initialiseUI();

        isSDKStarted = getIntent().getExtras().getBoolean("sdkStarted");

        startSDKButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isSDKStarted) {
                    isSDKStarted = false;
                    stopDetector();

                    startSDKButton.setBackground(ContextCompat.getDrawable(ChatActivity.this, R.drawable.start));


                } else {
                    isSDKStarted = true;
                    startDetector();
                    startSDKButton.setBackground(ContextCompat.getDrawable(ChatActivity.this, R.drawable.stop));
                }
            }
        });


        //We create a custom SurfaceView that resizes itself to match the aspect ratio of the incoming camera frames
        cameraPreview = new SurfaceView(this) {
            @Override
            public void onMeasure(int widthSpec, int heightSpec) {
                int measureWidth = 180;
                int measureHeight = MeasureSpec.getSize(heightSpec);
                int width;
                int height;
                if (previewHeight == 0 || previewWidth == 0) {
                    width = measureWidth;
                    height = measureHeight;
                } else {
                    float viewAspectRatio = (float)measureWidth/measureHeight;
                    float cameraPreviewAspectRatio = (float) previewWidth/previewHeight;

                    if (cameraPreviewAspectRatio > viewAspectRatio) {
                        width = measureWidth;
                        height =(int) (measureWidth / cameraPreviewAspectRatio);
                    } else {
                        width = (int) (measureHeight * cameraPreviewAspectRatio);
                        height = measureHeight;
                    }
                }
                setMeasuredDimension(width, height);
            }
        };

        setLayout();

        detector = new CameraDetector(this, CameraDetector.CameraType.CAMERA_FRONT, cameraPreview);

        detectEmotions();

        Log.d(this.TAG, "onCreate");



    }


    protected void detectEmotions(){

        detector.setDetectSmile(true);
        detector.setDetectAnger(true);
        detector.setDetectDisgust(true);
        detector.setDetectFear(true);
        detector.setDetectSadness(true);
        detector.setDetectAttention(true);
        detector.setDetectContempt(true);
        detector.setDetectSurprise(true);
        detector.setDetectValence(true);
        detector.setDetectAllEmojis(true);
        detector.setDetectJoy(true);
        detector.setImageListener(this);
        detector.setOnCameraEventListener(this);


    }

    protected void setLayout(){
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
        params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
        cameraPreview.setLayoutParams(params);
        mainLayout.addView(cameraPreview,0);
        cameraPreview.setVisibility(View.VISIBLE);
    }

    protected void initialiseUI(){

        //smileTextView = (TextView) findViewById(R.id.smile_textview);
        //contemptTextView = (TextView) findViewById(R.id.contempt_textview);
        //valenceTextView = (TextView) findViewById(R.id.valance_textview);
        //angerTextView = (TextView) findViewById(R.id.anger_textview);
       // surpriseTextView = (TextView) findViewById(R.id.surprise_textview);
        //focusTextView = (TextView) findViewById(R.id.focus_textview);
        //sadnessTextView = (TextView) findViewById(R.id.sadness_textview);
        resultTextView = (TextView)findViewById(R.id.result_emotion_textview);
        resultImageView = (ImageView)findViewById(R.id.result_imageView);
        startSDKButton = (Button) findViewById(R.id.sdk_start_button);
        startSDKButton.setBackground(ContextCompat.getDrawable(ChatActivity.this, R.drawable.stop));

        mainLayout = (RelativeLayout) findViewById(R.id.main_layout);


    }


    @Override
    protected void onResume() {
        super.onResume();
        if (isSDKStarted) {
            startDetector();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopDetector();
    }

    void startDetector() {
        if (!detector.isRunning()) {
            detector.start();
        }
    }

    void stopDetector() {
        if (detector.isRunning()) {
            detector.stop();
            resultTextView.setText("Stop Analysis");
            ChatFragment.emotion = "StopAnalysis"; //얼굴 없을 때 감정 초기화 (Detecting...)
            resultImageView.setImageResource(R.drawable.stop);

        }
    }

    @Override
    public void onImageResults(List<Face> list, Frame frame, float v) {
        if (list == null)

            return;
        if (list.size() == 0) {
            setText();
        } else {
            Face face = list.get(0);
            updateText(face);
        }
    }
    protected void setText(){
        //smileTextView.setText("no face detected");
        //angerTextView.setText("");
        //valenceTextView.setText(""); //fear->valance
        //surpriseTextView.setText("");//disgust -> surprise
        //contemptTextView.setText("");
        //focusTextView.setText("");
        //sadnessTextView.setText("");
        resultTextView.setText("Detecting.....");
        ChatFragment.emotion = "Detecting"; //얼굴 없을 때 감정 초기화 (Detecting...)
        resultImageView.setImageResource(R.drawable.eye); //얼굴 없을 때 감정 초기화 (Detecting...)

    }

    public float[] updateText(Face face){
        float solution[] = new float[7];
        float highest = 0;
        int index = 0;
        result="Detecting";
        resultImageView.setImageResource(R.drawable.eye); //얼굴 없을 때 감정 초기화 (Detecting...)
        solution[0] = face.emotions.getJoy();
        solution[1] = face.emotions.getAnger();
        solution[2] = face.emotions.getSadness();
        solution[3] = face.emotions.getSurprise(); //disgust -> surprise
        solution[4] = face.emotions.getContempt();
        //solution[5] = face.emotions.getFear();
        //solution[5] = face.expressions.getAttention();
        //solution[6] = face.emotions.getValence();
        for(int i = 0;i<5; i++){
            if(solution[i] > highest){
                highest = solution[i];
                if(highest>=1.0){
                    index = i;

                }else{
                    result="집중";
                    index=6;
                }
            }


        }

        switch(index){
            case 0:
                result = "기쁨";
                resultImageView.setImageResource(R.drawable.smile);
                break;
            case 1:
                result = "화남";
                resultImageView.setImageResource(R.drawable.anger);
                break;
            case 2:
                result = "슬픔";
                resultImageView.setImageResource(R.drawable.sadness);
                break;
            case 3:
                result = "놀람";
                resultImageView.setImageResource(R.drawable.surprise);
                break;
            case 4:
                result = "경멸";
                resultImageView.setImageResource(R.drawable.contempt);
                break;
            default:
                //result="집중\uD83D\uDE10";
                resultImageView.setImageResource(R.drawable.none);
                break;

        }

        //smileTextView.setText(String.format("SMILE\n%.2f",solution[0]));
        //angerTextView.setText(String.format("ANGER\n%.2f",solution[1]));
        //sadnessTextView.setText(String.format("SADNESS\n%.2f",solution[2]));
        //valenceTextView.setText(String.format("SCORE\n%.2f",solution[6])); //fear->valence
        //surpriseTextView.setText(String.format("SURPRISE\n%.2f",solution[3]));//disgust -> surprise
        //contemptTextView.setText(String.format("CONTEMPT\n%.2f",solution[4]));
        //focusTextView.setText(String.format("FOCUS\n%.2f",solution[5]));
        ChatFragment.emotion = result;
        resultTextView.setText(result);


        return solution;
}

    @SuppressWarnings("SuspiciousNameCombination")
    @Override
    public void onCameraSizeSelected(int width, int height, Frame.ROTATE rotate) {
        if (rotate == Frame.ROTATE.BY_90_CCW || rotate == Frame.ROTATE.BY_90_CW) {
            previewWidth = height;
            previewHeight = width;
        } else {
            previewHeight = height;
            previewWidth = width;
        }
        cameraPreview.requestLayout();
    }

        @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        chatFragment.backPressed();
        finish();
    }

}
