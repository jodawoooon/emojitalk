package gujc.emotionalTalk.chat;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import gujc.emotionalTalk.R;
import gujc.emotionalTalk.model.UserModel;

public class AnalyActivity extends AppCompatActivity {

    private FirebaseFirestore firestore;
    private String myUid;
    private static final String TAG = "Analysis";
    private TextView tv;
    private Map<String, UserModel> userList = new HashMap<>();
    private Handler mHandler;

    private Socket socket;
    private WebView myWebView;


    private BufferedReader networkReader;
    private PrintWriter networkWriter;
    private String line;
    private DataOutputStream dos;
    private DataInputStream dis;
    private String roomID;
    private String ip = "3.18.165.108";            // IP 번호
    private int port = 5000;
    private String str;
    String usernm ="";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analy);
        roomID = getIntent().getStringExtra("roomID");

        firestore = FirebaseFirestore.getInstance();
        myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        NetworkTask networkTask = new NetworkTask("http://3.18.165.108:9999/");
        networkTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);;
        firestore.collection("rooms").document(roomID).collection("messages").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                List<String> list = new ArrayList<>();

                if (task.isSuccessful()) {




                    for (QueryDocumentSnapshot document : task.getResult()) {



                        StringBuilder result = new StringBuilder();

                        StringBuilder result2 = new StringBuilder();

                        /*firestore.collection("users").document(document.get("uid").toString()).addSnapshotListener(new EventListener<DocumentSnapshot>() {
                            @Override
                            public void onEvent(DocumentSnapshot snapshot, FirebaseFirestoreException e) {
                                if (e != null) {
                                    Log.w(TAG, "Listen failed.", e);
                                    return;
                                }

                                if (snapshot != null && snapshot.exists()) {
                                    usernm = snapshot.get("usernm").toString();
                                    Log.d(TAG, "USERNM"+usernm);
                                } else {
                                    Log.d(TAG, "Current data: null");
                                }
                            }
                        });*/

                        //String timestamp = dateFormat.format(document.get("timestamp"));
                        //result2 = result.append(document.get("uid")+usernm).append((" : ")).append(document.get("msg")).append(" / ").append(document.get("timestamp")).append("\n");
                        //connect(result2.toString());

                        switch(document.get("uid").toString()) {
                            case "6KBJEoapNmTgJ1QVBQEmH5kUBkt2":
                                usernm = "이상해씨";
                                break;
                            case "XXh60yLOIYcbbjaiA1ukKNfpHjk1":
                                usernm = "익명";
                                break;
                            case "jg65kPXyeIXQJNqgGLUHRSWS3aF3":
                                usernm = "꼬부기";
                                break;
                            case "m3pmcyafZWd8cjZsgpU3pRp3gg82":
                                usernm = "지성";
                                break;

                            case "I0jGvjxLo2f8wGVo380Oa6hXOfG3":
                                usernm = "이름";
                                break;

                            default:
                                usernm = "새멤버";
                                break;

                        }
                        //result.append(document.get("uid")).append((" : ")).append(document.get("msg")).append(" / ").append(document.get("timestamp")).append("\n");
                        result.append(usernm).append((" : ")).append(document.get("msg")).append(" / ").append(document.get("timestamp")).append("\n");
                        list.add(result.toString());

                    }


                    Log.d(TAG, list.toString());
                    //tv.setText(list.toString());

                    //str = list.toString();

                    Log.d(TAG, "Error getting documents: ", task.getException());
                }
                for (String str : list){
                    connect(str);
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                connect("stop");



                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }


                //Log.w("while문 시작", "check");

                //try {
                //    Thread.sleep(1000);
                //} catch (InterruptedException e) {
                //    e.printStackTrace();
                //}

            }
        });








    }


    void connect(final String msg){
        mHandler = new Handler();
        Log.w("connect","연결 하는중");
        // 받아오는거
        Thread checkUpdate = new Thread() {
            public void run() {

                // 서버 접속
                try {
                    socket = new Socket(ip, port);
                    Log.w("서버 접속됨", "서버 접속됨");
                } catch (IOException e1) {
                    Log.w("서버접속못함", "서버접속못함");
                    e1.printStackTrace();
                }

                Log.w("edit 넘어가야 할 값 : ","안드로이드에서 서버로 연결요청");
                // Buffered가 잘못된듯.
                try {
                    dos = new DataOutputStream(socket.getOutputStream());   // output에 보낼꺼 넣음
                    dis = new DataInputStream(socket.getInputStream());     // input에 받을꺼 넣어짐

                    dos.writeUTF(msg);


            } catch (IOException e) {
                e.printStackTrace();
                Log.w("버퍼", "버퍼생성 잘못됨");
            }
                Log.w("버퍼", "버퍼생성 잘됨");



        }
        };
        // 소켓 접속 시도, 버퍼생성
        checkUpdate.start();
    }

    public Map<String, UserModel> getUserList() {
        return userList;
    }



    public class NetworkTask extends AsyncTask<String, String, Boolean> {

        String url;
        private int responseHttp = 0;
        private boolean flag = false;
        ImageView imageView;
        Intent intent;


        NetworkTask(String url){
            this.url = url;

        }


        protected void onPreExecute() {
            super.onPreExecute();

            imageView = (ImageView)findViewById(R.id.image);
            Glide.with(AnalyActivity.this).load(R.raw.load4).into(imageView);
            intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://3.18.165.108:9999/"));


        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected Boolean doInBackground(String... urlPath) {
            while(true) {
                try {
                        Log.d(TAG, "doInBackground: +"+flag);
                        URL url = new URL("http://3.18.165.108:9999/");
                        URLConnection connection = url.openConnection();
                        connection.setConnectTimeout(2000);
                        HttpURLConnection httpConnection = (HttpURLConnection) connection;
                        responseHttp = httpConnection.getResponseCode();
                        if (responseHttp == HttpURLConnection.HTTP_OK) {
                            flag = true;
                            break;
                        } else {
                            flag = false;
                        }


                } catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
            return flag;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            //progressDialog.dismiss();

            startActivity(intent);
            // 통신이 완료되면 호출됩니다.
            // 결과에 따른 UI 수정 등은 여기서 합니다.
            //Toast.makeText(getApplicationContext(), result, Toast.LENGTH_LONG).show();
        }
    }






}