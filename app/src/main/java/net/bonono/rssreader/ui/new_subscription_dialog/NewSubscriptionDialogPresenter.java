package net.bonono.rssreader.ui.new_subscription_dialog;

import net.bonono.rssreader.domain_logic.rss.Feed;
import net.bonono.rssreader.entity.Entry;
import net.bonono.rssreader.entity.Site;
import net.bonono.rssreader.repository.Repository;
import net.bonono.rssreader.repository.realm.EntryRepository;
import net.bonono.rssreader.repository.realm.RealmRepository;
import net.bonono.rssreader.repository.realm.SiteRepository;

import io.reactivex.annotations.NonNull;
import io.reactivex.observers.DisposableObserver;
import io.realm.Realm;

public class NewSubscriptionDialogPresenter implements NewSubscriptionDialogContract.Presenter {
    private NewSubscriptionDialogContract.View mView;
    private Repository<Site> mSiteRepo;
    private Repository<Entry> mEntryRepo;
    private Feed mLastSearched;

    public NewSubscriptionDialogPresenter(NewSubscriptionDialogContract.View view) {
        mView = view;

        Realm realm = Realm.getDefaultInstance();
        mSiteRepo = new SiteRepository(realm);
        mEntryRepo = new EntryRepository(realm);
    }

    @Override
    public void search(String url) {
        mView.showLoading(true);

        mView.bindLifeCycleAndScheduler(Feed.search(url))
                .subscribe(new DisposableObserver<Feed>() {
                    @Override
                    public void onNext(@NonNull Feed feed) {
                        mLastSearched = feed;
                        mView.completeToSearch(feed);
                        mView.showLoading(false);
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        mView.failedToSearch();
                        mView.showLoading(false);
                    }

                    @Override
                    public void onComplete() { }
                });
    }

    @Override
    public void addLastSearched() {
        if(mSiteRepo.count(new SiteRepository.SameUrl(mLastSearched.getSite().getUrl())) > 0) {
            mView.duplicated();
        } else {
            mSiteRepo.transaction(() -> {
                Site saved = mSiteRepo.save(mLastSearched.getSite());
                for (Entry e : mLastSearched.getEntries()) {
                    e.belongTo(saved);
                    mEntryRepo.save(e);
                }
            });
            mView.completeToSubscribe();
        }

        mLastSearched = null;
    }
}
