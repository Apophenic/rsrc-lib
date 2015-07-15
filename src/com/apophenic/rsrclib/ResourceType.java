package com.apophenic.rsrclib;

public enum ResourceType
{
    ALIS, ALRT, APPL, BNDL, CLCN, CLUT, CODE, CURS, DITL, DLOG, FREF, HFDR, ICL8, ICNS, ICON, KIND, MBAR, MDEF, MOOV,
    OPEN, PICT, PREF, SND, STR, STYL, TEXT, TEMPL, VERS, WDEF, WIND,
    PNG;

    public static ResourceType getValue(byte[] data)
    {
        String type = "";
        for (byte b : data)
        {
            type += (char) b;
        }

        return ResourceType.valueOf(type.toUpperCase().trim());
    }
}
