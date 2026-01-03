package com.creedvi.utils.m3dj.model.chunks;

public class M3DJ_Property {

    public enum PropertyFormat {
        COLOR,
        UINT8,
        UINT16,
        UINT32,
        FLOAT,
        MAP
    }

    public int id;
    public PropertyFormat format;
    public String key;

    protected int color;
    protected int value;
    protected float floatingValue;
    protected int textureId;

    public M3DJ_Property() {
        color = -1;
        value = -1;
        floatingValue = -1.0f;
        textureId = -1;
    }

    public M3DJ_Property(int id, PropertyFormat format, String key) {
        this.id = id;
        this.format = format;
        this.key = key;
    }

    public Object GetPropertyValue() {
        return switch (this.format) {
            case COLOR -> color;
            case UINT8, UINT16, UINT32 -> value;
            case FLOAT -> floatingValue;
            case MAP -> textureId;
        };
    }

    public void SetPropertyValue(float value) {
        switch (this.format) {
            case COLOR -> this.color = (int) value;
            case UINT8, UINT16, UINT32 -> this.value = (int) value;
            case FLOAT -> this.floatingValue = value;
            case MAP -> this.textureId = (int) value;
        }
    }

    public static M3DJ_Property[] propertyTypes = new M3DJ_Property[] {
        // Scalar Display Properties
        new M3DJ_Property(0, PropertyFormat.COLOR, "Kd"),
        new M3DJ_Property(1, PropertyFormat.COLOR, "Ka"),
        new M3DJ_Property(2, PropertyFormat.COLOR, "Ks"),
        new M3DJ_Property(3, PropertyFormat.FLOAT, "Ns"),
        new M3DJ_Property(4, PropertyFormat.COLOR, "Ke"),
        new M3DJ_Property(5, PropertyFormat.COLOR, "Tf"),
        new M3DJ_Property(6, PropertyFormat.FLOAT, "Km"),
        new M3DJ_Property(7, PropertyFormat.FLOAT, "d"),
        new M3DJ_Property(8, PropertyFormat.UINT8, "il"),

        // Scalar Physical Properties
        new M3DJ_Property(64, PropertyFormat.FLOAT, "Pr"),
        new M3DJ_Property(65, PropertyFormat.FLOAT, "Pm"),
        new M3DJ_Property(66, PropertyFormat.FLOAT, "PS"),
        new M3DJ_Property(67, PropertyFormat.FLOAT, "Ni"),
        new M3DJ_Property(68, PropertyFormat.FLOAT, "Nt"),

        // Textured display map properties
        new M3DJ_Property(128, PropertyFormat.MAP, "map_Kd"),
        new M3DJ_Property(129, PropertyFormat.MAP, "map_Ka"),
        new M3DJ_Property(130, PropertyFormat.MAP, "map_Ks"),
        new M3DJ_Property(131, PropertyFormat.MAP, "map_Ns"),
        new M3DJ_Property(132, PropertyFormat.MAP, "map_Ke"),
        new M3DJ_Property(133, PropertyFormat.MAP, "map_Tf"),
        new M3DJ_Property(134, PropertyFormat.MAP, "map_Km"),
        new M3DJ_Property(134, PropertyFormat.MAP, "map_Km"),
        new M3DJ_Property(135, PropertyFormat.MAP, "map_d"),
        new M3DJ_Property(136, PropertyFormat.MAP, "map_N"),
        new M3DJ_Property(136, PropertyFormat.MAP, "map_il"),

        // Textured Physical Map Properties
        new M3DJ_Property(192, PropertyFormat.MAP, "map_Pr"),
        new M3DJ_Property(193, PropertyFormat.MAP, "map_Pm"),
        new M3DJ_Property(193, PropertyFormat.MAP, "map_refl"),
        new M3DJ_Property(194, PropertyFormat.MAP, "map_Ps"),
        new M3DJ_Property(195, PropertyFormat.MAP, "map_Ni"),
        new M3DJ_Property(196, PropertyFormat.MAP, "map_Nt"),
    };

}