package com.mystictreegames.ui.newstickerview;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;

public class NewsTickerWidgetProvider extends AppWidgetProvider {
	//private static final String ACTION_CLICK = "ACTION_CLICK";

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		
		/*
		// Get all ids
		ComponentName thisWidget = new ComponentName(context,
				NewsTickerWidgetProvider.class);
		int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);
		for (int widgetId : allWidgetIds) {
			// Create some random data
			int number = (new Random().nextInt(100));

			RemoteViews remoteViews = new RemoteViews(context.getPackageName(),
					R.layout.news_ticker_widget);
			Log.w("WidgetExample", String.valueOf(number));
			// Set the text
			remoteViews.setTextViewText(R.id.update, String.valueOf(number));

			// Register an onClickListener
			Intent intent = new Intent(context, NewsTickerWidgetProvider.class);

			intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
			intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);

			PendingIntent pendingIntent = PendingIntent.getBroadcast(context,
					0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
			remoteViews.setOnClickPendingIntent(R.id.update, pendingIntent);
			appWidgetManager.updateAppWidget(widgetId, remoteViews);
		}
		*/
	}
}
