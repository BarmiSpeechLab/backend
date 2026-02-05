import java.util.List;

public record AiIpaReport(
    String summary,
    List<WeakPoint> weakPoints,
    List<String> recommendations
) {
    public record WeakPoint(String ipa, String reason) {}
}
