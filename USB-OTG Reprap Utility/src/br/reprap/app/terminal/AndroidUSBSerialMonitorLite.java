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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.View.OnLongClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.physicaloid.lib.Physicaloid;
import com.physicaloid.lib.usb.driver.uart.UartConfig;

public class AndroidUSBSerialMonitorLite extends Activity {
	static final int PREFERENCE_REV_NUM = 1;

	// debug settings
	private static final boolean SHOW_DEBUG = false;
	private static final boolean USE_WRITE_BUTTON_FOR_DEBUG = false;

	public static final String PREFS = "usbotg";

	public static final boolean isICSorHigher = (Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB_MR2);

	// occurs USB packet loss if TEXT_MAX_SIZE is over 6000
	private static final int TEXT_MAX_SIZE = 8192;

	private static final int MENU_ID_SETTING = 0;
	private static final int MENU_ID_CLEARTEXT = 1;
	private static final int MENU_ID_SENDTOEMAIL = 2;
	private static final int MENU_ID_OPENDEVICE = 3;
	private static final int MENU_ID_CLOSEDEVICE = 4;
	private static final int MENU_ID_WORDLIST = 5;
	private static final int DIALOG_SD_LIST = 6;
	private static final int COLD_EXTRUSION = 7;

	private static final int REQUEST_PREFERENCE = 0;
	private static final int REQUEST_WORD_LIST_ACTIVITY = 1;
	private static final int REQUEST_EEPROM = 2;

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

	Physicaloid mSerial;

	private ScrollView mSvText;
	private TextView mTvSerial;
	private StringBuilder mText = new StringBuilder();
	private boolean mStop = false;

	String TAG = "AndroidSerialTerminal";

	Handler mHandler = new Handler();

	private Button btWrite, btCustom0, btCustom1, btCustom2, btCustom3,
			btCustom4, btCustom5;
	private Button btUp, btLeft, btHomeZ, btRight, btDown, btZplus, btZminus,
			btHomeY, btHomeX, btMoff, btCE;
	private Button btExt, btRev, btHotOff, btBedOff, btFanOn, btFanOff,
			btStatus, btPrint, btPause, btEeprom, btPlay;
	private Button btRestart, btPLA, btABS;
	private EditText etWrite;
	private TextView HotTemp, BedTemp;

	SharedPreferences prefs;

	// Default settings
	private int mTextFontSize = 12;
	private Typeface mTextTypeface = Typeface.MONOSPACE;
	private int mDisplayType = DISP_CHAR;
	private int mReadLinefeedCode = LINEFEED_CODE_LF;
	private int mWriteLinefeedCode = LINEFEED_CODE_LF;
	private int mBaudrate = 250000;
	private int mDataBits = UartConfig.DATA_BITS8;
	private int mParity = UartConfig.PARITY_NONE;
	private int mStopBits = UartConfig.STOP_BITS1;
	private int mFlowControl = UartConfig.FLOW_CONTROL_OFF;
	private String mEmailAddress = "@gmail.com";
	private String[] sdFiles;

	private String XYZ_Steps;
	private String XY_Feedrate;
	private String Z_Feedrate;
	private String E_Feedrate;
	private String E_Steps;

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
		setContentView(R.layout.main);

		btUp = (Button) findViewById(R.id.btUp);
		btUp.setEnabled(false);
		btLeft = (Button) findViewById(R.id.btLeft);
		btLeft.setEnabled(false);
		btHomeZ = (Button) findViewById(R.id.btHomeZ);
		btHomeZ.setEnabled(false);
		btRight = (Button) findViewById(R.id.btRight);
		btRight.setEnabled(false);
		btDown = (Button) findViewById(R.id.btDown);
		btDown.setEnabled(false);
		btZplus = (Button) findViewById(R.id.btZplus);
		btZplus.setEnabled(false);
		btZminus = (Button) findViewById(R.id.btZminus);
		btZminus.setEnabled(false);
		btHomeY = (Button) findViewById(R.id.btHomeY);
		btHomeY.setEnabled(false);
		btHomeX = (Button) findViewById(R.id.btHomeX);
		btHomeX.setEnabled(false);
		btMoff = (Button) findViewById(R.id.btMoff);
		btMoff.setEnabled(false);
		btCE = (Button) findViewById(R.id.btCE);
		btCE.setEnabled(false);
		btExt = (Button) findViewById(R.id.btExt);
		btExt.setEnabled(false);
		btRev = (Button) findViewById(R.id.btRev);
		btRev.setEnabled(false);
		btHotOff = (Button) findViewById(R.id.btHotOff);
		btHotOff.setEnabled(false);
		btBedOff = (Button) findViewById(R.id.btBedOff);
		btBedOff.setEnabled(false);
		btFanOn = (Button) findViewById(R.id.btFanOn);
		btFanOn.setEnabled(false);
		btFanOff = (Button) findViewById(R.id.btFanOff);
		btFanOff.setEnabled(false);
		btStatus = (Button) findViewById(R.id.btStatus);
		btStatus.setEnabled(false);
		btPrint = (Button) findViewById(R.id.btPrint);
		btPrint.setEnabled(false);
		btPause = (Button) findViewById(R.id.btPause);
		btPause.setEnabled(false);
		btEeprom = (Button) findViewById(R.id.btEeprom);
		// btEeprom.setEnabled(false);
		btPlay = (Button) findViewById(R.id.btPlay);
		btPlay.setEnabled(false);
		btRestart = (Button) findViewById(R.id.btRestart);
		btRestart.setEnabled(false);
		btPLA = (Button) findViewById(R.id.btPLA);
		btPLA.setEnabled(false);
		btABS = (Button) findViewById(R.id.btABS);
		btABS.setEnabled(false);

		mSvText = (ScrollView) findViewById(R.id.svText);
		mTvSerial = (TextView) findViewById(R.id.tvSerial);
		btWrite = (Button) findViewById(R.id.btWrite);
		btWrite.setEnabled(false);
		etWrite = (EditText) findViewById(R.id.etWrite);
		etWrite.setEnabled(false);
		etWrite.setHint("CR : \\r, LF : \\n, bin : \\u0000");

		btCustom0 = (Button) findViewById(R.id.btCustom0);
		btCustom0.setEnabled(false);
		btCustom1 = (Button) findViewById(R.id.btCustom1);
		btCustom1.setEnabled(false);
		btCustom2 = (Button) findViewById(R.id.btCustom2);
		btCustom2.setEnabled(false);
		btCustom3 = (Button) findViewById(R.id.btCustom3);
		btCustom3.setEnabled(false);
		btCustom4 = (Button) findViewById(R.id.btCustom4);
		btCustom4.setEnabled(false);
		btCustom5 = (Button) findViewById(R.id.btCustom5);
		btCustom5.setEnabled(false);

		HotTemp = (TextView) findViewById(R.id.txHotTemp);
		BedTemp = (TextView) findViewById(R.id.txBedTemp);

		if (SHOW_DEBUG) {
			Log.d(TAG, "New FTDriver");
		}

		// get service
		mSerial = new Physicaloid(this);

		// load flags preference
		prefs = PreferenceManager.getDefaultSharedPreferences(this);

		if (SHOW_DEBUG) {
			Log.d(TAG, "New instance : " + mSerial);
		}
		// listen for new devices
		IntentFilter filter = new IntentFilter();
		filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
		filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
		registerReceiver(mUsbReceiver, filter);

		if (SHOW_DEBUG) {
			Log.d(TAG, "FTDriver beginning");
		}

		updatePrefs();
		openUsbSerial();

		etWrite.setOnKeyListener(new OnKeyListener() {
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
		// Write Button
		// ---------------------------------------------------------------------------------------
		if (!USE_WRITE_BUTTON_FOR_DEBUG) {
			btWrite.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					writeDataToSerial();
				}
			});
		} else {
			// Write test button for debug
			btWrite.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					String strWrite = "";
					for (int i = 0; i < 3000; ++i) {
						strWrite = strWrite + " " + Integer.toString(i);
					}
					if (SHOW_DEBUG) {
						Log.d(TAG, "FTDriver Write(" + strWrite.length()
								+ ") : " + strWrite);
					}
					mSerial.write(strWrite.getBytes(), strWrite.length());
				}
			});
		} // end of if(SHOW_WRITE_TEST_BUTTON)

		// Up Button Y-
		btUp.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (prefs.getBoolean("invert_y", true)) {
					String str = "G91 \nG1 Y" + XYZ_Steps + " F" + XY_Feedrate
							+ "\nG90";
					str = changeEscapeSequence(str);
					mSerial.write(str.getBytes(), str.length());
				} else {
					String str = "G91 \nG1 Y-" + XYZ_Steps + " F" + XY_Feedrate
							+ "\nG90";
					str = changeEscapeSequence(str);
					mSerial.write(str.getBytes(), str.length());
				}
			}
		});

		// Left Button X-
		btLeft.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (prefs.getBoolean("invert_x", true)) {
					String str = "G91 \nG1 X" + XYZ_Steps + " F" + XY_Feedrate
							+ "\nG90";
					str = changeEscapeSequence(str);
					mSerial.write(str.getBytes(), str.length());
					;
				} else {
					String str = "G91 \nG1 X-" + XYZ_Steps + " F" + XY_Feedrate
							+ "\nG90";
					str = changeEscapeSequence(str);
					mSerial.write(str.getBytes(), str.length());
					;
				}
			}
		});

		// Home Z Button
		btHomeZ.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String str = "G28 Z";
				str = changeEscapeSequence(str);
				mSerial.write(str.getBytes(), str.length());
			}
		});

		// TODO: Use long press to change step size for each axis
		btHomeZ.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				Toast.makeText(getApplicationContext(), "LONG PRESS...",
						Toast.LENGTH_SHORT).show();
				return true;
			}
		});

		// Right Button X+
		btRight.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (prefs.getBoolean("invert_x", true)) {
					String str = "G91 \nG1 X-" + XYZ_Steps + " F" + XY_Feedrate
							+ "\nG90";
					str = changeEscapeSequence(str);
					mSerial.write(str.getBytes(), str.length());
				} else {
					String str = "G91 \nG1 X" + XYZ_Steps + " F" + XY_Feedrate
							+ "\nG90";
					str = changeEscapeSequence(str);
					mSerial.write(str.getBytes(), str.length());
					;
				}
			}
		});

		// Down Button Y+
		btDown.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (prefs.getBoolean("invert_y", true)) {
					String str = "G91 \nG1 Y-" + XYZ_Steps + " F" + XY_Feedrate
							+ "\nG90";
					str = changeEscapeSequence(str);
					mSerial.write(str.getBytes(), str.length());
				} else {
					String str = "G91 \nG1 Y" + XYZ_Steps + " F" + XY_Feedrate
							+ "\nG90";
					str = changeEscapeSequence(str);
					mSerial.write(str.getBytes(), str.length());
				}
			}
		});

		// Zplus Button Z+
		btZplus.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String str = "G91 \nG1 Z" + XYZ_Steps + " F" + Z_Feedrate
						+ "\nG90";
				str = changeEscapeSequence(str);
				mSerial.write(str.getBytes(), str.length());
			}
		});

		// Zminus Button Z-
		btZminus.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String str = "G91 \nG1 Z-" + XYZ_Steps + " F" + Z_Feedrate
						+ "\nG90";
				str = changeEscapeSequence(str);
				mSerial.write(str.getBytes(), str.length());
			}
		});

		// Home Y Button
		btHomeY.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String str = "G28 Y";
				str = changeEscapeSequence(str);
				mSerial.write(str.getBytes(), str.length());
			}
		});

		// Home X Button
		btHomeX.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String str = "G28 X";
				str = changeEscapeSequence(str);
				mSerial.write(str.getBytes(), str.length());
			}
		});

		// Motors Off Button
		btMoff.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				String str = "M18";
				str = changeEscapeSequence(str);
				mSerial.write(str.getBytes(), str.length());
			}
		});

		// Cold Extrusion Button
		btCE.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				showDialog(COLD_EXTRUSION);
			}
		});

		// Extrude Button
		btExt.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				String str = "G1 E" + E_Steps + " F" + E_Feedrate;
				str = changeEscapeSequence(str);
				mSerial.write(str.getBytes(), str.length());
			}
		});

		// Reverse Extruder Button
		btRev.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				String str = "G1 E-" + E_Steps + " F" + E_Feedrate;
				str = changeEscapeSequence(str);
				mSerial.write(str.getBytes(), str.length());
			}
		});

		// Set PLA Temps Button
		btPLA.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				String strH1 = prefs.getString("pla_hotend_temp", "0");
				String strH2 = "M104 S" + strH1;
				strH2 = changeEscapeSequence(strH2);
				mSerial.write(strH2.getBytes(), strH2.length());

				String strH3 = strH1 + " ºC";
				HotTemp.setText(getString(R.string.hot_temp, strH3));

				String str1 = prefs.getString("pla_bed_temp", "0");
				String str2 = "M140 S" + str1;
				str2 = changeEscapeSequence(str2);
				mSerial.write(str2.getBytes(), str2.length());

				String str3 = str1 + " ºC";
				BedTemp.setText(getString(R.string.bedtemp, str3));
			}
		});

		// Set ABS Temps Button
		btABS.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				String strH1 = prefs.getString("abs_hotend_temp", "0");
				String strH2 = "M104 S" + strH1;
				strH2 = changeEscapeSequence(strH2);
				mSerial.write(strH2.getBytes(), strH2.length());

				String strH3 = strH1 + " ºC";
				HotTemp.setText(getString(R.string.hot_temp, strH3));

				String str1 = prefs.getString("abs_bed_temp", "0");
				String str2 = "M140 S" + str1;
				str2 = changeEscapeSequence(str2);
				mSerial.write(str2.getBytes(), str2.length());

				String str3 = str1 + " ºC";
				BedTemp.setText(getString(R.string.bedtemp, str3));
			}
		});

		// HotEnd Temp Off Button
		btHotOff.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				String str = "M104 S0";
				str = changeEscapeSequence(str);
				mSerial.write(str.getBytes(), str.length());
				HotTemp.setText("0 ºC");
			}
		});

		// Bed Temp Off Button
		btBedOff.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				String str = "M140 S0";
				str = changeEscapeSequence(str);
				mSerial.write(str.getBytes(), str.length());
				BedTemp.setText("0 ºC");
			}
		});

		// Fan ON Button
		btFanOn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				String str = "M106 S255";
				str = changeEscapeSequence(str);
				mSerial.write(str.getBytes(), str.length());
			}
		});

		// Fan OFF Button
		btFanOff.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				String str = "M106 S0";
				str = changeEscapeSequence(str);
				mSerial.write(str.getBytes(), str.length());
			}
		});

		// SD Print Status Button
		btStatus.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				mTvSerial.setText("");
				mText.setLength(0);

				mTvSerial.append("- Print Status:\n\n");

				String str = "M105 \nM27 \nM31";
				str = changeEscapeSequence(str);
				mSerial.write(str.getBytes(), str.length());
			}
		});

		// Print Button
		btPrint.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				mTvSerial.setText("");
				mText.setLength(0);

				mTvSerial.append("- SD Card Files:\n\n");

				String strCode = "M20";
				strCode = changeEscapeSequence(strCode);
				mSerial.write(strCode.getBytes(), strCode.length());

				Toast.makeText(getApplicationContext(), "Reading SD card...",
						Toast.LENGTH_SHORT).show();

				final Handler handler = new Handler();
				handler.postDelayed(new Runnable() {
					@Override
					public void run() {
						// Do something after 1000ms
						String strData = mTvSerial.getText().toString().trim();
						String strRegex = Pattern.quote("list") + "(.*?)"
								+ Pattern.quote("End");
						Pattern pattern = Pattern.compile(strRegex,
								Pattern.DOTALL);
						Matcher matcher = pattern.matcher(strData);

						while (matcher.find()) {
							String textInBetween = matcher.group(1);
							sdFiles = textInBetween.split("\\s");

							List<String> list = new ArrayList<String>(Arrays
									.asList(sdFiles));
							list.remove(0);
							String[] new_array = list.toArray(new String[0]);

							sdFiles = new_array;
							showDialog(DIALOG_SD_LIST);
						}
					}
				}, 1000);
			}
		});

		// Pause Button
		btPause.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				String str = "M25";
				str = changeEscapeSequence(str);
				mSerial.write(str.getBytes(), str.length());
			}
		});

		// Play Button
		btPlay.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				String str = "M24";
				str = changeEscapeSequence(str);
				mSerial.write(str.getBytes(), str.length());
			}
		});

		// Restart Button
		btRestart.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				mTvSerial.setText("");
				mText.setLength(0);

				// my attempt to force external reset
				mSerial.setConfig(new UartConfig(mBaudrate, mDataBits,
						mStopBits, mParity, true, true));
				final Handler handler = new Handler();
				handler.postDelayed(new Runnable() {
					@Override
					public void run() {
						mSerial.setConfig(new UartConfig(mBaudrate, mDataBits,
								mStopBits, mParity, false, false));
					}
				}, 1000); // Do something after 1000ms
			}
		});

		// EEPROM Button
		btEeprom.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				closeUsbSerial();
				startEpromActivity();
			}
		});

		// Custom Button 0
		btCustom0.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				customBt("custom_bt0");
			}
		});

		// Custom Button 1
		btCustom1.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				customBt("custom_bt1");
			}
		});

		// Custom Button 2
		btCustom2.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				customBt("custom_bt2");
			}
		});

		// Custom Button 3
		btCustom3.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				customBt("custom_bt3");
			}
		});

		// Custom Button 4
		btCustom4.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				customBt("custom_bt4");
			}
		});

		// Custom Button 5
		btCustom5.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				customBt("custom_bt5");
			}
		});
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_SD_LIST:

			return new AlertDialog.Builder(AndroidUSBSerialMonitorLite.this)
					// .setIconAttribute(android.R.attr.alertDialogIcon)
					.setTitle("Select G-Code File")
					.setSingleChoiceItems(sdFiles, 0,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									/*
									 * User clicked on a radio button do some
									 * stuff
									 */
								}
							})
					.setPositiveButton("Open",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									int selectedPosition = ((AlertDialog) dialog)
											.getListView()
											.getCheckedItemPosition();
									Toast.makeText(getApplicationContext(),
											sdFiles[selectedPosition],
											Toast.LENGTH_SHORT).show();

									String fileName = sdFiles[selectedPosition];
									fileName = fileName.toLowerCase();

									String str1 = "M23 " + fileName;
									str1 = changeEscapeSequence(str1);
									mSerial.write(str1.getBytes(),
											str1.length());
								}
							})
					.setNegativeButton("Cancel",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									/* User clicked No so do some stuff */
								}
							}).create();

		case COLD_EXTRUSION:
			return new AlertDialog.Builder(AndroidUSBSerialMonitorLite.this)
					.setTitle("Cold Extrusion")
					.setMessage(
							"Are you sure you want to do this, you removed your hotend?")
					.setPositiveButton("Enable",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									String strCode = "M302";
									strCode = changeEscapeSequence(strCode);
									mSerial.write(strCode.getBytes(),
											strCode.length());
									Toast.makeText(getApplicationContext(),
											"cold extrusion enabled",
											Toast.LENGTH_SHORT).show();
								}
							})
					.setNegativeButton("Cancel",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									/* User clicked No so do some stuff */
								}
							}).create();
		}
		return null;
	}

	private void startEpromActivity() {
		startActivityForResult(new Intent().setClassName(this.getPackageName(),
				EepromSettings.class.getName()), REQUEST_EEPROM);
	}

	private void customBt(String strBt) {
		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(this);
		String res = pref.getString(strBt, "");
		res = changeEscapeSequence(res);
		mSerial.write(res.getBytes(), res.length());
	}

	private void writeDataToSerial() {
		String strWrite = etWrite.getText().toString();
		strWrite = changeEscapeSequence(strWrite);
		if (SHOW_DEBUG) {
			Log.d(TAG, "FTDriver Write(" + strWrite.length() + ") : "
					+ strWrite);
		}
		mSerial.write(strWrite.getBytes(), strWrite.length());
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

	public void setWriteTextString(String str) {
		etWrite.setText(str);
	}

	// ---------------------------------------------------------------------------------------
	// Menu Button
	// ---------------------------------------------------------------------------------------
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(Menu.NONE, MENU_ID_OPENDEVICE, Menu.NONE, "Open Device");
		menu.add(Menu.NONE, MENU_ID_WORDLIST, Menu.NONE, "Word List ...");
		menu.add(Menu.NONE, MENU_ID_SETTING, Menu.NONE, "Setting ...");
		menu.add(Menu.NONE, MENU_ID_CLEARTEXT, Menu.NONE, "Clear Text");
		menu.add(Menu.NONE, MENU_ID_SENDTOEMAIL, Menu.NONE, "Email to ...");
		menu.add(Menu.NONE, MENU_ID_CLOSEDEVICE, Menu.NONE, "Close Device");
		/*
		 * if(mSerial!=null) { if(mSerial.isConnected()) {
		 * menu.getItem(MENU_ID_OPENDEVICE).setEnabled(false); } else {
		 * menu.getItem(MENU_ID_CLOSEDEVICE).setEnabled(false); } }
		 */return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_ID_OPENDEVICE:
			openUsbSerial();
			return true;
		case MENU_ID_WORDLIST:
			Intent intent = new Intent(this, WordListActivity.class);
			startActivityForResult(intent, REQUEST_WORD_LIST_ACTIVITY);
			return true;
		case MENU_ID_SETTING:
			startActivityForResult(new Intent().setClassName(
					this.getPackageName(),
					AndroidUSBSerialMonitorLitePrefActivity.class.getName()),
					REQUEST_PREFERENCE);
			return true;
		case MENU_ID_CLEARTEXT:
			mTvSerial.setText("");
			mText.setLength(0);
			return true;
		case MENU_ID_SENDTOEMAIL:
			sendTextToEmail();
			return true;
		case MENU_ID_CLOSEDEVICE:
			closeUsbSerial();
			return true;
		default:
			return false;
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == REQUEST_WORD_LIST_ACTIVITY) {
			if (resultCode == RESULT_OK) {
				try {
					String strWord = data.getStringExtra("word");
					etWrite.setText(strWord);
					// Set a cursor position last
					etWrite.setSelection(etWrite.getText().length());
				} catch (Exception e) {
					Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG)
							.show();
				}
			}
		} else if (requestCode == REQUEST_PREFERENCE) {

			SharedPreferences pref = PreferenceManager
					.getDefaultSharedPreferences(this);

			String res = pref.getString("display_list",
					Integer.toString(DISP_CHAR));
			mDisplayType = Integer.valueOf(res);

			res = pref.getString("fontsize_list", Integer.toString(12));
			mTextFontSize = Integer.valueOf(res);
			mTvSerial.setTextSize(mTextFontSize);

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
			mTvSerial.setTypeface(mTextTypeface);
			etWrite.setTypeface(mTextTypeface);

			res = pref.getString("readlinefeedcode_list",
					Integer.toString(LINEFEED_CODE_CRLF));
			mReadLinefeedCode = Integer.valueOf(res);

			res = pref.getString("writelinefeedcode_list",
					Integer.toString(LINEFEED_CODE_CRLF));
			mWriteLinefeedCode = Integer.valueOf(res);

			res = pref.getString("email_edittext", "@gmail.com");
			mEmailAddress = res;

			int intRes;

			res = pref.getString("baudrate_list", Integer.toString(250000));
			intRes = Integer.valueOf(res);
			if (mBaudrate != intRes) {
				mBaudrate = intRes;
				mSerial.setBaudrate(mBaudrate);
			}

			res = pref.getString("databits_list",
					Integer.toString(UartConfig.DATA_BITS8));
			intRes = Integer.valueOf(res);
			if (mDataBits != intRes) {
				mDataBits = Integer.valueOf(res);
				mSerial.setDataBits(mDataBits);
			}

			res = pref.getString("parity_list",
					Integer.toString(UartConfig.PARITY_NONE));
			intRes = Integer.valueOf(res);
			if (mParity != intRes) {
				mParity = intRes;
				mSerial.setParity(mParity);
			}

			res = pref.getString("stopbits_list",
					Integer.toString(UartConfig.STOP_BITS1));
			intRes = Integer.valueOf(res);
			if (mStopBits != intRes) {
				mStopBits = intRes;
				mSerial.setStopBits(mStopBits);
			}

			res = pref.getString("flowcontrol_list",
					Integer.toString(UartConfig.FLOW_CONTROL_OFF));
			intRes = Integer.valueOf(res);
			if (mFlowControl != intRes) {
				mFlowControl = intRes;
				if (mFlowControl == UartConfig.FLOW_CONTROL_ON) {
					mSerial.setDtrRts(true, true);
				} else {
					mSerial.setDtrRts(false, false);
				}
			}

			updatePrefs();

		} else if (requestCode == REQUEST_EEPROM) {

			// My attempt to reopen serial communication.
			final Handler handler = new Handler();
			handler.postDelayed(new Runnable() {
				@Override
				public void run() {
					openUsbSerial();
				}
			}, 500);
		}
	}

	/**
	 * Saves values for view switching
	 */
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(BUNDLEKEY_LOADTEXTVIEW, mTvSerial.getText()
				.toString());
	}

	/**
	 * Loads values for view switching
	 */

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		mTvSerial.setText(savedInstanceState.getString(BUNDLEKEY_LOADTEXTVIEW));
	}

	@Override
	public void onDestroy() {
		mSerial.close();
		mStop = true;
		unregisterReceiver(mUsbReceiver);
		super.onDestroy();
	}

	private void mainloop() {
		mStop = false;
		mRunningMainLoop = true;
		btWrite.setEnabled(true);
		etWrite.setEnabled(true);
		btUp.setEnabled(true);
		btLeft.setEnabled(true);
		btHomeZ.setEnabled(true);
		btRight.setEnabled(true);
		btDown.setEnabled(true);
		btZplus.setEnabled(true);
		btZminus.setEnabled(true);
		btHomeY.setEnabled(true);
		btHomeX.setEnabled(true);
		btMoff.setEnabled(true);
		btCE.setEnabled(true);
		btExt.setEnabled(true);
		btRev.setEnabled(true);
		btHotOff.setEnabled(true);
		btBedOff.setEnabled(true);
		btFanOn.setEnabled(true);
		btFanOff.setEnabled(true);
		btStatus.setEnabled(true);
		btPrint.setEnabled(true);
		btPLA.setEnabled(true);
		btABS.setEnabled(true);
		btPause.setEnabled(true);
		btPlay.setEnabled(true);
		btRestart.setEnabled(true);
		// btEeprom.setEnabled(true);
		btCustom0.setEnabled(true);
		btCustom1.setEnabled(true);
		btCustom2.setEnabled(true);
		btCustom3.setEnabled(true);
		btCustom4.setEnabled(true);
		btCustom5.setEnabled(true);
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
				len = mSerial.read(rbuf);
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
							if (mTvSerial.length() > TEXT_MAX_SIZE) {
								StringBuilder sb = new StringBuilder();
								sb.append(mTvSerial.getText());
								sb.delete(0, TEXT_MAX_SIZE / 2);
								mTvSerial.setText(sb);
							}
							mTvSerial.append(mText);
							mText.setLength(0);
							mSvText.fullScroll(ScrollView.FOCUS_DOWN);
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
		mTvSerial.setTypeface(mTextTypeface);
		etWrite.setTypeface(mTextTypeface);

		res = pref.getString("readlinefeedcode_list",
				Integer.toString(LINEFEED_CODE_CRLF));
		mReadLinefeedCode = Integer.valueOf(res);

		res = pref.getString("writelinefeedcode_list",
				Integer.toString(LINEFEED_CODE_CRLF));
		mWriteLinefeedCode = Integer.valueOf(res);

		res = pref.getString("email_edittext", "@gmail.com");
		mEmailAddress = res;

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

	private void updatePrefs() {

		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(this);

		String res = prefs.getString("hotend_temp", "0");
		String strB = res + " ºC";
		HotTemp.setText(getString(R.string.hot_temp, strB));
		res = prefs.getString("bed_temp", "0");
		String strD = res + " ºC";
		BedTemp.setText(getString(R.string.bedtemp, strD));

		res = pref.getString("step_list", "10");
		XYZ_Steps = res;

		res = pref.getString("xy_feedrate", "3000");
		XY_Feedrate = res;

		res = pref.getString("z_feedrate", "200");
		Z_Feedrate = res;

		res = pref.getString("e_feedrate", "300");
		E_Feedrate = res;

		res = pref.getString("e_steps", "5");
		E_Steps = res;

		res = pref.getString("name_bt0", "bt0");
		btCustom0.setText(res);

		res = pref.getString("name_bt1", "bt1");
		btCustom1.setText(res);

		res = pref.getString("name_bt2", "bt2");
		btCustom2.setText(res);

		res = pref.getString("name_bt3", "bt3");
		btCustom3.setText(res);

		res = pref.getString("name_bt4", "bt4");
		btCustom4.setText(res);

		res = pref.getString("name_bt5", "bt5");
		btCustom5.setText(res);

	}

	private void sendTextToEmail() {
		Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"
				+ mEmailAddress));

		intent.putExtra("subject", "Result of " + getString(R.string.app_name));
		intent.putExtra("body", mTvSerial.getText().toString().trim());
		startActivity(intent);
	}

	// Load default baud rate
	int loadDefaultBaudrate() {
		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(this);
		String res = pref.getString("baudrate_list", Integer.toString(250000));
		return Integer.valueOf(res);
	}

	void openUsbSerial() {
		if (mSerial == null) {
			Toast.makeText(this, "cannot open", Toast.LENGTH_SHORT).show();
			return;
		}

		if (!mSerial.isOpened()) {
			if (SHOW_DEBUG) {
				Log.d(TAG, "onNewIntent begin");
			}
			if (!mSerial.open()) {
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
				mSerial.setConfig(new UartConfig(mBaudrate, mDataBits,
						mStopBits, mParity, dtrOn, rtsOn));

				if (SHOW_DEBUG) {
					Log.d(TAG, "setConfig : baud : " + mBaudrate
							+ ", DataBits : " + mDataBits + ", StopBits : "
							+ mStopBits + ", Parity : " + mParity + ", dtr : "
							+ dtrOn + ", rts : " + rtsOn);
				}

				mTvSerial.setTextSize(mTextFontSize);

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
		mSerial.close();
	}

	protected void onNewIntent(Intent intent) {
		if (SHOW_DEBUG) {
			Log.d(TAG, "onNewIntent");
		}

		openUsbSerial();
	};

	private void detachedUi() {
		btWrite.setEnabled(false);
		etWrite.setEnabled(false);
		btUp.setEnabled(false);
		btLeft.setEnabled(false);
		btHomeZ.setEnabled(false);
		btRight.setEnabled(false);
		btDown.setEnabled(false);
		btZplus.setEnabled(false);
		btZminus.setEnabled(false);
		btHomeY.setEnabled(false);
		btHomeX.setEnabled(false);
		btMoff.setEnabled(false);
		btCE.setEnabled(false);
		btExt.setEnabled(false);
		btRev.setEnabled(false);
		btHotOff.setEnabled(false);
		btBedOff.setEnabled(false);
		btFanOn.setEnabled(false);
		btFanOff.setEnabled(false);
		btStatus.setEnabled(false);
		btPrint.setEnabled(false);
		btPause.setEnabled(false);
		btPlay.setEnabled(false);
		btPLA.setEnabled(false);
		btABS.setEnabled(false);
		btRestart.setEnabled(false);
		// btEeprom.setEnabled(false);
		btCustom0.setEnabled(false);
		btCustom1.setEnabled(false);
		btCustom2.setEnabled(false);
		btCustom3.setEnabled(false);
		btCustom4.setEnabled(false);
		btCustom5.setEnabled(false);
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
				if (!mSerial.isOpened()) {
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
				// mSerial.usbDetached(intent);
				mSerial.close();
			} else if (ACTION_USB_PERMISSION.equals(action)) {
				if (SHOW_DEBUG) {
					Log.d(TAG, "Request permission");
				}
				synchronized (this) {
					if (!mSerial.isOpened()) {
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
