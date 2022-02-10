package de.snap20lp.servericonchanger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

public class PacketUtils {

    public static int getVarIntSize(int value) {
        int total = 0;
        while (true) {
            value >>>= 7;
            total++;
            if (value == 0) {
                break;
            }
        }
        return total;
    }

    public static int getStringSize(String s) throws UnsupportedEncodingException {
        int total = 0;
        total += getVarIntSize(s.length());
        total += s.getBytes(StandardCharsets.UTF_8).length;
        return total;
    }

    public static int readVarInt(DataInputStream stream) throws IOException {
        int out = 0;
        int bytes = 0;
        byte in;
        while (true) {
            in = stream.readByte();

            out |= (in & 0x7F) << (bytes++ * 7);

            if (bytes > 5) {
                throw new RuntimeException("VarInt too big");
            }

            if ((in & 0x80) != 0x80) {
                break;
            }
        }
        return out;
    }

    public static void writeVarInt(DataOutputStream stream, int value) throws IOException {
        int part;
        while (true) {
            part = value & 0x7F;

            value >>>= 7;
            if (value != 0) {
                part |= 0x80;
            }

            stream.writeByte(part);

            if (value == 0) {
                break;
            }
        }
    }


    public static void writeString(DataOutputStream stream, String s) throws IOException {
        byte[] b = null;
        b = s.getBytes(StandardCharsets.UTF_8);
        writeVarInt(stream, b.length);
        stream.write(b);
    }
}