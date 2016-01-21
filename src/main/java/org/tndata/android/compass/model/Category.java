package org.tndata.android.compass.model;

import android.widget.ImageView;

import org.tndata.android.compass.util.ImageLoader;

import java.io.Serializable;


/**
 * Model class for categories.
 *
 * @author Edited by Ismael Alonso
 * @version 1.0.0
 */
public class Category extends TDCBase implements Serializable{
    private static final long serialVersionUID = -1751642109285216370L;

    private int order = -1;
    private String image_url = "";
    private String color = "";
    private String secondary_color = "";

    private boolean packaged_content = false;


    /*---------*
     * GETTERS *
     *---------*/

    public int getOrder(){
        return order;
    }

    public String getImageUrl(){
        return image_url;
    }

    public String getColor(){
        return color;
    }

    public String getSecondaryColor(){
        return this.secondary_color;
    }

    public boolean isPackagedContent(){
        return packaged_content;
    }


    /*---------*
     * UTILITY *
     *---------*/

    public void loadImageIntoView(ImageView imageView){
        String url = getImageUrl();
        if (url != null && !url.isEmpty()){
            ImageLoader.Options options = new ImageLoader.Options()
                    .setUsePlaceholder(false)
                    .setCropToCircle(true);
            ImageLoader.loadBitmap(imageView, url, options);
        }
    }

    @Override
    public String toString(){
        return "Category #" + getId() + ": " + getTitle();
    }
}
