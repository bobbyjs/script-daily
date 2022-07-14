package org.dreamcat.daily.script.poetry;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Jerry Will
 * @version 2022-05-19
 */
@Getter
@Setter
public class Poetry {

    private String author;
    private List<String> paragraphs;

    public String getTitle() {
        return null;
    }
}
