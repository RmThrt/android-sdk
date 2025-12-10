package com.activelook.activelooksdk.core.ble;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.core.util.Consumer;
import androidx.core.util.Predicate;
import android.content.Context;

import io.flutter.Log;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

import com.activelook.activelooksdk.DiscoveredGlasses;
import com.activelook.activelooksdk.Sdk;
import com.activelook.activelooksdk.types.GlassesUpdate;
import com.activelook.activelooksdk.Glasses;
import com.activelook.activelooksdk.types.holdFlushAction;
import com.activelook.activelooksdk.SerializedGlasses;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Dictionary;
import com.activelook.activelooksdk.types.Rotation;


import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.Bundle;
import android.util.Log;

import com.activelook.activelooksdk.DiscoveredGlasses;
import com.activelook.activelooksdk.Glasses;
import com.activelook.activelooksdk.Sdk;
import com.activelook.activelooksdk.types.ConfigurationDescription;
import com.activelook.activelooksdk.types.DemoPattern;
import com.activelook.activelooksdk.types.FontData;
import com.activelook.activelooksdk.types.FontInfo;
import com.activelook.activelooksdk.types.GaugeInfo;
import com.activelook.activelooksdk.types.Image1bppData;
import com.activelook.activelooksdk.types.ImageData;
import com.activelook.activelooksdk.types.ImageInfo;
import com.activelook.activelooksdk.types.ImgSaveFormat;
import com.activelook.activelooksdk.types.ImgStreamFormat;
import com.activelook.activelooksdk.types.LayoutExtraCmd;
import com.activelook.activelooksdk.types.LayoutParameters;
import com.activelook.activelooksdk.types.LedState;
import com.activelook.activelooksdk.types.Rotation;
import com.activelook.activelooksdk.types.holdFlushAction;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
/**
 * ActivelookSdkPlugin
 */
public class ActivelookSdkPlugin implements FlutterPlugin, MethodCallHandler {

    private MethodChannel channel;
    private MethodChannel channelSubscription;

    private Context context;
    private BinaryMessenger messenger;
    private Sdk activelookSdk = null;

    private ArrayList<DiscoveredGlasses> discoveredGlasses = new ArrayList<>();

    Glasses glasses;
    SerializedGlasses  storedGlasses;

    // Flutter plugin implementation
    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "ActiveLookSDK");
        channelSubscription = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "ActiveLookSDKSubscription");
        channel.setMethodCallHandler(this);
        channelSubscription.setMethodCallHandler(this);

        this.context = flutterPluginBinding.getApplicationContext();
        this.messenger = flutterPluginBinding.getBinaryMessenger();
    }

    private Rotation stringToRotation(String str) {
        switch (str) {
            case "Rotation.LEFT_TB":
                return Rotation.LEFT_TB;
            case "Rotation.LEFT_BT":
                return Rotation.LEFT_BT;
            case "Rotation.RIGHT_TB":
                return Rotation.RIGHT_TB;
            case "Rotation.RIGHT_BT":
                return Rotation.RIGHT_BT;
            case "Rotation.TOP_LR":
                return Rotation.TOP_LR;
            case "Rotation.TOP_RL":
                return Rotation.TOP_RL;
            case "Rotation.BOTTOM_LR":
                return Rotation.BOTTOM_LR;
            case "Rotation.BOTTOM_RL":
                return Rotation.BOTTOM_RL;
            default:
                return Rotation.BOTTOM_LR;
        }
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        if (call.method.equals("ActiveLookSDKSubscription#initSdk")) {
            String token = (String) call.arguments;
            this.initSdk(token);
            result.success("Sdk initialized");
        } else if (call.method.equals("ActiveLookSDKSubscription#startScan")) {
            this.startScan();
            result.success("Start scanning...");
        } else if (call.method.equals("ActiveLookSDKSubscription#subscribeBatteryLevel")) {
            this.subscribeBatteryLevel();
            result.success("Subscribe battery level...");
        } else if (call.method.equals("ActiveLookSDKSubscription#subscribeFlowControl")) {
            this.subscribeFlowControl();
            result.success("Subscribe Flow Control...");
        } else if (call.method.equals("ActiveLookSDKSubscription#subscribeToSensorInterfaceNotifications")) {
            this.subscribeToSensorInterfaceNotifications();
            result.success("Subscribe sensor interface notifications...");
        } else if (call.method.equals("ActiveLookSDK#connectGlasses")) {
            String name = (String) call.argument("name");
            this.connectGlasses(name);
            result.success("Connected !");
        } else if (call.method.equals("ActiveLookSDK#disconnectGlasses")) {
            this.disconnectGlasses();
            result.success("disconnecting !");
        } else if (call.method.equals("ActiveLookSDK#grey")) {
            int level = (int) call.arguments;
            this.grey(level);
            result.success("Grey !");
        } else if (call.method.equals("ActiveLookSDK#luma")) {
            byte intensity = Byte.valueOf(call.arguments.toString());
            this.luma(intensity);
            result.success("Luma !");
        } else if (call.method.equals("ActiveLookSDK#clear")) {
            this.clear();
            result.success("Cleared!");
        } else if (call.method.equals("ActiveLookSDK#shift")) {
            int x = call.argument("x");
            int y = call.argument("y");
            this.shift(x, y);
            result.success("Shifted " + x + " " + y + " !");
        } else if (call.method.equals("ActiveLookSDK#txt")) {
            int x = call.argument("x");
            int y = call.argument("y");
            Rotation rotation = stringToRotation(call.argument("rotation"));
            int font = call.argument("font");
            int color = call.argument("color");
            String string = call.argument("string");
            this.txt((short) x, (short) y, rotation, (byte) font, (byte) color, string);
            result.success("Txt " + string + " !");
        } else if (call.method.equals("ActiveLookSDK#circle")) {
            int x = call.argument("x");
            int y = call.argument("y");
            byte radius = Byte.valueOf(call.argument("radius").toString());
            this.circle((short) x, (short) y, (byte) radius);
            result.success("circle!");
        } else if (call.method.equals("ActiveLookSDK#circlef")) {
            int x = call.argument("x");
            int y = call.argument("y");
            byte radius = Byte.valueOf(call.argument("radius").toString());
            this.circlef((short) x, (short) y, (byte) radius);
            result.success("circlef!");
        } else if (call.method.equals("ActiveLookSDK#rectf")) {
            int x = call.argument("x");
            int y = call.argument("y");
            int width = call.argument("width");
            int height = call.argument("height");
            this.rectangleFull((short) x, (short) y, (short) width, (short) height);
            result.success("rectangleFull!");
        } else if (call.method.equals("ActiveLookSDK#polyline")) {
            byte thickness = Byte.valueOf(call.argument("thickness").toString());
            ArrayList<Integer> coordinatesList = (ArrayList<Integer>) call.argument("points");
            short[] coordinates = new short[coordinatesList.size()];
            for (int i = 0; i < coordinatesList.size(); i++) {
                coordinates[i] = coordinatesList.get(i).shortValue();
            }
            this.polyline(thickness, coordinates);
            result.success("Polyline !");
        }  } else if (call.method.equals("ActiveLookSDK#polylines")) {
          byte thickness = Byte.valueOf(call.argument("thickness").toString());
            ArrayList<ArrayList<Integer>> coordinatesList = (ArrayList<ArrayList<Integer>>) call.argument("points");
            short[][] coordinates = new short[coordinatesList.size()][];
            for (int i = 0; i < coordinatesList.size(); i++) {
                for (int j = 0; j < coordinatesList.get(i).size(); j++) {
                    coordinates[i][j] = coordinatesList.get(i).get(j).shortValue();
                }
            }
            this.polylines(thickness, coordinates);
            result.success("Polylines !");
        } else if (call.method.equals("ActiveLookSDK#holdAndFush")) {
            int action = call.arguments();
            this.holdAndFlush(action);
            result.success("holdAndFlush !");
        } else if (call.method.equals("ActiveLookSDK#color")) {
            byte color = Byte.valueOf(call.argument("value").toString());
            this.color(color);
            result.success("color !");
        } else if( call.method.equals("ActiveLookSDK#getSerializedGlasses")) {
            this.getSerializedGlasses();
            result.success("getSerializedGlasses !");
        } else if (call.method.equals("ActiveLookSDK#loadConfiguration")) {
            String assetPath = call.argument("assetPath");
            this.loadConfiguration(assetPath);
            result.success("assetPath !");
        } else if (call.method.equals("ActiveLookSDK#reconnectGlasses")) {
            String name = (String) call.argument("name");
            String uuid = (String) call.argument("uuid");
            String manufacturer = (String) call.argument("manufacturer");
            SerializedGlasses serializedGlasses =  new SerializedGlasses() {
                @Override
                public String getAddress() {
                    return uuid;
                }

                @Override
                public String getManufacturer() {
                    return manufacturer;
                }

                @Override
                public String getName() {
                    return name;
                }
            };
            this.reconnectGlasses(serializedGlasses);
            result.success("Connected !");
        } else if( call.method.equals("ActiveLookSDK#getSerializedGlasses")) {
            this.getSerializedGlasses();
            result.success("getSerializedGlasses !");
        } else if (call.method.equals("ActiveLookSDK#reconnectGlasses")) {
            SerializedGlasses serializedGlasses = call.argument("serializedGlasses");
            this.reconnectGlasses(serializedGlasses);
            result.success("reconnectGlasses !");
        }
        else if (call.method.equals("ActiveLookSDK#getBatteryLevel")) {
            this.getBatteryLevel();
            result.success("getBatteryLevel !");
        } else {
            result.notImplemented();
        }
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        channel.setMethodCallHandler(null);
        channelSubscription.setMethodCallHandler(null);
    }

    // Sdk methods calls
    private void initSdk(String token) {
        this.activelookSdk = Sdk.init(
                this.context,
                token,
                glassesUpdate -> invokeMethodOnUiThread("handleUpdateStart", new HashMap(), this.channelSubscription),
                glassesUpdate -> invokeMethodOnUiThread("handleUpdateAvailable", new HashMap(), this.channelSubscription),
                glassesUpdate -> invokeMethodOnUiThread("handleUpdateProgress", new HashMap(), this.channelSubscription),
                glassesUpdate -> invokeMethodOnUiThread("handleUpdateSuccess", new HashMap(), this.channelSubscription),
                glassesUpdate -> invokeMethodOnUiThread("handleUpdateError", new HashMap(), this.channelSubscription)
        );

        invokeMethodOnUiThread("handleSdkInitialized", new HashMap(), this.channel);
    }

    private void startScan() {
        System.out.println("Start scan");
        if (this.activelookSdk == null) {
            System.out.println("activelookSdk is not initialized null");
            return;
        }

        this.activelookSdk.startScan(dg -> {
            System.out.println("Start scan inside null");
            this.discoveredGlasses.add(dg);
            System.out.println("discoveredGlasses");
            HashMap<String, Object> args = new HashMap<>();
            args.put("uuid", dg.getAddress());
            args.put("name", dg.getName());
            System.out.println(dg.getAddress());
            System.out.println(dg.getName());
            invokeMethodOnUiThread("handleDiscoveredGlasses", args, this.channelSubscription);
            System.out.println("handleDiscoveredGlasses");
        });
        System.out.println("End of  scan inside null");
    }

    private void getBatteryLevel() {
        HashMap<String, Object> args = new HashMap<>();
        int batteryLevel = glasses.getDeviceInformation().getBatteryLevel();
        System.out.println("Battery level :" + batteryLevel);
        args.put("batteryLevel", batteryLevel);
        invokeMethodOnUiThread("handleBatteryLevel", args, this.channel);
    }

    private void connectGlasses(String name) {
        for (DiscoveredGlasses dg : this.discoveredGlasses) {
            if (dg.getName().equals(name)) {
                System.out.println("Connecting to glasses!");
                dg.connect(
                        glasses -> {
                            this.glasses = glasses;
                            this.storedGlasses =  new SerializedGlasses() {
                                @Override
                                public String getAddress() {
                                    return dg.getAddress();
                                }

                                @Override
                                public String getManufacturer() {
                                    return dg.getManufacturer();
                                }

                                @Override
                                public String getName() {
                                    return dg.getName();
                                }
                            };
                            invokeMethodOnUiThread("handleConnectedGlasses", new HashMap(), this.channelSubscription);
                        },
                        errorDiscoveredGlasses -> invokeMethodOnUiThread("handleConnectionFail", new HashMap(), this.channelSubscription),
                        glasses -> {
                            invokeMethodOnUiThread("handleDisconnectedGlasses", new HashMap(), this.channelSubscription);
                        }
                );

            }
        }
    }

    private void getSerializedGlasses() {
        HashMap<String, Object> args = new HashMap<>();
        args.put("uuid", storedGlasses.getAddress());
        args.put("name", storedGlasses.getName());
        args.put("manufacturer", storedGlasses.getManufacturer());
        invokeMethodOnUiThread("handleGetSerializedGlasses", args, this.channel);
    }

    private void reconnectGlasses(SerializedGlasses serializedGlasses){
        Sdk.getInstance().connect(serializedGlasses, glasses -> {
                    this.glasses = glasses;
                    this.storedGlasses =  serializedGlasses;
                    invokeMethodOnUiThread("handleConnectedGlasses", new HashMap(), this.channelSubscription);
                },
                errorDiscoveredGlasses -> invokeMethodOnUiThread("handleConnectionFail", new HashMap(), this.channelSubscription),
                glasses -> {
                    invokeMethodOnUiThread("handleDisconnectedGlasses", new HashMap(), this.channelSubscription);
                });
    }

    private void disconnectGlasses() {
        glasses.disconnect();
        invokeMethodOnUiThread("handleDisconnectGlasses", new HashMap(), this.channel);
    }

    // Utils
    private void log(String message) {
        Log.d("FROM_NATIVE", message);
    }

    private void loadConfiguration(String assetPath) {
        glasses.loadConfiguration(new BufferedReader(new InputStreamReader(getAssets().open("assetPath"))));
        invokeMethodOnUiThread("handleLoadConfiguration", new HashMap(), this.channel);
    }

    private void grey(int level) {
        glasses.grey((byte) level);
        invokeMethodOnUiThread("handleGrey", new HashMap(), this.channel);
    }

    private void shift(int x, int y) {
        glasses.shift((short) x, (short) y);
        invokeMethodOnUiThread("handleShift", new HashMap(), this.channel);
    }

    private void txt(short x, short y, Rotation rotation, byte font, byte color, String string) {
        glasses.txt(x, y, rotation, font, color, string);
        invokeMethodOnUiThread("handleTxt", new HashMap(), this.channel);
    }

    private void rectangleFull(short x, short y, short width, short height) {
        glasses.rectf(x, y, (short) (x + width), (short) (y + height));
        invokeMethodOnUiThread("handleRect", new HashMap(), this.channel);
    }

    private void polyline(byte thickness, short[] coordinates) {
        glasses.polyline(thickness, coordinates);
        invokeMethodOnUiThread("handlePolyline", new HashMap(), this.channel);
    }

    private void polylines(byte thickness, short[][] coordinates) {
        glasses.polylines(thickness, coordinates);
        invokeMethodOnUiThread("handlePolylines", new HashMap(), this.channel);
    }

    private void circle(short x, short y, byte radius) {
        glasses.circ(x, y, radius);
        invokeMethodOnUiThread("handleCircle", new HashMap(), this.channel);
    }

    private void luma(byte intensity) {
        glasses.luma(intensity);
        invokeMethodOnUiThread("handleLuma", new HashMap(), this.channel);
    }

    private void circlef(short x, short y, byte radius) {
        glasses.circf(x, y, radius);
        invokeMethodOnUiThread("handleCirclef", new HashMap(), this.channel);
    }

    private void clear() {
        glasses.clear();
        invokeMethodOnUiThread("handleClear", new HashMap(), this.channel);
    }

    private void holdAndFlush(int action) {
        if (action == 0) {
            glasses.holdFlush(holdFlushAction.HOLD);
            invokeMethodOnUiThread("handleHolded", new HashMap(), this.channel);
        } else {
            glasses.holdFlush(holdFlushAction.FLUSH);
            invokeMethodOnUiThread("handleFlushed", new HashMap(), this.channel);
        }
    }

    private void subscribeBatteryLevel() {
        glasses.subscribeToBatteryLevelNotifications( //Consumer<Integer> onEvent
                r -> {
                    HashMap<String, Object> args = new HashMap<>();
                    args.put("batteryLevel", r.toString());
                    invokeMethodOnUiThread("handleBatteryLevel", args, this.channelSubscription);
                    Log.d("Notif", "Battery: " + r.toString());
                }
        );
    }

    private void subscribeFlowControl() {
        glasses.subscribeToFlowControlNotifications( //Consumer<FlowControlStatus> onEvent
                r -> {
                    HashMap<String, Object> args = new HashMap<>();
                    args.put("flowControl", r.toString());
                    invokeMethodOnUiThread("handleFlowControl", args, this.channelSubscription);
                    Log.d("Notif", "Flow control: " + r.toString());
                });
    }

    private void subscribeToSensorInterfaceNotifications() {
        glasses.subscribeToFlowControlNotifications( //Consumer<FlowControlStatus> onEvent
                r -> {
                    invokeMethodOnUiThread("handleSensorInterfaceNotifications", new HashMap<>(), this.channelSubscription);
                    Log.d("Notif", "Sensor: Gesture!");
                });
    }

    private void color(byte value) {
        glasses.color(value);

        invokeMethodOnUiThread("handleColor", new HashMap(), this.channel);
    }

    private void runOnMainThread(final Runnable runnable) {
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            runnable.run();
        } else {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(runnable);
        }
    }

    void invokeMethodOnUiThread(final String methodName, final HashMap map, final MethodChannel _channel) {
        System.out.println(_channel.toString());
        runOnMainThread(new Runnable() {
            @Override
            public void run() {
                _channel.invokeMethod(methodName, map);
            }
        });
    }
}
