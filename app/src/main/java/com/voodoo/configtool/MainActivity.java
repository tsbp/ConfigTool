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

    private Button btnFind, btnSave;
    private TextView tvInfo, tvTmp, tvTime;
    private CheckBox chbSwap;
    private EditText ssid, ssidPass;

    private String[] devMode = {"Master","Slave"};
    private String[] sensType = {"Local", "Remote"};
    private String[] wifiMode= {"NULL_MODE","STATION_MODE","SOFTAP_MODE","STATIONAP_MODE"};
    private String[] wifiSecurityMode = {"AUTH_OPEN","AUTH_WEP","AUTH_WPA_PSK","AUTH_WPA2_PSK","AUTH_WPA_WPA2_PSK","AUTH_MAX"};

    public static String configReference = "lanConfig";

    private UDPProcessor udpsend;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnFind = (Button) findViewById(R.id.btnFind);
        btnSave = (Button) findViewById(R.id.btnSave);
        
        tvTmp = (TextView) findViewById(R.id.tvTmp);
        tvTime = (TextView) findViewById(R.id.tvTime);
        chbSwap = (CheckBox) findViewById(R.id.chbSwap);
        ssid = (EditText) findViewById(R.id.etSSID);
        ssid.clearFocus();
        ssidPass = (EditText) findViewById(R.id.etSSIDPASS);
        ssidPass.clearFocus();

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

                String str = "";
                byte[]ipAddr = IPHelper.getBroadcastIP4AsBytes();//new byte[]{ (byte)192, (byte) 168, (byte) 4, (byte) 255};
                try
                {
                    byte [] pack = new byte[8];
                    pack[0] = (byte) 0x20;
                    pack[1] = 0;

                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyMMddHHmmss", Locale.UK);
                    Calendar cal = Calendar.getInstance();
                    str =  dateFormat.format(cal.getTime());


                    for(int  i = 0; i < 6; i++)
                        pack[i + 2] = (byte)Byte.parseByte(str.substring(i * 2, i * 2 + 2));

                    DataFrame df = new DataFrame(pack);
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

                String str = (char)0x11 +(new String (configData)) + ssid.getText().toString() + "$" + ssidPass.getText().toString();
                byte[]ipAddr = IPHelper.getBroadcastIP4AsBytes();//new byte[]{ (byte)192, (byte) 168, (byte) 4, (byte) 255};
                try
                {
                    if(ssidPass.getText().toString().length() < 8)
                    {
                        Toast.makeText(MainActivity.this,"Пароль менее 8 символов",
                                Toast.LENGTH_SHORT).show();
                    }
                    else
                    {
                        DataFrame df = new DataFrame(str.getBytes());
                        udpsend.send(InetAddress.getByAddress(ipAddr), df);
                    }

                }
                catch(UnknownHostException e1)
                {

                }
                saveConfig((new String (configData)) + ssid.getText().toString() + "$" + ssidPass.getText().toString());
            }
        });
    }
    //==============================================================================================
    String rTmp1 = "____";
    String rTmp2 = "____";
    //==============================================================================================
    public void onFrameReceived(InetAddress ip, IDataFrame frame)
    {
        byte[] in = frame.getFrameData();
        String str = new String(in);

//        if (IPHelper.convertToString(ip) != "192.168.4.100" )
//            tvTmp.setText("123");

        switch(in[0])
        {
            case (byte)0x10: // BROADCAST_DATA
                str = str.substring(1,9) + "    ";

                if(in[9] != 0) tvTime.setText(in[11] + ":" + in[10] + ":"+ in[9] + ", " + in[12] + "." + (in[13]+1) + "."+ in[14]);

                if(str.charAt(0) != '0')
                    rTmp1 = str.substring(0,4);
                if(str.charAt(4) != '0')
                    rTmp2 = str.substring(4,8);

                tvTmp.setText(rTmp1 + "  ...  " + rTmp2);
                break;

            case (byte) 0x21://#define PLOT_DATA_ANS
                tvTmp.setText(ip.toString());
                break;

            default: //case (byte) 0xAA: //OK_ANS
                tvTmp.setText("SAVED!!!");
                break;

        }
        if(in.length > 12)
        {

        }
//        else
//
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
    @Override
    protected void onDestroy() {
        super.onDestroy();
        udpsend.stop();
    }

}
