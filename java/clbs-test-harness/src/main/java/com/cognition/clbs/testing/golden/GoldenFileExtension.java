package com.cognition.clbs.testing.golden;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

/**
 * JUnit 5 extension that injects a {@link GoldenFile} into test methods.
 *
 * <pre>{@code
 * @ExtendWith(GoldenFileExtension.class)
 * class RptPos00Test {
 *   @Test
 *   void printsPositionReport(GoldenFile golden) {
 *     golden.assertText(report.render());
 *   }
 * }
 * }</pre>
 *
 * <p>The golden root defaults to {@code src/test/resources/golden} relative to the module's working
 * directory and can be overridden with {@code -Dclbs.golden.dir=<path>}.
 */
public final class GoldenFileExtension implements ParameterResolver {

  public static final String ROOT_PROPERTY = "clbs.golden.dir";
  public static final Path DEFAULT_ROOT = Paths.get("src", "test", "resources", "golden");

  @Override
  public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext context) {
    return parameterContext.getParameter().getType() == GoldenFile.class;
  }

  @Override
  public Object resolveParameter(ParameterContext parameterContext, ExtensionContext context) {
    Class<?> testClass =
        context
            .getTestClass()
            .orElseThrow(
                () -> new ParameterResolutionException("GoldenFile requires a test class context"));
    String baseName =
        context.getTestMethod().map(m -> m.getName()).orElse(testClass.getSimpleName());
    return new GoldenFile(goldenRoot().resolve(testClass.getSimpleName()), baseName);
  }

  static Path goldenRoot() {
    String override = System.getProperty(ROOT_PROPERTY);
    return override == null || override.isBlank() ? DEFAULT_ROOT : Paths.get(override);
  }
}
