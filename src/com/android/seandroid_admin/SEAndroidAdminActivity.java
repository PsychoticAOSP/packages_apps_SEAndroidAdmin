package com.android.seandroid_admin;

import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.android.seandroid_admin.R;

import java.util.ArrayList;
import java.util.List;

public class SEAndroidAdminActivity extends PreferenceActivity {

    private List<Header> mHeaders;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return "com.android.seandroid_admin.AboutFragment".equals(fragmentName) ||
            "com.android.seandroid_admin.ConfigUpdateFragment".equals(fragmentName) ||
            "com.android.seandroid_admin.appops.AppOpsSummary".equals(fragmentName) ||
            "com.android.seandroid_admin.appops.AppOpsDetails".equals(fragmentName);
    }

    @Override
    public void onBuildHeaders(List<Header> headers) {
        loadHeadersFromResource(R.xml.enabled_headers, headers);
        updateHeaderList(headers);
    }

    private void updateHeaderList(List<Header> target) {
        //TODO maybe enable or disable headers here based on whether were device admin?
        // if selinux is disabled then we should gray out the selinux reload option
    }

    @Override
    public PreferenceActivity.Header onGetInitialHeader() {
        Header h = new PreferenceActivity.Header();
        //h.fragment = SELinuxEnforcingFragment.class.getCanonicalName();
        h.fragment = ConfigUpdateFragment.class.getCanonicalName();
        return h;
    }

    private static class HeaderViewHolder {
        TextView title;
        TextView summary;
    }

    private static class HeaderAdapter extends ArrayAdapter<Header> {
        static final int HEADER_TYPE_CATEGORY = 0;
        static final int HEADER_TYPE_NORMAL = 1;
        private static final int HEADER_TYPE_COUNT = HEADER_TYPE_NORMAL + 1;

        private LayoutInflater mInflater;

        static int getHeaderType(Header header) {
            int id = (int) header.id; // ids are integers, so downcast is okay
            switch (id) {
                case R.id.update_category:
                case R.id.about_category:
                    return HEADER_TYPE_CATEGORY;
                default:
                    return HEADER_TYPE_NORMAL;
            }
        }

        @Override
        public int getItemViewType(int position) {
            Header header = getItem(position);
            return getHeaderType(header);
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false; // because of categories
        }

        @Override
        public boolean isEnabled(int position) {
            return getItemViewType(position) != HEADER_TYPE_CATEGORY;
        }

        @Override
        public int getViewTypeCount() {
            return HEADER_TYPE_COUNT;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        public HeaderAdapter(Context context, List<Header> objects) {
            super(context, 0, objects);
            mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            HeaderViewHolder holder;
            final Header header = getItem(position);
            int headerType = getHeaderType(header);
            View view = null;

            if (convertView == null) {
                // New view, so start inflating views
                holder = new HeaderViewHolder();
                switch (headerType) {
                    case HEADER_TYPE_CATEGORY:
                        view = new TextView(getContext(), null,
                                android.R.attr.listSeparatorTextViewStyle);
                        holder.title = (TextView) view;
                        break;

                    case HEADER_TYPE_NORMAL:
                        view = mInflater.inflate(R.layout.preference_header_item, parent,
                                false);
                        holder.title = (TextView) view.findViewById(
                                com.android.internal.R.id.title);
                        holder.summary = (TextView) view.findViewById(
                                com.android.internal.R.id.summary);
                        break;
                }

                view.setTag(holder);
            } else {
                view = convertView;
                holder = (HeaderViewHolder) view.getTag();
            }

            // All view fields must be updated every time, because the view may be recycled
            switch (headerType) {
                case HEADER_TYPE_CATEGORY:
                    holder.title.setText(header.getTitle(getContext().getResources()));
                    break;

                case HEADER_TYPE_NORMAL:
                    holder.title.setText(header.getTitle(getContext().getResources()));
                    CharSequence summary = header.getSummary(getContext().getResources());
                    if (!TextUtils.isEmpty(summary)) {
                        holder.summary.setVisibility(View.VISIBLE);
                        holder.summary.setText(summary);
                    } else {
                        holder.summary.setVisibility(View.GONE);
                    }
            }

            return view;
        }
    }

    @Override
    public void setListAdapter(ListAdapter adapter) {
        if (mHeaders == null) {
            mHeaders = new ArrayList<Header>();
            for (int i = 0; i < adapter.getCount(); i++) {
                mHeaders.add((Header) adapter.getItem(i));
            }
        }
        super.setListAdapter(new HeaderAdapter(this, mHeaders));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onResume() {
        super.onResume();
    }
}
