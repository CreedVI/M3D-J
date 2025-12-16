package com.creedvi.utils.m3dj.model.chunks;

import com.creedvi.utils.m3dj.io.Tracelog;
import com.creedvi.utils.m3dj.model.chunks.VariableTypes.*;

public class M3DJ_Header {

    public float scale;
    public String title;
    public String author;
    public String licence;
    public String description;

    public VertexCoordType VC_T;
    public VariableType VI_T;
    public VariableType SI_T;
    public VariableType CI_T;
    public VariableType TI_T;
    public VariableType BI_T;
    public BonesPerVertex NB_T;
    public VariableType SK_T;
    public VariableType FC_T;
    public VariableType HI_T;
    public VariableType FI_T;
    public VariableType VD_T;
    public VariableType VP_T;

    public M3DJ_Header() {
        this.title = "";
        this.author = "";
        this.licence = "";
        this.description = "";
    }

    public void DumpBitField(Tracelog logger) {
        logger.out(
                Tracelog.LogType.LOG_DEBUG,
                "Model bitfield is defined as follows:" + "\n" +
                        "\t VC_T [Vertex Coordinate Type]: " + this.VC_T + "\n" +
                        "\t VI_T [Vertex Index Type]: " + this.VI_T + "\n" +
                        "\t SI_T [String Offset Type]: " + this.SI_T + "\n" +
                        "\t CI_T [Color Index Type]: " + this.CI_T + "\n" +
                        "\t TI_T [Texture Index Type]: " + this.TI_T + "\n" +
                        "\t BI_T [Bone Index Type]: " + this.BI_T + "\n" +
                        "\t NB_T [Number of Bones Type]: " + this.NB_T + "\n" +
                        "\t SK_T [Skin Index Type]: " + this.SK_T + "\n" +
                        "\t FC_T [Frame Bones Count]: " + this.FC_T + "\n" +
                        "\t HI_T [Shape Index Type]: " + this.HI_T + "\n" +
                        "\t FI_T [Face Index Type]: " + this.FI_T + "\n" +
                        "\t VD_T [Voxel Dimension Size]: " + this.VD_T + "\n" +
                        "\t VP_T [Voxel Pixel Type]: " + this.VP_T
        );
    }
}
