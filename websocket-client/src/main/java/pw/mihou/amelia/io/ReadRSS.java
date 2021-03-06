package pw.mihou.amelia.io;

import com.apptastic.rssreader.RssReader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import pw.mihou.amelia.Amelia;
import pw.mihou.amelia.wrappers.ItemWrapper;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class ReadRSS {

    private static final RssReader reader = new RssReader().setUserAgent("Amelia/1.0r1 (Language=Java/1.8, Developer=Shindou Mihou)");
    private static final LoadingCache<String, ItemWrapper> feeds = Caffeine.newBuilder()
            .expireAfterWrite(2, TimeUnit.MINUTES).refreshAfterWrite(9, TimeUnit.MINUTES)
            .build(key -> request(key).orElse(null));

    private static Optional<ItemWrapper> request(String url) {
        try {
            return reader.read(url).findFirst().map(ItemWrapper::new);
        } catch (IOException exception) {
            try {
                Amelia.log.error("Unable to connect to {}: {}", url, exception.getMessage());
                Amelia.log.info("Attempting to reconnect to {} in 2 seconds...", url);
                Thread.sleep(2 * 1000);
                return retry(url, 1);
            } catch(InterruptedException e) {
                Amelia.log.error("Thread was interrupted exception while attempting to retry {} for {} bucket: {}", url, 1, e.getMessage());
                return Optional.empty();
            }
        }
    }

    public static Optional<ItemWrapper> getLatest(String url) {
        return Optional.ofNullable(feeds.get(url));
    }

    private static Optional<ItemWrapper> retry(String url, int i) {
        if (i < 10) {
            int bucket = i * 1000;
            try {
                return reader.read(url)
                        .findFirst().map(ItemWrapper::new);
            } catch (IOException exception) {
                try {
                    Amelia.log.error("Unable to connect to {}: {}", url, exception.getMessage());
                    Amelia.log.info("Attempting to reconnect to {} in {} seconds...", url, bucket);
                    Thread.sleep(bucket);
                    return retry(url, i + 1);
                } catch (InterruptedException e) {
                    Amelia.log.error("Thread was interrupted exception while attempting to retry {} for {} bucket: {}", url, i, e.getMessage());
                    return Optional.empty();
                }
            }
        } else {
            Amelia.log.error("Failed to connect to {} after 10 attempts, sending error.", url);
            return Optional.empty();
        }
    }


}
