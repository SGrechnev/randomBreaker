import java.util.Random;

public class OtpGenerator {

    private final Random rnd = new Random();
    private final int limit = 1_000_000;

    public Random getRnd() {
        return rnd;
    }

    public int getLimit() {
        return limit;
    }

    public int generateOtp() {
        return rnd.nextInt(limit);
    }
}
