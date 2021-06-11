package gujc.emotionalTalk.chat;

import android.app.Activity;
import android.os.Bundle;
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

import java.util.List;

import gujc.emotionalTalk.R;

public class EmotionDetection extends Activity implements Detector.ImageListener, CameraDetector.CameraEventListener
{

    final String LOG_TAG = "Emotion Detection";

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emotion_detection);
        initialiseUI();

        isSDKStarted = getIntent().getExtras().getBoolean("sdkStarted");

        startSDKButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isSDKStarted) {
                    isSDKStarted = false;
                    stopDetector();

                    startSDKButton.setText("start analysis");
                } else {
                    isSDKStarted = true;
                    startDetector();
                    startSDKButton.setText("stop analysis");
                }
            }
        });


        //We create a custom SurfaceView that resizes itself to match the aspect ratio of the incoming camera frames
        cameraPreview = new SurfaceView(this) {
            @Override
            public void onMeasure(int widthSpec, int heightSpec) {
                int measureWidth = MeasureSpec.getSize(widthSpec);
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
                setMeasuredDimension(width,height);
            }
        };

        setLayout();

        detector = new CameraDetector(this, CameraDetector.CameraType.CAMERA_FRONT, cameraPreview);

        detectEmotions();
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
        params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        cameraPreview.setLayoutParams(params);
        mainLayout.addView(cameraPreview,0);
        cameraPreview.setVisibility(View.INVISIBLE);
    }

    protected void initialiseUI(){

        smileTextView = (TextView) findViewById(R.id.smile_textview);
        contemptTextView = (TextView) findViewById(R.id.contempt_textview);
        valenceTextView = (TextView) findViewById(R.id.valance_textview);
        angerTextView = (TextView) findViewById(R.id.anger_textview);
        surpriseTextView = (TextView) findViewById(R.id.surprise_textview);
        focusTextView = (TextView) findViewById(R.id.focus_textview);
        sadnessTextView = (TextView) findViewById(R.id.sadness_textview);
        resultTextView = (TextView)findViewById(R.id.result_emotion_textview);
        resultImageView = (ImageView)findViewById(R.id.result_imageView);
        startSDKButton = (Button) findViewById(R.id.sdk_start_button);
        startSDKButton.setText("stop analysis");
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
        smileTextView.setText("no face detected");
        angerTextView.setText("");
        valenceTextView.setText(""); //fear->valance
        surpriseTextView.setText("");//disgust -> surprise
        contemptTextView.setText("");
        focusTextView.setText("");
        sadnessTextView.setText("");
        resultTextView.setText("");

    }

    public float[] updateText(Face face){
        float solution[] = new float[7];
        float highest = 0;
        int index = 0;
        String result = "";
        solution[0] = face.emotions.getJoy();
        solution[1] = face.emotions.getAnger();
        solution[2] = face.emotions.getSadness();
        solution[3] = face.emotions.getSurprise();
        solution[4] = face.emotions.getContempt();
        solution[5] = face.expressions.getAttention();
        solution[6] = face.emotions.getValence();
        for(int i = 0;i<5; i++){
                if(solution[i] > highest){
                    highest = solution[i];
                    if(highest>=3.0){
                        index = i;

                    }else{
                        result="absence of expression";
                        index=6;
                    }
                }


        }

        switch(index){
            case 0:
                result = "Happy";
                resultImageView.setImageResource(R.drawable.smile);
                break;
            case 1:
                result = "Angry";
                resultImageView.setImageResource(R.drawable.anger);
                break;
            case 2:
                result = "Sad";
                resultImageView.setImageResource(R.drawable.sadness);
                break;
            case 3:
                result = "Surprise";
                resultImageView.setImageResource(R.drawable.surprise);
                break;
            case 4:
                result = "Contempt";
                resultImageView.setImageResource(R.drawable.contempt);
                break;
            default:
                resultImageView.setImageResource(R.drawable.none);
                break;

        }

        smileTextView.setText(String.format("SMILE\n%.2f",solution[0]));
        angerTextView.setText(String.format("ANGER\n%.2f",solution[1]));
        sadnessTextView.setText(String.format("SADNESS\n%.2f",solution[2]));
        valenceTextView.setText(String.format("SCORE\n%.2f",solution[6])); //fear->valence
        surpriseTextView.setText(String.format("SURPRISE\n%.2f",solution[3]));//disgust -> surprise
        contemptTextView.setText(String.format("CONTEMPT\n%.2f",solution[4]));
        focusTextView.setText(String.format("FOCUS\n%.2f",solution[5]));
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


}



