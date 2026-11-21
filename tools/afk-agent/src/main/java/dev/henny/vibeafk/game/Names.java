package dev.henny.vibeafk.game;

import java.util.Collections;
import java.util.List;

public interface Names {
    String kind();

    String cls(String moj);

    String mojOf(String runtime);

    String field(String mojOwner, String mojField);

    List<Member> methods(String mojOwner, String mojName);

    final class Member {
        public final String runtimeName;
        public final String[] runtimeParams;

        public Member(String runtimeName, String[] runtimeParams) {
            this.runtimeName = runtimeName;
            this.runtimeParams = runtimeParams;
        }
    }

    Names IDENTITY = new Names() {
        @Override
        public String kind() {
            return "mojang";
        }

        @Override
        public String cls(String moj) {
            return moj;
        }

        @Override
        public String mojOf(String runtime) {
            return runtime;
        }

        @Override
        public String field(String mojOwner, String mojField) {
            return mojField;
        }

        @Override
        public List<Member> methods(String mojOwner, String mojName) {
            return Collections.singletonList(new Member(mojName, null));
        }
    };
}
