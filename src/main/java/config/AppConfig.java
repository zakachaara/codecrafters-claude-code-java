package config;

public record AppConfig(String apiKey, String baseUrl, String model) {
    private static final String DEFAULT_BASE_URL = "https://openrouter.ai/api/v1";
    private static final String DEFAULT_MODEL = "anthropic/claude-haiku-4.5";

    public static AppConfig fromEnvironment() {
        String apiKey = System.getenv("OPENROUTER_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OPENROUTER_API_KEY is not set");
        }

        String baseUrl = System.getenv("OPENROUTER_BASE_URL");
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = DEFAULT_BASE_URL;
        }

        return new AppConfig(apiKey, baseUrl, DEFAULT_MODEL);
    }
}
