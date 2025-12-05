package com.example.woil;

public class CategoryModel {
    public String title;
    // either a drawable resource id (Integer) or a String URL
    public Object icon;

    public CategoryModel() { }

    public CategoryModel(String title, int drawableRes) {
        this.title = title;
        this.icon = drawableRes;
    }

    public CategoryModel(String title, String imageUrl) {
        this.title = title;
        this.icon = imageUrl;
    }
}
