package polar.com.androidblesdk;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ReplacementTransformationMethod;
import android.text.method.ScrollingMovementMethod;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import polar.com.sdk.api.PolarBleApi;
import polar.com.sdk.api.PolarBleApiCallback;
import polar.com.sdk.api.PolarBleApiDefaultImpl;
import polar.com.sdk.api.model.PolarAccelerometerData;
import polar.com.sdk.api.model.PolarDeviceInfo;
import polar.com.sdk.api.model.PolarEcgData;
import polar.com.sdk.api.model.PolarExerciseEntry;
import polar.com.sdk.api.model.PolarHrBroadcastData;
import polar.com.sdk.api.model.PolarHrData;
import polar.com.sdk.api.model.PolarOhrPPGData;
import polar.com.sdk.api.model.PolarOhrPPIData;

public class MainActivity extends AppCompatActivity {

    private final static String TAG = MainActivity.class.getSimpleName();

    PolarBleApi api;

    Disposable broadcastDisposable;

    Disposable ecgDisposable;
    Disposable accDisposable;
    Disposable ppgDisposable;
    Disposable ppiDisposable;
    Disposable scanDisposable;


    boolean connectToggle = false;
    String mInputDeviceId = "1770BF25"; // TODO replace with your device id

    Disposable autoConnectDisposable;
    boolean broadcastStarted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();

        api = PolarBleApiDefaultImpl.defaultImplementation(this);
        final Button broadcast = this.findViewById(R.id.broadcast_button);
        final Button connect = this.findViewById(R.id.connect_button);
        final Button ecg = this.findViewById(R.id.ecg_button);
        final Button acc = this.findViewById(R.id.acc_button);
        final Button ppg = this.findViewById(R.id.ohr_ppg_button);
        final Button ppi = this.findViewById(R.id.ohr_ppi_button);
        final Button scan = this.findViewById(R.id.scan_button);
        final Button list = this.findViewById(R.id.list_exercises);

        api.setApiCallback(new PolarBleApiCallback() {
            @Override
            public void blePowerStateChanged(boolean powered) {
                Log.d(TAG, "BLE power: " + powered);
            }

            @Override
            public void polarDeviceConnected(String identifier) {
                Log.d(TAG, "CONNECTED: " + identifier);
                mInputDeviceId = identifier;
            }

            @Override
            public void polarDeviceDisconnected(String identifier) {
                Log.d(TAG, "DISCONNECTED: " + identifier);
                ecgDisposable = null;
                accDisposable = null;
                ppgDisposable = null;
                ppiDisposable = null;
            }

            @Override
            public void ecgClientReady(String identifier) {
                Log.d(TAG, "ECG READY: " + identifier);
                // ecg streaming can be started now if needed
            }

            @Override
            public void accelerometerClientReady(String identifier) {
                Log.d(TAG, "ACC READY: " + identifier);
                // acc streaming can be started now if needed
            }

            @Override
            public void ppgClientReady(String identifier) {
                Log.d(TAG, "PPG READY: " + identifier);
                // ohr ppg can be started
            }

            @Override
            public void ppiClientReady(String identifier) {
                Log.d(TAG, "PPI READY: " + identifier);
                // ohr ppi can be started
            }

            @Override
            public void hrClientReady(String identifier) {
                Log.d(TAG, "HR READY: " + identifier);
                // hr notifications are about to start
            }

            @Override
            public void fwInformationReceived(String identifier, String fwVersion) {
                Log.d(TAG, "FW: " + fwVersion);

            }

            @Override
            public void batteryLevelReceived(String identifier, int level) {
                Log.d(TAG, "BATTERY LEVEL: " + level);

            }

            @Override
            public void hrNotificationReceived(String identifier, PolarHrData data) {
                Log.d(TAG, "HR value: " + data.hr);
            }

            @Override
            public void polarFtpReady(String s) {
                Log.d(TAG, "FTP ready");
            }
        });


        broadcast.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (broadcastDisposable == null) {
                    Log.d(TAG, "hr broadcast start...");
                    broadcastStarted = false;
                    broadcastDisposable = api.startListenForPolarHrBroadcasts(null).subscribe(
                            new Consumer<PolarHrBroadcastData>() {
                                @Override
                                public void accept(PolarHrBroadcastData polarBroadcastData) throws Exception {
                                    if (!broadcastStarted) {
                                        broadcastStarted = true;
                                        Log.d(TAG, "开始接受传感器的心率值，将会打印小于10的值，请注意观察！");
                                    }

                                    if (polarBroadcastData.hr < 10) {
                                        Log.d(TAG, "hr broadcast " + polarBroadcastData.polarDeviceInfo.deviceId + " HR: " + polarBroadcastData.hr + " batt: " + polarBroadcastData.batteryStatus);
                                    }

                                }
                            },
                            new Consumer<Throwable>() {
                                @Override
                                public void accept(Throwable throwable) throws Exception {
                                    Log.e(TAG, "hr broadcast " + throwable2Str(throwable));
                                }
                            },
                            new Action() {
                                @Override
                                public void run() throws Exception {
                                    Log.d(TAG, "hr broadcast complete");
                                }
                            }
                    );
                } else {
                    broadcastDisposable.dispose();
                    Log.d(TAG, String.format("hr broadcast stop ,isDisposed=%s", broadcastDisposable.isDisposed()));
                    broadcastDisposable = null;
                }
            }
        });

        connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                /*自动链接
                if(autoConnectDisposable!=null){
                    autoConnectDisposable.dispose();
                    autoConnectDisposable = null;
                }
                autoConnectDisposable = api.autoConnectToPolarDevice(-50);
                */

                if (!connectToggle) {
                    Log.d(TAG, "connect start...please turn on you device");
                    api.connectToPolarDevice(mInputDeviceId);
                } else {
                    Log.d(TAG, "connect stop");
                    api.disconnectFromPolarDevice(mInputDeviceId);
                }
                connectToggle = !connectToggle;
            }
        });

        ecg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ecgDisposable == null) {
                    Log.d(TAG, "ecg start...");
                    ecgDisposable = api.startEcgStreaming(mInputDeviceId).observeOn(AndroidSchedulers.mainThread()).subscribe(
                            new Consumer<PolarEcgData>() {
                                @Override
                                public void accept(PolarEcgData polarEcgData) throws Exception {
                                    int i = 0;
                                    for (PolarEcgData.PolarEcgSample data : polarEcgData.samples) {
                                        Log.d(TAG, "ecg" + i + "    mV: " + data.milliVolts);
                                        i++;
                                    }
                                }
                            },
                            new Consumer<Throwable>() {
                                @Override
                                public void accept(Throwable throwable) throws Exception {
                                    Log.e(TAG, "ecg " + throwable2Str(throwable));
                                }
                            },
                            new Action() {
                                @Override
                                public void run() throws Exception {
                                    Log.d(TAG, "ecg complete");
                                }
                            }
                    );
                } else {
                    // NOTE stops streaming if it is "running"
                    Log.d(TAG, "ecg stop");
                    ecgDisposable.dispose();
                    ecgDisposable = null;
                }
            }
        });

        acc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (accDisposable == null) {
                    Log.d(TAG, "acc start...");
                    accDisposable = api.startAccStreaming(mInputDeviceId,
                            PolarBleApi.AccelerometerSamplingRate.SAMPLING_200HZ,
                            PolarBleApi.AccelerometerRange.RANGE_8G).observeOn(AndroidSchedulers.mainThread()).subscribe(
                            new Consumer<PolarAccelerometerData>() {
                                @Override
                                public void accept(PolarAccelerometerData polarAccelerometerData) throws Exception {
                                    int i = 0;
                                    for (PolarAccelerometerData.PolarAccelerometerSample data : polarAccelerometerData.samples) {
                                        Log.d(TAG, "acc" + i + "    x: " + data.x + " y: " + data.y + " z: " + data.z);
                                        i++;
                                    }

                                }
                            },
                            new Consumer<Throwable>() {
                                @Override
                                public void accept(Throwable throwable) throws Exception {
                                    Log.e(TAG, "acc " + throwable2Str(throwable));
                                }
                            },
                            new Action() {
                                @Override
                                public void run() throws Exception {
                                    Log.d(TAG, "acc complete");
                                }
                            }
                    );
                } else {
                    // NOTE dispose will stop streaming if it is "running"
                    Log.d(TAG, "acc stop");
                    accDisposable.dispose();
                    accDisposable = null;
                }
            }
        });

        ppg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ppgDisposable == null) {
                    Log.d(TAG, "ppg start...");
                    ppgDisposable = api.startOhrPPGStreaming(mInputDeviceId).observeOn(AndroidSchedulers.mainThread()).subscribe(
                            new Consumer<PolarOhrPPGData>() {
                                @Override
                                public void accept(PolarOhrPPGData polarOhrPPGData) throws Exception {
                                    Log.d(TAG, "ppg    c: " + polarOhrPPGData.rc);
                                    int i = 0;
                                    for (PolarOhrPPGData.PolarOhrPPGSample data : polarOhrPPGData.samples) {
                                        Log.d(TAG, "ppg" + i + "    ppg0: " + data.ppg0 + " ppg1: " + data.ppg1 + " ppg2: " + data.ppg2 + "ambient: " + data.ambient);
                                        i++;
                                    }
                                }
                            },
                            new Consumer<Throwable>() {
                                @Override
                                public void accept(Throwable throwable) throws Exception {
                                    Log.e(TAG, "ppg " + throwable2Str(throwable));
                                }
                            },
                            new Action() {
                                @Override
                                public void run() throws Exception {
                                    Log.d(TAG, "ppg complete");
                                }
                            }
                    );
                } else {
                    Log.d(TAG, "ppg stop");
                    ppgDisposable.dispose();
                    ppgDisposable = null;
                }
            }
        });

        ppi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ppiDisposable == null) {
                    Log.d(TAG, "ppi start...");
                    ppiDisposable = api.startOhrPPIStreaming(mInputDeviceId).observeOn(AndroidSchedulers.mainThread()).subscribe(
                            new Consumer<PolarOhrPPIData>() {
                                @Override
                                public void accept(PolarOhrPPIData ppiData) throws Exception {
                                    Log.d(TAG, "ppi    c: " + ppiData.rc + " ppi: " + ppiData.ppi
                                            + " blocker: " + ppiData.blockerBit + " errorEstimate: " + ppiData.errorEstimate);

                                }
                            },
                            new Consumer<Throwable>() {
                                @Override
                                public void accept(Throwable throwable) throws Exception {
                                    Log.e(TAG, "ppi " + throwable2Str(throwable));
                                }
                            },
                            new Action() {
                                @Override
                                public void run() throws Exception {
                                    Log.d(TAG, "ppi complete");
                                }
                            }
                    );
                } else {
                    Log.d(TAG, "ppi stop");
                    ppiDisposable.dispose();
                    ppiDisposable = null;
                }
            }
        });

        scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (scanDisposable == null) {
                    Log.d(TAG, "scan start...");
                    scanDisposable = api.searchForPolarDevice().observeOn(AndroidSchedulers.mainThread()).subscribe(
                            new Consumer<PolarDeviceInfo>() {
                                @Override
                                public void accept(PolarDeviceInfo polarDeviceInfo) throws Exception {
                                    Log.d(TAG, "scan found id: " + polarDeviceInfo.deviceId + " rssi: " + polarDeviceInfo.rssi + " name: " + polarDeviceInfo.name);
                                }
                            },
                            new Consumer<Throwable>() {
                                @Override
                                public void accept(Throwable throwable) throws Exception {
                                    Log.e(TAG, "scan " + throwable2Str(throwable));
                                }
                            },
                            new Action() {
                                @Override
                                public void run() throws Exception {
                                    Log.d(TAG, "scan complete");
                                }
                            }
                    );
                } else {
                    Log.d(TAG, "scan stop");
                    scanDisposable.dispose();
                    scanDisposable = null;
                }
            }
        });

        list.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("CheckResult")
            @Override
            public void onClick(View v) {
                Log.d(TAG, "lit start...");
                api.listExercises(mInputDeviceId).observeOn(AndroidSchedulers.mainThread()).subscribe(
                        new Consumer<PolarExerciseEntry>() {
                            @Override
                            public void accept(PolarExerciseEntry polarExerciseEntry) throws Exception {
                                Log.d(TAG, "list    :" + polarExerciseEntry.date + " path: " + polarExerciseEntry.path);

                            }
                        },
                        new Consumer<Throwable>() {
                            @Override
                            public void accept(Throwable throwable) throws Exception {
                                Log.e(TAG, "list " + throwable2Str(throwable));
                            }
                        },
                        new Action() {
                            @Override
                            public void run() throws Exception {
                                Log.d(TAG, "list complete");
                            }
                        }
                );
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && savedInstanceState == null) {
            this.requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        api.shutDown();
    }

    EditText mEditInput;
    TextView mTvData;

    public void appData2Tv(String data) {
        CharSequence str = mTvData.getText();
        StringBuilder sb = new StringBuilder(str);
        int lineCount = mTvData.getLayout().getLineCount();
        //android.util.Log.i("lintest", "line = " + lineCount);
        if (lineCount > 300) {
            int pos = sb.indexOf("\n", sb.length() / 2);
            sb.delete(pos, sb.length() - 1);
        }
        String tag = lineCount % 2 == 0 ? "*" : "-";
        sb.insert(0, tag + " " + data + "\n");
        mTvData.setText(sb.toString());
    }

    private void init() {

        mTvData = findViewById(R.id.tv_data);
        mTvData.setMovementMethod(ScrollingMovementMethod.getInstance());
        mTvData.setLongClickable(true);
        mTvData.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                TextView textView = (TextView) v;
                ClipboardManager cmb = (ClipboardManager) MainActivity.this.getSystemService(Context.CLIPBOARD_SERVICE);
                cmb.setText(textView.getText().toString().trim()); //将内容放入粘贴管理器,在别的地方长按选择"粘贴"即可
                Toast.makeText(MainActivity.this, "copy done", Toast.LENGTH_SHORT).show();
                return true;
            }
        });

        mInputDeviceId = getSharedPreferences("device-info", Context.MODE_PRIVATE).getString("id-device", "");

        mEditInput = this.findViewById(R.id.edit_input);
        mEditInput.setText(mInputDeviceId);
        mEditInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

                if (actionId != EditorInfo.IME_ACTION_DONE) {
                    return false;
                }

                if (mEditInput.getText().toString().trim().length() != 8) {
                    Toast.makeText(MainActivity.this, "Device Id 是8位", Toast.LENGTH_SHORT).show();
                    return false;
                }

                mInputDeviceId = mEditInput.getText().toString().trim().toUpperCase();
                getSharedPreferences("device-info", Context.MODE_PRIVATE).edit().putString("id-device", mInputDeviceId).apply();
                appData2Tv("change device id ->" + mInputDeviceId);

                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);

                findViewById(R.id.layout_container).requestFocus();
                return true;
            }
        });
        mEditInput.setTransformationMethod(new ReplacementTransformationMethod() {
            @Override
            protected char[] getOriginal() {
                char[] originalCharArr = {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'};
                return originalCharArr;
            }

            @Override
            protected char[] getReplacement() {
                char[] replacementCharArr = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'};
                return replacementCharArr;
            }
        });

        Log.mActivity = this;
    }

    public static class Log {

        static MainActivity mActivity;

        public static void d(String tag, String message) {
            android.util.Log.w(tag, message);
            mActivity.appData2Tv(message);
        }

        public static void e(String tag, String message) {
            android.util.Log.e(tag, message);
            mActivity.appData2Tv("error : " + message);
        }
    }

    public static String throwable2Str(Throwable e) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PrintStream pr = new PrintStream(out);
            e.printStackTrace(pr);
            pr.flush();
            String message = out.toString();
            out.close();
            return message;
        } catch (Exception err) {
            return "unkonw";
        }

    }

}
