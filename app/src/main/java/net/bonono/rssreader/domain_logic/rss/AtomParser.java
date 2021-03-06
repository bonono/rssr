package net.bonono.rssreader.domain_logic.rss;

import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;

import net.bonono.rssreader.domain_logic.xml.XmlDefinition;
import net.bonono.rssreader.domain_logic.xml.XmlResult;
import net.bonono.rssreader.entity.Entry;
import net.bonono.rssreader.entity.Site;

import org.threeten.bp.LocalDateTime;
import org.threeten.bp.format.DateTimeFormatter;
import org.threeten.bp.format.DateTimeParseException;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

public class AtomParser implements FeedParser {
    private XmlDefinition mDef;

    public AtomParser() {
        XmlDefinition entry = XmlDefinition.forTag("entry");
        entry.nest(XmlDefinition.forText("title"));
        entry.nest(XmlDefinition.forText("link"));
        entry.nest(XmlDefinition.forText("summary"));
        entry.nest(XmlDefinition.forText("issued"));
        entry.nest(XmlDefinition.forText("published"));
        //entry.nest(XmlDefinition.forText("modified"));
        //entry.nest(XmlDefinition.forText("updated"));

        XmlDefinition feed = XmlDefinition.forTag("feed");
        feed.nest(XmlDefinition.forText("title"));
        feed.nest(XmlDefinition.forText("link"));
        feed.nest(XmlDefinition.forText("tagline"));
        feed.nest(XmlDefinition.forText("subtitle"));
        feed.nest(entry);

        mDef = XmlDefinition.rootOf(feed);
    }

    @Override
    public Feed parse(String xml) throws XmlPullParserException, IOException {
        Feed feed = new Feed();

        try (ByteArrayInputStream ba = new ByteArrayInputStream(xml.getBytes())) {
            XmlResult fd = mDef.parse(ba).get("feed");

            Site site = new Site();
            site.setTitle(fd.get("title").getText());
            site.setDescription(fd.get("tagline").getText());
            if (TextUtils.isEmpty(site.getDescription())) {
                site.setDescription(fd.get("subtitle").getText());
            }

            site.setUrl(selectLink(fd.getList("link")));
            feed.setSite(site);

            for (XmlResult e : fd.getList("entry")) {
                Entry entry = new Entry();

                entry.setTitle(e.get("title").getText());
                entry.setUrl(selectLink(e.getList("link")));
                entry.setDescription(e.get("summary").getText());

                entry.setCreatedAt(parseDate(e.get("issued").getText()));
                if (entry.getCreatedAt() == null) {
                    entry.setCreatedAt(parseDate(e.get("published").getText()));
                }

                // TODO: set updatedAt

                feed.addEntry(entry);
            }
        }

        return feed;
    }

    private static String selectLink(List<XmlResult> links) {
        for (XmlResult link : links) {
            if (link.getAttr("rel") != null && link.getAttr("rel").equals("alternate")) {
                return link.getAttr("href");
            }
        }

        return null;
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    public static LocalDateTime parseDate(String date) {
        if (TextUtils.isEmpty(date)) {
            return null;
        }

        try {
            return LocalDateTime.parse(date, DateTimeFormatter.ISO_INSTANT);
        } catch (DateTimeParseException e) {
            // ignore failing to parse
        }

        try {
            return LocalDateTime.parse(date, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        } catch (DateTimeParseException e) {
            // ignore failing to parse
        }

        return null;
    }
}
