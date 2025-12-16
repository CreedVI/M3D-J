package com.creedvi.utils.m3dj.model.chunks;

import static com.creedvi.utils.m3dj.model.chunks.VariableTypes.BonesPerVertex.*;
import static com.creedvi.utils.m3dj.model.chunks.VariableTypes.VariableType.*;
import static com.creedvi.utils.m3dj.model.chunks.VariableTypes.VertexCoordType.*;

public class VariableTypes {

    public enum VertexCoordType {
        INT8((byte) 0b00, Byte.BYTES),
        INT16((byte) 0b01, Short.BYTES),
        FLOAT((byte) 0b10, Float.BYTES),
        DOUBLE((byte) 0b11, Double.BYTES),
        ;

        final public byte bytePattern;
        final public int size;
        VertexCoordType(byte bytePattern, int size) {
            this.bytePattern = bytePattern;
            this.size = size;
        }
    }

    public static VertexCoordType GetVertexCoordTypeByBytePattern(byte bytePattern) {
        if (bytePattern == INT8.bytePattern) {
            return INT8;
        }
        else if (bytePattern == INT16.bytePattern) {
            return INT16;
        }
        else if (bytePattern == FLOAT.bytePattern) {
            return FLOAT;
        }
        else if (bytePattern == DOUBLE.bytePattern) {
            return DOUBLE;
        }
        else {
            return null;
        }
    }

    public enum VariableType {
        UINT8((byte) 0b00, Byte.BYTES),
        UINT16((byte) 0b01, Short.BYTES),
        UINT32((byte) 0b10, Float.BYTES),
        UNDEFINED((byte) 0b11, 0),
        ;

        final public byte bytePattern;
        final public int size;
        VariableType(byte b, int size) {
            this.bytePattern = b;
            this.size = size;
        }
    }

    public static VariableType GetVariableTypeByBytePattern(byte bytePattern) {
        if (bytePattern == UINT8.bytePattern) {
            return UINT8;
        }
        else if (bytePattern == VariableType.UINT16.bytePattern) {
            return VariableType.UINT16;
        }
        else if (bytePattern == UINT32.bytePattern) {
            return UINT32;
        }
        else if (bytePattern == UNDEFINED.bytePattern) {
            return UNDEFINED;
        }
        else {
            return null;
        }
    }

    public enum BonesPerVertex {
        ONE((byte) 0b00),
        TWO((byte) 0b01),
        FOUR((byte) 0b10),
        EIGHT((byte) 0b11),
        ;

        final public byte bytePattern;
        BonesPerVertex(byte b) {
            this.bytePattern = b;
        }
    }

    public static BonesPerVertex GetBonesPerVertexByBytePattern(byte bytePattern) {
        if (bytePattern == ONE.bytePattern) {
            return ONE;
        }
        else if (bytePattern == TWO.bytePattern) {
            return TWO;
        }
        else if (bytePattern == FOUR.bytePattern) {
            return FOUR;
        }
        else if (bytePattern == EIGHT.bytePattern) {
            return EIGHT;
        }
        else {
            return null;
        }
    }

}
