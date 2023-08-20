package com.wolftri.appyoutubedl;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.TextPaint;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import multitaks.code.Code;
import multitaks.directory.FileBlock;
import multitaks.directory.Storage;
import multitaks.directory.enums.DirectoryType;
import multitaks.socket.SocketClient;

public class MainActivity extends AppCompatActivity {

    private final static int REQUEST_CODE_CAMARE=0;

    private SocketClient socket=new SocketClient();
    private Handler uiHandler=new Handler(Looper.getMainLooper());
    private Storage s;
    private FileBlock file;
    private String path=Environment.getExternalStorageDirectory().getAbsolutePath()+"/yt-dlp";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.initComponents();
        //this.path=getExternalFilesDir("descargas").getAbsolutePath();
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,new String[]{
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
            },1);
        }
        Storage.exists(this.path,DirectoryType.FOLDER,true);
    }

    public void test(View v){
        text_info.setText(this.path);
    }

    public void startSocket(String ip, int port){
        text_info.setText("Espere");
        new Thread(new Runnable(){
            @Override
            public void run(){
                try{
                    text_info.setText("Esperando conexión: "+ip+":"+port);
                    socket.start(ip,port);
                    text_info.setText("Conectado: "+ip+":"+port);
                    socket.on("file_name", (name) -> {
                        uiHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                text_info.setText("Esperando archivo");
                            }
                        });
                        try {
                            s = new Storage(path+"/"+name);
                            file = s.fileBlock(1048576);
                            socket.emit("file_name_success", "true");
                        } catch (IOException ex) {
                            uiHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    text_info.setText(ex.getMessage());
                                }
                            });
                        }
                    });
                    socket.on("file_byte", (str_byte) -> {
                        uiHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                text_info.setText("Recibiendo: "+s.getSrc());
                            }
                        });
                        try {
                            file.write(Code.byteArrayToString(str_byte, 8192));
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    });
                    socket.on("file_byte_success",(message)->{
                        if(Boolean.parseBoolean(message)){
                            uiHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    text_info.setText("Finalizado: "+s.getSrc());
                                }
                            });
                        }
                    });
                }catch(Exception ex){
                    System.out.println(ex.getMessage());
                    uiHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            text_info.setText(ex.getMessage());
                        }
                    });
                }
            }
        }).start();
    }

    public void scan(View v){
        IntentIntegrator integrator=new IntentIntegrator(MainActivity.this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES);
        integrator.setPrompt("Escanea el código QR");
        integrator.setCameraId(REQUEST_CODE_CAMARE);
        integrator.setBeepEnabled(true);
        integrator.setBarcodeImageEnabled(true);
        integrator.initiateScan();
    }

    @Override
    protected void onActivityResult(int req_code, int res_code, @Nullable Intent data) {
        super.onActivityResult(req_code,res_code,data);
        IntentResult result=IntentIntegrator.parseActivityResult(req_code,res_code,data);
        switch(req_code){
            case REQUEST_CODE_CAMARE:{
                if(result==null){
                    text_info.setText("Error al escanear");
                    return;
                }
                if(result.getContents()==null){
                    text_info.setText("Se cancelo el escanear");
                    return;
                }
                String[] chain=result.getContents().split(":");
                this.startSocket(chain[0],Integer.parseInt(chain[1]));
                break;
            }
        }
    }

    private void initComponents(){
        this.btn_scan=findViewById(R.id.btn_scan);
        this.text_info=findViewById(R.id.text_info);
        this.box_ip=findViewById(R.id.box_ip);
        this.box_port=findViewById(R.id.box_port);
        this.btn_connect=findViewById(R.id.btn_connect);
        this.btn_connect.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                if(socket.isStart()){
                    btn_connect.setText("Conectarse");
                    try{
                        socket.close();
                    }catch(Exception ex){
                        System.out.println(ex.getMessage());
                        ex.printStackTrace();
                    }
                }else{
                    btn_connect.setText("Desconectarse");
                    startSocket(box_ip.getText().toString(),Integer.parseInt(box_port.getText().toString()));
                }
                text_info.setText("Hola!!!");
            }
        });
    }

    // Variables declaration for components
    private Button btn_scan;
    private TextView text_info;
    private EditText box_ip;
    private EditText box_port;
    private Button btn_connect;

}