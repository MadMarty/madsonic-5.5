package github.madmarty.madsonic.domain;

	public enum MadsonicTheme {
		
    DARK  		(1),
    LIGHT 		(2),
    HOLO  		(3),
    RED   		(4),
    PINK    	(5),
    GREEN    	(6),
    BLACK		(7),
    DARK_FULL 	(8),
    LIGHT_FULL  (9),
    HOLO_FULL 	(10),
    RED_FULL 	(11),
    PINK_FULL 	(12),
    GREEN_FULL  (13),
    BLACK_FULL 	(14);
	    
	private int code; 
	
	private MadsonicTheme(int c){
		code = c;
	}
	
	public int getThemeCode() {
		return code;
	}
}
