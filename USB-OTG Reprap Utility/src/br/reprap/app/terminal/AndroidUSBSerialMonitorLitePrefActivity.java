package br.reprap.app.terminal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import br.reprap.app.terminal.R;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;

public class AndroidUSBSerialMonitorLitePrefActivity extends PreferenceActivity {
	final static String BAUDRATE_KEY = "baudrate_list";

	Map<String, String> baudrateMap = new HashMap<String, String>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public void onBuildHeaders(List<Header> target) {
		// show a Preference header
		loadHeadersFromResource(R.xml.pref_header, target);
	}

	public static class SettingPrefsFragment extends PreferenceFragment {
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);

			// show a Preference inner
			addPreferencesFromResource(R.xml.pref_inner);
			updateSummary();

		}

		private SharedPreferences.OnSharedPreferenceChangeListener listener = new SharedPreferences.OnSharedPreferenceChangeListener() {

			public void onSharedPreferenceChanged(
					SharedPreferences sharedPreferences, String key) {

				updateSummary();

			}
		};

		private void updateSummary() {
			String summary;

			ListPreference lp = (ListPreference) getPreferenceScreen()
					.findPreference("baudrate_list");
			lp.setSummary(lp.getValue());

			lp = (ListPreference) getPreferenceScreen().findPreference(
					"databits_list");
			lp.setSummary(lp.getValue());

			lp = (ListPreference) getPreferenceScreen().findPreference(
					"parity_list");
			switch (Integer.valueOf(lp.getValue())) {
			case 0:
				summary = "None";
				break;
			case 1:
				summary = "Odd";
				break;
			case 2:
				summary = "Even";
				break;
			case 3:
				summary = "Mark";
				break;
			case 4:
				summary = "Space";
				break;
			default:
				summary = "None";
				lp.setValue("0");
				break;
			}
			lp.setSummary(summary);

			lp = (ListPreference) getPreferenceScreen().findPreference(
					"stopbits_list");
			switch (Integer.valueOf(lp.getValue())) {
			case 0:
				summary = "1";
				break;
			case 1:
				summary = "1.5";
				break;
			case 2:
				summary = "2";
				break;
			default:
				summary = "1";
				lp.setValue("0");
				break;
			}
			lp.setSummary(summary);

			lp = (ListPreference) getPreferenceScreen().findPreference(
					"flowcontrol_list");
			switch (Integer.valueOf(lp.getValue())) {
			case 0:
				summary = "Off";
				break;
			case 1:
				summary = "On";
				break;
			default:
				summary = "Off";
				lp.setValue("0");
				break;
			}
			lp.setSummary(summary);

			/*
			 * lp = (ListPreference)
			 * getPreferenceScreen().findPreference("break_list"); switch
			 * (Integer.valueOf(lp.getValue())) { case 0: summary = "None";
			 * break; case 1: summary = "Break"; break; default: summary =
			 * "none"; break; } lp.setSummary(summary);
			 */

			lp = (ListPreference) getPreferenceScreen().findPreference(
					"readlinefeedcode_list");
			switch (Integer.valueOf(lp.getValue())) {
			case 0:
				summary = "CR";
				break;
			case 1:
				summary = "CR+LF";
				break;
			case 2:
				summary = "LF";
				break;
			default:
				summary = "None";
				break;
			}
			lp.setSummary(summary);

			lp = (ListPreference) getPreferenceScreen().findPreference(
					"writelinefeedcode_list");
			switch (Integer.valueOf(lp.getValue())) {
			case 0:
				summary = "CR";
				break;
			case 1:
				summary = "CR+LF";
				break;
			case 2:
				summary = "LF";
				break;
			default:
				summary = "None";
				break;
			}
			lp.setSummary(summary);

			lp = (ListPreference) getPreferenceScreen().findPreference("rev");
		}

		@Override
		public void onResume() {
			super.onResume();
			getPreferenceScreen().getSharedPreferences()
					.registerOnSharedPreferenceChangeListener(listener);
		}

		@Override
		public void onPause() {
			super.onPause();
			getPreferenceScreen().getSharedPreferences()
					.unregisterOnSharedPreferenceChangeListener(listener);
		}
	}

	public static class PrinterPrefsFragment extends PreferenceFragment {
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);

			// show a Preference inner
			addPreferencesFromResource(R.xml.pref_printer);
			updateSummary();
		}

		private SharedPreferences.OnSharedPreferenceChangeListener listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
			public void onSharedPreferenceChanged(
					SharedPreferences sharedPreferences, String key) {
				updateSummary();
			}
		};

		private void updateSummary() {
			ListPreference lp = (ListPreference) getPreferenceScreen()
					.findPreference("step_list");
			lp.setSummary(lp.getValue());

			EditTextPreference etp = (EditTextPreference) getPreferenceScreen()
					.findPreference("xy_feedrate");
			etp.setSummary(etp.getText());
			;

			etp = (EditTextPreference) getPreferenceScreen().findPreference(
					"z_feedrate");
			etp.setSummary(etp.getText());
			;

			etp = (EditTextPreference) getPreferenceScreen().findPreference(
					"e_feedrate");
			etp.setSummary(etp.getText());
			;

			etp = (EditTextPreference) getPreferenceScreen().findPreference(
					"e_steps");
			etp.setSummary(etp.getText());
			;

			etp = (EditTextPreference) getPreferenceScreen().findPreference(
					"pla_hotend_temp");
			etp.setSummary(etp.getText());
			;

			etp = (EditTextPreference) getPreferenceScreen().findPreference(
					"pla_bed_temp");
			etp.setSummary(etp.getText());
			;

			etp = (EditTextPreference) getPreferenceScreen().findPreference(
					"abs_hotend_temp");
			etp.setSummary(etp.getText());
			;

			etp = (EditTextPreference) getPreferenceScreen().findPreference(
					"abs_bed_temp");
			etp.setSummary(etp.getText());
			;

			etp = (EditTextPreference) getPreferenceScreen().findPreference(
					"custom_bt0");
			etp.setSummary(etp.getText());
			;

			etp = (EditTextPreference) getPreferenceScreen().findPreference(
					"name_bt0");
			etp.setSummary(etp.getText());
			;

			etp = (EditTextPreference) getPreferenceScreen().findPreference(
					"custom_bt1");
			etp.setSummary(etp.getText());
			;

			etp = (EditTextPreference) getPreferenceScreen().findPreference(
					"name_bt1");
			etp.setSummary(etp.getText());
			;

			etp = (EditTextPreference) getPreferenceScreen().findPreference(
					"custom_bt2");
			etp.setSummary(etp.getText());
			;

			etp = (EditTextPreference) getPreferenceScreen().findPreference(
					"name_bt2");
			etp.setSummary(etp.getText());
			;

			etp = (EditTextPreference) getPreferenceScreen().findPreference(
					"custom_bt3");
			etp.setSummary(etp.getText());
			;

			etp = (EditTextPreference) getPreferenceScreen().findPreference(
					"name_bt3");
			etp.setSummary(etp.getText());
			;

			etp = (EditTextPreference) getPreferenceScreen().findPreference(
					"custom_bt4");
			etp.setSummary(etp.getText());
			;

			etp = (EditTextPreference) getPreferenceScreen().findPreference(
					"name_bt4");
			etp.setSummary(etp.getText());
			;

			etp = (EditTextPreference) getPreferenceScreen().findPreference(
					"custom_bt5");
			etp.setSummary(etp.getText());
			;

			etp = (EditTextPreference) getPreferenceScreen().findPreference(
					"name_bt5");
			etp.setSummary(etp.getText());
			;
		}

		@Override
		public void onResume() {
			super.onResume();
			getPreferenceScreen().getSharedPreferences()
					.registerOnSharedPreferenceChangeListener(listener);
		}

		@Override
		public void onPause() {
			super.onPause();
			getPreferenceScreen().getSharedPreferences()
					.unregisterOnSharedPreferenceChangeListener(listener);
		}
	}

	public static class SettingDisplayPrefsFragment extends PreferenceFragment {
		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);

			// show a Preference inner
			addPreferencesFromResource(R.xml.pref_disp_inner);
			updateSummary();
		}

		private SharedPreferences.OnSharedPreferenceChangeListener listener = new SharedPreferences.OnSharedPreferenceChangeListener() {

			public void onSharedPreferenceChanged(
					SharedPreferences sharedPreferences, String key) {

				updateSummary();

			}
		};

		private void updateSummary() {
			String summary;

			ListPreference lp = (ListPreference) getPreferenceScreen()
					.findPreference("fontsize_list");
			lp.setSummary(lp.getValue());

			lp = (ListPreference) getPreferenceScreen().findPreference(
					"typeface_list");
			switch (Integer.valueOf(lp.getValue())) {
			case 0:
				summary = "normal";
				break;
			case 1:
				summary = "sans";
				break;
			case 2:
				summary = "serif";
				break;
			case 3:
				summary = "monospace";
				break;
			default:
				summary = "normal";
				break;
			}
			lp.setSummary(summary);

			lp = (ListPreference) getPreferenceScreen().findPreference(
					"display_list");
			switch (Integer.valueOf(lp.getValue())) {
			case 0:
				summary = "Char";
				break;
			case 1:
				summary = "Dec";
				break;
			case 2:
				summary = "Hex";
				break;
			default:
				summary = "None";
				break;
			}
			lp.setSummary(summary);

			EditTextPreference etp = (EditTextPreference) getPreferenceScreen()
					.findPreference("email_edittext");
			etp.setSummary(etp.getText());
		}

		@Override
		public void onResume() {
			super.onResume();
			getPreferenceScreen().getSharedPreferences()
					.registerOnSharedPreferenceChangeListener(listener);
		}

		@Override
		public void onPause() {
			super.onPause();
			getPreferenceScreen().getSharedPreferences()
					.unregisterOnSharedPreferenceChangeListener(listener);
		}
	}

}
