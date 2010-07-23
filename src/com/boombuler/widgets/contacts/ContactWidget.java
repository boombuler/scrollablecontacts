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

import mobi.intuitit.android.content.LauncherIntent;
import android.appwidget.*;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.QuickContact;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;


public abstract class ContactWidget extends AppWidgetProvider {
	// Tag for logging
	private static final String TAG = "boombuler.ContactWidget";
	
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		// If no specific widgets requested, collect list of all
		if (appWidgetIds == null) {
			appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, ContactWidget.class));
		}
		Log.d(TAG, "recieved onUpdate");
        
        final int N = appWidgetIds.length;
        for (int i = 0; i < N; i++) {
            // Construct views
        	int appWidgetId = appWidgetIds[i];
            updateGroupTitle(context, appWidgetId);           
        }	
	}
	
	public void updateGroupTitle(Context context, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), getMainLayoutId(context, appWidgetId));
        String text = Preferences.getDisplayLabel(context, appWidgetId);
        
        boolean withHeader = text != ""; 
        
        if (Preferences.getBGImage(context, appWidgetId) == Preferences.BG_BLACK) {        	
            views.setImageViewResource(R.id.backgroundImg, withHeader ? R.drawable.background_dark_header : R.drawable.background_dark);
            views.setTextColor(R.id.group_caption, Color.WHITE);
        }
        else { 
        	views.setImageViewResource(R.id.backgroundImg, withHeader ? R.drawable.background_light_header : R.drawable.background_light);
        	views.setTextColor(R.id.group_caption, Color.BLACK);
        }
        
        
        // First set the display label
        views.setTextViewText(R.id.group_caption, text);
        // and if it is empty hide it
        views.setViewVisibility(R.id.group_caption, withHeader ? View.VISIBLE : View.GONE);
        
        AppWidgetManager awm = AppWidgetManager.getInstance(context);
        awm.updateAppWidget(appWidgetId, views); 
	}
	
	public int getMainLayoutId(Context aContext, int aAppWidgetId)
	{
		return R.layout.main;
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {
		final String action = intent.getAction();
		Log.d(TAG, "recieved -> " +  action);
		if (AppWidgetManager.ACTION_APPWIDGET_DELETED.equals(action)) {
			final int appWidgetId = intent.getExtras().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
					AppWidgetManager.INVALID_APPWIDGET_ID);
			if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
				this.onDeleted(context, new int[] { appWidgetId });
			}
		} else if (TextUtils.equals(action, LauncherIntent.Action.ACTION_READY)) {
			// Receive ready signal
			Log.d(TAG, "widget ready");
			onAppWidgetReady(context, intent);
		} else if (TextUtils.equals(action, LauncherIntent.Action.ACTION_FINISH)) {

		} else if (TextUtils.equals(action, LauncherIntent.Action.ACTION_ITEM_CLICK)) {
			// onItemClickListener
			onClick(context, intent);
		} else if (TextUtils.equals(action, LauncherIntent.Action.ACTION_VIEW_CLICK)) {
			// onClickListener
			onClick(context, intent);
		} else if (TextUtils.equals(action, LauncherIntent.Error.ERROR_SCROLL_CURSOR)) {
			// An error occurred
		    Log.d(TAG, intent.getStringExtra(LauncherIntent.Extra.EXTRA_ERROR_MESSAGE));
		} else
			super.onReceive(context, intent);
	}

	@Override
	public void onDeleted(Context context, int[] appWidgetIds) {
		super.onDeleted(context, appWidgetIds);
		// Drop the settings if the widget is deleted
		Preferences.DropSettings(context, appWidgetIds);
	}	
	
	/**
	 * On click of a child view in an item
	 */
	private void onClick(Context context, Intent intent) {
		Log.d(TAG, "starting onClick");
		int appWidgetId = intent.getExtras().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
		Log.d(TAG, "got appWidgetId: "+ appWidgetId);
		// Get the ItemIds from the Intent
		// Provided by the DataProvider in the format: 
		// "ContactID\r\nLookupKey"
		String itemId = intent.getStringExtra(LauncherIntent.Extra.Scroll.EXTRA_ITEM_POS);
		Log.d(TAG, "item position: "+ itemId);
		String[] ids = itemId.split("\r\n");
		Log.d(TAG, "itemIDs count:" + ids == null ? "NULL" : String.valueOf(ids.length));
		int viewId = intent.getIntExtra(LauncherIntent.Extra.EXTRA_VIEW_ID, -1);
		Log.d(TAG, "viewId: "+viewId);
		
		if (viewId == R.id.photo) {			
			Rect r;
			if (intent.hasExtra(LauncherIntent.Extra.Scroll.EXTRA_SOURCE_BOUNDS)) {
				Log.d(TAG, "got rect from launcher");
				r = (Rect)intent.getParcelableExtra(LauncherIntent.Extra.Scroll.EXTRA_SOURCE_BOUNDS);
			} else { // Fallback for older launcher versions			
				r = new Rect();
				Log.d(TAG, "determine display size");
				DisplayMetrics dm = context.getResources().getDisplayMetrics();
				r.right = dm.widthPixels;
				r.bottom = dm.heightPixels;
				r.top = 0;
				r.left = 0;
				Log.d(TAG, "displaysizerect: "+r);
			}
			try
			{
				Log.d(TAG, "building URI");
				Uri uri = ContactsContract.Contacts.CONTENT_LOOKUP_URI.buildUpon().appendPath(ids[1]).appendPath(ids[0]).build();
				Log.d(TAG, "lookup URI: "+uri);
				QuickContact.showQuickContact(context,r , 
						uri, 
						Preferences.getQuickContactSize(context, appWidgetId), null);
				Log.d(TAG, "quickcontact should now be visible!");
			}
			catch(ActivityNotFoundException expt)
			{ // 2.1 is foobar...
				Log.w(TAG, "QuickContact failed!");
				Uri uri = ContactsContract.Contacts.CONTENT_URI.buildUpon().appendEncodedPath(ids[0]).build();
				Log.d(TAG, "alternative lookupuri: "+uri);
				Intent launch = new Intent(Intent.ACTION_VIEW, uri);
				launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				Log.d(TAG, "will start contact activity");
				context.startActivity(launch);			
				Log.d(TAG, "contact activity launched");
			}
			
		}
	}
	
	
	/**
	 * Receive ready intent from Launcher, prepare scroll view resources
	 */
	public void onAppWidgetReady(Context context, Intent intent) {
		if (intent == null)
			return;

		int appWidgetId = intent.getExtras().getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
				AppWidgetManager.INVALID_APPWIDGET_ID);

		if (appWidgetId < 0) {
			return;
		}
		updateGroupTitle(context, appWidgetId);
		Intent replaceDummy = CreateMakeScrollableIntent(context, appWidgetId);

		// Send it out
		context.sendBroadcast(replaceDummy);
	}
	
	public Intent CreateMakeScrollableIntent(Context context, int appWidgetId) {
		Log.d(TAG, "creating ACTION_SCROLL_WIDGET_START intent");
		Intent result = new Intent(LauncherIntent.Action.ACTION_SCROLL_WIDGET_START);

		// Put widget info
		result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		result.putExtra(LauncherIntent.Extra.EXTRA_VIEW_ID, R.id.widget_content);

		result.putExtra(LauncherIntent.Extra.Scroll.EXTRA_DATA_PROVIDER_ALLOW_REQUERY, true);

		// Give a layout resource to be inflated. If this is not given, the launcher will create one
		result.putExtra(LauncherIntent.Extra.Scroll.EXTRA_LISTVIEW_LAYOUT_ID, R.layout.listview);
		result.putExtra(LauncherIntent.Extra.Scroll.EXTRA_ITEM_LAYOUT_ID, getListEntryLayoutId(context, appWidgetId));
		
		putProvider(result, DataProvider.CONTENT_URI_MESSAGES.buildUpon().appendEncodedPath(
				Integer.toString(appWidgetId)).toString());
		putMapping(context, appWidgetId, result);

		// Launcher can set onClickListener for each children of an item. Without
		// explicitly put this
		// extra, it will just set onItemClickListener by default
		result.putExtra(LauncherIntent.Extra.Scroll.EXTRA_ITEM_CHILDREN_CLICKABLE, true);
		return result;
	}
	
	public abstract int getListEntryLayoutId(Context aContext, int aAppWidgetId);
	
	/**
	 * Put provider info as extras in the specified intent
	 * 
	 * @param intent
	 */
	public void putProvider(Intent intent, String widgetUri) {
		if (intent == null)
			return;

		String whereClause = null;
		String orderBy = null;
		String[] selectionArgs = null;

		// Put the data uri in as a string. Do not use setData, Home++ does not
		// have a filter for that
		intent.putExtra(LauncherIntent.Extra.Scroll.EXTRA_DATA_URI, widgetUri);

		// Other arguments for managed query
		intent.putExtra(LauncherIntent.Extra.Scroll.EXTRA_PROJECTION, DataProvider.PROJECTION_APPWIDGETS);
		intent.putExtra(LauncherIntent.Extra.Scroll.EXTRA_SELECTION, whereClause);
		intent.putExtra(LauncherIntent.Extra.Scroll.EXTRA_SELECTION_ARGUMENTS, selectionArgs);
		intent.putExtra(LauncherIntent.Extra.Scroll.EXTRA_SORT_ORDER, orderBy);

	}

	/**
	 * Put mapping info as extras in intent
	 */
	public void putMapping(Context context, int appWidgetId, Intent intent) {
		if (intent == null)
			return;

		int NB_ITEMS_TO_FILL = 2;
		if (!Preferences.getShowName(context, appWidgetId))
			NB_ITEMS_TO_FILL = 1;

		int[] cursorIndices = new int[NB_ITEMS_TO_FILL];
		int[] viewTypes = new int[NB_ITEMS_TO_FILL];
		int[] layoutIds = new int[NB_ITEMS_TO_FILL];
		boolean[] clickable = new boolean[NB_ITEMS_TO_FILL];
		int[] defResources = new int[NB_ITEMS_TO_FILL];

		int iItem = 0;
		
		intent.putExtra(LauncherIntent.Extra.Scroll.EXTRA_ITEM_ACTION_VIEW_URI_INDEX, 
				DataProvider.DataProviderColumns.lookupkey.ordinal());
		
		cursorIndices[iItem] = DataProvider.DataProviderColumns.photo.ordinal();
		viewTypes[iItem] = LauncherIntent.Extra.Scroll.Types.IMAGEBLOB;
		layoutIds[iItem] = R.id.photo;
		clickable[iItem] = true;
		defResources[iItem] = R.drawable.no_image;

		if (Preferences.getShowName(context, appWidgetId)) {
			iItem++;
			
			cursorIndices[iItem] = DataProvider.DataProviderColumns.name.ordinal();
			viewTypes[iItem] = LauncherIntent.Extra.Scroll.Types.TEXTVIEW;
			layoutIds[iItem] = R.id.displayname;
			clickable[iItem] = false;
			defResources[iItem] = 0;
		}

		intent.putExtra(LauncherIntent.Extra.Scroll.Mapping.EXTRA_VIEW_IDS, layoutIds);
		intent.putExtra(LauncherIntent.Extra.Scroll.Mapping.EXTRA_VIEW_TYPES, viewTypes);
		intent.putExtra(LauncherIntent.Extra.Scroll.Mapping.EXTRA_VIEW_CLICKABLE, clickable);
		intent.putExtra(LauncherIntent.Extra.Scroll.Mapping.EXTRA_CURSOR_INDICES, cursorIndices);
		intent.putExtra(LauncherIntent.Extra.Scroll.Mapping.EXTRA_DEFAULT_RESOURCES, defResources);
	}
    
}