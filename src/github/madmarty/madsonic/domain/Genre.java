package github.madmarty.madsonic.domain;

import java.io.Serializable;

public class Genre implements Serializable {

	private static final long serialVersionUID = 3260624156055663639L;
	private String name;
    private String index;
	
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    public String getIndex() {
        return index;
    }

    public void setIndex(String index) {
        this.index = index;
    }
    
    @Override
    public String toString() {
        return name;
    }
}
