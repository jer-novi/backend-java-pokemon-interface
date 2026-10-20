package io;

import java.util.List;

public interface GameIO {
    void println(String text);

    String readLine(String prompt);

    default String readLine(String prompt, List<String> options) {
        return ChoiceHelper.resolveInput(readLine(prompt), options);
    }
}
