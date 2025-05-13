package wtf.demise.utils.misc;

import com.github.javafaker.Faker;

import java.util.concurrent.ThreadLocalRandom;

public class StringUtils {
    public static final String sb = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_";

    public static String ticksToElapsedTime(int ticks) {
        int i = ticks / 20;
        int j = i / 60;
        i = i % 60;
        return i < 10 ? j + ":0" + i : j + ":" + i;
    }

    public static String upperSnakeCaseToPascal(String string) {
        return string.charAt(0) + string.substring(1).toLowerCase();
    }

    public static String randomString(String pool, int length) {
        final StringBuilder builder = new StringBuilder();

        for (int i = 0; i < length; i++) {
            builder.append(pool.charAt(ThreadLocalRandom.current().nextInt(0, pool.length())));
        }

        return builder.toString();
    }

    public static String randomName(FakerMode fakerMode) {
        Faker faker = new Faker();

        return switch (fakerMode) {
            case NORMAL -> faker.name().fullName().replaceAll("[ .\\-,']", "");
            case FUNNY -> faker.funnyName().name().replace(" ", "").replaceAll("[ .\\-,']", "");
            case IM_FUCKING_BORED ->
                    (faker.country().name() + faker.country().capital()).replace(" ", "").replaceAll("[ .\\-,']", "");
        };
    }

    public enum FakerMode {
        NORMAL,
        FUNNY,
        IM_FUCKING_BORED
    }

    public static String capitalizeWords(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        String[] words = input.split("\\s");

        StringBuilder result = new StringBuilder();

        for (String word : words) {
            result.append(Character.toUpperCase(word.charAt(0)))
                    .append(word.substring(1).toLowerCase())
                    .append(" ");
        }

        return result.toString().trim();
    }
}