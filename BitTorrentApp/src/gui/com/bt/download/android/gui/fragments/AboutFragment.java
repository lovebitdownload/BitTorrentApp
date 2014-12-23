
package com.bt.download.android.gui.fragments;

import com.bt.download.android.R;

import org.apache.commons.io.IOUtils;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author gubatron
 * @author aldenml
 * 
 */
public class AboutFragment extends Fragment implements MainFragment {


    public AboutFragment() {
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_about, container, false);

        TextView title = (TextView) view.findViewById(R.id.fragment_about_title);
        String appVersionInfo = getString(R.string.application_version_info);
        appVersionInfo = String.format(appVersionInfo, getString(R.string.application_label));
        title.setText(appVersionInfo);

        TextView content = (TextView) view.findViewById(R.id.fragment_about_content);
        content.setText(Html.fromHtml(getAboutText()));
        content.setMovementMethod(LinkMovementMethod.getInstance());

        return view;
    }

    @Override
    public View getHeader(Activity activity) {
        LayoutInflater inflater = LayoutInflater.from(activity);
        TextView header = (TextView) inflater.inflate(R.layout.view_main_fragment_simple_header, null);
        header.setText(R.string.about);

        return header;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private String getAboutText() {
        try {
            InputStream raw = getResources().openRawResource(R.raw.about);
            return IOUtils.toString(raw, "UTF-8");
        } catch (IOException e) {
            return "";
        }
    }
}