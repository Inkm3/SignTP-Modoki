package com.github.inkm3.signtp.SignData;

import java.io.Serializable;

public class SerializeTextData implements Serializable {

    private static final String[] def = new String[]{ "", "", "", "" };

    public final String[]
            front,
            back,
            display;

    public SerializeTextData() {
        this.front = def;
        this.back = def;
        this.display = def;
    }

    public SerializeTextData(String[] front, String[] back, String[] display) {
        this.front = front;
        this.back = back;
        this.display = display;
    }

}
