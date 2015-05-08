package cjmaier2_dkturne2.nextstop;

/**
 * Enum of available routes and their associated colors.
 */
public enum Route {
    YELLOW(0xFFFFF200),
    RED(0xFFED1C24),
    LAVENDER(0xFFA78BC1),
    BLUE(0xFF335DAB),
    GREEN(0xFF008063),
    ORANGE(0xFFF99F2B),
    GREY(0xFF818285),
    BRONZE(0xFF9E8A66),
    BROWN(0xFF825622),
    GOLD(0xFFC99A4B),
    RUBY(0xFFDE1E64),
    TEAL(0xFF006991),
    SILVER(0xFFD1D2D4),
    NAVY(0xFF2A318A),
    PINK(0xFFF9CBDF),
    ILLINI(0xFF591C5A),
    AIRBUS(0xFF67B6E6),
    LIME(0xFF6FBE44);

    private int colVal;

    Route(int colVal){
        this.colVal = colVal;
    }

    public int getColVal(){
        return colVal;
    }
}
