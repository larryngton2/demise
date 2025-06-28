package wtf.demise.utils.misc;

import com.github.javafaker.Faker;
import lombok.experimental.UtilityClass;

import java.util.concurrent.ThreadLocalRandom;

@UtilityClass
public class StringUtils {
    public String upperSnakeCaseToPascal(String string) {
        return string.charAt(0) + string.substring(1).toLowerCase();
    }

    public String randomName(FakerMode fakerMode) {
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

    public String capitalizeWords(String input) {
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