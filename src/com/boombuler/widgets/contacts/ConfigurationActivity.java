/*
 * Copyright (C) 2010 Florian Sundermann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.boombuler.widgets.contacts;

import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.provider.ContactsContract;

public class ConfigurationActivity extends PreferenceActivity {

    private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private static final boolean mIsFroyo = Build.VERSION.SDK_INT > Build.VERSION_CODES.ECLAIR_MR1;

    private ListPreference bgimage;
    private DialogSeekBarPreference columnCount;
    private EditTextPreference displayLabel;
    private DialogSeekBarPreference backgroundAlpha;
    private ListPreference txtAlign;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Build GUI from resource
		addPreferencesFromResource(R.xml.preferences);

		// Get the starting Intent
		Intent launchIntent = getIntent();
		Bundle extras = launchIntent.getExtras();
        if (extras != null) {
            appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);

            // Cancel by default
			Intent resultValue = new Intent();
            resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            setResult(RESULT_OK, resultValue);
        } else {
            finish();
        }
        // prepare the GUI components
        prepareDisplayLabel();
        prepareContactGroups();
        prepareColumnCount();
		prepareShowName();
		prepareTextAlignment();
		prepareNameKinds();

		prepareOnClick();
		prepareBackgroundAlpha();
		prepareHelpBtn();
		prepareAboutBtn();
		
		prepareBGImage();
	}
	@Override
	protected void onStop() {
		Intent updateWidget = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
		int[] ids = new int[] { appWidgetId };
        updateWidget.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        sendBroadcast(updateWidget);
		super.onStop();
	}
	
	private void prepareBackgroundAlpha() {
		backgroundAlpha = (DialogSeekBarPreference)findPreference(Preferences.BACKGROUND_ALPHA);
		if (backgroundAlpha != null) {
			if (!mIsFroyo) {
				backgroundAlpha.setEnabled(false);
				backgroundAlpha.setSummary(getString(R.string.needs_froyo));
			} else {
				backgroundAlpha.setKey(Preferences.get(Preferences.BACKGROUND_ALPHA, appWidgetId));
				backgroundAlpha.setOnPreferenceChangeListener(new SetCurValue(null, null));
			}
			backgroundAlpha.setValue(Preferences.getBackgroundAlpha(this, appWidgetId));
		}
	}

	private void prepareColumnCount() {
		columnCount = (DialogSeekBarPreference)findPreference(Preferences.COLUMN_COUNT);
		if (columnCount != null) {
			columnCount.setKey(Preferences.get(Preferences.COLUMN_COUNT, appWidgetId));
			columnCount.setMin(1);
			columnCount.setMax(6);
			columnCount.setOnPreferenceChangeListener(new SetCurValue(null, null));
			columnCount.setValue(Preferences.getColumnCount(this, appWidgetId));
		}
	}

	private void prepareShowName() {
		// Find control and set the right preference-key for the AppWidgetId
		CheckBoxPreference showName = (CheckBoxPreference)findPreference(Preferences.SHOW_NAME);
		if (showName != null) {
			showName.setKey(Preferences.get(Preferences.SHOW_NAME, appWidgetId));
			showName.setChecked(Preferences.getShowName(this, appWidgetId));
		}
	}

	private void prepareNameKinds() {
		ListPreference nameKinds = (ListPreference)findPreference(Preferences.NAME_KIND);
		if (nameKinds != null) {
			nameKinds.setKey(Preferences.get(Preferences.NAME_KIND, appWidgetId));
			nameKinds.setDependency(Preferences.get(Preferences.SHOW_NAME, appWidgetId));
	
			CharSequence[] Titles = new CharSequence[] {
					getString(R.string.displayname),
					getString(R.string.givenname),
					getString(R.string.familyname),
					getString(R.string.alias)};
			CharSequence[] Values = new CharSequence[] {
					String.valueOf(Preferences.NAME_DISPLAY_NAME),
					String.valueOf(Preferences.NAME_GIVEN_NAME),
					String.valueOf(Preferences.NAME_FAMILY_NAME),
					String.valueOf(Preferences.NAME_ALIAS)};
			nameKinds.setOnPreferenceChangeListener(new SetCurValue(Titles, Values));
	
			nameKinds.setEntries(Titles);
			nameKinds.setEntryValues(Values);
			nameKinds.setValue(String.valueOf(Preferences.getNameKind(this, appWidgetId)));
		}
	}

	private void prepareTextAlignment() {
		txtAlign = (ListPreference)findPreference(Preferences.TEXT_ALIGN);
		if (txtAlign != null) {
			txtAlign.setKey(Preferences.get(Preferences.TEXT_ALIGN, appWidgetId));
			txtAlign.setDependency(Preferences.get(Preferences.SHOW_NAME, appWidgetId));
	
			CharSequence[] Titles = new CharSequence[] {
					getString(R.string.align_left),
					getString(R.string.align_center),
					getString(R.string.align_right)};
			CharSequence[] Values = new CharSequence[] {
					String.valueOf(Preferences.ALIGN_LEFT),
					String.valueOf(Preferences.ALIGN_CENTER),
					String.valueOf(Preferences.ALIGN_RIGHT)};
	
			SetCurValue changelist = new SetCurValue(Titles, Values);
			txtAlign.setOnPreferenceChangeListener(changelist);
	
			txtAlign.setEntries(Titles);
			txtAlign.setEntryValues(Values);
	
			String value = String.valueOf(Preferences.getTextAlign(this, appWidgetId));
			txtAlign.setValue(value);
			changelist.onPreferenceChange(txtAlign, value);
		}
	}

	private void prepareOnClick() {
		ListPreference onClick = (ListPreference)findPreference(Preferences.ON_CLICK);
		if (onClick != null) {
			onClick.setKey(Preferences.get(Preferences.ON_CLICK, appWidgetId));
	
			CharSequence[] Titles = new CharSequence[] {
					getString(R.string.quickcontactbar),
					getString(R.string.directdial),
					getString(R.string.showcontact),
					getString(R.string.sendsms)};
			CharSequence[] Values = new CharSequence[] {
					String.valueOf(Preferences.CLICK_QCB),
					String.valueOf(Preferences.CLICK_DIAL),
					String.valueOf(Preferences.CLICK_SHWCONTACT),
					String.valueOf(Preferences.CLICK_SMS)};
			onClick.setOnPreferenceChangeListener(new SetCurValue(Titles, Values));
	
			onClick.setEntries(Titles);
			onClick.setEntryValues(Values);
			onClick.setValue(String.valueOf(Preferences.getOnClickAction(this, appWidgetId)));
		}
	}

	private void prepareDisplayLabel() {
		// Find control and set the right preference-key for the AppWidgetId
		displayLabel = (EditTextPreference)findPreference(Preferences.DISPLAY_LABEL);
		if (displayLabel != null) {
			displayLabel.setKey(Preferences.get(Preferences.DISPLAY_LABEL, appWidgetId));
			// Set summary on value changed
			displayLabel.setOnPreferenceChangeListener(new SetCurValue(null, null));
	
			displayLabel.setText(Preferences.getDisplayLabel(this, appWidgetId));
		}
	}

	private void prepareContactGroups() {
		// Find control and set the right preference-key for the AppWidgetId
		ListPreference selectGroup = (ListPreference)findPreference(Preferences.GROUP_ID);
		if (selectGroup != null) {
			selectGroup.setKey(Preferences.get(Preferences.GROUP_ID, appWidgetId));
	
	    	Uri uri = ContactsContract.Groups.CONTENT_URI;
	    	String[] projection = new String[] {
	    			ContactsContract.Groups._ID,
	    			ContactsContract.Groups.TITLE
	    	};
	
	    	// avoid "null-groups"
	    	String selection = "("+ContactsContract.Groups._ID
	    	                 +" is not null) and ("
	    	                 + ContactsContract.Groups.TITLE + " is not null)";
	    	// read the ContactGroups
	    	Cursor orgCs = this.managedQuery(uri, projection, selection, null, null);
	
	    	CharSequence[] Titles = new CharSequence[orgCs.getCount()+Preferences.VIRTUAL_GROUP_COUNT];
	    	CharSequence[] Values = new CharSequence[orgCs.getCount()+Preferences.VIRTUAL_GROUP_COUNT];
	
	    	int pos = 0;
	    	// First add the "virtual" group "All Contacts"
	    	Titles[pos] = getString(R.string.allcontacts);
	    	Values[pos++] = String.valueOf(Preferences.GROUP_ALLCONTACTS);
	
	    	// First add the "stared" group "All Contacts"
	    	Titles[pos] = getString(R.string.starred);
	    	Values[pos++] = String.valueOf(Preferences.GROUP_STARRED);
	
	    	orgCs.moveToFirst();
	    	while (!orgCs.isAfterLast()) {
	    		// Then add one entry for each contact group
	    		Values[pos] = orgCs.getString(orgCs.getColumnIndex(ContactsContract.Groups._ID));
	        	Titles[pos++] = orgCs.getString(orgCs.getColumnIndex(ContactsContract.Groups.TITLE));
	    		orgCs.moveToNext();
	    	}
			orgCs.close();
	
			// Set the summary on value change
			selectGroup.setOnPreferenceChangeListener(new SetCurValue(Titles, Values));
	
			selectGroup.setEntries(Titles);
			selectGroup.setEntryValues(Values);
			selectGroup.setValue(String.valueOf(Preferences.getGroupId(this, appWidgetId)));
		}
	}

	private void prepareBGImage() {
		bgimage = (ListPreference)findPreference(Preferences.BGIMAGE);
		if (bgimage != null) {
			bgimage.setKey(Preferences.get(Preferences.BGIMAGE, appWidgetId));
			final CharSequence[] Titles, Values;
			if (mIsFroyo) {
				Titles = new CharSequence[] {
						getString(R.string.ics_style),
						getString(R.string.black),
						getString(R.string.white)};
				Values = new CharSequence[] {
						String.valueOf(Preferences.BG_ICS),
						String.valueOf(Preferences.BG_BLACK),
						String.valueOf(Preferences.BG_WHITE)};
			}
			else
			{
				Titles = new CharSequence[] {
						getString(R.string.black),
						getString(R.string.white),
						getString(R.string.transparent)};
				Values = new CharSequence[] {
						String.valueOf(Preferences.BG_BLACK),
						String.valueOf(Preferences.BG_WHITE),
						String.valueOf(Preferences.BG_TRANS)};
			}
			SetCurValue vcl = new SetCurValue(Titles, Values);
			bgimage.setOnPreferenceChangeListener(vcl);
	
			bgimage.setEntries(Titles);
			bgimage.setEntryValues(Values);
			String value = String.valueOf(Preferences.getBGImage(this, appWidgetId));
			bgimage.setValue(value);
			vcl.onPreferenceChange(bgimage, value);
		}
	}

	private void prepareHelpBtn() {
		Preference pref = findPreference("HELP");
		if (pref != null)
			pref.setOnPreferenceClickListener(new HelpButtonClick(this, true));
	}

	private void prepareAboutBtn() {
		Preference pref = findPreference("ABOUT");
		if (pref != null)
			pref.setOnPreferenceClickListener(new HelpButtonClick(this, false));
	}

	private class HelpButtonClick implements OnPreferenceClickListener {
		private final Context fContext;
		private final boolean fShowHelp;

		public HelpButtonClick(Context context, boolean showHelp) {
			fContext = context;
			fShowHelp = showHelp;
		}

		public boolean onPreferenceClick(Preference preference) {
			AlertDialog alertDialog;
			alertDialog = new AlertDialog.Builder(fContext).create();
			if (fShowHelp) {
				alertDialog.setTitle(fContext.getString(R.string.help));
				alertDialog.setMessage(fContext.getString(R.string.helptext));
			}
			else {
				alertDialog.setTitle(fContext.getString(R.string.about));
				alertDialog.setMessage(fContext.getString(R.string.abouttext));
			}
			alertDialog.setButton(fContext.getString(R.string.okbtn), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					dialog.cancel();
				}
			});
			alertDialog.show();
			return false;
		}

	}

	// OnPreferenceChangeListener to set the summary of the preference
	// to the display text of the new value
	private class SetCurValue implements OnPreferenceChangeListener {
		private final CharSequence[] fValues, fTitles;
		public SetCurValue(CharSequence[] Titles, CharSequence[] Values) {
			fValues = Values;
			fTitles = Titles;
		}

		public boolean onPreferenceChange(Preference preference, Object newValue) {
			CharSequence curVal = null;
			if (preference instanceof ListPreference) {
				for(int i = 0; i < fValues.length; i++) {
					if (fValues[i].equals(newValue)) {
						curVal = fTitles[i];
						break;
					}
				}
			}
			else if (preference instanceof EditTextPreference) {
				curVal = newValue.toString();
			}
			else if (preference instanceof DialogSeekBarPreference) {
				curVal = newValue.toString();
			}
			if (preference == bgimage) {
				boolean isICSStyle = newValue.toString().equals(String.valueOf(Preferences.BG_ICS));
				
				columnCount.setEnabled(!isICSStyle);
				txtAlign.setEnabled(!isICSStyle);
				displayLabel.setEnabled(!isICSStyle);				
				backgroundAlpha.setEnabled(!isICSStyle && mIsFroyo);
			}
			
			preference.setSummary(curVal);
			return true;
		}
	}
}