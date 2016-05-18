package com.voodoo.configtool;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import com.voodoo.configtool.UDPProcessor.OnReceiveListener;

public class MainActivity extends AppCompatActivity implements OnReceiveListener{

    Button btnFind, btnSave;
    TextView tvInfo;
    CheckBox chbSwap;
    EditText ssid, ssidPass;

    String[] devMode = {"Master","Slave"};
    String[] sensType = {"Local", "Remote"};
    String[] wifiMode= {"NULL_MODE","STATION_MODE","SOFTAP_MODE","STATIONAP_MODE"};
    String[] wifiSecurityMode = {"AUTH_OPEN","AUTH_WEP","AUTH_WPA_PSK","AUTH_WPA2_PSK","AUTH_WPA_WPA2_PSK","AUTH_MAX"};

    public static String configReference = "lanConfig";

    UDPProcessor udpsend;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnFind = (Button) findViewById(R.id.btnFind);
        btnSave = (Button) findViewById(R.id.btnSave);
        tvInfo = (TextView) findViewById(R.id.tvInfo);
        chbSwap = (CheckBox) findViewById(R.id.chbSwap);
        ssid = (EditText) findViewById(R.id.etSSID);
        ssidPass = (EditText) findViewById(R.id.etSSIDPASS);

        udpsend = new UDPProcessor(7777);
        udpsend.start();
        udpsend.setOnReceiveListener(this);
        final byte[] configData = new byte[6];
        for(int i = 0; i < configData.length; i++) configData[i] = 0;

        // адаптер
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, sensType);
        ArrayAdapter<String> adapterW = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, wifiMode);
        ArrayAdapter<String> adapterS = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, wifiSecurityMode);
        ArrayAdapter<String> adapterDMode = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, devMode);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        adapterW.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        adapterS.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        adapterDMode.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        Spinner spinner1 = (Spinner) findViewById(R.id.spinner1);
        Spinner spinner2 = (Spinner) findViewById(R.id.spinner2);
        Spinner spinnerW = (Spinner) findViewById(R.id.spinwWifiMode);
        Spinner spinnerS = (Spinner) findViewById(R.id.spinSecur);
        Spinner spinnerDMode = (Spinner) findViewById(R.id.spinDevMode);
        spinner1.setAdapter(adapter);
        spinner2.setAdapter(adapter);
        spinnerW.setAdapter(adapterW);
        spinnerS.setAdapter(adapterS);
        spinnerDMode.setAdapter(adapterDMode);
//        // заголовок
//        spinner1.setPrompt("Sens1");
//        spinner2.setPrompt("Sens2");
        // выделяем элемент
//        spinner1.setSelection(0);
//        spinner2.setSelection(0);
//        spinnerW.setSelection(0);

        byte[]cfg = loadConfig();
        if(cfg.length > 0)
        {
            spinnerDMode.setSelection((int)cfg[0]);
            spinnerS.setSelection((int)cfg[5]);
            spinnerW.setSelection((int)cfg[4]);
            spinnerDMode.setSelection((int)cfg[0]);
            spinner1.setSelection((int)cfg[1]);
            spinner2.setSelection((int)cfg[2]);
            if(cfg[3] == 1) chbSwap.setChecked(true);

            String str = new String(cfg);
            ssid.setText(str.substring(6,str.indexOf('$')));
            ssidPass.setText(str.substring(str.indexOf('$') + 1, str.length()));
        }

        // устанавливаем обработчик нажатия
        spinnerDMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                configData[0] = (byte) position;
            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        spinnerS.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                configData[5] = (byte) position;
            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        spinnerW.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                configData[4] = (byte) position;
            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        spinner1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                configData[1] = (byte) position;
            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        spinner2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                configData[2] = (byte) position;
            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });
        //==========================================================================================
        btnFind.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                String str = "I1";
                byte[]ipAddr = IPHelper.getBroadcastIP4AsBytes();//new byte[]{ (byte)192, (byte) 168, (byte) 4, (byte) 255};
                try
                {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.UK);
                    Calendar cal = Calendar.getInstance();
                    str += dateFormat.format(cal.getTime());
                    DataFrame df = new DataFrame(str.getBytes());
                    udpsend.send(InetAddress.getByAddress(ipAddr), df);
                }
                catch(UnknownHostException e1)
                {

                }
            }
        });
        //==========================================================================================
        btnSave.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                if(chbSwap.isChecked()) configData[3] = 1;
                else configData[3] = 0;

                String str = "HWCFG" +(new String (configData)) + ssid.getText().toString() + "$" + ssidPass.getText().toString();
                byte[]ipAddr = IPHelper.getBroadcastIP4AsBytes();//new byte[]{ (byte)192, (byte) 168, (byte) 4, (byte) 255};
                try
                {
                    DataFrame df = new DataFrame(str.getBytes());
                    udpsend.send(InetAddress.getByAddress(ipAddr), df);

                }
                catch(UnknownHostException e1)
                {

                }
                saveConfig((new String (configData)) + ssid.getText().toString() + "$" + ssidPass.getText().toString());
            }
        });
    }
    public void onFrameReceived(InetAddress ip, IDataFrame frame)
    {
        //String str = ;
        tvInfo.setText(new String(frame.getFrameData()));
    }

    //==============================================================================================
    void saveConfig(String aStr) {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(configReference, aStr);

        editor.apply();
    }
    //==============================================================================================
    byte[] loadConfig() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String conf = sharedPreferences.getString(configReference, "") ;
        return conf.getBytes();
    }

}
