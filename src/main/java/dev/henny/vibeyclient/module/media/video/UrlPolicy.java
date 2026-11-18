package dev.henny.vibeyclient.module.media.video;

import java.net.InetAddress;
import java.net.URI;
import java.util.Locale;

/**
 * What we accept from OTHER players. Only http(s) URLs that point at a public host: no local files, no other
 * schemes, no user:password@, no localhost / private / link-local addresses (another player must not be able to
 * make our game probe our LAN). Own screens are not filtered: the user typed the address themselves.
 */
public final class UrlPolicy {
    private UrlPolicy() {}

    public static boolean acceptRemote(MediaSource source) {
        if (source == null || source.kind() == MediaSource.Kind.FILE) return false;
        if (source.uri().length() > MediaSource.MAX_URI) return false;
        URI u;
        try {
            u = URI.create(source.uri());
        } catch (IllegalArgumentException e) {
            return false;
        }
        String scheme = u.getScheme() == null ? "" : u.getScheme().toLowerCase(Locale.ROOT);
        if (!scheme.equals("http") && !scheme.equals("https")) return false;
        if (u.getRawUserInfo() != null) return false;
        String host = u.getHost();
        if (host == null || host.isEmpty()) return false;
        return publicHost(host.toLowerCase(Locale.ROOT));
    }

    static boolean publicHost(String host) {
        if (host.startsWith("[") && host.endsWith("]")) host = host.substring(1, host.length() - 1);
        if (host.equals("localhost") || host.endsWith(".localhost") || host.endsWith(".local") || host.endsWith(".lan")
                || host.endsWith(".internal") || host.endsWith(".home.arpa") || !host.contains(".") && !host.contains(":")) {
            return false;
        }
        if (looksLikeIp(host)) {
            try {
                // A literal is parsed without a DNS lookup.
                InetAddress a = InetAddress.getByName(host);
                return !(a.isLoopbackAddress() || a.isAnyLocalAddress() || a.isLinkLocalAddress() || a.isSiteLocalAddress()
                        || a.isMulticastAddress() || isUniqueLocalV6(a) || isCgnat(a));
            } catch (Exception e) {
                return false;
            }
        }
        return true;
    }

    private static boolean looksLikeIp(String host) {
        return host.contains(":") || host.chars().allMatch(c -> c == '.' || c >= '0' && c <= '9');
    }

    private static boolean isUniqueLocalV6(InetAddress a) {
        byte[] b = a.getAddress();
        return b.length == 16 && (b[0] & 0xFE) == 0xFC;
    }

    private static boolean isCgnat(InetAddress a) {
        byte[] b = a.getAddress();
        return b.length == 4 && (b[0] & 0xFF) == 100 && (b[1] & 0xC0) == 64;
    }
}
