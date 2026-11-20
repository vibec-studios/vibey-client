package dev.henny.cosmetico.golden;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.util.stream.Stream;

class ParseGoldenTest {
    @TestFactory
    Stream<DynamicTest> everyBundleParsesLikeTheOldEngine() {
        return Golden.bundles().stream().map(f -> DynamicTest.dynamicTest(f, () ->
                GoldenCompare.exact("parse/" + Golden.name(f) + ".txt", ParseDump.dump(Golden.read(f), GoldenVersion.gate()))));
    }
}
