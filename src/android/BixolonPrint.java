package lu.post.cordova.plugins.bixolonprint;

import java.util.Map;
import java.util.HashMap;
//import java.util.Queue;
import java.util.Set;

import 	java.io.FileOutputStream;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.bixolon.android.library.BxlService;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;
import android.content.Context;

public class BixolonPrint extends CordovaPlugin {

    private static final String TAG = "BixolonPrint";

    BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    // Action to execute
    public static final String ACTION_PRINT_BITMAP_WITH_BASE64 = "printBitmapWithBase64";
    public static final String ACTION_PRINT_TEXT = "printText";
    public static final String ACTION_GET_STATUS = "getStatus";

    // Alignment string
    public static final String ALIGNMENT_LEFT = "LEFT";
    public static final String ALIGNMENT_CENTER = "CENTER";
    public static final String ALIGNMENT_RIGHT = "RIGHT";

    // Font string
    public static final String FONT_A = "A";
    public static final String FONT_B = "B";

    private CallbackContext cbContext;
    private Context ctx;
    private String lastActionName;
    private JSONArray lastActionArgs;
    private String actionSuccess;
    private String actionError;
    //private Queue<Integer> printerQueue;

    public boolean isValidAction;

    public String mConnectedDeviceName;
    public String mConnectedDeviceAddress;
    private JSONObject mConnectedDeviceStatus;
    public boolean mIsConnected;

    static BxlService mBxlService;
    //

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {

        this.ctx = this.cordova.getActivity().getApplicationContext();

        Log.d(TAG, "BixolonPrint.initialize_START");

        super.initialize(cordova, webView);

        mBxlService = new BxlService();
        this.mIsConnected = false;
        this.mConnectedDeviceName = null;
        this.mConnectedDeviceAddress = null;
        this.mConnectedDeviceStatus = null;

        this.actionSuccess = null;
        this.actionError = null;
        this.lastActionArgs = null;
        this.lastActionName = null;

        Log.d(TAG, "BixolonPrint.initialize_END");
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "BixolonPrint.onDestroy_START");
        super.onDestroy();
        mBxlService.Disconnect();
        mBxlService = null;
        Log.d(TAG, "BixolonPrint.onDestroy_END");
    }

    /**
     * Executes the request and returns PluginResult
     *
     * @param action          Action to execute
     * @param data            JSONArray of arguments to the plugin
     * @param callbackContext The callback context used when calling back into JavaScript.
     * @return A PluginRequest object with a status
     */
    @Override
    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) {
        Log.d(TAG, "BixolonPrint.execute_START");

        this.isValidAction = true;
        this.cbContext = callbackContext;
        this.lastActionName = action;
        this.lastActionArgs = args;

        Log.i(TAG, "action: " + action);

        if (ACTION_PRINT_TEXT.equals(action)) {
            JSONObject printConfig = args.optJSONObject(1);
        } else if (ACTION_PRINT_BITMAP_WITH_BASE64.equals(action)) {
            JSONObject printConfig = args.optJSONObject(1);
        } else if (ACTION_GET_STATUS.equals(action)) {
            JSONObject printConfig = args.optJSONObject(1);
        } else {
            this.isValidAction = false;
            this.cbContext.error("Invalid Action");
            Log.d(TAG, "Invalid action : " + action + " passed");
        }

        if (this.isValidAction) {
            this.connect();
        }

        //
        Log.d(TAG, "BixolonPrint.execute_END");
        return this.isValidAction;
    }

    private void connect() {
        Log.d(TAG, "BixolonPrint.connect_START");

        if (this.mIsConnected) {
            Log.d(TAG, "ok");
            this.onConnect();
        } else {

            try {

                Log.d(TAG, "ok");

                Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
                final String[] itemsAddr = new String[pairedDevices.size()];
                final String[] itemsName = new String[pairedDevices.size()];

                if (pairedDevices.size() > 0) {
                    // There are paired devices. Get the name and address of each paired device.
                    int index = 0;
                    for (BluetoothDevice device : pairedDevices) {
                        itemsAddr[index] = device.getAddress();
                        itemsName[index] = device.getName();
                        index++;
                    }

                    mConnectedDeviceAddress = itemsAddr[0];

                    Log.d(TAG, mConnectedDeviceAddress);

                    if (mBxlService.Connect(mConnectedDeviceAddress) == 0) {
                        this.onConnect();
                    }else{
                        this.onDisconnect();
                    }
                }

            }catch(Exception e){
                Log.d(TAG, e.getMessage());
            }
        }

        Log.d(TAG, "BixolonPrint.connect_END");
    }

    private void onConnect() {
        Log.d(TAG, "BixolonPrint.onConnect_START");

        this.mIsConnected = true;

        if (ACTION_PRINT_TEXT.equals(this.lastActionName)) {
            this.printText();
        } else if (ACTION_PRINT_BITMAP_WITH_BASE64.equals(this.lastActionName)) {
             this.printBitmapWithBase64();
        }

        Log.d(TAG, "BixolonPrint.onConnect_END");
    }

    private void disconnect() {
        Log.d(TAG, "BixolonPrint.disconnect_START");
        if(mBxlService.Disconnect() == 0){
            this.onDisconnect();
        }
        Log.d(TAG, "BixolonPrint.disconnect_END");
    }

    private void onDisconnect() {
        Log.d(TAG, "BixolonPrint.onDisconnect_START");

        String action = this.lastActionName;
        String error = this.actionError;
        String success = this.actionSuccess;
        JSONObject status = this.mConnectedDeviceStatus;

        if (!this.mIsConnected && this.isValidAction) {
            this.cbContext.error("Connection failed");
            return;
        }

        this.mIsConnected = false;
        this.mConnectedDeviceName = null;
        this.mConnectedDeviceAddress = null;
        this.mConnectedDeviceStatus = null;

        this.actionSuccess = null;
        this.actionError = null;
        this.lastActionArgs = null;
        this.lastActionName = null;

        if (error != null) {
            Log.d(TAG, "End with error");
            Log.d(TAG, error);
            this.cbContext.error(error);
        } else {
            if (ACTION_GET_STATUS.equals(action)) {
                this.cbContext.success(status);
            } else {
                this.cbContext.success(success);
            }
        }

        Log.d(TAG, "BixolonPrint.onDisconnect_END");
    }

    private void onPrintComplete() {
        Log.d(TAG, "BixolonPrint.onPrintComplete_START");
        this.actionSuccess = "print success";
        this.disconnect();
        Log.d(TAG, "BixolonPrint.onPrintComplete_END");
    }

    private void printText() {
        Log.d(TAG, "BixolonPrint.printText_START");

        JSONArray textLines;
        JSONObject printConfig;
        int lineFeed;
        int codePage;

        try {
            textLines = this.lastActionArgs.getJSONArray(0);
            printConfig = this.lastActionArgs.getJSONObject(1);
            lineFeed = printConfig.getInt("lineFeed");
            codePage = printConfig.getInt("codePage");
        } catch (JSONException e1) {
            this.isValidAction = false;
            this.actionError = "print error: " + e1.getMessage();
            this.disconnect();
            return;
        }

        String text;
        String align;
        String fontType;
        String fontStyle;
        int height;
        int width;

        int textAlignment;
        int textAttribute;
        int textSize;

        mBxlService.SetCharacterSet(codePage);

        JSONObject textLine;
        int arlength = textLines.length();

        for (int i = 0; i < arlength; i++) {
            try {

                Log.d(TAG, "BixolonPrint.printText: line:" + (i + 1) + " of " + arlength);

                textLine = textLines.getJSONObject(i);
                text = textLine.optString("text");
                align = textLine.optString("textAlign");
                width = textLine.optInt("textWidth");
                height = textLine.optInt("textHeight");
                fontType = textLine.optString("fontType");
                fontStyle = textLine.optString("fontStyle");

                textAlignment = this.getAlignment(align);
                textAttribute = this.getAttribute(fontType, fontStyle);
                textSize = this.getTextSize(width, height);

                mBxlService.PrintText(text + "\r\n", textAlignment, textAttribute, textSize);

            } catch (JSONException e2) {
                this.isValidAction = false;
                this.actionError = "print error: " + e2.getMessage();
                this.disconnect();
                return;
            }
        }


        mBxlService.LineFeed(lineFeed);
        Log.d(TAG, "BixolonPrint.printText_END");

        this.onPrintComplete();
    }

    private void printBitmapWithBase64() {

        Log.d(TAG, "BixolonPrint.printImage_START");

        FileOutputStream fos = null;

        try {

            JSONObject printConfig;
            boolean formFeed;
            int lineFeed;

            printConfig = this.lastActionArgs.getJSONObject(1);
            formFeed = printConfig.getBoolean("formFeed");
            lineFeed = printConfig.getInt("lineFeed");

            String base64EncodedData = this.lastActionArgs.getString(0);

            if (base64EncodedData != null) {

                Log.d(TAG, this.ctx.getFilesDir().toString());

                fos = this.ctx.openFileOutput("toPrint.png", Context.MODE_PRIVATE);
                byte[] decodedString = android.util.Base64.decode(base64EncodedData, android.util.Base64.DEFAULT);
                fos.write(decodedString);
                fos.flush();
                fos.close();
            }

            mBxlService.PrintImage(this.ctx.getFilesDir().toString()+"/toPrint.png", -1, 1, 50);
            mBxlService.LineFeed(lineFeed);

            Log.d(TAG, "BixolonPrint.printImage_END");

            this.onPrintComplete();

        } catch (Exception e) {
            this.isValidAction = false;
            this.actionError = "print error: " + e.getMessage();
            this.disconnect();
            return;
        } finally {
            if (fos != null) {
                fos = null;
            }
        }
    }

    /**
     * base 64로 encoding 된 image data을 받아서 bitmap을 생성하고 리턴합니다
     *
     * @param base64EncodedData
     *            얻고자 하는 image url
     * @return 생성된 bitmap
     */
    private Bitmap getDecodedBitmap(String base64EncodedData) {

        byte[] imageAtBytes = Base64.decode(base64EncodedData.getBytes(), Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(imageAtBytes, 0, imageAtBytes.length);
    }

     /*         METODI ACCESSORI
     ---------------------------------------*/

    /**
     * @param fontType
     * @param fontStyle
     * @return
     */
    @SuppressLint("DefaultLocale")
    private int getAttribute(String fontType, String fontStyle) {
        // setting attribute
        int attribute = 0;

        if (fontType != null) {
            if (fontType.toUpperCase().equals("A")) {
                attribute |= mBxlService.BXL_FT_DEFAULT;
            }
            if (fontType.toUpperCase().equals("B")) {
                attribute |= mBxlService.BXL_FT_FONTB;
            }
        }

        // TODO add multiple selection
        if (fontStyle != null) {
            if (fontStyle.toUpperCase().equals("UNDERLINE")) {
                attribute |= mBxlService.BXL_FT_UNDERLINE;
            }

            if (fontStyle.toUpperCase().equals("BOLD")) {
                attribute |= mBxlService.BXL_FT_BOLD;
            }

            if (fontStyle.toUpperCase().equals("REVERSE")) {
                attribute |= mBxlService.BXL_FT_REVERSE;
            }
        }

        return attribute;
    }

    /**
     * @param align
     * @return
     */
    @SuppressLint("DefaultLocale")
    private int getAlignment(String align) {
        int alignment = mBxlService.BXL_ALIGNMENT_LEFT;
        if (align != null) {
            if (ALIGNMENT_LEFT.equals(align.toUpperCase())) {
                alignment = mBxlService.BXL_ALIGNMENT_LEFT;
            } else if (ALIGNMENT_CENTER.equals(align.toUpperCase())) {
                alignment = mBxlService.BXL_ALIGNMENT_CENTER;
            } else if (ALIGNMENT_RIGHT.equals(align.toUpperCase())) {
                alignment = mBxlService.BXL_ALIGNMENT_RIGHT;
            }
        }

        return alignment;
    }

    /**
     * @param width
     * @param height
     * @return
     */
    private int getTextSize(int width, int height) {
        int size = 0;

        switch (width) {
        case 0:
            size = mBxlService.BXL_TS_0WIDTH;
            break;
        case 1:
            size = mBxlService.BXL_TS_1WIDTH;
            break;
        case 2:
            size = mBxlService.BXL_TS_2WIDTH;
            break;
        case 3:
            size = mBxlService.BXL_TS_3WIDTH;
            break;
        case 4:
            size = mBxlService.BXL_TS_4WIDTH;
            break;
        case 5:
            size = mBxlService.BXL_TS_5WIDTH;
            break;
        case 6:
            size = mBxlService.BXL_TS_6WIDTH;
            break;
        case 7:
            size = mBxlService.BXL_TS_7WIDTH;
            break;
        }

        switch (height) {
        case 0:
            size |= mBxlService.BXL_TS_0HEIGHT;
            break;
        case 1:
            size |= mBxlService.BXL_TS_1HEIGHT;
            break;
        case 2:
            size |= mBxlService.BXL_TS_2HEIGHT;
            break;
        case 3:
            size |= mBxlService.BXL_TS_3HEIGHT;
            break;
        case 4:
            size |= mBxlService.BXL_TS_4HEIGHT;
            break;
        case 5:
            size |= mBxlService.BXL_TS_5HEIGHT;
            break;
        case 6:
            size |= mBxlService.BXL_TS_6HEIGHT;
            break;
        case 7:
            size |= mBxlService.BXL_TS_7HEIGHT;
            break;
        }

        return size;
    }

    private Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			Log.d(TAG, String.valueOf(msg.what));
			if (msg.what == BxlService.BXL_STS_COVEROPEN) {
                Log.d(TAG, "Cover is open.");
//			} else if (msg.what == BxlService.BXL_STS_DRAWER_LOW) {
//				buffer.append("Drawer kick-out connector pin 3 is LOW.\n");
//			} else if (msg.what == BxlService.BXL_STS_DRAWER_HIGH) {
//				buffer.append("Drawer kick-out connector pin 3 is HIGH.\n");
			} else if (msg.what == BxlService.BXL_STS_MECHANICAL_ERROR) {
                Log.d(TAG, "Mechanical error.");
			} else if (msg.what == BxlService.BXL_STS_AUTO_CUTTER_ERROR) {
                Log.d(TAG, "Auto cutter error occurred.");
			} else if (msg.what == BxlService.BXL_STS_ERROR) {
                Log.d(TAG, "Unrecoverable error.");
			} else if (msg.what == BxlService.BXL_STS_PAPER_NEAR_END) {
                Log.d(TAG, "Paper near end sensor: paper near end.");
			} else if (msg.what == BxlService.BXL_STS_NO_PAPER) {
                Log.d(TAG, "Paper end sensor: no paper present.");
			}
		}
	};
}
