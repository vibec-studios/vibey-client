package dev.henny.cosmetico.host;

import dev.henny.cosmetico.model.Bundle;

public interface BundleResources {
    void prepare(Bundle b);

    void evict(Bundle b);

    void prefetchSounds(Bundle b);

    BundleResources NONE = new BundleResources() {
        @Override
        public void prepare(Bundle b) {}

        @Override
        public void evict(Bundle b) {}

        @Override
        public void prefetchSounds(Bundle b) {}
    };
}
