import java.util.Map;
import java.util.HashMap;
//import java.util.Queue;
import java.util.Set;

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
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

public class BixolonPrint extends CordovaPlugin {

    private static final String TAG = "BixolonPrint";

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
            this.onConnect();
        }else if (mBxlService.Connect() == 0) {
            this.onConnect();
        }
        Log.d(TAG, "BixolonPrint.connect_END");
    }

    private void onConnect() {
        Log.d(TAG, "BixolonPrint.onConnect_START");

        this.mIsConnected = true;

        if (ACTION_PRINT_TEXT.equals(this.lastActionName)) {
            this.printText();
        }
        // } else if (ACTION_PRINT_BITMAP_WITH_BASE64.equals(this.lastActionName)) {
        //     this.printBitmapWithBase64();
        // } else if (ACTION_GET_STATUS.equals(this.lastActionName)) {
        //     this.getStatus();
        // }

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
            this.cbContext.error("Connection failed DEB");
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
}
