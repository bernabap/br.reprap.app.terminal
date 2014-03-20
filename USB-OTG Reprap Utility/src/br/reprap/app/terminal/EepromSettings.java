/*
 * Android USB Serial Monitor Lite
 * 
 * Copyright (C) 2012 Keisuke SUZUKI
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * thanks to Arun.
 * 
 * USB-OTG RepRap Utility
 * 
 * Copyright (C) 2014 Bernardo Baptista
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package br.reprap.app.terminal;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import br.reprap.app.terminal.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnKeyListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.physicaloid.lib.Physicaloid;
import com.physicaloid.lib.usb.driver.uart.UartConfig;

public class EepromSettings extends Activity {
	static final int PREFERENCE_REV_NUM = 1;

	// debug settings
	private static final boolean SHOW_DEBUG = false;

	public static final boolean isICSorHigher = (Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB_MR2);

	// occurs USB packet loss if TEXT_MAX_SIZE is over 6000
	private static final int TEXT_MAX_SIZE = 8192;

	private static final int DIALOG_SAVE = 0;
	private static final int DIALOG_RESTORE = 1;

	// Defines of Display Settings
	private static final int DISP_CHAR = 0;
	private static final int DISP_DEC = 1;
	private static final int DISP_HEX = 2;

	// Linefeed Code Settings
	private static final int LINEFEED_CODE_CR = 0;
	private static final int LINEFEED_CODE_CRLF = 1;
	private static final int LINEFEED_CODE_LF = 2;

	// Load Bundle Key (for view switching)
	private static final String BUNDLEKEY_LOADTEXTVIEW = "bundlekey.LoadTextView";

	Physicaloid mSerial1;

	private TextView mtvSerial1;
	private StringBuilder mText = new StringBuilder();
	private boolean mStop = false;

	String TAG = "AndroidSerialTerminal";

	Handler mHandler = new Handler();

	// Buttons
	private Button btReload;
	private Button btRestore;
	private Button btSave;
	private EditText etWrite1;
	private EditText StepsPerMm;
	private EditText MaxFeedrate;
	private EditText MaxAcceleration;
	private EditText Acceleration;
	private EditText PidSettings;
	private EditText HomingOffset;
	private EditText AdvancedVars;

	// Default settings
	private int mTextFontSize = 12;
	private Typeface mTextTypeface = Typeface.MONOSPACE;
	private int mDisplayType = DISP_CHAR;
	private int mReadLinefeedCode = LINEFEED_CODE_LF;
	private int mWriteLinefeedCode = LINEFEED_CODE_LF;
	private int mBaudrate = 9600;
	private int mDataBits = UartConfig.DATA_BITS8;
	private int mParity = UartConfig.PARITY_NONE;
	private int mStopBits = UartConfig.STOP_BITS1;
	private int mFlowControl = UartConfig.FLOW_CONTROL_OFF;

	private boolean mRunningMainLoop = false;

	private static final String ACTION_USB_PERMISSION = "jp.ksksue.app.terminal.USB_PERMISSION";

	// Linefeed
	private final static String BR = System.getProperty("line.separator");

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Hide keyboard
		getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

		/*
		 * FIXME : How to check that there is a title bar menu or not. // Should
		 * not set a Window.FEATURE_NO_TITLE on Honeycomb because a user cannot
		 * see menu button. if(isICSorHigher) {
		 * if(!getWindow().hasFeature(Window.FEATURE_ACTION_BAR)) {
		 * requestWindowFeature(Window.FEATURE_NO_TITLE); } }
		 */
		setContentView(R.layout.eeprom_layout);

		mtvSerial1 = (TextView) findViewById(R.id.tvSerial1);
		btReload = (Button) findViewById(R.id.btReload);
		btReload.setEnabled(false);
		btRestore = (Button) findViewById(R.id.btRestore);
		btRestore.setEnabled(false);
		btSave = (Button) findViewById(R.id.btSave);
		btSave.setEnabled(false);
		etWrite1 = (EditText) findViewById(R.id.etWrite1);
		etWrite1.setEnabled(false);
		etWrite1.setHint("CR : \\r, LF : \\n, bin : \\u0000");
		StepsPerMm = (EditText) findViewById(R.id.txStepsPermm);
		MaxFeedrate = (EditText) findViewById(R.id.txMaxFeedrate);
		MaxAcceleration = (EditText) findViewById(R.id.txMaxAcceleration);
		Acceleration = (EditText) findViewById(R.id.txAcceleration);
		PidSettings = (EditText) findViewById(R.id.txPidSettings);
		HomingOffset = (EditText) findViewById(R.id.txHomingOffset);
		AdvancedVars = (EditText) findViewById(R.id.txAdvancedVars);

		if (SHOW_DEBUG) {
			Log.d(TAG, "New FTDriver");
		}

		// get service
		mSerial1 = new Physicaloid(this);

		if (SHOW_DEBUG) {
			Log.d(TAG, "New instance : " + mSerial1);
		}
		// listen for new devices
		IntentFilter filter = new IntentFilter();
		filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
		filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
		registerReceiver(mUsbReceiver, filter);

		if (SHOW_DEBUG) {
			Log.d(TAG, "FTDriver beginning");
		}

		openUsbSerial();

		etWrite1.setOnKeyListener(new OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (event.getAction() == KeyEvent.ACTION_UP
						&& keyCode == KeyEvent.KEYCODE_ENTER) {
					writeDataToSerial();
					return true;
				}
				return false;
			}
		});
		// ---------------------------------------------------------------------------------------
		// Reload Button
		// ---------------------------------------------------------------------------------------
		btReload.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mtvSerial1.setText("");
				mText.setLength(0);

				String str1 = "M501";
				str1 = changeEscapeSequence(str1);
				mSerial1.write(str1.getBytes(), str1.length());

				final Handler handler = new Handler();
				handler.postDelayed(new Runnable() {
					@Override
					public void run() {
						regexEeprom();
					}
				}, 1000);
			}
		});

		// Restore Button
		btRestore.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				showDialog(DIALOG_RESTORE);
			}
		});

		// Save Button
		btSave.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				showDialog(DIALOG_SAVE);
			}
		});
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_RESTORE:

			return new AlertDialog.Builder(EepromSettings.this)
					.setIcon(R.drawable.eprom)
					.setTitle("Restore EEPROM?")
					.setMessage("This will restore your eeprom values!")

					.setPositiveButton("Restore",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									restoreEeprom();
									Toast.makeText(getApplicationContext(),
											"eeprom restored",
											Toast.LENGTH_SHORT).show();
								}
							})
					.setNegativeButton("Cancel",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									dialog.cancel();
								}
							}).create();

		case DIALOG_SAVE:

			return new AlertDialog.Builder(EepromSettings.this)
					.setIcon(R.drawable.eprom)
					.setTitle("Confirm Save?")
					.setMessage("This will replace your eeprom values!")
					.setPositiveButton("Save",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									saveEeprom();
									Toast.makeText(getApplicationContext(),
											"saved to eeprom",
											Toast.LENGTH_SHORT).show();
								}
							})
					.setNegativeButton("Cancel",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									dialog.cancel();
								}
							}).create();
		}
		return null;
	}

	public void restoreEeprom() {
		String str1 = "M502";
		str1 = changeEscapeSequence(str1);
		mSerial1.write(str1.getBytes(), str1.length());
		str1 = "M500";
		str1 = changeEscapeSequence(str1);
		mSerial1.write(str1.getBytes(), str1.length());
		mtvSerial1.setText("");
		mText.setLength(0);
	}

	private void regexEeprom() {
		String strEeprom = mtvSerial1.getText().toString();

		Pattern stepspermm = Pattern.compile("(?<=M92)\\s(\\S+\\s){4}(?=echo)");
		Matcher stepspermm_match = stepspermm.matcher(strEeprom);
		if (stepspermm_match.find()) {
			String strData = stepspermm_match.group().toString();
			strData = strData.substring(1);
			StepsPerMm.setText(getString(R.string.steps_permm, strData));
		} else {
			StepsPerMm.setText(getString(R.string.steps_permm, "reload again"));
		}

		Pattern maxacceleration = Pattern
				.compile("(?<=M201)\\s(\\S+\\s){4}(?=echo)");
		Matcher maxacceleration_match = maxacceleration.matcher(strEeprom);
		if (maxacceleration_match.find()) {
			String strData = maxacceleration_match.group().toString();
			strData = strData.substring(1);
			MaxAcceleration.setText(getString(R.string.max_acceleration,
					strData));
		} else {
			MaxAcceleration.setText(getString(R.string.max_acceleration,
					"reload again"));
		}

		Pattern maxfeedrate = Pattern
				.compile("(?<=M203)\\s(\\S+\\s){4}(?=echo)");
		Matcher maxfeedrate_match = maxfeedrate.matcher(strEeprom);
		if (maxfeedrate_match.find()) {
			String strData = maxfeedrate_match.group().toString();
			strData = strData.substring(1);
			MaxFeedrate.setText(getString(R.string.max_feedrate, strData));
		} else {
			MaxFeedrate
					.setText(getString(R.string.max_feedrate, "reload again"));
		}

		Pattern acceleration = Pattern
				.compile("(?<=M204)\\s(\\S+\\s){2}(?=echo)");
		Matcher acceleration_match = acceleration.matcher(strEeprom);
		if (acceleration_match.find()) {
			String strData = acceleration_match.group().toString();
			strData = strData.substring(1);
			Acceleration.setText(getString(R.string.acceleration, strData));
		} else {
			Acceleration.setText(getString(R.string.acceleration,
					"reload again"));
		}

		Pattern advancedvars = Pattern
				.compile("(?<=M205)\\s(\\S+\\s){6}(?=echo)");
		Matcher advancedvars_match = advancedvars.matcher(strEeprom);
		if (advancedvars_match.find()) {
			String strData = advancedvars_match.group().toString();
			strData = strData.substring(1);
			AdvancedVars.setText(getString(R.string.advanced_vars, strData));
		} else {
			AdvancedVars.setText(getString(R.string.advanced_vars,
					"reload again"));
		}

		Pattern homeoffset = Pattern
				.compile("(?<=M206)\\s(\\S+\\s){3}(?=echo)");
		Matcher homeoffset_match = homeoffset.matcher(strEeprom);
		;
		if (homeoffset_match.find()) {
			String strData = homeoffset_match.group().toString();
			strData = strData.substring(1);
			HomingOffset.setText(getString(R.string.homing_offset, strData));
		} else {
			HomingOffset.setText(getString(R.string.homing_offset,
					"reload again"));
		}

		Pattern pidsettings = Pattern.compile("(?<=M301)\\s(\\S+\\s){3}(?=ok)");
		Matcher pidsettings_match = pidsettings.matcher(strEeprom);
		if (pidsettings_match.find()) {
			String strData = pidsettings_match.group().toString();
			strData = strData.substring(1);
			PidSettings.setText(getString(R.string.pid_settings, strData));
		} else {
			PidSettings
					.setText(getString(R.string.pid_settings, "reload again"));
		}

	}

	public void saveEeprom() {
		mtvSerial1.setText("");
		mText.setLength(0);

		String strCode = StepsPerMm.getText().toString();
		strCode = "M92 " + strCode;
		strCode = changeEscapeSequence(strCode);
		mSerial1.write(strCode.getBytes(), strCode.length());

		strCode = MaxAcceleration.getText().toString();
		strCode = "M201 " + strCode;
		strCode = changeEscapeSequence(strCode);
		mSerial1.write(strCode.getBytes(), strCode.length());

		strCode = MaxFeedrate.getText().toString();
		strCode = "M203 " + strCode;
		strCode = changeEscapeSequence(strCode);
		mSerial1.write(strCode.getBytes(), strCode.length());

		strCode = Acceleration.getText().toString();
		strCode = "M204 " + strCode;
		strCode = changeEscapeSequence(strCode);
		mSerial1.write(strCode.getBytes(), strCode.length());

		strCode = AdvancedVars.getText().toString();
		strCode = "M205 " + strCode;
		strCode = changeEscapeSequence(strCode);
		mSerial1.write(strCode.getBytes(), strCode.length());

		strCode = HomingOffset.getText().toString();
		strCode = "M206 " + strCode;
		strCode = changeEscapeSequence(strCode);
		mSerial1.write(strCode.getBytes(), strCode.length());

		strCode = PidSettings.getText().toString();
		strCode = "M301 " + strCode;
		strCode = changeEscapeSequence(strCode);
		mSerial1.write(strCode.getBytes(), strCode.length());

		strCode = "M500 ";
		strCode = changeEscapeSequence(strCode);
		mSerial1.write(strCode.getBytes(), strCode.length());

		mtvSerial1.setText("");
		mText.setLength(0);
	}

	private void writeDataToSerial() {
		String strWrite = etWrite1.getText().toString();
		strWrite = changeEscapeSequence(strWrite);
		if (SHOW_DEBUG) {
			Log.d(TAG, "FTDriver Write(" + strWrite.length() + ") : "
					+ strWrite);
		}
		mSerial1.write(strWrite.getBytes(), strWrite.length());
	}

	private String changeEscapeSequence(String in) {
		String out = new String();
		try {
			out = unescapeJava(in);
		} catch (IOException e) {
			return "";
		}
		switch (mWriteLinefeedCode) {
		case LINEFEED_CODE_CR:
			out = out + "\r";
			break;
		case LINEFEED_CODE_CRLF:
			out = out + "\r\n";
			break;
		case LINEFEED_CODE_LF:
			out = out + "\n";
			break;
		default:
		}
		return out;
	}

	/**
	 * Saves values for view switching
	 */
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(BUNDLEKEY_LOADTEXTVIEW, mtvSerial1.getText()
				.toString());
	}

	/**
	 * Loads values for view switching
	 */

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		mtvSerial1
				.setText(savedInstanceState.getString(BUNDLEKEY_LOADTEXTVIEW));
	}

	@Override
	public void onDestroy() {
		mSerial1.close();
		closeUsbSerial();
		mStop = true;
		unregisterReceiver(mUsbReceiver);
		super.onDestroy();
	}

	private void mainloop() {
		mStop = false;
		mRunningMainLoop = true;
		btReload.setEnabled(true);
		btRestore.setEnabled(true);
		btSave.setEnabled(true);
		etWrite1.setEnabled(true);
		// Toast.makeText(this, "connected", Toast.LENGTH_SHORT).show();
		if (SHOW_DEBUG) {
			Log.d(TAG, "start mainloop");
		}
		new Thread(mLoop).start();
	}

	private Runnable mLoop = new Runnable() {
		@Override
		public void run() {
			int len;
			byte[] rbuf = new byte[4096];

			for (;;) {// this is the main loop for transferring

				// ////////////////////////////////////////////////////////
				// Read and Display to Terminal
				// ////////////////////////////////////////////////////////
				len = mSerial1.read(rbuf);
				rbuf[len] = 0;

				if (len > 0) {
					if (SHOW_DEBUG) {
						Log.d(TAG, "Read  Length : " + len);
					}

					switch (mDisplayType) {
					case DISP_CHAR:
						setSerialDataToTextView(mDisplayType, rbuf, len, "", "");
						break;
					case DISP_DEC:
						setSerialDataToTextView(mDisplayType, rbuf, len, "013",
								"010");
						break;
					case DISP_HEX:
						setSerialDataToTextView(mDisplayType, rbuf, len, "0d",
								"0a");
						break;
					}

					mHandler.post(new Runnable() {
						public void run() {
							if (mtvSerial1.length() > TEXT_MAX_SIZE) {
								StringBuilder sb = new StringBuilder();
								sb.append(mtvSerial1.getText());
								sb.delete(0, TEXT_MAX_SIZE / 2);
								mtvSerial1.setText(sb);
							}
							mtvSerial1.append(mText);
							mText.setLength(0);
							// msvText1.fullScroll(ScrollView.FOCUS_DOWN);
						}
					});
				}

				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				if (mStop) {
					mRunningMainLoop = false;
					return;
				}
			}
		}
	};

	private String IntToHex2(int Value) {
		char HEX2[] = { Character.forDigit((Value >> 4) & 0x0F, 16),
				Character.forDigit(Value & 0x0F, 16) };
		String Hex2Str = new String(HEX2);
		return Hex2Str;
	}

	boolean lastDataIs0x0D = false;

	void setSerialDataToTextView(int disp, byte[] rbuf, int len, String sCr,
			String sLf) {
		int tmpbuf;
		for (int i = 0; i < len; ++i) {
			if (SHOW_DEBUG) {
				Log.d(TAG, "Read  Data[" + i + "] : " + rbuf[i]);
			}

			// "\r":CR(0x0D) "\n":LF(0x0A)
			if ((mReadLinefeedCode == LINEFEED_CODE_CR) && (rbuf[i] == 0x0D)) {
				mText.append(sCr);
				mText.append(BR);
			} else if ((mReadLinefeedCode == LINEFEED_CODE_LF)
					&& (rbuf[i] == 0x0A)) {
				mText.append(sLf);
				mText.append(BR);
			} else if ((mReadLinefeedCode == LINEFEED_CODE_CRLF)
					&& (rbuf[i] == 0x0D) && (rbuf[i + 1] == 0x0A)) {
				mText.append(sCr);
				if (disp != DISP_CHAR) {
					mText.append(" ");
				}
				mText.append(sLf);
				mText.append(BR);
				++i;
			} else if ((mReadLinefeedCode == LINEFEED_CODE_CRLF)
					&& (rbuf[i] == 0x0D)) {
				// case of rbuf[last] == 0x0D and rbuf[0] == 0x0A
				mText.append(sCr);
				lastDataIs0x0D = true;
			} else if (lastDataIs0x0D && (rbuf[0] == 0x0A)) {
				if (disp != DISP_CHAR) {
					mText.append(" ");
				}
				mText.append(sLf);
				mText.append(BR);
				lastDataIs0x0D = false;
			} else if (lastDataIs0x0D && (i != 0)) {
				// only disable flag
				lastDataIs0x0D = false;
				--i;
			} else {
				switch (disp) {
				case DISP_CHAR:
					mText.append((char) rbuf[i]);
					break;
				case DISP_DEC:
					tmpbuf = rbuf[i];
					if (tmpbuf < 0) {
						tmpbuf += 256;
					}
					mText.append(String.format("%1$03d", tmpbuf));
					mText.append(" ");
					break;
				case DISP_HEX:
					mText.append(IntToHex2((int) rbuf[i]));
					mText.append(" ");
					break;
				default:
					break;
				}
			}
		}
	}

	void loadDefaultSettingValues() {
		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(this);
		String res = pref
				.getString("display_list", Integer.toString(DISP_CHAR));
		mDisplayType = Integer.valueOf(res);

		res = pref.getString("fontsize_list", Integer.toString(12));
		mTextFontSize = Integer.valueOf(res);

		res = pref.getString("typeface_list", Integer.toString(3));
		switch (Integer.valueOf(res)) {
		case 0:
			mTextTypeface = Typeface.DEFAULT;
			break;
		case 1:
			mTextTypeface = Typeface.SANS_SERIF;
			break;
		case 2:
			mTextTypeface = Typeface.SERIF;
			break;
		case 3:
			mTextTypeface = Typeface.MONOSPACE;
			break;
		}
		mtvSerial1.setTypeface(mTextTypeface);
		etWrite1.setTypeface(mTextTypeface);

		res = pref.getString("baudrate_list", Integer.toString(250000));
		mBaudrate = Integer.valueOf(res);

		res = pref.getString("databits_list",
				Integer.toString(UartConfig.DATA_BITS8));
		mDataBits = Integer.valueOf(res);

		res = pref.getString("parity_list",
				Integer.toString(UartConfig.PARITY_NONE));
		mParity = Integer.valueOf(res);

		res = pref.getString("stopbits_list",
				Integer.toString(UartConfig.STOP_BITS1));
		mStopBits = Integer.valueOf(res);

		res = pref.getString("flowcontrol_list",
				Integer.toString(UartConfig.FLOW_CONTROL_OFF));
		mFlowControl = Integer.valueOf(res);
	}

	// Load default baud rate
	int loadDefaultBaudrate() {
		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(this);
		String res = pref.getString("baudrate_list", Integer.toString(250000));
		return Integer.valueOf(res);
	}

	private void openUsbSerial() {
		if (mSerial1 == null) {
			Toast.makeText(this, "cannot open", Toast.LENGTH_SHORT).show();
			return;
		}

		if (!mSerial1.isOpened()) {
			if (SHOW_DEBUG) {
				Log.d(TAG, "onNewIntent begin");
			}
			if (!mSerial1.open()) {
				Toast.makeText(this, "cannot open", Toast.LENGTH_SHORT).show();
				return;
			} else {
				loadDefaultSettingValues();

				boolean dtrOn = false;
				boolean rtsOn = false;
				if (mFlowControl == UartConfig.FLOW_CONTROL_ON) {
					dtrOn = true;
					rtsOn = true;
				}
				mSerial1.setConfig(new UartConfig(mBaudrate, mDataBits,
						mStopBits, mParity, dtrOn, rtsOn));

				if (SHOW_DEBUG) {
					Log.d(TAG, "setConfig : baud : " + mBaudrate
							+ ", DataBits : " + mDataBits + ", StopBits : "
							+ mStopBits + ", Parity : " + mParity + ", dtr : "
							+ dtrOn + ", rts : " + rtsOn);
				}

				mtvSerial1.setTextSize(mTextFontSize);

				// Toast.makeText(this, "connected", Toast.LENGTH_SHORT).show();
			}
		}

		if (!mRunningMainLoop) {
			mainloop();
		}

	}

	private void closeUsbSerial() {
		detachedUi();
		mStop = true;
		mSerial1.close();
	}

	protected void onNewIntent(Intent intent) {
		if (SHOW_DEBUG) {
			Log.d(TAG, "onNewIntent");
		}

		openUsbSerial();
	};

	private void detachedUi() {
		btReload.setEnabled(false);
		btRestore.setEnabled(false);
		btSave.setEnabled(false);
		etWrite1.setEnabled(false);
		// Toast.makeText(this, "disconnected", Toast.LENGTH_SHORT).show();
	}

	// BroadcastReceiver when insert/remove the device USB plug into/from a USB
	// port
	BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();

			if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
				if (SHOW_DEBUG) {
					Log.d(TAG, "Device attached");
				}
				if (!mSerial1.isOpened()) {
					if (SHOW_DEBUG) {
						Log.d(TAG, "Device attached begin");
					}
					openUsbSerial();
				}
				if (!mRunningMainLoop) {
					if (SHOW_DEBUG) {
						Log.d(TAG, "Device attached mainloop");
					}
					mainloop();
				}
			} else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
				if (SHOW_DEBUG) {
					Log.d(TAG, "Device detached");
				}
				mStop = true;
				detachedUi();
				// mSerial1.usbDetached(intent);
				mSerial1.close();
			} else if (ACTION_USB_PERMISSION.equals(action)) {
				if (SHOW_DEBUG) {
					Log.d(TAG, "Request permission");
				}
				synchronized (this) {
					if (!mSerial1.isOpened()) {
						if (SHOW_DEBUG) {
							Log.d(TAG, "Request permission begin");
						}
						openUsbSerial();
					}
				}
				if (!mRunningMainLoop) {
					if (SHOW_DEBUG) {
						Log.d(TAG, "Request permission mainloop");
					}
					mainloop();
				}
			}
		}
	};

	/**
	 * <p>
	 * Unescapes any Java literals found in the <code>String</code> to a
	 * <code>Writer</code>.
	 * </p>
	 * 
	 * <p>
	 * For example, it will turn a sequence of <code>'\'</code> and
	 * <code>'n'</code> into a newline character, unless the <code>'\'</code> is
	 * preceded by another <code>'\'</code>.
	 * </p>
	 * 
	 * <p>
	 * A <code>null</code> string input has no effect.
	 * </p>
	 * 
	 * @param out
	 *            the <code>String</code> used to output unescaped characters
	 * @param str
	 *            the <code>String</code> to unescape, may be null
	 * @throws IllegalArgumentException
	 *             if the Writer is <code>null</code>
	 * @throws IOException
	 *             if error occurs on underlying Writer
	 */
	private String unescapeJava(String str) throws IOException {
		if (str == null) {
			return "";
		}
		int sz = str.length();
		StringBuffer unicode = new StringBuffer(4);

		StringBuilder strout = new StringBuilder();
		boolean hadSlash = false;
		boolean inUnicode = false;
		for (int i = 0; i < sz; i++) {
			char ch = str.charAt(i);
			if (inUnicode) {
				// if in unicode, then we're reading unicode
				// values in somehow
				unicode.append(ch);
				if (unicode.length() == 4) {
					// unicode now contains the four hex digits
					// which represents our unicode character
					try {
						int value = Integer.parseInt(unicode.toString(), 16);
						strout.append((char) value);
						unicode.setLength(0);
						inUnicode = false;
						hadSlash = false;
					} catch (NumberFormatException nfe) {
						// throw new
						// NestableRuntimeException("Unable to parse unicode value: "
						// + unicode, nfe);
						throw new IOException("Unable to parse unicode value: "
								+ unicode, nfe);
					}
				}
				continue;
			}
			if (hadSlash) {
				// handle an escaped value
				hadSlash = false;
				switch (ch) {
				case '\\':
					strout.append('\\');
					break;
				case '\'':
					strout.append('\'');
					break;
				case '\"':
					strout.append('"');
					break;
				case 'r':
					strout.append('\r');
					break;
				case 'f':
					strout.append('\f');
					break;
				case 't':
					strout.append('\t');
					break;
				case 'n':
					strout.append('\n');
					break;
				case 'b':
					strout.append('\b');
					break;
				case 'u': {
					// uh-oh, we're in unicode country....
					inUnicode = true;
					break;
				}
				default:
					strout.append(ch);
					break;
				}
				continue;
			} else if (ch == '\\') {
				hadSlash = true;
				continue;
			}
			strout.append(ch);
		}
		if (hadSlash) {
			// then we're in the weird case of a \ at the end of the
			// string, let's output it anyway.
			strout.append('\\');
		}
		return new String(strout.toString());
	}

}