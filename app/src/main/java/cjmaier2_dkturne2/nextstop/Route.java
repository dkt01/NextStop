package cjmaier2_dkturne2.nextstop;

/**
 * Enum of available routes and their associated colors.
 */
public enum Route {
    YELLOW(0xFFF200),
    RED(0xED1C24),
    LAVENDER(0xA78BC1),
    BLUE(0x335DAB),
    GREEN(0x008063),
    ORANGE(0xF99F2B),
    GREY(0x818285),
    BRONZE(0x9E8A66),
    BROWN(0x825622),
    GOLD(0xC99A4B),
    RUBY(0xDE1E64),
    TEAL(0x006991),
    SILVER(0xD1D2D4),
    NAVY(0x2A318A),
    PINK(0xF9CBDF),
    ILLINI(0x591C5A),
    AIRBUS(0x67B6E6),
    LIME(0x6FBE44);

    private int colVal;

    Route(int colVal){
        this.colVal = colVal;
    }

    public int getColVal(){
        return colVal;
    }
}
