
package de.bbukowski.notifcount;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by bbukowski on 07.08.14. Maintained by woalk since 2015/05/05.
 */
public class AppListActivity extends ListActivity {

  private static SettingsHelper mSettingsHelper;
  private static AppListAdapter mAdapter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    mSettingsHelper = new SettingsHelper(this);
    new LoadAppsInfoTask().execute();
    getActionBar().setDisplayHomeAsUpEnabled(true);
    getActionBar().setTitle(R.string.pref_apps_increase_onupdate_title);
    
    this.getListView().setFastScrollEnabled(true);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == android.R.id.home)
      onBackPressed();
    return true;
  }

  private static class AppInfo {
    String title;
    String summary;
    Drawable icon;
    boolean enabled;
    boolean extract;
  }

  private List<AppInfo> loadApps(ProgressDialog dialog) {
    PackageManager packageManager = getPackageManager();
    List<ApplicationInfo> packages = packageManager
        .getInstalledApplications(PackageManager.GET_META_DATA);
    List<AppInfo> apps = new ArrayList<AppInfo>();

    dialog.setMax(packages.size());
    int i = 1;

    for (ApplicationInfo app : packages) {
      AppInfo appInfo = new AppInfo();
      appInfo.title = (String) app.loadLabel(packageManager);
      appInfo.summary = app.packageName;
      appInfo.icon = app.loadIcon(packageManager);
      appInfo.enabled = mSettingsHelper.isListed(app.packageName);
      appInfo.extract = mSettingsHelper.isListedExtract(app.packageName);
      apps.add(appInfo);
      dialog.setProgress(i++);
    }

    Collections.sort(apps, new Comparator<AppInfo>() {
      @Override
      public int compare(AppInfo appInfo1, AppInfo appInfo2) {
        boolean app1 = mSettingsHelper.isListed(appInfo1.summary);
        boolean app2 = mSettingsHelper.isListed(appInfo2.summary);

        if (app1 && app2 || !app1 && !app2) {
          return appInfo1.title.compareToIgnoreCase(appInfo2.title);
        } else if (app1) {
          return -1;
        } else if (app2) {
          return 1;
        }

        return 0;
      }
    });

    return apps;
  }

  private class LoadAppsInfoTask extends AsyncTask<Void, Void, Void> {
    ProgressDialog dialog;
    List<AppInfo> appInfos;

    @Override
    protected void onPreExecute() {
      dialog = new ProgressDialog(AppListActivity.this);
      dialog.setMessage(getString(R.string.loading));
      dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
      dialog.setCancelable(false);
      dialog.show();
    }

    @Override
    protected Void doInBackground(Void... params) {
      appInfos = loadApps(dialog);
      return null;
    }

    @Override
    protected void onProgressUpdate(Void... progress) {
      mAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onPostExecute(Void void_) {
      super.onPostExecute(void_);
      mAdapter = new AppListAdapter(AppListActivity.this, appInfos);
      setListAdapter(mAdapter);
      dialog.dismiss();
    }
  }

  private static class AppListAdapter extends ArrayAdapter<AppInfo> {
    LayoutInflater mInflater;

    public AppListAdapter(Context context, List<AppInfo> items) {
      super(context, 0, items);
      mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    private static class Holder {
      ImageView icon;
      TextView title;
      TextView summary;
      CheckBox checkbox;
      LinearLayout itemlayout;
      RadioGroup radioG;
      RadioButton radio0;
      RadioButton radio1;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      final Holder holder;
      final AppInfo item = getItem(position);
      View view;

      if (convertView == null) {
        holder = new Holder();
        view = mInflater.inflate(R.layout.whitelist_item, parent, false);
        holder.icon = (ImageView) view.findViewById(R.id.icon);
        holder.title = (TextView) view.findViewById(android.R.id.title);
        holder.summary = (TextView) view.findViewById(android.R.id.summary);
        holder.checkbox = (CheckBox) view.findViewById(R.id.checkbox);
        holder.itemlayout = (LinearLayout) view.findViewById(R.id.itemlayout);
        holder.radioG = (RadioGroup) view.findViewById(R.id.radioG);
        holder.radio0 = (RadioButton) view.findViewById(R.id.radio0);
        holder.radio1 = (RadioButton) view.findViewById(R.id.radio1);
        view.setTag(holder);
      } else {
        view = convertView;
        holder = (Holder) view.getTag();
        holder.radioG.setVisibility(View.GONE);
      }

      holder.title.setText(item.title);
      holder.summary.setText(item.summary);
      holder.icon.setImageDrawable(item.icon);

      holder.checkbox.setOnCheckedChangeListener(null);
      holder.checkbox.setChecked(item.enabled);
      holder.checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
          item.enabled = isChecked;
          if (isChecked) {
            mSettingsHelper.addListItem(item.summary);
          } else {
            mSettingsHelper.removeListItem(item.summary);
            holder.radio0.setChecked(false);
            holder.radio1.setChecked(true);
          }
        }
      });

      holder.itemlayout.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          int vis = holder.radioG.getVisibility();
          holder.radioG.setVisibility(vis == View.GONE ? View.VISIBLE : View.GONE);
        }
      });

      View.OnClickListener radioClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          holder.checkbox.setChecked(true);
        }
      };
      holder.radio0.setOnClickListener(radioClick);
      holder.radio1.setOnClickListener(radioClick);

      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT)
        holder.radio0.setVisibility(View.GONE);

      holder.radio0.setOnCheckedChangeListener(null);
      holder.radio0.setChecked(item.extract);
      holder.radio1.setChecked(!item.extract);
      holder.radio0.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
          item.extract = isChecked;
          if (isChecked) {
            mSettingsHelper.addListItemExtract(item.summary);
          } else {
            mSettingsHelper.removeListItemExtract(item.summary);
          }
        }
      });

      return view;
    }
  }
}
