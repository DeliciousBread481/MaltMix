package com.deliciousbread481.maltmix.ui.widget;  
  
import android.content.Context;  
import android.util.AttributeSet;  
  
public class MarqueeTextView extends androidx.appcompat.widget.AppCompatTextView {  
    public MarqueeTextView(Context c) { super(c); }  
    public MarqueeTextView(Context c, AttributeSet a) { super(c, a); }  
    public MarqueeTextView(Context c, AttributeSet a, int s) { super(c, a, s); }  
  
    @Override  
    public boolean isFocused() { return true; }  
}