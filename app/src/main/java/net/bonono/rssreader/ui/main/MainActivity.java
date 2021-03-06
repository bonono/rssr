package net.bonono.rssreader.ui.main;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.support.customtabs.CustomTabsIntent;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.bonono.rssreader.R;
import net.bonono.rssreader.entity.Entry;
import net.bonono.rssreader.entity.Site;
import net.bonono.rssreader.repository.realm.EntryRepository;
import net.bonono.rssreader.repository.realm.SiteRepository;

import java.util.List;

import io.realm.RealmChangeListener;
import io.realm.RealmModel;

public class MainActivity extends AppCompatActivity implements MainContract.View {
    private MainContract.Presenter mPresenter;
    private Toolbar mToolbar;
    private DrawerLayout mDrawerLayout;
    private TabLayout mTab;
    private ViewPager mPager;
    private EntryListPagerAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPresenter = new MainPresenter(this, new SiteRepository(), new EntryRepository());
        mDrawerLayout = (DrawerLayout)findViewById(R.id.drawer_root);
        mToolbar = (Toolbar)findViewById(R.id.toolbar);
        mTab = (TabLayout)findViewById(R.id.tab);
        mPager = (ViewPager)findViewById(R.id.pager);

        mTab.setupWithViewPager(mPager, true);

        mPresenter.loadDefaultSite();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mPresenter.dispose();
    }

    @Override
    public void show(Site site) {
        closeDrawer();

        mToolbar.setTitle(site.getTitle());

        if (mAdapter == null) {
            mAdapter = new EntryListPagerAdapter(this, getSupportFragmentManager(), site);
            mPager.setAdapter(mAdapter);
        } else {
            mAdapter.updateSite(site);
        }
    }

    @Override
    public void showNoSite() {

    }

    @Override
    public void showEntryDetail(Entry entry) {
        // dummy

        CustomTabsIntent.Builder chromeIntent = new CustomTabsIntent.Builder();
        chromeIntent
                .setToolbarColor(ContextCompat.getColor(this, R.color.colorPrimary))
                .build()
                .launchUrl(this, Uri.parse(entry.getUrl()));
    }

    @Override
    public void closeDrawer() {
        mDrawerLayout.closeDrawers();
    }

    @Override
    public MainContract.Presenter getPresenter() {
        return mPresenter;
    }

    @Override
    public void onClickEntry(Entry entry) {
        mPresenter.activateEntry(entry);
    }

    private static class EntryListPagerAdapter extends FragmentStatePagerAdapter {
        private static final int[] sPageTitles = new int[] {
                R.string.tab_unread,
                R.string.tab_bookmark,
                R.string.tab_all
        };

        private Context mContext;
        private Site mSite;

        EntryListPagerAdapter(Context context, FragmentManager manager, Site site) {
            super(manager);
            mContext = context;
            mSite = site;
        }

        void updateSite(Site site) {
            mSite = site;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return EntryRepository.Filter.values().length;
        }

        @Override
        public Fragment getItem(int position) {
            return EntryListFragment.newInstance(mSite, EntryRepository.Filter.values()[position]);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mContext.getString(sPageTitles[position]);
        }

        @Override
        public int getItemPosition(Object object) {
            return PagerAdapter.POSITION_NONE;
        }
    }
}
